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
import id.nationcore.NationCore;
import id.nationcore.models.CabinetDecision;
import id.nationcore.models.Government;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

public class RepublicGovernmentGUI {

    private final NationCore plugin;
    public static final String TITLE = "§9§lREPUBLIC GOVERNMENT";

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
        boolean isPresident = false;

        if (gov != null) {
            leaderUUID = gov.getPresidentUUID();
            term = gov.getCurrentTerm();
            termEndTime = gov.getTermEndTime();
            isPresident = gov.hasPresident() && gov.getPresidentUUID().equals(player.getUniqueId());
            boolean isMinister = gov.getCabinetMemberByUUID(player.getUniqueId()) != null;
            if (isPresident || isMinister)
                isAuthorized = true;
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
            combinedLore
                    .add("§7President: §f" + (offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown"));
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

        // Minister Offices
        String healthMinisterName = "Vacant";
        String defenseMinisterName = "Vacant";
        String treasuryMinisterName = "Vacant";

        if (gov != null) {
            UUID healthUUID = gov.getCabinetMember(Government.CabinetPosition.HEALTH);
            if (healthUUID != null) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(healthUUID);
                if (op.getName() != null) healthMinisterName = op.getName();
            }
            UUID defenseUUID = gov.getCabinetMember(Government.CabinetPosition.DEFENSE);
            if (defenseUUID != null) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(defenseUUID);
                if (op.getName() != null) defenseMinisterName = op.getName();
            }
            UUID treasuryUUID = gov.getCabinetMember(Government.CabinetPosition.TREASURY);
            if (treasuryUUID != null) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(treasuryUUID);
                if (op.getName() != null) treasuryMinisterName = op.getName();
            }
        }

        inv.setItem(13, createOfficeItem(Material.GOLDEN_HELMET, "§e§lMinister of Treasury Office", treasuryMinisterName));
        inv.setItem(10, createOfficeItem(Material.TURTLE_HELMET, "§e§lMinister of Health Office", healthMinisterName));
        inv.setItem(16, createOfficeItem(Material.NETHERITE_HELMET, "§e§lMinister of Defense Office", defenseMinisterName));

        // Nation Settings Button (Slot 50)
        inv.setItem(50, GovernmentGUIUtils.createItem(
                Material.FURNACE_MINECART,
                "§e§lNation Settings",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Configure your nation settings.",
                "§7• Rename nation & TAG",
                "§7• Toggle Tax system",
                "§7• Disband nation",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§eClick to open settings",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
        ));

        // Announcement Button (Slot 49)
        List<String> announcementLore = new ArrayList<>();
        announcementLore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        announcementLore.add("§7Update the announcement message");
        announcementLore.add("§7shown on the main menu.");
        announcementLore.add("");
        announcementLore.add("§7Cooldown: §f12 hours");

        long lastAnn = nation != null ? nation.getLastAnnouncementTime() : 0;
        long timeSinceLastAnn = System.currentTimeMillis() - lastAnn;
        long cooldownDurationAnn = 12L * 60 * 60 * 1000; // 12 hours
        boolean onCooldownAnn = timeSinceLastAnn < cooldownDurationAnn;

        if (!isPresident) {
            announcementLore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            announcementLore.add("§cOnly the President can use this.");
        } else if (onCooldownAnn) {
            long remainingAnn = cooldownDurationAnn - timeSinceLastAnn;
            announcementLore.add("§7Status: §cOn Cooldown");
            announcementLore.add("§7Remaining: §f" + MessageUtils.formatTime(remainingAnn));
            announcementLore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            announcementLore.add("§cThis action is on cooldown.");
        } else {
            announcementLore.add("§7Status: §aReady");
            announcementLore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            announcementLore.add("§eClick to update message");
        }

        inv.setItem(49, GovernmentGUIUtils.createItem(Material.COMMAND_BLOCK_MINECART, "§e§lSet Announcement Message",
                announcementLore.toArray(new String[0])));

        // 2. BACK (Slot 43)
        inv.setItem(43,
                GovernmentGUIUtils.createItem(Material.SPECTRAL_ARROW, "§7§l← Back to Menu", "§7Return to main menu"));

        // 3. Salary (Slot 48)
        inv.setItem(48, GovernmentGUIUtils.createItem(Material.CHEST_MINECART, "§a§lSalary Claim",
                "§7President & Cabinet",
                "§7Daily Salary Claim",
                "",
                "§eClick to open"));

        // Guide & Help (Slot 37)
        inv.setItem(37, GovernmentGUIUtils.createItem(
                Material.BOOKSHELF,
                "§b§lGuide & Help",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Welcome to the Republic Government.",
                "§7This console is used to manage the",
                "§7administrative aspects of the nation.",
                "",
                "§3§lQuick Actions Guide:",
                "§8• §bMember Management: §7Manage roles & senators",
                "§8• §bDiplomacy Management: §7Foreign relationships",
                "§8• §bExecutive Orders: §7Enable special policies",
                "§8• §bArena Games: §7Host duels & tournaments",
                "§8• §bTax & Settings: §7Adjust economy & options",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
        ));

        // 7. Executive Order (Slot 30)
        inv.setItem(30, GovernmentGUIUtils.createItem(Material.ARMS_UP_POTTERY_SHERD, "§e§lExecutive Order",
                "§7Manage executive orders for the nation",
                "",
                "§eClick to open"));

        // 8. Member Management (Slot 22)
        inv.setItem(22, GovernmentGUIUtils.createItem(Material.FRIEND_POTTERY_SHERD, "§e§lMember Management",
                "§7Manage your nation members & roles",
                "",
                "§eClick to open"));

        // 9. Broadcast Message (Slot 32)
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

        inv.setItem(32, GovernmentGUIUtils.createItem(Material.SCRAPE_POTTERY_SHERD, "§e§lBroadcast Message",
                broadcastLore.toArray(new String[0])));

        // 11. Border Management (Slot 21)
        inv.setItem(21, GovernmentGUIUtils.createItem(Material.SHELTER_POTTERY_SHERD, "§e§lBorder Management",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Manage your nation's territory.",
                "§7• Claim & release chunks",
                "§7• Reallocate your capital",
                "§7• Toggle border visualization",
                "§7• Set a territory welcome message",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§aClick to open."));

        // 12. Presidential Games (Slot 31)
        if (nation != null) {
            boolean isActive = plugin.getArenaManager().isArenaActive(nation);
            if (isActive) {
                inv.setItem(31, GovernmentGUIUtils.createItem(Material.PRIZE_POTTERY_SHERD, "§c§lPresidential Games",
                        "§7Arena game is currently active!",
                        "",
                        "§aClick to view Arena Menu"));
            } else {
                if (isPresident) {
                    boolean canStart = plugin.getArenaManager().canStartArena(nation);
                    int startCost = plugin.getConfig().getInt("arena.start-cost", 100000);
                    double treasury = nation.getTreasury().getBalance();

                    if (canStart) {
                        inv.setItem(31, GovernmentGUIUtils.createItem(Material.PRIZE_POTTERY_SHERD,
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
                        List<String> loreList = new ArrayList<>();
                        loreList.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                        if (gamesUsed >= maxGames) {
                            loreList.add("§c✗ Game limit reached (" + gamesUsed + "/" + maxGames + ")");
                        }
                        if (treasury < startCost) {
                            loreList.add("§c✗ Insufficient treasury funds");
                        }
                        loreList.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                        loreList.add("§7Fix the above issues first.");

                        inv.setItem(31, GovernmentGUIUtils.createItem(Material.BARRIER,
                                "§c§l✗ CANNOT START ARENA §8[PRESIDENT]",
                                loreList.toArray(new String[0])));
                    }
                } else {
                    inv.setItem(31, GovernmentGUIUtils.createItem(Material.BARRIER, "§c§l✗ CANNOT START ARENA",
                            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            "§c✗ Only the President can start",
                            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
                }
            }
        } else {
            inv.setItem(31, GovernmentGUIUtils.createItem(Material.PRIZE_POTTERY_SHERD, "§c§lPresidential Games",
                    "§7Start a new arena game!",
                    "",
                    "§cOnly President can start",
                    "§aClick to start"));
        }

        // 13. Diplomacy Management (Slot 23)
        inv.setItem(23, GovernmentGUIUtils.createItem(Material.SKULL_POTTERY_SHERD, "§e§lDiplomacy Management",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Manage your foreign relations",
                "§7with every other nation.",
                "§7• Review each nation's current status",
                "§7• Propose Peace, Alliance, Truce or War",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§aClick to open."));

        player.openInventory(inv);
    }

    private ItemStack createOfficeItem(Material material, String name, String ministerName) {
        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Manage and execute specific");
        lore.add("§7cabinet decisions & policies.");
        lore.add("");
        lore.add("§7Minister: §f" + ministerName);
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§eClick to open office console");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        ItemStack item = GovernmentGUIUtils.createItem(material, name, lore.toArray(new String[0]));
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            try {
                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(this.plugin, "dummy_hide_attrs");
                org.bukkit.attribute.AttributeModifier modifier = new org.bukkit.attribute.AttributeModifier(
                    key,
                    0.0,
                    org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER,
                    org.bukkit.inventory.EquipmentSlotGroup.ANY
                );
                meta.addAttributeModifier(org.bukkit.attribute.Attribute.ARMOR, modifier);
            } catch (Throwable t) {
                // fallback or ignore if not supported
            }
            for (org.bukkit.inventory.ItemFlag flag : org.bukkit.inventory.ItemFlag.values()) {
                meta.addItemFlags(flag);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
