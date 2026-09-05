package com.greenchain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer_service_request")
public class CustomerServiceRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String phone;
    private String email;
    private String source;
    private String question;
    private String content;
    private Integer status;
    private String reply;
    private String replyBy;
    private LocalDateTime replyTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // AI 转人工新增
    private String chatSessionId;
    private String chatHistory;
    private Integer adminUnreadCount;
}
