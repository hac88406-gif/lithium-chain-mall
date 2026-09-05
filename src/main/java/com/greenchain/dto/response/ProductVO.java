package com.greenchain.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品响应VO
 */
@Data
public class ProductVO {

    private Long id;

    private String name;

    /** 分类ID */
    private Long categoryId;

    /** 分类名称（列表页直接显示，不用前端再查一次） */
    private String categoryName;

    /** 销量（统计/热销榜用） */
    private Integer sales;

    private BigDecimal price;

    private Integer stock;

    private String description;

    /** 状态值："1" / "0" */
    private String status;

    /** 状态中文名：上架 / 下架 */
    private String statusName;

    private String imageUrl;

    private LocalDateTime createTime;
}