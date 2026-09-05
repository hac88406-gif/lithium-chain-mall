package com.greenchain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 车间实体类
 */
@Data
@TableName("t_workshop")
public class Workshop {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 车间名称 */
    private String name;

    /** 车间位置 */
    private String location;

    /** 车间面积（平方米） */
    private BigDecimal area;

    /** 车间描述 */
    private String description;

    /** 3D模型文件URL */
    private String modelUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}