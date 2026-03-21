package com.aditya.transactionservice.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

@Configuration
//@Profile("!test")
public class RedisConfig {

    private RedisServer redisServer;

//    @PostConstruct
//    public void startRedis() throws Exception {
//        redisServer = new RedisServer(6379);
//        redisServer.start();
//    }

//    @PreDestroy
//    public void stopRedis() {
//        if (redisServer != null) {
//            redisServer.stop();
//        }
//    }
}