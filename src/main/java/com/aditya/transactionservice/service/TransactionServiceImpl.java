package com.aditya.transactionservice.service;

import com.aditya.transactionservice.dto.TransactionRequest;
import com.aditya.transactionservice.entity.Transaction;
import com.aditya.transactionservice.entity.TransactionStatus;
import com.aditya.transactionservice.exception.InvalidTransactionException;
import com.aditya.transactionservice.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public Transaction createTransaction(TransactionRequest request) {

        Transaction transaction =
                new Transaction(
                        request.getAmount(),
                        request.getType(),
                        request.getDescription()
                );

        transaction = transactionRepository.save(transaction);

        transaction.updateStatus(TransactionStatus.PROCESSING);

        if (transaction.getAmount() < 0) {
            transaction.updateStatus(TransactionStatus.FAILED);
            throw new InvalidTransactionException("Amount must be positive");
        }

        transaction.updateStatus(TransactionStatus.SUCCESS);


        return transactionRepository.save(transaction);
    }

    @Override
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }
}

