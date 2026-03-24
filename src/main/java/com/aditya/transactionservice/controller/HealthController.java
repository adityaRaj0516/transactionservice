package com.aditya.transactionservice.controller;

import com.aditya.transactionservice.dto.HealthResponse;
import com.aditya.transactionservice.repository.AccountRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final AccountRepository accountRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public HealthController(AccountRepository accountRepository,
                            RedisTemplate<String, Object> redisTemplate) {
        this.accountRepository = accountRepository;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/health")
    public HealthResponse health() {

        String dbStatus;
        String redisStatus;

        try {
            accountRepository.count();
            dbStatus = "UP";
        } catch (Exception ex) {
            dbStatus = "DOWN";
        }

        try {
            redisTemplate.opsForValue().set("health:test", "ok");
            redisTemplate.opsForValue().get("health:test");
            redisStatus = "UP";
        } catch (Exception ex) {
            redisStatus = "DOWN";
        }

        String overall =
                ("UP".equals(dbStatus) && "UP".equals(redisStatus))
                        ? "UP"
                        : "DEGRADED";

        return new HealthResponse(
                overall,
                "Transaction Service",
                dbStatus,
                redisStatus
        );
    }
}