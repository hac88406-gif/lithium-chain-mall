package com.greenchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greenchain.entity.AfterSale;
import com.greenchain.dto.response.AfterSaleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AfterSaleMapper extends BaseMapper<AfterSale> {

    /**
     * 根据订单ID查询售后记录（用于判断订单是否已提交过售后）
     */
    @Select("SELECT * FROM after_sale WHERE order_id = #{orderId} LIMIT 1")
    AfterSale findByOrderId(@Param("orderId") Long orderId);

    /**
     * 查询订单是否有"进行中"的售后记录（待审核/已同意），被拒绝的不算。
     * 用于防止同一订单重复申请售后（被拒绝后允许重新申请）。
     */
    @Select("SELECT * FROM after_sale WHERE order_id = #{orderId} AND status IN (0, 1) LIMIT 1")
    AfterSale findActiveByOrderId(@Param("orderId") Long orderId);

    /**
     * 查询我的售后列表（按申请时间倒序）
     */
    @Select("SELECT * FROM after_sale WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<AfterSale> findByUserId(@Param("userId") Long userId);

    /**
     * 管理后台售后分页列表（关联订单号、用户信息）
     * MyBatis-Plus 分页插件自动拦截 Page 参数生成 LIMIT 语句
     *
     * @param page 分页参数
     * @param status 状态筛选，null 查全部
     */
    @Select("<script>" +
            "SELECT a.id, a.order_id AS orderId, a.user_id AS userId, a.after_sale_type AS afterSaleType, " +
            "       a.reason, a.evidence, a.status, a.reject_reason AS rejectReason, " +
            "       a.create_time AS createTime, a.update_time AS updateTime, " +
            "       o.order_no AS orderNo, u.username, u.nickname " +
            "FROM after_sale a " +
            "LEFT JOIN `order` o ON a.order_id = o.id " +
            "LEFT JOIN `user` u ON a.user_id = u.id " +
            "<where>" +
            "  <if test='status != null'> AND a.status = #{status}</if>" +
            "</where>" +
            "ORDER BY a.create_time DESC" +
            "</script>")
    Page<AfterSaleVO> selectPageWithOrderAndUser(Page<AfterSaleVO> page, @Param("status") Integer status);

    /**
     * 查询售后详情（关联订单号、用户信息）
     */
    @Select("SELECT a.id, a.order_id AS orderId, a.user_id AS userId, a.after_sale_type AS afterSaleType, " +
            "       a.reason, a.evidence, a.status, a.reject_reason AS rejectReason, " +
            "       a.create_time AS createTime, a.update_time AS updateTime, " +
            "       o.order_no AS orderNo, u.username, u.nickname " +
            "FROM after_sale a " +
            "LEFT JOIN `order` o ON a.order_id = o.id " +
            "LEFT JOIN `user` u ON a.user_id = u.id " +
            "WHERE a.id = #{id}")
    AfterSaleVO selectDetailById(@Param("id") Long id);
}
