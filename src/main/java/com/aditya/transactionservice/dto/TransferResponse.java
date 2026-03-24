package com.aditya.transactionservice.dto;

public class TransferResponse {

    private String status;
    private String message;
    private String transactionId;

    public static TransferResponse success(String transactionId) {
        TransferResponse response = new TransferResponse();
        response.setStatus("SUCCESS");
        response.setMessage("Transfer completed successfully");
        response.setTransactionId(transactionId);
        return response;
    }

    public static TransferResponse failure(String message) {
        TransferResponse response = new TransferResponse();
        response.setStatus("FAILED");
        response.setMessage(message);
        return response;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
}