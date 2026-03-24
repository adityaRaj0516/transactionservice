package com.aditya.transactionservice.service;

import com.aditya.transactionservice.dto.TransferResponse;
import com.aditya.transactionservice.entity.*;
import com.aditya.transactionservice.exception.DuplicateRequestException;
import com.aditya.transactionservice.exception.InsufficientBalanceException;
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

    @Override
    @Transactional
    public TransferResponse transfer(Long sourceId, Long targetId,
                                     BigDecimal amount, String idempotencyKey) {

        String redisKey = "idem:" + idempotencyKey;

        log.info("Transfer request source={} target={} amount={} key={}",
                sourceId, targetId, amount, idempotencyKey);

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            log.warn("Missing idempotency key");
            throw new InvalidTransactionException("Idempotency key required");
        }

        Object cached = redisTemplate.opsForValue().get(redisKey);
        if (cached instanceof TransferResponse) {
            log.info("Idempotency cache hit key={}", idempotencyKey);
            return (TransferResponse) cached;
        }

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PROCESSING", Duration.ofMinutes(5));

        if (Boolean.FALSE.equals(acquired)) {
            log.warn("Duplicate request in progress key={}", idempotencyKey);

            Object retry = redisTemplate.opsForValue().get(redisKey);
            if (retry instanceof TransferResponse) {
                return (TransferResponse) retry;
            }

            throw new DuplicateRequestException("Request already processing");
        }

        Transfer transfer = null;

        try {

            Transfer existing = transferRepository.findByIdempotencyKey(idempotencyKey).orElse(null);

            if (existing != null) {

                boolean sameRequest =
                        existing.getSourceAccountId().equals(sourceId) &&
                                existing.getTargetAccountId().equals(targetId) &&
                                existing.getAmount().compareTo(amount) == 0;

                if (!sameRequest) {
                    log.warn("Idempotency key conflict key={} existing=[{} -> {} : {}] incoming=[{} -> {} : {}]",
                            idempotencyKey,
                            existing.getSourceAccountId(),
                            existing.getTargetAccountId(),
                            existing.getAmount(),
                            sourceId,
                            targetId,
                            amount
                    );
                    throw new InvalidTransactionException("Idempotency key reused with different request");
                }

                log.info("DB idempotency hit key={}", idempotencyKey);

                TransferResponse response = mapToResponse(existing);

                redisTemplate.opsForValue()
                        .set(redisKey, response, Duration.ofMinutes(10));

                return response;
            }

            if (sourceId.equals(targetId)) {
                log.warn("Same account transfer sourceId={}", sourceId);
                throw new InvalidTransactionException("Cannot transfer to same account");
            }

            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Invalid amount {}", amount);
                throw new InvalidTransactionException("Amount must be positive");
            }

            transfer = new Transfer();
            transfer.setIdempotencyKey(idempotencyKey);
            transfer.setSourceAccountId(sourceId);
            transfer.setTargetAccountId(targetId);
            transfer.setAmount(amount);
            transfer.setStatus(TransactionStatus.INITIATED);

            try {
                transferRepository.save(transfer);
            } catch (DataIntegrityViolationException e) {
                log.warn("DB duplicate key hit key={}", idempotencyKey);

                Transfer existingTransfer = transferRepository
                        .findByIdempotencyKey(idempotencyKey)
                        .orElseThrow(() ->
                                new InvalidTransactionException("Inconsistent idempotency state"));

                TransferResponse response = mapToResponse(existingTransfer);

                redisTemplate.opsForValue()
                        .set(redisKey, response, Duration.ofMinutes(10));

                return response;
            }

            Account source = accountRepository.findById(sourceId)
                    .orElseThrow(() ->
                            new InvalidTransactionException("Source account not found"));

            Account target = accountRepository.findById(targetId)
                    .orElseThrow(() ->
                            new InvalidTransactionException("Target account not found"));

            if (source.getBalance().compareTo(amount) < 0) {
                log.warn("Insufficient balance sourceId={} balance={} requested={}",
                        sourceId, source.getBalance(), amount);
                throw new InsufficientBalanceException("Insufficient balance");
            }

            source.setBalance(source.getBalance().subtract(amount));
            target.setBalance(target.getBalance().add(amount));

            accountRepository.save(source);
            accountRepository.save(target);
            accountRepository.flush();

            Transaction debit = new Transaction(
                    amount,
                    TransactionType.DEBIT,
                    "Transfer to account " + targetId
            );
            debit.setAccount(source);
            debit.updateStatus(TransactionStatus.SUCCESS);

            Transaction credit = new Transaction(
                    amount,
                    TransactionType.CREDIT,
                    "Transfer from account " + sourceId
            );
            credit.setAccount(target);
            credit.updateStatus(TransactionStatus.SUCCESS);

            transactionRepository.save(debit);
            transactionRepository.save(credit);

            transfer.setStatus(TransactionStatus.SUCCESS);
            transfer.setTransactionId(debit.getId().toString());
            transferRepository.save(transfer);

            TransferResponse response = mapToResponse(transfer);

            redisTemplate.opsForValue()
                    .set(redisKey, response, Duration.ofMinutes(10));

            log.info("Transfer success txnId={} key={}",
                    transfer.getTransactionId(), idempotencyKey);

            return response;

        } catch (Exception ex) {

            log.error("Transfer failed key={} reason={}", idempotencyKey, ex.getMessage());

            if (transfer != null) {
                transfer.setStatus(TransactionStatus.FAILED);
                transferRepository.save(transfer);
            }

            redisTemplate.delete(redisKey);

            throw ex;
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