package com.oasis.atm;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The Bank owns all accounts and enforces business rules
 * (limits, transfers between accounts).
 */
public class Bank {

    public static final BigDecimal DAILY_WITHDRAW_LIMIT = new BigDecimal("25000.00");
    public static final BigDecimal PER_TXN_LIMIT = new BigDecimal("10000.00");

    private final String name;
    private final Map<String, Account> accounts = new LinkedHashMap<>();

    public Bank(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public void addAccount(Account account) {
        accounts.put(account.getUserId(), account);
    }

    public Account find(String userId) {
        return accounts.get(userId == null ? null : userId.trim());
    }

    public void deposit(Account acc, BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("50000")) > 0) {
            throw new IllegalArgumentException("ATM deposits are limited to " + Money.format(new BigDecimal("50000")) + " per transaction.");
        }
        acc.deposit(amount);
    }

    public void withdraw(Account acc, BigDecimal amount, BigDecimal withdrawnToday) throws InsufficientFundsException {
        if (amount.remainder(new BigDecimal("100")).signum() != 0) {
            throw new IllegalArgumentException("Withdrawals must be in multiples of Rs.100 (notes available: 100/200/500).");
        }
        if (amount.compareTo(PER_TXN_LIMIT) > 0) {
            throw new IllegalArgumentException("Per-transaction limit is " + Money.format(PER_TXN_LIMIT) + ".");
        }
        if (withdrawnToday.add(amount).compareTo(DAILY_WITHDRAW_LIMIT) > 0) {
            throw new IllegalArgumentException("Daily withdrawal limit of " + Money.format(DAILY_WITHDRAW_LIMIT)
                    + " exceeded. Remaining today: " + Money.format(DAILY_WITHDRAW_LIMIT.subtract(withdrawnToday)));
        }
        acc.withdraw(amount);
    }

    /** Transfers money; both accounts are updated and both get a history entry. */
    public void transfer(Account from, String toId, BigDecimal amount) throws InsufficientFundsException {
        Account to = find(toId);
        if (to == null) {
            throw new IllegalArgumentException("Recipient account '" + toId + "' does not exist.");
        }
        if (to == from) {
            throw new IllegalArgumentException("You cannot transfer to your own account.");
        }
        from.debitTransfer(amount, to.getUserId());   // throws before any change if funds are low
        to.creditTransfer(amount, from.getUserId());
    }
}
