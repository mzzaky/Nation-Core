package id.nationcore.gui.communist;

import id.nationcore.gui.GovernmentGUIUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;
import id.nationcore.models.CommunistGovernment;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

public class CommunistGovernmentGUI {

    private final NationCore plugin;
    public static final String TITLE = "§c§lCOMMUNIST GOVERNMENT";

    private static final int[] FILLER_SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 51, 52, 53
    };

    public CommunistGovernmentGUI(NationCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Nation nation) {
        if (nation == null) return;
        
        CommunistGovernment cg = nation.getCommunistGovernment();
        if (cg == null) return;

        boolean isSekjen = cg.hasSecretaryGeneral() && cg.getSecretaryGeneralUUID().equals(player.getUniqueId());
        boolean isPolitburo = cg.getPolitburoMemberByUUID(player.getUniqueId()) != null;

        if (!isSekjen && !isPolitburo) {
            MessageUtils.send(player, "§cOnly the Secretary General and Politburo can open the Government GUI.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        ItemStack filler = GovernmentGUIUtils.createItem(Material.RED_STAINED_GLASS_PANE, " ");
        for (int slot : FILLER_SLOTS) {
            inv.setItem(slot, filler);
        }

        inv.setItem(4, buildNationProfile(nation, cg));

        inv.setItem(21, GovernmentGUIUtils.createItem(Material.ARMS_UP_POTTERY_SHERD, "§e§lCabinet Member Management",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7This feature is currently",
                "§7under development.",
                "",
                "§c⚠ Coming Soon",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        inv.setItem(22, GovernmentGUIUtils.createItem(Material.MOURNER_POTTERY_SHERD, "§e§lNation Member Management",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7View and manage all nation members.",
                "§7• Kick members",
                "§7• Promote to Party",
                "§7• Appoint to Politburo",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§eClick to open."));

        inv.setItem(23, GovernmentGUIUtils.createItem(Material.SHEAF_POTTERY_SHERD, "§e§lParty Member Management",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7This feature is currently",
                "§7under development.",
                "",
                "§c⚠ Coming Soon",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        inv.setItem(30, GovernmentGUIUtils.createItem(Material.EXPLORER_POTTERY_SHERD, "§e§lBorder Management",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7This feature is currently",
                "§7under development.",
                "",
                "§c⚠ Coming Soon",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        inv.setItem(31, GovernmentGUIUtils.createItem(Material.EXPLORER_POTTERY_SHERD, "§e§lEvent Management",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7This feature is currently",
                "§7under development.",
                "",
                "§c⚠ Coming Soon",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        inv.setItem(32, GovernmentGUIUtils.createItem(Material.EXPLORER_POTTERY_SHERD, "§e§lDiplomacy Management",
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
                "§7Click to rename your nation.",
                "§7You will be prompted to type",
                "§7the new name in chat.",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        inv.setItem(49, GovernmentGUIUtils.createItem(Material.COMPASS, "§e§lLeave Nation",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Click to leave your nation.",
                "§c⚠ You cannot undo this action!",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        inv.setItem(50, GovernmentGUIUtils.createItem(Material.TNT_MINECART, "§e§lDisband Nation",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Click to disband your nation.",
                "§c⚠ Warning: Irreversible action!",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        player.openInventory(inv);
    }

    private ItemStack buildNationProfile(Nation nation, CommunistGovernment cg) {
        int memberCount = nation.getMemberCount();
        int partyCount = cg != null ? cg.getPartyMemberCount() : 0;

        return GovernmentGUIUtils.createItem(Material.GLOW_ITEM_FRAME,
                "§c§l" + nation.getName(),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Government : §fCommunist",
                "§7Tag        : §f[" + nation.getTag() + "]",
                "§7Members    : §f" + memberCount,
                "§7Party      : §f" + partyCount,
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§8Displays information about the nation");
    }
}
