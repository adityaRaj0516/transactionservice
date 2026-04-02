package com.aditya.transactionservice.service;

import com.aditya.transactionservice.dto.TransferRequest;
import com.aditya.transactionservice.dto.TransferResponse;

import com.aditya.transactionservice.exception.InvalidTransactionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final TransferOrchestrator orchestrator;

    @Override
    public TransferResponse transfer(TransferRequest request, String idempotencyKey) {

        if (request.getSourceId().equals(request.getTargetId())) {
            throw new InvalidTransactionException("Source and target cannot be same");
        }

        return orchestrator.execute(request, idempotencyKey);
    }
}