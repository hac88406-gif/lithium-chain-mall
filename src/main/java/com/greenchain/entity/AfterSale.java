package com.greenchain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 售后申请实体
 * <p>
 * 仅做业务状态流转（待审核 -> 同意/拒绝），不做真实资金退款、不做库存回补。
 */
@Data
@TableName("after_sale")
public class AfterSale {

    /** 售后类型：退款 */
    public static final int TYPE_REFUND = 1;
    /** 售后类型：退货 */
    public static final int TYPE_RETURN = 2;

    /** 售后状态：待审核 */
    public static final int STATUS_PENDING = 0;
    /** 售后状态：同意 */
    public static final int STATUS_APPROVED = 1;
    /** 售后状态：拒绝 */
    public static final int STATUS_REJECTED = 2;

    /** 售后单ID */
    @TableId(type = IdType.AUTO)
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
}
