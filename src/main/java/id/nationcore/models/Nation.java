package id.nationcore.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entitas inti pada sistem multi-nation.
 *
 * Setiap Nation memiliki:
 *   • identitas (id unik, nama, tag, jenis pemerintahan)
 *   • daftar anggota beserta role internalnya
 *   • Treasury sendiri (terpisah dari kas server lama)
 *   • lokasi ibukota opsional (akan dipakai pada fitur capital claim)
 *
 * Sub-state spesifik per jenis pemerintahan (kabinet republik, politbiro
 * komunis, dll.) hidup di dalam objek terpisah dan akan dirangkai oleh
 * manager-manager government pada fase berikutnya.
 */
public class Nation {

    /**
     * Role anggota relatif terhadap nation-nya.
     * Catatan: untuk REPUBLIC sebutan resmi adalah "Presiden / Wakil / Menteri",
     * sedangkan untuk COMMUNIST adalah "Sekjen / Politbiro / Anggota Partai".
     * Enum ini netral — flavor disesuaikan oleh GovernmentType saat ditampilkan.
     */
    public enum NationRole {
        LEADER,   // Presiden / Sekretaris Jenderal
        OFFICER,  // Wakil presiden / kabinet / politbiro
        CITIZEN   // Anggota biasa
    }

    public static class NationMember {
        private UUID uuid;
        private String name;
        private NationRole role;
        private long joinedAt;

        public NationMember(UUID uuid, String name, NationRole role) {
            this.uuid = uuid;
            this.name = name;
            this.role = role;
            this.joinedAt = System.currentTimeMillis();
        }

        public UUID getUuid() { return uuid; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public NationRole getRole() { return role; }
        public void setRole(NationRole role) { this.role = role; }
        public long getJoinedAt() { return joinedAt; }
        public void setJoinedAt(long joinedAt) { this.joinedAt = joinedAt; }
    }

    /**
     * Lokasi ibukota — disimpan sebagai data primitif agar mudah di-serialize
     * oleh Gson tanpa perlu kustomisasi.
     */
    public static class CapitalLocation {
        private String world;
        private double x;
        private double y;
        private double z;
        private float yaw;
        private float pitch;
        private int radius;
        private long claimedAt;

        public CapitalLocation() {}

        public CapitalLocation(String world, double x, double y, double z,
                               float yaw, float pitch, int radius) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.radius = radius;
            this.claimedAt = System.currentTimeMillis();
        }

        public String getWorld() { return world; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public float getYaw() { return yaw; }
        public float getPitch() { return pitch; }
        public int getRadius() { return radius; }
        public long getClaimedAt() { return claimedAt; }

        public void setRadius(int radius) { this.radius = radius; }
    }

    private String id;
    private String name;
    private String tag;
    private GovernmentType type;
    private long foundedAt;

    private UUID leaderUUID;
    private String leaderName;

    private Map<UUID, NationMember> members;

    private CapitalLocation capital;

    private Treasury treasury;

    /**
     * Sub-state pemerintahan Republik (presiden/cabinet/approval).
     * Otomatis di-init untuk type == REPUBLIC. Null untuk pemerintahan lain.
     */
    private Government republicGovernment;

    /**
     * Sub-state pemerintahan Komunis (Sekjen/Politbiro/Party).
     * Otomatis di-init untuk type == COMMUNIST. Null untuk pemerintahan lain.
     */
    private CommunistGovernment communistGovernment;

    /**
     * Sub-state pemerintahan Monarki (King/High Council/Royal Soldiers).
     * Otomatis di-init untuk type == MONARCHY. Null untuk pemerintahan lain.
     */
    private MonarchyGovernment monarchyGovernment;

    /**
     * Sub-state pemerintahan Caliphate (Caliph/Shura Council/State Scholars).
     * Otomatis di-init untuk type == CALIPHATE. Null untuk pemerintahan lain.
     */
    private CaliphateGovernment caliphateGovernment;

    // === Sub-state per-nation (Phase 2) ===
    // Election: hanya REPUBLIC yang menggunakan ini
    private Election election;
    private List<ExecutiveOrder> activeOrders;
    private List<CabinetDecision> activeDecisions;
    private RecallPetition recallPetition;
    private long lastExecutiveOrderTime;
    private int gamesThisTerm;

    // === Diplomacy State ===
    private Map<String, DiplomacyStatus> diplomacyRelations;
    private List<DiplomacyRequest> diplomacyRequests;

    // === Research State (Phase 2 — Nation Research) ===
    /** Per-nation research progress; lazily initialized. */
    private NationResearchData researchData;

    // === Shared Storage State (Communist Exclusive) ===
    /** Communal item storage accessible by all nation members; lazily initialized. */
    private SharedStorageData sharedStorageData;

    public Nation() {
        this.members = new HashMap<>();
        this.treasury = new Treasury();
        this.activeOrders = new ArrayList<>();
        this.activeDecisions = new ArrayList<>();
        this.diplomacyRelations = new HashMap<>();
        this.diplomacyRequests = new ArrayList<>();
    }

    public Nation(String id, String name, GovernmentType type, UUID founderUUID, String founderName) {
        this();
        this.id = id;
        this.name = name;
        this.type = type;
        this.tag = generateDefaultTag(name);
        this.foundedAt = System.currentTimeMillis();
        this.leaderUUID = founderUUID;
        this.leaderName = founderName;
        addMember(new NationMember(founderUUID, founderName, NationRole.LEADER));
        // Auto-init pemerintahan sesuai jenis: founder otomatis jadi pemimpin
        // pertama tanpa pemilu (founding term).
        if (type == GovernmentType.REPUBLIC) {
            initRepublicGovernment(founderUUID, founderName);
            this.election = new Election();
        } else if (type == GovernmentType.COMMUNIST) {
            initCommunistGovernment(founderUUID, founderName);
        } else if (type == GovernmentType.MONARCHY) {
            initMonarchyGovernment(founderUUID, founderName);
        } else if (type == GovernmentType.CALIPHATE) {
            initCaliphateGovernment(founderUUID, founderName);
        }
    }

    private void initCaliphateGovernment(UUID founderUUID, String founderName) {
        CaliphateGovernment cg = new CaliphateGovernment();
        cg.setCaliphUUID(founderUUID);
        cg.setCaliphName(founderName);
        cg.setAscensionTime(System.currentTimeMillis());
        cg.setLastCaliphActivity(System.currentTimeMillis());
        this.caliphateGovernment = cg;
    }

    private void initMonarchyGovernment(UUID founderUUID, String founderName) {
        MonarchyGovernment mg = new MonarchyGovernment();
        mg.setKingUUID(founderUUID);
        mg.setKingName(founderName);
        mg.setCoronationTime(System.currentTimeMillis());
        mg.setLastKingActivity(System.currentTimeMillis());
        this.monarchyGovernment = mg;
    }

    private void initRepublicGovernment(UUID founderUUID, String founderName) {
        Government gov = new Government();
        gov.setPresidentUUID(founderUUID);
        gov.setPresidentName(founderName);
        gov.setTermStartTime(System.currentTimeMillis());
        gov.setLastPresidentActivity(System.currentTimeMillis());
        gov.setConsecutiveTerms(1);
        this.republicGovernment = gov;
    }

    private void initCommunistGovernment(UUID founderUUID, String founderName) {
        CommunistGovernment cg = new CommunistGovernment();
        cg.setSecretaryGeneralUUID(founderUUID);
        cg.setSecretaryGeneralName(founderName);
        cg.setTermStartTime(System.currentTimeMillis());
        cg.setLastSecretaryActivity(System.currentTimeMillis());
        cg.setConsecutiveTerms(1);
        // Founder otomatis jadi anggota Partai
        cg.addPartyMember(founderUUID);
        this.communistGovernment = cg;
    }

    private static String generateDefaultTag(String nationName) {
        if (nationName == null || nationName.isBlank()) return "NTN";
        String cleaned = nationName.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (cleaned.length() >= 3) {
            return cleaned.substring(0, 3);
        }
        return cleaned.isEmpty() ? "NTN" : cleaned;
    }

    // === Membership API ===

    public void addMember(NationMember member) {
        members.put(member.getUuid(), member);
    }

    public boolean removeMember(UUID uuid) {
        return members.remove(uuid) != null;
    }

    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public NationMember getMember(UUID uuid) {
        return members.get(uuid);
    }

    public int getMemberCount() {
        return members.size();
    }

    public List<NationMember> getMembersByRole(NationRole role) {
        List<NationMember> result = new ArrayList<>();
        for (NationMember m : members.values()) {
            if (m.getRole() == role) result.add(m);
        }
        return result;
    }

    public Map<UUID, NationMember> getMembers() {
        return members;
    }

    // === Capital API ===

    public boolean hasCapital() {
        return capital != null && capital.getWorld() != null;
    }

    public CapitalLocation getCapital() {
        return capital;
    }

    public void setCapital(CapitalLocation capital) {
        this.capital = capital;
    }

    // === Treasury API ===

    public Treasury getTreasury() {
        if (treasury == null) treasury = new Treasury();
        return treasury;
    }

    public void setTreasury(Treasury treasury) {
        this.treasury = treasury;
    }

    // === Government sub-states ===

    public Government getRepublicGovernment() {
        return republicGovernment;
    }

    public void setRepublicGovernment(Government republicGovernment) {
        this.republicGovernment = republicGovernment;
    }

    public CommunistGovernment getCommunistGovernment() {
        return communistGovernment;
    }

    public void setCommunistGovernment(CommunistGovernment communistGovernment) {
        this.communistGovernment = communistGovernment;
    }

    public MonarchyGovernment getMonarchyGovernment() {
        return monarchyGovernment;
    }

    public void setMonarchyGovernment(MonarchyGovernment monarchyGovernment) {
        this.monarchyGovernment = monarchyGovernment;
    }

    public CaliphateGovernment getCaliphateGovernment() {
        return caliphateGovernment;
    }

    public void setCaliphateGovernment(CaliphateGovernment caliphateGovernment) {
        this.caliphateGovernment = caliphateGovernment;
    }

    // === Sub-state accessors (Phase 2) ===

    public Election getElection() {
        if (election == null && type == GovernmentType.REPUBLIC) {
            election = new Election();
        }
        return election;
    }

    public void setElection(Election election) {
        this.election = election;
    }

    public List<ExecutiveOrder> getActiveOrders() {
        if (activeOrders == null) activeOrders = new ArrayList<>();
        return activeOrders;
    }

    public void setActiveOrders(List<ExecutiveOrder> activeOrders) {
        this.activeOrders = activeOrders;
    }

    public List<CabinetDecision> getActiveDecisions() {
        if (activeDecisions == null) activeDecisions = new ArrayList<>();
        return activeDecisions;
    }

    public void setActiveDecisions(List<CabinetDecision> activeDecisions) {
        this.activeDecisions = activeDecisions;
    }

    public RecallPetition getRecallPetition() {
        return recallPetition;
    }

    public void setRecallPetition(RecallPetition recallPetition) {
        this.recallPetition = recallPetition;
    }

    public long getLastExecutiveOrderTime() {
        return lastExecutiveOrderTime;
    }

    public void setLastExecutiveOrderTime(long lastExecutiveOrderTime) {
        this.lastExecutiveOrderTime = lastExecutiveOrderTime;
    }

    public int getGamesThisTerm() {
        return gamesThisTerm;
    }

    public void setGamesThisTerm(int gamesThisTerm) {
        this.gamesThisTerm = gamesThisTerm;
    }

    // === Identity getters/setters ===

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public GovernmentType getType() { return type; }
    public void setType(GovernmentType type) { this.type = type; }

    public long getFoundedAt() { return foundedAt; }
    public void setFoundedAt(long foundedAt) { this.foundedAt = foundedAt; }

    public UUID getLeaderUUID() { return leaderUUID; }
    public void setLeaderUUID(UUID leaderUUID) { this.leaderUUID = leaderUUID; }

    public String getLeaderName() { return leaderName; }
    public void setLeaderName(String leaderName) { this.leaderName = leaderName; }

    // === Diplomacy API ===
    public Map<String, DiplomacyStatus> getDiplomacyRelations() {
        if (diplomacyRelations == null) diplomacyRelations = new HashMap<>();
        return diplomacyRelations;
    }

    public DiplomacyStatus getDiplomacyStatusWith(String otherNationId) {
        return getDiplomacyRelations().getOrDefault(otherNationId, DiplomacyStatus.PEACE);
    }

    public void setDiplomacyStatus(String otherNationId, DiplomacyStatus status) {
        if (status == DiplomacyStatus.PEACE) {
            getDiplomacyRelations().remove(otherNationId); // default
        } else {
            getDiplomacyRelations().put(otherNationId, status);
        }
    }

    public List<DiplomacyRequest> getDiplomacyRequests() {
        if (diplomacyRequests == null) diplomacyRequests = new ArrayList<>();
        return diplomacyRequests;
    }

    public void addDiplomacyRequest(DiplomacyRequest request) {
        getDiplomacyRequests().add(request);
    }

    public boolean removeDiplomacyRequest(String senderId) {
        return getDiplomacyRequests().removeIf(req -> req.getSenderNationId().equals(senderId));
    }

    public DiplomacyRequest getDiplomacyRequest(String senderId) {
        return getDiplomacyRequests().stream()
                .filter(req -> req.getSenderNationId().equals(senderId))
                .findFirst().orElse(null);
    }

    // === Research API ===

    /**
     * Returns the research state for this nation, initializing it on first
     * access so old save files (pre-research migration) get a default object
     * without requiring a manual data migration.
     */
    public NationResearchData getResearchData() {
        if (researchData == null) researchData = new NationResearchData();
        return researchData;
    }

    public void setResearchData(NationResearchData researchData) {
        this.researchData = researchData;
    }

    // === Shared Storage API ===

    /**
     * Returns the shared storage state for this communist nation,
     * initializing it on first access so old save files get a default object
     * without requiring a manual data migration.
     */
    public SharedStorageData getSharedStorageData() {
        if (sharedStorageData == null) sharedStorageData = new SharedStorageData();
        sharedStorageData.ensureSize();
        return sharedStorageData;
    }

    public void setSharedStorageData(SharedStorageData sharedStorageData) {
        this.sharedStorageData = sharedStorageData;
    }
}
