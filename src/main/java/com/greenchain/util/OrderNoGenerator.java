package com.greenchain.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单号生成器（全局唯一）
 * <p>
 * 背景：项目原先存在两套订单号规则 ——
 * {@code ClientOrderController} 用 {@code "GN" + System.currentTimeMillis()}（毫秒时间戳），
 * {@code OrderServiceImpl} 用 {@code "GC" + yyyyMMddHHmmss + UUID前8位}。
 * 前者在同一毫秒内被并发调用会产生完全相同的订单号，
 * 而 {@code order} 表的 {@code order_no} 是 {@code NOT NULL UNIQUE}，
 * 因此并发下单会直接抛唯一键冲突，导致下单失败。
 * <p>
 * 本类统一两条链路的订单号规则，格式为：
 * <pre>
 *   GC + yyyyMMddHHmmssSSS + 6 位随机码
 *   例：GC20260910231945123K7M2QP
 * </pre>
 * 唯一性来源为两层：
 * <ol>
 *   <li>时间戳精确到毫秒（17 位），保证跨毫秒天然不重复；</li>
 *   <li>毫秒内的并发用 6 位随机码区分，字符集 32 个（已剔除 0/O/1/I 等易混淆字符），
 *       同一毫秒内可提供 32^6 ≈ 10.7 亿种组合，碰撞概率可忽略。</li>
 * </ol>
 * 总长度 25 字符，在 {@code order_no VARCHAR(50)} 容量内。
 * <p>
 * 说明：本类为无状态工具类，使用 {@link ThreadLocalRandom} 避免多线程竞争，
 * 不依赖 Spring 容器，可直接静态调用。
 *
 * @author green-chain
 */
public final class OrderNoGenerator {

    /** 订单号统一前缀：Green Chain */
    private static final String PREFIX = "GC";

    /** 时间戳格式：精确到毫秒 */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /** 随机码字符集：去除 0/O/1/I/L 等易混淆字符，共 32 个 */
    private static final char[] SUFFIX_CHARS = "23456789ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();

    /** 随机码长度 */
    private static final int SUFFIX_LENGTH = 6;

    private OrderNoGenerator() {
        // 工具类禁止实例化
    }

    /**
     * 生成全局唯一订单号。
     *
     * @return 形如 {@code GC20260910231945123K7M2QP} 的订单号
     */
    public static String generate() {
        StringBuilder sb = new StringBuilder(PREFIX.length() + 17 + SUFFIX_LENGTH);
        sb.append(PREFIX).append(LocalDateTime.now().format(TIME_FORMATTER));

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            sb.append(SUFFIX_CHARS[random.nextInt(SUFFIX_CHARS.length)]);
        }
        return sb.toString();
    }
}
