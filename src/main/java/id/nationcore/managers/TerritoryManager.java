package id.nationcore.managers;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import id.nationcore.NationCore;
import id.nationcore.models.Nation;
import id.nationcore.models.Nation.CapitalLocation;
import id.nationcore.utils.MessageUtils;

/**
 * Territory &amp; capital claim logic. Deliberately split from {@link NationManager}
 * so event listeners can depend-inject just this slice without exposing the
 * membership operations.
 *
 * The territory model is chunk-based: every nation owns a set of 16×16 chunks
 * (keyed as {@code "world;chunkX;chunkZ"}). One of those chunks is additionally
 * flagged as the capital via {@link CapitalLocation}. A location belongs to a
 * nation when its chunk is in that nation's owned set.
 */
public class TerritoryManager {

    public static class Result {
        private final boolean success;
        private final String message;

        private Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        public static Result ok(String msg) { return new Result(true, msg); }
        public static Result fail(String msg) { return new Result(false, msg); }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    private final NationCore plugin;

    public TerritoryManager(NationCore plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------
    // Chunk key helpers
    // ---------------------------------------------------------------

    /** Builds the canonical owned-chunk key for a world + chunk coordinate. */
    public static String chunkKey(String world, int chunkX, int chunkZ) {
        return world + ";" + chunkX + ";" + chunkZ;
    }

    /** Builds the owned-chunk key for the chunk containing {@code loc}. */
    public static String chunkKeyOf(Location loc) {
        return chunkKey(loc.getWorld().getName(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }

    /**
     * The chunk key of a nation's capital, or {@code null} when it has none.
     * Uses floor-based chunk maths so it matches {@link #chunkKeyOf(Location)}
     * for negative coordinates.
     */
    public String capitalChunkKey(Nation nation) {
        if (nation == null || !nation.hasCapital()) return null;
        CapitalLocation cap = nation.getCapital();
        int cx = ((int) Math.floor(cap.getX())) >> 4;
        int cz = ((int) Math.floor(cap.getZ())) >> 4;
        return chunkKey(cap.getWorld(), cx, cz);
    }

    /** @return true if {@code nation} owns the chunk at the given world coordinate. */
    public boolean ownsChunkAt(Nation nation, String world, int chunkX, int chunkZ) {
        if (nation == null) return false;
        String key = chunkKey(world, chunkX, chunkZ);
        if (nation.getTerritory().contains(key)) return true;
        // Defensive fallback for any save data whose capital was never seeded.
        return key.equals(capitalChunkKey(nation));
    }

    /** @return true if the given chunk is this nation's capital chunk. */
    public boolean isCapitalChunk(Nation nation, String world, int chunkX, int chunkZ) {
        return chunkKey(world, chunkX, chunkZ).equals(capitalChunkKey(nation));
    }

    // ---------------------------------------------------------------
    // Lookups
    // ---------------------------------------------------------------

    /**
     * @return the nation whose territory contains this location, or null when the
     *         chunk is unclaimed.
     */
    public Nation getNationAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        String world = loc.getWorld().getName();
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;

        for (Nation nation : plugin.getNationManager().getAllNations()) {
            if (ownsChunkAt(nation, world, cx, cz)) {
                return nation;
            }
        }
        return null;
    }

    /**
     * @return true when the location lies inside a nation's territory AND the
     *         player is NOT a member of that nation. Unclaimed → false.
     */
    public boolean isProtectedFrom(Location loc, UUID playerUUID) {
        Nation atLoc = getNationAt(loc);
        if (atLoc == null) return false;
        return !atLoc.isMember(playerUUID);
    }

    /**
     * @return the nation (other than {@code excludeNation}) that owns this
     *         location's chunk, or null if none.
     */
    public Nation findOverlapping(Location loc, Nation excludeNation) {
        if (loc == null || loc.getWorld() == null) return null;
        String world = loc.getWorld().getName();
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;

        for (Nation nation : plugin.getNationManager().getAllNations()) {
            if (excludeNation != null && nation.getId().equals(excludeNation.getId())) continue;
            if (ownsChunkAt(nation, world, cx, cz)) {
                return nation;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------
    // Authorization
    // ---------------------------------------------------------------

    /** Only the nation leader (or an admin) may reshape a nation's borders. */
    public boolean canManageTerritory(Player player, Nation nation) {
        if (player == null || nation == null) return false;
        if (player.hasPermission("nation.admin")) return true;
        return nation.getLeaderUUID() != null
                && nation.getLeaderUUID().equals(player.getUniqueId());
    }

    // ---------------------------------------------------------------
    // Capital claim (first-time) — still used by /nc capital claim
    // ---------------------------------------------------------------

    /**
     * Claims the capital at the player's location. Only the nation leader can
     * claim, and only when the nation has no capital yet.
     */
    public Result claimCapital(Player leader, Location loc) {
        Nation nation = plugin.getNationManager().getNationOf(leader.getUniqueId());
        if (nation == null) return Result.fail("You are not part of any nation.");
        if (nation.getLeaderUUID() == null || !nation.getLeaderUUID().equals(leader.getUniqueId())) {
            return Result.fail("Only the nation leader can claim the capital.");
        }
        if (nation.hasCapital()) {
            return Result.fail("Your nation already has a capital. Use Reallocate Capital instead.");
        }
        if (loc == null || loc.getWorld() == null) {
            return Result.fail("Invalid location.");
        }

        Nation overlap = findOverlapping(loc, nation);
        if (overlap != null) {
            return Result.fail("This chunk is already claimed by nation '" +
                    overlap.getName() + "'.");
        }

        double cost = plugin.getConfig().getDouble("nation.capital.claim-cost", 0);
        if (cost > 0) {
            if (!plugin.getTreasuryManager().canAfford(nation, cost)) {
                return Result.fail("The nation treasury does not have enough funds. Claim cost: $" +
                        String.format("%,.0f", cost));
            }
            plugin.getTreasuryManager().withdraw(nation, id.nationcore.models.Treasury.TransactionType.MISC_EXPENSE, cost,
                    "Capital claim " + nation.getName(), leader.getUniqueId());
        }

        CapitalLocation capital = new CapitalLocation(
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch(), 0);
        nation.setCapital(capital);
        nation.getTerritory().add(chunkKeyOf(loc));

        plugin.getDataManager().saveNations();

        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;

        Bukkit.broadcastMessage("§eCapital of §6" + nation.getName() +
                "§e has been established at chunk §f(" + chunkX + ", " + chunkZ + ")§e in world §f" + loc.getWorld().getName() + "§e.");

        return Result.ok("The capital of " + nation.getName() + " has been successfully claimed!");
    }

    // ---------------------------------------------------------------
    // Border Management operations
    // ---------------------------------------------------------------

    /**
     * Claims the chunk the leader is standing in as nation territory.
     * Requires payment from the leader's Vault balance, the chunk to be
     * unclaimed, and the chunk to border existing territory.
     */
    public Result claimTerritory(Player leader, Location loc) {
        Nation nation = plugin.getNationManager().getNationOf(leader.getUniqueId());
        if (nation == null) return Result.fail("You are not part of any nation.");
        if (!canManageTerritory(leader, nation)) {
            return Result.fail("Only the nation leader can claim territory.");
        }
        if (loc == null || loc.getWorld() == null) return Result.fail("Invalid location.");
        if (!nation.hasCapital()) {
            return Result.fail("Your nation must establish a capital before expanding its territory.");
        }

        String world = loc.getWorld().getName();
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;
        String key = chunkKey(world, cx, cz);

        if (ownsChunkAt(nation, world, cx, cz)) {
            return Result.fail("Your nation already owns the chunk you are standing in.");
        }

        Nation overlap = findOverlapping(loc, nation);
        if (overlap != null) {
            return Result.fail("This chunk is already claimed by nation '" + overlap.getName() + "'.");
        }

        if (!isAdjacentToOwn(nation, world, cx, cz)) {
            return Result.fail("You can only claim chunks that border your existing territory.");
        }

        double cost = plugin.getConfig().getDouble("nation.territory.claim-cost", 25000);
        if (cost > 0) {
            if (!plugin.getVaultHook().has(leader.getUniqueId(), cost)) {
                return Result.fail("You need $" + format(cost) + " in your balance to claim this chunk.");
            }
            plugin.getVaultHook().withdraw(leader.getUniqueId(), cost);
        }

        nation.getTerritory().add(key);
        plugin.getDataManager().saveNations();

        return Result.ok("Claimed chunk (" + cx + ", " + cz + ") for " + nation.getName() +
                (cost > 0 ? " for $" + format(cost) + "." : "."));
    }

    /**
     * Releases the chunk the leader is standing in from the nation's territory.
     * The capital chunk is protected and cannot be released.
     */
    public Result disbandTerritory(Player leader, Location loc) {
        Nation nation = plugin.getNationManager().getNationOf(leader.getUniqueId());
        if (nation == null) return Result.fail("You are not part of any nation.");
        if (!canManageTerritory(leader, nation)) {
            return Result.fail("Only the nation leader can release territory.");
        }
        if (loc == null || loc.getWorld() == null) return Result.fail("Invalid location.");

        String world = loc.getWorld().getName();
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;
        String key = chunkKey(world, cx, cz);

        if (!ownsChunkAt(nation, world, cx, cz)) {
            return Result.fail("Your nation does not own the chunk you are standing in.");
        }
        if (isCapitalChunk(nation, world, cx, cz)) {
            return Result.fail("The capital chunk cannot be released. Relocate the capital first.");
        }

        nation.getTerritory().remove(key);
        plugin.getDataManager().saveNations();

        return Result.ok("Released chunk (" + cx + ", " + cz + ") from " + nation.getName() + ".");
    }

    /**
     * Moves the capital marker to the chunk the leader is standing in. The chunk
     * must already be owned, is charged against the leader's Vault balance, and
     * is gated behind a real-time cooldown.
     */
    public Result reallocateCapital(Player leader, Location loc) {
        Nation nation = plugin.getNationManager().getNationOf(leader.getUniqueId());
        if (nation == null) return Result.fail("You are not part of any nation.");
        if (!canManageTerritory(leader, nation)) {
            return Result.fail("Only the nation leader can relocate the capital.");
        }
        if (loc == null || loc.getWorld() == null) return Result.fail("Invalid location.");
        if (!nation.hasCapital()) {
            return Result.fail("Your nation does not have a capital to relocate.");
        }

        String world = loc.getWorld().getName();
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;
        String key = chunkKey(world, cx, cz);

        if (!ownsChunkAt(nation, world, cx, cz)) {
            return Result.fail("You can only relocate the capital to a chunk your nation already owns.");
        }
        if (isCapitalChunk(nation, world, cx, cz)) {
            return Result.fail("This chunk is already your capital.");
        }

        long cooldownMs = (long) (plugin.getConfig()
                .getDouble("nation.territory.capital-relocate-cooldown-hours", 24) * 3_600_000L);
        long since = System.currentTimeMillis() - nation.getLastCapitalRelocateAt();
        if (nation.getLastCapitalRelocateAt() > 0 && since < cooldownMs
                && !leader.hasPermission("nation.admin")) {
            long remaining = cooldownMs - since;
            return Result.fail("Capital relocation is on cooldown. Remaining: " +
                    MessageUtils.formatTime(remaining) + ".");
        }

        double cost = plugin.getConfig().getDouble("nation.territory.capital-relocate-cost", 50000);
        if (cost > 0) {
            if (!plugin.getVaultHook().has(leader.getUniqueId(), cost)) {
                return Result.fail("You need $" + format(cost) + " in your balance to relocate the capital.");
            }
            plugin.getVaultHook().withdraw(leader.getUniqueId(), cost);
        }

        // Keep the (already-owned) destination in the set and move the marker.
        nation.getTerritory().add(key);
        CapitalLocation newCapital = new CapitalLocation(
                world, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), 0);
        nation.setCapital(newCapital);
        nation.setLastCapitalRelocateAt(System.currentTimeMillis());
        plugin.getDataManager().saveNations();

        return Result.ok("Relocated the capital of " + nation.getName() +
                " to chunk (" + cx + ", " + cz + ").");
    }

    /** Orthogonal (N/S/E/W) adjacency to any chunk the nation already owns. */
    private boolean isAdjacentToOwn(Nation nation, String world, int cx, int cz) {
        return ownsChunkAt(nation, world, cx + 1, cz)
                || ownsChunkAt(nation, world, cx - 1, cz)
                || ownsChunkAt(nation, world, cx, cz + 1)
                || ownsChunkAt(nation, world, cx, cz - 1);
    }

    private String format(double value) {
        return String.format("%,.0f", value);
    }

    // ---------------------------------------------------------------
    // Misc
    // ---------------------------------------------------------------

    /**
     * Builds a bukkit Location from a stored CapitalLocation, or null when the
     * world is not loaded.
     */
    public Location toBukkitLocation(CapitalLocation cap) {
        if (cap == null) return null;
        var world = Bukkit.getWorld(cap.getWorld());
        if (world == null) return null;
        return new Location(world, cap.getX(), cap.getY(), cap.getZ(), cap.getYaw(), cap.getPitch());
    }
}
