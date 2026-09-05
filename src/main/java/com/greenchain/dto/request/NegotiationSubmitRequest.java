package com.greenchain.dto.request;

import lombok.Data;

/**
 * 提交洽谈请求DTO
 */
@Data
public class NegotiationSubmitRequest {

    /** 关联订单ID */
    private Long orderId;

    /** 洽谈主题 */
    private String title;

    /** 洽谈内容 */
    private String content;
}