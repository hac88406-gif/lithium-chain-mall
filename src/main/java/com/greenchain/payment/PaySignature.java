package com.greenchain.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 支付签名工具类
 * <p>
 * 高仿真微信/支付宝的签名规则：
 * 1. 把参与签名的参数按 key 的字典序升序排列；
 * 2. 拼接成 {@code key1=value1&key2=value2&...}（签名字段本身、空值、sign 字段剔除）；
 * 3. 用 secretKey 作为 HMAC 密钥对拼接串做 {@code HMAC-SHA256}，输出小写十六进制作为签名。
 *
 * <p>签名与验签共用同一套规则：服务端拿到回调参数后按同样规则重新计算签名，比对入参 sign，
 * 一致才认为回调来自可信网关，否则拒绝处理（防伪造回调）。
 */
public final class PaySignature {

    private PaySignature() {}

    /** 参数字典序排序时忽略的字段：sign 本身不参与签名 */
    private static final Set<String> IGNORE_FIELDS =
            Set.of("sign", "sign_type", "signType");

    /**
     * 计算参数签名（待签字段剔除 sign 本身）。
     *
     * @param params    参与签名的参数（key-value 均为字符串）
     * @param secretKey HMAC 密钥
     * @return 小写十六进制 HMAC-SHA256 签名
     */
    public static String sign(Map<String, String> params, String secretKey) {
        return hmacSha256Hex(buildSignString(params), secretKey);
    }

    /**
     * 校验回调签名：按住参数字典序重算并比对。
     *
     * @param params    回调参数（含 sign）
     * @param sign      回调携带的签名
     * @param secretKey HMAC 密钥
     * @return true-签名合法
     */
    public static boolean verify(Map<String, String> params, String sign, String secretKey) {
        if (params == null || sign == null || sign.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            return false;
        }
        String expected = sign(params, secretKey);
        // 恒时间比较，避免时序侧信道
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                sign.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 构建参与签名的待签字符串：剔除 sign/sign_type 空值后，按 key 字典序升序拼接。
     */
    public static String buildSignString(Map<String, String> params) {
        // TreeMap 天然按 key 字典序升序排序
        Map<String, String> sorted = new TreeMap<>();
        if (params != null) {
            for (Map.Entry<String, String> e : params.entrySet()) {
                if (e.getKey() == null || e.getValue() == null || e.getValue().isEmpty()) {
                    continue;
                }
                if (IGNORE_FIELDS.contains(e.getKey())) {
                    continue;
                }
                sorted.put(e.getKey(), e.getValue());
            }
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    /**
     * HMAC-SHA256 签名，输出小写十六进制字符串。
     */
    private static String hmacSha256Hex(String data, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : raw) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 签名失败", e);
        }
    }
}