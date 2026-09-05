package com.greenchain.dto.request;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 发起售后申请请求 DTO
 * <p>
 * 校验规则配合 Controller 的 @Valid 注解触发，异常由 GlobalExceptionHandler 统一处理。
 */
@Data
public class AfterSaleApplyRequest {

    /** 关联订单ID */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /** 售后类型 1退款 2退货 */
    @NotNull(message = "售后类型不能为空")
    @Min(value = 1, message = "售后类型只能为 1退款 或 2退货")
    @Max(value = 2, message = "售后类型只能为 1退款 或 2退货")
    private Integer afterSaleType;

    /** 申请理由 */
    @Size(max = 512, message = "申请理由最多512字")
    private String reason;

    /** 凭证图片URL（多张用逗号分隔，最多 9 张） */
    @Size(max = 2048, message = "凭证图片URL总长度不能超过2048字符")
    private String evidence;
}
