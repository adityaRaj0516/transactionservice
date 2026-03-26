package com.aditya.transactionservice.service;

import com.aditya.transactionservice.dto.TransactionRequest;
import com.aditya.transactionservice.dto.TransferResponse;
import com.aditya.transactionservice.entity.Account;
import com.aditya.transactionservice.entity.Transaction;
import com.aditya.transactionservice.entity.TransactionStatus;
import com.aditya.transactionservice.entity.TransactionType;
import com.aditya.transactionservice.exception.InvalidTransactionException;
import com.aditya.transactionservice.idempotency.IdempotencyRecord;
import com.aditya.transactionservice.idempotency.IdempotencyService;
import com.aditya.transactionservice.repository.AccountRepository;
import com.aditya.transactionservice.repository.TransactionRepository;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Logger log =
            LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final IdempotencyService idempotencyService;

    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  AccountRepository accountRepository,
                                  AccountService accountService,
                                  IdempotencyService idempotencyService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.idempotencyService = idempotencyService;
    }

    @Override
    @Transactional
    public Transaction createTransaction(TransactionRequest request, String key) {

        log.info("Transaction request accountId={} amount={} type={} key={}",
                request.getAccountId(), request.getAmount(), request.getType(), key);

        if (key == null || key.isBlank()) {
            throw new RuntimeException("Idempotency key required");
        }

        BigDecimal amount = request.getAmount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Amount must be positive");
        }

        String requestHash = generateHash(request);

        Optional<IdempotencyRecord> existing = idempotencyService.get(key);

        if (existing.isPresent()) {
            idempotencyService.validate(key, requestHash);
            log.info("Returning cached transaction for key={}", key);
            return mapToTransaction(existing.get().getResponse());
        }

        if (!idempotencyService.acquireLock(key)) {
            throw new RuntimeException("Duplicate request in progress");
        }

        try {
            Account account = accountService.getForUpdate(request.getAccountId());

            log.info("Balance before={} accountId={}", account.getBalance(), account.getId());

            if (request.getType() == TransactionType.DEBIT) {
                accountService.debit(account, amount);
            } else if (request.getType() == TransactionType.CREDIT) {
                accountService.credit(account, amount);
            } else {
                throw new InvalidTransactionException("Invalid transaction type");
            }

            log.info("Balance after={} accountId={}", account.getBalance(), account.getId());

            accountRepository.save(account);

            Transaction transaction = new Transaction(
                    amount,
                    request.getType(),
                    request.getDescription()
            );

            transaction.setAccount(account);
            transaction.updateStatus(TransactionStatus.SUCCESS);

            Transaction saved = transactionRepository.save(transaction);

            IdempotencyRecord record = new IdempotencyRecord(
                    key,
                    requestHash,
                    mapToResponse(saved),
                    "SUCCESS"
            );

            idempotencyService.saveSuccess(key, record);

            log.info("Transaction success id={} key={}", saved.getId(), key);

            return saved;

        } catch (Exception ex) {

            log.error("Transaction failed key={} error={}", key, ex.getMessage(), ex);

            IdempotencyRecord record = new IdempotencyRecord(
                    key,
                    requestHash,
                    null,
                    "FAILURE"
            );

            idempotencyService.saveFailure(key, record);

            throw ex;

        } finally {
            idempotencyService.releaseLock(key);
        }
    }

    private String generateHash(TransactionRequest request) {
        return request.getAccountId() + ":" +
                request.getAmount() + ":" +
                request.getType();
    }

    private TransferResponse mapToResponse(Transaction transaction) {
        TransferResponse response = new TransferResponse();
        response.setId(transaction.getId());
        response.setAmount(transaction.getAmount());
        response.setType(transaction.getType());
        response.setStatus(transaction.getStatus());
        return response;
    }

    private Transaction mapToTransaction(TransferResponse response) {
        Transaction tx = new Transaction();
        tx.setId(response.getId());
        tx.setAmount(response.getAmount());
        tx.setType(response.getType());
        tx.updateStatus(response.getStatus());
        return tx;
    }

    @Override
    public Transaction getById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new InvalidTransactionException("Transaction not found with id: " + id));
    }

    @Override
    public Page<Transaction> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public Transaction update(Long id, TransactionRequest request) {

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new InvalidTransactionException("Transaction not found with id: " + id));

        BigDecimal amount = request.getAmount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Amount must be positive");
        }

        transaction.setAmount(amount);
        transaction.setType(request.getType());
        transaction.setDescription(request.getDescription());

        return transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public void delete(Long id) {

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new InvalidTransactionException("Transaction not found with id: " + id));

        transactionRepository.delete(transaction);
    }

    @Override
    public Page<Transaction> getTransactionsByAccount(Long accountId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return transactionRepository.findByAccountId(accountId, pageable);
    }

    @Override
    public BigDecimal getAccountBalance(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new InvalidTransactionException("Account not found with id: " + accountId))
                .getBalance();
    }

    @Override
    @Transactional
    public Account createAccount(Account account) {

        if (account.getBalance() == null || account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidTransactionException("Initial balance cannot be negative");
        }

        return accountRepository.save(account);
    }
}