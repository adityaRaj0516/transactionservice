package com.aditya.transactionservice.service;

import com.aditya.transactionservice.dto.TransactionRequest;
import com.aditya.transactionservice.entity.Account;
import com.aditya.transactionservice.entity.Transaction;
import com.aditya.transactionservice.entity.TransactionStatus;
import com.aditya.transactionservice.entity.TransactionType;
import com.aditya.transactionservice.exception.DuplicateRequestException;
import com.aditya.transactionservice.exception.InsufficientBalanceException;
import com.aditya.transactionservice.exception.InvalidTransactionException;
import com.aditya.transactionservice.repository.AccountRepository;
import com.aditya.transactionservice.repository.TransactionRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Logger log =
            LoggerFactory.getLogger(TransactionServiceImpl.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

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

        log.info("Transaction request received accountId={} amount={} type={}",
                request.getAccountId(), request.getAmount(), request.getType());

        BigDecimal amount = request.getAmount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid amount {}", amount);
            throw new InvalidTransactionException("Amount must be positive");
        }

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> {
                    log.warn("Account not found id={}", request.getAccountId());
                    return new InvalidTransactionException("Account not found with id: " + request.getAccountId());
                });

        if (request.getType() == TransactionType.DEBIT) {

            if (account.getBalance().compareTo(amount) < 0) {
                log.warn("Insufficient balance accountId={} balance={} requested={}",
                        account.getId(), account.getBalance(), amount);
                throw new InsufficientBalanceException("Insufficient balance");
            }

            account.setBalance(account.getBalance().subtract(amount));
            log.info("Debited amount={} from accountId={}", amount, account.getId());

        } else if (request.getType() == TransactionType.CREDIT) {

            account.setBalance(account.getBalance().add(amount));
            log.info("Credited amount={} to accountId={}", amount, account.getId());

        } else {
            log.warn("Invalid transaction type {}", request.getType());
            throw new InvalidTransactionException("Invalid transaction type");
        }

        accountRepository.save(account);

        Transaction transaction = new Transaction(
                amount,
                request.getType(),
                request.getDescription()
        );

        transaction.setAccount(account);
        transaction.updateStatus(TransactionStatus.SUCCESS);

        Transaction saved = transactionRepository.save(transaction);

        log.info("Transaction success id={}", saved.getId());

        return saved;
    }

    @Override
    public Transaction getById(Long id) {

        log.info("Fetching transaction id={}", id);

        return transactionRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Transaction not found id={}", id);
                    return new InvalidTransactionException("Transaction not found with id: " + id);
                });
    }

    @Override
    public Page<Transaction> getAllTransactions(Pageable pageable) {

        log.info("Fetching transactions page={} size={}",
                pageable.getPageNumber(), pageable.getPageSize());

        return transactionRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public Transaction update(Long id, TransactionRequest request) {

        log.info("Updating transaction id={} amount={} type={}",
                id, request.getAmount(), request.getType());

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Transaction not found id={}", id);
                    return new InvalidTransactionException("Transaction not found with id: " + id);
                });

        BigDecimal amount = request.getAmount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid update amount {}", amount);
            throw new InvalidTransactionException("Amount must be positive");
        }

        transaction.setAmount(amount);
        transaction.setType(request.getType());
        transaction.setDescription(request.getDescription());

        Transaction updated = transactionRepository.save(transaction);

        log.info("Transaction updated id={}", id);

        return updated;
    }

    @Override
    @Transactional
    public void delete(Long id) {

        log.warn("Deleting transaction id={}", id);

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Transaction not found id={}", id);
                    return new InvalidTransactionException("Transaction not found with id: " + id);
                });

        transactionRepository.delete(transaction);

        log.info("Transaction deleted id={}", id);
    }

    @Override
    public Page<Transaction> getTransactionsByAccount(Long accountId, int page, int size) {

        log.info("Fetching transactions accountId={} page={} size={}", accountId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return transactionRepository.findByAccountId(accountId, pageable);
    }

    @Override
    public BigDecimal getAccountBalance(Long accountId) {

        log.info("Fetching balance accountId={}", accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.warn("Account not found id={}", accountId);
                    return new InvalidTransactionException("Account not found with id: " + accountId);
                });

        log.info("Balance accountId={} balance={}", accountId, account.getBalance());

        return account.getBalance();
    }

    @Override
    @Transactional
    public Account createAccount(Account account) {

        log.info("Creating account with balance={}", account.getBalance());

        if (account.getBalance() == null || account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Invalid initial balance {}", account.getBalance());
            throw new InvalidTransactionException("Initial balance cannot be negative");
        }

        Account saved = accountRepository.save(account);

        log.info("Account created id={}", saved.getId());

        return saved;
    }
}