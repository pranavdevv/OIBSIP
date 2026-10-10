package com.oasis.atm;

/** Thrown when an account does not have enough balance for a debit. */
public class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
