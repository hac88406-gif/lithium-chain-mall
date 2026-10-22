package com.greenchain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greenchain.common.BusinessException;
import com.greenchain.dto.response.OrderItemVO;
import com.greenchain.dto.response.OrderVO;
import com.greenchain.dto.response.PageResult;
import com.greenchain.entity.*;
import com.greenchain.mapper.*;
import com.greenchain.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单服务实现类
 * <p>
 * 注意：客户端下单统一走 ClientOrderController（直购链路，小写状态值 pending/cancelled）；
 * 本类提供 saveOrderWithItems 供其复用事务化写入（先主表后明细，异常整体回滚），
 * 同时为管理后台（listAll / getById）与定时任务（cancelBySystem 超时关单）提供服务。
 * 库存回补统一使用 ProductMapper.restoreStock 原子加法；
 * 但"是否允许回补"由 OrderMapper 的条件 UPDATE 决定，避免并发重复回补。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order saveOrderWithItems(Order order, List<OrderItem> items) {
        // 先写主表：insert 后 MyBatis-Plus 会回填自增主键，明细依赖该 id 作为外键
        orderMapper.insert(order);

        if (items != null) {
            for (OrderItem item : items) {
                item.setOrderId(order.getId());
                orderItemMapper.insert(item);
            }
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelBySystem(Long orderId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            log.warn("系统取消失败：订单不存在，orderId={}", orderId);
            return;
        }
        // 快速路径：非"待支付"状态直接跳过，避免无谓的明细查询与日志噪音。
        // 注意：这里只是性能优化，不能当作并发正确性的保证——
        //       真正的幂等由 doCancelOrder 里的条件 UPDATE（WHERE status='pending'）兜住，
        //       因为本次查询之后状态仍可能被用户手动取消改掉。
        if (!"pending".equals(order.getStatus())) {
            log.warn("系统取消跳过：订单 {} 状态为 {}，非待支付，无需关单",
                    order.getOrderNo(), order.getStatus());
            return;
        }
        doCancelOrder(order, reason);
    }

    /**
     * 共享的订单取消私有方法：条件更新状态 + 原子回补库存
     * <p>
     * 系统取消（cancelBySystem）及未来的取消场景共用本方法，
     * 取消前的归属校验由调用方负责，但<b>状态流转一律由本方法的条件 UPDATE 决定</b>。
     * <p>
     * 两步顺序不能反：
     * <ol>
     *   <li><b>先</b>原子地把 pending 改成 cancelled（{@code WHERE id = ? AND status = 'pending'}），
     *       影响行数=1 才说明本次调用真正完成了关单；</li>
     *   <li><b>再</b>回补库存（{@code stock = stock + n}，原子加法）。</li>
     * </ol>
     * 如果先回补再改状态，或沿用"先查状态再 updateById"，用户手动取消与定时任务并发时
     * 两边都能通过状态判断，库存会被回补两次（虚增）。条件 UPDATE 把"判断 + 修改"合成
     * 一条语句，由数据库行级锁保证只有一方返回 1，从根上避免重复回补。
     *
     * @param order  待取消订单（必须已查好，至少要保证 id 与 orderNo 存在）
     * @param reason 取消原因（仅用于日志记录，Order 实体无 cancelReason 字段不写入数据库）
     */
    private void doCancelOrder(Order order, String reason) {
        // ① 条件更新：只有抢到这次状态流转的调用才允许回补库存
        int rows = orderMapper.cancelIfPendingById(order.getId());
        if (rows == 0) {
            // 已被用户手动取消 / 已被其他实例关单 / 已支付 —— 放弃回补，直接返回
            log.warn("系统取消跳过：订单 {} 状态已变更，本次不回补库存", order.getOrderNo());
            return;
        }

        // ② 原子回补库存（stock = stock + quantity），只在状态流转成功后执行
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, order.getId());
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        long restoredTotal = 0L;
        for (OrderItem item : items) {
            productMapper.restoreStock(item.getProductId(), item.getQuantity());
            restoredTotal += item.getQuantity();
        }

        log.info("订单取消成功：{}，原因：{}，回补库存件数：{}",
                order.getOrderNo(), reason, restoredTotal);
    }

    @Override
    public OrderVO getById(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        return convertToVO(order);
    }

    @Override
    public PageResult<OrderVO> listAll(Long current, Long size, String status) {
        Page<Order> page = new Page<>(current, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        IPage<Order> orderPage = orderMapper.selectPage(page, wrapper);
        IPage<OrderVO> voPage = orderPage.convert(this::convertToVO);
        return PageResult.of(voPage);
    }

    private OrderVO convertToVO(Order order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusName(getStatusName(order.getStatus()));
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());

        // 查询用户信息
        User user = userMapper.selectById(order.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
        }

        // 查询订单明细
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, order.getId());
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        List<OrderItemVO> itemVOs = new ArrayList<>();
        for (OrderItem item : items) {
            OrderItemVO itemVO = new OrderItemVO();
            itemVO.setId(item.getId());
            itemVO.setProductId(item.getProductId());
            itemVO.setProductName(item.getName());
            itemVO.setPrice(item.getPrice());
            itemVO.setQuantity(item.getQuantity());
            itemVO.setSubtotal(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
            itemVOs.add(itemVO);
        }
        vo.setItems(itemVOs);

        return vo;
    }

    private String getStatusName(String status) {
        if ("pending".equals(status)) return "待支付";
        if ("paid".equals(status)) return "已支付";
        if ("shipped".equals(status)) return "已发货";
        if ("completed".equals(status)) return "已完成";
        if ("cancelled".equals(status)) return "已取消";
        return status;
    }
}