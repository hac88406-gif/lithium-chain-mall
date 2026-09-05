package com.greenchain.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greenchain.common.Result;
import com.greenchain.entity.Category;
import com.greenchain.mapper.CategoryMapper;
import com.greenchain.util.CacheUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 管理端商品分类管理
 * <p>
 * 客户端分类列表走 ClientProductController#getCategories 接口（带 Redis 缓存 cache:category:list）。
 * 管理端任何增/改/删操作都要清掉该缓存，确保客户端下次访问拿到最新数据。
 */
@RestController
@RequestMapping("/api/admin/category")
@RequiredArgsConstructor
public class AdminCategoryController {

    private static final String CACHE_KEY = "cache:category:list";

    private final CategoryMapper categoryMapper;
    private final CacheUtil cacheUtil;

    /**
     * 分类列表（分页）。管理端页面 table 数据源。
     */
    @GetMapping("/page")
    public Result<Page<Category>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Category::getName, keyword);
        }
        wrapper.orderByAsc(Category::getSort, Category::getId);
        Page<Category> result = categoryMapper.selectPage(new Page<>(page, size), wrapper);
        return Result.success(result);
    }

    /**
     * 新增分类。
     */
    @PostMapping
    public Result<Category> add(@RequestBody Map<String, Object> body) {
        Category cat = new Category();
        cat.setName((String) body.get("name"));
        Object sort = body.get("sort");
        cat.setSort(sort == null ? 0 : ((Number) sort).intValue());
        Object parentId = body.get("parentId");
        cat.setParentId(parentId == null ? 0L : ((Number) parentId).longValue());
        Object status = body.get("status");
        cat.setStatus(status == null ? 1 : ((Number) status).intValue());
        cat.setCreateTime(LocalDateTime.now());
        cat.setUpdateTime(LocalDateTime.now());
        categoryMapper.insert(cat);
        cacheUtil.delete(CACHE_KEY);
        return Result.success(cat);
    }

    /**
     * 修改分类。
     */
    @PutMapping("/{id}")
    public Result<Category> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Category cat = categoryMapper.selectById(id);
        if (cat == null) {
            return Result.error(400, "分类不存在");
        }
        if (body.containsKey("name")) cat.setName((String) body.get("name"));
        if (body.containsKey("sort")) {
            Object sort = body.get("sort");
            cat.setSort(sort == null ? 0 : ((Number) sort).intValue());
        }
        if (body.containsKey("status")) {
            Object status = body.get("status");
            cat.setStatus(status == null ? 1 : ((Number) status).intValue());
        }
        if (body.containsKey("parentId")) {
            Object pid = body.get("parentId");
            cat.setParentId(pid == null ? 0L : ((Number) pid).longValue());
        }
        cat.setUpdateTime(LocalDateTime.now());
        categoryMapper.updateById(cat);
        cacheUtil.delete(CACHE_KEY);
        return Result.success(cat);
    }

    /**
     * 删除分类。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryMapper.deleteById(id);
        cacheUtil.delete(CACHE_KEY);
        return Result.success();
    }
}
