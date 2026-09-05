package com.greenchain.dto.request;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 售后审核请求 DTO（管理后台）
 * <p>
 * status 仅允许 1同意 / 2拒绝；拒绝时 rejectReason 由 Service 层校验必填。
 */
@Data
public class AfterSaleReviewRequest {

    /** 售后单ID */
    @NotNull(message = "售后单ID不能为空")
    private Long afterSaleId;

    /** 审核结果 1同意 2拒绝 */
    @NotNull(message = "审核结果不能为空")
    @Min(value = 1, message = "审核结果只能为 1同意 或 2拒绝")
    @Max(value = 2, message = "审核结果只能为 1同意 或 2拒绝")
    private Integer status;

    /** 拒绝理由（拒绝时必填，由 Service 校验） */
    @Size(max = 512, message = "拒绝理由最多512字")
    private String rejectReason;
}
