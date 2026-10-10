package com.oasis.atm;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

/**
 * A bank account. All state is private (encapsulation); balance only changes
 * through validated deposit/withdraw operations, and every change is logged.
 */
public class Account {

    private final String userId;
    private final String holderName;
    private String pinHash;              // PIN is never stored in plain text
    private BigDecimal balance;
    private boolean locked;
    private final List<Transaction> history = new ArrayList<>();

    public Account(String userId, String holderName, String pin, BigDecimal openingBalance) {
        this.userId = userId;
        this.holderName = holderName;
        this.pinHash = hash(pin);
        this.balance = openingBalance;
    }

    // ---------- getters ----------
    public String getUserId() { return userId; }
    public String getHolderName() { return holderName; }
    public BigDecimal getBalance() { return balance; }
    public boolean isLocked() { return locked; }
    public List<Transaction> getHistory() { return Collections.unmodifiableList(history); }

    public void lock() { this.locked = true; }

    public boolean verifyPin(String pin) {
        return pinHash.equals(hash(pin));
    }

    public void changePin(String newPin) {
        this.pinHash = hash(newPin);
        history.add(new Transaction(Transaction.Type.PIN_CHANGE, BigDecimal.ZERO, balance, null));
    }

    // ---------- money operations ----------
    void deposit(BigDecimal amount) {
        balance = balance.add(amount);
        history.add(new Transaction(Transaction.Type.DEPOSIT, amount, balance, null));
    }

    void withdraw(BigDecimal amount) throws InsufficientFundsException {
        ensureFunds(amount);
        balance = balance.subtract(amount);
        history.add(new Transaction(Transaction.Type.WITHDRAW, amount, balance, null));
    }

    void debitTransfer(BigDecimal amount, String toId) throws InsufficientFundsException {
        ensureFunds(amount);
        balance = balance.subtract(amount);
        history.add(new Transaction(Transaction.Type.TRANSFER_OUT, amount, balance, toId));
    }

    void creditTransfer(BigDecimal amount, String fromId) {
        balance = balance.add(amount);
        history.add(new Transaction(Transaction.Type.TRANSFER_IN, amount, balance, fromId));
    }

    private void ensureFunds(BigDecimal amount) throws InsufficientFundsException {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient Funds! Available balance: " + Money.format(balance));
        }
    }

    static String hash(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(pin.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
