package com.oasis.atm;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

/**
 * The ATM is the console user interface. It authenticates the user and
 * drives the menu; all money rules are delegated to {@link Bank}.
 */
public class ATM {

    private static final int MAX_PIN_ATTEMPTS = 3;
    private static final String LINE = "+------------+---------------------+--------------------------+----------------+----------------+";

    private final Bank bank;
    private final Scanner in;

    // session state
    private Account current;
    private int sessionStartIndex;
    private BigDecimal withdrawnToday = BigDecimal.ZERO;

    public ATM(Bank bank, Scanner in) {
        this.bank = bank;
        this.in = in;
    }

    public void start() {
        banner();
        if (!login()) {
            System.out.println("\n  Too many failed attempts. Access denied. Please contact your branch.");
            return;
        }
        sessionStartIndex = current.getHistory().size();
        System.out.println("\n  Welcome, " + current.getHolderName() + "!");
        menuLoop();
    }

    // ---------------- authentication ----------------
    private boolean login() {
        for (int attempt = 1; attempt <= MAX_PIN_ATTEMPTS; attempt++) {
            String id = prompt("  Enter User ID : ");
            String pin = promptSecret("  Enter PIN     : ");
            Account acc = bank.find(id);

            if (acc != null && acc.isLocked()) {
                System.out.println("  This card is blocked due to repeated wrong PINs.");
                return false;
            }
            if (acc != null && acc.verifyPin(pin)) {
                current = acc;
                return true;
            }
            int left = MAX_PIN_ATTEMPTS - attempt;
            System.out.println("  Invalid User ID or PIN." + (left > 0 ? " Attempts left: " + left : ""));
            if (left == 0 && acc != null) acc.lock();
        }
        return false;
    }

    // ---------------- menu ----------------
    private void menuLoop() {
        while (true) {
            System.out.println();
            System.out.println("  ==================== MAIN MENU ====================");
            System.out.println("   1. Transaction History");
            System.out.println("   2. Withdraw");
            System.out.println("   3. Deposit");
            System.out.println("   4. Transfer");
            System.out.println("   5. Quit");
            System.out.println("   ---------------- extras ----------------");
            System.out.println("   6. Balance Enquiry");
            System.out.println("   7. Change PIN");
            System.out.println("   8. Full Mini-Statement (all-time)");
            System.out.println("  ===================================================");
            String choice = prompt("  Select an option: ");

            switch (choice) {
                case "1": showHistory(true); break;
                case "2": withdraw(); break;
                case "3": deposit(); break;
                case "4": transfer(); break;
                case "5":
                    printReceiptSummary();
                    System.out.println("\n  Thank you for banking with " + bank.getName() + ". Goodbye, "
                            + current.getHolderName() + "! Please collect your card.");
                    return;
                case "6": System.out.println("\n  Available balance: " + Money.format(current.getBalance())); break;
                case "7": changePin(); break;
                case "8": showHistory(false); break;
                default: System.out.println("  Invalid option. Please choose 1-8.");
            }
        }
    }

    private void showHistory(boolean sessionOnly) {
        List<Transaction> all = current.getHistory();
        List<Transaction> list = sessionOnly ? all.subList(sessionStartIndex, all.size()) : all;
        System.out.println("\n  " + (sessionOnly ? "TRANSACTION HISTORY (this session)" : "MINI-STATEMENT (all-time)"));
        if (list.isEmpty()) {
            System.out.println("  No transactions yet.");
            return;
        }
        System.out.println(LINE);
        System.out.printf("| %-10s | %-19s | %-24s | %14s | %14s |%n", "Txn ID", "Date & Time", "Description", "Amount", "Balance");
        System.out.println(LINE);
        list.forEach(System.out::println);
        System.out.println(LINE);
    }

    private void withdraw() {
        BigDecimal amount = askAmount("  Enter amount to withdraw (multiples of 100): Rs.");
        if (amount == null) return;
        try {
            bank.withdraw(current, amount, withdrawnToday);
            withdrawnToday = withdrawnToday.add(amount);
            System.out.println("  Please collect your cash: " + Money.format(amount));
            System.out.println("  Remaining balance: " + Money.format(current.getBalance()));
        } catch (InsufficientFundsException | IllegalArgumentException e) {
            System.out.println("  " + e.getMessage());
        }
    }

    private void deposit() {
        BigDecimal amount = askAmount("  Enter amount to deposit: Rs.");
        if (amount == null) return;
        try {
            bank.deposit(current, amount);
            System.out.println("  Deposited " + Money.format(amount) + ". New balance: " + Money.format(current.getBalance()));
        } catch (IllegalArgumentException e) {
            System.out.println("  " + e.getMessage());
        }
    }

    private void transfer() {
        String toId = prompt("  Enter recipient account ID: ");
        Account to = bank.find(toId);
        if (to == null || to == current) {
            System.out.println(to == null ? "  Recipient account not found." : "  You cannot transfer to your own account.");
            return;
        }
        BigDecimal amount = askAmount("  Enter amount to transfer: Rs.");
        if (amount == null) return;
        String confirm = prompt("  Transfer " + Money.format(amount) + " to " + to.getHolderName()
                + " (" + to.getUserId() + ")? (y/n): ");
        if (!confirm.equalsIgnoreCase("y")) {
            System.out.println("  Transfer cancelled.");
            return;
        }
        try {
            bank.transfer(current, toId, amount);
            System.out.println("  Transfer successful. New balance: " + Money.format(current.getBalance()));
        } catch (InsufficientFundsException | IllegalArgumentException e) {
            System.out.println("  " + e.getMessage());
        }
    }

    private void changePin() {
        String old = promptSecret("  Enter current PIN: ");
        if (!current.verifyPin(old)) {
            System.out.println("  Incorrect PIN.");
            return;
        }
        String p1 = promptSecret("  Enter new 4-digit PIN: ");
        if (!p1.matches("\\d{4}")) {
            System.out.println("  PIN must be exactly 4 digits.");
            return;
        }
        if (p1.equals(old)) {
            System.out.println("  New PIN must differ from the old PIN.");
            return;
        }
        String p2 = promptSecret("  Re-enter new PIN: ");
        if (!p1.equals(p2)) {
            System.out.println("  PINs do not match.");
            return;
        }
        current.changePin(p1);
        System.out.println("  PIN changed successfully.");
    }

    private void printReceiptSummary() {
        int count = current.getHistory().size() - sessionStartIndex;
        System.out.println("\n  ---------- SESSION SUMMARY ----------");
        System.out.println("  Account      : " + mask(current.getUserId()));
        System.out.println("  Transactions : " + count);
        System.out.println("  Balance      : " + Money.format(current.getBalance()));
        System.out.println("  -------------------------------------");
    }

    // ---------------- helpers ----------------
    private BigDecimal askAmount(String label) {
        BigDecimal amount = Money.parse(prompt(label));
        if (amount == null) {
            System.out.println("  Invalid amount. Enter a positive number (max 2 decimals).");
        }
        return amount;
    }

    private String prompt(String label) {
        System.out.print(label);
        if (!in.hasNextLine()) {          // input closed (Ctrl+D / Ctrl+Z)
            System.out.println("\n  Session terminated.");
            System.exit(0);
        }
        return in.nextLine().trim();
    }

    /** Hides the PIN when a real console is available (e.g. cmd / terminal). */
    private String promptSecret(String label) {
        java.io.Console console = System.console();
        if (console != null) {
            char[] pw = console.readPassword(label);
            return pw == null ? "" : new String(pw).trim();
        }
        return prompt(label);
    }

    private static String mask(String id) {
        return id.length() <= 2 ? id : "*".repeat(id.length() - 2) + id.substring(id.length() - 2);
    }

    private void banner() {
        System.out.println();
        System.out.println("  ###################################################");
        System.out.println("  #                                                 #");
        System.out.printf ("  #   %-44s  #%n", "Welcome to " + bank.getName() + " ATM");
        System.out.println("  #   Insert card... (enter your User ID & PIN)     #");
        System.out.println("  #                                                 #");
        System.out.println("  ###################################################");
    }
}
