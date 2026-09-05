package com.greenchain.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greenchain.annotation.RequiresPermission;
import com.greenchain.common.Result;
import com.greenchain.entity.News;
import com.greenchain.mapper.NewsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理后台 - 资讯/新闻 CRUD
 * 路径前缀 /api/admin/news，挂在 AdminAuthInterceptor → PermissionInterceptor 链下，
 * 按钮级权限：sys:news:list/add/edit/delete（与 schema.sql sys_permission.id=18-21 对齐）。
 */
@RestController
@RequestMapping("/api/admin/news")
@RequiredArgsConstructor
public class AdminNewsController {

    private final NewsMapper newsMapper;

    /**
     * 分页查询资讯列表（GET /api/admin/news?current=1&size=10&keyword=...&category=...&status=1）
     */
    @GetMapping
    @RequiresPermission("sys:news:list")
    public Result<IPage<News>> list(@RequestParam(defaultValue = "1") Long current,
                                    @RequestParam(defaultValue = "10") Long size,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) String category,
                                    @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<News> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(News::getTitle, keyword.trim())
                              .or()
                              .like(News::getSummary, keyword.trim()));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(News::getCategory, category);
        }
        if (status != null) {
            wrapper.eq(News::getStatus, status);
        }
        wrapper.orderByDesc(News::getSort).orderByDesc(News::getCreateTime).orderByDesc(News::getId);
        IPage<News> page = newsMapper.selectPage(new Page<>(current, size), wrapper);
        return Result.success(page);
    }

    /**
     * 新增资讯（POST /api/admin/news）
     */
    @PostMapping
    @RequiresPermission("sys:news:add")
    public Result<News> create(@RequestBody News news) {
        if (news.getCreateTime() == null) news.setCreateTime(LocalDateTime.now());
        news.setUpdateTime(LocalDateTime.now());
        if (news.getStatus() == null) news.setStatus(1);
        if (news.getViewCount() == null) news.setViewCount(0);
        if (news.getSort() == null) news.setSort(0);
        newsMapper.insert(news);
        return Result.success(news);
    }

    /**
     * 编辑资讯（PUT /api/admin/news/{id}）
     */
    @PutMapping("/{id}")
    @RequiresPermission("sys:news:edit")
    public Result<News> update(@PathVariable Long id, @RequestBody News news) {
        News existed = newsMapper.selectById(id);
        if (existed == null) {
            return Result.error("资讯不存在");
        }
        news.setId(id);
        news.setUpdateTime(LocalDateTime.now());
        newsMapper.updateById(news);
        return Result.success(newsMapper.selectById(id));
    }

    /**
     * 删除资讯（DELETE /api/admin/news/{id}）
     */
    @DeleteMapping("/{id}")
    @RequiresPermission("sys:news:delete")
    public Result<Void> delete(@PathVariable Long id) {
        newsMapper.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除（DELETE /api/admin/news/batch?ids=1,2,3）
     */
    @DeleteMapping("/batch")
    @RequiresPermission("sys:news:delete")
    public Result<Void> deleteBatch(@RequestParam String ids) {
        for (String s : ids.split(",")) {
            if (StringUtils.hasText(s)) newsMapper.deleteById(Long.parseLong(s.trim()));
        }
        return Result.success();
    }
}
