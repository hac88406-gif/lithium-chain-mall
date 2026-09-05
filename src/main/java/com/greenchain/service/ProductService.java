package com.greenchain.service;

import com.greenchain.dto.request.ProductQueryRequest;
import com.greenchain.dto.request.ProductSaveRequest;
import com.greenchain.dto.response.PageResult;
import com.greenchain.dto.response.ProductVO;
import com.greenchain.entity.Product;

/**
 * 商品服务接口
 */
public interface ProductService {

    /**
     * 分页查询商品（客户端）
     */
    PageResult<ProductVO> pageQuery(ProductQueryRequest request);

    /**
     * 根据ID查询商品
     */
    ProductVO getById(Long id);

    /**
     * 新增商品（管理员）
     */
    ProductVO add(ProductSaveRequest request);

    /**
     * 编辑商品（管理员）
     */
    ProductVO update(ProductSaveRequest request);

    /**
     * 上架/下架商品（管理员）
     */
    void updateStatus(Long id, String status);

    /**
     * 删除商品（管理员）
     */
    void delete(Long id);

    /**
     * 转换实体为VO
     */
    ProductVO convertToVO(Product product);
}