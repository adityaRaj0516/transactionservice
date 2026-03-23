package com.aditya.transactionservice.service;

import com.aditya.transactionservice.dto.TransferResponse;
import com.aditya.transactionservice.entity.*;
import com.aditya.transactionservice.exception.InvalidTransactionException;
import com.aditya.transactionservice.repository.AccountRepository;
import com.aditya.transactionservice.repository.TransactionRepository;
import com.aditya.transactionservice.repository.TransferRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Service
public class TransferServiceImpl implements TransferService {

    private static final Logger log =
            LoggerFactory.getLogger(TransferServiceImpl.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferRepository transferRepository;

    public TransferServiceImpl(RedisTemplate<String, Object> redisTemplate,
                               AccountRepository accountRepository,
                               TransactionRepository transactionRepository,
                               TransferRepository transferRepository) {
        this.redisTemplate = redisTemplate;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transferRepository = transferRepository;
    }

    @Transactional
    public TransferResponse transfer(Long sourceId, Long targetId,
                                     BigDecimal amount, String idempotencyKey) {

        String redisKey = "idem:" + idempotencyKey;

        Object cached = redisTemplate.opsForValue().get(redisKey);
        if (cached instanceof TransferResponse) {
            return (TransferResponse) cached;
        }

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PROCESSING", Duration.ofMinutes(5));

        if (Boolean.FALSE.equals(acquired)) {
            Object retryCheck = redisTemplate.opsForValue().get(redisKey);
            if (retryCheck instanceof TransferResponse) {
                return (TransferResponse) retryCheck;
            }
            throw new RuntimeException("Request already processing");
        }

        try {
            log.info("Initiating transfer {} -> {} amount {}", sourceId, targetId, amount);

            Optional<Transfer> existing = transferRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return mapToResponse(existing.get());
            }

            if (sourceId.equals(targetId)) {
                throw new InvalidTransactionException("Cannot transfer to same account");
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidTransactionException("Amount must be positive");
            }

            Transfer transfer = new Transfer();
            transfer.setIdempotencyKey(idempotencyKey);
            transfer.setSourceAccountId(sourceId);
            transfer.setTargetAccountId(targetId);
            transfer.setAmount(amount);
            transfer.setStatus(TransactionStatus.INITIATED);

            try {
                transferRepository.save(transfer); // DB-level idempotency guard
            } catch (DataIntegrityViolationException e) {
                Transfer existingTransfer = transferRepository
                        .findByIdempotencyKey(idempotencyKey)
                        .orElseThrow(() -> new RuntimeException("Inconsistent state"));
                return mapToResponse(existingTransfer);
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

            Transaction creditTxn = new Transaction(
                    amount,
                    TransactionType.CREDIT,
                    "Transfer from account " + sourceId
            );
            creditTxn.setAccount(target);
            creditTxn.updateStatus(TransactionStatus.SUCCESS);

            transactionRepository.save(debitTxn);
            transactionRepository.save(creditTxn);

            transfer.setStatus(TransactionStatus.SUCCESS);
            transfer.setTransactionId(debitTxn.getId().toString());
            transferRepository.save(transfer);

            TransferResponse response = mapToResponse(transfer);

            redisTemplate.opsForValue()
                    .set(redisKey, response, Duration.ofMinutes(10));

            return response;

        } catch (Exception e) {
            redisTemplate.delete(redisKey);
            throw e;
        }
    }

    private TransferResponse mapToResponse(Transfer transfer) {
        TransferResponse response = new TransferResponse();
        response.setStatus("SUCCESS");
        response.setMessage("Transfer completed successfully");
        response.setTransactionId(transfer.getTransactionId());
        return response;
    }
}