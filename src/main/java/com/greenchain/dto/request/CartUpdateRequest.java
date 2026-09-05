package com.greenchain.dto.request;

import lombok.Data;

/**
 * 购物车修改请求DTO
 */
@Data
public class CartUpdateRequest {

    /** 购物车项ID */
    private Long id;

    /** 新数量 */
    private Integer quantity;
}