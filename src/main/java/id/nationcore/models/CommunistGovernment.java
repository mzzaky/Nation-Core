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
 * Communist Government sub-state for {@link Nation} of type COMMUNIST.
 *
 * Power Structure (proposal §B.1):
 *   1. Secretary General — Party leader, elected by Party members
 *      (not popular vote). Internal party elections implemented in Phase 4B;
 *      currently founder holds office automatically.
 *   2. Politburo — Decision-making council, equivalent to Cabinet in Republic.
 *      Four positions: Propaganda, Defense, Treasury, Health.
 *   3. Party Member — players registered with the Party, have internal voting rights.
 *   4. Ordinary Citizens — nation members not registered with the Party.
 *
 * Design note: parallel structure with {@link Government} but no inheritance.
 * Positions & flow differ significantly (politburo vs cabinet, party
 * voting vs popular vote, subsidy/tax vs transparent treasury) so separate
 * classes are cleaner.
 */
public class CommunistGovernment {

    /**
     * Posisi di Kabinet Komunis. Sesuai spec:
     *   • Propaganda — special minister Komunis (tidak ada di Republic)
     *   • Defense    — paralel dengan Republic (REVAMP executive ordersnya di Phase 4C)
     *   • Treasury   — paralel dengan Republic (REVAMP)
     *   • Health     — minister baru khusus Komunis
     */
    public enum PolitburoPosition {
        PROPAGANDA("Minister of Propaganda", "&d[Propaganda]"),
        DEFENSE("Minister of Defense", "&4[Defense]"),
        TREASURY("Minister of Treasury", "&6[Treasury]"),
        HEALTH("Minister of Health", "&a[Health]");

        private final String displayName;
        private final String prefix;

        PolitburoPosition(String displayName, String prefix) {
            this.displayName = displayName;
            this.prefix = prefix;
        }

        public String getDisplayName() { return displayName; }
        public String getPrefix() { return prefix; }
    }

    public static class PolitburoMember {
        private UUID uuid;
        private String name;
        private PolitburoPosition position;
        private long appointedTime;
        private long lastDecisionTime;
        private int decisionsUsed;
        private long lastDailyReward;

        public PolitburoMember(UUID uuid, String name, PolitburoPosition position) {
            this.uuid = uuid;
            this.name = name;
            this.position = position;
            this.appointedTime = System.currentTimeMillis();
        }

        public UUID getUuid() { return uuid; }
        public void setUuid(UUID uuid) { this.uuid = uuid; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public PolitburoPosition getPosition() { return position; }
        public void setPosition(PolitburoPosition position) { this.position = position; }
        public long getAppointedTime() { return appointedTime; }
        public void setAppointedTime(long appointedTime) { this.appointedTime = appointedTime; }
        public long getLastDecisionTime() { return lastDecisionTime; }
        public void setLastDecisionTime(long lastDecisionTime) { this.lastDecisionTime = lastDecisionTime; }
        public int getDecisionsUsed() { return decisionsUsed; }
        public void setDecisionsUsed(int decisionsUsed) { this.decisionsUsed = decisionsUsed; }
        public long getLastDailyReward() { return lastDailyReward; }
        public void setLastDailyReward(long lastDailyReward) { this.lastDailyReward = lastDailyReward; }
    }

    /**
     * Pencatat pemain yang sedang dikenakan Sensor Media atau Gulag
     * (executive order khusus komunis). Akan diisi pada Phase 4B.
     */
    public static class Sanction {
        private UUID targetUUID;
        private String targetName;
        private SanctionType type;
        private long startTime;
        private long expirationTime;
        private UUID issuedBy;

        public enum SanctionType {
            CENSOR_MEDIA,    // muted from public & nation chat
            GULAG            // imprisoned with mining/farming tasks
        }

        public Sanction(UUID targetUUID, String targetName, SanctionType type,
                        long durationMillis, UUID issuedBy) {
            this.targetUUID = targetUUID;
            this.targetName = targetName;
            this.type = type;
            this.startTime = System.currentTimeMillis();
            this.expirationTime = this.startTime + durationMillis;
            this.issuedBy = issuedBy;
        }

        public UUID getTargetUUID() { return targetUUID; }
        public String getTargetName() { return targetName; }
        public SanctionType getType() { return type; }
        public long getStartTime() { return startTime; }
        public long getExpirationTime() { return expirationTime; }
        public UUID getIssuedBy() { return issuedBy; }
        public boolean isExpired() {
            return expirationTime > 0 && System.currentTimeMillis() > expirationTime;
        }
    }

    private UUID secretaryGeneralUUID;
    private String secretaryGeneralName;
    private long termStartTime;
    private int consecutiveTerms;
    private long lastSecretaryActivity;
    private long lastDailyReward;

    private Map<PolitburoPosition, PolitburoMember> politburo;

    /** UUID pemain yang terdaftar sebagai anggota Partai (subset dari nation members). */
    private Set<UUID> partyMembers;

    /** Kapan terakhir subsidi kesejahteraan harian dibagikan ke seluruh anggota. */
    private long lastSubsidyDistribution;

    /** Kapan terakhir pajak Komunis (50/anggota) ditarik. */
    private long lastTaxPhase;

    /** Kapan terakhir Free Food dibagikan ke anggota. */
    private long lastFreeFoodDistribution;

    /** Daftar sanksi aktif (Sensor Media / Gulag). */
    private List<Sanction> activeSanctions;

    /** Total subsidi yang sudah dibayarkan dari kas — untuk display & audit. */
    private double totalSubsidyPayouts;

    private long lastBroadcastTime;

    public CommunistGovernment() {
        this.politburo = new EnumMap<>(PolitburoPosition.class);
        this.partyMembers = new HashSet<>();
        this.activeSanctions = new ArrayList<>();
        this.partyVotes = new HashMap<>();
        this.censorshipUsedOn = new HashSet<>();
        this.decisionCooldowns = new HashMap<>();
        this.consecutiveTerms = 0;
        this.totalSubsidyPayouts = 0.0;
        this.lastBroadcastTime = 0;
        this.electionCycleStart = System.currentTimeMillis();
        this.orderHistory = new ArrayList<>();
    }

    public long getLastBroadcastTime() {
        return lastBroadcastTime;
    }

    public void setLastBroadcastTime(long lastBroadcastTime) {
        this.lastBroadcastTime = lastBroadcastTime;
    }

    /**
     * Pemilihan Sekretaris Jenderal: vote partai (key = voter, value = candidate).
     * Direset setiap akhir cycle (default 7 hari).
     */
    private Map<UUID, UUID> partyVotes;

    /** Kapan election cycle saat ini dimulai. */
    private long electionCycleStart;

    // === Phase 4C — state efek 20 decisions ===
    /** Defense Protocol aktif sampai timestamp ini (Resistance I ke anggota). */
    private long defenseProtocolUntil;
    /** Offense Protocol aktif sampai timestamp ini (Strength I ke anggota). */
    private long offenseProtocolUntil;
    /** Quarantine: non-anggota tidak boleh masuk teritori sampai timestamp ini. */
    private long quarantineUntil;
    /** Plague: musuh di teritori dapat Weakness II + Hunger sampai timestamp ini. */
    private long plagueUntil;
    /** Market Event: bonus $25 per villager trade sampai timestamp ini. */
    private long marketEventUntil;
    /** Field Medicine cooldown ends — CD 2 jam (durasi efek sendiri 5 menit). */
    private long fieldMedicineCooldownUntil;
    /** Vaccination Drive: anggota kebal poison/wither sampai timestamp ini. */
    private long vaccinationUntil;
    /** Sensor Media aktif sampai timestamp ini (PROP_MEDIA_CENSORSHIP). */
    private long sensorMediaUntil;
    /** Glorifikasi Pemimpin aktif sampai timestamp ini. */
    private long glorificationUntil;
    /** Military Emergency aktif sampai timestamp ini. */
    private long militaryEmergencyUntil;

    /** Sisa fase pajak yang dibebaskan via Distribution Program. */
    private int distributionProgramPhasesLeft;
    /** Sisa fase pajak yang digandakan (200%) via Tax Intensification. */
    private int taxIntensificationPhasesLeft;

    /** Template pesan pengganti saat Sensor Media aktif. */
    private String censorshipReplacement;
    /** Anggota yang sudah kena censorship (1x per orang). */
    private Set<UUID> censorshipUsedOn;

    /**
     * Cooldown decision per pemain. Key luar = playerUUID,
     * key dalam = decision name, value = lastUseTime ms.
     */
    private Map<UUID, Map<String, Long>> decisionCooldowns;

    // === Sekretaris Jenderal API ===

    public boolean hasSecretaryGeneral() {
        return secretaryGeneralUUID != null;
    }

    public UUID getSecretaryGeneralUUID() { return secretaryGeneralUUID; }
    public void setSecretaryGeneralUUID(UUID uuid) { this.secretaryGeneralUUID = uuid; }
    public String getSecretaryGeneralName() { return secretaryGeneralName; }
    public void setSecretaryGeneralName(String name) { this.secretaryGeneralName = name; }
    public long getTermStartTime() { return termStartTime; }
    public void setTermStartTime(long termStartTime) { this.termStartTime = termStartTime; }
    public int getConsecutiveTerms() { return consecutiveTerms; }
    public void setConsecutiveTerms(int consecutiveTerms) { this.consecutiveTerms = consecutiveTerms; }
    public long getLastSecretaryActivity() { return lastSecretaryActivity; }
    public void setLastSecretaryActivity(long lastSecretaryActivity) {
        this.lastSecretaryActivity = lastSecretaryActivity;
    }
    public long getLastDailyReward() { return lastDailyReward; }
    public void setLastDailyReward(long lastDailyReward) { this.lastDailyReward = lastDailyReward; }

    // === Politbiro API ===

    public Map<PolitburoPosition, PolitburoMember> getPolitburo() {
        if (politburo == null) politburo = new EnumMap<>(PolitburoPosition.class);
        return politburo;
    }

    public PolitburoMember getPolitburoMember(PolitburoPosition position) {
        return getPolitburo().get(position);
    }

    public PolitburoMember getPolitburoMemberByUUID(UUID uuid) {
        for (PolitburoMember m : getPolitburo().values()) {
            if (m.getUuid().equals(uuid)) return m;
        }
        return null;
    }

    public PolitburoPosition getPositionByUUID(UUID uuid) {
        for (Map.Entry<PolitburoPosition, PolitburoMember> entry : getPolitburo().entrySet()) {
            if (entry.getValue().getUuid().equals(uuid)) return entry.getKey();
        }
        return null;
    }

    public void appointPolitburo(PolitburoPosition position, PolitburoMember member) {
        getPolitburo().put(position, member);
    }

    public void removePolitburo(PolitburoPosition position) {
        getPolitburo().remove(position);
    }

    // === Party membership API ===

    public Set<UUID> getPartyMembers() {
        if (partyMembers == null) partyMembers = new HashSet<>();
        return partyMembers;
    }

    public boolean isPartyMember(UUID uuid) {
        return getPartyMembers().contains(uuid);
    }

    public void addPartyMember(UUID uuid) {
        getPartyMembers().add(uuid);
    }

    public boolean removePartyMember(UUID uuid) {
        return getPartyMembers().remove(uuid);
    }

    public int getPartyMemberCount() {
        return getPartyMembers().size();
    }

    // === Subsidi / pajak progresif tracking ===

    public long getLastSubsidyDistribution() { return lastSubsidyDistribution; }
    public void setLastSubsidyDistribution(long t) { this.lastSubsidyDistribution = t; }
    public long getLastTaxPhase() { return lastTaxPhase; }
    public void setLastTaxPhase(long t) { this.lastTaxPhase = t; }
    public long getLastFreeFoodDistribution() { return lastFreeFoodDistribution; }
    public void setLastFreeFoodDistribution(long t) { this.lastFreeFoodDistribution = t; }
    public double getTotalSubsidyPayouts() { return totalSubsidyPayouts; }
    public void setTotalSubsidyPayouts(double v) { this.totalSubsidyPayouts = v; }
    public void addSubsidyPayout(double amount) { this.totalSubsidyPayouts += amount; }

    // === Sekjen election (party voting) API ===

    public Map<UUID, UUID> getPartyVotes() {
        if (partyVotes == null) partyVotes = new HashMap<>();
        return partyVotes;
    }

    public void clearPartyVotes() {
        getPartyVotes().clear();
    }

    public long getElectionCycleStart() { return electionCycleStart; }
    public void setElectionCycleStart(long t) { this.electionCycleStart = t; }

    /**
     * Hitung perolehan vote per kandidat. Hanya menghitung vote dari pemain
     * yang masih anggota Partai (vote dari mantan party member dianggap gugur).
     */
    public Map<UUID, Integer> getVoteCounts() {
        Map<UUID, Integer> counts = new HashMap<>();
        for (Map.Entry<UUID, UUID> entry : getPartyVotes().entrySet()) {
            UUID voter = entry.getKey();
            UUID candidate = entry.getValue();
            if (!isPartyMember(voter)) continue; // gugur bila keluar Partai
            if (!isPartyMember(candidate)) continue; // kandidat juga harus party member
            counts.merge(candidate, 1, Integer::sum);
        }
        return counts;
    }

    /**
     * @return UUID kandidat dengan vote terbanyak. Bila tidak ada vote sama
     *         sekali, kembalikan null. Bila tie, pilih yang muncul pertama
     *         (deterministic order tidak dijamin tapi cukup untuk gameplay).
     */
    public UUID getCurrentLeadCandidate() {
        Map<UUID, Integer> counts = getVoteCounts();
        UUID leader = null;
        int maxVotes = -1;
        for (Map.Entry<UUID, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                leader = entry.getKey();
            }
        }
        return leader;
    }

    // === Phase 4C — Active decision state getters/setters ===

    public long getDefenseProtocolUntil() { return defenseProtocolUntil; }
    public void setDefenseProtocolUntil(long v) { this.defenseProtocolUntil = v; }
    public boolean isDefenseProtocolActive() {
        return System.currentTimeMillis() < defenseProtocolUntil;
    }

    public long getOffenseProtocolUntil() { return offenseProtocolUntil; }
    public void setOffenseProtocolUntil(long v) { this.offenseProtocolUntil = v; }
    public boolean isOffenseProtocolActive() {
        return System.currentTimeMillis() < offenseProtocolUntil;
    }

    public long getQuarantineUntil() { return quarantineUntil; }
    public void setQuarantineUntil(long v) { this.quarantineUntil = v; }
    public boolean isQuarantineActive() {
        return System.currentTimeMillis() < quarantineUntil;
    }

    public long getPlagueUntil() { return plagueUntil; }
    public void setPlagueUntil(long v) { this.plagueUntil = v; }
    public boolean isPlagueActive() {
        return System.currentTimeMillis() < plagueUntil;
    }

    public long getMarketEventUntil() { return marketEventUntil; }
    public void setMarketEventUntil(long v) { this.marketEventUntil = v; }
    public boolean isMarketEventActive() {
        return System.currentTimeMillis() < marketEventUntil;
    }

    public long getFieldMedicineCooldownUntil() { return fieldMedicineCooldownUntil; }
    public void setFieldMedicineCooldownUntil(long v) { this.fieldMedicineCooldownUntil = v; }
    public boolean isFieldMedicineOnCooldown() {
        return System.currentTimeMillis() < fieldMedicineCooldownUntil;
    }

    public long getVaccinationUntil() { return vaccinationUntil; }
    public void setVaccinationUntil(long v) { this.vaccinationUntil = v; }
    public boolean isVaccinationActive() {
        return System.currentTimeMillis() < vaccinationUntil;
    }

    public long getSensorMediaUntil() { return sensorMediaUntil; }
    public void setSensorMediaUntil(long v) { this.sensorMediaUntil = v; }
    public boolean isSensorMediaActive() {
        return System.currentTimeMillis() < sensorMediaUntil;
    }

    public long getGlorificationUntil() { return glorificationUntil; }
    public void setGlorificationUntil(long v) { this.glorificationUntil = v; }
    public boolean isGlorificationActive() {
        return System.currentTimeMillis() < glorificationUntil;
    }

    public long getMilitaryEmergencyUntil() { return militaryEmergencyUntil; }
    public void setMilitaryEmergencyUntil(long v) { this.militaryEmergencyUntil = v; }
    public boolean isMilitaryEmergencyActive() {
        return System.currentTimeMillis() < militaryEmergencyUntil;
    }

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

    // === Sanctions API ===

    public List<Sanction> getActiveSanctions() {
        if (activeSanctions == null) activeSanctions = new ArrayList<>();
        return activeSanctions;
    }

    public void addSanction(Sanction sanction) {
        getActiveSanctions().add(sanction);
    }

    public Sanction getActiveSanction(UUID targetUUID, Sanction.SanctionType type) {
        for (Sanction s : getActiveSanctions()) {
            if (s.getTargetUUID().equals(targetUUID) && s.getType() == type && !s.isExpired()) {
                return s;
            }
        }
        return null;
    }

    public boolean hasSanction(UUID targetUUID, Sanction.SanctionType type) {
        return getActiveSanction(targetUUID, type) != null;
    }

    public boolean removeSanction(UUID targetUUID, Sanction.SanctionType type) {
        return getActiveSanctions().removeIf(s ->
                s.getTargetUUID().equals(targetUUID) && s.getType() == type);
    }

    public void clearExpiredSanctions() {
        getActiveSanctions().removeIf(Sanction::isExpired);
    }

    // === Order & Decision History API ===

    private List<String> orderHistory = new ArrayList<>();

    public List<String> getOrderHistory() {
        if (orderHistory == null) {
            orderHistory = new ArrayList<>();
        }
        return orderHistory;
    }

    public void setOrderHistory(List<String> orderHistory) {
        this.orderHistory = orderHistory;
    }

    public void addOrderHistory(String name) {
        getOrderHistory().add(0, name); // Prepend to show newest first
        while (getOrderHistory().size() > 3) {
            getOrderHistory().remove(getOrderHistory().size() - 1);
        }
    }
}
