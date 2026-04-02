package com.aditya.transactionservice.service;

import com.aditya.transactionservice.dto.TransferResponse;
import com.aditya.transactionservice.entity.*;
import com.aditya.transactionservice.exception.DuplicateRequestException;
import com.aditya.transactionservice.exception.InvalidTransactionException;
import com.aditya.transactionservice.exception.TransferProcessingException;
import com.aditya.transactionservice.idempotency.IdempotencyRecord;
import com.aditya.transactionservice.idempotency.IdempotencyService;
import com.aditya.transactionservice.repository.AccountRepository;
import com.aditya.transactionservice.repository.TransactionRepository;
import com.aditya.transactionservice.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Transactional(rollbackFor = Exception.class)
    public TransferResponse execute(TransferRequest request, String key) {

        Long sourceId = request.getSourceId();
        Long targetId = request.getTargetId();
        BigDecimal amount = request.getAmount();

        String requestHash = sourceId + ":" + targetId + ":" + amount;

        log.info("Transfer request received: {} -> {} amount={} key={}",
                sourceId, targetId, amount, key);

        if (key == null || key.isBlank()) {
            throw new InvalidTransactionException("Idempotency key required");
        }

        if (sourceId.equals(targetId)) {
            throw new InvalidTransactionException("Cannot transfer to same account");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Amount must be greater than zero");
        }

        idempotencyService.validate(key, requestHash);

        Optional<IdempotencyRecord> existing = idempotencyService.get(key);

        if (existing.isPresent()) {
            log.info("Returning cached transfer key={}", key);
            return existing.get().getResponse();
        }

        boolean lockAcquired = false;

        try {

            lockAcquired = idempotencyService.acquireLock(key);

            if (!lockAcquired) {
                throw new DuplicateRequestException("Duplicate request in progress");
            }
            // Lock ordering
            Account first = sourceId < targetId
                    ? accountService.getForUpdate(sourceId)
                    : accountService.getForUpdate(targetId);

            Account second = sourceId < targetId
                    ? accountService.getForUpdate(targetId)
                    : accountService.getForUpdate(sourceId);

            Account source = sourceId.equals(first.getId()) ? first : second;
            Account target = targetId.equals(first.getId()) ? first : second;

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

            transferRepository.save(transfer);

            TransferResponse response = new TransferResponse();
            response.setId(debit.getId());
            response.setAmount(amount);
            response.setType(TransactionType.DEBIT);
            response.setStatus(TransactionStatus.SUCCESS);

            IdempotencyRecord record =
                    new IdempotencyRecord(key, requestHash, response, "SUCCESS");

            idempotencyService.saveSuccess(key, record);

            log.info("Transfer completed id={} key={}", debit.getId(), key);

            return response;

        } catch (InvalidTransactionException | DuplicateRequestException ex) {

            log.error("Transfer failed key={} error={}", key, ex.getMessage(), ex);

            IdempotencyRecord record =
                    new IdempotencyRecord(key, requestHash, null, "FAILURE");

            idempotencyService.saveFailure(key, record);

            throw ex;

        } catch (Exception ex) {

            log.error("Transfer failed key={} error={}", key, ex.getMessage(), ex);

            IdempotencyRecord record =
                    new IdempotencyRecord(key, requestHash, null, "FAILURE");

            idempotencyService.saveFailure(key, record);

            throw new TransferProcessingException("Transfer failed", ex);
        } finally {
            if (lockAcquired) {
                idempotencyService.releaseLock(key);
            }
        }
    }
}