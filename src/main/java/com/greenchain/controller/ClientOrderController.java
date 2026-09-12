package com.greenchain.controller;

import com.greenchain.entity.Order;
import com.greenchain.entity.OrderItem;
import com.greenchain.entity.Product;
import com.greenchain.entity.UserAddress;
import com.greenchain.annotation.IdempotentToken;
import com.greenchain.dto.request.ClientOrderCreateRequest;
import com.greenchain.dto.request.ClientOrderItemRequest;
import com.greenchain.dto.response.PaymentVO;
import com.greenchain.mapper.OrderItemMapper;
import com.greenchain.mapper.OrderMapper;
import com.greenchain.mapper.ProductMapper;
import com.greenchain.mapper.UserAddressMapper;
import com.greenchain.service.OrderService;
import com.greenchain.service.PaymentService;
import com.greenchain.util.CacheUtil;
import com.greenchain.util.OrderNoGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/client/orders")
public class ClientOrderController {

    /** Redis 分布式锁 key 前缀：lock:product:{productId} */
    private static final String LOCK_KEY_PREFIX = "lock:product:";

    /** 锁自动过期时长（秒），防止持有者异常宕机导致死锁 */
    private static final long LOCK_EXPIRE_SECONDS = 10L;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private CacheUtil cacheUtil;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单
     * <p>
     * 参数校验：请求体由 {@link ClientOrderCreateRequest} 承载，方法参数上的 {@code @Valid}
     * 在绑定阶段触发声明式校验（明细非空、商品ID非空、数量 1~9999、备注长度），
     * 校验失败由 GlobalExceptionHandler 统一返回 {@code {code:400, message:"..."}}。
     * 原先使用 Map 接收、方法体内靠 instanceof 取值，现已改为 DTO，JSON 结构不变。
     * <p>
     * 幂等保护：请求头携带 Idempotent-Token（先调 GET /api/client/idempotent/token 获取），
     * 由 IdempotentAspect 校验并原子消费令牌，防止重复提交；业务逻辑本身不受影响。
     */
    @PostMapping
    @IdempotentToken
    public Map<String, Object> createOrder(@RequestAttribute("userId") Long userId,
                                           @Valid @RequestBody ClientOrderCreateRequest request) {
        Map<String, Object> result = new HashMap<>();

        List<ClientOrderItemRequest> items = request.getItems();
        Long addressId = request.getAddressId();
        String remark = request.getRemark();

        // ===== 新增①：下单二次校验（结算兜底，防止绕过购物车直接传参下单） =====
        // 数量的合法性（非空、1~9999）已由 ClientOrderItemRequest 的注解在参数绑定阶段拦下，
        // 这里只做"必须查库才能判断"的部分：商品存在、商品上架（status=1）；
        // 库存是否充足由锁内条件扣减保证。
        // 商品快照缓存：productId → Product，后续算价/建明细直接复用，避免重复查库
        Map<Long, Product> productSnapshot = new HashMap<>();
        for (ClientOrderItemRequest item : items) {
            Long productId = item.getProductId();

            Product product = productMapper.selectById(productId);
            if (product == null) {
                result.put("code", 404);
                result.put("message", "商品不存在");
                return result;
            }
            if (product.getStatus() == null || product.getStatus() != 1) {
                result.put("code", 400);
                result.put("message", "商品已下架，无法下单");
                return result;
            }
            productSnapshot.put(productId, product);
        }

        // ===== 新增②：Redis 分布式锁 + 条件扣库存（防超卖） =====
        // 锁 key：lock:product:{productId}，SETNX + 10s 过期，加锁失败直接返回业务失败（不自旋）；
        // 扣减使用数据库条件 UPDATE（stock >= quantity 才扣），即使锁异常也不会扣成负数；
        // 扣减成功的条目记录在 deducted 中，本订单后续步骤失败时统一回补（订单内补偿）。
        List<ClientOrderItemRequest> deducted = new ArrayList<>();
        for (ClientOrderItemRequest item : items) {
            Long productId = item.getProductId();
            Integer quantity = item.getQuantity();

            String lockKey = LOCK_KEY_PREFIX + productId;
            // 加锁失败：回补已扣库存并直接返回，不做自旋等待
            String lockToken = cacheUtil.tryLock(lockKey, LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
            if (lockToken == null) {
                compensateStock(deducted);
                result.put("code", 429);
                result.put("message", "当前下单人数较多，请稍后再试");
                return result;
            }

            try {
                // 条件扣减：返回 0 表示库存不足（stock < quantity）
                int rows = productMapper.deductStock(productId, quantity);
                if (rows == 0) {
                    compensateStock(deducted);
                    result.put("code", 400);
                    result.put("message", "商品库存不足");
                    return result;
                }
                deducted.add(item);
            } finally {
                // 无论成功失败，释放本条锁
                cacheUtil.unlock(lockKey, lockToken);
            }
        }

        // ===== 原有订单创建逻辑（复用）：失败时回补已扣库存 =====
        Order order = null;
        try {
            // ===== B3 修复：订单总额一律按 DB 现价重算，不再信任请求体传入的 price =====
            BigDecimal totalAmount = BigDecimal.ZERO;
            int totalQuantity = 0;

            for (ClientOrderItemRequest item : items) {
                // 取 DB 现价（商品快照已在上面的二次校验中查好，直接复用）
                BigDecimal price = productSnapshot.get(item.getProductId()).getPrice();

                totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
                totalQuantity += item.getQuantity();
            }

            order = new Order();
            // 订单号统一由 OrderNoGenerator 生成（GC + 毫秒时间戳 + 6 位随机码）。
            // 原实现为 "GN" + System.currentTimeMillis()，同一毫秒内的并发下单会生成相同订单号，
            // 触发 order_no 的 UNIQUE 约束导致下单失败（见 backend-run.log 中的历史报错）。
            order.setOrderNo(OrderNoGenerator.generate());
            order.setUserId(userId);
            order.setAddressId(addressId);
            order.setTotalAmount(totalAmount);
            order.setTotalQuantity(totalQuantity);
            order.setStatus("pending");
            order.setRemark(remark);
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());

            // 组装明细（orderId 由 OrderService 在事务内回填，此处不预设）
            List<OrderItem> orderItems = new ArrayList<>();
            for (ClientOrderItemRequest item : items) {
                // B3 修复：明细单价同样取 DB 现价（复用商品快照），彻底废弃请求体 price
                Product product = productSnapshot.get(item.getProductId());

                OrderItem orderItem = new OrderItem();
                orderItem.setProductId(item.getProductId());
                orderItem.setName(product != null ? product.getName() : "");
                orderItem.setImage(product != null ? product.getImage() : "");
                orderItem.setSpec("");
                orderItem.setPrice(product != null ? product.getPrice() : BigDecimal.ZERO);
                orderItem.setQuantity(item.getQuantity());
                orderItem.setCreateTime(LocalDateTime.now());

                orderItems.add(orderItem);
            }

            // 主表与明细在同一事务内写入：任一步失败整体回滚，不会残留"有订单无明细"的脏数据。
            // 说明：库存扣减故意不纳入本事务 —— 它发生在 Redis 锁内（每条商品一次短事务），
            // 若并入大事务，解锁会早于事务提交，持锁期间的行锁等待反而会放大并发冲突；
            // 因此库存仍沿用锁内条件 UPDATE + 下方 compensateStock 补偿的方案。
            orderService.saveOrderWithItems(order, orderItems);
        } catch (Exception e) {
            // 订单创建失败：回补本订单已扣减的库存，避免库存凭空丢失
            compensateStock(deducted);
            result.put("code", 500);
            result.put("message", "下单失败，请重试");
            return result;
        }

        result.put("code", 200);
        result.put("message", "下单成功");
        result.put("data", order);
        return result;
    }

    /**
     * 下单中途失败时，回补本订单已成功扣减的库存（订单内补偿，非订单取消回滚）
     * 注意：本阶段不实现订单取消/退款的库存回滚，取消订单仍沿用原有回补逻辑。
     */
    private void compensateStock(List<ClientOrderItemRequest> deducted) {
        for (ClientOrderItemRequest item : deducted) {
            if (item.getProductId() == null) continue;
            productMapper.restoreStock(item.getProductId(), item.getQuantity());
        }
    }

    @GetMapping
    public Map<String, Object> getOrders(@RequestAttribute("userId") Long userId,
                                         @RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "20") Integer size,
                                         @RequestParam(required = false) String status) {
        Map<String, Object> result = new HashMap<>();

        List<Order> orders = orderMapper.findByUserId(userId);

        if (status != null && !status.isEmpty()) {
            List<Order> filtered = new ArrayList<>();
            for (Order o : orders) {
                if (status.equals(o.getStatus())) {
                    filtered.add(o);
                }
            }
            orders = filtered;
        }

        for (Order order : orders) {
            if (order.getAddressId() != null) {
                UserAddress address = userAddressMapper.selectById(order.getAddressId());
                if (address != null) {
                    order.setAddressName(address.getName());
                    order.setAddressPhone(address.getPhone());
                    order.setAddressDetail(address.getProvince() + address.getCity() + address.getDistrict() + address.getDetail());
                }
            }
        }

        result.put("code", 200);
        result.put("data", orders);
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getOrder(@RequestAttribute("userId") Long userId,
                                        @PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();

        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            result.put("code", 404);
            result.put("message", "订单不存在");
            return result;
        }

        List<OrderItem> items = orderItemMapper.findByOrderId(id);

        if (order.getAddressId() != null) {
            UserAddress address = userAddressMapper.selectById(order.getAddressId());
            if (address != null) {
                order.setAddressName(address.getName());
                order.setAddressPhone(address.getPhone());
                order.setAddressDetail(address.getProvince() + address.getCity() + address.getDistrict() + address.getDetail());
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("items", items);

        result.put("code", 200);
        result.put("data", data);
        return result;
    }

    @PostMapping("/{id}/pay")
    public Map<String, Object> payOrder(@RequestAttribute("userId") Long userId,
                                        @PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();

        // ===== 真实支付打通：payOrder 语义改为"发起预下单"（原同步置已付款逻辑移至支付回调） =====
        // 仅创建支付流水并返回支付信息，订单仍为 pending，待 PayPaymentChannel 异步回调 notify 后置 paid。
        PaymentVO vo = paymentService.createPayment(userId, id);
        result.put("code", 200);
        result.put("message", "发起支付成功");
        result.put("data", vo);
        return result;
    }

    @PostMapping("/{id}/cancel")
    public Map<String, Object> cancelOrder(@RequestAttribute("userId") Long userId,
                                           @PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();

        // 先做归属校验（仅为了给出准确的 404 提示；真正的状态流转由下面的原子 UPDATE 保证）
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            result.put("code", 404);
            result.put("message", "订单不存在");
            return result;
        }

        // ===== B4 修复①：原子状态流转（条件 UPDATE） =====
        // 仅当 status IN (pending, paid) 时才置为 cancelled；
        // 并发双取消时数据库行级锁保证只有一个请求影响行数为 1，另一个为 0。
        int rows = orderMapper.cancelIfPendingOrPaid(id, userId);
        if (rows == 0) {
            result.put("code", 400);
            result.put("message", "订单状态不可取消");
            return result;
        }

        // ===== B4 修复②：仅取消成功才回补库存，且改用原子 UPDATE（stock = stock + n） =====
        // 替换原先 selectById → setStock(stock+n) → updateById 的读改写：
        // 读改写在并发下会丢失更新，导致重复取消时库存虚增。
        List<OrderItem> items = orderItemMapper.findByOrderId(id);
        for (OrderItem item : items) {
            productMapper.restoreStock(item.getProductId(), item.getQuantity());
        }

        result.put("code", 200);
        result.put("message", "订单已取消");
        return result;
    }

    @PostMapping("/{id}/confirm")
    public Map<String, Object> confirmReceive(@RequestAttribute("userId") Long userId,
                                              @PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();

        // ===== P0-3 修复：原子确认收货（条件 UPDATE 替换"先查后改"）=====
        // 归属校验 + 状态流转校验一步完成，数据库行级锁保证并发多次确认仅 1 次成功。
        // 影响行数=0 时区分"404不存在/非本人"与"400状态不可确认"：先查一次归属给准确提示，
        // 真正更新仍走原子 SQL，避免检查-更新之间的 TOCTOU 竞态。
        Order checkOrder = orderMapper.selectById(id);
        if (checkOrder == null || !checkOrder.getUserId().equals(userId)) {
            result.put("code", 404);
            result.put("message", "订单不存在");
            return result;
        }

        int rows = orderMapper.confirmIfPaidOrShipped(id, userId);
        if (rows == 0) {
            result.put("code", 400);
            result.put("message", "订单状态不正确，无法确认收货");
            return result;
        }

        // 销量 sales 原子累加：确保"每确认一笔仅加一次数量"，与上面的原子确认配套
        // （未来若改为"支付成功即加销量"，把这里的代码移至 payIfPending/markPaidByOrderNo 成功分支即可）
        try {
            List<OrderItem> items = orderItemMapper.findByOrderId(id);
            for (OrderItem item : items) {
                productMapper.addSales(item.getProductId(), item.getQuantity());
            }
        } catch (Exception e) {
            // 销量统计失败不影响确认收货主流程，仅记录错误
            log.warn("确认收货：订单 {} 销量累加失败：{}", id, e.getMessage());
        }

        result.put("code", 200);
        result.put("message", "已确认收货");
        return result;
    }
}