package com.greenchain.controller;

import com.greenchain.common.Result;
import com.greenchain.dto.request.GuestCartDTO;
import com.greenchain.entity.CartItem;
import com.greenchain.entity.Product;
import com.greenchain.mapper.CartItemMapper;
import com.greenchain.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户端购物车 Controller（统一返回 Result<T>，与项目约定保持一致）
 */
@RestController
@RequestMapping("/api/client/cart")
public class ClientCartController {

    @Autowired
    private CartItemMapper cartItemMapper;

    @Autowired
    private ProductMapper productMapper;

    /**
     * 查询购物车列表（过滤已下架商品）
     */
    @GetMapping
    public Result<List<CartItem>> getCart(@RequestAttribute("userId") Long userId) {
        List<CartItem> items = cartItemMapper.findByUserIdOnShelf(userId);
        return Result.success(items);
    }

    /**
     * 加入购物车（新增商品上架状态校验，下架商品禁止加购）
     */
    @PostMapping
    public Result<Void> addToCart(@RequestAttribute("userId") Long userId,
                                  @RequestBody GuestCartDTO dto) {
        // ===== P0-4 修复：使用 DTO 接收而非 Map，编译期杜绝空指针 =====
        if (dto.getProductId() == null) {
            return Result.error(400, "商品ID不能为空");
        }
        Long productId = dto.getProductId();
        Integer quantity = dto.getQuantity() != null && dto.getQuantity() > 0 ? dto.getQuantity() : 1;

        Product product = productMapper.selectById(productId);
        if (product == null) {
            return Result.error(404, "商品不存在");
        }
        // 校验：商品已下架（status != 1）禁止加入购物车
        if (product.getStatus() == null || product.getStatus() != 1) {
            return Result.error(400, "该商品已下架，无法加购");
        }

        CartItem existingItem = cartItemMapper.findByUserIdAndProductId(userId, productId);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            existingItem.setUpdateTime(LocalDateTime.now());
            cartItemMapper.updateById(existingItem);
        } else {
            CartItem cartItem = new CartItem();
            cartItem.setUserId(userId);
            cartItem.setProductId(productId);
            cartItem.setName(product.getName());
            cartItem.setImage(product.getImage());
            cartItem.setPrice(product.getPrice());
            cartItem.setQuantity(quantity);
            cartItem.setStock(product.getStock());
            cartItem.setCreateTime(LocalDateTime.now());
            cartItem.setUpdateTime(LocalDateTime.now());
            cartItemMapper.insert(cartItem);
        }

        return Result.success("添加成功");
    }

    @PutMapping("/{id}")
    public Result<Void> updateCartItem(@RequestAttribute("userId") Long userId,
                                       @PathVariable Long id,
                                       @RequestBody GuestCartDTO dto) {
        CartItem cartItem = cartItemMapper.selectById(id);
        if (cartItem == null || !cartItem.getUserId().equals(userId)) {
            return Result.error(404, "购物车项不存在");
        }

        if (dto.getQuantity() != null && dto.getQuantity() > 0) {
            cartItem.setQuantity(dto.getQuantity());
            cartItem.setUpdateTime(LocalDateTime.now());
            cartItemMapper.updateById(cartItem);
        }

        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteCartItem(@RequestAttribute("userId") Long userId,
                                       @PathVariable Long id) {
        CartItem cartItem = cartItemMapper.selectById(id);
        if (cartItem == null || !cartItem.getUserId().equals(userId)) {
            return Result.error(404, "购物车项不存在");
        }

        cartItemMapper.deleteById(id);
        return Result.success("删除成功");
    }

    @DeleteMapping("/clear")
    public Result<Void> clearCart(@RequestAttribute("userId") Long userId) {
        List<CartItem> items = cartItemMapper.findByUserId(userId);
        for (CartItem item : items) {
            cartItemMapper.deleteById(item.getId());
        }
        return Result.success("清空成功");
    }

    /**
     * 游客购物车合并（登录后调用）
     * <p>
     * 关键点：
     * 1. userId 一律通过 @RequestAttribute("userId") 从 JWT 解析获取，完全忽略前端传参；
     * 2. 合并规则：逐条处理游客购物车，商品不存在 / 已下架 / quantity 非法的记录直接跳过（不抛异常）；
     *    用户购物车已有相同商品则数量累加，否则新增购物车项（商品快照从 product 表实时读取）。
     */
    @PostMapping("/mergeGuestCart")
    public Result<Void> mergeGuestCart(@RequestAttribute("userId") Long userId,
                                       @RequestBody List<GuestCartDTO> guestCart) {
        if (guestCart == null || guestCart.isEmpty()) {
            return Result.success("合并成功");
        }

        for (GuestCartDTO dto : guestCart) {
            if (dto.getProductId() == null || dto.getQuantity() == null || dto.getQuantity() <= 0) {
                continue;
            }

            Product product = productMapper.selectById(dto.getProductId());
            if (product == null || product.getStatus() == null || product.getStatus() != 1) {
                continue;
            }

            CartItem existingItem = cartItemMapper.findByUserIdAndProductId(userId, dto.getProductId());
            if (existingItem != null) {
                existingItem.setQuantity(existingItem.getQuantity() + dto.getQuantity());
                existingItem.setUpdateTime(LocalDateTime.now());
                cartItemMapper.updateById(existingItem);
            } else {
                CartItem cartItem = new CartItem();
                cartItem.setUserId(userId);
                cartItem.setProductId(dto.getProductId());
                cartItem.setName(product.getName());
                cartItem.setImage(product.getImage());
                cartItem.setPrice(product.getPrice());
                cartItem.setQuantity(dto.getQuantity());
                cartItem.setStock(product.getStock());
                cartItem.setCreateTime(LocalDateTime.now());
                cartItem.setUpdateTime(LocalDateTime.now());
                cartItemMapper.insert(cartItem);
            }
        }

        return Result.success("合并成功");
    }
}