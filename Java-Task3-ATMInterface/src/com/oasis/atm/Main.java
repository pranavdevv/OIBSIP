package com.oasis.atm;

import java.math.BigDecimal;
import java.util.Scanner;

/**
 * Entry point. Seeds the bank with demo accounts and starts ATM sessions.
 *
 * Demo credentials:
 *   User ID 1001 / PIN 1234  (Pranav)
 *   User ID 1002 / PIN 4321  (Ananya)
 *   User ID 1003 / PIN 1111  (Rahul)
 */
public class Main {

    public static void main(String[] args) {
        Bank bank = new Bank("Oasis Bank");
        bank.addAccount(new Account("1001", "Pranav", "1234", new BigDecimal("50000.00")));
        bank.addAccount(new Account("1002", "Ananya", "4321", new BigDecimal("32000.00")));
        bank.addAccount(new Account("1003", "Rahul",  "1111", new BigDecimal("1500.00")));

        Scanner sc = new Scanner(System.in);
        ATM atm = new ATM(bank, sc);

        // Allows multiple customers to use the machine one after another.
        while (true) {
            atm.start();
            System.out.print("\n  Next customer? (y/n): ");
            if (!sc.hasNextLine() || !sc.nextLine().trim().equalsIgnoreCase("y")) break;
        }
        System.out.println("  ATM shutting down.");
    }
}
