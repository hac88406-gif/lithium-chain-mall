package com.greenchain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greenchain.common.BusinessException;
import com.greenchain.dto.request.OrderCreateRequest;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 订单服务实现类
 * <p>
 * 注意：项目存在两套订单链路——本类（Service 风格，大写状态 PENDING/CANCELLED）
 * 与 ClientOrderController（Controller 直连 Mapper 风格，小写状态 pending/cancelled）。
 * 前端实际调用的是 ClientOrderController，本类主要供管理后台及定时任务使用。
 * 为统一状态语义，cancel/cancelBySystem 已对齐为小写状态值（pending/cancelled），
 * 库存回补统一使用 ProductMapper.restoreStock 原子方法（条件 UPDATE，防并发回补过量）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO create(Long userId, OrderCreateRequest request) {
        // 校验购物车项
        if (request.getCartIds() == null || request.getCartIds().isEmpty()) {
            throw new BusinessException(400, "请选择要结算的商品");
        }

        // 查询所有购物车项
        List<Cart> cartList = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Long cartId : request.getCartIds()) {
            Cart cart = cartMapper.selectById(cartId);
            if (cart == null || !cart.getUserId().equals(userId)) {
                throw new BusinessException(400, "购物车项不存在或不属于当前用户");
            }
            cartList.add(cart);

            // 计算金额
            Product product = productMapper.selectById(cart.getProductId());
            if (product == null) {
                throw new BusinessException(400, "商品不存在");
            }
            // 校验库存
            if (product.getStock() < cart.getQuantity()) {
                throw new BusinessException(400, "商品[" + product.getName() + "]库存不足");
            }
            totalAmount = totalAmount.add(product.getPrice().multiply(new BigDecimal(cart.getQuantity())));
        }

        // 创建订单（小写状态值，与 ClientOrderController 统一）
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus("pending");
        order.setRemark(request.getRemark());
        orderMapper.insert(order);

        // 创建订单明细
        for (Cart cart : cartList) {
            Product product = productMapper.selectById(cart.getProductId());
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(cart.getProductId());
            item.setName(product.getName());
            item.setPrice(product.getPrice());
            item.setQuantity(cart.getQuantity());
            orderItemMapper.insert(item);

            // 扣减库存
            product.setStock(product.getStock() - cart.getQuantity());
            productMapper.updateById(product);
        }

        // 删除已结算的购物车项
        for (Cart cart : cartList) {
            cartMapper.deleteById(cart.getId());
        }

        log.info("创建订单成功：{}，金额：{}", order.getOrderNo(), totalAmount);
        return convertToVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO pay(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!"pending".equals(order.getStatus())) {
            throw new BusinessException(400, "订单状态不正确，无法支付");
        }
        order.setStatus("paid");
        orderMapper.updateById(order);
        log.info("订单支付成功：{}", order.getOrderNo());
        return convertToVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO cancel(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(404, "订单不存在");
        }
        // 用户取消：允许取消待支付和已支付两种状态（用户视角：支付后仍可申请取消）
        if (!"pending".equals(order.getStatus()) && !"paid".equals(order.getStatus())) {
            throw new BusinessException(400, "订单状态不正确，无法取消");
        }
        doCancelOrder(order, "用户取消");
        return convertToVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelBySystem(Long orderId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            log.warn("系统取消失败：订单不存在，orderId={}", orderId);
            return;
        }
        // 系统取消：仅允许取消"待支付"状态订单（超时未支付自动关单）
        // 已取消/已支付/已发货/已完成 等非待支付状态直接跳过（幂等）
        if (!"pending".equals(order.getStatus())) {
            log.warn("系统取消跳过：订单 {} 状态为 {}，非待支付，无需关单",
                    order.getOrderNo(), order.getStatus());
            return;
        }
        doCancelOrder(order, reason);
    }

    /**
     * 共享的订单取消私有方法：更新状态 + 原子回补库存
     * <p>
     * 用户取消（cancel）和系统取消（cancelBySystem）共用本方法，
     * 区别仅在取消前的状态校验和归属校验（由调用方负责）。
     * <p>
     * 库存回补使用 ProductMapper.restoreStock 原子方法（条件 UPDATE：stock + quantity），
     * 比 setStock 非原子操作更严谨，并发下不会回补过量。
     *
     * @param order  待取消订单（必须已查好并满足取消前置条件）
     * @param reason 取消原因（仅用于日志记录，Order 实体无 cancelReason 字段不写入数据库）
     */
    private void doCancelOrder(Order order, String reason) {
        order.setStatus("cancelled");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 原子回补库存（restoreStock 是条件 UPDATE 原子操作，防并发回补过量）
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
    public PageResult<OrderVO> listByUserId(Long userId, Long current, Long size) {
        Page<Order> page = new Page<>(current, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId)
               .orderByDesc(Order::getCreateTime);
        IPage<Order> orderPage = orderMapper.selectPage(page, wrapper);
        IPage<OrderVO> voPage = orderPage.convert(this::convertToVO);
        return PageResult.of(voPage);
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

    private String generateOrderNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "GC" + timestamp + uuid;
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