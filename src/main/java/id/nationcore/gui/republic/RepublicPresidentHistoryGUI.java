package id.nationcore.gui.republic;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import id.nationcore.NationCore;
import id.nationcore.models.PresidentHistory;
import id.nationcore.utils.MessageUtils;

/**
 * President History GUI - Displays detailed historical records of all past
 * presidents.
 *
 * <p>
 * Main view: paginated list of president entries (player heads),
 * each showing rank, name, term duration, approval rating, and end reason.
 *
 * <p>
 * Detail view: clicking a president head opens a full record breakdown
 * including cabinet members, executive orders, term dates, and accomplishments.
 *
 * <p>
 * Hall of Fame panel (bottom row) highlights the record holders for
 * longest term, highest approval, and most executive orders issued.
 */
public class RepublicPresidentHistoryGUI {

    // ─────────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────────

    public static final String HISTORY_TITLE = "§5§l📜 PRESIDENT HISTORY 📜";
    public static final String DETAIL_TITLE = "§d§l🏛 PRESIDENT RECORD 🏛";

    private static final int INVENTORY_SIZE = 54;
    private static final int[] HISTORY_SLOTS = { 10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34 };

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd MMM yyyy");

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────

    private final NationCore plugin;

    /**
     * Tracks which history record a player is currently viewing in the detail GUI.
     * Key = viewer UUID, Value = index in the history list.
     */
    private final Map<UUID, Integer> viewingRecord = new HashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────

    public RepublicPresidentHistoryGUI(NationCore plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Opens the main President History list to {@code player}.
     */
    public void openHistoryMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, INVENTORY_SIZE, HISTORY_TITLE);
        List<PresidentHistory.PresidentRecord> history = plugin.getDataManager().getAllPresidentHistory();

        // ── Section header ──────────────────────────────────────────────────
        ItemStack header = createItem(Material.ENCHANTED_BOOK,
                "§5§l📜 Presidential Records",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Total presidents recorded: §f" + history.size(),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Click any president to view",
                "§7their full record in detail.");
        inv.setItem(4, header);

        // ── President entries ────────────────────────────────────────────────
        if (history.isEmpty()) {
            ItemStack empty = createItem(Material.PAPER,
                    "§7§lNo Records Yet",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7No president has",
                    "§7completed a term yet.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            inv.setItem(22, empty);
        } else {
            int maxShow = Math.min(history.size(), HISTORY_SLOTS.length);
            for (int i = 0; i < maxShow; i++) {
                PresidentHistory.PresidentRecord rec = history.get(i);
                inv.setItem(HISTORY_SLOTS[i], buildRecordHead(rec, i + 1, false));
            }
        }

        // ── Hall of Fame (bottom row) ─────────────────────────────────────
        placeHallOfFame(inv, history);

        // ── Navigation ────────────────────────────────────────────────────
        id.nationcore.models.Government gov = plugin.getDataManager().getGovernment();
        boolean isGov = (gov.hasPresident() && gov.getPresidentUUID().equals(player.getUniqueId()))
                || (gov.getCabinetMemberByUUID(player.getUniqueId()) != null)
                || player.hasPermission("nation.admin");

        inv.setItem(45, createItem(Material.ARROW,
                "§7§l← Back",
                "§7Return to Main Menu"));

        fillGlass(inv);
        player.openInventory(inv);
    }

    /**
     * Opens the detail record view for the history entry at {@code index}.
     *
     * @param player The player who clicked.
     * @param index  0-based index into {@code getAllPresidentHistory()}.
     */
    public void openDetailMenu(Player player, int index) {
        List<PresidentHistory.PresidentRecord> history = plugin.getDataManager().getAllPresidentHistory();

        if (index < 0 || index >= history.size()) {
            openHistoryMenu(player);
            return;
        }

        viewingRecord.put(player.getUniqueId(), index);

        PresidentHistory.PresidentRecord rec = history.get(index);
        Inventory inv = Bukkit.createInventory(null, INVENTORY_SIZE, DETAIL_TITLE);

        var offlinePlayer = Bukkit.getOfflinePlayer(rec.getPlayerId());
        int rank = index + 1;

        // ── President Head (center) ──────────────────────────────────────
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        headMeta.setOwningPlayer(offlinePlayer);

        String rankIcon = getRankIcon(rank);
        headMeta.setDisplayName("§d§l" + rankIcon + " " + rec.getName() + "  §8(#" + rank + ")");
        headMeta.setLore(buildDetailHeadLore(rec));
        head.setItemMeta(headMeta);
        inv.setItem(4, head);

        // ── Term Info ───────────────────────────────────────────────────
        inv.setItem(10, buildTermItem(rec));

        // ── Approval Rating ─────────────────────────────────────────────
        inv.setItem(12, buildApprovalItem(rec));

        // ── Executive Orders ────────────────────────────────────────────
        inv.setItem(14, buildOrdersItem(rec));

        // ── Arena Games ─────────────────────────────────────────────────
        inv.setItem(16, buildGamesItem(rec));

        // ── Cabinet Members ─────────────────────────────────────────────
        inv.setItem(20, buildCabinetItem(rec));

        // ── Accomplishments ─────────────────────────────────────────────
        inv.setItem(22, buildAccomplishmentsItem(rec));

        // ── End Reason ──────────────────────────────────────────────────
        inv.setItem(24, buildEndReasonItem(rec));

        // ── Prev / Next navigation ───────────────────────────────────────
        if (index > 0) {
            PresidentHistory.PresidentRecord prev = history.get(index - 1);
            inv.setItem(36, buildNavHead(prev, index - 1, "§e« Previous"));
        }
        if (index < history.size() - 1) {
            PresidentHistory.PresidentRecord next = history.get(index + 1);
            inv.setItem(44, buildNavHead(next, index + 1, "§eNext »"));
        }

        inv.setItem(45, createItem(Material.ARROW,
                "§7§l← Back to List",
                "§7Return to history list"));

        fillGlass(inv);
        player.openInventory(inv);
    }

    /**
     * Returns the index stored by {@link #openDetailMenu} for the given viewer.
     *
     * @return the recorded index, or {@code -1} if none.
     */
    public int getViewingRecord(UUID viewerUUID) {
        return viewingRecord.getOrDefault(viewerUUID, -1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hall of Fame helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void placeHallOfFame(Inventory inv,
            List<PresidentHistory.PresidentRecord> history) {
        if (history.isEmpty())
            return;

        PresidentHistory presidentHistory = new PresidentHistory();
        history.forEach(presidentHistory::addRecord);

        // Highest Approval
        PresidentHistory.PresidentRecord topApproval = presidentHistory.getHighestApproval();
        if (topApproval != null) {
            inv.setItem(47, buildHallEntry(topApproval,
                    "§e§l⭐ Best Approval Rating",
                    "§eRating: §f" + String.format("%.2f", topApproval.getApproval()) + " / 5.0",
                    Material.NETHER_STAR));
        }

        // Longest Term
        PresidentHistory.PresidentRecord longestTerm = presidentHistory.getLongestTerm();
        if (longestTerm != null) {
            inv.setItem(49, buildHallEntry(longestTerm,
                    "§a§l⏳ Longest Term",
                    "§aDuration: §f" + longestTerm.getTermDurationDays() + " days",
                    Material.CLOCK));
        }

        // Most Orders
        PresidentHistory.PresidentRecord mostOrders = presidentHistory.getMostOrdersIssued();
        if (mostOrders != null) {
            inv.setItem(51, buildHallEntry(mostOrders,
                    "§c§l📜 Most Executive Orders",
                    "§cOrders: §f" + mostOrders.getOrders(),
                    Material.WRITABLE_BOOK));
        }
    }

    private ItemStack buildHallEntry(PresidentHistory.PresidentRecord rec,
            String title,
            String stat,
            Material fallback) {
        var offlinePlayer = Bukkit.getOfflinePlayer(rec.getPlayerId());
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(offlinePlayer);
        meta.setDisplayName(title);
        meta.setLore(Arrays.asList(
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7President: §f" + rec.getName(),
                stat,
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Click to view full record"));
        item.setItemMeta(meta);
        return item;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Record item builders
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a player-head item for the history list or detail nav.
     *
     * @param showDetailHint Whether to append "Click to view full record" hint.
     */
    private ItemStack buildRecordHead(PresidentHistory.PresidentRecord rec,
            int rank,
            boolean showDetailHint) {
        var offlinePlayer = Bukkit.getOfflinePlayer(rec.getPlayerId());

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(offlinePlayer);

        String rankIcon = getRankIcon(rank);
        String color = getRankColor(rank);
        meta.setDisplayName(color + "§l#" + rank + " " + rec.getName());

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        // Term duration
        int days = rec.getTermDurationDays();
        lore.add("§7⏳ Term Duration: §f" + days + (days == 1 ? " day" : " days"));

        // Approval rating with star bar
        double approval = rec.getApproval();
        lore.add("§7⭐ Approval: " + buildStarBar(approval) + " §e(" + String.format("%.1f", approval) + "/5.0)");

        // Orders
        lore.add("§7📜 Orders Issued: §f" + rec.getOrders());

        // Games
        if (rec.getGames() > 0) {
            lore.add("§7⚔ Arena Games: §f" + rec.getGames());
        }

        // End reason badge
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7End: " + formatEndReason(rec.getReason()));

        if (!rec.getAccomplishments().isEmpty()) {
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            lore.add("§7Accomplishments: §f" + rec.getAccomplishments().size());
        }

        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§aClick to view full record");

        meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private List<String> buildDetailHeadLore(PresidentHistory.PresidentRecord rec) {
        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        // Dates
        String start = rec.getTermStart() > 0
                ? DATE_FMT.format(new Date(rec.getTermStart()))
                : "Unknown";
        String end = rec.getTermEnd() > 0
                ? DATE_FMT.format(new Date(rec.getTermEnd()))
                : "In office";
        lore.add("§7Served: §f" + start + " → " + end);

        lore.add("§7⭐ Approval: §e" + String.format("%.2f", rec.getApproval()) + " / 5.0");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7End reason: " + formatEndReason(rec.getReason()));
        return lore;
    }

    private ItemStack buildTermItem(PresidentHistory.PresidentRecord rec) {
        int days = rec.getTermDurationDays();
        long ms = rec.getTermDuration();
        long h = (ms / (1000 * 60 * 60)) % 24;
        long m = (ms / (1000 * 60)) % 60;

        String start = rec.getTermStart() > 0
                ? DATE_FMT.format(new Date(rec.getTermStart()))
                : "Unknown";
        String end = rec.getTermEnd() > 0
                ? DATE_FMT.format(new Date(rec.getTermEnd()))
                : "Ongoing";

        return createItem(Material.CLOCK,
                "§a§l⏳ Term Information",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Start Date:  §f" + start,
                "§7End Date:    §f" + end,
                "§7Duration:    §f" + days + "d " + h + "h " + m + "m",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    private ItemStack buildApprovalItem(PresidentHistory.PresidentRecord rec) {
        double a = rec.getApproval();
        String color = a >= 4.0 ? "§a" : (a >= 2.5 ? "§e" : "§c");
        return createItem(Material.NETHER_STAR,
                "§e§l⭐ Approval Rating",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Rating: " + color + String.format("%.2f", a) + " §7/ 5.0",
                "§7Stars:  " + buildStarBar(a),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                color + getRatingLabel(a));
    }

    private ItemStack buildOrdersItem(PresidentHistory.PresidentRecord rec) {
        int orders = rec.getOrders();
        String orderColor = orders >= 5 ? "§a" : (orders >= 2 ? "§e" : "§c");
        return createItem(Material.WRITABLE_BOOK,
                "§c§l📜 Executive Orders",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Orders Issued: " + orderColor + orders,
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                orders == 0 ? "§7No orders were issued" : "§7Actively led the nation");
    }

    private ItemStack buildGamesItem(PresidentHistory.PresidentRecord rec) {
        int games = rec.getGames();
        return createItem(Material.IRON_SWORD,
                "§4§l⚔ Presidential Arena",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Games Hosted: §f" + games,
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                games == 0 ? "§7No arena games hosted"
                        : "§7Hosted §f" + games + " §7arena games");
    }

    private ItemStack buildCabinetItem(PresidentHistory.PresidentRecord rec) {
        Map<id.nationcore.models.Government.CabinetPosition, String> cabinet = rec.getCabinetMembers();
        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        if (cabinet == null || cabinet.isEmpty()) {
            lore.add("§7No cabinet members recorded");
        } else {
            for (var entry : cabinet.entrySet()) {
                String posName = entry.getKey().name()
                        .replace("_", " ")
                        .replace("MINISTER OF ", "Min. ");
                lore.add("§7" + posName + ": §f" + entry.getValue());
            }
        }
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        return createItem(Material.LECTERN, "§e§l🎖 Cabinet Members",
                lore.toArray(new String[0]));
    }

    private ItemStack buildAccomplishmentsItem(PresidentHistory.PresidentRecord rec) {
        List<String> accomplishments = rec.getAccomplishments();
        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        if (accomplishments == null || accomplishments.isEmpty()) {
            lore.add("§7No accomplishments recorded");
        } else {
            for (String acc : accomplishments) {
                lore.add("§7• §f" + acc);
            }
        }
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        return createItem(Material.GOLDEN_APPLE, "§6§l🏆 Accomplishments",
                lore.toArray(new String[0]));
    }

    private ItemStack buildEndReasonItem(PresidentHistory.PresidentRecord rec) {
        String reason = rec.getReason();
        Material mat;
        if ("RECALL".equalsIgnoreCase(reason)) {
            mat = Material.REDSTONE_TORCH;
        } else if ("INACTIVE".equalsIgnoreCase(reason) || "ADMIN_REMOVAL".equalsIgnoreCase(reason)) {
            mat = Material.BARRIER;
        } else {
            mat = Material.LIME_CONCRETE;
        }
        return createItem(mat,
                "§c§l🚩 End of Term",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Reason: " + formatEndReason(reason),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    /** Builds a compact player-head nav button for Prev / Next in detail view. */
    private ItemStack buildNavHead(PresidentHistory.PresidentRecord rec,
            int targetIndex,
            String label) {
        var offlinePlayer = Bukkit.getOfflinePlayer(rec.getPlayerId());
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(offlinePlayer);
        meta.setDisplayName(label + ": §f" + rec.getName());
        meta.setLore(Arrays.asList(
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Approval: §e" + String.format("%.1f", rec.getApproval()) + "/5.0",
                "§7End: " + formatEndReason(rec.getReason()),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§aClick to view"));
        head.setItemMeta(meta);
        return head;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Display helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a coloured rank medal icon for positions 1–3, empty string otherwise.
     */
    private String getRankIcon(int rank) {
        return switch (rank) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> "#" + rank;
        };
    }

    /** Returns a colour code based on rank position. */
    private String getRankColor(int rank) {
        return switch (rank) {
            case 1 -> "§6";
            case 2 -> "§7";
            case 3 -> "§c";
            default -> "§f";
        };
    }

    /**
     * Builds a visual star bar string for values 0–5.
     * e.g. 3.5 → "§e★★★§7★★"
     */
    private String buildStarBar(double value) {
        int full = (int) Math.round(value);
        int empty = 5 - full;
        return "§e" + "★".repeat(Math.max(0, full)) + "§7" + "★".repeat(Math.max(0, empty));
    }

    /** Translates raw end-reason codes to human-readable coloured text. */
    private String formatEndReason(String reason) {
        if (reason == null)
            return "§7Unknown";
        return switch (reason.toUpperCase()) {
            case "TERM_END" -> "§aCompleted Full Term";
            case "RECALL" -> "§cRecalled by Citizens";
            case "INACTIVE" -> "§eRemoved – Inactivity";
            case "ADMIN_REMOVAL" -> "§4Admin Removal";
            default -> "§7" + reason;
        };
    }

    /** Returns a performance label based on approval rating. */
    private String getRatingLabel(double approval) {
        if (approval >= 4.5)
            return "Outstanding Leader";
        if (approval >= 3.5)
            return "Excellent Performance";
        if (approval >= 2.5)
            return "Satisfactory";
        if (approval >= 1.5)
            return "Below Average";
        return "Poor Performance";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GUI utilities
    // ─────────────────────────────────────────────────────────────────────────

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) {
            meta.setLore(Arrays.asList(lore));
        }
        item.setItemMeta(meta);
        return item;
    }

    private void fillGlass(Inventory inv) {
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, glass);
            }
        }
    }
}
