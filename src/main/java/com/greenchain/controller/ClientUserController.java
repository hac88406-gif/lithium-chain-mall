package com.greenchain.controller;

import com.greenchain.entity.Product;
import com.greenchain.entity.User;
import com.greenchain.entity.UserAddress;
import com.greenchain.mapper.ProductMapper;
import com.greenchain.mapper.UserAddressMapper;
import com.greenchain.mapper.UserMapper;
import com.greenchain.util.CacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/client/user")
public class ClientUserController {

    /** Redis ZSet 用户浏览足迹 key 前缀：footprint:user:{userId}，value=商品ID，score=时间戳 */
    private static final String FOOTPRINT_KEY_PREFIX = "footprint:user:";

    /** 足迹最多保留 20 条 */
    private static final int FOOTPRINT_MAX_SIZE = 20;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserAddressMapper addressMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CacheUtil cacheUtil;

    @GetMapping
    public Map<String, Object> getUser(@RequestAttribute("userId") Long userId) {
        Map<String, Object> result = new HashMap<>();

        User user = userMapper.selectById(userId);
        if (user == null) {
            result.put("code", 404);
            result.put("message", "用户不存在");
            return result;
        }

        result.put("code", 200);
        result.put("data", user);
        return result;
    }

    @PutMapping
    public Map<String, Object> updateUser(@RequestAttribute("userId") Long userId,
                                          @RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();

        User user = userMapper.selectById(userId);
        if (user == null) {
            result.put("code", 404);
            result.put("message", "用户不存在");
            return result;
        }

        String nickname = request.get("nickname");
        String avatar = request.get("avatar");
        String phone = request.get("phone");

        if (nickname != null) user.setNickname(nickname);
        if (avatar != null) user.setAvatar(avatar);
        if (phone != null) user.setPhone(phone);
        user.setUpdateTime(LocalDateTime.now());

        userMapper.updateById(user);

        result.put("code", 200);
        result.put("message", "更新成功");
        result.put("data", user);
        return result;
    }

    @GetMapping("/addresses")
    public Map<String, Object> getAddresses(@RequestAttribute("userId") Long userId) {
        Map<String, Object> result = new HashMap<>();

        List<UserAddress> addresses = addressMapper.findByUserId(userId);

        result.put("code", 200);
        result.put("data", addresses);
        return result;
    }

    @PostMapping("/addresses")
    public Map<String, Object> addAddress(@RequestAttribute("userId") Long userId,
                                          @RequestBody UserAddress address) {
        Map<String, Object> result = new HashMap<>();

        address.setUserId(userId);
        address.setCreateTime(LocalDateTime.now());
        address.setUpdateTime(LocalDateTime.now());
        address.setIsDefault(0);

        addressMapper.insert(address);

        result.put("code", 200);
        result.put("message", "添加成功");
        result.put("data", address);
        return result;
    }

    @PutMapping("/addresses/{id}")
    public Map<String, Object> updateAddress(@RequestAttribute("userId") Long userId,
                                             @PathVariable Long id,
                                             @RequestBody UserAddress address) {
        Map<String, Object> result = new HashMap<>();

        UserAddress existing = addressMapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            result.put("code", 404);
            result.put("message", "地址不存在");
            return result;
        }

        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            List<UserAddress> addresses = addressMapper.findByUserId(userId);
            for (UserAddress addr : addresses) {
                if (addr.getIsDefault() == 1) {
                    addr.setIsDefault(0);
                    addr.setUpdateTime(LocalDateTime.now());
                    addressMapper.updateById(addr);
                }
            }
        }

        address.setId(id);
        address.setUserId(userId);
        address.setUpdateTime(LocalDateTime.now());
        addressMapper.updateById(address);

        result.put("code", 200);
        result.put("message", "更新成功");
        return result;
    }

    @DeleteMapping("/addresses/{id}")
    public Map<String, Object> deleteAddress(@RequestAttribute("userId") Long userId,
                                             @PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();

        UserAddress existing = addressMapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            result.put("code", 404);
            result.put("message", "地址不存在");
            return result;
        }

        addressMapper.deleteById(id);

        result.put("code", 200);
        result.put("message", "删除成功");
        return result;
    }

    /**
     * 查询当前用户的浏览足迹（最近 20 条，按浏览时间倒序）
     * <p>
     * 鉴权：/api/client/user/** 归属拦截器保护范围，必须携带 JWT。
     * 数据来源：Redis ZSet footprint:user:{userId}，Redis 为空时返回空列表不报错。
     * 已下架 / 不存在的商品过滤掉。
     */
    @GetMapping("/footprint")
    public Map<String, Object> getFootprint(@RequestAttribute("userId") Long userId) {
        Map<String, Object> result = new HashMap<>();

        String footprintKey = FOOTPRINT_KEY_PREFIX + userId;
        // 倒序取最近 FOOTPRINT_MAX_SIZE 条（score=时间戳，越大越新）
        Set<ZSetOperations.TypedTuple<Object>> tuples =
                cacheUtil.zReverseRangeWithScores(footprintKey, 0, FOOTPRINT_MAX_SIZE - 1);

        if (tuples == null || tuples.isEmpty()) {
            result.put("code", 200);
            result.put("data", new ArrayList<>());
            return result;
        }

        // 批量查商品详情，过滤已下架 / 不存在的
        List<Map<String, Object>> footprintList = new ArrayList<>();
        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            Object productIdObj = tuple.getValue();
            if (productIdObj == null) continue;

            Long productId = ((Number) productIdObj).longValue();
            Product product = productMapper.selectById(productId);
            if (product == null) continue;

            Map<String, Object> item = new HashMap<>();
            item.put("product", product);
            item.put("browseTime", tuple.getScore() != null ? tuple.getScore().longValue() : null);
            footprintList.add(item);
        }

        result.put("code", 200);
        result.put("data", footprintList);
        return result;
    }
}