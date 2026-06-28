package id.nationcore.gui.communist;

import id.nationcore.gui.NationMenuBase;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;
import id.nationcore.models.CommunistGovernment;
import id.nationcore.models.ExecutiveOrder;
import id.nationcore.models.ExecutiveOrder.ExecutiveOrderType;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

public class CommunistLeaderOrdersMenu extends NationMenuBase {

    public static final String TITLE = ChatColor.translateAlternateColorCodes('&',
            "&c&l☭ &4&lCommunist Executive Orders");

    public static final int SLOT_BACK = 49;
    public static final int SLOT_CLOSE = 53;

    private static final int[] FILLER_SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 50, 51, 52, 53
    };

    private static final int[] ORDER_SLOTS = {
            20, 21, 22, 23, 24,
            29, 30, 31, 32, 33
    };

    public CommunistLeaderOrdersMenu(NationCore plugin) {
        super(plugin);
    }

    public static ExecutiveOrderType getExecutiveOrderAtSlot(Player player, int slot) {
        int index = -1;
        for (int i = 0; i < ORDER_SLOTS.length; i++) {
            if (ORDER_SLOTS[i] == slot) {
                index = i;
                break;
            }
        }
        if (index == -1) return null;

        ExecutiveOrderType[] types = ExecutiveOrderType.values();
        if (index < types.length) {
            return types[index];
        }
        return null;
    }

    public void open(Player player, Nation nation) {
        if (nation == null) {
            player.closeInventory();
            MessageUtils.send(player, "&cYou are not in a nation.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        CommunistGovernment cg = nation.getCommunistGovernment();

        // 1. Filler
        ItemStack filler = pane(Material.RED_STAINED_GLASS_PANE);
        for (int slot : FILLER_SLOTS) {
            inv.setItem(slot, filler);
        }

        // 3. Information (slot 4)
        inv.setItem(4, buildNationProfile(nation, cg));

        // 2. Back button (slot 49)
        inv.setItem(SLOT_BACK, buildIcon(Material.SPECTRAL_ARROW, "&e&l← Back to Menu", "&7Return to main government menu"));

        // 4. Executive Orders
        ExecutiveOrderType[] types = ExecutiveOrderType.values();
        for (int i = 0; i < types.length; i++) {
            if (i < ORDER_SLOTS.length) {
                inv.setItem(ORDER_SLOTS[i], buildExecutiveOrderCard(nation, cg, player, types[i]));
            }
        }

        player.openInventory(inv);
    }

    private ItemStack buildNationProfile(Nation nation, CommunistGovernment cg) {
        int activeOrdersCount = plugin.getExecutiveOrderManager().getActiveOrders(nation).size();
        
        String leaderName = "Vacant";
        if (cg != null && cg.hasSecretaryGeneral()) {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(cg.getSecretaryGeneralUUID());
            if (op.getName() != null) {
                leaderName = op.getName();
            }
        }

        return buildIcon(Material.GLOW_ITEM_FRAME,
                "&c&lExecutive Decrees & Info",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&7Nation Name   : &f" + nation.getName() + " [" + nation.getTag() + "]",
                "&7Secretary Gen : &f" + leaderName,
                "&7Active Decrees: &a" + activeOrdersCount,
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&7As Secretary General, you can issue executive orders",
                "&7that affect all citizens of your nation.",
                "&7These decrees cost treasury funds and last",
                "&7for a limited duration.",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    private ItemStack buildExecutiveOrderCard(Nation nation, CommunistGovernment cg, Player viewer, ExecutiveOrderType type) {
        boolean isSekjen = cg != null && cg.hasSecretaryGeneral()
                && cg.getSecretaryGeneralUUID().equals(viewer.getUniqueId());
        boolean isAdmin = viewer.hasPermission("nation.admin");
        boolean canIssue = isSekjen || isAdmin;

        boolean active = plugin.getExecutiveOrderManager().isOrderActive(nation, type);
        boolean onCooldown = !active && plugin.getExecutiveOrderManager().isOrderOnCooldown(nation, type);
        long cost = plugin.getConfig().getLong("executive-orders.cost", 1_000_000);
        long cooldownRemaining = onCooldown ? plugin.getExecutiveOrderManager().getOrderCooldownRemaining(nation, type) : 0L;
        boolean canAfford = plugin.getTreasuryManager().canAfford(nation, cost);
        id.nationcore.models.ExecutiveOrder activeOrder = active ? plugin.getExecutiveOrderManager().getActiveOrder(nation, type) : null;

        Material material;
        String status;
        if (active) {
            material = Material.ENCHANTED_BOOK;
            status = "&a[ACTIVE]";
        } else if (onCooldown) {
            material = Material.BOOK;
            status = "&c[COOLDOWN]";
        } else if (!canIssue) {
            material = Material.BOOK;
            status = "&8[NO ACCESS]";
        } else if (!canAfford) {
            material = Material.BOOK;
            status = "&8[INSUFFICIENT FUNDS]";
        } else {
            material = Material.WRITABLE_BOOK;
            status = "&e[AVAILABLE]";
        }

        String leaderName = "Vacant";
        if (cg != null && cg.hasSecretaryGeneral()) {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(cg.getSecretaryGeneralUUID());
            if (op.getName() != null) {
                leaderName = op.getName();
            }
        }

        List<String> lore = new ArrayList<>();
        lore.add("§8" + "▬".repeat(30));
        lore.add("§7\"" + type.getFlavorText() + "\"");
        lore.add("§8" + "▬".repeat(30));
        lore.add("§7Issuer: §fSecretary General " + leaderName);
        lore.add("§7Cost: §6$" + MessageUtils.formatNumber(cost));
        lore.add("§7Duration: §b" + (type.getDefaultDuration() == 0
                ? "Instant"
                : MessageUtils.formatTimeShort(type.getDefaultDuration())));
        lore.add("§8" + "▬".repeat(30));
        lore.add("§e§lDECREE EFFECTS:");
        lore.add("§7" + type.getEffectDescription());
        lore.add("§8" + "▬".repeat(30));

        if (active && activeOrder != null) {
            lore.add("§a✔ §lORDER ACTIVE");
            lore.add("§7Time Remaining: §a" + MessageUtils.formatTime(activeOrder.getRemainingTime()));
        } else if (onCooldown) {
            lore.add("§c⏳ §lON COOLDOWN");
            lore.add("§7Available in: §c" + MessageUtils.formatTime(cooldownRemaining));
        } else if (!canIssue) {
            lore.add("§c✖ §lNO AUTHORIZATION");
            lore.add("§7Only the Secretary General can issue this.");
        } else if (!canAfford) {
            lore.add("§c✖ §lINSUFFICIENT FUNDS");
            lore.add("§7Nation treasury requires §6$" + MessageUtils.formatNumber(cost));
        } else {
            lore.add("§a⚡ §lREADY TO ISSUE");
            lore.add("§eClick to authorize this decree.");
        }
        lore.add("§8" + "▬".repeat(30));

        ItemStack item = buildIcon(material, "&6&l" + type.getDisplayName() + " " + status, lore);
        
        if (active && material == Material.ENCHANTED_BOOK) {
            item = glowing(item);
        }

        return item;
    }
}
