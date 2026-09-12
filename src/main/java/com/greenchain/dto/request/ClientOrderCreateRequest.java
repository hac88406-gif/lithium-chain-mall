package com.greenchain.dto.request;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 客户端直接下单请求 DTO（商品直购，对应 POST /api/client/orders）
 * <p>
 * 改造背景：该接口原先使用 {@code @RequestBody Map<String, Object>} 接收参数，
 * 所有字段都靠方法体内 {@code instanceof} / 强制类型转换来取值，
 * 既无法在接口层面表达约束，也容易因类型不符抛出 ClassCastException。
 * 改用 DTO 后校验前移到参数绑定阶段，异常由 GlobalExceptionHandler 统一处理为
 * {@code {code:400, message:"..."}}，与前端 {@code createRes.code} 的判断方式保持一致。
 * <p>
 * 兼容性说明：请求体 JSON 结构与改造前完全一致，前端无需改动。
 * 其中明细里的 {@code price} 字段会被 Jackson 忽略 —— 订单金额一律按数据库现价重算，
 * 不信任前端传入的价格（见 ClientOrderController#createOrder 的 B3 修复）。
 *
 * @author green-chain
 */
@Data
public class ClientOrderCreateRequest {

    /** 购买商品明细，至少一项 */
    @NotEmpty(message = "请选择商品")
    @Valid
    private List<ClientOrderItemRequest> items;

    /** 收货地址ID；允许为空（与原逻辑保持一致，下单不强制绑定地址） */
    private Long addressId;

    /** 订单备注；数据库列 remark VARCHAR(500) */
    @Size(max = 500, message = "备注最多500字")
    private String remark;
}
