package com.greenchain.service.impl;

import com.greenchain.entity.Order;
import com.greenchain.entity.OrderItem;
import com.greenchain.mapper.OrderItemMapper;
import com.greenchain.mapper.OrderMapper;
import com.greenchain.mapper.ProductMapper;
import com.greenchain.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OrderServiceImpl 单元测试
 * <p>
 * 重点测试 {@link OrderServiceImpl#cancelBySystem(Long, String)} 订单系统自动关单方法，
 * 覆盖"正常取消 + 库存回补"、"订单不存在幂等跳过"、"非待支付状态跳过"三种场景。
 * <p>
 * 纯 Mockito 单元测试，不启动 Spring 容器，速度快，只测业务逻辑。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl - 订单服务单元测试")
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    // ==================== cancelBySystem 测试 ====================

    @Nested
    @DisplayName("cancelBySystem - 系统自动关单")
    class CancelBySystem {

        private Order pendingOrder;

        @BeforeEach
        void setUp() {
            pendingOrder = new Order();
            pendingOrder.setId(1001L);
            pendingOrder.setOrderNo("GC20260903001");
            pendingOrder.setStatus("pending");
        }

        @Test
        @DisplayName("正常取消待支付订单：更新状态 + 回补库存")
        void should_cancel_pending_order_and_restore_stock() {
            // given：模拟一个待支付订单 + 两条明细
            OrderItem item1 = new OrderItem();
            item1.setProductId(10L);
            item1.setQuantity(2);
            OrderItem item2 = new OrderItem();
            item2.setProductId(20L);
            item2.setQuantity(1);
            List<OrderItem> items = List.of(item1, item2);

            when(orderMapper.selectById(1001L)).thenReturn(pendingOrder);
            when(orderItemMapper.selectList(any())).thenReturn(items);
            // restoreStock 返回 1 表示原子回补成功
            when(productMapper.restoreStock(10L, 2)).thenReturn(1);
            when(productMapper.restoreStock(20L, 1)).thenReturn(1);

            // when
            orderService.cancelBySystem(1001L, "超时未支付自动关单");

            // then：状态被更新为 cancelled
            verify(orderMapper, times(1)).updateById(argThat(order ->
                    "cancelled".equals(order.getStatus()) && order.getId() == 1001L));

            // 两条明细的库存都被回补
            verify(productMapper, times(1)).restoreStock(10L, 2);
            verify(productMapper, times(1)).restoreStock(20L, 1);
        }

        @Test
        @DisplayName("订单不存在：幂等跳过，不抛异常")
        void should_skip_when_order_not_found() {
            // given
            when(orderMapper.selectById(9999L)).thenReturn(null);

            // when：不能抛异常，正常返回
            orderService.cancelBySystem(9999L, "任何原因");

            // then：不会调 updateById，不会查订单明细，不会回补库存
            verify(orderMapper, never()).updateById(any());
            verify(orderItemMapper, never()).selectList(any());
            verify(productMapper, never()).restoreStock(anyLong(), anyInt());
        }

        @Test
        @DisplayName("非待支付状态订单：跳过不处理（幂等）")
        void should_skip_when_order_already_paid() {
            // given：已支付订单
            Order paidOrder = new Order();
            paidOrder.setId(1002L);
            paidOrder.setOrderNo("GC20260903002");
            paidOrder.setStatus("paid");
            when(orderMapper.selectById(1002L)).thenReturn(paidOrder);

            // when
            orderService.cancelBySystem(1002L, "超时未支付");

            // then：不会触发取消逻辑
            verify(orderMapper, never()).updateById(any());
            verify(productMapper, never()).restoreStock(anyLong(), anyInt());
        }

        @Test
        @DisplayName("订单已取消状态：跳过（幂等）")
        void should_skip_when_order_already_cancelled() {
            // given
            Order cancelledOrder = new Order();
            cancelledOrder.setId(1003L);
            cancelledOrder.setOrderNo("GC20260903003");
            cancelledOrder.setStatus("cancelled");
            when(orderMapper.selectById(1003L)).thenReturn(cancelledOrder);

            // when
            orderService.cancelBySystem(1003L, "重复关单不应报错");

            // then
            verify(orderMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("订单无明细：只更新状态，不回补库存")
        void should_cancel_without_restoring_when_no_items() {
            // given
            when(orderMapper.selectById(1001L)).thenReturn(pendingOrder);
            when(orderItemMapper.selectList(any())).thenReturn(new ArrayList<>());

            // when
            orderService.cancelBySystem(1001L, "超时未支付");

            // then：状态更新了但没调 restoreStock
            verify(orderMapper, times(1)).updateById(any());
            verify(productMapper, never()).restoreStock(anyLong(), anyInt());
        }
    }
}
