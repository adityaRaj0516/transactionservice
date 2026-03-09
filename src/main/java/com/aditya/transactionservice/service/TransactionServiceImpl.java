package com.aditya.transactionservice.service;

import com.aditya.transactionservice.dto.TransactionRequest;
import com.aditya.transactionservice.entity.Account;
import com.aditya.transactionservice.entity.Transaction;
import com.aditya.transactionservice.entity.TransactionStatus;
import com.aditya.transactionservice.entity.TransactionType;
import com.aditya.transactionservice.exception.InvalidTransactionException;
import com.aditya.transactionservice.repository.AccountRepository;
import com.aditya.transactionservice.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;


    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }


    @Override
    @Transactional
    public Transaction createTransaction(TransactionRequest request) {

        if (request.getAmount() <= 0) {
            throw new InvalidTransactionException("Amount must be positive");
        }

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() ->
                        new InvalidTransactionException(
                                "Account not found with id: " + request.getAccountId()
                        )
                );

        BigDecimal amount = BigDecimal.valueOf(request.getAmount());

        if (request.getType() == TransactionType.CREDIT) {

            account.setBalance(account.getBalance().add(amount));

        } else if (request.getType() == TransactionType.DEBIT) {

            if (account.getBalance().compareTo(amount) < 0) {
                throw new InvalidTransactionException("Insufficient balance");
            }

            account.setBalance(account.getBalance().subtract(amount));
        }

        Transaction transaction = new Transaction(
                request.getAmount(),
                request.getType(),
                request.getDescription()
        );

        transaction.setAccount(account);
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

    @Override
    public Page<Transaction> getTransactionsByAccount(Long accountId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return transactionRepository.findByAccountId(accountId, pageable);

    }

    @Override
    public BigDecimal getAccountBalance(Long accountId) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new InvalidTransactionException(
                                "Account not found with id: " + accountId
                        )
                );

        return account.getBalance();
    }

    @Override
    @Transactional
    public Account createAccount(Account account) {

        if (account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidTransactionException("Initial balance cannot be negative");
        }

        return accountRepository.save(account);
    }

}

