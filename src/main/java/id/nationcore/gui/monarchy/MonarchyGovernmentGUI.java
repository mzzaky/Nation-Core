package id.nationcore.gui.monarchy;

import id.nationcore.gui.GovernmentGUIUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;
import id.nationcore.models.MonarchyGovernment;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

/**
 * Government menu for the Royal Court of a MONARCHY nation.
 *
 * The page is access-controlled — only the King and members of the High
 * Council may open it. The button row at the bottom mirrors the Communist
 * variant (rename, leave, disband) so the same chat-input flow can be
 * reused without adding a fourth pending-state map.
 */
public class MonarchyGovernmentGUI {

    @SuppressWarnings("unused")
    private final NationCore plugin;
    public static final String TITLE = "§6§lROYAL COURT";

    private static final int[] FILLER_SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 51, 52, 53
    };

    public MonarchyGovernmentGUI(NationCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Nation nation) {
        if (nation == null) return;

        MonarchyGovernment mg = nation.getMonarchyGovernment();
        if (mg == null) return;

        boolean isKing = mg.hasKing() && mg.getKingUUID().equals(player.getUniqueId());
        boolean isCouncil = mg.getCouncilMemberByUUID(player.getUniqueId()) != null;

        if (!isKing && !isCouncil && !player.hasPermission("nation.admin")) {
            MessageUtils.send(player, "§cOnly the King and the High Council can enter the Royal Court.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        ItemStack filler = GovernmentGUIUtils.createItem(Material.YELLOW_STAINED_GLASS_PANE, " ");
        for (int slot : FILLER_SLOTS) {
            inv.setItem(slot, filler);
        }

        inv.setItem(4, buildNationProfile(nation, mg));

        inv.setItem(13, GovernmentGUIUtils.createItem(Material.WRITABLE_BOOK, "§6§lRoyal Decisions",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Open the catalogue of",
                "§7High Council orders and",
                "§7the King's executive orders.",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§eClick to open."));

        inv.setItem(21, GovernmentGUIUtils.createItem(Material.GOLDEN_HELMET, "§e§lHigh Council Management",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7This feature is currently",
                "§7under development.",
                "",
                "§c⚠ Coming Soon",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        inv.setItem(22, GovernmentGUIUtils.createItem(Material.PLAYER_HEAD, "§e§lSubject Management",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7This feature is currently",
                "§7under development.",
                "",
                "§c⚠ Coming Soon",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        inv.setItem(23, GovernmentGUIUtils.createItem(Material.GOLDEN_SWORD, "§e§lRoyal Soldier Roster",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7This feature is currently",
                "§7under development.",
                "",
                "§c⚠ Coming Soon",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        inv.setItem(30, GovernmentGUIUtils.createItem(Material.LODESTONE, "§e§lBorder Management",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7This feature is currently",
                "§7under development.",
                "",
                "§c⚠ Coming Soon",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        inv.setItem(31, GovernmentGUIUtils.createItem(Material.BELL, "§e§lEvent Management",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7This feature is currently",
                "§7under development.",
                "",
                "§c⚠ Coming Soon",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        inv.setItem(32, GovernmentGUIUtils.createItem(Material.PAPER, "§e§lDiplomacy Management",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7This feature is currently",
                "§7under development.",
                "",
                "§c⚠ Coming Soon",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        inv.setItem(43, GovernmentGUIUtils.createItem(Material.SPECTRAL_ARROW, "§c§lBack",
                "§7Return to previous menu"));

        inv.setItem(48, GovernmentGUIUtils.createItem(Material.WRITABLE_BOOK, "§e§lRename Nation",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Click to rename your kingdom.",
                "§7You will be prompted to type",
                "§7the new name in chat.",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        inv.setItem(49, GovernmentGUIUtils.createItem(Material.COMPASS, "§e§lLeave Nation",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Click to renounce your loyalty.",
                "§c⚠ You cannot undo this action!",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        inv.setItem(50, GovernmentGUIUtils.createItem(Material.TNT_MINECART, "§e§lDisband Nation",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Click to dissolve your kingdom.",
                "§c⚠ Warning: Irreversible action!",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        player.openInventory(inv);
    }

    private ItemStack buildNationProfile(Nation nation, MonarchyGovernment mg) {
        int memberCount = nation.getMemberCount();
        int soldiers = mg != null ? mg.getRoyalSoldierCount() : 0;

        return GovernmentGUIUtils.createItem(Material.GLOW_ITEM_FRAME,
                "§e§l" + nation.getName(),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Government : §fMonarchy",
                "§7Tag        : §f[" + nation.getTag() + "]",
                "§7Subjects   : §f" + memberCount,
                "§7Soldiers   : §f" + soldiers,
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§8Displays information about the kingdom");
    }
}
