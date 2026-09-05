package com.greenchain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 预下单响应（支付通道 → 业务层）
 * <p>
 * 包含发起支付需要的所有信息。对于 Mock 网关，返回模拟二维码链接、签名参数。
 * 对于真实微信/支付宝，返回 prepay_id、nonce_str 等拉起所需参数。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrePayResponse {

    /** 是否成功 */
    private boolean success;

    /** 平台支付流水号（paymentNo） */
    private String paymentNo;

    /** 第三方平台交易号（transactionId，Mock 预生成） */
    private String transactionId;

    /** 支付金额 */
    private BigDecimal amount;

    /** 支付链接/二维码链接 */
    private String payUrl;

    /** 签名参数（用于验证回调，Map 包含参与签名的键值对） */
    private Map<String, String> signParams;

    /** 签名字符串 */
    private String sign;

    /** 失败原因 */
    private String errMsg;

    public static PrePayResponse fail(String errMsg) {
        PrePayResponse resp = new PrePayResponse();
        resp.setSuccess(false);
        resp.setErrMsg(errMsg);
        return resp;
    }

    public static PrePayResponse success(String paymentNo, String transactionId,
                                          BigDecimal amount, String payUrl,
                                          Map<String, String> signParams, String sign) {
        PrePayResponse resp = new PrePayResponse();
        resp.setSuccess(true);
        resp.setPaymentNo(paymentNo);
        resp.setTransactionId(transactionId);
        resp.setAmount(amount);
        resp.setPayUrl(payUrl);
        resp.setSignParams(signParams);
        resp.setSign(sign);
        return resp;
    }
}