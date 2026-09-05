package com.greenchain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商务洽谈实体类
 */
@Data
@TableName("t_negotiation")
public class Negotiation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联订单ID */
    private Long orderId;

    /** 提交用户ID */
    private Long userId;

    /** 洽谈主题 */
    private String title;

    /** 洽谈内容 */
    private String content;

    /** 状态：IN_PROGRESS-进行中，COMPLETED-已完成，REJECTED-已拒绝 */
    private String status;

    /** 管理员回复 */
    private String reply;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}