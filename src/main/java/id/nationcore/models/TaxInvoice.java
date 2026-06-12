package id.nationcore.models;

/**
 * A single tax bill issued to one player for one billing cycle.
 *
 * Lifecycle:
 *   ACTIVE        — freshly issued, payable at face value for 1 real-life day.
 *   OVERDUE       — due time exceeded, the bill doubles (penalty category).
 *   NATIONAL_DEBT — 3 real-life days after issue the (doubled) value is recorded
 *                   permanently and force-collected by the state, even if the
 *                   player has already left the issuing nation.
 *   PAID          — settled in full.
 */
public class TaxInvoice {

    /** Time a fresh invoice stays payable at face value: 1 real-life day. */
    public static final long DUE_PERIOD_MILLIS = 24L * 60 * 60 * 1000;

    /** Time from issue until an unpaid invoice becomes national debt: 3 real-life days. */
    public static final long DEBT_PERIOD_MILLIS = 3L * 24 * 60 * 60 * 1000;

    /** Multiplier applied to the invoice value once the due period is exceeded. */
    public static final double PENALTY_MULTIPLIER = 2.0;

    public enum InvoiceStatus {
        ACTIVE("Active", "§e"),
        OVERDUE("Penalty", "§c"),
        NATIONAL_DEBT("National Debt", "§4"),
        PAID("Paid", "§a");

        private final String displayName;
        private final String color;

        InvoiceStatus(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() { return displayName; }
        public String getColor() { return color; }
    }

    private String id;
    private String nationId;
    private String nationName;
    private double baseAmount;
    private double paidAmount;
    private boolean penaltyApplied;
    private long createdAt;
    private long paidAt;
    private InvoiceStatus status;

    public TaxInvoice(String id, String nationId, String nationName, double baseAmount) {
        this.id = id;
        this.nationId = nationId;
        this.nationName = nationName;
        this.baseAmount = baseAmount;
        this.paidAmount = 0;
        this.penaltyApplied = false;
        this.createdAt = System.currentTimeMillis();
        this.paidAt = 0;
        this.status = InvoiceStatus.ACTIVE;
    }

    // === Getters / Setters ===

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNationId() { return nationId; }
    public void setNationId(String nationId) { this.nationId = nationId; }
    public String getNationName() { return nationName; }
    public void setNationName(String nationName) { this.nationName = nationName; }
    public double getBaseAmount() { return baseAmount; }
    public void setBaseAmount(double baseAmount) { this.baseAmount = baseAmount; }
    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }
    public boolean isPenaltyApplied() { return penaltyApplied; }
    public void setPenaltyApplied(boolean penaltyApplied) { this.penaltyApplied = penaltyApplied; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getPaidAt() { return paidAt; }
    public void setPaidAt(long paidAt) { this.paidAt = paidAt; }
    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }

    // === Derived values ===

    /** Total value of this bill — doubles once the penalty has been applied. */
    public double getTotalDue() {
        return penaltyApplied ? baseAmount * PENALTY_MULTIPLIER : baseAmount;
    }

    /** Amount still owed on this bill. */
    public double getRemaining() {
        return Math.max(0, getTotalDue() - paidAmount);
    }

    public boolean isOutstanding() {
        return status != InvoiceStatus.PAID;
    }

    public long getAgeMillis() {
        return System.currentTimeMillis() - createdAt;
    }

    /** Time left before this invoice doubles (0 if already overdue). */
    public long getMillisUntilOverdue() {
        return Math.max(0, createdAt + DUE_PERIOD_MILLIS - System.currentTimeMillis());
    }

    /** Time left before this invoice becomes national debt (0 if already debt). */
    public long getMillisUntilDebt() {
        return Math.max(0, createdAt + DEBT_PERIOD_MILLIS - System.currentTimeMillis());
    }
}
