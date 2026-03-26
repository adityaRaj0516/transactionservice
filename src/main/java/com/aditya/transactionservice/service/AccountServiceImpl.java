package com.aditya.transactionservice.service;

import com.aditya.transactionservice.entity.Account;
import com.aditya.transactionservice.exception.InsufficientBalanceException;
import com.aditya.transactionservice.exception.InvalidTransactionException;
import com.aditya.transactionservice.repository.AccountRepository;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public Account getForUpdate(Long accountId) {

        log.info("Fetching account with lock accountId={}", accountId);

        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> {
                    log.warn("Account not found accountId={}", accountId);
                    return new InvalidTransactionException("Account not found with id: " + accountId);
                });

        if (account.getBalance() == null) {
            log.error("Account balance is null accountId={}", accountId);
            throw new InvalidTransactionException("Account balance cannot be null");
        }

        log.debug("Account locked accountId={} balance={}", account.getId(), account.getBalance());

        return account;
    }

    @Override
    public void debit(Account account, BigDecimal amount) {

        log.info("Debit request accountId={} amount={}",
                account != null ? account.getId() : null, amount);

        if (account == null) {
            throw new InvalidTransactionException("Account cannot be null");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Amount must be positive");
        }

        BigDecimal balance = account.getBalance();

        if (balance == null) {
            throw new InvalidTransactionException("Account balance cannot be null");
        }

        log.debug("Before debit accountId={} balance={}", account.getId(), balance);

        if (balance.compareTo(amount) < 0) {
            log.warn("Insufficient balance accountId={} balance={} requested={}",
                    account.getId(), balance, amount);
            throw new InsufficientBalanceException("Insufficient balance");
        }

        account.setBalance(balance.subtract(amount));

        log.info("Debit successful accountId={} newBalance={}",
                account.getId(), account.getBalance());
    }

    @Override
    public void credit(Account account, BigDecimal amount) {

        log.info("Credit request accountId={} amount={}",
                account != null ? account.getId() : null, amount);

        if (account == null) {
            throw new InvalidTransactionException("Account cannot be null");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Amount must be positive");
        }

        BigDecimal balance = account.getBalance();

        if (balance == null) {
            throw new InvalidTransactionException("Account balance cannot be null");
        }

        log.debug("Before credit accountId={} balance={}", account.getId(), balance);

        account.setBalance(balance.add(amount));

        log.info("Credit successful accountId={} newBalance={}",
                account.getId(), account.getBalance());
    }
}