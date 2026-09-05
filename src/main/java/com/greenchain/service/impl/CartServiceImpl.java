package com.greenchain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.greenchain.common.BusinessException;
import com.greenchain.dto.request.CartAddRequest;
import com.greenchain.dto.request.CartUpdateRequest;
import com.greenchain.dto.response.CartVO;
import com.greenchain.entity.Cart;
import com.greenchain.entity.Product;
import com.greenchain.mapper.CartMapper;
import com.greenchain.mapper.ProductMapper;
import com.greenchain.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 购物车服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartMapper cartMapper;
    private final ProductMapper productMapper;

    @Override
    public List<CartVO> listByUserId(Long userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId)
               .orderByDesc(Cart::getCreateTime);
        List<Cart> cartList = cartMapper.selectList(wrapper);

        List<CartVO> result = new ArrayList<>();
        for (Cart cart : cartList) {
            result.add(convertToVO(cart));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartVO add(Long userId, CartAddRequest request) {
        // 校验商品是否存在
        Product product = productMapper.selectById(request.getProductId());
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException(400, "商品已下架");
        }
        if (product.getStock() < request.getQuantity()) {
            throw new BusinessException(400, "库存不足");
        }

        // 检查购物车是否已有该商品
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId)
               .eq(Cart::getProductId, request.getProductId());
        Cart existing = cartMapper.selectOne(wrapper);

        if (existing != null) {
            // 已有则累加数量
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
            cartMapper.updateById(existing);
            log.info("购物车累加商品：用户{}，商品{}，数量{}", userId, product.getName(), existing.getQuantity());
            return convertToVO(existing);
        } else {
            // 新增
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(request.getProductId());
            cart.setQuantity(request.getQuantity());
            cartMapper.insert(cart);
            log.info("购物车新增商品：用户{}，商品{}", userId, product.getName());
            return convertToVO(cart);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartVO update(Long userId, CartUpdateRequest request) {
        Cart cart = cartMapper.selectById(request.getId());
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException(404, "购物车项不存在");
        }
        cart.setQuantity(request.getQuantity());
        cartMapper.updateById(cart);
        log.info("购物车更新数量：{} -> {}", cart.getId(), request.getQuantity());
        return convertToVO(cart);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long cartId) {
        Cart cart = cartMapper.selectById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException(404, "购物车项不存在");
        }
        cartMapper.deleteById(cartId);
        log.info("删除购物车项：{}", cartId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(Long userId, List<Long> cartIds) {
        for (Long cartId : cartIds) {
            delete(userId, cartId);
        }
    }

    private CartVO convertToVO(Cart cart) {
        CartVO vo = new CartVO();
        vo.setId(cart.getId());
        vo.setUserId(cart.getUserId());
        vo.setProductId(cart.getProductId());
        vo.setQuantity(cart.getQuantity());
        vo.setCreateTime(cart.getCreateTime());

        Product product = productMapper.selectById(cart.getProductId());
        if (product != null) {
            vo.setProductName(product.getName());
            vo.setProductPrice(product.getPrice());
            vo.setProductImage(product.getImage());
        }
        return vo;
    }
}