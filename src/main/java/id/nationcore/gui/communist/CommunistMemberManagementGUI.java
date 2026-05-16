package id.nationcore.gui.communist;

import java.util.ArrayList;
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
import id.nationcore.models.CommunistGovernment;
import id.nationcore.models.CommunistGovernment.PolitburoPosition;
import id.nationcore.models.Nation;
import id.nationcore.models.Nation.NationMember;
import id.nationcore.models.Nation.NationRole;
import id.nationcore.utils.MessageUtils;

/**
 * Custom GUI for nation member management in a Communist government.
 *
 * <p>Layout (6 rows × 9 cols = 54 slots):</p>
 * <pre>
 *  Row 0 (0-8)   : header / filler
 *  Row 1-4       : member heads (slots 9-44, up to 36 entries per page)
 *  Row 5 (45-53) : navigation (prev / info / next / back / close)
 * </pre>
 *
 * <p>Clicking a member head opens an action sub-menu for that member.</p>
 *
 * <h3>Action sub-menu titles (prefix-based for identification):</h3>
 * <ul>
 *   <li>{@link #ACTION_TITLE_PREFIX} + playerName  — member action menu</li>
 *   <li>{@link #POLITBURO_PICK_TITLE_PREFIX} + playerName — position picker</li>
 * </ul>
 */
public class CommunistMemberManagementGUI {

    // ── GUI title constants ────────────────────────────────────────────────
    public static final String TITLE         = "§c§l⚑ MEMBER MANAGEMENT ⚑";
    public static final String ACTION_TITLE_PREFIX        = "§c§l[ACTION] §f";
    public static final String POLITBURO_PICK_TITLE_PREFIX = "§c§l[POLITBURO] §f";

    /** Slots in the main list used for member heads (rows 1-4). */
    private static final int MEMBERS_PER_PAGE = 28;
    private static final int[] MEMBER_SLOTS;

    static {
        // Slots 10-16, 19-25, 28-34, 37-43 — leaving edges as glass pane borders
        List<Integer> slots = new ArrayList<>();
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                slots.add(row * 9 + col);
            }
        }
        MEMBER_SLOTS = slots.stream().mapToInt(Integer::intValue).toArray();
    }

    private final NationCore plugin;

    public CommunistMemberManagementGUI(NationCore plugin) {
        this.plugin = plugin;
    }

    // ════════════════════════════════════════════════════════════════════════
    // MAIN LIST
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Opens the main member list (page 0 = first page).
     */
    public void open(Player viewer, Nation nation, int page) {
        if (nation == null) return;
        CommunistGovernment cg = nation.getCommunistGovernment();

        List<NationMember> members = new ArrayList<>(nation.getMembers().values());
        // Sort: leader first, then officers, then citizens
        members.sort((a, b) -> a.getRole().ordinal() - b.getRole().ordinal());

        int totalPages = Math.max(1, (int) Math.ceil(members.size() / (double) MEMBERS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // ── Glass border ───────────────────────────────────────────────────
        ItemStack border = createItem(Material.RED_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, border);
        }

        // ── Header info (slot 4) ───────────────────────────────────────────
        int nationMembers  = nation.getMemberCount();
        int partyMembers   = cg != null ? cg.getPartyMemberCount() : 0;
        int politburoCount = cg != null ? cg.getPolitburo().size() : 0;

        inv.setItem(4, createItem(Material.WRITABLE_BOOK,
                "§c§l⚑ " + nation.getName() + " — Member Management",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Nation Members : §f" + nationMembers,
                "§7Party Members  : §f" + partyMembers,
                "§7Politburo      : §f" + politburoCount + " / " + PolitburoPosition.values().length,
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Click a member to manage."));

        // ── Member heads ───────────────────────────────────────────────────
        int startIndex = page * MEMBERS_PER_PAGE;
        for (int i = 0; i < MEMBERS_PER_PAGE; i++) {
            int memberIndex = startIndex + i;
            if (memberIndex >= members.size()) break;

            NationMember member = members.get(memberIndex);
            inv.setItem(MEMBER_SLOTS[i], buildMemberHead(member, cg));
        }

        // ── Navigation row (row 5) ─────────────────────────────────────────
        // Previous page
        if (page > 0) {
            inv.setItem(45, createItem(Material.ARROW,
                    "§e§l← Previous Page",
                    "§7Page " + page + " / " + totalPages));
        }

        // Page indicator (slot 49)
        inv.setItem(49, createItem(Material.PAPER,
                "§e§lPage " + (page + 1) + " / " + totalPages,
                "§7" + members.size() + " members total"));

        // Next page
        if (page < totalPages - 1) {
            inv.setItem(53, createItem(Material.ARROW,
                    "§e§lNext Page →",
                    "§7Page " + (page + 2) + " / " + totalPages));
        }

        // Back to Government GUI (slot 47)
        inv.setItem(47, createItem(Material.SPECTRAL_ARROW,
                "§c§l← Back",
                "§7Return to Government Menu"));

        viewer.openInventory(inv);
    }

    // ════════════════════════════════════════════════════════════════════════
    // ACTION SUB-MENU
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Opens an action menu for a specific member (kick / party / politburo).
     *
     * @param viewer   the player viewing the GUI
     * @param nation   the nation the target belongs to
     * @param targetUUID UUID of the member being managed
     */
    public void openActionMenu(Player viewer, Nation nation, UUID targetUUID) {
        if (nation == null) return;
        NationMember member = nation.getMember(targetUUID);
        if (member == null) {
            MessageUtils.send(viewer, "§cMember not found.");
            return;
        }

        CommunistGovernment cg = nation.getCommunistGovernment();
        String name = member.getName();
        String title = ACTION_TITLE_PREFIX + name;

        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Border
        ItemStack border = createItem(Material.RED_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        // ── Member profile (slot 4) ───────────────────────────────────────
        boolean isParty     = cg != null && cg.isPartyMember(targetUUID);
        boolean isPolitburo = cg != null && cg.getPolitburoMemberByUUID(targetUUID) != null;
        boolean isSekjen    = cg != null && cg.hasSecretaryGeneral()
                && cg.getSecretaryGeneralUUID().equals(targetUUID);
        boolean isLeader    = nation.getLeaderUUID() != null
                && nation.getLeaderUUID().equals(targetUUID);

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skull = (SkullMeta) head.getItemMeta();
        skull.setOwningPlayer(Bukkit.getOfflinePlayer(targetUUID));
        skull.setDisplayName("§e§l" + name);

        List<String> profileLore = new ArrayList<>();
        profileLore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        profileLore.add("§7Nation Role  : " + formatRole(member.getRole()));
        profileLore.add("§7Party Member : " + (isParty     ? "§a✔ Yes" : "§c✘ No"));
        profileLore.add("§7Politburo    : " + (isPolitburo ? "§a✔ " + getPolitburoPositionName(cg, targetUUID) : "§c✘ No"));
        profileLore.add("§7Secretary    : " + (isSekjen    ? "§a✔ Yes" : "§c✘ No"));
        profileLore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        skull.setLore(profileLore);
        head.setItemMeta(skull);
        inv.setItem(4, head);

        // ── Action buttons ────────────────────────────────────────────────

        // 1. Kick (slot 10) — disabled for leader/sekjen
        if (!isLeader && !isSekjen) {
            inv.setItem(10, createItem(Material.RED_CONCRETE,
                    "§c§l✖ Kick from Nation",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Remove §f" + name + " §7from the nation.",
                    "§c⚠ This action cannot be undone!",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§cClick to kick this player."));
        } else {
            inv.setItem(10, createItem(Material.BARRIER,
                    "§7§l✖ Kick Unavailable",
                    "§7Cannot kick the nation leader / Secretary General."));
        }

        // 2. Add to Party (slot 13) — only if not already party member
        if (!isParty) {
            inv.setItem(13, createItem(Material.LIME_CONCRETE,
                    "§a§l★ Promote to Party",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Grant §f" + name + " §7Party membership.",
                    "§7Party members can vote in",
                    "§7Secretary General elections.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§aClick to add to Party."));
        } else {
            inv.setItem(13, createItem(Material.YELLOW_CONCRETE,
                    "§e§l★ Party Member",
                    "§7" + name + " §7is already a Party member."));
        }

        // 3. Appoint to Politburo (slot 16) — only if already in party
        if (isParty && !isSekjen) {
            inv.setItem(16, createItem(Material.GOLD_BLOCK,
                    "§6§l⬆ Appoint to Politburo",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Appoint §f" + name + " §7as a",
                    "§7Politburo (Cabinet) minister.",
                    "§7You will choose the position",
                    "§7on the next screen.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§6Click to select position."));
        } else if (!isParty) {
            inv.setItem(16, createItem(Material.GRAY_CONCRETE,
                    "§7§l⬆ Politburo Unavailable",
                    "§7Must be a Party member first.",
                    "§7Add to Party before appointing",
                    "§7to the Politburo."));
        } else {
            // is Sekjen — already highest rank
            inv.setItem(16, createItem(Material.YELLOW_CONCRETE,
                    "§e§l⬆ Secretary General",
                    "§7" + name + " §7is the Secretary General."));
        }

        // ── Back button ───────────────────────────────────────────────────
        inv.setItem(22, createItem(Material.SPECTRAL_ARROW,
                "§c§l← Back",
                "§7Return to member list."));

        viewer.openInventory(inv);
    }

    // ════════════════════════════════════════════════════════════════════════
    // POLITBURO POSITION PICKER
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Opens a position picker so the Sekjen can choose which Politburo slot
     * to appoint the target player into.
     *
     * @param viewer      the Sekjen
     * @param nation      the nation
     * @param targetUUID  the player being appointed
     */
    public void openPolitburoPositionPicker(Player viewer, Nation nation, UUID targetUUID) {
        if (nation == null) return;
        NationMember member = nation.getMember(targetUUID);
        if (member == null) return;

        CommunistGovernment cg = nation.getCommunistGovernment();
        String name = member.getName();
        String title = POLITBURO_PICK_TITLE_PREFIX + name;

        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Border
        ItemStack border = createItem(Material.RED_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        // Header (slot 4)
        inv.setItem(4, createItem(Material.GOLD_INGOT,
                "§6§lAppoint: §f" + name,
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Select a Politburo position below.",
                "§7If the slot is occupied it will",
                "§7be replaced by §f" + name + "§7.",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        // Position buttons (slots 10, 12, 14, 16)
        int[] positionSlots = {10, 12, 14, 16};
        PolitburoPosition[] positions = PolitburoPosition.values();
        Material[] positionMaterials = {
                Material.MAGENTA_WOOL,    // PROPAGANDA
                Material.RED_WOOL,        // DEFENSE
                Material.GOLD_BLOCK,      // TREASURY
                Material.LIME_WOOL        // HEALTH
        };

        for (int i = 0; i < positions.length && i < positionSlots.length; i++) {
            PolitburoPosition pos = positions[i];
            CommunistGovernment.PolitburoMember current = cg != null ? cg.getPolitburoMember(pos) : null;

            List<String> lore = new ArrayList<>();
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            if (current != null) {
                lore.add("§7Current: §e" + current.getName());
                lore.add("§c⚠ Will replace current minister");
            } else {
                lore.add("§7Status: §aVacant");
            }
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            lore.add("§6Click to appoint to this slot.");

            inv.setItem(positionSlots[i], createItem(positionMaterials[i],
                    "§e§l" + pos.getDisplayName(),
                    lore.toArray(new String[0])));
        }

        // Back (slot 22)
        inv.setItem(22, createItem(Material.SPECTRAL_ARROW,
                "§c§l← Back",
                "§7Return to member actions."));

        viewer.openInventory(inv);
    }

    // ════════════════════════════════════════════════════════════════════════
    // ITEM BUILDERS
    // ════════════════════════════════════════════════════════════════════════

    private ItemStack buildMemberHead(NationMember member, CommunistGovernment cg) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(member.getUuid()));

        boolean isParty     = cg != null && cg.isPartyMember(member.getUuid());
        boolean isPolitburo = cg != null && cg.getPolitburoMemberByUUID(member.getUuid()) != null;
        boolean isSekjen    = cg != null && cg.hasSecretaryGeneral()
                && cg.getSecretaryGeneralUUID().equals(member.getUuid());
        boolean isOnline    = Bukkit.getPlayer(member.getUuid()) != null;

        // Prefix icon in name based on highest rank
        String prefix;
        if (isSekjen)         prefix = "§c☆ ";
        else if (isPolitburo) prefix = "§6★ ";
        else if (isParty)     prefix = "§a▶ ";
        else                  prefix = "§7· ";

        meta.setDisplayName(prefix + member.getName());

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Status    : " + (isOnline ? "§aOnline" : "§8Offline"));
        lore.add("§7Role      : " + formatRole(member.getRole()));
        lore.add("§7Party     : " + (isParty     ? "§a✔ Member" : "§7—"));
        if (isPolitburo && cg != null) {
            lore.add("§7Politburo : §6✔ " + getPolitburoPositionName(cg, member.getUuid()));
        } else if (isSekjen) {
            lore.add("§7Secretary : §c✔ General");
        }
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§eClick to manage this member.");

        meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        if (lore.length > 0) {
            List<String> loreList = new ArrayList<>();
            for (String line : lore) loreList.add(line);
            meta.setLore(loreList);
        }
        item.setItemMeta(meta);
        return item;
    }

    // ════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════════════════

    private String formatRole(NationRole role) {
        return switch (role) {
            case LEADER  -> "§c§lLeader";
            case OFFICER -> "§6Officer";
            case CITIZEN -> "§7Citizen";
        };
    }

    private String getPolitburoPositionName(CommunistGovernment cg, UUID uuid) {
        if (cg == null) return "Unknown";
        PolitburoPosition pos = cg.getPositionByUUID(uuid);
        return pos != null ? pos.getDisplayName() : "Unknown";
    }

    /**
     * Resolves the target player UUID from an action-menu title.
     * Returns null if the title does not match.
     */
    public static String extractNameFromActionTitle(String title, String prefix) {
        if (title == null || !title.startsWith(prefix)) return null;
        return title.substring(prefix.length());
    }

    /**
     * Resolves the PolitburoPosition clicked in the position-picker inventory
     * based on the slot number.
     */
    public static PolitburoPosition getPositionForPickerSlot(int slot) {
        PolitburoPosition[] positions = PolitburoPosition.values();
        int[] positionSlots = {10, 12, 14, 16};
        for (int i = 0; i < positionSlots.length && i < positions.length; i++) {
            if (positionSlots[i] == slot) return positions[i];
        }
        return null;
    }
}
