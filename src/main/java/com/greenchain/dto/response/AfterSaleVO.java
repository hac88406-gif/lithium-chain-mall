package com.greenchain.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 售后视图对象（管理后台列表/详情、买家列表/详情复用）
 * 在 AfterSale 基础上附加订单号、用户信息，便于展示。
 */
@Data
public class AfterSaleVO {

    /** 售后单ID */
    private Long id;

    /** 关联订单ID */
    private Long orderId;

    /** 用户ID */
    private Long userId;

    /** 售后类型 1退款 2退货 */
    private Integer afterSaleType;

    /** 申请理由 */
    private String reason;

    /** 凭证图片（逗号分隔URL） */
    private String evidence;

    /** 状态 0待审核 1同意 2拒绝 */
    private Integer status;

    /** 拒绝理由 */
    private String rejectReason;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 订单号（关联 order 表） */
    private String orderNo;

    /** 买家用户名 */
    private String username;

    /** 买家昵称 */
    private String nickname;
}
