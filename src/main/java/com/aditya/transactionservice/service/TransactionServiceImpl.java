package com.aditya.transactionservice.service;

import com.aditya.transactionservice.dto.TransactionRequest;
import com.aditya.transactionservice.entity.Account;
import com.aditya.transactionservice.entity.Transaction;
import com.aditya.transactionservice.entity.TransactionStatus;
import com.aditya.transactionservice.entity.TransactionType;
import com.aditya.transactionservice.exception.InvalidTransactionException;
import com.aditya.transactionservice.repository.AccountRepository;
import com.aditya.transactionservice.repository.TransactionRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Duration;

@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final Logger log =
            LoggerFactory.getLogger(TransactionServiceImpl.class);

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

        log.info("Creating transaction for account {} amount {} type {}",
                request.getAccountId(),
                request.getAmount(),
                request.getType());

        BigDecimal amount = request.getAmount();

        if(amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid transaction amount {}", request.getAmount());
            throw new InvalidTransactionException("Amount must be positive");
        }

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> {
                    log.warn("Account not found with id {}", request.getAccountId());
                    return new InvalidTransactionException(
                            "Account not found with id: " + request.getAccountId());
                });

        if (request.getType() == TransactionType.CREDIT) {

            account.setBalance(account.getBalance().add(amount));
            log.info("Credited {} to account {}", amount, account.getId());

        } else if (request.getType() == TransactionType.DEBIT) {

            if (account.getBalance().compareTo(amount) < 0) {
                log.warn("Insufficient balance for account {} current balance {} requested {}",
                        account.getId(),
                        account.getBalance(),
                        amount);

                throw new InvalidTransactionException("Insufficient balance");
            }

            account.setBalance(account.getBalance().subtract(amount));
            log.info("Debited {} from account {}", amount, account.getId());
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

        log.info("Transaction completed successfully with id {}", saved.getId());

        return saved;
    }

    @Transactional
    public void transfer(Long sourceId, Long targetId, BigDecimal amount, String idempotencyKey) {

        String lockKey = "idem:" + idempotencyKey;

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "PROCESSING", Duration.ofMinutes(5));

        if (Boolean.FALSE.equals(acquired)) {
            throw new RuntimeException("Duplicate request or already processing");
        }

        try {
            log.info("Initiating transfer {} -> {} amount {}", sourceId, targetId, amount);

            if (sourceId.equals(targetId)) {
                throw new InvalidTransactionException("Cannot transfer to same account");
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidTransactionException("Amount must be positive");
            }

            Account source = accountRepository.findById(sourceId)
                    .orElseThrow(() -> new InvalidTransactionException("Source account not found"));

            Account target = accountRepository.findById(targetId)
                    .orElseThrow(() -> new InvalidTransactionException("Target account not found"));

            if (source.getBalance().compareTo(amount) < 0) {
                throw new InvalidTransactionException("Insufficient balance");
            }

            source.setBalance(source.getBalance().subtract(amount));
            target.setBalance(target.getBalance().add(amount));

            accountRepository.save(source);
            accountRepository.save(target);
            accountRepository.flush();

            Transaction debitTxn = new Transaction(
                    amount,
                    TransactionType.DEBIT,
                    "Transfer to account " + targetId
            );
            debitTxn.setAccount(source);
            debitTxn.updateStatus(TransactionStatus.SUCCESS);
            debitTxn.setIdempotencyKey(idempotencyKey + "_DEBIT");

            Transaction creditTxn = new Transaction(
                    amount,
                    TransactionType.CREDIT,
                    "Transfer from account " + sourceId
            );
            creditTxn.setAccount(target);
            creditTxn.updateStatus(TransactionStatus.SUCCESS);
            creditTxn.setIdempotencyKey(idempotencyKey + "_CREDIT");

            transactionRepository.save(debitTxn);
            transactionRepository.save(creditTxn);

            redisTemplate.opsForValue()
                    .set(lockKey, "COMPLETED", Duration.ofMinutes(10));

            log.info("Transfer completed successfully");

        } catch (Exception e) {
            redisTemplate.delete(lockKey);
            throw e;
        }
    }

    @Override
    public Transaction getById(Long id) {

        log.info("Fetching transaction with id {}", id);

        return transactionRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Transaction not found with id {}", id);
                    return new InvalidTransactionException(
                            "Transaction not found with id: " + id);
                });
    }

    @Override
    public Page<Transaction> getAllTransactions(Pageable pageable) {

        log.info("Fetching transactions page {} size {}",
                pageable.getPageNumber(),
                pageable.getPageSize());

        return transactionRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public Transaction update(Long id, TransactionRequest request) {

        log.info("Updating transaction {} with amount {} type {}",
                id,
                request.getAmount(),
                request.getType());

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Transaction not found for update id {}", id);
                    return new InvalidTransactionException("Transaction not found with id: " + id);
                });

        BigDecimal amount = request.getAmount();

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid update amount {}", request.getAmount());
            throw new InvalidTransactionException("Amount must be positive");
        }

        transaction.setAmount(amount);
        transaction.setType(request.getType());
        transaction.setDescription(request.getDescription());

        Transaction updated = transactionRepository.save(transaction);

        log.info("Transaction {} updated successfully", id);

        return updated;
    }

    @Override
    @Transactional
    public void delete(Long id) {

        log.warn("Deleting transaction with id {}", id);

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Transaction not found for deletion id {}", id);
                    return new InvalidTransactionException(
                            "Transaction not found with id: " + id);
                });

        transactionRepository.delete(transaction);

        log.info("Transaction {} deleted successfully", id);
    }

    @Override
    public Page<Transaction> getTransactionsByAccount(Long accountId, int page, int size) {

        log.info("Fetching transactions for account {} page {} size {}", accountId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return transactionRepository.findByAccountId(accountId, pageable);
    }

    @Override
    public BigDecimal getAccountBalance(Long accountId) {

        log.info("Fetching balance for account {}", accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.warn("Account not found for balance check {}", accountId);
                    return new InvalidTransactionException(
                            "Account not found with id: " + accountId);
                });

        log.info("Current balance for account {} is {}", accountId, account.getBalance());

        return account.getBalance();
    }

    @Override
    @Transactional
    public Account createAccount(Account account) {

        log.info("Creating account with initial balance {}", account.getBalance());

        if (account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Attempted to create account with negative balance {}", account.getBalance());
            throw new InvalidTransactionException("Initial balance cannot be negative");
        }

        Account saved = accountRepository.save(account);

        log.info("Account created successfully with id {}", saved.getId());

        return saved;
    }
}