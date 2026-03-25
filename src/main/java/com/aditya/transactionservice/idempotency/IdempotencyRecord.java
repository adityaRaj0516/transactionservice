package com.aditya.transactionservice.idempotency;

import com.aditya.transactionservice.dto.TransferResponse;

public class IdempotencyRecord {

    private String hash;
    private TransferResponse response;

    public IdempotencyRecord() {}

    public IdempotencyRecord(String hash, TransferResponse response) {
        this.hash = hash;
        this.response = response;
    }

    public String getHash() { return hash; }
    public TransferResponse getResponse() { return response; }

    public void setHash(String hash) { this.hash = hash; }
    public void setResponse(TransferResponse response) { this.response = response; }
}
