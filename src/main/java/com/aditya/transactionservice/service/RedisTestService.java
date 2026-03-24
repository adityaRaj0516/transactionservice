package com.aditya.transactionservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RedisTestService {

    private static final Logger log =
            LoggerFactory.getLogger(RedisTestService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Simple check to confirm Redis is running (used when starting via Docker)
    public void testRedisConnection() {
        try {
            redisTemplate.opsForValue().set("health:test", "ok");
            Object value = redisTemplate.opsForValue().get("health:test");
            log.info("Redis check value={}", value);
        } catch (Exception ex) {
            log.error("Redis not available. Start Redis before running app");
            throw ex;
        }
    }
}