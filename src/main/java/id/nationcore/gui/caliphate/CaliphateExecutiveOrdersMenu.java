package id.nationcore.gui.caliphate;

import id.nationcore.gui.NationMenuBase;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;
import id.nationcore.models.CaliphateGovernment;
import id.nationcore.models.ExecutiveOrder;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

/**
 * Executive Orders menu for CALIPHATE nations.
 *
 * Caliphate has no minister-level decisions — only the Caliph's executive
 * orders. The standard executive-order cooldown applies (the Caliph is NOT
 * exempt; only the King is). Hence this menu is single-tab and just lays
 * out the executive-order catalogue.
 */
@SuppressWarnings("deprecation")
public class CaliphateExecutiveOrdersMenu extends NationMenuBase {

    public static final String TITLE = ChatColor.translateAlternateColorCodes('&',
            "&2&l☪ &a&lCaliph's Executive Orders");

    public static final int SLOT_BACK = 49;
    public static final int SLOT_CLOSE = 53;

    private static final int[] FILLER_SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 50, 51, 52
    };

    private static final int[] DECISION_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42
    };

    public CaliphateExecutiveOrdersMenu(NationCore plugin) {
        super(plugin);
    }

    public static ExecutiveOrder.ExecutiveOrderType getExecutiveOrderAtSlot(int slot) {
        int index = -1;
        for (int i = 0; i < DECISION_SLOTS.length; i++) {
            if (DECISION_SLOTS[i] == slot) { index = i; break; }
        }
        if (index == -1) return null;
        var types = ExecutiveOrder.ExecutiveOrderType.values();
        return index < types.length ? types[index] : null;
    }

    public void open(Player player, Nation nation) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        CaliphateGovernment cg = nation.getCaliphateGovernment();

        ItemStack filler = pane(Material.LIME_STAINED_GLASS_PANE);
        for (int slot : FILLER_SLOTS) inv.setItem(slot, filler);

        inv.setItem(4, buildNationProfile(nation, cg));
        inv.setItem(SLOT_BACK, buildIcon(Material.SPECTRAL_ARROW, "&e&l⮜ Back", "&7Return to main menu"));

        var types = ExecutiveOrder.ExecutiveOrderType.values();
        for (int i = 0; i < types.length && i < DECISION_SLOTS.length; i++) {
            inv.setItem(DECISION_SLOTS[i], buildExecutiveOrderCard(nation, cg, player, types[i]));
        }

        player.openInventory(inv);
    }

    private ItemStack buildNationProfile(Nation nation, CaliphateGovernment cg) {
        int memberCount = nation.getMemberCount();
        int shura = cg != null ? cg.getShuraCount() : 0;
        int scholars = cg != null ? cg.getScholarCount() : 0;
        return buildIcon(Material.GLOW_ITEM_FRAME,
                "&2&l" + nation.getName(),
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&7Government     : &fCaliphate",
                "&7Tag            : &f[" + nation.getTag() + "]",
                "&7Citizens       : &f" + memberCount,
                "&7Shura Council  : &f" + shura + "&8/&f" + CaliphateGovernment.MAX_SHURA,
                "&7State Scholars : &f" + scholars + "&8/&f" + CaliphateGovernment.MAX_SCHOLARS,
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&8Displays information about the caliphate");
    }

    private ItemStack buildExecutiveOrderCard(Nation nation, CaliphateGovernment cg, Player viewer,
                                              ExecutiveOrder.ExecutiveOrderType type) {
        boolean isCaliph = cg != null && cg.hasCaliph() && cg.getCaliphUUID().equals(viewer.getUniqueId());
        boolean isAdmin = viewer.hasPermission("nation.admin");
        boolean canIssue = isCaliph || isAdmin;

        boolean active = plugin.getExecutiveOrderManager().isOrderActive(nation, type);
        boolean onCooldown = plugin.getExecutiveOrderManager().isOrderOnCooldown(nation, type);
        long cooldownRemaining = onCooldown
                ? plugin.getExecutiveOrderManager().getOrderCooldownRemaining(nation, type)
                : 0;
        long cost = plugin.getConfig().getLong("executive-orders.cost", 1_000_000);
        boolean canAfford = plugin.getTreasuryManager().canAfford(nation, cost);
        ExecutiveOrder activeOrder = active
                ? plugin.getExecutiveOrderManager().getActiveOrder(nation, type) : null;

        Material material;
        String status;
        if (active) {
            material = Material.LIME_CONCRETE;
            status = "&a[ACTIVE]";
        } else if (onCooldown) {
            material = Material.RED_CONCRETE;
            status = "&c[COOLDOWN]";
        } else if (!canIssue) {
            material = Material.LIGHT_BLUE_CONCRETE;
            status = "&8[NO ACCESS]";
        } else if (!canAfford) {
            material = Material.GRAY_CONCRETE;
            status = "&8[INSUFFICIENT FUNDS]";
        } else {
            material = Material.LIME_CONCRETE_POWDER;
            status = "&a[AVAILABLE]";
        }

        String caliphName = "Vacant seat";
        if (cg != null && cg.hasCaliph()) {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(cg.getCaliphUUID());
            if (op.getName() != null) caliphName = op.getName();
        }

        List<String> lore = new ArrayList<>();
        lore.add("&7" + type.getFlavorText());
        lore.add("");
        lore.add("&7Caliph: &f" + caliphName);
        lore.add("&2&lEffects:");
        lore.add("&7" + type.getEffectDescription());
        lore.add("");
        lore.add("&7Duration: &f" + (type.getDefaultDuration() == 0
                ? "instant"
                : MessageUtils.formatTimeShort(type.getDefaultDuration())));
        lore.add("&7Cost: &a$" + MessageUtils.formatNumber(cost));
        lore.add("");

        if (active && activeOrder != null) {
            lore.add("&aRemaining time: &f" + MessageUtils.formatTime(activeOrder.getRemainingTime()));
        } else if (onCooldown) {
            lore.add("&cCooldown: &f" + MessageUtils.formatTime(cooldownRemaining));
        } else if (!canIssue) {
            lore.add("&8Only the Caliph can issue executive orders.");
        } else if (!canAfford) {
            lore.add("&cInsufficient funds in Bayt al-Mal.");
        } else {
            lore.add("&aClick &7→ Issue executive order");
        }

        return buildIcon(material, "&2&l" + type.getDisplayName() + " " + status, lore);
    }
}
