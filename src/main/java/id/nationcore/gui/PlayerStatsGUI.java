package id.nationcore.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import id.nationcore.NationCore;
import id.nationcore.models.CabinetDecision;
import id.nationcore.models.Government;
import id.nationcore.models.PlayerData;
import id.nationcore.utils.MessageUtils;

/**
 * Player Stats GUI - Displays full player statistics
 */
public class PlayerStatsGUI {

    private final NationCore plugin;

    public static final String STATS_GUI_TITLE = "§b§l📊 PLAYER STATISTICS 📊";
    public static final String LEADERBOARD_TITLE = "§6§l🏆 LEADERBOARD 🏆";

    public PlayerStatsGUI(NationCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Open player statistics menu
     */
    public void openPlayerStats(Player viewer, UUID targetUUID) {
        PlayerData data = plugin.getDataManager().getPlayerData(targetUUID);
        var offlinePlayer = Bukkit.getOfflinePlayer(targetUUID);
        String targetName = offlinePlayer.getName();

        Inventory inv = Bukkit.createInventory(null, 45, STATS_GUI_TITLE);

        // Player Head (Center top)
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        headMeta.setOwningPlayer(offlinePlayer);
        headMeta.setDisplayName("§b§l" + targetName);

        List<String> headLore = new ArrayList<>();
        headLore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        headLore.add("§7Level: §f" + calculateLevel(data));
        headLore.add("§7Total Playtime: §f" + MessageUtils.formatTime(data.getTotalPlaytime()));
        headLore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        // Status role
        Government gov = plugin.getDataManager().getGovernment();
        String role = "§7Citizen";
        if (gov.hasPresident() && gov.getPresidentUUID().equals(targetUUID)) {
            role = "§6§l👑 PRESIDENT";
        } else {
            for (CabinetDecision.CabinetPosition pos : CabinetDecision.CabinetPosition.values()) {
                UUID minister = gov.getCabinetMember(pos.toGovernmentPosition());
                if (minister != null && minister.equals(targetUUID)) {
                    role = "§e§l🎖 " + pos.getDisplayName();
                    break;
                }
            }
        }
        headLore.add("§7Status: " + role);

        headMeta.setLore(headLore);
        head.setItemMeta(headMeta);
        inv.setItem(4, head);

        // === ROW 2: Political Statistics ===

        // Votes Cast
        ItemStack votesItem = createItem(Material.PAPER, "§a§l🗳 Votes Cast",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Total votes: §f" + data.getTotalVotesCast(),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Active participation in",
                "§7presidential elections");
        inv.setItem(10, votesItem);

        // Endorsements
        ItemStack endorseItem = createItem(Material.GOLDEN_APPLE, "§6§l⭐ Endorsements",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Given: §f" + data.getEndorsementsGiven(),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Support for candidates",
                "§7in elections");
        inv.setItem(12, endorseItem);

        // Times President
        ItemStack presItem = createItem(Material.GOLDEN_HELMET, "§6§l👑 Presidential Term",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Times served: §f" + data.getTimesServedAsPresident() + "x",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Experience as",
                "§7state leader");
        inv.setItem(14, presItem);

        // Times Cabinet
        ItemStack cabItem = createItem(Material.IRON_HELMET, "§e§l🎖 Ministerial Term",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Times served: §f" + data.getTimesServedAsCabinet() + "x",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Experience as",
                "§7cabinet member");
        inv.setItem(16, cabItem);

        // === ROW 3: Arena Statistics ===

        // Arena Kills
        ItemStack killsItem = createItem(Material.DIAMOND_SWORD, "§c§l⚔ Arena Kills",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Total kills: §a" + data.getArenaKills(),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(19, killsItem);

        // Arena Deaths
        ItemStack deathsItem = createItem(Material.SKELETON_SKULL, "§4§l💀 Arena Deaths",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Total deaths: §c" + data.getArenaDeaths(),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(21, deathsItem);

        // K/D Ratio
        double kd = data.getArenaDeaths() > 0 ? (double) data.getArenaKills() / data.getArenaDeaths()
                : data.getArenaKills();
        String kdColor = kd >= 2.0 ? "§a" : (kd >= 1.0 ? "§e" : "§c");
        ItemStack kdItem = createItem(Material.EXPERIENCE_BOTTLE, "§e§l📈 K/D Ratio",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Ratio: " + kdColor + String.format("%.2f", kd),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(23, kdItem);

        // Kill Streak
        ItemStack streakItem = createItem(Material.BLAZE_ROD, "§6§l🔥 Kill Streak",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Current: §f" + data.getCurrentKillstreak(),
                "§7Best: §6" + data.getBestKillstreak(),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(25, streakItem);

        // === ROW 4: Additional Information ===

        // Level Progress
        int currentLevel = calculateLevel(data);
        double hoursPlayed = data.getTotalPlaytime() / (1000.0 * 60 * 60);
        double progress = (hoursPlayed % 10) / 10 * 100;

        ItemStack levelItem = createItem(Material.ENCHANTING_TABLE, "§d§l✦ Level Progress",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Current level: §d" + currentLevel,
                "§7Progress to next: §f" + String.format("%.1f", progress) + "%",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7(10 hours playtime = 1 level)");
        inv.setItem(30, levelItem);

        // Online Status
        boolean isOnline = offlinePlayer.isOnline();
        Material statusMat = isOnline ? Material.LIME_DYE : Material.GRAY_DYE;
        String statusText = isOnline ? "§a§lONLINE" : "§7§lOFFLINE";
        ItemStack statusItem = createItem(statusMat, statusText,
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                isOnline ? "§7Currently playing" : "§7Last login:",
                isOnline ? ""
                        : "§f" + (offlinePlayer.getLastPlayed() > 0
                                ? MessageUtils.formatTime(System.currentTimeMillis() - offlinePlayer.getLastPlayed())
                                        + " ago"
                                : "N/A"),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(32, statusItem);

        // === Footer ===

        // Back button
        ItemStack backItem = createItem(Material.ARROW, "§7§lBack", "§7Back to main menu");
        inv.setItem(36, backItem);

        // Close button
        ItemStack closeItem = createItem(Material.BARRIER, "§c§lClose", "§7Close menu");
        inv.setItem(44, closeItem);

        fillGlass(inv);
        viewer.openInventory(inv);
    }

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

    private int calculateLevel(PlayerData data) {
        double hours = data.getTotalPlaytime() / (1000.0 * 60 * 60);
        return (int) (hours / 10) + 1;
    }
}
