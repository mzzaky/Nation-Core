package id.nationcore.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import id.nationcore.NationCore;
import id.nationcore.models.Government;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.Nation;
import id.nationcore.models.Nation.NationMember;
import id.nationcore.models.Nation.NationRole;
import id.nationcore.models.PlayerData;
import id.nationcore.models.Treasury;
import id.nationcore.models.Treasury.TransactionType;

/**
 * Central for all multi-nation operations: creation, disbanding,
 * membership, and nation searching.
 *
 * This manager stores in-memory state; persistence is handled by
 * {@link DataManager} which calls {@link #getAllNations()}
 * when writing to disk.
 */
public class NationManager {

    /** Structured result for operations that can fail for various reasons. */
    public static class Result {
        private final boolean success;
        private final String message;
        private final Nation nation;

        private Result(boolean success, String message, Nation nation) {
            this.success = success;
            this.message = message;
            this.nation = nation;
        }

        public static Result ok(String message, Nation nation) { return new Result(true, message, nation); }
        public static Result fail(String message) { return new Result(false, message, null); }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Nation getNation() { return nation; }
    }

    private final NationCore plugin;

    /** Key: nation id (UUID string). Value: Nation. */
    private final Map<String, Nation> nations = new HashMap<>();

    /**
     * Players who just chose a government type in CreateNationGUI and
     * are waiting to type the nation name in chat.
     */
    private final Map<UUID, GovernmentType> pendingCreations = new HashMap<>();

    /**
     * Players requesting to change the nation name.
     */
    private final Map<UUID, Nation> pendingRenames = new HashMap<>();

    public NationManager(NationCore plugin) {
        this.plugin = plugin;
    }

    public void setPendingCreation(UUID uuid, GovernmentType type) {
        pendingCreations.put(uuid, type);
    }

    public GovernmentType getPendingCreation(UUID uuid) {
        return pendingCreations.get(uuid);
    }

    public GovernmentType consumePendingCreation(UUID uuid) {
        return pendingCreations.remove(uuid);
    }

    public boolean hasPendingCreation(UUID uuid) {
        return pendingCreations.containsKey(uuid);
    }

    public void setPendingRename(UUID uuid, Nation nation) {
        pendingRenames.put(uuid, nation);
    }

    public Nation getPendingRename(UUID uuid) {
        return pendingRenames.get(uuid);
    }

    public Nation consumePendingRename(UUID uuid) {
        return pendingRenames.remove(uuid);
    }

    public boolean hasPendingRename(UUID uuid) {
        return pendingRenames.containsKey(uuid);
    }

    // ---------------------------------------------------------------
    // Persistence bridge — called by DataManager during load/save
    // ---------------------------------------------------------------

    public void loadNations(Collection<Nation> loaded) {
        nations.clear();
        if (loaded == null) return;
        for (Nation n : loaded) {
            if (n == null || n.getId() == null) continue;
            // Migrate pre-territory save data: guarantee the capital chunk is part
            // of the owned-chunk set so the chunk-based lookups remain accurate.
            if (n.hasCapital()) {
                Nation.CapitalLocation cap = n.getCapital();
                int cx = ((int) Math.floor(cap.getX())) >> 4;
                int cz = ((int) Math.floor(cap.getZ())) >> 4;
                n.getTerritory().add(TerritoryManager.chunkKey(cap.getWorld(), cx, cz));
            }
            nations.put(n.getId(), n);
        }
    }

    public Collection<Nation> getAllNations() {
        return nations.values();
    }

    public List<Nation> getNationsSortedByMembers() {
        List<Nation> sorted = new ArrayList<>(nations.values());
        sorted.sort((a, b) -> Integer.compare(b.getMemberCount(), a.getMemberCount()));
        return sorted;
    }

    public int getNationCount() {
        return nations.size();
    }

    // ---------------------------------------------------------------
    // Lookup
    // ---------------------------------------------------------------

    public Nation getNation(String id) {
        return nations.get(id);
    }

    public Nation getNationByName(String name) {
        if (name == null) return null;
        String needle = name.trim().toLowerCase(Locale.ROOT);
        for (Nation n : nations.values()) {
            if (n.getName() != null && n.getName().toLowerCase(Locale.ROOT).equals(needle)) {
                return n;
            }
        }
        return null;
    }

    public Nation getNationOf(UUID playerUUID) {
        PlayerData data = plugin.getDataManager().getPlayerData(playerUUID);
        if (data == null || !data.hasNation()) return null;
        return nations.get(data.getNationId());
    }

    public boolean hasNation(UUID playerUUID) {
        return getNationOf(playerUUID) != null;
    }

    /**
     * Mencari nation tempat sebuah NPC (fake member) terdaftar.
     * Menggunakan data langsung di fakeMembers map pada setiap nation.
     *
     * @return Nation yang memiliki NPC ini, atau null jika tidak ditemukan
     */
    public Nation getNationOfNpc(UUID npcUUID) {
        for (Nation nation : nations.values()) {
            if (nation.isFakeMember(npcUUID)) return nation;
        }
        return null;
    }

    /**
     * @return The player's nation treasury if it exists, or the global legacy treasury
     *         (old data before the multi-nation system) as a fallback.
     */
    public Treasury resolveTreasury(UUID playerUUID) {
        Nation n = getNationOf(playerUUID);
        return n != null ? n.getTreasury() : plugin.getDataManager().getTreasury();
    }

    /**
     * @return The player's Republic Government if it exists, or the global legacy Government
     *         as a fallback. Returns null for non-REPUBLIC nations
     *         (to be replaced by CommunistGovernment in Phase 4).
     */
    public Government resolveGovernment(UUID playerUUID) {
        Nation n = getNationOf(playerUUID);
        if (n == null) return plugin.getDataManager().getGovernment();
        if (n.getType() == GovernmentType.REPUBLIC) return n.getRepublicGovernment();
        return null;
    }

    // ---------------------------------------------------------------
    // Creation
    // ---------------------------------------------------------------

    /**
     * Creates a new nation. Validates unique name, vault requirements, and
     * ensures the founder is not already in any nation.
     *
     * Valid name: 3–24 alphanumeric characters + spaces.
     */
    public Result createNation(Player founder, String desiredName, GovernmentType type) {
        if (founder == null) return Result.fail("Invalid player.");
        if (type == null) return Result.fail("Invalid government type.");

        String validation = validateNationName(desiredName);
        if (validation != null) return Result.fail(validation);

        if (getNationByName(desiredName) != null) {
            return Result.fail("Nation name is already taken.");
        }

        UUID founderUUID = founder.getUniqueId();
        if (hasNation(founderUUID)) {
            return Result.fail("You are already in a nation. Leave first to create a new one.");
        }

        // Check if the current chunk is already claimed by another nation
        org.bukkit.Location loc = founder.getLocation();
        Nation atLoc = plugin.getTerritoryManager().getNationAt(loc);
        if (atLoc != null) {
            return Result.fail("The chunk you are standing on is already claimed by nation '" + atLoc.getName() + "'.");
        }

        // Check vault requirements
        double cost = plugin.getNationCreationCost(type);
        if (plugin.getVaultHook().getBalance(founderUUID) < cost) {
            return Result.fail("You need at least $" + formatNumber(cost) + " to establish a nation.");
        }

        // Check playtime requirements (hours)
        double minPlaytimeHours = plugin.getNationCreationMinPlaytime(type);
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(founderUUID, founder.getName());
        if (minPlaytimeHours > 0 && data.getPlaytimeHours() < minPlaytimeHours) {
            return Result.fail("You need at least " + minPlaytimeHours + " hours of playtime to establish a nation.");
        }

        // Withdraw establishment fee
        if (!plugin.getVaultHook().withdraw(founderUUID, cost)) {
            return Result.fail("Failed to withdraw the establishment fee from your balance.");
        }

        // Buat nation baru
        String id = UUID.randomUUID().toString();
        Nation nation = new Nation(id, desiredName.trim(), type, founderUUID, founder.getName());

        // Auto-claim the capital chunk
        Nation.CapitalLocation capital = new Nation.CapitalLocation(
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch(), 0);
        nation.setCapital(capital);
        nation.getTerritory().add(TerritoryManager.chunkKeyOf(loc));

        // Starting fund for nation treasury (calculated as percentage of creation cost).
        // Nilai diambil dari config sebagai persentase (0-100), lalu dikalikan dengan cost.
        double startingTreasuryPercent = plugin.getNationCreationStartingTreasuryPercent(type);
        double startingFund = (cost * startingTreasuryPercent / 100.0);
        if (startingFund > 0) {
            nation.getTreasury().deposit(TransactionType.TERM_START_FUND, startingFund,
                    "Initial nation establishment fund (" + String.format("%.0f", startingTreasuryPercent) + "% of creation cost)", founderUUID);
        }

        nations.put(id, nation);

        // Update player data
        data.setNationId(id);
        data.setNationRole(NationRole.LEADER);
        data.setNationJoinedAt(System.currentTimeMillis());

        plugin.getDataManager().saveNations();
        plugin.getDataManager().savePlayerData();

        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        Bukkit.broadcastMessage("§eCapital of §6" + nation.getName() +
                "§e has been established at chunk §f(" + chunkX + ", " + chunkZ + ")§e in world §f" + loc.getWorld().getName() + "§e.");

        return Result.ok("Nation '" + nation.getName() + "' has been successfully established!", nation);
    }

    // ---------------------------------------------------------------
    // Membership
    // ---------------------------------------------------------------

    public Result joinNation(Player player, Nation nation) {
        if (nation == null) return Result.fail("Nation not found.");
        UUID uuid = player.getUniqueId();

        if (hasNation(uuid)) {
            return Result.fail("You are already in a nation.");
        }

        nation.addMember(new NationMember(uuid, player.getName(), NationRole.CITIZEN));

        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(uuid, player.getName());
        data.setNationId(nation.getId());
        data.setNationRole(NationRole.CITIZEN);
        data.setNationJoinedAt(System.currentTimeMillis());

        plugin.getDataManager().saveNations();
        plugin.getDataManager().savePlayerData();

        return Result.ok("You have successfully joined " + nation.getName() + ".", nation);
    }

    /**
     * Member leaves voluntarily. LEADER cannot leave directly —
     * must transfer leadership or disband first.
     */
    public Result leaveNation(Player player) {
        Nation nation = getNationOf(player.getUniqueId());
        if (nation == null) return Result.fail("You are not in any nation.");

        if (nation.getLeaderUUID() != null && nation.getLeaderUUID().equals(player.getUniqueId())) {
            return Result.fail("The leader cannot leave. Transfer leadership or disband the nation first.");
        }

        nation.removeMember(player.getUniqueId());

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data != null) data.clearNation();

        plugin.getDataManager().saveNations();
        plugin.getDataManager().savePlayerData();

        return Result.ok("You have left nation " + nation.getName() + ".", nation);
    }

    /**
     * Forcefully remove a member from the nation (kick). Can only be done by
     * the nation leader (General Secretary / President) or an admin. The target cannot
     * be the nation leader / General Secretary.
     *
     * @param kicker      the player performing the kick (must be leader/admin)
     * @param targetUUID  UUID of the player to be kicked
     * @return Result containing status and message
     */
    public Result kickMember(Player kicker, UUID targetUUID) {
        Nation nation = getNationOf(kicker.getUniqueId());
        if (nation == null) return Result.fail("You are not in any nation.");

        boolean isLeader = nation.getLeaderUUID() != null
                && nation.getLeaderUUID().equals(kicker.getUniqueId());
        boolean isAdmin  = kicker.hasPermission("nation.admin");
        if (!isLeader && !isAdmin) {
            return Result.fail("Only the nation leader can kick members.");
        }

        if (targetUUID.equals(kicker.getUniqueId())) {
            return Result.fail("You cannot kick yourself. Use /nc leave.");
        }

        NationMember target = nation.getMember(targetUUID);
        if (target == null) return Result.fail("That player is not a member of this nation.");

        if (target.getRole() == NationRole.LEADER) {
            return Result.fail("Cannot kick the nation leader.");
        }

        // Clean up Party and Politburo membership if COMMUNIST
        if (nation.getCommunistGovernment() != null) {
            var cg = nation.getCommunistGovernment();
            var politburoPos = cg.getPositionByUUID(targetUUID);
            if (politburoPos != null) cg.removePolitburo(politburoPos);
            cg.removePartyMember(targetUUID);
        }

        // Clean up Shura Council & State Scholars membership if CALIPHATE
        if (nation.getCaliphateGovernment() != null) {
            var caliphate = nation.getCaliphateGovernment();
            caliphate.removeShuraMember(targetUUID);
            caliphate.removeScholar(targetUUID);
        }

        nation.removeMember(targetUUID);

        PlayerData data = plugin.getDataManager().getPlayerData(targetUUID);
        if (data != null) data.clearNation();

        // Notify target if online
        org.bukkit.entity.Player targetPlayer = org.bukkit.Bukkit.getPlayer(targetUUID);
        if (targetPlayer != null) {
            plugin.getServer().getScheduler().runTask(plugin, () ->
                id.nationcore.utils.MessageUtils.send(targetPlayer,
                        "§cYou have been kicked from nation §6" + nation.getName() + "§c.")
            );
        }

        plugin.getDataManager().saveNations();
        plugin.getDataManager().savePlayerData();

        return Result.ok(target.getName() + " has been successfully kicked from " + nation.getName() + ".", nation);
    }

    /**
     * Leader disbands the nation. All members will be released from membership;
     * remaining treasury is forfeited (for Phase 1 — can be returned in future phases).
     */
    public Result disbandNation(Player leader) {
        Nation nation = getNationOf(leader.getUniqueId());
        if (nation == null) return Result.fail("You are not in any nation.");

        if (nation.getLeaderUUID() == null || !nation.getLeaderUUID().equals(leader.getUniqueId())) {
            plugin.getLogger().info("[DEBUG] Disband failed for " + leader.getName());
            plugin.getLogger().info("[DEBUG] Nation Leader UUID: " + nation.getLeaderUUID());
            plugin.getLogger().info("[DEBUG] Player UUID: " + leader.getUniqueId());
            return Result.fail("Only the nation leader can disband the nation.");
        }

        // Release all members
        for (UUID memberUUID : new ArrayList<>(nation.getMembers().keySet())) {
            PlayerData data = plugin.getDataManager().getPlayerData(memberUUID);
            if (data != null) data.clearNation();
        }

        nations.remove(nation.getId());

        plugin.getDataManager().saveNations();
        plugin.getDataManager().savePlayerData();

        // Broadcast
        Bukkit.broadcastMessage("§eNation §6" + nation.getName() + "§e has been disbanded by its leader.");

        return Result.ok("Nation '" + nation.getName() + "' has been successfully disbanded.", nation);
    }

    // ---------------------------------------------------------------
    // Validation helpers
    // ---------------------------------------------------------------

    /**
     * @return null if valid; error message if not.
     */
    public static String validateNationName(String name) {
        if (name == null || name.isBlank()) return "Nation name cannot be empty.";
        String trimmed = name.trim();
        if (trimmed.length() < 3) return "Nation name must be at least 3 characters.";
        if (trimmed.length() > 24) return "Nation name can be at most 24 characters.";
        if (!trimmed.matches("[A-Za-z0-9 ]+")) {
            return "Nation name can only contain letters, numbers, and spaces.";
        }
        return null;
    }

    private static String formatNumber(double value) {
        return String.format("%,.0f", value);
    }
}
