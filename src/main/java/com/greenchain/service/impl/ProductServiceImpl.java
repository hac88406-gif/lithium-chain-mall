package com.greenchain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greenchain.common.BusinessException;
import com.greenchain.dto.request.ProductQueryRequest;
import com.greenchain.dto.request.ProductSaveRequest;
import com.greenchain.dto.response.PageResult;
import com.greenchain.dto.response.ProductVO;
import com.greenchain.entity.Category;
import com.greenchain.entity.Product;
import com.greenchain.mapper.CategoryMapper;
import com.greenchain.mapper.ProductMapper;
import com.greenchain.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;

    @Override
    public PageResult<ProductVO> pageQuery(ProductQueryRequest request) {
        Page<Product> page = new Page<>(request.getCurrent(), request.getSize());
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(request.getName())) {
            wrapper.like(Product::getName, request.getName());
        }
        if (request.getCategoryId() != null) {
            wrapper.eq(Product::getCategoryId, request.getCategoryId());
        }
        if (StringUtils.hasText(request.getStatus())) {
            // 管理端传 "1"/"0" 精确过滤；不传则查全部，不默认只查上架
            wrapper.eq(Product::getStatus, Integer.parseInt(request.getStatus()));
        }
        // 管理后台商品列表固定按 ID 升序（从上到下 1、2、3…），符合后台管理习惯
        wrapper.orderByAsc(Product::getId);

        IPage<Product> productPage = productMapper.selectPage(page, wrapper);

        IPage<ProductVO> voPage = productPage.convert(this::convertToVO);
        // 批量补 categoryName，避免 N+1 查库
        enrichCategoryNames(voPage.getRecords());
        return PageResult.of(voPage);
    }

    @Override
    public ProductVO getById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        ProductVO vo = convertToVO(product);
        enrichCategoryNames(java.util.Collections.singletonList(vo));
        return vo;
    }

    @Override
    public ProductVO add(ProductSaveRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setCategoryId(request.getCategoryId());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock() != null ? request.getStock() : 0);
        product.setDescription(request.getDescription());
        product.setImage(request.getImageUrl());
        product.setSales(0);
        product.setStatus(request.getStatus() != null ? request.getStatus() : 1);

        productMapper.insert(product);
        log.info("新增商品：{}，分类ID={}", product.getName(), product.getCategoryId());
        ProductVO vo = convertToVO(product);
        enrichCategoryNames(java.util.Collections.singletonList(vo));
        return vo;
    }

    @Override
    public ProductVO update(ProductSaveRequest request) {
        if (request.getId() == null) {
            throw new BusinessException(400, "商品ID不能为空");
        }
        Product product = productMapper.selectById(request.getId());
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }

        product.setName(request.getName());
        product.setCategoryId(request.getCategoryId());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock() != null ? request.getStock() : product.getStock());
        product.setDescription(request.getDescription());
        if (StringUtils.hasText(request.getImageUrl())) {
            product.setImage(request.getImageUrl());
        }
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }

        productMapper.updateById(product);
        log.info("编辑商品：{}，分类ID={}", product.getName(), product.getCategoryId());
        ProductVO vo = convertToVO(product);
        enrichCategoryNames(java.util.Collections.singletonList(vo));
        return vo;
    }

    @Override
    public void updateStatus(Long id, String status) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        // 兼容 "ON_SHELF"/"OFF_SHELF" 字符串和 "1"/"0" 字符串
        int statusValue;
        if ("ON_SHELF".equalsIgnoreCase(status)) {
            statusValue = 1;
        } else if ("OFF_SHELF".equalsIgnoreCase(status)) {
            statusValue = 0;
        } else {
            statusValue = Integer.parseInt(status);
        }
        product.setStatus(statusValue);
        productMapper.updateById(product);
        log.info("商品状态更新：{} -> {}", product.getName(), statusValue);
    }

    @Override
    public void delete(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        productMapper.deleteById(id);
        log.info("删除商品：{}", product.getName());
    }

    @Override
    public ProductVO convertToVO(Product product) {
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setCategoryId(product.getCategoryId());
        vo.setPrice(product.getPrice());
        vo.setStock(product.getStock());
        vo.setSales(product.getSales() != null ? product.getSales() : 0);
        vo.setDescription(product.getDescription());
        vo.setStatus(String.valueOf(product.getStatus()));
        vo.setStatusName(getStatusName(product.getStatus()));
        vo.setImageUrl(product.getImage());
        vo.setCreateTime(product.getCreateTime());
        return vo;
    }

    /**
     * 把 ProductVO 列表里的 categoryId 一次性查出名称补齐
     */
    private void enrichCategoryNames(List<ProductVO> list) {
        if (list == null || list.isEmpty()) return;
        List<Long> ids = list.stream()
                .map(ProductVO::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) return;
        List<Category> categories = categoryMapper.selectBatchIds(ids);
        Map<Long, String> nameMap = new HashMap<>();
        for (Category c : categories) {
            nameMap.put(c.getId(), c.getName());
        }
        for (ProductVO vo : list) {
            if (vo.getCategoryId() != null) {
                vo.setCategoryName(nameMap.getOrDefault(vo.getCategoryId(), "未分类"));
            } else {
                vo.setCategoryName("未分类");
            }
        }
    }

    private String getStatusName(Integer status) {
        if (status != null && status == 1) return "上架";
        if (status != null && status == 0) return "下架";
        return "未知";
    }
}