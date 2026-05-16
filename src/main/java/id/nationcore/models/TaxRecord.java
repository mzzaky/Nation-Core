package id.nationcore.models;

import java.util.*;

public class TaxRecord {

    private boolean enabled;
    private long lastCollectionTime;
    private Map<String, PlayerTaxData> playerTaxData; // UUID string -> data
    private List<TaxTransaction> taxHistory;
    private double totalTaxCollected;
    private double totalPenaltiesCollected;
    private int totalCollectionCycles;

    public TaxRecord() {
        this.enabled = true;
        this.lastCollectionTime = 0;
        this.playerTaxData = new HashMap<>();
        this.taxHistory = new ArrayList<>();
        this.totalTaxCollected = 0;
        this.totalPenaltiesCollected = 0;
        this.totalCollectionCycles = 0;
    }

    public static class PlayerTaxData {
        private String playerName;
        private int totalTaxesPaid;
        private int missedPayments;
        private double totalAmountPaid;
        private double totalPenaltiesPaid;
        private double outstandingDebt;
        private long lastPaymentTime;
        private boolean exempt;
        private List<String> punishmentHistory; // timestamps of punishments applied

        public PlayerTaxData(String playerName) {
            this.playerName = playerName;
            this.totalTaxesPaid = 0;
            this.missedPayments = 0;
            this.totalAmountPaid = 0;
            this.totalPenaltiesPaid = 0;
            this.outstandingDebt = 0;
            this.lastPaymentTime = 0;
            this.exempt = false;
            this.punishmentHistory = new ArrayList<>();
        }

        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }
        public int getTotalTaxesPaid() { return totalTaxesPaid; }
        public void setTotalTaxesPaid(int totalTaxesPaid) { this.totalTaxesPaid = totalTaxesPaid; }
        public int getMissedPayments() { return missedPayments; }
        public void setMissedPayments(int missedPayments) { this.missedPayments = missedPayments; }
        public double getTotalAmountPaid() { return totalAmountPaid; }
        public void setTotalAmountPaid(double totalAmountPaid) { this.totalAmountPaid = totalAmountPaid; }
        public double getTotalPenaltiesPaid() { return totalPenaltiesPaid; }
        public void setTotalPenaltiesPaid(double totalPenaltiesPaid) { this.totalPenaltiesPaid = totalPenaltiesPaid; }
        public double getOutstandingDebt() { return outstandingDebt; }
        public void setOutstandingDebt(double outstandingDebt) { this.outstandingDebt = outstandingDebt; }
        public long getLastPaymentTime() { return lastPaymentTime; }
        public void setLastPaymentTime(long lastPaymentTime) { this.lastPaymentTime = lastPaymentTime; }
        public boolean isExempt() { return exempt; }
        public void setExempt(boolean exempt) { this.exempt = exempt; }
        public List<String> getPunishmentHistory() { return punishmentHistory; }
        public void setPunishmentHistory(List<String> punishmentHistory) { this.punishmentHistory = punishmentHistory; }

        public void recordPayment(double amount) {
            this.totalTaxesPaid++;
            this.totalAmountPaid += amount;
            this.lastPaymentTime = System.currentTimeMillis();
        }

        public void recordPenalty(double amount) {
            this.totalPenaltiesPaid += amount;
        }

        public void addMissedPayment() {
            this.missedPayments++;
        }

        public void addDebt(double amount) {
            this.outstandingDebt += amount;
        }

        public void clearDebt() {
            this.outstandingDebt = 0;
        }

        public void payDebt(double amount) {
            this.outstandingDebt = Math.max(0, this.outstandingDebt - amount);
        }
    }

    public static class TaxTransaction {
        private String playerUUID;
        private String playerName;
        private TaxTransactionType type;
        private double amount;
        private long timestamp;
        private String description;

        public TaxTransaction(String playerUUID, String playerName, TaxTransactionType type,
                              double amount, String description) {
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.type = type;
            this.amount = amount;
            this.timestamp = System.currentTimeMillis();
            this.description = description;
        }

        public String getPlayerUUID() { return playerUUID; }
        public String getPlayerName() { return playerName; }
        public TaxTransactionType getType() { return type; }
        public double getAmount() { return amount; }
        public long getTimestamp() { return timestamp; }
        public String getDescription() { return description; }
    }

    public enum TaxTransactionType {
        TAX_PAID("Tax Paid"),
        LATE_PENALTY("Late Penalty"),
        DEBT_PAYMENT("Debt Payment"),
        PUNISHMENT_APPLIED("Punishment Applied"),
        TAX_EXEMPTION("Tax Exemption"),
        DEBT_FORGIVEN("Debt Forgiven");

        private final String displayName;

        TaxTransactionType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }
    }

    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public long getLastCollectionTime() { return lastCollectionTime; }
    public void setLastCollectionTime(long lastCollectionTime) { this.lastCollectionTime = lastCollectionTime; }
    public Map<String, PlayerTaxData> getPlayerTaxData() { return playerTaxData; }
    public void setPlayerTaxData(Map<String, PlayerTaxData> playerTaxData) { this.playerTaxData = playerTaxData; }
    public List<TaxTransaction> getTaxHistory() { return taxHistory; }
    public void setTaxHistory(List<TaxTransaction> taxHistory) { this.taxHistory = taxHistory; }
    public double getTotalTaxCollected() { return totalTaxCollected; }
    public void setTotalTaxCollected(double totalTaxCollected) { this.totalTaxCollected = totalTaxCollected; }
    public double getTotalPenaltiesCollected() { return totalPenaltiesCollected; }
    public void setTotalPenaltiesCollected(double totalPenaltiesCollected) { this.totalPenaltiesCollected = totalPenaltiesCollected; }
    public int getTotalCollectionCycles() { return totalCollectionCycles; }
    public void setTotalCollectionCycles(int totalCollectionCycles) { this.totalCollectionCycles = totalCollectionCycles; }

    public PlayerTaxData getOrCreatePlayerTaxData(String uuid, String name) {
        return playerTaxData.computeIfAbsent(uuid, k -> new PlayerTaxData(name));
    }

    public PlayerTaxData getPlayerTaxData(String uuid) {
        return playerTaxData.get(uuid);
    }

    public void addTransaction(TaxTransaction transaction) {
        taxHistory.add(transaction);
        trimHistory();
    }

    private void trimHistory() {
        if (taxHistory.size() > 500) {
            taxHistory = new ArrayList<>(taxHistory.subList(taxHistory.size() - 500, taxHistory.size()));
        }
    }

    public List<TaxTransaction> getRecentTransactions(int count) {
        int size = taxHistory.size();
        if (size <= count) {
            return new ArrayList<>(taxHistory);
        }
        return new ArrayList<>(taxHistory.subList(size - count, size));
    }

    public void incrementCycle() {
        totalCollectionCycles++;
        lastCollectionTime = System.currentTimeMillis();
    }

    public void addTaxCollected(double amount) {
        totalTaxCollected += amount;
    }

    public void addPenaltyCollected(double amount) {
        totalPenaltiesCollected += amount;
    }
}
