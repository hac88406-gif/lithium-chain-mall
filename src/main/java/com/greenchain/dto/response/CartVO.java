package com.greenchain.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车响应VO
 */
@Data
public class CartVO {

    private Long id;

    private Long userId;

    private Long productId;

    private String productName;

    private String productType;

    private BigDecimal productPrice;

    private String productImage;

    private Integer quantity;

    private LocalDateTime createTime;
}