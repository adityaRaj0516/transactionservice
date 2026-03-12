package com.aditya.transactionservice.controller;

import com.aditya.transactionservice.dto.ApiResponse;
import com.aditya.transactionservice.entity.Account;
import com.aditya.transactionservice.entity.Transaction;
import com.aditya.transactionservice.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private static final Logger log =
            LoggerFactory.getLogger(AccountController.class);

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
    public ResponseEntity<ApiResponse<Account>> createAccount(@RequestBody Account account) {

        log.info("Create account request received with balance {}", account.getBalance());

        Account created = transactionService.createAccount(account);

        log.info("Account created successfully with id {}", created.getId());

        ApiResponse<Account> response =
                new ApiResponse<>(true, "Account created successfully", created);

        return ResponseEntity.ok(response);
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