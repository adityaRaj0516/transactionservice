package com.aditya.transactionservice.service;

import com.aditya.transactionservice.dto.TransferResponse;

import java.math.BigDecimal;

public interface TransferService {
    TransferResponse transfer(Long sourceId, Long targetId, BigDecimal amount, String key);
}
