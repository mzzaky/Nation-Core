package id.nationcore.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;
import id.nationcore.models.DiplomacyRequest;
import id.nationcore.models.DiplomacyStatus;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

/**
 * Shared foundation for every per-nation Diplomacy Management interface.
 *
 * The plugin exposes one uniquely named subclass per government type (living in
 * each nation's own UI package). All rendering, navigation and security logic is
 * centralized here so the behaviour — and the safety guarantees — stay identical
 * across Republic, Communist, Monarchy and Caliphate.
 *
 * Two screens are produced:
 *   • Diplomacy Management — a 54-slot roster of every other registered nation
 *     together with the current relation and a statistics summary.
 *   • Diplomatic Envoy (Sub Menu Select) — a 27-slot panel for one target nation
 *     where the leadership may propose Peace, Alliance, Truce or War.
 *
 * Subclasses only supply theming (filler colour, accent colour, menu titles) and
 * the {@link GovernmentType} this menu is bound to.
 */
@SuppressWarnings("deprecation")
public abstract class AbstractDiplomacyMenu {

    protected final NationCore plugin;

    protected AbstractDiplomacyMenu(NationCore plugin) {
        this.plugin = plugin;
    }

    // ── Theming hooks (implemented per nation) ──────────────────────────────

    /** Stained-glass-pane material used as filler, coloured per nation type. */
    protected abstract Material fillerMaterial();

    /** Accent colour code (e.g. "§2") used for headings. */
    protected abstract String accent();

    /** Unique inventory title for the Diplomacy Management roster. */
    public abstract String managementTitle();

    /** Unique inventory title for the Sub Menu Select / Diplomatic Envoy panel. */
    public abstract String selectTitle();

    /** Government type this menu may only be operated by. */
    protected abstract GovernmentType expectedType();

    // ── Layout ──────────────────────────────────────────────────────────────

    private static final int[] MGMT_FILLER = {
            0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 50, 51, 52, 53
    };
    /** Roster slots, ordered, that display registered nations. */
    private static final int[] MGMT_LIST_SLOTS = {
            10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43
    };
    private static final int MGMT_STATS = 4;
    private static final int MGMT_BACK = 49;

    private static final int[] SELECT_FILLER = {
            0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 19, 20, 21, 23, 24, 25, 26
    };
    private static final int SELECT_INFO = 4;
    private static final int SELECT_PEACE = 10;
    private static final int SELECT_ALLIANCE = 12;
    private static final int SELECT_TRUCE = 14;
    private static final int SELECT_WAR = 16;
    private static final int SELECT_BACK = 22;

    private static final String DIVIDER = "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";

    // ── Rendering: Diplomacy Management roster ───────────────────────────────

    public void openManagement(Player player, Nation nation) {
        if (nation == null) return;

        Inventory inv = Bukkit.createInventory(null, 54, managementTitle());

        ItemStack filler = GovernmentGUIUtils.createItem(fillerMaterial(), " ");
        for (int slot : MGMT_FILLER) {
            inv.setItem(slot, filler);
        }

        inv.setItem(MGMT_STATS, buildStatsItem(nation));
        inv.setItem(MGMT_BACK, GovernmentGUIUtils.createItem(Material.SPECTRAL_ARROW, "§c§lBack",
                DIVIDER,
                "§7Return to the Government menu.",
                DIVIDER));

        List<Nation> others = listOtherNations(nation);
        if (others.isEmpty()) {
            inv.setItem(MGMT_LIST_SLOTS[0], GovernmentGUIUtils.createItem(Material.MAP,
                    "§7§lNo Other Nations",
                    DIVIDER,
                    "§7No other nations have been",
                    "§7founded on the server yet.",
                    DIVIDER));
        } else {
            int max = Math.min(others.size(), MGMT_LIST_SLOTS.length);
            for (int i = 0; i < max; i++) {
                inv.setItem(MGMT_LIST_SLOTS[i], buildNationEntry(nation, others.get(i)));
            }
        }

        player.openInventory(inv);
    }

    // ── Rendering: Sub Menu Select (Diplomatic Envoy) ────────────────────────

    public void openSelect(Player player, Nation viewer, Nation target) {
        if (viewer == null || target == null) return;

        Inventory inv = Bukkit.createInventory(null, 27, selectTitle());

        ItemStack filler = GovernmentGUIUtils.createItem(fillerMaterial(), " ");
        for (int slot : SELECT_FILLER) {
            inv.setItem(slot, filler);
        }

        boolean canManage = plugin.getDiplomacyManager().canManageDiplomacy(player.getUniqueId(), viewer);

        inv.setItem(SELECT_INFO, buildTargetInfo(viewer, target));
        inv.setItem(SELECT_PEACE, buildProposeButton(viewer, target, DiplomacyStatus.PEACE, canManage));
        inv.setItem(SELECT_ALLIANCE, buildProposeButton(viewer, target, DiplomacyStatus.ALLIANCE, canManage));
        inv.setItem(SELECT_TRUCE, buildProposeButton(viewer, target, DiplomacyStatus.TRUCE, canManage));
        inv.setItem(SELECT_WAR, buildProposeButton(viewer, target, DiplomacyStatus.WAR, canManage));
        inv.setItem(SELECT_BACK, GovernmentGUIUtils.createItem(Material.SPECTRAL_ARROW, "§c§lBack",
                DIVIDER,
                "§7Return to Diplomacy Management.",
                DIVIDER));

        player.openInventory(inv);
    }

    // ── Click handling: Diplomacy Management roster ──────────────────────────

    public void handleManagementClick(GUIListener gui, Player player, ItemStack clicked, int slot) {
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Nation nation = guardNation(player);
        if (nation == null) return;

        if (clicked.getType() == fillerMaterial()) return;

        MessageUtils.playSound(player, Sound.UI_BUTTON_CLICK);

        if (slot == MGMT_BACK) {
            gui.openGovernmentGUI(player);
            return;
        }
        if (slot == MGMT_STATS) {
            return; // statistics display only
        }

        Nation target = nationAtSlot(nation, slot);
        if (target == null) return;

        gui.viewingDiplomacyTarget.put(player.getUniqueId(), target.getId());
        openSelect(player, nation, target);
    }

    // ── Click handling: Sub Menu Select ──────────────────────────────────────

    public void handleSelectClick(GUIListener gui, Player player, ItemStack clicked, int slot) {
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Nation nation = guardNation(player);
        if (nation == null) return;

        if (clicked.getType() == fillerMaterial()) return;

        MessageUtils.playSound(player, Sound.UI_BUTTON_CLICK);

        if (slot == SELECT_BACK) {
            openManagement(player, nation);
            return;
        }
        if (slot == SELECT_INFO) {
            return; // information display only
        }

        DiplomacyStatus status = statusForSlot(slot);
        if (status == null) return;

        Nation target = resolveTarget(gui, player);
        if (target == null) {
            MessageUtils.send(player, "<red>That nation no longer exists.</red>");
            openManagement(player, nation);
            return;
        }

        // Authorization, self-targeting, duplicate-status and pending-request
        // checks are all enforced inside DiplomacyManager#proposeDiplomacy.
        plugin.getDiplomacyManager().proposeDiplomacy(player, nation, target, status);

        // Refresh so the envoy panel reflects the new pending request.
        Nation refreshed = plugin.getNationManager().getNation(target.getId());
        if (refreshed != null) {
            openSelect(player, nation, refreshed);
        } else {
            openManagement(player, nation);
        }
    }

    // ── Security helpers ──────────────────────────────────────────────────────

    /**
     * Re-resolves the clicking player's nation and verifies it still matches the
     * government type this menu is bound to. Prevents acting on a stale inventory
     * after the player left/disbanded/changed nations. Returns null (and closes
     * the inventory) when access is no longer valid.
     */
    private Nation guardNation(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null || nation.getType() != expectedType()) {
            MessageUtils.send(player, "<red>You can no longer access this diplomacy menu.</red>");
            player.closeInventory();
            return null;
        }
        return nation;
    }

    private Nation resolveTarget(GUIListener gui, Player player) {
        String targetId = gui.viewingDiplomacyTarget.get(player.getUniqueId());
        if (targetId == null) return null;
        return plugin.getNationManager().getNation(targetId);
    }

    private DiplomacyStatus statusForSlot(int slot) {
        return switch (slot) {
            case SELECT_PEACE -> DiplomacyStatus.PEACE;
            case SELECT_ALLIANCE -> DiplomacyStatus.ALLIANCE;
            case SELECT_TRUCE -> DiplomacyStatus.TRUCE;
            case SELECT_WAR -> DiplomacyStatus.WAR;
            default -> null;
        };
    }

    // ── Data helpers ──────────────────────────────────────────────────────────

    /** Every registered nation except the viewer, ordered by member count. */
    private List<Nation> listOtherNations(Nation viewer) {
        List<Nation> result = new ArrayList<>(plugin.getNationManager().getNationsSortedByMembers());
        result.removeIf(n -> n.getId().equals(viewer.getId()));
        return result;
    }

    /** Maps a clicked roster slot back to the nation rendered there (or null). */
    private Nation nationAtSlot(Nation viewer, int slot) {
        int idx = indexOf(MGMT_LIST_SLOTS, slot);
        if (idx < 0) return null;
        List<Nation> others = listOtherNations(viewer);
        if (idx >= others.size()) return null;
        return others.get(idx);
    }

    /** Outgoing proposals are stored on the recipient; scan for ours. */
    private int countOutgoingRequests(Nation viewer) {
        int count = 0;
        for (Nation n : plugin.getNationManager().getAllNations()) {
            if (n.getId().equals(viewer.getId())) continue;
            if (n.getDiplomacyRequest(viewer.getId()) != null) count++;
        }
        return count;
    }

    // ── Item builders ─────────────────────────────────────────────────────────

    private ItemStack buildStatsItem(Nation nation) {
        int total = listOtherNations(nation).size();
        int allies = 0, truces = 0, wars = 0;
        for (DiplomacyStatus status : nation.getDiplomacyRelations().values()) {
            switch (status) {
                case ALLIANCE -> allies++;
                case TRUCE -> truces++;
                case WAR -> wars++;
                default -> { }
            }
        }
        int peace = Math.max(0, total - (allies + truces + wars));
        int incoming = nation.getDiplomacyRequests().size();
        int outgoing = countOutgoingRequests(nation);

        return GovernmentGUIUtils.createItem(Material.GLOW_ITEM_FRAME,
                accent() + "§lDiplomacy Statistics",
                DIVIDER,
                "§7Registered nations : §f" + total,
                "",
                "§a● Peace     : §f" + peace,
                "§b● Alliance  : §f" + allies,
                "§e● Truce     : §f" + truces,
                "§c● War       : §f" + wars,
                "",
                "§7Incoming proposals : §f" + incoming,
                "§7Outgoing proposals : §f" + outgoing,
                DIVIDER,
                "§8An overview of your foreign relations");
    }

    private ItemStack buildNationEntry(Nation viewer, Nation target) {
        DiplomacyStatus status = viewer.getDiplomacyStatusWith(target.getId());

        List<String> lore = new ArrayList<>();
        lore.add(DIVIDER);
        lore.add("§7Government : §f" + target.getType().getDisplayName());
        lore.add("§7Tag        : §f[" + target.getTag() + "]");
        lore.add("§7Leader     : §f" + safe(target.getLeaderName()));
        lore.add("§7Members    : §f" + target.getMemberCount());
        lore.add("");
        lore.add("§7Relation   : " + tl(status.getColoredName()));

        DiplomacyRequest outgoing = target.getDiplomacyRequest(viewer.getId());
        if (outgoing != null) {
            lore.add("§e⏳ Proposal sent: " + tl(outgoing.getRequestedStatus().getColoredName()));
        }
        DiplomacyRequest incoming = viewer.getDiplomacyRequest(target.getId());
        if (incoming != null) {
            lore.add("§b⏳ They proposed: " + tl(incoming.getRequestedStatus().getColoredName()));
        }

        lore.add(DIVIDER);
        lore.add("§eClick to manage this relation.");

        return GovernmentGUIUtils.createItem(target.getType().getIconMaterial(),
                tl(status.getColorCode()) + "§l" + target.getName(),
                lore.toArray(new String[0]));
    }

    private ItemStack buildTargetInfo(Nation viewer, Nation target) {
        DiplomacyStatus status = viewer.getDiplomacyStatusWith(target.getId());

        List<String> lore = new ArrayList<>();
        lore.add(DIVIDER);
        lore.add("§7Government : §f" + target.getType().getDisplayName());
        lore.add("§7Tag        : §f[" + target.getTag() + "]");
        lore.add("§7Leader     : §f" + safe(target.getLeaderName()));
        lore.add("§7Members    : §f" + target.getMemberCount());
        lore.add("");
        lore.add("§7Current relation: " + tl(status.getColoredName()));

        DiplomacyRequest outgoing = target.getDiplomacyRequest(viewer.getId());
        if (outgoing != null) {
            lore.add("§e⏳ You proposed " + tl(outgoing.getRequestedStatus().getColoredName()) + " §e(awaiting reply)");
        }
        DiplomacyRequest incoming = viewer.getDiplomacyRequest(target.getId());
        if (incoming != null) {
            lore.add("§b⏳ They proposed " + tl(incoming.getRequestedStatus().getColoredName()) + "§b.");
            lore.add("§7Use §f/nc diplomacy accept " + target.getName() + " §7to accept.");
        }

        lore.add(DIVIDER);
        lore.add("§8Diplomatic dossier for this nation");

        return GovernmentGUIUtils.createItem(Material.GLOW_ITEM_FRAME,
                accent() + "§l" + target.getName(),
                lore.toArray(new String[0]));
    }

    private ItemStack buildProposeButton(Nation viewer, Nation target, DiplomacyStatus status, boolean canManage) {
        DiplomacyStatus current = viewer.getDiplomacyStatusWith(target.getId());
        String color = tl(status.getColorCode());

        List<String> lore = new ArrayList<>();
        lore.add(DIVIDER);
        lore.addAll(wrap("§7", status.getDescription(), 34));
        lore.add("");
        if (current == status) {
            lore.add("§7Current relation — already active.");
        } else if (!canManage) {
            lore.add("§cOnly the Leader or Defense Minister");
            lore.add("§ccan change diplomatic relations.");
        } else {
            lore.add("§eClick to propose " + color + status.getDisplayName() + "§e.");
        }
        lore.add(DIVIDER);

        return GovernmentGUIUtils.createItem(Material.WRITABLE_BOOK,
                color + "§lPropose " + status.getDisplayName(),
                lore.toArray(new String[0]));
    }

    // ── Tiny utilities ────────────────────────────────────────────────────────

    /** Translates ampersand colour codes to section signs for raw item text. */
    private String tl(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    private String safe(String input) {
        return (input == null || input.isBlank()) ? "Unknown" : input;
    }

    private int indexOf(int[] array, int value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) return i;
        }
        return -1;
    }

    /** Greedy word-wrap that keeps each lore line below {@code maxLen} chars. */
    private List<String> wrap(String prefix, String text, int maxLen) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() > 0 && line.length() + word.length() + 1 > maxLen) {
                out.add(prefix + line);
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) out.add(prefix + line);
        return out;
    }
}
