package com.greenchain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("business_cooperation")
public class BusinessCooperation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;
    private String companyName;
    private String companyAddress;
    private String contactPerson;
    private String position;
    private String phone;
    private String email;
    private String requirementType;
    private String budget;
    private String intention;
    private String deliveryCycle;
    private String mediaTitle;
    private String mediaFormat;
    private String mediaDate;
    private String remark;
    private Integer status;
    private String reply;
    private String replyBy;
    private LocalDateTime replyTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
