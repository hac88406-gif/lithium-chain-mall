package com.greenchain.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.greenchain.entity.CustomerServiceRequest;
import com.greenchain.entity.Order;
import com.greenchain.mapper.CarouselMapper;
import com.greenchain.mapper.CategoryMapper;
import com.greenchain.mapper.CustomerServiceRequestMapper;
import com.greenchain.mapper.OrderMapper;
import com.greenchain.service.OrderService;
import com.greenchain.util.CacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 订单与缓存相关的定时任务
 * <p>
 * 包含两个任务：
 * <ol>
 *   <li>超时未支付订单自动关单：每 5 分钟扫描一次，取消"待支付"且创建时间超过阈值的订单，
 *       并通过系统取消接口自动回补库存。集群多实例场景下用 Redis 分布式锁防重复执行。</li>
 *   <li>每日缓存预热：凌晨 00:05 将首页热点数据（轮播图、商品分类）主动写入 Redis，
 *       首个请求到达时直接命中缓存，避免数据库冷启动。</li>
 * </ol>
 * <p>
 * 所有任务均使用 tryLock/unlock 实现单实例执行，任务内异常独立 try-catch，
 * 一个任务失败不影响另一个任务。
 */
@Slf4j
@Component
public class OrderScheduleTask {

    /** 超时关单任务的分布式锁 key（集群多实例防重复执行） */
    private static final String LOCK_CANCEL_ORDER = "lock:schedule:cancel-order";

    /** 缓存预热任务的分布式锁 key */
    private static final String LOCK_CACHE_WARMUP = "lock:schedule:cache-warmup";

    /** 锁自动过期时长（分钟），防止任务执行期间持有者宕机导致死锁 */
    private static final long LOCK_EXPIRE_MINUTES = 4L;

    /** 缓存 key：首页轮播图列表（与 ClientProductController 保持一致） */
    private static final String CACHE_KEY_CAROUSEL_LIST = "cache:carousel:list";

    /** 缓存 key：商品分类列表（与 ClientProductController 保持一致） */
    private static final String CACHE_KEY_CATEGORY_LIST = "cache:category:list";

    /** 客服工单超时关闭任务的分布式锁 key */
    private static final String LOCK_TIMEOUT_SERVICE_REQ = "lock:schedule:timeout-service-req";

    /** 缓存 TTL（分钟），与 ClientProductController 设置一致 */
    private static final long CACHE_TTL_MINUTES = 30L;

    /** 超时未支付的分钟数（默认 30 分钟，可临时改为 1 分钟测试） */
    @Value("${order.timeout.minutes:30}")
    private long orderTimeoutMinutes;

    /** 客服工单闲置超时分钟数（默认 5 分钟：客户关窗口后兜底关闭，避免卡死在活跃中） */
    @Value("${service-request.idle.timeout.minutes:5}")
    private long serviceRequestIdleTimeoutMinutes;

    @Autowired
    private CacheUtil cacheUtil;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private CarouselMapper carouselMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private CustomerServiceRequestMapper requestMapper;

    /**
     * 超时未支付订单自动关单
     * <p>
     * 默认每 5 分钟执行一次（cron 可在 application.yml 中通过 order.cancel.cron 覆盖），
     * 查询条件：status = "pending" 且 createTime < (now - orderTimeoutMinutes)。
     * 抢不到分布式锁说明集群其他实例已在执行本次任务，直接 return 不自旋。
     */
    @Scheduled(cron = "${order.cancel.cron:0 */5 * * * ?}")
    public void cancelTimeoutOrders() {
        // ① 抢分布式锁：抢不到直接 return（集群多实例防重复）
        String lockToken = cacheUtil.tryLock(LOCK_CANCEL_ORDER, LOCK_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (lockToken == null) {
            log.debug("超时关单任务：抢锁失败，可能其他实例已在执行，跳过本次");
            return;
        }

        int cancelledCount = 0;
        try {
            // ② 查询超时未支付订单：status=pending 且 createTime 早于阈值
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(orderTimeoutMinutes);
            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Order::getStatus, "pending")
                   .lt(Order::getCreateTime, threshold);
            List<Order> timeoutOrders = orderMapper.selectList(wrapper);

            if (timeoutOrders.isEmpty()) {
                log.debug("超时关单任务：当前无超时待支付订单，本次跳过");
                return;
            }

            // ③ 逐单调用系统取消接口（含库存回补 + 幂等保护）
            for (Order order : timeoutOrders) {
                try {
                    orderService.cancelBySystem(order.getId(), "超时未支付自动关单");
                    cancelledCount++;
                } catch (Exception e) {
                    // 单订单取消失败不影响其他订单，仅记 warn 日志
                    log.warn("超时关单任务：订单 {} 取消失败，跳过：{}", order.getOrderNo(), e.getMessage());
                }
            }

            log.info("超时关单任务完成：共发现 {} 笔超时订单，成功取消 {} 笔，阈值={}分钟前",
                    timeoutOrders.size(), cancelledCount, orderTimeoutMinutes);

        } catch (Exception e) {
            log.error("超时关单任务异常：{}", e.getMessage(), e);
        } finally {
            // ④ 释放分布式锁
            cacheUtil.unlock(LOCK_CANCEL_ORDER, lockToken);
        }
    }

    /**
     * 每日缓存预热（凌晨 00:05 执行）
     * <p>
     * 将首页热点数据（轮播图、商品分类）主动查询并写入 Redis，
     * 使首个用户请求直接命中缓存，避免数据库冷启动导致的首次访问慢。
     * <p>
     * Cache Aside 模式预热策略：直接查数据库后 set 缓存，不绕 Controller 的"先 get 再回源再 set"逻辑，
     * 因为预热的目的就是确保缓存一定存在，不存在才需要写。
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void warmupCache() {
        // 抢分布式锁：集群多实例只执行一次预热
        String lockToken = cacheUtil.tryLock(LOCK_CACHE_WARMUP, LOCK_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (lockToken == null) {
            log.debug("缓存预热任务：抢锁失败，可能其他实例已在执行，跳过本次");
            return;
        }

        try {
            // 预热轮播图
            List<com.greenchain.entity.Carousel> carousels = carouselMapper.selectList(null);
            cacheUtil.set(CACHE_KEY_CAROUSEL_LIST, carousels, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("缓存预热成功：轮播图 {} 条", carousels.size());

            // 预热商品分类
            List<com.greenchain.entity.Category> categories = categoryMapper.selectList(null);
            cacheUtil.set(CACHE_KEY_CATEGORY_LIST, categories, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("缓存预热成功：商品分类 {} 条", categories.size());

            log.info("每日缓存预热任务完成");
        } catch (Exception e) {
            log.error("缓存预热任务异常：{}", e.getMessage(), e);
        } finally {
            cacheUtil.unlock(LOCK_CACHE_WARMUP, lockToken);
        }
    }

    /**
     * 客服工单闲置超时自动关闭（后端兜底）
     * <p>
     * 每 1 分钟扫描一次：status IN (0,1) 且 updateTime 早于阈值的工单，批量置为 2=超时关闭。
     * 用 updateTime 做判断——客户/管理员每发一条消息都会 update updateTime。
     * 为什么需要后端兜底？前端 10 秒闲置倒计时只在聊天窗口打开时才跑，
     * 客户关窗口/刷新页面/浏览器崩了 → 前端定时器消失 → 工单卡死在 0 或 1。
     * 后端用 5 分钟宽松阈值兜底，确保无工单永远停在"活跃中"。
     */
    @Scheduled(cron = "${service-request.timeout.cron:0 */1 * * * ?}")
    public void closeIdleServiceRequests() {
        String lockToken = cacheUtil.tryLock(LOCK_TIMEOUT_SERVICE_REQ, LOCK_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (lockToken == null) {
            log.debug("客服超时关闭任务：抢锁失败，跳过本次");
            return;
        }
        try {
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(serviceRequestIdleTimeoutMinutes);
            // 找所有活跃中（0=待应答/1=活跃中）且最后活动时间早于阈值的工单
            LambdaQueryWrapper<CustomerServiceRequest> w = new LambdaQueryWrapper<>();
            w.in(CustomerServiceRequest::getStatus, 0, 1)
             .lt(CustomerServiceRequest::getUpdateTime, threshold);
            List<CustomerServiceRequest> idleList = requestMapper.selectList(w);

            if (idleList.isEmpty()) return;

            // 批量更新为超时关闭（status=2）
            for (CustomerServiceRequest req : idleList) {
                try {
                    CustomerServiceRequest update = new CustomerServiceRequest();
                    update.setId(req.getId());
                    update.setStatus(2);
                    update.setUpdateTime(LocalDateTime.now());
                    requestMapper.updateById(update);
                } catch (Exception e) {
                    log.warn("客服工单超时关闭：工单 {} 更新失败，跳过：{}", req.getId(), e.getMessage());
                }
            }
            log.info("客服超时关闭任务：共扫描到 {} 条闲置工单，已置为超时关闭(2)，阈值={}分钟前",
                    idleList.size(), serviceRequestIdleTimeoutMinutes);
        } catch (Exception e) {
            log.error("客服超时关闭任务异常：{}", e.getMessage(), e);
        } finally {
            cacheUtil.unlock(LOCK_TIMEOUT_SERVICE_REQ, lockToken);
        }
    }
}
