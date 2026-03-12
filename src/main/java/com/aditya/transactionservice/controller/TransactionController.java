package com.aditya.transactionservice.controller;

import com.aditya.transactionservice.dto.TransactionRequest;
import com.aditya.transactionservice.entity.Transaction;
import com.aditya.transactionservice.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private static final Logger log =
            LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<Page<Transaction>> getAll(Pageable pageable) {

        log.info("Fetching transactions page {} size {}", pageable.getPageNumber(), pageable.getPageSize());

        return ResponseEntity.ok(
                transactionService.getAllTransactions(pageable)
        );
    }



    @PostMapping
    public ResponseEntity<Transaction> create(
            @Valid @RequestBody TransactionRequest request) {

        log.info("Create transaction request received for account {} amount {}",
                request.getAccountId(), request.getAmount());

        return ResponseEntity.ok(
                transactionService.createTransaction(request)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getById(@PathVariable Long id) {

        log.info("Fetching transaction with id {}", id);

        return ResponseEntity.ok(transactionService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Transaction> update(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request
    ) {

        log.info("Updating transaction {} with amount {}", id, request.getAmount());

        return ResponseEntity.ok(
                transactionService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {

        log.warn("Deleting transaction with id {}", id);

        transactionService.delete(id);
        return ResponseEntity.ok("Transaction deleted successfully");
    }

}

