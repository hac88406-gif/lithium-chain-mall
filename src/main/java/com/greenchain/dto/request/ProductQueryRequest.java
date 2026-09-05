package com.greenchain.dto.request;

import lombok.Data;

/**
 * 商品查询请求DTO（分页+筛选）
 */
@Data
public class ProductQueryRequest {

    /** 商品名称（模糊查询） */
    private String name;

    /** 分类ID（筛选指定分类商品） */
    private Long categoryId;

    /** 状态：1-上架 / 0-下架（管理员不传则查全部，不默认只查上架） */
    private String status;

    /** 当前页码 */
    private Long current = 1L;

    /** 每页大小 */
    private Long size = 10L;
}