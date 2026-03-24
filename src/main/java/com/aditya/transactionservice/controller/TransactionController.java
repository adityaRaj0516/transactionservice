package com.aditya.transactionservice.controller;

import com.aditya.transactionservice.dto.TransactionRequest;
import com.aditya.transactionservice.dto.TransferRequest;
import com.aditya.transactionservice.dto.TransferResponse;
import com.aditya.transactionservice.entity.Transaction;
import com.aditya.transactionservice.service.TransactionService;
import com.aditya.transactionservice.service.TransferService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private static final Logger log =
            LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService transactionService;
    private final TransferService transferService;

    public TransactionController(TransactionService transactionService,
                                 TransferService transferService) {
        this.transactionService = transactionService;
        this.transferService = transferService;
    }

    @GetMapping
    public ResponseEntity<Page<Transaction>> getAll(Pageable pageable) {

        log.info("Fetching transactions page={} size={}",
                pageable.getPageNumber(), pageable.getPageSize());

        return ResponseEntity.ok(
                transactionService.getAllTransactions(pageable)
        );
    }

    @PostMapping
    public ResponseEntity<Transaction> create(
            @Valid @RequestBody TransactionRequest request) {

        log.info("Create transaction accountId={} amount={}",
                request.getAccountId(), request.getAmount());

        return ResponseEntity.ok(
                transactionService.createTransaction(request)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getById(@PathVariable Long id) {

        log.info("Fetching transaction id={}", id);

        return ResponseEntity.ok(
                transactionService.getById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Transaction> update(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request) {

        log.info("Updating transaction id={} amount={}",
                id, request.getAmount());

        return ResponseEntity.ok(
                transactionService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        log.warn("Deleting transaction id={}", id);

        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(
            @RequestHeader("Idempotency-Key") @NotBlank String key,
            @Valid @RequestBody TransferRequest request) {

        log.info("Transfer request source={} target={} amount={} key={}",
                request.getSourceId(),
                request.getTargetId(),
                request.getAmount(),
                key);

        TransferResponse response = transferService.transfer(
                request.getSourceId(),
                request.getTargetId(),
                request.getAmount(),
                key
        );

        return ResponseEntity.ok(response);
    }
}