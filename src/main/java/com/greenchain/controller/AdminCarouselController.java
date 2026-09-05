package com.greenchain.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greenchain.annotation.RequiresPermission;
import com.greenchain.common.Result;
import com.greenchain.entity.Carousel;
import com.greenchain.mapper.CarouselMapper;
import com.greenchain.util.CacheUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 管理后台 - 首页轮播图 CRUD
 * 路径：/api/admin/carousels，权限标识与 schema.sql sys_permission.id=22-25 对齐。
 * <p>
 * 写操作（新增/编辑/删除）后主动清 ClientProductController 用的 Redis 缓存键
 * cache:carousel:list，保证前台下一次 /carousel 请求能拉到最新轮播图。
 */
@RestController
@RequestMapping("/api/admin/carousels")
@RequiredArgsConstructor
public class AdminCarouselController {

    private static final String CACHE_KEY = "cache:carousel:list";
    private final CarouselMapper carouselMapper;
    private final CacheUtil cacheUtil;

    /**
     * 分页查询轮播（GET /api/admin/carousels?current=1&size=10）
     */
    @GetMapping
    @RequiresPermission("sys:carousel:list")
    public Result<IPage<Carousel>> list(@RequestParam(defaultValue = "1") Long current,
                                        @RequestParam(defaultValue = "10") Long size,
                                        @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Carousel> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Carousel::getStatus, status);
        wrapper.orderByAsc(Carousel::getSort).orderByDesc(Carousel::getId);
        return Result.success(carouselMapper.selectPage(new Page<>(current, size), wrapper));
    }

    /**
     * 新增轮播
     */
    @PostMapping
    @RequiresPermission("sys:carousel:add")
    public Result<Carousel> create(@RequestBody Carousel carousel) {
        if (carousel.getCreateTime() == null) carousel.setCreateTime(LocalDateTime.now());
        carousel.setUpdateTime(LocalDateTime.now());
        if (carousel.getStatus() == null) carousel.setStatus(1);
        if (carousel.getSort() == null) carousel.setSort(0);
        carouselMapper.insert(carousel);
        cacheUtil.delete(CACHE_KEY);
        return Result.success(carousel);
    }

    /**
     * 编辑轮播
     */
    @PutMapping("/{id}")
    @RequiresPermission("sys:carousel:edit")
    public Result<Carousel> update(@PathVariable Long id, @RequestBody Carousel carousel) {
        Carousel existed = carouselMapper.selectById(id);
        if (existed == null) return Result.error("轮播不存在");
        carousel.setId(id);
        carousel.setUpdateTime(LocalDateTime.now());
        carouselMapper.updateById(carousel);
        cacheUtil.delete(CACHE_KEY);
        return Result.success(carouselMapper.selectById(id));
    }

    /**
     * 删除轮播
     */
    @DeleteMapping("/{id}")
    @RequiresPermission("sys:carousel:delete")
    public Result<Void> delete(@PathVariable Long id) {
        carouselMapper.deleteById(id);
        cacheUtil.delete(CACHE_KEY);
        return Result.success();
    }
}
