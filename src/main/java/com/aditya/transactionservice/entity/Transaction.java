package com.aditya.transactionservice.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaction")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Double amount;
    private String type;
    private String description;
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;


    public Transaction() {

    }

    public Transaction(Double amount, String type, String description) {
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.status = TransactionStatus.INITIATED;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Double getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void updateStatus(TransactionStatus newStatus) {

        if (this.status == TransactionStatus.SUCCESS ||
                this.status == TransactionStatus.FAILED) {

            throw new IllegalStateException("Cannot change status from terminal state");
        }

        this.status = newStatus;
    }

}
