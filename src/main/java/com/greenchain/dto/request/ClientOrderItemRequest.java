package com.greenchain.dto.request;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 客户端下单请求中的商品明细项
 * <p>
 * 校验规则配合 {@link ClientOrderCreateRequest} 上的 {@code @Valid} 级联触发。
 *
 * @author green-chain
 */
@Data
public class ClientOrderItemRequest {

    /** 商品ID */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 购买数量
     * <p>
     * 原实现缺省为 1、非法值退化为 1，但"用户没填数量却下单成功"本身是隐患，
     * 故此处改为显式必填。已核对前端（Checkout.vue）下单时始终传入 quantity。
     */
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于0")
    @Max(value = 9999, message = "单个商品购买数量不能超过9999")
    private Integer quantity;
}
