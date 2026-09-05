package com.greenchain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 退款请求参数（业务层 → 支付通道）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    /** 原支付流水号（paymentNo，用于关联原支付单） */
    private String origPaymentNo;

    /** 原支付交易号（transactionId） */
    private String origTransactionId;

    /** 本笔退款的流水号（refundNo） */
    private String refundNo;

    /** 退款金额（元） */
    private BigDecimal amount;

    /** 退款原因 */
    private String reason;
}