package com.greenchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greenchain.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Select("SELECT * FROM product WHERE status = 1")
    List<Product> selectAllActive();

    /**
     * 条件扣减库存（数据库原子操作，防超卖双保险）
     * 仅当当前库存 >= 购买数量时才扣减，stock 永远不会小于 0。
     * 与 Redis 分布式锁配合使用：锁保证同一商品串行处理，本 SQL 兜底防超卖。
     *
     * @param productId 商品ID
     * @param quantity  购买数量（必须 > 0）
     * @return 影响行数：1-扣减成功；0-库存不足（stock < quantity）
     */
    @Update("UPDATE product SET stock = stock - #{quantity} " +
            "WHERE id = #{productId} AND stock >= #{quantity}")
    int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 回补库存（下单中途失败时的订单内补偿，非订单取消回滚）
     *
     * @param productId 商品ID
     * @param quantity  回补数量
     * @return 影响行数
     */
    @Update("UPDATE product SET stock = stock + #{quantity} WHERE id = #{productId}")
    int restoreStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 按销量倒序取前 N 个上架商品（Redis ZSet 回退源）
     */
    @Select("SELECT * FROM product WHERE status = 1 ORDER BY sales DESC LIMIT #{limit}")
    List<Product> selectOrderBySalesDesc(@Param("limit") int limit);

    /**
     * 原子累加销量（确认收货/支付成功时调用，防并发重复累加）
     * 与订单状态原子 UPDATE 配套使用：仅当 rows==1 时执行一次，避免销量虚高。
     *
     * @param productId 商品ID
     * @param quantity  累加的销量（订单明细中的购买数量）
     * @return 影响行数
     */
    @Update("UPDATE product SET sales = sales + #{quantity} WHERE id = #{productId}")
    int addSales(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}