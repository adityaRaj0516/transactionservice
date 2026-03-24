package com.aditya.transactionservice.dto;

public class HealthResponse {

    private String status;
    private String service;
    private String database;
    private String redis;

    public HealthResponse(String status, String service, String database, String redis) {
        this.status = status;
        this.service = service;
        this.database = database;
        this.redis = redis;
    }

    public String getStatus() {
        return status;
    }

    public String getService() {
        return service;
    }

    public String getDatabase() {
        return database;
    }

    public String getRedis() {
        return redis;
    }
}