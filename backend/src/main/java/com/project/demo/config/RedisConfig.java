package com.project.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 * 配置目的:
 * 1. 设置 Key 的序列化方式 (String)
 * 2. 设置 Value 的序列化方式 (JSON)
 * 3. 方便后续通过 RedisTemplate 操作 Redis
 */
@Configuration
public class RedisConfig {

    /**
     * 配置 RedisTemplate
     * RedisTemplate 是 Redis 操作的核心类
     * 它提供了各种数据类型的方法: opsForValue(), opsForHash(), opsForList() 等
     *
     * @param connectionFactory Spring Boot 自动配置的 Redis 连接工厂
     * @return RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // ========== Key 序列化器 ==========
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // ========== Value 序列化器 ==========
        // 创建 ObjectMapper 并注册 JavaTimeModule
        ObjectMapper objectMapper = new ObjectMapper();

        // 注册 JavaTimeModule 以支持 LocalDateTime 等 Java 8 日期时间类型
        objectMapper.registerModule(new JavaTimeModule());

        // 配置类型验证器，允许所有类型（生产环境建议限制特定包）
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();

        // 使用 GenericJackson2JsonRedisSerializer，它会自动处理类型信息
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(
                objectMapper.copy().activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL)
        );

        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // ========== 启用默认值 ==========
        template.setEnableDefaultSerializer(true);
        template.afterPropertiesSet();

        return template;
    }
}


