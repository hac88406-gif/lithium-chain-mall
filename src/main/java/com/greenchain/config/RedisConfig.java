package com.greenchain.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 * <p>
 * Spring Boot 默认提供的 RedisTemplate 使用 JDK 序列化（JdkSerializationRedisSerializer），
 * 存入 Redis 的内容是二进制乱码，不方便查看和调试。
 * <p>
 * 这里自定义 RedisTemplate：
 * 1. key / hashKey 使用 String 序列化（字符串明文，避免 key 乱码）；
 * 2. value / hashValue 使用 Jackson JSON 序列化（JSON 明文，避免 value 乱码，
 *    且反序列化时能自动还原为原来的 Java 对象类型）；
 * 3. 注册 JavaTimeModule，支持实体中 LocalDateTime 类型字段（Carousel/Category 等
 *    实体的 createTime、updateTime 均为 LocalDateTime）的序列化与反序列化。
 */
@Configuration
public class RedisConfig {

    /**
     * 自定义 RedisTemplate<String, Object>
     *
     * @param factory Redis 连接工厂，由 Spring Boot 根据 yml 中的 spring.redis 配置自动创建
     * @return 序列化策略配置好的 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 设置连接工厂
        template.setConnectionFactory(factory);

        // ========== 配置 Jackson JSON 序列化器（用于 value） ==========
        Jackson2JsonRedisSerializer<Object> jacksonSerializer =
                new Jackson2JsonRedisSerializer<>(Object.class);

        ObjectMapper om = new ObjectMapper();
        // 指定所有属性可见域为 ANY，保证私有字段也能被序列化
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 序列化时在 JSON 中写入类型信息（@class 属性），
        // 反序列化时自动还原为原对象类型（如 List<Carousel> 中的 Carousel）
        om.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        // 注册 JavaTimeModule，支持 LocalDateTime 等 Java 8 时间类型的序列化
        // （若不注册，序列化 LocalDateTime 字段会抛 InvalidDefinitionException）
        om.registerModule(new JavaTimeModule());
        jacksonSerializer.setObjectMapper(om);

        // ========== key 使用 String 序列化（明文字符串） ==========
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // key 采用 String 序列化，Redis 中显示为明文，避免乱码
        template.setKeySerializer(stringSerializer);
        // hash 的 field 也采用 String 序列化
        template.setHashKeySerializer(stringSerializer);
        // value 采用 Jackson JSON 序列化
        template.setValueSerializer(jacksonSerializer);
        // hash 的 value 也采用 Jackson JSON 序列化
        template.setHashValueSerializer(jacksonSerializer);

        // 执行序列化器初始化（使配置生效）
        template.afterPropertiesSet();
        return template;
    }
}
