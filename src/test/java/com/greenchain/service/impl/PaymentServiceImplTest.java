package com.greenchain.service.impl;

import com.greenchain.entity.PaymentTransaction;
import com.greenchain.mapper.OrderItemMapper;
import com.greenchain.mapper.OrderMapper;
import com.greenchain.mapper.PaymentTransactionMapper;
import com.greenchain.mapper.ProductMapper;
import com.greenchain.payment.PaymentChannel;
import com.greenchain.payment.PaymentProperties;
import com.greenchain.payment.dto.NotifyParseResult;
import com.greenchain.util.CacheUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * PaymentServiceImpl 支付回调单元测试
 * <p>
 * 覆盖回调链路的四道关卡，证明"重复回调不重复改单、异常回调不改单"：
 * <ol>
 *   <li>验签失败 → FAIL，不触碰数据库；</li>
 *   <li>金额与本地流水不一致 → FAIL，不给改单（防改价）；</li>
 *   <li>正常回调 → 流水置成功 + 订单置已支付 + 累加销量；</li>
 *   <li>重复回调（条件 UPDATE 影响行数=0）→ 直接回执 SUCCESS，不重复改单。</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl - 支付回调单元测试")
class PaymentServiceImplTest {

    @Mock
    private PaymentTransactionMapper paymentTransactionMapper;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private CacheUtil cacheUtil;
    @Mock
    private PaymentChannel channel;
    @Mock
    private PaymentProperties properties;

    private PaymentServiceImpl paymentService;

    private Map<String, String> params;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(paymentTransactionMapper, orderMapper, orderItemMapper,
                productMapper, cacheUtil, List.of(channel), properties);
        // 策略选路：配置的 default-channel 必须与注入通道的 channel() 一致
        when(properties.getDefaultChannel()).thenReturn("mock");
        when(channel.channel()).thenReturn("mock");

        params = new LinkedHashMap<>();
        params.put("mch_id", "10000001");
        params.put("out_trade_no", "GC20261020001");
        params.put("payment_no", "pay001");
        params.put("transaction_id", "txn001");
        params.put("total_fee", "200");
    }

    @Test
    @DisplayName("验签失败：返回 FAIL，不写任何数据")
    void should_reject_when_signature_invalid() {
        when(channel.parseNotify(any(), any())).thenReturn(NotifyParseResult.invalid("签名校验失败"));

        assertEquals("FAIL", paymentService.handleNotify(params, "bad-sign"));

        verifyNoInteractions(paymentTransactionMapper);
    }

    @Test
    @DisplayName("回调金额与本地流水不一致：返回 FAIL，不改单（防改价）")
    void should_reject_when_amount_mismatch() {
        when(channel.parseNotify(any(), any()))
                .thenReturn(NotifyParseResult.ok("pay001", "GC20261020001",
                        new BigDecimal("1.00"), "txn001", params));
        when(paymentTransactionMapper.selectOne(any())).thenReturn(localTx(new BigDecimal("2.00")));

        assertEquals("FAIL", paymentService.handleNotify(params, "sign"));

        verify(paymentTransactionMapper, never()).markPaySuccess(anyString(), anyString());
        verify(orderMapper, never()).markPaidByOrderNo(anyString());
    }

    @Test
    @DisplayName("回调订单号与本地流水不一致：返回 FAIL，不放行")
    void should_reject_when_order_no_mismatch() {
        when(channel.parseNotify(any(), any()))
                .thenReturn(NotifyParseResult.ok("pay001", "GC-OTHER",
                        new BigDecimal("2.00"), "txn001", params));
        when(paymentTransactionMapper.selectOne(any())).thenReturn(localTx(new BigDecimal("2.00")));

        assertEquals("FAIL", paymentService.handleNotify(params, "sign"));

        verify(paymentTransactionMapper, never()).markPaySuccess(anyString(), anyString());
    }

    @Test
    @DisplayName("正常回调：流水置成功 + 订单置已支付")
    void should_mark_paid_on_valid_notify() {
        when(channel.parseNotify(any(), any()))
                .thenReturn(NotifyParseResult.ok("pay001", "GC20261020001",
                        new BigDecimal("2.00"), "txn001", params));
        when(paymentTransactionMapper.selectOne(any())).thenReturn(localTx(new BigDecimal("2.00")));
        when(paymentTransactionMapper.markPaySuccess("pay001", "txn001")).thenReturn(1);
        when(orderMapper.markPaidByOrderNo("GC20261020001")).thenReturn(1);
        // 订单查不到 → 销量累加内部直接 return，测试只关心状态机
        when(orderMapper.selectOne(any())).thenReturn(null);

        assertEquals("SUCCESS", paymentService.handleNotify(params, "sign"));

        verify(paymentTransactionMapper).markPaySuccess("pay001", "txn001");
        verify(orderMapper).markPaidByOrderNo("GC20261020001");
    }

    @Test
    @DisplayName("重复回调：条件 UPDATE 影响行数=0，回执 SUCCESS 且不重复改单")
    void should_ignore_duplicated_notify() {
        when(channel.parseNotify(any(), any()))
                .thenReturn(NotifyParseResult.ok("pay001", "GC20261020001",
                        new BigDecimal("2.00"), "txn001", params));
        when(paymentTransactionMapper.selectOne(any())).thenReturn(localTx(new BigDecimal("2.00")));
        // 已是 success 状态：WHERE status='created' 不再命中
        when(paymentTransactionMapper.markPaySuccess("pay001", "txn001")).thenReturn(0);

        assertEquals("SUCCESS", paymentService.handleNotify(params, "sign"));

        verify(orderMapper, never()).markPaidByOrderNo(anyString());
        verify(orderMapper, never()).selectOne(any());
    }

    private PaymentTransaction localTx(BigDecimal amount) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setId(1L);
        tx.setOrderId(100L);
        tx.setOrderNo("GC20261020001");
        tx.setUserId(1L);
        tx.setPaymentNo("pay001");
        tx.setChannel("mock");
        tx.setType(PaymentTransaction.TYPE_PAYMENT);
        tx.setStatus(PaymentTransaction.STATUS_CREATED);
        tx.setAmount(amount);
        return tx;
    }
}
