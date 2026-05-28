package com.project.demo.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 * 封装常用的 Redis 操作
 * 使用泛型 T 便于操作各种对象
 */
@Component
public class RedisUtils {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 存储 Refresh Token
     *
     * @param userId        用户ID
     * @param refreshToken   Refresh Token
     * @param expireTime     过期时间(秒)
     */
    public void storeRefreshToken(Long userId, String refreshToken, long expireTime) {
        String key = "refresh:" + userId;
        redisTemplate.opsForValue().set(key, refreshToken, expireTime, TimeUnit.SECONDS);
    }

    /**
     * 获取用户的 Refresh Token
     *
     * @param userId 用户ID
     * @return Refresh Token 或 null
     */
    public String getRefreshToken(Long userId) {
        String key = "refresh:" + userId;
        return (String) redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除 Refresh Token (退出登录时调用)
     *
     * @param userId 用户ID
     */
    public void deleteRefreshToken(Long userId) {
        String key = "refresh:" + userId;
        redisTemplate.delete(key);
    }

    /**
     * 存储验证码
     *
     * @param email        邮箱
     * @param code         验证码
     * @param type         验证码类型 (register/login/reset-password)
     * @param expireTime   过期时间(秒)
     * Key 设计: verify:code:{type}:{email}
     * 示例: verify:code:register:user@example.com
     */
    public void storeVerifyCode(String email, String code, String type, long expireTime) {
        String key = "verify:code:" + type + ":" + email;
        redisTemplate.opsForValue().set(key, code, expireTime, TimeUnit.SECONDS);
    }

    /**
     * 删除验证码
     *
     * @param email 邮箱
     * @param type  验证码类型
     */
    public void deleteVerifyCode(String email, String type) {
        String key = "verify:code:" + type + ":" + email;
        redisTemplate.delete(key);
    }

    /**
     * 验证验证码
     *
     * @param email 邮箱
     * @param code  验证码
     * @param type  验证码类型
     * @return true-正确
     */
    public boolean verifyCode(String email, String code, String type){
        String key = "verify:code:" + type + ":" + email;
        String storedCode = (String) redisTemplate.opsForValue().get(key);
        return code.equals(storedCode);
    }

    /**
     * 检查是否可以发送验证码
     *
     * @param email         邮箱
     * @param type          验证码类型
     * @param interval      发送间隔(秒)
     * @return true-可以发送
     */
    public boolean canSendCode(String email, String type, long interval) {
        String key = "verify:send:" + type + ":" + email;
        // 如果key存在, 说明在间隔时间内不允许发送
        return !Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 记录发送时间
     *
     * @param email    邮箱
     * @param type     验证码类型
     * @param interval 间隔(秒)
     */
    public void recordSendTime(String email, String type, long interval) {
        String key = "verify:send:" + type + ":" + email;
        redisTemplate.opsForValue().set(key, System.currentTimeMillis(), interval, TimeUnit.SECONDS);
    }

    /**
     * 存储用户信息
     *
     * @param userId     用户ID
     * @param userDetails 用户详情
     * @param expireTime 过期时间(秒)
     */
    public void storeUserDetails(Long userId, Object userDetails, long expireTime) {
        String key = "user:" + userId;
        redisTemplate.opsForValue().set(key, userDetails, expireTime, TimeUnit.SECONDS);
    }

    /**
     * 获取用户信息
     *
     * @param userId 用户ID
     * @return UserDetailsImpl 或 null
     */
    @SuppressWarnings("unchecked")
    public <T> T getUserDetails(Long userId) {
        String key = "user:" + userId;
        return (T) redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除用户信息 (退出登录时调用)
     *
     * @param userId 用户ID
     */
    public boolean deleteUserDetails(Long userId) {
        String key = "user:" + userId;
        return delete(key);
    }

    /**
     * 设置 Key-Value
     */
    public <T> void set(String key, T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置 Key-Value 并指定过期时间
     */
    public <T> void set(String key, T value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除 Key
     */
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /**
     * 检查 Key 是否存在
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 设置过期时间
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }
}