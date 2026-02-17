package com.aditya.transactionservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private BigDecimal balance;

    public Account() {}

    public Account(String name, BigDecimal balance) {
        this.name = name;
        this.balance = balance;
    }

    public Long getId() { return id; }

    public String getName() { return name; }

    public BigDecimal getBalance() { return balance; }

    public void setName(String name) { this.name = name; }

    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
