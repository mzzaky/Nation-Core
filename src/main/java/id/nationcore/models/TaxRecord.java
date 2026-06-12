package id.nationcore.models;

import java.util.*;

import id.nationcore.models.TaxInvoice.InvoiceStatus;

/**
 * Root ledger of the invoice-based nation tax system.
 *
 * Every billing cycle (1 real-life day) the system issues one {@link TaxInvoice}
 * per active nation member. This record keeps one {@link PlayerTaxProfile} per
 * player holding that player's invoices, auto-pay preference, lifetime
 * statistics and recent payment log.
 */
public class TaxRecord {

    private boolean enabled;
    private long lastCycleTime;
    private int totalCycles;
    private double totalTaxCollected;
    private double totalPenaltiesCollected;
    private double totalDebtCollected;
    private long invoiceSequence;
    private Map<String, PlayerTaxProfile> playerProfiles; // UUID string -> profile

    public TaxRecord() {
        this.enabled = true;
        this.lastCycleTime = 0;
        this.totalCycles = 0;
        this.totalTaxCollected = 0;
        this.totalPenaltiesCollected = 0;
        this.totalDebtCollected = 0;
        this.invoiceSequence = 0;
        this.playerProfiles = new HashMap<>();
    }

    /**
     * Re-creates any collection Gson left as null — e.g. when loading a data
     * file written by the pre-invoice tax system.
     */
    public void ensureInitialized() {
        if (playerProfiles == null) {
            playerProfiles = new HashMap<>();
        }
        playerProfiles.values().removeIf(Objects::isNull);
        for (PlayerTaxProfile profile : playerProfiles.values()) {
            profile.ensureInitialized();
        }
    }

    /** How a payment was made — shown in the transaction log. */
    public enum PaymentMethod {
        MANUAL("Manual"),
        AUTO_PAY("Auto Pay"),
        FORCED("Forced Seizure");

        private final String displayName;

        PaymentMethod(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }
    }

    /** One money movement against an invoice, kept for the transaction log. */
    public static class TaxPayment {
        private String invoiceId;
        private double amount;
        private long timestamp;
        private PaymentMethod method;
        private String description;

        public TaxPayment(String invoiceId, double amount, PaymentMethod method, String description) {
            this.invoiceId = invoiceId;
            this.amount = amount;
            this.timestamp = System.currentTimeMillis();
            this.method = method;
            this.description = description;
        }

        public String getInvoiceId() { return invoiceId; }
        public double getAmount() { return amount; }
        public long getTimestamp() { return timestamp; }
        public PaymentMethod getMethod() { return method; }
        public String getDescription() { return description; }
    }

    /** Per-player tax ledger: invoices, auto-pay preference and statistics. */
    public static class PlayerTaxProfile {

        private static final int MAX_PAYMENT_LOG = 20;
        private static final int MAX_PAID_INVOICES = 25;

        private String playerName;
        private boolean autoPay;
        private List<TaxInvoice> invoices;
        private List<TaxPayment> paymentLog;
        private int invoicesIssued;
        private int invoicesPaid;
        private int invoicesPaidLate;
        private int invoicesDefaulted;
        private double totalAmountPaid;
        private double totalPenaltyPaid;
        private double totalDebtRepaid;
        private long lastPaymentTime;

        public PlayerTaxProfile(String playerName) {
            this.playerName = playerName;
            this.autoPay = false;
            this.invoices = new ArrayList<>();
            this.paymentLog = new ArrayList<>();
        }

        public void ensureInitialized() {
            if (invoices == null) invoices = new ArrayList<>();
            if (paymentLog == null) paymentLog = new ArrayList<>();
            invoices.removeIf(Objects::isNull);
            paymentLog.removeIf(Objects::isNull);
        }

        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }
        public boolean isAutoPay() { return autoPay; }
        public void setAutoPay(boolean autoPay) { this.autoPay = autoPay; }
        public List<TaxInvoice> getInvoices() { return invoices; }
        public void setInvoices(List<TaxInvoice> invoices) { this.invoices = invoices; }
        public List<TaxPayment> getPaymentLog() { return paymentLog; }
        public void setPaymentLog(List<TaxPayment> paymentLog) { this.paymentLog = paymentLog; }
        public int getInvoicesIssued() { return invoicesIssued; }
        public void setInvoicesIssued(int invoicesIssued) { this.invoicesIssued = invoicesIssued; }
        public int getInvoicesPaid() { return invoicesPaid; }
        public void setInvoicesPaid(int invoicesPaid) { this.invoicesPaid = invoicesPaid; }
        public int getInvoicesPaidLate() { return invoicesPaidLate; }
        public void setInvoicesPaidLate(int invoicesPaidLate) { this.invoicesPaidLate = invoicesPaidLate; }
        public int getInvoicesDefaulted() { return invoicesDefaulted; }
        public void setInvoicesDefaulted(int invoicesDefaulted) { this.invoicesDefaulted = invoicesDefaulted; }
        public double getTotalAmountPaid() { return totalAmountPaid; }
        public void setTotalAmountPaid(double totalAmountPaid) { this.totalAmountPaid = totalAmountPaid; }
        public double getTotalPenaltyPaid() { return totalPenaltyPaid; }
        public void setTotalPenaltyPaid(double totalPenaltyPaid) { this.totalPenaltyPaid = totalPenaltyPaid; }
        public double getTotalDebtRepaid() { return totalDebtRepaid; }
        public void setTotalDebtRepaid(double totalDebtRepaid) { this.totalDebtRepaid = totalDebtRepaid; }
        public long getLastPaymentTime() { return lastPaymentTime; }
        public void setLastPaymentTime(long lastPaymentTime) { this.lastPaymentTime = lastPaymentTime; }

        public void addInvoice(TaxInvoice invoice) {
            getInvoicesSafe().add(invoice);
            invoicesIssued++;
            trimPaidInvoices();
        }

        private List<TaxInvoice> getInvoicesSafe() {
            if (invoices == null) invoices = new ArrayList<>();
            return invoices;
        }

        /** All invoices not yet fully settled, oldest first. */
        public List<TaxInvoice> getOutstandingInvoices() {
            List<TaxInvoice> result = new ArrayList<>();
            for (TaxInvoice invoice : getInvoicesSafe()) {
                if (invoice.isOutstanding()) {
                    result.add(invoice);
                }
            }
            result.sort(Comparator.comparingLong(TaxInvoice::getCreatedAt));
            return result;
        }

        /** Sum still owed across every outstanding invoice. */
        public double getOutstandingTotal() {
            double total = 0;
            for (TaxInvoice invoice : getOutstandingInvoices()) {
                total += invoice.getRemaining();
            }
            return total;
        }

        /** Sum still owed on invoices that escalated to national debt. */
        public double getNationalDebtRemaining() {
            double total = 0;
            for (TaxInvoice invoice : getInvoicesSafe()) {
                if (invoice.getStatus() == InvoiceStatus.NATIONAL_DEBT) {
                    total += invoice.getRemaining();
                }
            }
            return total;
        }

        public boolean hasNationalDebt() {
            return getNationalDebtRemaining() > 0;
        }

        public void recordPayment(TaxPayment payment) {
            if (paymentLog == null) paymentLog = new ArrayList<>();
            paymentLog.add(payment);
            lastPaymentTime = payment.getTimestamp();
            if (paymentLog.size() > MAX_PAYMENT_LOG) {
                paymentLog = new ArrayList<>(
                        paymentLog.subList(paymentLog.size() - MAX_PAYMENT_LOG, paymentLog.size()));
            }
        }

        /** Newest payments first. */
        public List<TaxPayment> getRecentPayments(int count) {
            if (paymentLog == null) return new ArrayList<>();
            List<TaxPayment> copy = new ArrayList<>(paymentLog);
            Collections.reverse(copy);
            if (copy.size() > count) {
                return new ArrayList<>(copy.subList(0, count));
            }
            return copy;
        }

        /** Keeps history bounded: drops the oldest settled invoices only. */
        private void trimPaidInvoices() {
            List<TaxInvoice> paid = new ArrayList<>();
            for (TaxInvoice invoice : getInvoicesSafe()) {
                if (!invoice.isOutstanding()) {
                    paid.add(invoice);
                }
            }
            if (paid.size() <= MAX_PAID_INVOICES) return;
            paid.sort(Comparator.comparingLong(TaxInvoice::getCreatedAt));
            int toRemove = paid.size() - MAX_PAID_INVOICES;
            for (int i = 0; i < toRemove; i++) {
                invoices.remove(paid.get(i));
            }
        }
    }

    // === Getters / Setters ===

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public long getLastCycleTime() { return lastCycleTime; }
    public void setLastCycleTime(long lastCycleTime) { this.lastCycleTime = lastCycleTime; }
    public int getTotalCycles() { return totalCycles; }
    public void setTotalCycles(int totalCycles) { this.totalCycles = totalCycles; }
    public double getTotalTaxCollected() { return totalTaxCollected; }
    public void setTotalTaxCollected(double totalTaxCollected) { this.totalTaxCollected = totalTaxCollected; }
    public double getTotalPenaltiesCollected() { return totalPenaltiesCollected; }
    public void setTotalPenaltiesCollected(double totalPenaltiesCollected) { this.totalPenaltiesCollected = totalPenaltiesCollected; }
    public double getTotalDebtCollected() { return totalDebtCollected; }
    public void setTotalDebtCollected(double totalDebtCollected) { this.totalDebtCollected = totalDebtCollected; }
    public Map<String, PlayerTaxProfile> getPlayerProfiles() {
        if (playerProfiles == null) playerProfiles = new HashMap<>();
        return playerProfiles;
    }
    public void setPlayerProfiles(Map<String, PlayerTaxProfile> playerProfiles) { this.playerProfiles = playerProfiles; }

    public PlayerTaxProfile getOrCreateProfile(String uuid, String name) {
        PlayerTaxProfile profile = getPlayerProfiles().computeIfAbsent(uuid, k -> new PlayerTaxProfile(name));
        profile.ensureInitialized();
        if (name != null) {
            profile.setPlayerName(name);
        }
        return profile;
    }

    public PlayerTaxProfile getProfile(String uuid) {
        PlayerTaxProfile profile = getPlayerProfiles().get(uuid);
        if (profile != null) {
            profile.ensureInitialized();
        }
        return profile;
    }

    /** Sequential, human-readable invoice number (INV-00001, INV-00002, ...). */
    public String nextInvoiceId() {
        invoiceSequence++;
        return String.format("INV-%05d", invoiceSequence);
    }

    public void incrementCycle() {
        totalCycles++;
        lastCycleTime = System.currentTimeMillis();
    }

    public void addTaxCollected(double amount) {
        totalTaxCollected += amount;
    }

    public void addPenaltyCollected(double amount) {
        totalPenaltiesCollected += amount;
    }

    public void addDebtCollected(double amount) {
        totalDebtCollected += amount;
    }
}
