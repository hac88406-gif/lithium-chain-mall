package com.greenchain.controller;

import com.greenchain.common.Result;
import com.greenchain.mapper.OrderItemMapper;
import com.greenchain.mapper.OrderMapper;
import com.greenchain.mapper.ProductMapper;
import com.greenchain.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台仪表盘/数据大屏接口
 * <p>
 * 批次 2：summary 与 dailyTrend 已接真实统计 SQL（OrderMapper.xml / OrderItemMapper.xml），
 * categorySales/statusDistribution/hotProducts/provinceOrders 直接调用对应 Mapper 统计方法。
 * 注意：本 Controller 不加 @RequiresPermission，管理员登录即可访问（走 AdminAuthInterceptor 鉴权）。
 * <p>
 * P1-7 修复：新增裸路径 GET /api/admin/dashboard → 与 /overview 返回相同数据，
 *           兼容前端 api/index.js 中定义的 getDashboard() 方法（无 /overview 后缀）。
 */
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * P1-7 兼容裸路径：前端 getDashboard() 会请求 GET /api/admin/dashboard（无 /overview）
     * 直接调用 overview() 返回相同数据，避免出现 404。
     *
     * @return 与 /overview 完全相同的 Result 结构
     */
    @GetMapping
    public Result<Map<String, Object>> dashboard() {
        return overview();
    }

    /**
     * 仪表盘总览数据（6 大块）
     * <p>
     * 返回 Map 结构：
     * - summary            核心指标汇总（今日下单/今日已付/本月营收/商品数/用户数/待处理订单/待处理售后）
     * - dailyTrend         近 7 天订单量与销售额趋势（先造 7 天骨架，再按 date 合并真实统计数据）
     * - categorySales      分类销售额分布（有效成交口径）
     * - statusDistribution 订单状态分布
     * - hotProducts        热销商品 TOP10
     * - provinceOrders     省份订单分布（TOP15）
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();                  // 今日 00:00:00
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);             // 今日 23:59:59.999...
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();
        LocalDateTime yesterdayEnd = today.minusDays(1).atTime(LocalTime.MAX);
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();// 本月 1 号 00:00:00

        Map<String, Object> data = new HashMap<>();

        // ========== 1. summary 核心指标汇总（真实统计，null 一律兜底 0） ==========
        Map<String, Object> summary = new LinkedHashMap<>();
        Long todayOrders = orderMapper.countCreatedBetween(todayStart, todayEnd);
        summary.put("todayOrders", todayOrders != null ? todayOrders : 0L);                       // 今日下单数
        BigDecimal todayPaid = orderMapper.sumPaidAmountBetween(todayStart, todayEnd);
        summary.put("todayPaidAmount", todayPaid != null ? todayPaid : BigDecimal.ZERO);           // 今日已支付金额
        // 昨日对比数据（用于前端计算环比百分比）
        Long yesterdayOrders = orderMapper.countCreatedBetween(yesterdayStart, yesterdayEnd);
        summary.put("yesterdayOrders", yesterdayOrders != null ? yesterdayOrders : 0L);
        BigDecimal yesterdayPaid = orderMapper.sumPaidAmountBetween(yesterdayStart, yesterdayEnd);
        summary.put("yesterdayPaidAmount", yesterdayPaid != null ? yesterdayPaid : BigDecimal.ZERO);
        BigDecimal monthRevenue = orderMapper.sumPaidAmountBetween(monthStart, todayEnd);
        summary.put("monthRevenue", monthRevenue != null ? monthRevenue : BigDecimal.ZERO);       // 本月营收
        Long pendingOrders = orderMapper.countByStatus("pending");
        summary.put("pendingOrders", pendingOrders != null ? pendingOrders : 0L);                  // 待支付订单数
        Long pendingAfterSale = orderMapper.countAfterSalePending();
        summary.put("pendingAfterSale", pendingAfterSale != null ? pendingAfterSale : 0L);         // 待处理售后数
        summary.put("totalProducts", productMapper.selectCount(null));  // 商品总数
        summary.put("totalUsers", userMapper.selectCount(null));        // 用户总数
        data.put("summary", summary);

        // ========== 2. dailyTrend 近 7 天趋势（先造 7 天骨架，再按 date 合并真实数据） ==========
        // 2.1 查询近 7 天真实统计（start=7天前的 00:00，end=今天最后时刻）
        LocalDateTime trendStart = today.minusDays(6).atStartOfDay();
        List<Map<String, Object>> statRows = orderMapper.statDailyOrderCountAndAmount(trendStart, todayEnd);
        // 把查询结果转成 date -> row 的索引，便于按天匹配
        Map<String, Map<String, Object>> statIndex = new HashMap<>();
        if (statRows != null) {
            for (Map<String, Object> row : statRows) {
                Object dateKey = row.get("date");
                if (dateKey != null) {
                    statIndex.put(dateKey.toString(), row);
                }
            }
        }

        // 2.2 造 7 天骨架：对每一天，统计结果里有匹配的就填真实值，没有就留 0
        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            String dateStr = day.format(dateFormatter);
            Map<String, Object> item = new HashMap<>();
            item.put("date", dateStr);                                        // 完整日期：2026-09-01
            item.put("label", day.getMonthValue() + "/" + day.getDayOfMonth()); // 短标签：9/1
            Map<String, Object> matched = statIndex.get(dateStr);
            item.put("orderCount", matched != null ? matched.get("orderCount") : 0); // 订单量
            item.put("amount", matched != null ? matched.get("amount") : 0);         // 销售额
            dailyTrend.add(item);
        }
        data.put("dailyTrend", dailyTrend);

        // ========== 3~6. 直接调用 Mapper 统计方法（XML 已实现） ==========
        data.put("categorySales", orderItemMapper.statSalesGroupByCategory());   // 分类销售额分布
        data.put("statusDistribution", orderMapper.statStatusDistribution());    // 订单状态分布
        data.put("hotProducts", orderItemMapper.statHotProductsTop10());         // 热销商品 TOP10
        data.put("provinceOrders", orderMapper.statProvinceOrderCount());        // 省份订单分布

        return Result.success(data);
    }

    /**
     * 数据大屏接口（当前与 overview 返回一致）
     * <p>
     * 未来大屏数据可加 Redis 缓存（如 5 分钟 TTL），与 overview 的实时数据区分开，
     * 本阶段先直接复用 overview 的结果。
     */
    @GetMapping("/screen")
    public Result<Map<String, Object>> screen() {
        return overview();
    }
}
