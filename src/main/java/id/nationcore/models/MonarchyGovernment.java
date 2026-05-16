package id.nationcore.models;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Monarchy Government sub-state for {@link Nation} of type MONARCHY.
 *
 * Power Structure:
 *   1. King — Absolute, lifelong leader. Not elected, not recallable. The King
 *      may freely issue executive orders (no cooldown) and may also execute
 *      every High Council order on behalf of any vacant or absent council
 *      member.
 *   2. High Council — Equivalent of Communist Politburo. Four positions:
 *      Chancellor (Treasury), Marshal (Defense), Saint (Health), Herald
 *      (Propaganda equivalent — kept so the order catalogue stays identical
 *      to the Communist mechanic, as the spec requires).
 *   3. Royal Soldiers — registered subjects with no voting rights (kept for
 *      symmetry and future expansion; they cannot depose the King).
 *
 * Design note: parallel structure with {@link CommunistGovernment} with the
 * same field shape so the existing executive-order and treasury machinery
 * can be reused with minimal branching.
 */
public class MonarchyGovernment {

    /**
     * High Council seats. Renames mirror the Communist politburo positions
     * so existing decision/order catalogues map 1:1 between the two regimes.
     */
    public enum HighCouncilPosition {
        HERALD("Herald", "&d[Herald]"),
        MARSHAL("Marshal", "&4[Marshal]"),
        CHANCELLOR("Chancellor", "&6[Chancellor]"),
        SAINT("Saint", "&a[Saint]");

        private final String displayName;
        private final String prefix;

        HighCouncilPosition(String displayName, String prefix) {
            this.displayName = displayName;
            this.prefix = prefix;
        }

        public String getDisplayName() { return displayName; }
        public String getPrefix() { return prefix; }
    }

    public static class HighCouncilMember {
        private UUID uuid;
        private String name;
        private HighCouncilPosition position;
        private long appointedTime;
        private long lastDecisionTime;
        private int decisionsUsed;
        private long lastDailyReward;

        public HighCouncilMember(UUID uuid, String name, HighCouncilPosition position) {
            this.uuid = uuid;
            this.name = name;
            this.position = position;
            this.appointedTime = System.currentTimeMillis();
        }

        public UUID getUuid() { return uuid; }
        public void setUuid(UUID uuid) { this.uuid = uuid; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public HighCouncilPosition getPosition() { return position; }
        public void setPosition(HighCouncilPosition position) { this.position = position; }
        public long getAppointedTime() { return appointedTime; }
        public void setAppointedTime(long appointedTime) { this.appointedTime = appointedTime; }
        public long getLastDecisionTime() { return lastDecisionTime; }
        public void setLastDecisionTime(long lastDecisionTime) { this.lastDecisionTime = lastDecisionTime; }
        public int getDecisionsUsed() { return decisionsUsed; }
        public void setDecisionsUsed(int decisionsUsed) { this.decisionsUsed = decisionsUsed; }
        public long getLastDailyReward() { return lastDailyReward; }
        public void setLastDailyReward(long lastDailyReward) { this.lastDailyReward = lastDailyReward; }
    }

    private UUID kingUUID;
    private String kingName;
    private long coronationTime;
    private long lastKingActivity;

    private Map<HighCouncilPosition, HighCouncilMember> highCouncil;

    /** UUID of registered Royal Soldiers (subset of nation members). */
    private Set<UUID> royalSoldiers;

    /** Last time the royal tax phase was collected. */
    private long lastTaxPhase;

    /** Last time royal alms (free food) were distributed. */
    private long lastAlmsDistribution;

    /** Last time the royal subsidy was paid out. */
    private long lastSubsidyDistribution;

    /** Total subsidy paid from the royal treasury — for display & audit. */
    private double totalSubsidyPayouts;

    private long lastBroadcastTime;

    // === Active decision state (mirrors Communist) ===
    private long defenseProtocolUntil;
    private long offenseProtocolUntil;
    private long quarantineUntil;
    private long plagueUntil;
    private long marketEventUntil;
    private long fieldMedicineCooldownUntil;
    private long vaccinationUntil;
    private long sensorMediaUntil;
    private long glorificationUntil;
    private long militaryEmergencyUntil;

    private int distributionProgramPhasesLeft;
    private int taxIntensificationPhasesLeft;

    private String censorshipReplacement;
    private Set<UUID> censorshipUsedOn;

    /**
     * Cooldown decision per pemain. Key luar = playerUUID,
     * key dalam = decision name, value = lastUseTime ms.
     * For the King (kingUUID) cooldown lookups always return 0 — the
     * MonarchyManager bypasses cooldown for the absolute leader.
     */
    private Map<UUID, Map<String, Long>> decisionCooldowns;

    public MonarchyGovernment() {
        this.highCouncil = new EnumMap<>(HighCouncilPosition.class);
        this.royalSoldiers = new HashSet<>();
        this.censorshipUsedOn = new HashSet<>();
        this.decisionCooldowns = new HashMap<>();
        this.totalSubsidyPayouts = 0.0;
        this.lastBroadcastTime = 0;
    }

    // === King API ===

    public boolean hasKing() {
        return kingUUID != null;
    }

    public UUID getKingUUID() { return kingUUID; }
    public void setKingUUID(UUID uuid) { this.kingUUID = uuid; }
    public String getKingName() { return kingName; }
    public void setKingName(String name) { this.kingName = name; }
    public long getCoronationTime() { return coronationTime; }
    public void setCoronationTime(long t) { this.coronationTime = t; }
    public long getLastKingActivity() { return lastKingActivity; }
    public void setLastKingActivity(long t) { this.lastKingActivity = t; }

    public long getLastBroadcastTime() { return lastBroadcastTime; }
    public void setLastBroadcastTime(long t) { this.lastBroadcastTime = t; }

    // === High Council API ===

    public Map<HighCouncilPosition, HighCouncilMember> getHighCouncil() {
        if (highCouncil == null) highCouncil = new EnumMap<>(HighCouncilPosition.class);
        return highCouncil;
    }

    public HighCouncilMember getCouncilMember(HighCouncilPosition position) {
        return getHighCouncil().get(position);
    }

    public HighCouncilMember getCouncilMemberByUUID(UUID uuid) {
        for (HighCouncilMember m : getHighCouncil().values()) {
            if (m.getUuid().equals(uuid)) return m;
        }
        return null;
    }

    public HighCouncilPosition getPositionByUUID(UUID uuid) {
        for (Map.Entry<HighCouncilPosition, HighCouncilMember> entry : getHighCouncil().entrySet()) {
            if (entry.getValue().getUuid().equals(uuid)) return entry.getKey();
        }
        return null;
    }

    public void appointCouncil(HighCouncilPosition position, HighCouncilMember member) {
        getHighCouncil().put(position, member);
    }

    public void removeCouncil(HighCouncilPosition position) {
        getHighCouncil().remove(position);
    }

    // === Royal Soldier API ===

    public Set<UUID> getRoyalSoldiers() {
        if (royalSoldiers == null) royalSoldiers = new HashSet<>();
        return royalSoldiers;
    }

    public boolean isRoyalSoldier(UUID uuid) {
        return getRoyalSoldiers().contains(uuid);
    }

    public void addRoyalSoldier(UUID uuid) {
        getRoyalSoldiers().add(uuid);
    }

    public boolean removeRoyalSoldier(UUID uuid) {
        return getRoyalSoldiers().remove(uuid);
    }

    public int getRoyalSoldierCount() {
        return getRoyalSoldiers().size();
    }

    // === Treasury / tax tracking ===

    public long getLastTaxPhase() { return lastTaxPhase; }
    public void setLastTaxPhase(long t) { this.lastTaxPhase = t; }
    public long getLastAlmsDistribution() { return lastAlmsDistribution; }
    public void setLastAlmsDistribution(long t) { this.lastAlmsDistribution = t; }
    public long getLastSubsidyDistribution() { return lastSubsidyDistribution; }
    public void setLastSubsidyDistribution(long t) { this.lastSubsidyDistribution = t; }
    public double getTotalSubsidyPayouts() { return totalSubsidyPayouts; }
    public void setTotalSubsidyPayouts(double v) { this.totalSubsidyPayouts = v; }
    public void addSubsidyPayout(double amount) { this.totalSubsidyPayouts += amount; }

    // === Active decision state getters/setters ===

    public long getDefenseProtocolUntil() { return defenseProtocolUntil; }
    public void setDefenseProtocolUntil(long v) { this.defenseProtocolUntil = v; }
    public boolean isDefenseProtocolActive() { return System.currentTimeMillis() < defenseProtocolUntil; }

    public long getOffenseProtocolUntil() { return offenseProtocolUntil; }
    public void setOffenseProtocolUntil(long v) { this.offenseProtocolUntil = v; }
    public boolean isOffenseProtocolActive() { return System.currentTimeMillis() < offenseProtocolUntil; }

    public long getQuarantineUntil() { return quarantineUntil; }
    public void setQuarantineUntil(long v) { this.quarantineUntil = v; }
    public boolean isQuarantineActive() { return System.currentTimeMillis() < quarantineUntil; }

    public long getPlagueUntil() { return plagueUntil; }
    public void setPlagueUntil(long v) { this.plagueUntil = v; }
    public boolean isPlagueActive() { return System.currentTimeMillis() < plagueUntil; }

    public long getMarketEventUntil() { return marketEventUntil; }
    public void setMarketEventUntil(long v) { this.marketEventUntil = v; }
    public boolean isMarketEventActive() { return System.currentTimeMillis() < marketEventUntil; }

    public long getFieldMedicineCooldownUntil() { return fieldMedicineCooldownUntil; }
    public void setFieldMedicineCooldownUntil(long v) { this.fieldMedicineCooldownUntil = v; }
    public boolean isFieldMedicineOnCooldown() { return System.currentTimeMillis() < fieldMedicineCooldownUntil; }

    public long getVaccinationUntil() { return vaccinationUntil; }
    public void setVaccinationUntil(long v) { this.vaccinationUntil = v; }
    public boolean isVaccinationActive() { return System.currentTimeMillis() < vaccinationUntil; }

    public long getSensorMediaUntil() { return sensorMediaUntil; }
    public void setSensorMediaUntil(long v) { this.sensorMediaUntil = v; }
    public boolean isSensorMediaActive() { return System.currentTimeMillis() < sensorMediaUntil; }

    public long getGlorificationUntil() { return glorificationUntil; }
    public void setGlorificationUntil(long v) { this.glorificationUntil = v; }
    public boolean isGlorificationActive() { return System.currentTimeMillis() < glorificationUntil; }

    public long getMilitaryEmergencyUntil() { return militaryEmergencyUntil; }
    public void setMilitaryEmergencyUntil(long v) { this.militaryEmergencyUntil = v; }
    public boolean isMilitaryEmergencyActive() { return System.currentTimeMillis() < militaryEmergencyUntil; }

    public int getDistributionProgramPhasesLeft() { return distributionProgramPhasesLeft; }
    public void setDistributionProgramPhasesLeft(int v) { this.distributionProgramPhasesLeft = v; }

    public int getTaxIntensificationPhasesLeft() { return taxIntensificationPhasesLeft; }
    public void setTaxIntensificationPhasesLeft(int v) { this.taxIntensificationPhasesLeft = v; }

    public String getCensorshipReplacement() { return censorshipReplacement; }
    public void setCensorshipReplacement(String v) { this.censorshipReplacement = v; }

    public Set<UUID> getCensorshipUsedOn() {
        if (censorshipUsedOn == null) censorshipUsedOn = new HashSet<>();
        return censorshipUsedOn;
    }

    public Map<UUID, Map<String, Long>> getDecisionCooldowns() {
        if (decisionCooldowns == null) decisionCooldowns = new HashMap<>();
        return decisionCooldowns;
    }

    // unused legacy fields kept to silence Gson warnings on old saves
    @SuppressWarnings("unused")
    private List<Object> activeSanctions = new ArrayList<>();
}
