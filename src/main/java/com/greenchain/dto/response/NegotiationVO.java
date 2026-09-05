package com.greenchain.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商务洽谈响应VO
 */
@Data
public class NegotiationVO {

    private Long id;

    private Long orderId;

    private String orderNo;

    private Long userId;

    private String username;

    private String title;

    private String content;

    private String status;

    private String statusName;

    private String reply;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}