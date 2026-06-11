package id.nationcore.gui.caliphate;

import id.nationcore.gui.GovernmentGUIUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;
import id.nationcore.models.CaliphateGovernment;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

/**
 * Government menu for the Caliphate Court of a CALIPHATE nation.
 *
 * Caliphate has no cabinet/ministers; this menu is therefore intentionally
 * sparse — exposing executive-order access plus advisory rosters and the
 * standard rename/leave/disband row that mirrors the Monarchy variant.
 */
public class CaliphateGovernmentGUI {

    @SuppressWarnings("unused")
    private final NationCore plugin;
    public static final String TITLE = "§2§lCALIPHATE COURT";

    private static final int[] FILLER_SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 51, 52, 53
    };

    public CaliphateGovernmentGUI(NationCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Nation nation) {
        if (nation == null) return;

        CaliphateGovernment cg = nation.getCaliphateGovernment();
        if (cg == null) return;

        boolean isCaliph = cg.hasCaliph() && cg.getCaliphUUID().equals(player.getUniqueId());
        boolean isShura = cg.isShuraMember(player.getUniqueId());
        boolean isScholar = cg.isScholar(player.getUniqueId());

        if (!isCaliph && !isShura && !isScholar && !player.hasPermission("nation.admin")) {
            MessageUtils.send(player, "§cOnly the Caliph, Shura Council, and State Scholars may enter the Caliphate Court.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        ItemStack filler = GovernmentGUIUtils.createItem(Material.LIME_STAINED_GLASS_PANE, " ");
        for (int slot : FILLER_SLOTS) {
            inv.setItem(slot, filler);
        }

        inv.setItem(4, buildNationProfile(nation, cg));

        inv.setItem(13, GovernmentGUIUtils.createItem(Material.WRITABLE_BOOK, "§2§lExecutive Orders",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Open the catalogue of",
                "§7the Caliph's executive orders.",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§aClick to open."));

        inv.setItem(21, GovernmentGUIUtils.createItem(Material.EMERALD, "§a§lShura Council Management",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7This feature is currently",
                "§7under development.",
                "",
                "§c⚠ Coming Soon",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        inv.setItem(22, GovernmentGUIUtils.createItem(Material.PLAYER_HEAD, "§e§lCitizen Management",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7This feature is currently",
                "§7under development.",
                "",
                "§c⚠ Coming Soon",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        inv.setItem(23, GovernmentGUIUtils.createItem(Material.ENDER_PEARL, "§b§lState Scholars Management",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7This feature is currently",
                "§7under development.",
                "",
                "§c⚠ Coming Soon",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        inv.setItem(30, GovernmentGUIUtils.createItem(Material.LODESTONE, "§e§lBorder Management",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Manage your caliphate's territory.",
                "§7• Claim & release chunks",
                "§7• Reallocate your capital",
                "§7• Toggle border visualization",
                "§7• Set a territory welcome message",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§aClick to open."));

        inv.setItem(31, GovernmentGUIUtils.createItem(Material.BELL, "§e§lEvent Management",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7This feature is currently",
                "§7under development.",
                "",
                "§c⚠ Coming Soon",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        inv.setItem(32, GovernmentGUIUtils.createItem(Material.PAPER, "§e§lDiplomacy Management",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Manage your foreign relations",
                "§7with every other nation.",
                "§7• Review each nation's current status",
                "§7• Propose Peace, Alliance, Truce or War",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§aClick to open."));

        inv.setItem(43, GovernmentGUIUtils.createItem(Material.SPECTRAL_ARROW, "§c§lBack",
                "§7Return to previous menu"));

        inv.setItem(48, GovernmentGUIUtils.createItem(Material.WRITABLE_BOOK, "§e§lRename Nation",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Click to rename your caliphate.",
                "§7You will be prompted to type",
                "§7the new name in chat.",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        inv.setItem(49, GovernmentGUIUtils.createItem(Material.COMPASS, "§e§lLeave Nation",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Click to renounce your citizenship.",
                "§c⚠ You cannot undo this action!",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        inv.setItem(50, GovernmentGUIUtils.createItem(Material.TNT_MINECART, "§e§lDisband Nation",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Click to dissolve your caliphate.",
                "§c⚠ Warning: Irreversible action!",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        player.openInventory(inv);
    }

    private ItemStack buildNationProfile(Nation nation, CaliphateGovernment cg) {
        int memberCount = nation.getMemberCount();
        int shura = cg != null ? cg.getShuraCount() : 0;
        int scholars = cg != null ? cg.getScholarCount() : 0;

        return GovernmentGUIUtils.createItem(Material.GLOW_ITEM_FRAME,
                "§2§l" + nation.getName(),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Government     : §fCaliphate",
                "§7Tag            : §f[" + nation.getTag() + "]",
                "§7Citizens       : §f" + memberCount,
                "§7Shura Council  : §f" + shura + "§8/§f" + CaliphateGovernment.MAX_SHURA,
                "§7State Scholars : §f" + scholars + "§8/§f" + CaliphateGovernment.MAX_SCHOLARS,
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§8Displays information about the caliphate");
    }
}
