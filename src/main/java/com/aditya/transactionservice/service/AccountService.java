package com.aditya.transactionservice.service;

import com.aditya.transactionservice.entity.Account;

import java.math.BigDecimal;

public interface AccountService {

    Account getForUpdate(Long accountId);

    void debit(Account account, BigDecimal amount);

    void credit(Account account, BigDecimal amount);
}
