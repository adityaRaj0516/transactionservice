package com.aditya.transactionservice.service;

import com.aditya.transactionservice.dto.TransferResponse;
import com.aditya.transactionservice.entity.*;
import com.aditya.transactionservice.exception.DuplicateRequestException;
import com.aditya.transactionservice.exception.InsufficientBalanceException;
import com.aditya.transactionservice.exception.InvalidTransactionException;
import com.aditya.transactionservice.exception.TransferProcessingException;
import com.aditya.transactionservice.idempotency.IdempotencyRecord;
import com.aditya.transactionservice.idempotency.IdempotencyService;
import com.aditya.transactionservice.repository.AccountRepository;
import com.aditya.transactionservice.repository.TransactionRepository;
import com.aditya.transactionservice.repository.TransferRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aditya.transactionservice.dto.TransferRequest;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferOrchestrator {

    private final IdempotencyService idempotencyService;
    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferRepository transferRepository;
    private final EntityManager entityManager;

    @Transactional(rollbackFor = Exception.class)
    public TransferResponse execute(TransferRequest request, String key) {

        Long sourceId = request.getSourceId();
        Long targetId = request.getTargetId();
        BigDecimal amount = request.getAmount();

        String requestHash = sourceId + ":" + targetId + ":" + amount;

        log.info("Transfer initiated source={} target={} amount={} key={}",
                sourceId, targetId, amount, key);

        // ---------- Validation ----------
        if (key == null || key.isBlank()) {
            throw new InvalidTransactionException("Idempotency key required");
        }

        if (sourceId.equals(targetId)) {
            throw new InvalidTransactionException("Cannot transfer to same account");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Amount must be greater than zero");
        }

        try {
            idempotencyService.validate(key, requestHash);
        } catch (DuplicateRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis unavailable during validate key={}", key);
        }

        // ---------- Idempotency check ----------
        Optional<IdempotencyRecord> existing = Optional.empty();
        try {
            existing = idempotencyService.get(key);
        } catch (Exception e) {
            log.warn("Redis unavailable during GET key={}", key);
        }

        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();

            if ("SUCCESS".equals(record.getStatus())) {
                log.info("Idempotent replay (SUCCESS) key={}", key);
                return record.getResponse();
            }

            if ("PROCESSING".equals(record.getStatus())) {
                throw new DuplicateRequestException("In progress");
            }
        }

        boolean lockAcquired = false;

        try {
            // ---------- Lock ----------
            try {
                lockAcquired = idempotencyService.acquireLock(key);
            } catch (Exception e) {
                log.warn("Redis unavailable during LOCK key={}", key);
                lockAcquired = true;
            }

            if (!lockAcquired) {
                throw new DuplicateRequestException("Duplicate request in progress");
            }

            try {
                idempotencyService.saveProcessing(
                        key,
                        new IdempotencyRecord(key, requestHash, null, "PROCESSING")
                );
            } catch (Exception e) {
                log.warn("Redis unavailable during PROCESSING save key={}", key);
            }

            // ---------- DB lock ordering ----------
            Account first = sourceId < targetId
                    ? accountService.getForUpdate(sourceId)
                    : accountService.getForUpdate(targetId);

            Account second = sourceId < targetId
                    ? accountService.getForUpdate(targetId)
                    : accountService.getForUpdate(sourceId);

            Account source = sourceId.equals(first.getId()) ? first : second;
            Account target = targetId.equals(first.getId()) ? first : second;

            // ---------- Business logic ----------
            accountService.debit(source, amount);
            accountService.credit(target, amount);

            accountRepository.save(source);
            accountRepository.save(target);

            Transaction debit = new Transaction(amount, TransactionType.DEBIT, "Transfer");
            debit.setAccount(source);
            debit.updateStatus(TransactionStatus.SUCCESS);

            Transaction credit = new Transaction(amount, TransactionType.CREDIT, "Transfer");
            credit.setAccount(target);
            credit.updateStatus(TransactionStatus.SUCCESS);

            transactionRepository.save(debit);
            transactionRepository.save(credit);

            Transfer transfer = new Transfer();
            transfer.setSourceAccountId(sourceId);
            transfer.setTargetAccountId(targetId);
            transfer.setAmount(amount);
            transfer.setStatus(TransactionStatus.SUCCESS);
            transfer.setTransactionId(String.valueOf(debit.getId()));
            transfer.setIdempotencyKey(key);

            // ---------- DB insert ----------
            try {
                transferRepository.save(transfer);
            } catch (DataIntegrityViolationException dbEx) {

                log.warn("DB duplicate detected key={}", key);

                entityManager.clear();

                throw new DuplicateRequestException("Duplicate request detected");
            }

            // ---------- Success ----------
            TransferResponse response = new TransferResponse();
            response.setId(debit.getId());
            response.setAmount(amount);
            response.setType(TransactionType.DEBIT);
            response.setStatus(TransactionStatus.SUCCESS);

            try {
                idempotencyService.saveSuccess(
                        key,
                        new IdempotencyRecord(key, requestHash, response, "SUCCESS")
                );
            } catch (Exception e) {
                log.warn("Redis unavailable during SUCCESS save key={}", key);
            }

            log.info("Transfer completed id={} key={}", debit.getId(), key);

            return response;

        } catch (InvalidTransactionException | DuplicateRequestException | InsufficientBalanceException e) {
            log.warn("Transfer rejected key={} reason={}", key, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected failure key={} message={}", key, e.getMessage(), e);
            throw new TransferProcessingException("Transfer failed", e);
        } finally {
            if (lockAcquired) {
                try {
                    idempotencyService.releaseLock(key);
                } catch (Exception e) {
                    log.warn("Redis unavailable during lock release key={}", key);
                }
            }
        }
    }
}