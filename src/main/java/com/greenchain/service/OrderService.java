package com.greenchain.service;

import com.greenchain.dto.request.OrderCreateRequest;
import com.greenchain.dto.response.OrderVO;
import com.greenchain.dto.response.PageResult;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 创建订单（从购物车结算）
     */
    OrderVO create(Long userId, OrderCreateRequest request);

    /**
     * 支付订单（简化：直接标记为已支付）
     */
    OrderVO pay(Long userId, Long orderId);

    /**
     * 取消订单
     */
    OrderVO cancel(Long userId, Long orderId);

    /**
     * 系统取消订单（定时任务超时关单使用）
     * <p>
     * 与 cancel(userId, orderId) 共用相同的库存回补和状态更新逻辑，
     * 区别在于：不做 userId 归属校验（系统操作）、仅允许取消"待支付"状态订单、
     * 允许传入取消原因（如"超时未支付"）。
     * <p>
     * 幂等保障：已取消/已支付等非待支付状态的订单直接跳过并打印 warn 日志，不抛异常。
     *
     * @param orderId 要取消的订单ID
     * @param reason  取消原因（如"超时未支付自动关单"），用于日志记录；Order 实体无 cancelReason 字段，不写入数据库
     */
    void cancelBySystem(Long orderId, String reason);

    /**
     * 查询用户订单列表（客户端）
     */
    PageResult<OrderVO> listByUserId(Long userId, Long current, Long size);

    /**
     * 查询订单详情
     */
    OrderVO getById(Long orderId);

    /**
     * 查询所有订单列表（管理员）
     */
    PageResult<OrderVO> listAll(Long current, Long size, String status);
}