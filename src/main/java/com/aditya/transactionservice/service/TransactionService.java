package com.aditya.transactionservice.service;

import com.aditya.transactionservice.dto.TransactionRequest;
import com.aditya.transactionservice.entity.Transaction;

import java.util.List;

public interface TransactionService {

    Transaction createTransaction(TransactionRequest request);

    List<Transaction> getAllTransactions();
}

