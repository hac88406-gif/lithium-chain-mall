package com.greenchain.service;

import com.greenchain.dto.request.CartAddRequest;
import com.greenchain.dto.request.CartUpdateRequest;
import com.greenchain.dto.response.CartVO;

import java.util.List;

/**
 * 购物车服务接口
 */
public interface CartService {

    /**
     * 查询用户购物车列表
     */
    List<CartVO> listByUserId(Long userId);

    /**
     * 新增购物车项
     */
    CartVO add(Long userId, CartAddRequest request);

    /**
     * 修改购物车项
     */
    CartVO update(Long userId, CartUpdateRequest request);

    /**
     * 删除购物车项
     */
    void delete(Long userId, Long cartId);

    /**
     * 批量删除购物车项
     */
    void deleteBatch(Long userId, List<Long> cartIds);
}