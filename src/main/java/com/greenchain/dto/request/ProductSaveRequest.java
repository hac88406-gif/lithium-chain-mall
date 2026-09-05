package com.greenchain.dto.request;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 商品新增/编辑请求DTO
 */
@Data
public class ProductSaveRequest {

    /** 商品ID（编辑时必填，新增时为空） */
    private Long id;

    /** 商品名称 */
    private String name;

    /** 分类ID（product.category_id 外键关联 category.id） */
    private Long categoryId;

    /** 状态：1-上架，0-下架（新增时不传默认上架 1） */
    private Integer status;

    /** 单价 */
    private BigDecimal price;

    /** 库存数量 */
    private Integer stock;

    /** 商品描述 */
    private String description;

    /** 商品图片URL（来自 admin/upload/image 返回值） */
    private String imageUrl;
}