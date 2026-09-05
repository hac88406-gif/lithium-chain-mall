package com.greenchain.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单响应VO
 */
@Data
public class OrderVO {

    private Long id;

    private String orderNo;

    private Long userId;

    private String username;

    private BigDecimal totalAmount;

    private String status;

    private String statusName;

    private String remark;

    private LocalDateTime createTime;

    /** 订单明细列表 */
    private List<OrderItemVO> items;
}