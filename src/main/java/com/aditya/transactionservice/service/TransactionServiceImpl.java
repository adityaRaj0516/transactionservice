package com.aditya.transactionservice.service;

import com.aditya.transactionservice.dto.TransactionRequest;
import com.aditya.transactionservice.entity.Transaction;
import com.aditya.transactionservice.entity.TransactionStatus;
import com.aditya.transactionservice.exception.InvalidTransactionException;
import com.aditya.transactionservice.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

        if (request.getAmount() <= 0) {
            throw new InvalidTransactionException("Amount must be positive");
        }

        Transaction transaction = new Transaction(
                request.getAmount(),
                request.getType(),
                request.getDescription()
        );

        transaction.updateStatus(TransactionStatus.SUCCESS);

        return transactionRepository.save(transaction);
    }

    @Override
    public Transaction getById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() ->
                        new InvalidTransactionException(
                                "Transaction not found with id: " + id
                        )
                );
    }


    @Override
    public Page<Transaction> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }


    @Override
    @Transactional
    public Transaction update(Long id, TransactionRequest request) {

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() ->
                        new InvalidTransactionException(
                                "Transaction not found with id: " + id
                        )
                );

        if (request.getAmount() <= 0) {
            throw new InvalidTransactionException("Amount must be positive");
        }

        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setDescription(request.getDescription());

        return transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public void delete(Long id) {

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() ->
                        new InvalidTransactionException(
                                "Transaction not found with id: " + id
                        )
                );

        transactionRepository.delete(transaction);
    }

}

