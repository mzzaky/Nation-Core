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
        boolean isAuthorized = false;

        if (gov != null) {
            leaderUUID = gov.getPresidentUUID();
            term = gov.getCurrentTerm();
            termEndTime = gov.getTermEndTime();
            boolean isPresident = gov.hasPresident() && gov.getPresidentUUID().equals(player.getUniqueId());
            boolean isMinister = gov.getCabinetMemberByUUID(player.getUniqueId()) != null;
            if (isPresident || isMinister) isAuthorized = true;
        }

        if (!isAuthorized) {
            MessageUtils.send(player, "§cOnly the President and Ministers can open the Government GUI.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // 1. FILLER
        int[] lightBlueSlots = { 0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 51, 52, 53 };
        ItemStack lightBlueGlass = GovernmentGUIUtils.createItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ");
        for (int s : lightBlueSlots) {
            inv.setItem(s, lightBlueGlass);
        }

        // Active Effects (Slot 4)
        var activeOrders = nation != null
                ? plugin.getExecutiveOrderManager().getActiveOrders(nation)
                : plugin.getExecutiveOrderManager().getActiveOrders();
        List<CabinetDecision> activeDecisions = nation != null
                ? nation.getActiveDecisions()
                : plugin.getDataManager().getActiveDecisions();
        int totalActive = activeOrders.size() + activeDecisions.size();

        List<String> combinedLore = new ArrayList<>();
        combinedLore.add("§7Orders: §f" + activeOrders.size());
        combinedLore.add("§7Decisions: §f" + activeDecisions.size());
        combinedLore.add("");
        combinedLore.add("§6§lPresidential Status:");
        if (leaderUUID != null) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(leaderUUID);
            combinedLore.add("§7President: §f" + (offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown"));
        } else {
            combinedLore.add("§7President: §cNone (Election active)");
        }
        if (nation != null) {
            combinedLore.add("§7Nation: §f" + nation.getName());
        }
        combinedLore.add("§7Term: §f#" + term);
        long remainingTime = termEndTime - System.currentTimeMillis();
        combinedLore.add("§7Time left: §f" + MessageUtils.formatTime(Math.max(0, remainingTime)));
        if (gov != null) {
            combinedLore.add("§7Approval Rating: §e" + String.format("%.1f", gov.getApprovalRating()) + "/5.0");
        }

        inv.setItem(4, GovernmentGUIUtils.createItem(
                Material.GLOW_ITEM_FRAME,
                "§a§lActive Effects: §f" + totalActive,
                combinedLore.toArray(new String[0])));

        // 2. BACK (Slot 43)
        inv.setItem(43, GovernmentGUIUtils.createItem(Material.SPECTRAL_ARROW, "§7§l← Back to Menu", "§7Return to main menu"));

        // 3. Salary (Slot 37)
        inv.setItem(37, GovernmentGUIUtils.createItem(Material.EMERALD, "§a§lSalary Claim",
                "§7President & Cabinet",
                "§7Daily Salary Claim",
                "",
                "§eClick to open"));

        // 4. Rename Nation (Slot 48)
        inv.setItem(48, GovernmentGUIUtils.createItem(Material.WRITABLE_BOOK, "§e§lRename Nation",
                "§7Rename your nation via chat",
                "",
                "§eClick to rename"));

        // 5. Leave Nation (Slot 49)
        inv.setItem(49, GovernmentGUIUtils.createItem(Material.COMPASS, "§e§lLeave Nation",
                "§7Leave your current nation",
                "",
                "§cClick to leave"));

        // 6. Disband Nation (Slot 50)
        inv.setItem(50, GovernmentGUIUtils.createItem(Material.TNT_MINECART, "§e§lDisband Nation",
                "§7Disband your nation",
                "",
                "§cClick to disband"));

        // 7. Minister Management (Slot 21)
        inv.setItem(21, GovernmentGUIUtils.createItem(Material.ARMS_UP_POTTERY_SHERD, "§e§lMinister Management",
                "§7Manage your cabinet members & decisions",
                "",
                "§eClick to open"));

        // 8. Member Management (Slot 22)
        inv.setItem(22, GovernmentGUIUtils.createItem(Material.FRIEND_POTTERY_SHERD, "§e§lMember Management",
                "§7Manage your nation members & roles",
                "",
                "§eClick to open"));

        // 9. Broadcast Message (Slot 23)
        long lastBroadcast = gov != null ? gov.getLastBroadcastTime() : 0;
        long timeSinceLast = System.currentTimeMillis() - lastBroadcast;
        long cooldownDuration = 6L * 60 * 60 * 1000; // 6 hours
        boolean onCooldown = timeSinceLast < cooldownDuration;

        List<String> broadcastLore = new ArrayList<>();
        broadcastLore.add("§7Broadcast a custom message to all members.");
        broadcastLore.add("§7Cooldown: §f6 hours");
        if (onCooldown) {
            long remaining = cooldownDuration - timeSinceLast;
            broadcastLore.add("");
            broadcastLore.add("§cCooldown remaining: §f" + MessageUtils.formatTime(remaining));
        } else {
            broadcastLore.add("");
            broadcastLore.add("§eClick to broadcast");
        }

        inv.setItem(23, GovernmentGUIUtils.createItem(Material.SCRAPE_POTTERY_SHERD, "§e§lBroadcast Message",
                broadcastLore.toArray(new String[0])));

        // 11. Border Management (Slot 30)
        inv.setItem(30, GovernmentGUIUtils.createItem(Material.SHELTER_POTTERY_SHERD, "§e§lBorder Management",
                "§7Coming Soon"));

        // 12. Presidential Games (Slot 31)
        inv.setItem(31, GovernmentGUIUtils.createItem(Material.PRIZE_POTTERY_SHERD, "§c§lPresidential Games",
                "§7Start a new arena game!",
                "",
                "§cOnly President can start",
                "§aClick to start"));

        // 13. Diplomacy Management (Slot 32)
        inv.setItem(32, GovernmentGUIUtils.createItem(Material.SKULL_POTTERY_SHERD, "§e§lDiplomacy Management",
                "§7Coming Soon"));

        player.openInventory(inv);
    }
}
