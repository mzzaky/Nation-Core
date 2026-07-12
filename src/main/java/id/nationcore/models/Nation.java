package id.nationcore.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    /**
     * Fake members (NPC) yang terdaftar sebagai anggota nation ini.
     * Key: UUID deterministik hasil FakeMember.generateNpcUUID().
     */
    private Map<UUID, FakeMember> fakeMembers;

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
    private ArenaSession arenaSession;
    private long lastExecutiveOrderTime;
    /**
     * Per-order cooldown tracking. Key = {@link ExecutiveOrder.ExecutiveOrderType}
     * name, value = epoch millis of the last time that specific order was issued
     * by this nation. Lazily initialized for backward compatibility with save
     * files created before per-order cooldowns existed.
     */
    private Map<String, Long> orderCooldowns;
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

    // === Territory State (Phase 3 — Border Management) ===
    /**
     * Every chunk this nation owns, stored as "world;chunkX;chunkZ" keys.
     * The capital chunk is always part of this set (seeded on creation and on
     * load-time migration of pre-territory save data).
     */
    private Set<String> territory;

    /**
     * Custom greeting shown to ANY player — member or not — who steps into this
     * nation's territory. {@code null}/blank means no welcome message is set.
     * May contain ampersand ({@code &}) colour codes.
     */
    private String welcomeMessage;

    /**
     * Whether this nation's borders are currently being visualized with world
     * particles. Defaults to {@code false} (off) and is toggled from the
     * Border Management menu.
     */
    private boolean borderVisible;

    /** Epoch millis of the last capital relocation — used to enforce cooldown. */
    private long lastCapitalRelocateAt;

    private String announcementMessage;
    private long announcementCreatedAt;
    private long lastAnnouncementTime;

    private Boolean taxEnabled = true;

    public Nation() {
        this.members = new HashMap<>();
        this.fakeMembers = new HashMap<>();
        this.treasury = new Treasury();
        this.activeOrders = new ArrayList<>();
        this.activeDecisions = new ArrayList<>();
        this.diplomacyRelations = new HashMap<>();
        this.diplomacyRequests = new ArrayList<>();
        this.territory = new LinkedHashSet<>();
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

    /**
     * Jumlah total anggota nation: real player + fake member (NPC).
     */
    public int getMemberCount() {
        int fakeCount = fakeMembers != null ? fakeMembers.size() : 0;
        return members.size() + fakeCount;
    }

    /** Jumlah anggota pemain nyata saja (tidak termasuk NPC). */
    public int getRealMemberCount() {
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

    // === Fake Member (NPC) API ===

    /**
     * Lazy-init guard: Gson mungkin deserialize tanpa memanggil constructor
     * sehingga fakeMembers bisa null pada data lama.
     */
    private Map<UUID, FakeMember> ensureFakeMembers() {
        if (fakeMembers == null) fakeMembers = new HashMap<>();
        return fakeMembers;
    }

    public void addFakeMember(FakeMember npc) {
        ensureFakeMembers().put(npc.getId(), npc);
    }

    public boolean removeFakeMember(UUID npcUUID) {
        return ensureFakeMembers().remove(npcUUID) != null;
    }

    public boolean isFakeMember(UUID uuid) {
        return ensureFakeMembers().containsKey(uuid);
    }

    public FakeMember getFakeMember(UUID uuid) {
        return ensureFakeMembers().get(uuid);
    }

    public int getFakeMemberCount() {
        return ensureFakeMembers().size();
    }

    public Collection<FakeMember> getAllFakeMembers() {
        return Collections.unmodifiableCollection(ensureFakeMembers().values());
    }

    public Map<UUID, FakeMember> getFakeMembers() {
        return ensureFakeMembers();
    }

    /**
     * Mencari FakeMember berdasarkan nama (case-insensitive).
     * Mengembalikan null jika tidak ditemukan.
     */
    public FakeMember getFakeMemberByName(String name) {
        if (name == null) return null;
        for (FakeMember npc : ensureFakeMembers().values()) {
            if (npc.getName().equalsIgnoreCase(name)) return npc;
        }
        return null;
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

    // === Territory API (Phase 3 — Border Management) ===

    /**
     * Owned chunk keys ("world;chunkX;chunkZ"). Lazily initialized so save data
     * created before the territory system loads without a manual migration.
     */
    public Set<String> getTerritory() {
        if (territory == null) territory = new LinkedHashSet<>();
        return territory;
    }

    public void setTerritory(Set<String> territory) {
        this.territory = territory;
    }

    /** Number of chunks currently claimed by this nation. */
    public int getTerritorySize() {
        return getTerritory().size();
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public boolean hasWelcomeMessage() {
        return welcomeMessage != null && !welcomeMessage.isBlank();
    }

    public boolean isBorderVisible() {
        return borderVisible;
    }

    public void setBorderVisible(boolean borderVisible) {
        this.borderVisible = borderVisible;
    }

    public long getLastCapitalRelocateAt() {
        return lastCapitalRelocateAt;
    }

    public void setLastCapitalRelocateAt(long lastCapitalRelocateAt) {
        this.lastCapitalRelocateAt = lastCapitalRelocateAt;
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

    public ArenaSession getArenaSession() {
        return arenaSession;
    }

    public void setArenaSession(ArenaSession arenaSession) {
        this.arenaSession = arenaSession;
    }

    public long getLastExecutiveOrderTime() {
        return lastExecutiveOrderTime;
    }

    public void setLastExecutiveOrderTime(long lastExecutiveOrderTime) {
        this.lastExecutiveOrderTime = lastExecutiveOrderTime;
    }

    /**
     * Epoch millis this nation last issued the given executive order type.
     * Returns 0 (never issued) when there is no record.
     */
    public long getOrderCooldown(String orderTypeName) {
        if (orderCooldowns == null) return 0L;
        return orderCooldowns.getOrDefault(orderTypeName, 0L);
    }

    public void setOrderCooldown(String orderTypeName, long time) {
        if (orderCooldowns == null) orderCooldowns = new HashMap<>();
        orderCooldowns.put(orderTypeName, time);
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

    public boolean isTaxEnabled() {
        return taxEnabled == null || taxEnabled;
    }

    public void setTaxEnabled(boolean taxEnabled) {
        this.taxEnabled = taxEnabled;
    }

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

    // === Announcement API ===

    public String getAnnouncementMessage() {
        return announcementMessage;
    }

    public void setAnnouncementMessage(String announcementMessage) {
        this.announcementMessage = announcementMessage;
    }

    public long getAnnouncementCreatedAt() {
        return announcementCreatedAt;
    }

    public void setAnnouncementCreatedAt(long announcementCreatedAt) {
        this.announcementCreatedAt = announcementCreatedAt;
    }

    public long getLastAnnouncementTime() {
        return lastAnnouncementTime;
    }

    public void setLastAnnouncementTime(long lastAnnouncementTime) {
        this.lastAnnouncementTime = lastAnnouncementTime;
    }
}
