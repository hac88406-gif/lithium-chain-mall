package com.greenchain.dto.request;

import lombok.Data;

/**
 * 游客购物车合并请求 DTO
 * <p>
 * 前端 localStorage（key=guest_cart）存储格式为：
 * [{productId: 1001, quantity: 2}, {productId: 1002, quantity: 1}]
 * 登录成功后，前端将整个数组作为请求体提交到 /api/client/cart/mergeGuestCart。
 * <p>
 * 注意：仅两个字段，与 CartItem 实体的 productId/quantity 对齐，本项目无 SKU 概念。
 * userId 一律从 JWT 解析（@RequestAttribute），此 DTO 不携带 userId。
 */
@Data
public class GuestCartDTO {

    /** 商品ID（对应 product 表主键） */
    private Long productId;

    /** 数量，必须大于 0 */
    private Integer quantity;
}
