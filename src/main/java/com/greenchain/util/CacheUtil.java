package com.greenchain.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存工具类（简单封装）
 * <p>
 * 封装常用的 get / set / delete 三个缓存操作，
 * 底层使用 RedisConfig 中配置好的 RedisTemplate<String, Object>
 * （key 为 String 明文，value 为 Jackson JSON）。
 * <p>
 * 使用示例：
 * <pre>
 *     // 写缓存，30 分钟过期
 *     cacheUtil.set("cache:carousel:list", carousels, 30, TimeUnit.MINUTES);
 *     // 读缓存
 *     Object data = cacheUtil.get("cache:carousel:list");
 *     // 数据库更新后删除缓存
 *     cacheUtil.delete("cache:carousel:list");
 * </pre>
 */
@Component
public class CacheUtil {

    /**
     * 默认缓存过期时间（分钟）
     */
    private static final long DEFAULT_TIMEOUT_MINUTES = 30L;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 写入缓存（带默认过期时间 30 分钟）
     *
     * @param key   缓存键
     * @param value 缓存值（会被 Jackson 序列化为 JSON 存储）
     */
    public void set(String key, Object value) {
        set(key, value, DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 写入缓存（自定义过期时间）
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时长
     * @param unit    时长单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 读取缓存
     *
     * @param key 缓存键
     * @return 缓存值；键不存在或已过期时返回 null（调用方需要自行回源查数据库）
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存
     * <p>
     * 用于数据库数据发生增删改后，主动清除对应缓存，
     * 下次查询时重新从数据库加载最新数据并写入缓存。
     *
     * @param key 缓存键
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 原子删除：仅当 key 存在时删除，并返回是否删除成功
     * <p>
     * Redis DEL 命令本身是原子操作，用于幂等令牌消费场景：
     * 并发重复请求中只有一个请求能删除成功，其余请求拿到 false 被拦截。
     *
     * @param key 缓存键
     * @return true-删除成功（令牌被本次请求消费）；false-key 不存在（已被消费或已过期）
     */
    public boolean deleteIfPresent(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    // ==================== 分布式锁（SETNX + 过期时间简单实现） ====================

    /**
     * 尝试获取分布式锁（Redis SETNX + 过期时间实现）
     * <p>
     * 加锁失败直接返回 null（由调用方决定返回业务失败），本工具类不做自旋等待。
     * 锁的 value 为随机 token，用于解锁时校验，防止误删其他持有者的锁。
     *
     * @param key     锁键（如 lock:product:1001）
     * @param timeout 锁自动过期时长（防止持有者宕机导致死锁）
     * @param unit    时长单位
     * @return 获取成功返回锁 token（解锁时需原样传回）；获取失败返回 null
     */
    public String tryLock(String key, long timeout, TimeUnit unit) {
        String token = UUID.randomUUID().toString();
        // SETNX 语义：key 不存在才写入并返回 true，同时设置过期时间
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, token, timeout, unit);
        return Boolean.TRUE.equals(ok) ? token : null;
    }

    /**
     * 释放分布式锁
     * <p>
     * 先校验锁的 value 是否为本人加锁时返回的 token，一致才删除，
     * 防止锁已过期被他人获取后误删他人的锁。
     *
     * @param key   锁键
     * @param token 加锁时返回的 token
     */
    public void unlock(String key, String token) {
        if (token == null) {
            return;
        }
        Object value = redisTemplate.opsForValue().get(key);
        if (token.equals(value)) {
            redisTemplate.delete(key);
        }
    }

    // ==================== 计数器（用于限流、统计） ====================

    /**
     * INCR 自增 1
     * <p>
     * Redis INCR 是原子操作，不存在并发竞态，适合做限流计数器（如"1 秒内请求次数"）。
     * key 不存在时会先初始化为 0 再自增，返回值为自增后的数值。
     *
     * @param key 计数器键（如 rate_limit:user:123）
     * @return 自增后的值
     */
    public long incr(String key) {
        Long value = redisTemplate.opsForValue().increment(key);
        return value != null ? value : 0L;
    }

    /**
     * 自增指定步长
     * <p>
     * 底层等价于 Redis INCRBY 命令，原子自增 delta 步长。
     * key 不存在时初始化为 0 再加 delta。
     *
     * @param key   计数器键
     * @param delta 自增步长（正数为加，负数为减）
     * @return 自增后的值
     */
    public long incrBy(String key, long delta) {
        Long value = redisTemplate.opsForValue().increment(key, delta);
        return value != null ? value : 0L;
    }

    /**
     * 设置过期时间
     * <p>
     * 用于为计数器或其他缓存键设置 TTL，常搭配 incr 使用（如限流计数器需在时间窗口到期后自动重置）。
     * 如果 key 不存在或已过期，调用无效果（不会报错）。
     *
     * @param key     要设置过期的键
     * @param timeout 过期时长
     * @param unit    时长单位
     */
    public void expire(String key, long timeout, TimeUnit unit) {
        if (key == null) {
            return;
        }
        redisTemplate.expire(key, timeout, unit);
    }

    // ==================== ZSet 有序集合（用于热销排行榜、浏览足迹） ====================

    /**
     * ZADD：写入成员及分数
     * <p>
     * 用于热销排行榜（value=商品ID，score=销量）、浏览足迹（value=商品ID，score=时间戳）等场景。
     * 同一成员重复写入会覆盖其 score 值。
     *
     * @param key   ZSet 键（如 rank:hot_products、footprint:user:123）
     * @param value 成员（如商品 ID 或商品对象）
     * @param score 分数（如销量、时间戳）
     */
    public void zAdd(String key, Object value, double score) {
        if (key == null || value == null) {
            return;
        }
        redisTemplate.opsForZSet().add(key, value, score);
    }

    /**
     * ZINCRBY：成员分数累加
     * <p>
     * 用于销量实时累加场景（商品售出一件即 +1），原子操作。
     * 成员不存在时会先初始化为 0 再加 delta。
     *
     * @param key   ZSet 键
     * @param value 成员
     * @param delta 分数增量（正数累加，负数递减）
     * @return 累加后的分数；如果操作失败返回 null
     */
    public Double zIncrementScore(String key, Object value, double delta) {
        if (key == null || value == null) {
            return null;
        }
        return redisTemplate.opsForZSet().incrementScore(key, value, delta);
    }

    /**
     * 按分数倒序取成员（取 TOP N）
     * <p>
     * Redis ZREVRANGE 命令语义，分数从高到低排列。
     * start/end 为排名下标（从 0 开始），如取前 10 名传 0,-10；取全部传 0,-1。
     *
     * @param key   ZSet 键
     * @param start 起始排名（包含）
     * @param end   结束排名（包含，负数表示倒数第 N 名）
     * @return 成员集合；key 不存在时返回空集合
     */
    public Set<Object> zReverseRange(String key, long start, long end) {
        if (key == null) {
            return java.util.Collections.emptySet();
        }
        Set<Object> result = redisTemplate.opsForZSet().reverseRange(key, start, end);
        return result != null ? result : java.util.Collections.emptySet();
    }

    /**
     * 按分数倒序取成员 + 分数（带值版本）
     * <p>
     * 与 zReverseRange 语义相同，但返回 TypedTuple 集合，每个元素同时包含 value 和 score。
     * 常用于排行榜展示：既需要商品 ID 又需要销量数值。
     *
     * @param key   ZSet 键
     * @param start 起始排名（包含）
     * @param end   结束排名（包含，负数表示倒数第 N 名）
     * @return TypedTuple 集合，每个元素包含 value + score；key 不存在时返回空集合
     */
    public Set<TypedTuple<Object>> zReverseRangeWithScores(String key, long start, long end) {
        if (key == null) {
            return java.util.Collections.emptySet();
        }
        Set<TypedTuple<Object>> result = redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
        return result != null ? result : java.util.Collections.emptySet();
    }

    /**
     * 删除指定排名区间的成员
     * <p>
     * 按分数从低到高的排名删除（Redis ZREMRANGEBYRANK 语义），常用于浏览足迹只保留最近 N 条：
     * 新增一条后删除排名 0 到 size-N-1 区间的老记录。
     *
     * @param key   ZSet 键
     * @param start 起始排名（包含，0 表示分数最低的成员）
     * @param end   结束排名（包含，负数表示倒数第 N 名）
     */
    public void zRemoveRange(String key, long start, long end) {
        if (key == null) {
            return;
        }
        redisTemplate.opsForZSet().removeRange(key, start, end);
    }

    /**
     * 获取 ZSet 成员总数
     * <p>
     * 用于判断排行榜是否为空、浏览足迹数量等。
     *
     * @param key ZSet 键
     * @return 成员数量；key 不存在时返回 0
     */
    public Long zCard(String key) {
        if (key == null) {
            return 0L;
        }
        Long size = redisTemplate.opsForZSet().zCard(key);
        return size != null ? size : 0L;
    }

    // ==================== Hash 结构（存业务对象，比 String 存 JSON 更灵活） ====================
    // Hash 适合存"对象属性"——购物车 cart:userId → { productId: quantity, ... }
    // 单字段增删改都是 O(1)，不需要反序列化整个 JSON

    /**
     * Hash 设字段
     *
     * @param key   Hash 键（如 cart:userId）
     * @param field 字段（如 productId=1001）
     * @param value 值（如 quantity=2）
     */
    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * Hash 设字段 + TTL（首次创建时一起过期）
     */
    public void hSet(String key, String field, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForHash().put(key, field, value);
        redisTemplate.expire(key, timeout, unit);
    }

    /**
     * Hash 查单个字段
     */
    public Object hGet(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    /**
     * Hash 查全部字段（返回 Map）
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * Hash 字段自增（原子，购物车加减库存的核心操作）
     *
     * @param delta 增量（正数加，负数减）
     * @return 自增后的值
     */
    public Long hIncrBy(String key, String field, long delta) {
        return redisTemplate.opsForHash().increment(key, field, delta);
    }

    /**
     * Hash 删除字段
     */
    public Long hDelete(String key, String... fields) {
        return redisTemplate.opsForHash().delete(key, (Object[]) fields);
    }

    /**
     * Hash 键是否存在（整个 Hash 或某个 field）
     */
    public Boolean hHasKey(String key, String field) {
        return redisTemplate.opsForHash().hasKey(key, field);
    }

    // ==================== 限流（Rate Limiting）固定窗口算法 ====================
    // 用 Redis INCR 计数器实现：key 不存在时先初始化为 1 并设置过期时间，
    // 之后每次请求 INCR +1，超过阈值就拒绝。简单高效，适合下单防刷/接口限频。

    /**
     * 限流尝试获取令牌（固定窗口）
     * <p>
     * 核心逻辑：INCR 原子计数，首次计数时设置 TTL。返回 true 表示放行，false 表示限流。
     * 固定窗口的缺陷是窗口边界可能有双倍流量（比如 0:59 和 1:00 同时请求各 5 次，总共 10 次），
     * 但实现最简单、性能最好，毕设够用。
     * <p>
     * 使用示例：
     * <pre>
     *   // 同一 IP 1 分钟最多 5 次下单
     *   if (!cacheUtil.tryAcquire("rate_limit:order:ip_192.168.1.1", 5, 60)) {
     *       throw new BusinessException(429, "操作太频繁，请稍后再试");
     *   }
     * </pre>
     *
     * @param key          限流键（建议带业务前缀 + 唯一标识如 IP / userId）
     * @param maxRequests  时间窗口内最大请求次数
     * @param windowSeconds 窗口大小（秒）
     * @return true=放行；false=超过阈值被限流
     */
    public boolean tryAcquire(String key, int maxRequests, long windowSeconds) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            return false;
        }
        if (count == 1) {
            // 第一次计数：设置过期时间让窗口自动滑动
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        return count <= maxRequests;
    }
}
