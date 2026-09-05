package com.greenchain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 预下单请求参数（业务层 → 支付通道）
 * <p>
 * 由业务层根据订单信息组装，交给 {@code PaymentChannel.prePay}。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrePayRequest {

    /** 平台支付流水号（paymentNo，业务层生成，参与签名/回调幂等） */
    private String paymentNo;

    /** 内部订单号（order_order_no） */
    private String orderNo;

    /** 订单ID */
    private Long orderId;

    /** 支付金额（元） */
    private BigDecimal amount;

    /** 商品/订单标题（用于展示） */
    private String subject;
}