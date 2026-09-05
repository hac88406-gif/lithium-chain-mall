package com.greenchain.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 异步回调通知解析结果（支付通道 → 业务层）
 * <p>
 * 业务层只需关心 {@link #valid} 是否通过验签，以及 {orderNo/amount/transactionId} 三要素。
 */
@Data
public class NotifyParseResult {

    /** 是否通过验签 */
    private boolean valid;

    /** 平台支付流水号（payment_transaction.payment_no，用于幂等） */
    private String paymentNo;

    /** 业务订单号 */
    private String orderNo;

    /** 支付金额 */
    private BigDecimal amount;

    /** 第三方交易号 */
    private String transactionId;

    /** 原始回调参数（验签通过后原样保留，便于日志/幂等） */
    private Map<String, String> rawParams;

    /** 验签失败原因 */
    private String errMsg;

    public static NotifyParseResult invalid(String errMsg) {
        NotifyParseResult r = new NotifyParseResult();
        r.setValid(false);
        r.setErrMsg(errMsg);
        return r;
    }

    public static NotifyParseResult ok(String paymentNo, String orderNo, BigDecimal amount,
                                       String transactionId, Map<String, String> rawParams) {
        NotifyParseResult r = new NotifyParseResult();
        r.setValid(true);
        r.setPaymentNo(paymentNo);
        r.setOrderNo(orderNo);
        r.setAmount(amount);
        r.setTransactionId(transactionId);
        r.setRawParams(rawParams);
        return r;
    }
}