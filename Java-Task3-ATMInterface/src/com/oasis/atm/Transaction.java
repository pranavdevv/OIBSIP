package com.oasis.atm;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Immutable record of a single banking transaction.
 */
public class Transaction {

    public enum Type { DEPOSIT, WITHDRAW, TRANSFER_OUT, TRANSFER_IN, PIN_CHANGE }

    private static final AtomicLong SEQ = new AtomicLong(100001);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final long id;
    private final Type type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final String counterparty;   // other account for transfers, may be null
    private final LocalDateTime timestamp;

    public Transaction(Type type, BigDecimal amount, BigDecimal balanceAfter, String counterparty) {
        this.id = SEQ.getAndIncrement();
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.counterparty = counterparty;
        this.timestamp = LocalDateTime.now();
    }

    public long getId() { return id; }
    public Type getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getCounterparty() { return counterparty; }
    public LocalDateTime getTimestamp() { return timestamp; }

    private String describe() {
        switch (type) {
            case TRANSFER_OUT: return "Transfer to " + counterparty;
            case TRANSFER_IN:  return "Transfer from " + counterparty;
            case DEPOSIT:      return "Cash deposit";
            case WITHDRAW:     return "Cash withdrawal";
            case PIN_CHANGE:   return "PIN changed";
            default:           return type.name();
        }
    }

    private String signedAmount() {
        if (type == Type.PIN_CHANGE) return "-";
        boolean credit = type == Type.DEPOSIT || type == Type.TRANSFER_IN;
        return (credit ? "+" : "-") + Money.format(amount);
    }

    @Override
    public String toString() {
        return String.format("| TXN%-7d | %-19s | %-24s | %14s | %14s |",
                id, FMT.format(timestamp), describe(), signedAmount(),
                type == Type.PIN_CHANGE ? "-" : Money.format(balanceAfter));
    }
}
