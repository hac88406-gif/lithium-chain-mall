package com.greenchain.dto.request;

import lombok.Data;

/**
 * 购物车新增请求DTO
 */
@Data
public class CartAddRequest {

    /** 商品ID */
    private Long productId;

    /** 数量 */
    private Integer quantity = 1;
}