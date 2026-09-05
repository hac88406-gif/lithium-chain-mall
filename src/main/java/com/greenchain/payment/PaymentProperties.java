package com.greenchain.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 支付相关配置：从 application.yml 读取。
 * <pre>
 * greenchain:
 *   payment:
 *     default-channel: mock          # 当前启用的支付通道（对应 PaymentChannel 实现）
 *     mock:
 *       secret-key: greenchain-mock-secret-for-local-demo   # Mock 网关签名密钥
 * </pre>
 */
@Component
public class PaymentProperties {

    /** 当前启用的支付通道标识（须与某个 {@link PaymentChannel#channel()} 一致） */
    @Value("${greenchain.payment.default-channel:mock}")
    private String defaultChannel;

    /** Mock 支付网关签名密钥 */
    @Value("${greenchain.payment.mock.secret-key:greenchain-mock-secret-for-local-demo}")
    private String mockSecretKey;

    public String getDefaultChannel() {
        return defaultChannel;
    }

    public String getMockSecretKey() {
        return mockSecretKey;
    }
}