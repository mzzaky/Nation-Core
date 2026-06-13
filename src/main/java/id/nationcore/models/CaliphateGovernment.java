package id.nationcore.models;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Caliphate Government sub-state for {@link Nation} of type CALIPHATE.
 *
 * Power Structure (per spec):
 *   1. Caliph        — Lifelong head of state. Not elected, not recallable.
 *                      Holds executive-order authority but follows the standard
 *                      cooldown that applies to other regimes (only the King
 *                      is exempt; the Caliph is not).
 *   2. Shura Council — Advisory body of up to 3 members. Display only — no
 *                      decision/order powers.
 *   3. State Scholars— Religious/learned council of up to 5 members. Display
 *                      only — no decision/order powers.
 *   4. Citizens      — Ordinary nation members.
 *
 * Design intent: there is intentionally NO cabinet/politburo enum. The
 * Caliphate has no ministerial decisions — only the Caliph's executive orders
 * exist. Tax/Bayt al-Mal scheduling mirrors the Communist tax-phase / free-food
 * loops so existing config keys are reused.
 */
public class CaliphateGovernment {

    /** Maximum members for the advisory bodies (per spec). */
    public static final int MAX_SHURA = 3;
    public static final int MAX_SCHOLARS = 5;

    private UUID caliphUUID;
    private String caliphName;
    private long ascensionTime;
    private long lastCaliphActivity;
    private long lastDailyReward;

    /** Shura Council members (subset of nation members). Max 3. */
    private Set<UUID> shuraCouncil;

    /** State Scholars (subset of nation members). Max 5. */
    private Set<UUID> stateScholars;

    /** Last time the Jizya tax phase was collected. */
    private long lastTaxPhase;

    /** Last time the Zakat distribution was performed. */
    private long lastZakatDistribution;

    /** Last time the Bayt al-Mal subsidy was paid out. */
    private long lastSubsidyDistribution;

    /** Total subsidy paid out from Bayt al-Mal — for display & audit. */
    private double totalSubsidyPayouts;

    /** Phases left where Jizya tax is suspended (Zakat relief). */
    private int taxReliefPhasesLeft;

    /** Phases left where Jizya tax is doubled (special levy). */
    private int taxLevyPhasesLeft;

    private long lastBroadcastTime;

    public CaliphateGovernment() {
        this.shuraCouncil = new HashSet<>();
        this.stateScholars = new HashSet<>();
        this.totalSubsidyPayouts = 0.0;
        this.lastBroadcastTime = 0;
    }

    // === Caliph API ===

    public boolean hasCaliph() {
        return caliphUUID != null;
    }

    public UUID getCaliphUUID() { return caliphUUID; }
    public void setCaliphUUID(UUID uuid) { this.caliphUUID = uuid; }
    public String getCaliphName() { return caliphName; }
    public void setCaliphName(String name) { this.caliphName = name; }
    public long getAscensionTime() { return ascensionTime; }
    public void setAscensionTime(long t) { this.ascensionTime = t; }
    public long getLastCaliphActivity() { return lastCaliphActivity; }
    public void setLastCaliphActivity(long t) { this.lastCaliphActivity = t; }
    public long getLastDailyReward() { return lastDailyReward; }
    public void setLastDailyReward(long lastDailyReward) { this.lastDailyReward = lastDailyReward; }

    public long getLastBroadcastTime() { return lastBroadcastTime; }
    public void setLastBroadcastTime(long t) { this.lastBroadcastTime = t; }

    // === Shura Council API ===

    public Set<UUID> getShuraCouncil() {
        if (shuraCouncil == null) shuraCouncil = new HashSet<>();
        return shuraCouncil;
    }

    public boolean isShuraMember(UUID uuid) {
        return getShuraCouncil().contains(uuid);
    }

    public boolean addShuraMember(UUID uuid) {
        if (getShuraCouncil().size() >= MAX_SHURA) return false;
        return getShuraCouncil().add(uuid);
    }

    public boolean removeShuraMember(UUID uuid) {
        return getShuraCouncil().remove(uuid);
    }

    public int getShuraCount() {
        return getShuraCouncil().size();
    }

    // === State Scholars API ===

    public Set<UUID> getStateScholars() {
        if (stateScholars == null) stateScholars = new HashSet<>();
        return stateScholars;
    }

    public boolean isScholar(UUID uuid) {
        return getStateScholars().contains(uuid);
    }

    public boolean addScholar(UUID uuid) {
        if (getStateScholars().size() >= MAX_SCHOLARS) return false;
        return getStateScholars().add(uuid);
    }

    public boolean removeScholar(UUID uuid) {
        return getStateScholars().remove(uuid);
    }

    public int getScholarCount() {
        return getStateScholars().size();
    }

    // === Treasury / tax tracking ===

    public long getLastTaxPhase() { return lastTaxPhase; }
    public void setLastTaxPhase(long t) { this.lastTaxPhase = t; }
    public long getLastZakatDistribution() { return lastZakatDistribution; }
    public void setLastZakatDistribution(long t) { this.lastZakatDistribution = t; }
    public long getLastSubsidyDistribution() { return lastSubsidyDistribution; }
    public void setLastSubsidyDistribution(long t) { this.lastSubsidyDistribution = t; }
    public double getTotalSubsidyPayouts() { return totalSubsidyPayouts; }
    public void setTotalSubsidyPayouts(double v) { this.totalSubsidyPayouts = v; }
    public void addSubsidyPayout(double amount) { this.totalSubsidyPayouts += amount; }

    public int getTaxReliefPhasesLeft() { return taxReliefPhasesLeft; }
    public void setTaxReliefPhasesLeft(int v) { this.taxReliefPhasesLeft = v; }

    public int getTaxLevyPhasesLeft() { return taxLevyPhasesLeft; }
    public void setTaxLevyPhasesLeft(int v) { this.taxLevyPhasesLeft = v; }
}
