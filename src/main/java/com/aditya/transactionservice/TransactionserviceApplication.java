package com.aditya.transactionservice;

import com.aditya.transactionservice.service.RedisTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TransactionserviceApplication implements CommandLineRunner {

	@Autowired
	private RedisTestService redisTestService;

	public static void main(String[] args) {
		SpringApplication.run(TransactionserviceApplication.class, args);
	}

	@Override
	public void run(String... args) {
		redisTestService.testRedis();
	}
}
