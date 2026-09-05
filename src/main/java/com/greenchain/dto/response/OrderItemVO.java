package com.greenchain.dto.response;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 订单明细响应VO
 */
@Data
public class OrderItemVO {

    private Long id;

    private Long productId;

    private String productName;

    private BigDecimal price;

    private Integer quantity;

    /** 小计金额 */
    private BigDecimal subtotal;
}