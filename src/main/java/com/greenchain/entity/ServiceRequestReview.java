package com.greenchain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 人工客服评价表：客户在会话超时关闭（status=2）后提交评价 → 工单自动归档（status=3）。
 * 一个工单只允许一条评价（UNIQUE KEY service_request_review.uk_request_id）。
 */
@Data
@TableName("service_request_review")
public class ServiceRequestReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 customer_service_request.id */
    private Long requestId;

    /** 星级 1-5 */
    private Integer rating;

    /** 客户简短评价文字（可选，最长 500） */
    private String content;

    /** 评价提交时间 */
    private LocalDateTime createTime;
}
