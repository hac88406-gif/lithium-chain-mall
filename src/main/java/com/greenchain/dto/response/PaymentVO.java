package com.greenchain.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付相关视图对象（返回给前端）
 */
@Data
public class PaymentVO {

    /** 订单ID */
    private Long orderId;

    /** 订单号 */
    private String orderNo;

    /** 平台支付流水号 */
    private String paymentNo;

    /** 支付金额（元） */
    private BigDecimal amount;

    /** 支付链接/二维码（Mock 网关） */
    private String payUrl;

    /** 支付通道标识 */
    private String channel;

    /** 支付流水状态 */
    private String status;
}