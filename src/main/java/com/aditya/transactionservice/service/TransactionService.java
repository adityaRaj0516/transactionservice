package com.aditya.transactionservice.service;

import com.aditya.transactionservice.dto.TransactionRequest;
import com.aditya.transactionservice.dto.TransferResponse;
import com.aditya.transactionservice.entity.Account;
import com.aditya.transactionservice.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionService {

    Transaction createTransaction(TransactionRequest request);
    Transaction getById(Long id);
    Page<Transaction> getAllTransactions(Pageable pageable);
    Page<Transaction> getTransactionsByAccount(Long accountId, int page, int size);
    Transaction update(Long id, TransactionRequest request);
    void delete(Long id);
    BigDecimal getAccountBalance(Long accountId);
    Account createAccount(Account account);
    TransferResponse transfer(Long sourceId, Long targetId, BigDecimal amount, String idempotencyKey);
}

