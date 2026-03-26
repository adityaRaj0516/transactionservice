package com.aditya.transactionservice.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    public Transaction() {
    }

    public Transaction(BigDecimal amount, TransactionType type, String description) {
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.status = TransactionStatus.INITIATED;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = TransactionStatus.INITIATED;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionType getType() {
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

    public Account getAccount() {
        return account;
    }

    public void updateStatus(TransactionStatus newStatus) {

        if (this.status == TransactionStatus.SUCCESS ||
                this.status == TransactionStatus.FAILED) {
            throw new IllegalStateException("Cannot change status from terminal state");
        }

        this.status = newStatus;
    }
}