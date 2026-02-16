package com.aditya.transactionservice.service;

import com.aditya.transactionservice.dto.TransactionRequest;
import com.aditya.transactionservice.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TransactionService {

    Transaction createTransaction(TransactionRequest request);
    Transaction getById(Long id);
    Page<Transaction> getAllTransactions(Pageable pageable);
    Transaction update(Long id, TransactionRequest request);
    void delete(Long id);
}

