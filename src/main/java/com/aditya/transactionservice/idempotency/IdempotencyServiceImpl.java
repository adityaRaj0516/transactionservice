package com.aditya.transactionservice.idempotency;

import com.aditya.transactionservice.exception.DuplicateRequestException;
import com.aditya.transactionservice.exception.TransferProcessingException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyServiceImpl.class);

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LOCK_PREFIX = "lock:";
    private static final long LOCK_TTL = 30; // seconds
    private static final long RECORD_TTL = 10; // minutes

    @Override
    public Optional<IdempotencyRecord> get(String key) {
        log.info("Checking idempotency key={}", key);

        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            log.debug("No idempotency record found key={}", key);
            return Optional.empty();
        }

        try {
            IdempotencyRecord record = (IdempotencyRecord) value;
            log.debug("Idempotency record found key={} status={}", key, record.getStatus());
            return Optional.of(record);
        } catch (ClassCastException ex) {
            log.error("Invalid object type in Redis for key={}", key);
            throw new TransferProcessingException("Corrupted idempotency record", ex);
        }
    }

    @Override
    public void validate(String key, String requestHash) {

        Optional<IdempotencyRecord> existing = get(key);

        if (existing.isEmpty()) {
            return;
        }

        IdempotencyRecord record = existing.get();

        if (record.getRequestHash() == null || !record.getRequestHash().equals(requestHash)) {
            log.error("Idempotency hash mismatch key={} storedHash={} incomingHash={}",
                    key, record.getRequestHash(), requestHash);

            throw new DuplicateRequestException("Idempotency key reused with different request");
        }

        log.debug("Idempotency validated key={}", key);
    }

    @Override
    public boolean acquireLock(String key) {
        String lockKey = LOCK_PREFIX + key;

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "LOCKED", LOCK_TTL, TimeUnit.SECONDS);

        log.info("Lock acquire key={} success={}", key, success);

        return Boolean.TRUE.equals(success);
    }

    @Override
    public void releaseLock(String key) {
        String lockKey = LOCK_PREFIX + key;

        redisTemplate.delete(lockKey);

        log.info("Lock released key={}", key);
    }

    @Override
    public void saveSuccess(String key, IdempotencyRecord record) {
        record.setStatus("SUCCESS");

        redisTemplate.opsForValue().set(key, record, RECORD_TTL, TimeUnit.MINUTES);

        log.info("Idempotency success stored key={} ttl={}min", key, RECORD_TTL);
    }

    @Override
    public void saveFailure(String key, IdempotencyRecord record) {
        record.setStatus("FAILURE");

        redisTemplate.opsForValue().set(key, record, RECORD_TTL, TimeUnit.MINUTES);

        log.info("Idempotency failure stored key={} ttl={}min", key, RECORD_TTL);
    }
}