package com.aditya.transactionservice.service;

import com.aditya.transactionservice.dto.TransferRequest;
import com.aditya.transactionservice.dto.TransferResponse;

import java.math.BigDecimal;

public interface TransferService {
    public TransferResponse transfer(TransferRequest request, String idempotencyKey);
}
