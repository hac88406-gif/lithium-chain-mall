package com.greenchain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("service_request_message")
public class ServiceRequestMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long requestId;
    /** customer | admin */
    private String senderType;
    private String content;
    private LocalDateTime createTime;
    /** 0=未读 1=已读 */
    private Integer isRead;
}
