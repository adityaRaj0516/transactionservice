package com.aditya.transactionservice.idempotency;

import java.util.Optional;

public interface IdempotencyService {

    Optional<IdempotencyRecord> get(String key);

    void validate(String key, String requestHash);

    boolean acquireLock(String key);

    void releaseLock(String key);

    void saveSuccess(String key, IdempotencyRecord record);

    void saveFailure(String key, IdempotencyRecord record);
}