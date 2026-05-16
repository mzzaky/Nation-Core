package id.nationcore.gui.republic;

import id.nationcore.gui.GovernmentGUIUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import id.nationcore.NationCore;
import id.nationcore.models.CabinetDecision;
import id.nationcore.models.Election;
import id.nationcore.models.Government;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

public class RepublicGovernmentGUI {

    private final NationCore plugin;
    public static final String TITLE = "§6§lGOVERNMENT";

    // ── Filler slots ────────────────────────────────────────────────────────
    private static final int[] FILLER_SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8,
            9, 17, 18, 26, 27, 35, 36, 44,
            45, 46, 47, 48, 49, 50, 51, 52, 53
    };

    private static final Material PRIMARY_FILLER = Material.LIGHT_BLUE_STAINED_GLASS_PANE;

    public RepublicGovernmentGUI(NationCore plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    private ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§r");
            item.setItemMeta(meta);
        }
        return item;
    }

    @SuppressWarnings("deprecation")
    public void open(Player player, Nation nation) {
        Government gov = nation != null ? nation.getRepublicGovernment() : plugin.getDataManager().getGovernment();
        
        UUID leaderUUID = null;
        int term = 0;
        long termEndTime = 0;
        long lastBroadcastTime = 0;
        boolean isAuthorized = false;

        if (gov != null) {
            leaderUUID = gov.getPresidentUUID();
            term = gov.getCurrentTerm();
            termEndTime = gov.getTermEndTime();
            lastBroadcastTime = gov.getLastBroadcastTime();
            boolean isPresident = gov.hasPresident() && gov.getPresidentUUID().equals(player.getUniqueId());
            boolean isMinister = gov.getCabinetMemberByUUID(player.getUniqueId()) != null;
            if (isPresident || isMinister) isAuthorized = true;
        }

        if (!isAuthorized) {
            MessageUtils.send(player, "§cOnly the President and Ministers can open the Government GUI.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // ── Place fillers ────────────────────────────────────────────────
        ItemStack filler = pane(PRIMARY_FILLER);
        for (int s : FILLER_SLOTS) {
            inv.setItem(s, filler);
        }

        // ── Row 0 ────────────────────────────────────────
        var activeOrders = nation != null
                ? plugin.getExecutiveOrderManager().getActiveOrders(nation)
                : plugin.getExecutiveOrderManager().getActiveOrders();
        List<CabinetDecision> activeDecisions = nation != null
                ? nation.getActiveDecisions()
                : plugin.getDataManager().getActiveDecisions();
        int totalActive = activeOrders.size() + activeDecisions.size();

        inv.setItem(4, GovernmentGUIUtils.createItem(
                totalActive > 0 ? Material.BEACON : Material.GLASS,
                "§a§lActive Effects: §f" + totalActive,
                "§7Orders: §f" + activeOrders.size(),
                "§7Decisions: §f" + activeDecisions.size()));

        // ── Row 1 ────────────────────────────────────────
        inv.setItem(10, GovernmentGUIUtils.createItem(Material.WRITABLE_BOOK, "§c§lExecutive Orders",
                "§7View and manage",
                "§7executive orders",
                "",
                "§aClick to view"));

        inv.setItem(11, GovernmentGUIUtils.createItem(Material.LECTERN, "§e§lCabinet",
                "§7View cabinet members",
                "§7and active decisions",
                "",
                "§aClick to view"));

        if (leaderUUID != null) {
            inv.setItem(13, createLeaderHead(leaderUUID, term, termEndTime, nation, gov));
        } else {
            inv.setItem(13, GovernmentGUIUtils.createItem(Material.BARRIER, "§c§lNo President",
                    "§7Election in progress",
                    "",
                    "§eClick for election info"));
        }

        Election election = nation != null ? nation.getElection() : plugin.getDataManager().getElection();
        if (election != null) {
            inv.setItem(15, GovernmentGUIUtils.createItem(Material.PAPER, "§b§lElection Info",
                    "§7Phase: §f" + election.getPhase().getDisplayName(),
                    "§7Candidates: §f" + election.getCandidates().size(),
                    "",
                    "§aClick for voting GUI"));
        }

        inv.setItem(16, GovernmentGUIUtils.createItem(Material.GOLD_BLOCK, "§6§lTreasury",
                "§7State finances",
                "",
                "§aClick to view"));

        // ── Row 3 ────────────────────────────────────────
        inv.setItem(28, GovernmentGUIUtils.createItem(Material.EMERALD, "§a§lSalary Claim",
                "§7President & Cabinet",
                "§7Daily Salary Claim",
                "",
                "§eClick to open"));

        inv.setItem(30, GovernmentGUIUtils.createItem(Material.BOOK, "§5§lPresident History",
                "§7View previous",
                "§7presidents",
                "",
                "§aClick to view"));

        inv.setItem(32, GovernmentGUIUtils.createItem(Material.NETHER_STAR, "§c§lPresidential Games",
                "§7Start a new arena game!",
                "",
                "§cOnly President can start",
                "§aClick to start"));

        long cooldown = 8L * 60 * 60 * 1000;
        long nextAvailable = lastBroadcastTime + cooldown;
        List<String> broadcastLore = new ArrayList<>();
        broadcastLore.add("§7Send a global broadcast");
        broadcastLore.add("§7message to all players!");
        broadcastLore.add("");
        if (System.currentTimeMillis() >= nextAvailable) {
            broadcastLore.add("§cOnly President can use");
            broadcastLore.add("§aClick to type message");
        } else {
            long remaining = nextAvailable - System.currentTimeMillis();
            broadcastLore.add("§cCooldown: " + MessageUtils.formatTime(remaining));
        }
        inv.setItem(34, GovernmentGUIUtils.createItem(Material.BELL, "§b§lBroadcast Message",
                broadcastLore.toArray(new String[0])));

        // ── Row 4 ────────────────────────────────────────
        inv.setItem(39, GovernmentGUIUtils.createItem(Material.COMPASS, "§b§lMain Menu", "§7Return to Main Menu"));
        inv.setItem(41, GovernmentGUIUtils.createItem(Material.BARRIER, "§c§lClose", "§7Click to close"));

        player.openInventory(inv);
    }

    @SuppressWarnings("deprecation")
    private ItemStack createLeaderHead(UUID uuid, int term, long termEndTime, Nation nation, Government gov) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        meta.setOwningPlayer(offlinePlayer);
        meta.setDisplayName("§6§lPRESIDENT: " + (offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown"));

        List<String> lore = new ArrayList<>();
        if (nation != null) {
            lore.add("§7Nation: §6" + nation.getName());
        }
        lore.add("§7Term #" + term);
        lore.add("§7Time left in term:");
        long remaining = termEndTime - System.currentTimeMillis();
        lore.add("§f" + MessageUtils.formatTime(Math.max(0, remaining)));
        
        if (gov != null) {
            lore.add("");
            lore.add("§7Approval Rating:");
            lore.add("§e" + String.format("%.1f", gov.getApprovalRating()) + "/5.0");
        }

        meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
    }
}
