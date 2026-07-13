package id.nationcore.models;

import java.util.*;

public class Treasury {

    private double balance;
    private List<Transaction> transactions;
    private double totalIncome;
    private double totalExpenses;

    public Treasury() {
        this.balance = 0;
        this.transactions = new ArrayList<>();
        this.totalIncome = 0;
        this.totalExpenses = 0;
    }

    public static class Transaction {
        private TransactionType type;
        private double amount;
        private String description;
        private UUID relatedPlayer;
        private long timestamp;

        public Transaction(TransactionType type, double amount, String description, UUID relatedPlayer) {
            this.type = type;
            this.amount = amount;
            this.description = description;
            this.relatedPlayer = relatedPlayer;
            this.timestamp = System.currentTimeMillis();
        }

        public TransactionType getType() {
            return type;
        }

        public double getAmount() {
            return amount;
        }

        public String getDescription() {
            return description;
        }

        public UUID getRelatedPlayer() {
            return relatedPlayer;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    public enum TransactionType {
        TAX_INCOME("Tax Income"),
        FINE_INCOME("Fine Income"),
        DONATION("Donation"),
        TERM_START_FUND("Term Starting Fund"),
        EXECUTIVE_ORDER("Executive Order"),
        PRESIDENTIAL_GAMES("Presidential Games"),
        CABINET_SALARY("Cabinet Salary"),
        VOTER_REWARD("Voter Reward"),
        STIMULUS("Economic Stimulus"),
        MISC_EXPENSE("Miscellaneous"),
        DEPOSIT_REFUND("Candidate Deposit Refund"),
        REGISTRATION_FEE("Candidate Registration Fee"),
        PRESIDENT_SALARY("President Salary"),
        GLOBAL_TAX_INCOME("Global Tax Income"),
        TAX_PENALTY_INCOME("Tax Penalty Income");

        private final String displayName;

        TransactionType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public double getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(double totalIncome) {
        this.totalIncome = totalIncome;
    }

    public double getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(double totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public boolean deposit(TransactionType type, double amount, String description, UUID player) {
        if (amount <= 0)
            return false;
        balance += amount;
        totalIncome += amount;
        transactions.add(new Transaction(type, amount, description, player));
        trimTransactions();
        return true;
    }

    public boolean withdraw(TransactionType type, double amount, String description, UUID player) {
        if (amount <= 0 || balance < amount)
            return false;
        balance -= amount;
        totalExpenses += amount;
        transactions.add(new Transaction(type, -amount, description, player));
        trimTransactions();
        return true;
    }

    public boolean canAfford(double amount) {
        return balance >= amount;
    }

    private void trimTransactions() {
        // Keep only last 1000 transactions
        if (transactions.size() > 1000) {
            transactions = new ArrayList<>(transactions.subList(transactions.size() - 1000, transactions.size()));
        }
    }

    public List<Transaction> getRecentTransactions(int count) {
        int size = transactions.size();
        if (size <= count) {
            return new ArrayList<>(transactions);
        }
        return new ArrayList<>(transactions.subList(size - count, size));
    }
}
