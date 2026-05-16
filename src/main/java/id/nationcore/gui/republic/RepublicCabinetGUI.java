package id.nationcore.gui.republic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import id.nationcore.utils.MessageUtils;

/**
 * Republic Cabinet GUI — displays all cabinet positions, their current ministers,
 * statistics, and allows the President to appoint players to each position.
 */
public class RepublicCabinetGUI {

    private final NationCore plugin;

    // ── GUI Titles ──────────────────────────────────────────────────────────
    public static final String CABINET_GUI_TITLE = "§e§l📋 CABINET 📋";
    public static final String CABINET_DECISIONS_TITLE = "§d§l⚖ CABINET DECISIONS ⚖";
    public static final String CABINET_APPOINT_TITLE = "§a§l👤 APPOINT MINISTER: ";

    // ── Slot layout for the 5 cabinet positions ──────────────────────────
    private static final int[] MINISTER_SLOTS = { 11, 13, 15 };

    public RepublicCabinetGUI(NationCore plugin) {
        this.plugin = plugin;
    }

    // ════════════════════════════════════════════════════════════════════════
    // MAIN CABINET MENU
    // ════════════════════════════════════════════════════════════════════════

    public void openCabinetMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, CABINET_GUI_TITLE);
        Government gov = plugin.getDataManager().getGovernment();

        boolean isPresident = gov.hasPresident()
                && gov.getPresidentUUID().equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("nation.admin");
        boolean canAppoint = isPresident || isAdmin;

        // ── Header ──────────────────────────────────────────────────────────
        int filledPositions = 0;
        for (Government.CabinetPosition pos : Government.CabinetPosition.values()) {
            if (gov.getCabinetMember(pos) != null)
                filledPositions++;
        }

        ItemStack headerItem = createItem(Material.ENCHANTED_BOOK,
                "§e§l📋 Cabinet Overview",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Total Positions: §f" + Government.CabinetPosition.values().length,
                "§7Filled: §a" + filledPositions + " §7/ §f" + Government.CabinetPosition.values().length,
                "§7Vacant: §c" + (Government.CabinetPosition.values().length - filledPositions),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                canAppoint ? "§aClick a position to appoint." : "§7Click a minister to view decisions.");
        inv.setItem(4, headerItem);

        // ── Cabinet position slots ───────────────────────────────────────────
        Government.CabinetPosition[] positions = Government.CabinetPosition.values();
        for (int i = 0; i < MINISTER_SLOTS.length && i < positions.length; i++) {
            Government.CabinetPosition pos = positions[i];
            UUID ministerUUID = gov.getCabinetMember(pos);

            ItemStack item;
            if (ministerUUID != null) {
                item = createMinisterHead(ministerUUID, pos, gov, canAppoint);
            } else {
                item = createVacantSlot(pos, canAppoint);
            }
            inv.setItem(MINISTER_SLOTS[i], item);
        }

        // ── President info ─────────────────────────────────────────────────
        if (gov.hasPresident()) {
            ItemStack presidentItem = createPresidentHead(gov);
            inv.setItem(31, presidentItem);
        }

        // ── Active decisions summary ────────────────────────────────────────
        List<CabinetDecision> activeDecisions = plugin.getDataManager().getActiveDecisions();
        ItemStack decisionsItem;
        if (!activeDecisions.isEmpty()) {
            List<String> loreParts = new ArrayList<>();
            loreParts.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            loreParts.add("§7Active decisions: §a" + activeDecisions.size());
            loreParts.add("");
            activeDecisions.stream().limit(5).forEach(d -> loreParts.add(
                    "§7• §f" + d.getType().name()
                            + " §7(" + d.getMinisterPosition().getDisplayName() + ")"));
            if (activeDecisions.size() > 5)
                loreParts.add("§7... and §f" + (activeDecisions.size() - 5) + " §7more");
            loreParts.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            loreParts.add("§aClick to view active decisions");
            decisionsItem = createItem(Material.BEACON,
                    "§d§lActive Cabinet Decisions: §f" + activeDecisions.size(),
                    loreParts.toArray(new String[0]));
        } else {
            decisionsItem = createItem(Material.GLASS,
                    "§7§lNo Active Decisions",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7No cabinet decisions are",
                    "§7currently active.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        }
        inv.setItem(29, decisionsItem);

        // ── My position button ─────────────────────────────────────────────
        UUID myUUID = player.getUniqueId();
        Government.CabinetPosition myPos = gov.getPositionByUUID(myUUID);
        if (myPos != null) {
            ItemStack myPosItem = createItem(Material.DIAMOND,
                    "§b§lYour Position: §f" + myPos.getDisplayName(),
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7You are currently serving as:",
                    "§e" + myPos.getDisplayName(),
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§aClick to manage your decisions");
            inv.setItem(33, myPosItem);
        }

        // ── Navigation ─────────────────────────────────────────────────────
        inv.setItem(45, createItem(Material.ARROW, "§7§l← Back", "§7Return to Government Menu"));
        inv.setItem(53, createItem(Material.BARRIER, "§c§lClose", "§7Click to close"));

        fillGlass(inv);
        player.openInventory(inv);
    }

    // ════════════════════════════════════════════════════════════════════════
    // CABINET DECISIONS MENU
    // ════════════════════════════════════════════════════════════════════════

    public void openCabinetDecisionsMenu(Player player, CabinetDecision.CabinetPosition position) {
        Inventory inv = Bukkit.createInventory(null, 36, CABINET_DECISIONS_TITLE);
        Government gov = plugin.getDataManager().getGovernment();

        UUID minister = gov.getCabinetMember(Government.CabinetPosition.valueOf(position.name()));
        boolean canIssue = minister != null && minister.equals(player.getUniqueId());

        // ── Position info header ────────────────────────────────────────────
        String ministerName = minister != null
                ? Bukkit.getOfflinePlayer(minister).getName()
                : "§cEmpty";
        ItemStack infoItem = createItem(Material.PAPER,
                "§6§l" + position.getDisplayName(),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Minister: §f" + ministerName,
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(4, infoItem);

        // ── Decision items ──────────────────────────────────────────────────
        int slot = 10;
        for (CabinetDecision.DecisionType type : CabinetDecision.DecisionType.values()) {
            if (type.getPosition() != position)
                continue;
            if (slot >= 26)
                break;

            boolean active = plugin.getCabinetManager().isDecisionActive(type);
            boolean onCooldown = false; // Cooldown not yet implemented

            Material material;
            String status;
            List<String> lore = new ArrayList<>();

            if (active) {
                material = Material.LIME_WOOL;
                status = "§a[ACTIVE]";
                CabinetDecision decision = plugin.getDataManager().getActiveDecisions().stream()
                        .filter(d -> d.getType() == type)
                        .findFirst().orElse(null);
                if (decision != null) {
                    lore.add("§7Time left: §f" + MessageUtils.formatTime(decision.getRemainingTime()));
                }
            } else if (onCooldown) {
                material = Material.RED_WOOL;
                status = "§c[COOLDOWN]";
                lore.add("§7Cooldown: §fN/A");
            } else {
                material = Material.YELLOW_WOOL;
                status = "§e[AVAILABLE]";
            }

            lore.add("");
            lore.add("§7" + type.getDescription());
            lore.add("");
            lore.add("§7Duration: §f" + MessageUtils.formatTime(type.getDurationMillis() > 0 ? type.getDurationMillis() : 24 * 3600000L));
            lore.add("§7Cost: §6" + MessageUtils.formatNumber(plugin.getCabinetManager().getDecisionCost(type)));

            if (canIssue && !active && !onCooldown) {
                lore.add("");
                lore.add("§aClick to issue!");
            }

            inv.setItem(slot, createItem(material,
                    "§e§l" + type.name() + " " + status,
                    lore.toArray(new String[0])));

            slot++;
            if ((slot + 1) % 9 == 0)
                slot += 2;
        }

        inv.setItem(27, createItem(Material.ARROW, "§7§lBack", "§7Return to Cabinet"));
        inv.setItem(35, createItem(Material.BARRIER, "§c§lClose", "§7Click to close"));

        fillGlass(inv);
        player.openInventory(inv);
    }

    // ════════════════════════════════════════════════════════════════════════
    // APPOINTMENT SUBMENU (President only)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Opens a player-picker GUI so the President can select who to appoint
     * into the given cabinet position.
     */
    public void openAppointMenu(Player player, Government.CabinetPosition position) {
        String title = CABINET_APPOINT_TITLE + position.getDisplayName();
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // ── Position info header ────────────────────────────────────────────
        Government gov = plugin.getDataManager().getGovernment();
        UUID currentMinister = gov.getCabinetMember(position);
        String currentName = currentMinister != null
                ? Bukkit.getOfflinePlayer(currentMinister).getName()
                : "None";

        ItemStack infoItem = createItem(Material.LECTERN,
                "§e§l" + position.getDisplayName(),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Role: §f" + position.getDisplayName(),
                "§7Current Minister: §6" + currentName,
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Click a player below to appoint",
                "§7them as the new minister.",
                currentMinister != null ? "§c⚠ This will replace the current minister." : "");
        inv.setItem(4, infoItem);

        // ── Online player list ──────────────────────────────────────────────
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        int slot = 9; // Start from row 2
        for (Player target : onlinePlayers) {
            if (slot >= 45)
                break; // Max 36 player slots (rows 2-5)

            // Skip the President themselves from the picker
            if (target.getUniqueId().equals(player.getUniqueId()))
                continue;

            boolean isCurrentMinister = target.getUniqueId().equals(currentMinister);
            Government.CabinetPosition existingPos = gov.getPositionByUUID(target.getUniqueId());

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(target);

            List<String> lore = new ArrayList<>();
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

            if (isCurrentMinister) {
                meta.setDisplayName("§a✔ §f" + target.getName() + " §7(Current)");
                lore.add("§7Currently serving as:");
                lore.add("§e" + position.getDisplayName());
                lore.add("");
                lore.add("§c⚠ Click to remove from position");
            } else {
                meta.setDisplayName("§f§l" + target.getName());
                if (existingPos != null) {
                    lore.add("§7Currently serving as: §e" + existingPos.getDisplayName());
                    lore.add("§c⚠ Will be moved from current role");
                } else {
                    lore.add("§7Status: §aAvailable");
                }
                lore.add("");
                lore.add("§aClick to appoint as §e" + position.getDisplayName());
            }

            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            meta.setLore(lore);
            head.setItemMeta(meta);
            inv.setItem(slot, head);
            slot++;
        }

        // ── Empty state ──────────────────────────────────────────────────────
        if (onlinePlayers.size() <= 1) { // Only the president is online
            ItemStack emptyItem = createItem(Material.GRAY_CONCRETE,
                    "§c§lNo Players Online",
                    "§7There are no other players",
                    "§7online to appoint.");
            inv.setItem(22, emptyItem);
        }

        // ── Remove current minister button ──────────────────────────────────
        if (currentMinister != null) {
            ItemStack removeItem = createItem(Material.RED_CONCRETE,
                    "§c§lRemove Current Minister",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Minister: §f" + currentName,
                    "§7Position: §e" + position.getDisplayName(),
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§c⚠ Click to remove the current minister",
                    "§7and leave the position vacant.");
            inv.setItem(45, removeItem);
        }

        // ── Navigation ─────────────────────────────────────────────────────
        inv.setItem(49, createItem(Material.ARROW, "§7§l← Back", "§7Return to Cabinet"));
        inv.setItem(53, createItem(Material.BARRIER, "§c§lClose", "§7Click to close"));

        fillGlass(inv);
        player.openInventory(inv);
    }

    // ════════════════════════════════════════════════════════════════════════
    // ITEM BUILDERS
    // ════════════════════════════════════════════════════════════════════════

    private ItemStack createMinisterHead(UUID ministerUUID,
            Government.CabinetPosition pos,
            Government gov,
            boolean canAppoint) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        var offlinePlayer = Bukkit.getOfflinePlayer(ministerUUID);
        meta.setOwningPlayer(offlinePlayer);
        meta.setDisplayName("§e§l" + pos.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Minister: §f" + offlinePlayer.getName());

        // Appointment time
        Government.CabinetMember member = gov.getCabinetMemberObject(pos);
        if (member != null) {
            long appointedAgo = System.currentTimeMillis() - member.getAppointedTime();
            lore.add("§7Appointed: §f" + MessageUtils.formatTime(appointedAgo) + " ago");
            lore.add("§7Decisions used: §f" + member.getDecisionsUsed());
        }

        // Active decisions count
        long decisionCount = plugin.getDataManager().getActiveDecisions().stream()
                .filter(d -> d.getMinisterPosition().name().equals(pos.name()))
                .count();
        lore.add("§7Active decisions: §a" + decisionCount);

        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        if (canAppoint) {
            lore.add("§a[Left Click] §7View decisions");
            lore.add("§e[Right Click] §7Appoint new minister");
        } else {
            lore.add("§aClick for decisions");
        }

        meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createVacantSlot(Government.CabinetPosition pos, boolean canAppoint) {
        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§cVacant Position");
        lore.add("");

        // Position description
        String description = switch (pos) {
            case DEFENSE -> "Oversees military and defense policy.";
            case TREASURY -> "Manages the national treasury.";
            case HEALTH -> "Manages national health and emergency protocols.";
            default -> "Minister of the government.";
        };
        lore.add("§7" + description);
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        if (canAppoint) {
            lore.add("§eClick to appoint a minister");
        } else {
            lore.add("§7Waiting to be appointed");
            lore.add("§7by President");
        }

        return createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                "§7§l" + pos.getDisplayName(),
                lore.toArray(new String[0]));
    }

    private ItemStack createPresidentHead(Government gov) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        var offlinePlayer = Bukkit.getOfflinePlayer(gov.getPresidentUUID());
        meta.setOwningPlayer(offlinePlayer);
        meta.setDisplayName("§6§l👑 PRESIDENT: " + offlinePlayer.getName());

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Term #" + gov.getCurrentTerm());
        lore.add("§7Time left: §f" + MessageUtils.formatTime(gov.getTermEndTime() - System.currentTimeMillis()));
        lore.add("§7Approval: §e" + String.format("%.1f", gov.getApprovalRating()) + "/5.0 ⭐");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
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

    // ── Slot helpers ────────────────────────────────────────────────────────

    /**
     * Maps an inventory slot from the main cabinet menu to its
     * corresponding {@link Government.CabinetPosition}, or {@code null}
     * if the slot is not a minister slot.
     */
    public Government.CabinetPosition getPositionForSlot(int slot) {
        Government.CabinetPosition[] positions = Government.CabinetPosition.values();
        for (int i = 0; i < MINISTER_SLOTS.length && i < positions.length; i++) {
            if (MINISTER_SLOTS[i] == slot)
                return positions[i];
        }
        return null;
    }
}
