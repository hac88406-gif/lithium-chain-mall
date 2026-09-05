package com.greenchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greenchain.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT * FROM `order` WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Order> findByUserId(@Param("userId") Long userId);

    // ==================== 原子状态流转（B4 修复）：条件 UPDATE 替代"先查后改" ====================

    /**
     * 原子取消订单（B4 修复）
     * 仅当订单属于该用户且状态为 pending/paid 时，才置为 cancelled。
     * 数据库行级锁保证并发场景下同一订单只有一次取消成功，
     * 影响行数=0 表示订单不存在、非本人、或状态已不可取消。
     *
     * @param id     订单ID
     * @param userId 当前用户ID（归属校验，防止横向越权取消他人订单）
     * @return 影响行数：1-取消成功；0-状态不可取消/订单不存在/非本人订单
     */
    @Update("UPDATE `order` SET status = 'cancelled', update_time = NOW() " +
            "WHERE id = #{id} AND user_id = #{userId} AND status IN ('pending', 'paid')")
    int cancelIfPendingOrPaid(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 原子支付订单（B4 顺手加固）
     * 仅当订单属于该用户且状态为 pending 时才置为 paid，
     * 防止并发/重复支付把已取消订单改回 paid。
     *
     * @param id     订单ID
     * @param userId 当前用户ID
     * @return 影响行数：1-支付成功；0-状态不可支付/订单不存在/非本人订单
     */
    @Update("UPDATE `order` SET status = 'paid', update_time = NOW() " +
            "WHERE id = #{id} AND user_id = #{userId} AND status = 'pending'")
    int payIfPending(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 支付回调成功后原子置已付款（按订单号，回调无 userId 上下文）。
     * 仅当订单状态仍为 pending 时置为 paid，防止把已取消/已关闭订单改回 paid。
     *
     * @param orderNo 订单号
     * @return 影响行数：1-置为已付款；0-订单不存在或状态非 pending
     */
    @Update("UPDATE `order` SET status = 'paid', update_time = NOW() " +
            "WHERE order_no = #{orderNo} AND status = 'pending'")
    int markPaidByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 原子确认收货（P0-3 修复：防并发竞态先查后改）
     * 仅当订单属于该用户且状态为 paid/shipped 时才置为 completed，
     * 数据库行级锁保证并发多次确认只有一次影响行数为 1，避免后续 sales 累加被重复执行。
     *
     * @param id     订单ID
     * @param userId 当前用户ID（归属校验，防横向越权）
     * @return 影响行数：1-确认成功；0-状态不可确认/订单不存在/非本人订单
     */
    @Update("UPDATE `order` SET status = 'completed', update_time = NOW() " +
            "WHERE id = #{id} AND user_id = #{userId} AND status IN ('paid', 'shipped')")
    int confirmIfPaidOrShipped(@Param("id") Long id, @Param("userId") Long userId);

    // ==================== 仪表盘统计方法（SQL 在 OrderMapper.xml 中实现） ====================

    /**
     * 统计某时间段内创建的订单数（仪表盘：今日下单数）
     *
     * @param start 开始时间（含）
     * @param end   结束时间（不含）
     * @return 订单数量
     */
    Long countCreatedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 统计某时间段内已支付订单的实付总金额（仪表盘：今日已支付金额 / 本月营收）
     *
     * @param start 开始时间（含）
     * @param end   结束时间（不含）
     * @return 金额合计，无数据时返回 null（Service 层需按 0 处理）
     */
    BigDecimal sumPaidAmountBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 按订单状态统计订单数（仪表盘：待支付订单数）
     *
     * @param status 订单状态（如 pending / paid / shipped / completed / cancelled）
     * @return 该状态的订单数量
     */
    Long countByStatus(@Param("status") String status);

    /**
     * 统计待处理售后申请数（after_sale.status=0 待审核）
     * <p>
     * 逻辑简单直接用注解 SQL，不进 XML。
     *
     * @return 待处理售后数量
     */
    @Select("SELECT COUNT(1) FROM after_sale WHERE status = 0")
    Long countAfterSalePending();

    /**
     * 按天统计订单量与销售额（仪表盘：近 7 天趋势图）
     *
     * @param start 开始日期（含）
     * @param end   结束日期（含）
     * @return 每天一条记录：date / orderCount / amount
     */
    List<Map<String, Object>> statDailyOrderCountAndAmount(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 订单状态分布统计（仪表盘：状态饼图）
     *
     * @return 每种状态一条记录：status / count
     */
    List<Map<String, Object>> statStatusDistribution();

    /**
     * 省份订单量分布统计（仪表盘：省份订单榜，JOIN user_address 取省份）
     *
     * @return 每个省份一条记录：province / count
     */
    List<Map<String, Object>> statProvinceOrderCount();
}