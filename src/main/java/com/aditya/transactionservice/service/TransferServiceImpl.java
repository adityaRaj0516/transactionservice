package com.aditya.transactionservice.service;

import com.aditya.transactionservice.dto.TransferRequest;
import com.aditya.transactionservice.dto.TransferResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final TransferOrchestrator orchestrator;

    @Override
    public TransferResponse transfer(TransferRequest request, String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new RuntimeException("Idempotency key required");
        }

        return orchestrator.execute(request, idempotencyKey);
    }
}