package id.nationcore.gui.republic;

import id.nationcore.gui.NationMenuBase;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;
import id.nationcore.models.Government;
import id.nationcore.models.ExecutiveOrder.ExecutiveOrderType;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

/**
 * President's executive-order console for REPUBLIC nations. The catalogue of
 * orders shown here is driven by the {@code executive_order.president} list in
 * nations/republic.yaml (backed by order.yaml).
 */
public class RepublicExecutiveOrdersMenu extends NationMenuBase {

    public static final String TITLE = ChatColor.translateAlternateColorCodes('&',
            "&9&l⚖ &b&lRepublic Executive Orders");

    public static final int SLOT_BACK = 49;
    public static final int SLOT_CLOSE = 53; // kept for backwards compatibility in GUIListener

    public static final String POSITION_KEY = "president";

    private static final int[] FILLER_SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 50, 51, 52, 53
    };

    public static final int[] ORDER_SLOTS = {
            20, 21, 22, 23, 24,
            29, 30, 31, 32, 33
    };

    public RepublicExecutiveOrdersMenu(NationCore plugin) {
        super(plugin);
    }

    public void open(Player player, Nation nation) {
        if (nation == null) {
            player.closeInventory();
            MessageUtils.send(player, "&cYou are not in a nation.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        Government gov = nation.getRepublicGovernment();

        ItemStack filler = pane(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        for (int slot : FILLER_SLOTS) {
            inv.setItem(slot, filler);
        }

        inv.setItem(4, buildNationProfile(nation, gov));
        inv.setItem(SLOT_BACK, buildIcon(Material.SPECTRAL_ARROW, "&e&l← Back to Menu", "&7Return to main government menu"));

        boolean isPresident = gov != null && gov.hasPresident()
                && gov.getPresidentUUID().equals(player.getUniqueId());
        boolean canIssue = isPresident || player.hasPermission("nation.admin");
        String issuerLabel = "President " + presidentName(gov);

        List<ExecutiveOrderType> orders = plugin.getExecutiveOrderManager()
                .getOrdersForPosition(nation.getType(), POSITION_KEY);
        for (int i = 0; i < orders.size() && i < ORDER_SLOTS.length; i++) {
            inv.setItem(ORDER_SLOTS[i], buildExecutiveOrderCard(nation, orders.get(i), canIssue, issuerLabel, "&b"));
        }

        player.openInventory(inv);
    }

    private String presidentName(Government gov) {
        if (gov != null && gov.hasPresident()) {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(gov.getPresidentUUID());
            if (op.getName() != null) return op.getName();
        }
        return "Vacant";
    }

    private ItemStack buildNationProfile(Nation nation, Government gov) {
        int activeOrdersCount = plugin.getExecutiveOrderManager().getActiveOrders(nation).size();

        return buildIcon(Material.GLOW_ITEM_FRAME,
                "&b&lExecutive Decrees & Info",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&7Nation Name   : &f" + nation.getName() + " [" + nation.getTag() + "]",
                "&7President     : &f" + presidentName(gov),
                "&7Active Decrees: &a" + activeOrdersCount,
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&7As President, you can issue executive orders",
                "&7that affect all citizens of your nation.",
                "&7These decrees cost treasury funds and last",
                "&7for a limited duration.",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
}
