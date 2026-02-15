package com.aditya.transactionservice.controller;

import com.aditya.transactionservice.dto.TransactionRequest;
import com.aditya.transactionservice.entity.Transaction;
import com.aditya.transactionservice.service.TransactionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public Transaction create(@RequestBody TransactionRequest request) {
        return transactionService.createTransaction(request);
    }
}

