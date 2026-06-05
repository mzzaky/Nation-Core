package id.nationcore.gui.republic;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import id.nationcore.NationCore;
import id.nationcore.models.ArenaSession;
import id.nationcore.models.Government;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

/**
 * RepublicArenaGUI — Full management interface for the Presidential Arena Games.
 *
 * <p>
 * Features:
 * <ul>
 * <li>Live session status (active/inactive) with time remaining</li>
 * <li>Real-time leaderboard (top 10 players)</li>
 * <li>Player's own arena statistics</li>
 * <li>Economy information (kill reward, death penalty, killstreak bonuses)</li>
 * <li>Daily & grand prize rewards panel</li>
 * <li>President-only controls: start arena, end arena</li>
 * <li>Quick-action buttons: join, leave, view kit</li>
 * </ul>
 */
public class RepublicArenaGUI {

    // ─────────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────────

    public static final String ARENA_MENU_TITLE = "§4§l⚔ PRESIDENTIAL ARENA ⚔";
    public static final String ARENA_LEADERBOARD_TITLE = "§c§l🏆 ARENA LEADERBOARD 🏆";
    public static final String ARENA_KIT_TITLE = "§6§l🗡 ARENA KIT INFO 🗡";

    private static final int INVENTORY_SIZE = 54;
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd MMM yyyy HH:mm");

    // Daily reward structure (matches ArenaManager)
    private static final int[] DAILY_REWARDS = { 100000, 75000, 60000, 50000, 40000, 35000, 30000, 27500, 25000,
            25000 };
    private static final int[] NETHERITE_REWARDS = { 3, 2, 2, 1, 1, 1, 1, 1, 1, 1 };
    private static final String[] RANK_MEDALS = { "🥇", "🥈", "🥉", "#4", "#5", "#6", "#7", "#8", "#9", "#10" };

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────

    private final NationCore plugin;

    // ─────────────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────

    public RepublicArenaGUI(NationCore plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Opens the main Arena management GUI for {@code player}.
     */
    public void openArenaMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, INVENTORY_SIZE, ARENA_MENU_TITLE);

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        boolean isActive = plugin.getArenaManager().isArenaActive(nation);
        boolean isInArena = plugin.getArenaManager().isInArena(player.getUniqueId());
        boolean isPresident = isPresidentOf(player, nation);

        ArenaSession session = nation != null ? nation.getArenaSession() : null;

        // ── Row 0: Header ─────────────────────────────────────────────────
        inv.setItem(4, buildSessionStatusItem(isActive, session));

        // ── Row 1: Main info panels ────────────────────────────────────────
        inv.setItem(10, buildSessionInfoItem(isActive, session, nation));
        inv.setItem(12, buildEconomyInfoItem());
        inv.setItem(14, buildKillstreakInfoItem());
        inv.setItem(16, buildRewardsInfoItem());

        // ── Row 2: Player stats + leaderboard + kit ────────────────────────
        inv.setItem(19, buildMyStatsItem(player, session, nation));
        inv.setItem(22, buildLeaderboardShortItem(session, nation));
        inv.setItem(25, buildKitInfoItem());

        // ── Row 3: Action buttons ──────────────────────────────────────────
        buildActionButtons(inv, player, isActive, isInArena, isPresident, session, nation);

        // ── Row 4: Navigation ─────────────────────────────────────────────
        inv.setItem(45, createItem(Material.ARROW, "§7§l← Back to Menu", "§7Return to main menu"));
        inv.setItem(49, buildRefreshItem());
        inv.setItem(53, createItem(Material.BARRIER, "§c§lClose", "§7Click to close"));

        fillGlass(inv);
        player.openInventory(inv);
    }

    /**
     * Opens the full live leaderboard GUI for {@code player}.
     */
    public void openLeaderboard(Player player) {
        Inventory inv = Bukkit.createInventory(null, INVENTORY_SIZE, ARENA_LEADERBOARD_TITLE);
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        ArenaSession session = nation != null ? nation.getArenaSession() : null;

        // Header
        boolean isActive = session != null && !session.isExpired();
        String statusLine = isActive ? "§a§lACTIVE SESSION" : "§7No Active Session";
        int totalPlayers = isActive ? session.getPlayerStats().size() : 0;

        inv.setItem(4, createItem(Material.GOLD_BLOCK,
                "§6§l🏆 Arena Leaderboard",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Status: " + statusLine,
                "§7Total Participants: §f" + totalPlayers,
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Rankings are sorted by total kills"));

        // Leaderboard entries
        int[] lbSlots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21 };

        if (!isActive || session.getPlayerStats().isEmpty()) {
            inv.setItem(22, createItem(Material.GRAY_CONCRETE,
                    "§7§lNo Data Available",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7The arena is currently inactive",
                    "§7or no one has participated yet.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        } else {
            List<Map.Entry<UUID, ArenaSession.ArenaStats>> top = plugin.getArenaManager().getLeaderboard(nation, 10);

            for (int i = 0; i < top.size() && i < lbSlots.length; i++) {
                Map.Entry<UUID, ArenaSession.ArenaStats> entry = top.get(i);
                ArenaSession.ArenaStats stats = entry.getValue();

                int dailyReward = i < DAILY_REWARDS.length ? DAILY_REWARDS[i] : 0;
                int netheriteReward = i < NETHERITE_REWARDS.length ? NETHERITE_REWARDS[i] : 0;
                String medal = RANK_MEDALS[i];

                ItemStack head = buildLeaderboardHead(
                        entry.getKey(),
                        stats,
                        i + 1,
                        medal,
                        dailyReward,
                        netheriteReward);
                inv.setItem(lbSlots[i], head);
            }
        }

        // Daily reward breakdown (bottom row)
        inv.setItem(46, buildDailyRewardBreakdownItem());
        inv.setItem(48, buildGrandPrizeItem());

        // Back / Close
        inv.setItem(45, createItem(Material.ARROW, "§7§l← Back", "§7Return to Arena Menu"));
        inv.setItem(53, createItem(Material.BARRIER, "§c§lClose", "§7Click to close"));

        fillGlass(inv);
        player.openInventory(inv);
    }

    /**
     * Opens the Arena Kit information GUI.
     */
    public void openKitInfo(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ARENA_KIT_TITLE);

        // Kit items displayed as info
        inv.setItem(4, createItem(Material.IRON_HELMET, "§7Iron Helmet", "§7Standard arena armor"));
        inv.setItem(10, createItem(Material.IRON_CHESTPLATE, "§7Iron Chestplate", "§7Standard arena armor"));
        inv.setItem(11, createItem(Material.IRON_LEGGINGS, "§7Iron Leggings", "§7Standard arena armor"));
        inv.setItem(12, createItem(Material.IRON_BOOTS, "§7Iron Boots", "§7Standard arena armor"));
        inv.setItem(13, createItem(Material.IRON_SWORD,
                "§c§lArena Sword",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Your primary melee weapon"));
        inv.setItem(14, createItem(Material.BOW,
                "§e§lBow",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Ranged weapon"));
        inv.setItem(15, createItem(Material.ARROW, "§7Arrow x32", "§7Ammunition for your bow"));
        inv.setItem(16, createItem(Material.GOLDEN_APPLE,
                "§6Golden Apple x3",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Emergency healing item"));

        inv.setItem(20, createItem(Material.COOKED_BEEF,
                "§e§lCooked Beef x16",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Keep your hunger level full!"));

        // Killstreak upgrades info
        inv.setItem(22, createItem(Material.NETHERITE_SWORD,
                "§4§l🔥 Killstreak Upgrades",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§75 Kills  §f— §63x Golden Apple §7+ §aSpeed buff",
                "§710 Kills §f— §bDiamond Sword §7+ §cStrength buff",
                "§725 Kills §f— §4Killstreak Axe §7+ §cStrength II",
                "§750 Kills §f— §4Full Netherite §7+ §cStr II + Res II",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Killstreaks reset on death!"));

        // Back button
        inv.setItem(18, createItem(Material.ARROW, "§7§l← Back", "§7Return to Arena Menu"));
        inv.setItem(26, createItem(Material.BARRIER, "§c§lClose", "§7Click to close"));

        fillGlass(inv);
        player.openInventory(inv);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Item Builders — Session Info
    // ─────────────────────────────────────────────────────────────────────────

    private ItemStack buildSessionStatusItem(boolean isActive, ArenaSession session) {
        if (!isActive) {
            return createItem(Material.GRAY_CONCRETE,
                    "§8§l⚔ ARENA STATUS: §c§lINACTIVE",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7No presidential arena games",
                    "§7are currently running.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7The President can start a new",
                    "§7session from this menu.");
        }

        long remaining = session.getRemainingTime();
        String timeLeft = formatDuration(remaining);
        int day = session.getCurrentDay();
        String startedBy = Bukkit.getOfflinePlayer(session.getActivatedBy()).getName();

        return createItem(Material.LIME_CONCRETE,
                "§a§l⚔ ARENA STATUS: §2§lACTIVE",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Declared by: §f" + (startedBy != null ? startedBy : "Unknown"),
                "§7Day: §f" + day,
                "§7Time Remaining: §e" + timeLeft,
                "§7Players in Arena: §f" + plugin.getArenaManager().getArenaPlayers().size(),
                "§7Total Participants: §f" + session.getPlayerStats().size(),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§aSession is live! Join now!");
    }

    private ItemStack buildSessionInfoItem(boolean isActive, ArenaSession session, Nation nation) {
        if (!isActive) {
            int maxGames = plugin.getConfig().getInt("arena.max-games-per-term", 2);
            int gamesUsed = nation != null ? nation.getGamesThisTerm() : 0;
            int startCost = plugin.getConfig().getInt("arena.start-cost", 100000);
            int duration = (int) plugin.getConfig().getLong("arena.duration-days", 7);
            boolean canStart = plugin.getArenaManager().canStartArena(nation);

            return createItem(Material.CAMPFIRE,
                    "§e§l📋 Session Information",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Games This Term: §f" + gamesUsed + " §8/ §f" + maxGames,
                    "§7Start Cost: §6$" + MessageUtils.formatNumber(startCost),
                    "§7Arena Duration: §f" + duration + " days",
                    "§7Radius: §f" + plugin.getConfig().getInt("arena.radius", 100) + " blocks",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    canStart ? "§aReady to start!" : "§cCannot start arena",
                    !canStart && gamesUsed >= maxGames ? "§c  ✗ Game limit reached" : "",
                    "§7(President only)");
        }

        String startDate = DATE_FMT.format(new Date(session.getStartTime()));
        String endDate = DATE_FMT.format(new Date(session.getEndTime()));

        return createItem(Material.CAMPFIRE,
                "§e§l📋 Session Information",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Started: §f" + startDate,
                "§7Ends: §f" + endDate,
                "§7Day: §f" + session.getCurrentDay(),
                "§7Time Left: §e" + formatDuration(session.getRemainingTime()),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Participants: §f" + session.getPlayerStats().size());
    }

    private ItemStack buildEconomyInfoItem() {
        int killReward = plugin.getConfig().getInt("arena.kill-reward", 5000);
        int deathPenalty = plugin.getConfig().getInt("arena.death-penalty", 5000);

        return createItem(Material.GOLD_INGOT,
                "§6§l💰 Economy",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Per Kill: §a+$" + MessageUtils.formatNumber(killReward),
                "§7Per Death: §c-$" + MessageUtils.formatNumber(deathPenalty),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Rewards go directly to",
                "§7your personal vault balance.");
    }

    private ItemStack buildKillstreakInfoItem() {
        return createItem(Material.BLAZE_ROD,
                "§c§l🔥 Killstreak Bonuses",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§75  Kill Streak: §6+$10,000",
                "§710 Kill Streak: §6+$25,000",
                "§725 Kill Streak: §6+$50,000",
                "§750 Kill Streak: §6+$100,000",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Streaks also unlock exclusive",
                "§7combat gear upgrades!",
                "§c⚠ Resets on death.");
    }

    private ItemStack buildRewardsInfoItem() {
        int grandPrize = plugin.getConfig().getInt("arena.grand-prize", 1000000);

        return createItem(Material.NETHER_STAR,
                "§e§l🏆 Prize Pool",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§f§lDaily Rewards (Top 10):",
                "§7🥇 1st: §6$100,000 §7+ §f3 Netherite",
                "§7🥈 2nd: §6$75,000 §7+ §f2 Netherite",
                "§7🥉 3rd: §6$60,000 §7+ §f2 Netherite",
                "§7#4-10: §6$25k-$50k §7+ §f1 Netherite",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§f§lGrand Prize (Winner):",
                "§7👑 Champion: §6$" + MessageUtils.formatNumber(grandPrize),
                "§7     + §fArena Champion Trophy",
                "§7     + §fChampion's Chestplate",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Click to view full leaderboard.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Item Builders — Player Stats
    // ─────────────────────────────────────────────────────────────────────────

    private ItemStack buildMyStatsItem(Player player, ArenaSession session, Nation nation) {
        int kills = 0;
        int deaths = 0;
        int bestStreak = 0;
        int curStreak = plugin.getArenaManager().getKillStreak(player.getUniqueId());
        double kdr = 0;
        double vaultEarned = 0;

        if (session != null) {
            ArenaSession.ArenaStats stats = session.getPlayerStats().get(player.getUniqueId());
            if (stats != null) {
                kills = stats.getKills();
                deaths = stats.getDeaths();
                bestStreak = stats.getBestStreak();
                kdr = stats.getKDR();
                vaultEarned = stats.getVaultEarned();
            }
        }

        // Determine current rank
        String rankStr = "§7—";
        if (session != null && kills > 0) {
            List<Map.Entry<UUID, ArenaSession.ArenaStats>> lb = plugin.getArenaManager()
                    .getLeaderboard(nation, session.getPlayerStats().size());
            for (int i = 0; i < lb.size(); i++) {
                if (lb.get(i).getKey().equals(player.getUniqueId())) {
                    rankStr = "§f#" + (i + 1);
                    if (i == 0)
                        rankStr = "§6🥇 #1";
                    else if (i == 1)
                        rankStr = "§7🥈 #2";
                    else if (i == 2)
                        rankStr = "§c🥉 #3";
                    break;
                }
            }
        }

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(player.getUniqueId()));
        meta.setDisplayName("§b§l📊 My Arena Stats");
        meta.setLore(Arrays.asList(
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Current Rank: " + rankStr,
                "§7Kills: §a" + kills,
                "§7Deaths: §c" + deaths,
                "§7K/D Ratio: §f" + String.format("%.2f", kdr),
                "§7Current Streak: §e" + curStreak,
                "§7Best Streak: §6" + bestStreak,
                "§7Vault Earned: §a$" + MessageUtils.formatNumber((long) vaultEarned),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                session == null ? "§7No active session." : "§7Stats for current session only."));
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack buildLeaderboardShortItem(ArenaSession session, Nation nation) {
        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        if (session == null || session.getPlayerStats().isEmpty()) {
            lore.add("§7No participants yet.");
        } else {
            List<Map.Entry<UUID, ArenaSession.ArenaStats>> top = plugin.getArenaManager().getLeaderboard(nation, 5);
            String[] medals = { "🥇", "🥈", "🥉", "#4", "#5" };
            for (int i = 0; i < top.size(); i++) {
                ArenaSession.ArenaStats s = top.get(i).getValue();
                lore.add("§7" + medals[i] + " §f" + s.getPlayerName() + " §7— §a" + s.getKills() + " kills");
            }
        }

        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§aClick to view full leaderboard.");

        return createItemWithLore(Material.PLAYER_HEAD, "§e§l🏆 Top Players", lore);
    }

    private ItemStack buildKitInfoItem() {
        return createItem(Material.IRON_CHESTPLATE,
                "§6§l🗡 Arena Kit",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Full Iron Armor",
                "§7Arena Sword (Iron)",
                "§7Bow + 32 Arrows",
                "§7Golden Apple x3",
                "§7Cooked Beef x16",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Kit is given automatically",
                "§7when you join the arena.",
                "§aClick to view details.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Item Builders — Action Buttons
    // ─────────────────────────────────────────────────────────────────────────

    private void buildActionButtons(Inventory inv, Player player,
            boolean isActive, boolean isInArena,
            boolean isPresident, ArenaSession session, Nation nation) {

        // JOIN button (slot 37)
        if (isActive && !isInArena) {
            inv.setItem(37, createItem(Material.LIME_CONCRETE,
                    "§a§l▶ JOIN ARENA",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Click to join the active",
                    "§7arena session!",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§c⚠ Your inventory will be",
                    "§c   cleared for the kit!"));
        } else if (isActive && isInArena) {
            inv.setItem(37, createItem(Material.RED_CONCRETE,
                    "§c§l■ LEAVE ARENA",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Click to exit the arena",
                    "§7and return to your location.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Your original inventory",
                    "§7will be restored."));
        } else {
            inv.setItem(37, createItem(Material.GRAY_CONCRETE,
                    "§7§l▶ JOIN ARENA",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7No active session.",
                    "§7Wait for the President",
                    "§7to start a new game."));
        }

        // VIEW LEADERBOARD button (slot 40)
        inv.setItem(40, createItem(Material.GOLD_BLOCK,
                "§6§l🏆 Full Leaderboard",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7View all participants and",
                "§7their live kill counts.",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§aClick to open."));

        // PRESIDENT-ONLY CONTROLS (slot 43)
        if (isPresident && nation != null) {
            if (!isActive) {
                boolean canStart = plugin.getArenaManager().canStartArena(nation);
                int startCost = plugin.getConfig().getInt("arena.start-cost", 100000);
                double treasury = nation.getTreasury().getBalance();

                if (canStart) {
                    inv.setItem(43, createItem(Material.BEACON,
                            "§a§l★ START ARENA §8[PRESIDENT]",
                            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            "§7Cost: §6$" + MessageUtils.formatNumber(startCost),
                            "§7Treasury: §6$" + MessageUtils.formatNumber((long) treasury),
                            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            "§aAll conditions met!",
                            "§eClick to launch the arena."));
                } else {
                    int maxGames = plugin.getConfig().getInt("arena.max-games-per-term", 2);
                    int gamesUsed = nation.getGamesThisTerm();
                    inv.setItem(43, createItem(Material.BARRIER,
                            "§c§l✗ CANNOT START ARENA §8[PRESIDENT]",
                            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            gamesUsed >= maxGames ? "§c✗ Game limit reached (" + gamesUsed + "/" + maxGames + ")" : "",
                            treasury < startCost ? "§c✗ Insufficient treasury funds" : "",
                            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            "§7Fix the above issues first."));
                }
            } else {
                // Active — show end arena button
                inv.setItem(43, createItem(Material.TNT,
                        "§4§l💥 END ARENA §8[PRESIDENT]",
                        "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                        "§7Ends the arena immediately.",
                        "§7Final rewards will be",
                        "§7distributed to the winner.",
                        "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                        "§c⚠ This action is irreversible!"));
            }
        }
    }

    private ItemStack buildRefreshItem() {
        return createItem(Material.CLOCK,
                "§7§l↻ Refresh",
                "§7Click to refresh this GUI",
                "§7with the latest data.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Item Builders — Leaderboard
    // ─────────────────────────────────────────────────────────────────────────

    private ItemStack buildLeaderboardHead(UUID uuid, ArenaSession.ArenaStats stats,
            int rank, String medal,
            int dailyReward, int netheriteReward) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));

        String color = rank == 1 ? "§6" : rank == 2 ? "§7" : rank == 3 ? "§c" : "§f";
        meta.setDisplayName(color + "§l" + medal + " " + stats.getPlayerName());

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Kills: §a" + stats.getKills());
        lore.add("§7Deaths: §c" + stats.getDeaths());
        lore.add("§7K/D Ratio: §f" + String.format("%.2f", stats.getKDR()));
        lore.add("§7Best Streak: §6" + stats.getBestStreak());
        lore.add("§7Vault Earned: §a$" + MessageUtils.formatNumber((long) stats.getVaultEarned()));
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Daily Reward: §6$" + MessageUtils.formatNumber(dailyReward));
        if (netheriteReward > 0) {
            lore.add("§7           + §f" + netheriteReward + " Netherite Ingot");
        }
        if (rank == 1) {
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            lore.add("§6§lGrand Prize Eligible!");
            lore.add("§7Win: §6$1,000,000 §7+ Trophy");
        }

        meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack buildDailyRewardBreakdownItem() {
        return createItem(Material.CLOCK,
                "§a§l⏰ Daily Reward Schedule",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Distributed every 24h",
                "§7to the top 10 players.",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7🥇 §6$100,000 §7+ §f3 Netherite",
                "§7🥈 §6$75,000  §7+ §f2 Netherite",
                "§7🥉 §6$60,000  §7+ §f2 Netherite",
                "§7#4 §6$50,000  §7+ §f1 Netherite",
                "§7#5 §6$40,000  §7+ §f1 Netherite",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Offline players receive money");
    }

    private ItemStack buildGrandPrizeItem() {
        int grandPrize = plugin.getConfig().getInt("arena.grand-prize", 1000000);
        return createItem(Material.NETHER_STAR,
                "§6§l👑 Grand Prize",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Awarded at the end of",
                "§7the arena session to #1.",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7💰 §6$" + MessageUtils.formatNumber(grandPrize),
                "§7🏆 §fArena Champion Trophy",
                "§7⚔ §fChampion's Netherite Chestplate",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Most total kills wins!");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isPresidentOf(Player player, Nation nation) {
        if (nation == null) return false;
        Government gov = nation.getRepublicGovernment();
        return gov != null && gov.hasPresident()
                && gov.getPresidentUUID().equals(player.getUniqueId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────────────────

    private String formatDuration(long millis) {
        if (millis <= 0)
            return "§cExpired";
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;

        if (days > 0)
            return days + "d " + hours + "h " + minutes + "m";
        if (hours > 0)
            return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) {
            List<String> filtered = new ArrayList<>();
            for (String line : lore)
                if (line != null && !line.isEmpty())
                    filtered.add(line);
            meta.setLore(filtered);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItemWithLore(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void fillGlass(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, glass);
            }
        }
    }
}
