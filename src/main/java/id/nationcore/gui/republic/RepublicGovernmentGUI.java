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
import org.bukkit.inventory.meta.SkullMeta;

import id.nationcore.NationCore;
import id.nationcore.models.CabinetDecision;
import id.nationcore.models.Election;
import id.nationcore.models.Government;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

public class RepublicGovernmentGUI {

    private final NationCore plugin;
    public static final String TITLE = "§9§lREPUBLIC GOVERNMENT";

    private static final int[] FILLER_SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8,
            9, 17,
            18, 26,
            27, 35,
            36, 44,
            45, 46, 47, 51, 52, 53
    };

    private static final Material PRIMARY_FILLER = Material.LIGHT_BLUE_STAINED_GLASS_PANE;

    public RepublicGovernmentGUI(NationCore plugin) {
        this.plugin = plugin;
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

        ItemStack filler = GovernmentGUIUtils.createItem(PRIMARY_FILLER, " ");
        for (int slot : FILLER_SLOTS) {
            inv.setItem(slot, filler);
        }

        // ── Header: Nation / Government profile ────────────────────────
        inv.setItem(4, buildNationProfile(nation, gov, term, termEndTime));

        // ── Row 2: Core management (Orders / President / Cabinet) ──────
        inv.setItem(20, GovernmentGUIUtils.createItem(Material.WRITABLE_BOOK, "§e§lExecutive Orders",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Issue and review active",
                "§7presidential executive orders.",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§eClick to open."));

        if (leaderUUID != null) {
            inv.setItem(22, buildPresidentHead(leaderUUID, term, termEndTime, nation, gov));
        } else {
            inv.setItem(22, GovernmentGUIUtils.createItem(Material.PLAYER_HEAD, "§c§lNo President",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7The presidential seat is vacant.",
                    "§7An election may be in progress.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        }

        inv.setItem(24, GovernmentGUIUtils.createItem(Material.LECTERN, "§e§lCabinet Management",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Inspect cabinet members and",
                "§7active ministerial decisions.",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§eClick to open."));

        // ── Row 3: Finance & politics (Election / Treasury / Salary) ───
        Election election = nation != null ? nation.getElection() : plugin.getDataManager().getElection();
        if (election != null) {
            inv.setItem(29, GovernmentGUIUtils.createItem(Material.PAPER, "§e§lElection Info",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Phase     : §f" + election.getPhase().getDisplayName(),
                    "§7Candidates: §f" + election.getCandidates().size(),
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§eClick to open the voting GUI."));
        } else {
            inv.setItem(29, GovernmentGUIUtils.createItem(Material.PAPER, "§e§lElection Info",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7No active election cycle.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        }

        inv.setItem(31, GovernmentGUIUtils.createItem(Material.GOLD_BLOCK, "§e§lState Treasury",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Manage state finances",
                "§7and review transactions.",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§eClick to open."));

        inv.setItem(33, GovernmentGUIUtils.createItem(Material.EMERALD, "§e§lSalary Claim",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Daily salary payouts for",
                "§7President and Cabinet members.",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§eClick to open."));

        // ── Row 4: Heritage & engagement (History / Games / Broadcast) ─
        inv.setItem(38, GovernmentGUIUtils.createItem(Material.BOOK, "§e§lPresident History",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Browse previous presidents",
                "§7and their term records.",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§eClick to open."));

        inv.setItem(40, GovernmentGUIUtils.createItem(Material.NETHER_STAR, "§e§lPresidential Games",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Launch a new arena event.",
                "§c⚠ Only the President can start",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§eClick to start."));

        inv.setItem(42, buildBroadcastButton(lastBroadcastTime));

        // ── Row 5: Footer actions (Main Menu / Status / Close) ─────────
        inv.setItem(48, GovernmentGUIUtils.createItem(Material.COMPASS, "§e§lMain Menu",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Return to the Republic main menu.",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        inv.setItem(49, buildActiveEffects(nation));

        inv.setItem(50, GovernmentGUIUtils.createItem(Material.BARRIER, "§c§lClose",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Close this menu.",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        player.openInventory(inv);
    }

    private ItemStack buildNationProfile(Nation nation, Government gov, int term, long termEndTime) {
        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Government : §fRepublic");
        if (nation != null) {
            lore.add("§7Tag        : §f[" + nation.getTag() + "]");
            lore.add("§7Members    : §f" + nation.getMemberCount());
        }
        if (gov != null) {
            lore.add("§7Term       : §f#" + term);
            long remaining = termEndTime - System.currentTimeMillis();
            lore.add("§7Time Left  : §f" + MessageUtils.formatTime(Math.max(0, remaining)));
            lore.add("§7Approval   : §e" + String.format("%.1f", gov.getApprovalRating()) + "/5.0");
        }
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§8Overview of the republic.");

        String name = nation != null ? nation.getName() : "Republic";
        return GovernmentGUIUtils.createItem(Material.GLOW_ITEM_FRAME,
                "§9§l" + name,
                lore.toArray(new String[0]));
    }

    @SuppressWarnings("deprecation")
    private ItemStack buildPresidentHead(UUID uuid, int term, long termEndTime, Nation nation, Government gov) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        meta.setOwningPlayer(offlinePlayer);
        meta.setDisplayName("§9§lPRESIDENT: §f" + (offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown"));

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        if (nation != null) {
            lore.add("§7Nation     : §f" + nation.getName());
        }
        lore.add("§7Term       : §f#" + term);
        long remaining = termEndTime - System.currentTimeMillis();
        lore.add("§7Time Left  : §f" + MessageUtils.formatTime(Math.max(0, remaining)));
        if (gov != null) {
            lore.add("§7Approval   : §e" + String.format("%.1f", gov.getApprovalRating()) + "/5.0");
        }
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§8Current head of state.");

        meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack buildBroadcastButton(long lastBroadcastTime) {
        long cooldown = 8L * 60 * 60 * 1000;
        long nextAvailable = lastBroadcastTime + cooldown;

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Send a global broadcast");
        lore.add("§7message to all players.");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        if (System.currentTimeMillis() >= nextAvailable) {
            lore.add("§c⚠ Only the President can use");
            lore.add("§eClick to type the message.");
        } else {
            long remaining = nextAvailable - System.currentTimeMillis();
            lore.add("§cCooldown: §f" + MessageUtils.formatTime(remaining));
        }

        return GovernmentGUIUtils.createItem(Material.BELL, "§e§lBroadcast Message",
                lore.toArray(new String[0]));
    }

    private ItemStack buildActiveEffects(Nation nation) {
        var activeOrders = nation != null
                ? plugin.getExecutiveOrderManager().getActiveOrders(nation)
                : plugin.getExecutiveOrderManager().getActiveOrders();
        List<CabinetDecision> activeDecisions = nation != null
                ? nation.getActiveDecisions()
                : plugin.getDataManager().getActiveDecisions();
        int totalActive = activeOrders.size() + activeDecisions.size();

        Material icon = totalActive > 0 ? Material.BEACON : Material.GLASS;
        return GovernmentGUIUtils.createItem(icon,
                "§a§lActive Effects: §f" + totalActive,
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Orders    : §f" + activeOrders.size(),
                "§7Decisions : §f" + activeDecisions.size(),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
}
