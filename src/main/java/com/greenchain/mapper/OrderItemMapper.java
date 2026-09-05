package com.greenchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greenchain.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @Select("SELECT * FROM order_item WHERE order_id = #{orderId}")
    List<OrderItem> findByOrderId(@Param("orderId") Long orderId);

    // ==================== 仪表盘统计方法（SQL 在 OrderItemMapper.xml 中实现） ====================

    /**
     * 按商品分类统计销售额（仪表盘：分类销售额分布）
     * <p>
     * JOIN order_item + product + category，按分类聚合销量与金额。
     *
     * @return 每个分类一条记录：categoryId / categoryName / salesCount / salesAmount
     */
    List<Map<String, Object>> statSalesGroupByCategory();

    /**
     * 热销商品 TOP10（仪表盘：热销商品榜）
     * <p>
     * 按订单条目汇总销量排序取前 10。
     *
     * @return 每个商品一条记录：productId / productName / salesCount / salesAmount
     */
    List<Map<String, Object>> statHotProductsTop10();
}