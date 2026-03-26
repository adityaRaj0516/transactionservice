package com.aditya.transactionservice.idempotency;

import com.aditya.transactionservice.dto.TransferResponse;

public class IdempotencyRecord {

    private String key;
    private String requestHash;
    private TransferResponse response;
    private String status; // SUCCESS / FAILURE

    public IdempotencyRecord() {}

    public IdempotencyRecord(String key, String requestHash, TransferResponse response, String status) {
        this.key = key;
        this.requestHash = requestHash;
        this.response = response;
        this.status = status;
    }

    public String getKey() { return key; }
    public String getRequestHash() { return requestHash; }
    public TransferResponse getResponse() { return response; }
    public String getStatus() { return status; }

    public void setKey(String key) { this.key = key; }
    public void setRequestHash(String requestHash) { this.requestHash = requestHash; }
    public void setResponse(TransferResponse response) { this.response = response; }
    public void setStatus(String status) { this.status = status; }

}