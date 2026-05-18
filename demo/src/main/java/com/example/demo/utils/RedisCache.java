package com.example.demo.utils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisCache {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisCache(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 设置缓存
    public void set(String key, Object value, long expire) {
        redisTemplate.opsForValue().set(key, value, expire, TimeUnit.MILLISECONDS);
    }

    // 获取缓存
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // 删除缓存
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    // 设置token（2小时有效期）
    public void setToken(String token, Object userInfo) {
        set("token:" + token, userInfo, 7200000);
    }

    // 获取token
    public Object getToken(String token) {
        return get("token:" + token);
    }

    // 删除token
    public void deleteToken(String token) {
        delete("token:" + token);
    }
}