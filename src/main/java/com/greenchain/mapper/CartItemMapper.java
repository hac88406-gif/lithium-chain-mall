package com.greenchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greenchain.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {

    @Select("SELECT * FROM cart_item WHERE user_id = #{userId}")
    List<CartItem> findByUserId(@Param("userId") Long userId);

    /**
     * 查询购物车列表（过滤已下架商品）
     * SQL 定义在 CartItemMapper.xml，关联 product 表只返回上架（status=1）商品的购物车项。
     * 购物车记录本身不删除，仅查询时过滤。
     */
    List<CartItem> findByUserIdOnShelf(@Param("userId") Long userId);

    @Select("SELECT * FROM cart_item WHERE user_id = #{userId} AND product_id = #{productId}")
    CartItem findByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
}