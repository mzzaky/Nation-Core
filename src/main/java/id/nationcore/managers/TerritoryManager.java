package id.nationcore.managers;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import id.nationcore.NationCore;
import id.nationcore.models.Nation;
import id.nationcore.models.Nation.CapitalLocation;
import id.nationcore.models.Treasury.TransactionType;

/**
 * Logic territory & capital claim. Sengaja dipisah dari {@link NationManager}
 * supaya event listener bisa dependency-inject hanya bagian ini tanpa
 * exposure operasi membership.
 *
 * Bentuk teritori: lingkaran 2D (radius pada bidang X-Z, ignore Y) — lebih
 * natural daripada kotak untuk "klaim radial dari titik berdiri".
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

    /**
     * @return Nation yang teritorinya mencakup lokasi tsb, atau null bila bebas.
     *         Bila ada overlap, mengembalikan nation dengan radius terkecil
     *         (paling spesifik) — tapi dalam praktiknya overlap dicegah saat klaim.
     */
    public Nation getNationAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        String worldName = loc.getWorld().getName();

        Nation closest = null;
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            if (!nation.hasCapital()) continue;
            CapitalLocation cap = nation.getCapital();
            if (!worldName.equals(cap.getWorld())) continue;

            double dx = loc.getX() - cap.getX();
            double dz = loc.getZ() - cap.getZ();
            double distSq = dx * dx + dz * dz;
            int radius = cap.getRadius();
            if (distSq <= (double) radius * radius) {
                if (closest == null || cap.getRadius() < closest.getCapital().getRadius()) {
                    closest = nation;
                }
            }
        }
        return closest;
    }

    /**
     * @return true bila lokasi di dalam teritori suatu nation DAN pemain
     *         bukan anggota nation tsb. Tidak ada teritori → false.
     */
    public boolean isProtectedFrom(Location loc, UUID playerUUID) {
        Nation atLoc = getNationAt(loc);
        if (atLoc == null) return false;
        return !atLoc.isMember(playerUUID);
    }

    /**
     * Cek apakah radius dari lokasi tsb akan overlap dengan teritori nation lain.
     */
    public Nation findOverlapping(Location loc, int radius, Nation excludeNation) {
        if (loc == null || loc.getWorld() == null) return null;
        String worldName = loc.getWorld().getName();
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            if (excludeNation != null && nation.getId().equals(excludeNation.getId())) continue;
            if (!nation.hasCapital()) continue;
            CapitalLocation cap = nation.getCapital();
            if (!worldName.equals(cap.getWorld())) continue;

            double dx = loc.getX() - cap.getX();
            double dz = loc.getZ() - cap.getZ();
            double distSq = dx * dx + dz * dz;
            // Overlap bila jarak antar pusat < radius1 + radius2
            int sumRadius = radius + cap.getRadius();
            if (distSq <= (double) sumRadius * sumRadius) {
                return nation;
            }
        }
        return null;
    }

    /**
     * Klaim ibukota di lokasi pemain. Hanya leader nation yang bisa klaim.
     * Validasi: pemain leader, belum ada capital sebelumnya (atau bisa relocate
     * dengan biaya — Phase 3 simple: tolak relocate), dan tidak overlap.
     */
    public Result claimCapital(Player leader, Location loc) {
        Nation nation = plugin.getNationManager().getNationOf(leader.getUniqueId());
        if (nation == null) return Result.fail("Anda tidak tergabung di nation manapun.");
        if (nation.getLeaderUUID() == null || !nation.getLeaderUUID().equals(leader.getUniqueId())) {
            return Result.fail("Hanya pemimpin nation yang dapat mengklaim ibukota.");
        }
        if (nation.hasCapital()) {
            return Result.fail("Nation Anda sudah memiliki ibukota. Relokasi belum didukung.");
        }
        if (loc == null || loc.getWorld() == null) {
            return Result.fail("Lokasi tidak valid.");
        }

        int defaultRadius = plugin.getConfig().getInt("nation.capital.default-radius", 50);

        Nation overlap = findOverlapping(loc, defaultRadius, nation);
        if (overlap != null) {
            return Result.fail("Lokasi terlalu dekat dengan teritori nation '" +
                    overlap.getName() + "'. Cari tempat lain.");
        }

        // Biaya klaim opsional dari config (default 0 — gratis untuk pertama)
        double cost = plugin.getConfig().getDouble("nation.capital.claim-cost", 0);
        if (cost > 0) {
            if (!plugin.getTreasuryManager().canAfford(nation, cost)) {
                return Result.fail("Kas nation tidak cukup. Biaya klaim: $" +
                        String.format("%,.0f", cost));
            }
            plugin.getTreasuryManager().withdraw(nation, TransactionType.MISC_EXPENSE, cost,
                    "Klaim ibukota " + nation.getName(), leader.getUniqueId());
        }

        CapitalLocation capital = new CapitalLocation(
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch(), defaultRadius);
        nation.setCapital(capital);

        plugin.getDataManager().saveNations();

        // Broadcast ke server
        Bukkit.broadcastMessage("§eIbukota §6" + nation.getName() +
                "§e berdiri di §f" + loc.getWorld().getName() +
                " (" + (int) loc.getX() + ", " + (int) loc.getY() + ", " + (int) loc.getZ() +
                ") §7radius §f" + defaultRadius + "§7 blok.");

        return Result.ok("Ibukota " + nation.getName() + " berhasil diklaim!");
    }

    /**
     * Bangun objek Location bukkit dari CapitalLocation tersimpan, atau null bila
     * world tidak terload.
     */
    public Location toBukkitLocation(CapitalLocation cap) {
        if (cap == null) return null;
        var world = Bukkit.getWorld(cap.getWorld());
        if (world == null) return null;
        return new Location(world, cap.getX(), cap.getY(), cap.getZ(), cap.getYaw(), cap.getPitch());
    }
}
