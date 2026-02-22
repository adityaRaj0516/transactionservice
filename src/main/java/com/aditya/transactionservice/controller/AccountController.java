package com.aditya.transactionservice.controller;

import com.aditya.transactionservice.entity.Account;
import com.aditya.transactionservice.entity.Transaction;
import com.aditya.transactionservice.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final TransactionService transactionService;

    public AccountController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable Long id) {
        return ResponseEntity.ok(
                transactionService.getAccountBalance(id)
        );
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody BigDecimal initialBalance) {
        return ResponseEntity.ok(
                transactionService.createAccount(initialBalance)
        );
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<Page<Transaction>> getTransactionsByAccount(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                transactionService.getTransactionsByAccount(id, page, size)
        );
    }

}