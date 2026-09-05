package com.greenchain.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 退款结果（支付通道 → 业务层）
 */
@Data
public class RefundResult {

    /** 是否成功 */
    private boolean success;

    /** 本笔退款流水号（refundNo） */
    private String refundNo;

    /** 退款交易号（mock 生成） */
    private String refundTransactionId;

    /** 退款金额 */
    private BigDecimal amount;

    /** 失败原因 */
    private String errMsg;

    public static RefundResult fail(String errMsg) {
        RefundResult r = new RefundResult();
        r.setSuccess(false);
        r.setErrMsg(errMsg);
        return r;
    }

    public static RefundResult ok(String refundNo, String refundTransactionId, BigDecimal amount) {
        RefundResult r = new RefundResult();
        r.setSuccess(true);
        r.setRefundNo(refundNo);
        r.setRefundTransactionId(refundTransactionId);
        r.setAmount(amount);
        return r;
    }
}