package com.aditya.transactionservice.dto;

import com.aditya.transactionservice.entity.TransactionStatus;
import com.aditya.transactionservice.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TransferResponse {

    private Long id;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private String message;
}