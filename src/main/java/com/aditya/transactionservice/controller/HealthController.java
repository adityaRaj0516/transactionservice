package com.aditya.transactionservice.controller;

import com.aditya.transactionservice.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.aditya.transactionservice.entity.Transaction;
import com.aditya.transactionservice.repository.TransactionRepository;
import org.springframework.web.bind.annotation.PostMapping;


import java.util.Map;

//@RestController
//public class HealthController {
//
//    @GetMapping("/health")
//    public Map<String, String> health(){
//        return Map.of(
//               "status", "UP",
//               "service", "Transaction Service"
//        );
//    }
//    @GetMapping("/health")
//    public HealthResponse health(){
//        return new HealthResponse("UP", "Transaction Service");
//    }
//}

@RestController
public class HealthController {

    private final TransactionRepository transactionRepository;

    public HealthController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse(
                "UP",
                "Transaction Service"
        );
    }

    @PostMapping("/test-transaction")
    public String createTestTransaction() {
        Transaction tx = new Transaction(
                1000.0,
                "CREDIT",
                "Test transaction"
        );

        transactionRepository.save(tx);

        return "Transaction saved!";
    }
}
