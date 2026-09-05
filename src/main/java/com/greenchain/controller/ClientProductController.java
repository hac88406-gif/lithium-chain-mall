package com.greenchain.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greenchain.entity.Product;
import com.greenchain.entity.Carousel;
import com.greenchain.entity.Category;
import com.greenchain.mapper.ProductMapper;
import com.greenchain.mapper.CarouselMapper;
import com.greenchain.mapper.CategoryMapper;
import com.greenchain.util.CacheUtil;
import com.greenchain.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/client")
public class ClientProductController {

    /**
     * 首页轮播图列表缓存键
     */
    private static final String CACHE_KEY_CAROUSEL_LIST = "cache:carousel:list";

    /**
     * 商品分类列表缓存键
     */
    private static final String CACHE_KEY_CATEGORY_LIST = "cache:category:list";

    /** Redis ZSet 热销榜 key：rank:product:sales，value=商品ID，score=累计销量 */
    private static final String RANK_KEY_PRODUCT_SALES = "rank:product:sales";

    /** Redis ZSet 用户浏览足迹 key 前缀：footprint:user:{userId}，value=商品ID，score=时间戳 */
    private static final String FOOTPRINT_KEY_PREFIX = "footprint:user:";

    /** 足迹最多保留 20 条（按时间倒序） */
    private static final int FOOTPRINT_MAX_SIZE = 20;

    /** 足迹过期时间（天） */
    private static final long FOOTPRINT_TTL_DAYS = 30L;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CarouselMapper carouselMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 缓存工具类（get/set/delete）
     */
    @Autowired
    private CacheUtil cacheUtil;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 从请求头提取已登录用户 ID（公开接口不强制鉴权：有合法 token 就解析，没有就返回 null）。
     * 解决：/products/{id} 等在 ClientAuthInterceptor 白名单里的接口，
     *       即使前端带上 Authorization 头也不会被拦截器 setAttribute("userId")，
     *       需要 Controller 自行解析 request.getHeader("Authorization")。
     */
    private Long resolveLoginUserId(HttpServletRequest request) {
        try {
            Object attr = request.getAttribute("userId");
            if (attr instanceof Number) {
                return ((Number) attr).longValue();
            }
            String header = request.getHeader("Authorization");
            if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
                return null;
            }
            String token = header.substring(7);
            if (!jwtUtil.validateToken(token)) {
                return null;
            }
            String uid = jwtUtil.getUserIdFromToken(token);
            return uid == null ? null : Long.parseLong(uid);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 商品列表（P0-4 修复：MyBatis-Plus 真分页替换"全表查+内存过滤"性能炸弹）
     * <p>
     * 原实现 L103 selectList(null) 每次全表扫描 → Java 层过滤/排序/subList 分页，
     * 商品上 1000+ 后 DB/堆内存/GC 全面爆炸；
     * 新实现使用 LambdaQueryWrapper + Page，WHERE/ORDER BY/LIMIT/OFFSET 全部下推到 MySQL，
     * 一次请求只取 page×size 条记录，同时通过 count(1) 得到总记录数，
     * 返回 data.total / data.pages / data.records 三段结构，前端分页组件更方便直接用。
     */
    @GetMapping("/products")
    public Map<String, Object> getProducts(@RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer size,
                                           @RequestParam(required = false) Long categoryId,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String sort) {
        Map<String, Object> result = new java.util.HashMap<>();

        // 1. 构建 Wrapper（条件全部推到 WHERE 层）
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        // 默认只展示上架商品：符合前台商城语义（与商品管理页显式查 status=1 一致）
        wrapper.eq(Product::getStatus, 1);
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = "%" + keyword.trim().toLowerCase() + "%";
            // 关键词模糊匹配 name 或 description → MySQL ILIKE 等价
            wrapper.and(w -> w.like(Product::getName, keyword.trim())
                              .or()
                              .like(Product::getDescription, keyword.trim()));
        }

        // 2. 排序（推到 ORDER BY 层）
        if ("sales".equals(sort)) {
            wrapper.orderByDesc(Product::getSales);
        } else if ("price-asc".equals(sort)) {
            wrapper.orderByAsc(Product::getPrice);
        } else if ("price-desc".equals(sort)) {
            wrapper.orderByDesc(Product::getPrice);
        } else {
            // 默认排序：高销量优先 + ID 升序作为第二序，保证分页翻页不乱序
            wrapper.orderByDesc(Product::getSales).orderByAsc(Product::getId);
        }

        // 3. MyBatis-Plus 真分页（SELECT ... LIMIT size OFFSET (page-1)*size）
        //    另一条 SELECT COUNT(1) 自动生成，total/pages 自动计算
        IPage<Product> pageResult = productMapper.selectPage(
                new Page<>(Math.max(page, 1), Math.max(Math.min(size, 100), 1)),  // 每页上限 100 防拖库
                wrapper);

        // 4. 兼容老版返回结构：records 放入 data（老前端取 data），同时附 total/pages
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("records", pageResult.getRecords());
        dataMap.put("total",   pageResult.getTotal());
        dataMap.put("pages",   pageResult.getPages());
        dataMap.put("current", pageResult.getCurrent());
        dataMap.put("size",    pageResult.getSize());

        // 向下兼容：老版 Checkout.vue / Products.vue 直接用 response.data 作为 List<Product>
        // 因此这里同时返回 records 字段 + 顶层 records 字段，同时满足老新两种消费方式
        result.put("code", 200);
        result.put("data", pageResult.getRecords());      // 老版直接消费数组
        result.put("page", dataMap);                        // 新版可以用 page.total / page.pages
        return result;
    }

    /**
     * 商品详情（公开接口，已在拦截器排除列表）
     * <p>
     * 新增：已登录用户浏览时自动记录足迹（Redis ZSet，score=当前时间戳，只保留最近 20 条）；
     * 未登录用户不记录足迹，不报错。足迹记录用 request.getAttribute("userId") 取登录态，
     * 不使用 @RequestAttribute 强制鉴权，保持接口公开性。
     */
    @GetMapping("/products/{id}")
    public Map<String, Object> getProduct(@PathVariable Long id, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        Product product = productMapper.selectById(id);
        if (product == null) {
            result.put("code", 404);
            result.put("message", "商品不存在");
            return result;
        }

        // 已登录用户：记录浏览足迹（失败不阻断主流程）
        // 注意：商品详情是白名单公开接口，拦截器不会注入 userId，
        //       所以这里用 resolveLoginUserId 自行解析 Authorization 头。
        try {
            Long userId = resolveLoginUserId(request);
            if (userId != null) {
                String footprintKey = FOOTPRINT_KEY_PREFIX + userId;
                // score = 当前时间戳（毫秒），时间倒序即最近浏览的在前面
                cacheUtil.zAdd(footprintKey, product.getId(), System.currentTimeMillis());
                // 只保留最近 20 条：删除排名 0 到 -(size-21) 的老记录（0 是分数最低即最旧）
                cacheUtil.zRemoveRange(footprintKey, 0, -(FOOTPRINT_MAX_SIZE + 1));
                cacheUtil.expire(footprintKey, FOOTPRINT_TTL_DAYS, TimeUnit.DAYS);
            }
        } catch (Exception e) {
            // 足迹记录失败不影响商品详情查询
        }

        result.put("code", 200);
        result.put("data", product);
        return result;
    }

    /**
     * 同系列（同 SPU）兄弟规格列表（公开接口，已在拦截器排除列表 /api/client/products/** 下）
     * <p>
     * 与 {@link #getProducts} 商品列表不同：这里<b>包含已下架（status=0）</b>的规格，
     * 供商品详情页「所有规格全部展示，下架/缺货置灰不可选」使用；
     * 被管理员删除的规格在数据库中物理不存在，自然不会返回（符合"删除才消失"的业务约定）。
     * <p>
     * SPU 聚合规则（按商品名提取基础名）统一由前端 utils/spu.js 的 spuKeyOf 计算，
     * 后端只负责把同分类下全部在架 + 下架商品返回，前端再按聚合键过滤，
     * 避免在 Java 侧重复维护一套名称正则。
     */
    @GetMapping("/products/{id}/siblings")
    public Map<String, Object> getSiblings(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();

        Product product = productMapper.selectById(id);
        if (product == null) {
            result.put("code", 404);
            result.put("message", "商品不存在");
            return result;
        }

        // 同分类 + 状态为上架(1)/下架(0)，按价格升序、ID 升序（前端还会二次排序）
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getCategoryId, product.getCategoryId())
               .in(Product::getStatus, 0, 1)
               .orderByAsc(Product::getPrice)
               .orderByAsc(Product::getId);
        List<Product> list = productMapper.selectList(wrapper);

        result.put("code", 200);
        result.put("data", list);
        return result;
    }

    /**
     * 热销榜 TOP10（公开接口，已在拦截器排除列表 /api/client/products/** 下）
     * <p>
     * 数据来源：MySQL product 表真实 sales 字段（与后台商品管理销量一致）。
     * 查询全部上架状态商品，按销量降序截取前 10 条返回。
     */
    @GetMapping("/products/rank/top10")
    public Map<String, Object> getRankTop10() {
        Map<String, Object> result = new HashMap<>();

        List<Product> top10 = productMapper.selectOrderBySalesDesc(10);

        List<Map<String, Object>> rankList = new ArrayList<>();
        for (Product product : top10) {
            Map<String, Object> item = new HashMap<>();
            item.put("product", product);
            item.put("sales", product.getSales() != null ? product.getSales().longValue() : 0L);
            rankList.add(item);
        }

        result.put("code", 200);
        result.put("data", rankList);
        return result;
    }

    /**
     * 商品分类列表（带 Redis 缓存）
     * <p>
     * 查询流程：优先读 Redis 缓存 -> 缓存未命中再查数据库 -> 查询结果写入缓存（30 分钟过期）。
     * 原有数据库查询逻辑保留作为回源，未改动业务逻辑。
     * <p>
     * 缓存失效策略：分类数据发生增删改时，调用方需调用
     * {@code cacheUtil.delete(CACHE_KEY_CATEGORY_LIST)} 主动清除缓存。
     */
    @SuppressWarnings("unchecked")
    @GetMapping("/categories")
    public Map<String, Object> getCategories() {
        Map<String, Object> result = new java.util.HashMap<>();

        // 1. 优先从 Redis 缓存读取
        List<Category> categories = (List<Category>) cacheUtil.get(CACHE_KEY_CATEGORY_LIST);

        // 2. 缓存未命中，回源查询数据库并写入缓存（原有数据库查询逻辑保留）
        if (categories == null) {
            categories = categoryMapper.selectList(null);
            cacheUtil.set(CACHE_KEY_CATEGORY_LIST, categories, 30, TimeUnit.MINUTES);
        }

        result.put("code", 200);
        result.put("data", categories);
        return result;
    }

    /**
     * 首页轮播图列表（带 Redis 缓存）
     * <p>
     * 查询流程：优先读 Redis 缓存 -> 缓存未命中再查数据库 -> 查询结果写入缓存（30 分钟过期）。
     * 原有数据库查询逻辑保留作为回源，未改动业务逻辑。
     * <p>
     * 缓存失效策略：轮播图数据发生增删改时，调用方需调用
     * {@code cacheUtil.delete(CACHE_KEY_CAROUSEL_LIST)} 主动清除缓存。
     */
    @SuppressWarnings("unchecked")
    @GetMapping("/carousel")
    public Map<String, Object> getCarousel() {
        Map<String, Object> result = new java.util.HashMap<>();

        // 1. 优先从 Redis 缓存读取
        List<Carousel> carousels = (List<Carousel>) cacheUtil.get(CACHE_KEY_CAROUSEL_LIST);

        // 2. 缓存未命中，回源查询数据库并写入缓存（原有数据库查询逻辑保留）
        if (carousels == null) {
            carousels = carouselMapper.selectList(null);
            cacheUtil.set(CACHE_KEY_CAROUSEL_LIST, carousels, 30, TimeUnit.MINUTES);
        }

        result.put("code", 200);
        result.put("data", carousels);
        return result;
    }
}