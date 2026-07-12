package id.nationcore.gui.caliphate;

import id.nationcore.gui.NationMenuBase;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;
import id.nationcore.models.CaliphateGovernment;
import id.nationcore.models.ExecutiveOrder.ExecutiveOrderType;
import id.nationcore.models.Nation;

/**
 * Caliph's executive-order console for CALIPHATE nations.
 *
 * The Caliphate has no ministers — only the Caliph issues executive orders.
 * This menu now uses the same flat card layout as the Republic and Communist
 * consoles, driven by the {@code executive_order.caliph} list in
 * nations/caliphate.yaml (backed by order.yaml). The standard per-order
 * cooldown applies (only the King is exempt).
 */
public class CaliphateExecutiveOrdersMenu extends NationMenuBase {

    public static final String TITLE = ChatColor.translateAlternateColorCodes('&',
            "&2&l☪ &a&lCaliph's Executive Orders");

    public static final int SLOT_BACK = 49;
    public static final int SLOT_CLOSE = 53;

    public static final String POSITION_KEY = "caliph";

    private static final int[] FILLER_SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 50, 51, 52, 53
    };

    public static final int[] ORDER_SLOTS = {
            20, 21, 22, 23, 24,
            29, 30, 31, 32, 33
    };

    public CaliphateExecutiveOrdersMenu(NationCore plugin) {
        super(plugin);
    }

    public void open(Player player, Nation nation) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        CaliphateGovernment cg = nation.getCaliphateGovernment();

        ItemStack filler = pane(Material.LIME_STAINED_GLASS_PANE);
        for (int slot : FILLER_SLOTS) {
            inv.setItem(slot, filler);
        }

        inv.setItem(4, buildNationProfile(nation, cg));
        inv.setItem(SLOT_BACK, buildIcon(Material.SPECTRAL_ARROW, "&e&l← Back to Menu", "&7Return to main menu"));

        boolean isCaliph = cg != null && cg.hasCaliph() && cg.getCaliphUUID().equals(player.getUniqueId());
        boolean canIssue = isCaliph || player.hasPermission("nation.admin");
        String issuerLabel = "Caliph " + caliphName(cg);

        List<ExecutiveOrderType> orders = plugin.getExecutiveOrderManager()
                .getOrdersForPosition(nation.getType(), POSITION_KEY);
        for (int i = 0; i < orders.size() && i < ORDER_SLOTS.length; i++) {
            inv.setItem(ORDER_SLOTS[i], buildExecutiveOrderCard(nation, orders.get(i), canIssue, issuerLabel, "&2"));
        }

        player.openInventory(inv);
    }

    private String caliphName(CaliphateGovernment cg) {
        if (cg != null && cg.hasCaliph()) {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(cg.getCaliphUUID());
            if (op.getName() != null) return op.getName();
        }
        return "Vacant seat";
    }

    private ItemStack buildNationProfile(Nation nation, CaliphateGovernment cg) {
        int memberCount = nation.getMemberCount();
        int shura = cg != null ? cg.getShuraCount() : 0;
        int scholars = cg != null ? cg.getScholarCount() : 0;
        int activeOrdersCount = plugin.getExecutiveOrderManager().getActiveOrders(nation).size();
        return buildIcon(Material.GLOW_ITEM_FRAME,
                "&2&l" + nation.getName(),
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&7Government     : &fCaliphate",
                "&7Tag            : &f[" + nation.getTag() + "]",
                "&7Caliph         : &f" + caliphName(cg),
                "&7Citizens       : &f" + memberCount,
                "&7Shura Council  : &f" + shura + "&8/&f" + CaliphateGovernment.MAX_SHURA,
                "&7State Scholars : &f" + scholars + "&8/&f" + CaliphateGovernment.MAX_SCHOLARS,
                "&7Active Decrees : &a" + activeOrdersCount,
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&7Only the Caliph may issue executive orders.");
    }
}
