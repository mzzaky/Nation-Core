package id.nationcore.gui.republic;

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
import id.nationcore.models.ExecutiveOrder;
import id.nationcore.models.ExecutiveOrder.ExecutiveOrderType;
import id.nationcore.models.Government;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

public class RepublicExecutiveOrdersMenu extends NationMenuBase {

    public static final String TITLE = ChatColor.translateAlternateColorCodes('&',
            "&9&l⚖ &b&lExecutive Orders");

    private static final int[] SLOTS = { 11, 13, 15, 20, 22, 24, 29, 31, 33 };

    public static final int SLOT_BACK = 45;
    public static final int SLOT_INFO = 49;
    public static final int SLOT_CLOSE = 53;

    public RepublicExecutiveOrdersMenu(NationCore plugin) {
        super(plugin);
    }

    public static ExecutiveOrderType getOrderAtSlot(int slot) {
        ExecutiveOrderType[] types = ExecutiveOrderType.values();
        for (int i = 0; i < SLOTS.length && i < types.length; i++) {
            if (SLOTS[i] == slot) return types[i];
        }
        return null;
    }

    public void open(Player player, Nation nation) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        Government gov = nation.getRepublicGovernment();
        boolean canIssue = (gov != null && gov.hasPresident()
                && gov.getPresidentUUID().equals(player.getUniqueId()))
                || player.hasPermission("nation.admin");

        decorateChrome(inv, nation);

        ExecutiveOrderType[] types = ExecutiveOrderType.values();
        for (int i = 0; i < SLOTS.length && i < types.length; i++) {
            inv.setItem(SLOTS[i], buildOrderCard(nation, types[i], canIssue));
        }

        inv.setItem(SLOT_INFO, buildInfoCard(nation, gov, canIssue));
        inv.setItem(SLOT_BACK, buildBackButton());
        inv.setItem(SLOT_CLOSE, buildCloseButton());

        fillEmptySlots(inv, pane(Material.BLUE_STAINED_GLASS_PANE));
        player.openInventory(inv);
    }

    public void openLegacy(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        Government gov = plugin.getDataManager().getGovernment();
        boolean canIssue = (gov.hasPresident()
                && gov.getPresidentUUID().equals(player.getUniqueId()))
                || player.hasPermission("nation.admin");

        decorateChrome(inv, null);

        ExecutiveOrderType[] types = ExecutiveOrderType.values();
        for (int i = 0; i < SLOTS.length && i < types.length; i++) {
            inv.setItem(SLOTS[i], buildOrderCardLegacy(types[i], canIssue));
        }

        inv.setItem(SLOT_INFO, buildInfoCardLegacy(gov, canIssue));
        inv.setItem(SLOT_BACK, buildBackButton());
        inv.setItem(SLOT_CLOSE, buildCloseButton());

        fillEmptySlots(inv, pane(Material.BLUE_STAINED_GLASS_PANE));
        player.openInventory(inv);
    }

    private void decorateChrome(Inventory inv, Nation nation) {
        ItemStack primary = pane(Material.BLUE_STAINED_GLASS_PANE);
        ItemStack accent = pane(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemStack separator = pane(Material.CYAN_STAINED_GLASS_PANE);

        for (int i = 0; i < 9; i++) inv.setItem(i, primary);
        inv.setItem(0, accent);
        inv.setItem(8, accent);
        inv.setItem(4, buildHeader(nation));

        for (int i = 36; i < 45; i++) inv.setItem(i, separator);

        for (int i = 45; i < 54; i++) inv.setItem(i, primary);
        inv.setItem(45, accent);
        inv.setItem(53, accent);
    }

    private ItemStack buildHeader(Nation nation) {
        if (nation == null) {
            return buildIcon(Material.WRITABLE_BOOK,
                    "&b&lExecutive Orders",
                    "&7Centralized Government (legacy)",
                    "&7Compatibility mode without nation.",
                    "",
                    "&8Only the President can issue orders.");
        }
        return buildIcon(Material.WRITABLE_BOOK,
                "&b&lExecutive Orders &8• &f" + nation.getName(),
                "&7Republic &f[" + nation.getTag() + "]",
                "&7Only the President of " + nation.getName() + " can",
                "&7issue executive orders.",
                "",
                "&8Costs & cooldowns are drawn from nation treasury.");
    }

    private ItemStack buildOrderCard(Nation nation, ExecutiveOrderType type, boolean canIssue) {
        boolean active = plugin.getExecutiveOrderManager().isOrderActive(nation, type);
        boolean onCooldown = !active && plugin.getExecutiveOrderManager().isOrderOnCooldown(nation, type);
        long cost = plugin.getConfig().getLong("executive-orders.cost", 1_000_000);

        return buildOrderCardCommon(type, active, onCooldown, canIssue, cost,
                active ? plugin.getExecutiveOrderManager().getActiveOrder(nation, type) : null,
                onCooldown ? plugin.getExecutiveOrderManager().getOrderCooldownRemaining(nation, type) : 0L,
                plugin.getTreasuryManager().canAfford(nation, cost));
    }

    private ItemStack buildOrderCardLegacy(ExecutiveOrderType type, boolean canIssue) {
        boolean active = plugin.getExecutiveOrderManager().isOrderActive(type);
        boolean onCooldown = !active && plugin.getExecutiveOrderManager().isOrderOnCooldown(type);
        long cost = plugin.getConfig().getLong("executive-orders.cost", 1_000_000);

        return buildOrderCardCommon(type, active, onCooldown, canIssue, cost,
                active ? plugin.getExecutiveOrderManager().getActiveOrder(type) : null,
                onCooldown ? plugin.getExecutiveOrderManager().getOrderCooldownRemaining(type) : 0L,
                plugin.getTreasuryManager().canAfford(cost));
    }

    private ItemStack buildOrderCardCommon(ExecutiveOrderType type, boolean active, boolean onCooldown,
                                           boolean canIssue, long cost, ExecutiveOrder activeOrder,
                                           long cooldownRemaining, boolean canAfford) {
        Material material;
        String status;
        if (active) {
            material = Material.LIME_CONCRETE;
            status = "&a[ACTIVE]";
        } else if (onCooldown) {
            material = Material.RED_CONCRETE;
            status = "&c[COOLDOWN]";
        } else if (!canAfford) {
            material = Material.GRAY_CONCRETE;
            status = "&8[INSUFFICIENT FUNDS]";
        } else if (canIssue) {
            material = Material.YELLOW_CONCRETE;
            status = "&e[AVAILABLE]";
        } else {
            material = Material.LIGHT_BLUE_CONCRETE;
            status = "&b[AVAILABLE]";
        }

        List<String> lore = new ArrayList<>();
        lore.add("&7" + type.getFlavorText());
        lore.add("");
        lore.add("&6&lEffects:");
        lore.add("&7" + type.getEffectDescription());
        lore.add("");
        lore.add("&7Duration: &f" + (type.getDefaultDuration() == 0
                ? "instant"
                : MessageUtils.formatTimeShort(type.getDefaultDuration())));
        lore.add("&7Cost: &6$" + MessageUtils.formatNumber(cost));
        lore.add("");

        if (active && activeOrder != null) {
            lore.add("&aRemaining time: &f" + MessageUtils.formatTime(activeOrder.getRemainingTime()));
        } else if (onCooldown) {
            lore.add("&cCooldown: &f" + MessageUtils.formatTime(cooldownRemaining));
        } else if (!canAfford) {
            lore.add("&cInsufficient funds to issue.");
        } else if (canIssue) {
            lore.add("&aClick &7→ Issue executive order");
        } else {
            lore.add("&8Only the President can issue orders.");
        }

        return buildIcon(material, "&6&l" + type.getDisplayName() + " " + status, lore);
    }

    private ItemStack buildInfoCard(Nation nation, Government gov, boolean canIssue) {
        long cost = plugin.getConfig().getLong("executive-orders.cost", 1_000_000);
        long cooldownDays = plugin.getConfig().getLong("executive-orders.cooldown-days", 7);
        long remaining = plugin.getExecutiveOrderManager().getOrderCooldownRemaining(nation, ExecutiveOrderType.GOLDEN_AGE);
        int activeCount = plugin.getExecutiveOrderManager().getActiveOrders(nation).size();
        double balance = nation.getTreasury().getBalance();

        List<String> lore = new ArrayList<>();
        lore.add("&7Nation: &f" + nation.getName());
        lore.add("&7Treasury: &6$" + MessageUtils.formatNumber(balance));
        lore.add("&7Active orders: &f" + activeCount);
        lore.add("");
        lore.add("&7Cost per order: &6$" + MessageUtils.formatNumber(cost));
        lore.add("&7Nation cooldown: &f" + cooldownDays + " days");
        lore.add(remaining > 0
                ? "&cRemaining cooldown: &f" + MessageUtils.formatTime(remaining)
                : "&aCooldown ready.");
        lore.add("");
        lore.add(canIssue
                ? "&aYou are authorized to issue orders."
                : "&8Only the President can issue orders.");
        return buildIcon(Material.BOOK, "&e&lGovernment Information", lore);
    }

    private ItemStack buildInfoCardLegacy(Government gov, boolean canIssue) {
        long cost = plugin.getConfig().getLong("executive-orders.cost", 1_000_000);
        long cooldownDays = plugin.getConfig().getLong("executive-orders.cooldown-days", 7);
        long remaining = plugin.getExecutiveOrderManager().getOrderCooldownRemaining(ExecutiveOrderType.GOLDEN_AGE);
        int activeCount = plugin.getExecutiveOrderManager().getActiveOrders().size();
        double balance = plugin.getDataManager().getTreasury().getBalance();

        List<String> lore = new ArrayList<>();
        lore.add("&7Mode: &fLegacy (global treasury)");
        lore.add("&7Treasury: &6$" + MessageUtils.formatNumber(balance));
        lore.add("&7Active orders: &f" + activeCount);
        lore.add("");
        lore.add("&7Cost per order: &6$" + MessageUtils.formatNumber(cost));
        lore.add("&7Cooldown: &f" + cooldownDays + " days");
        lore.add(remaining > 0
                ? "&cRemaining cooldown: &f" + MessageUtils.formatTime(remaining)
                : "&aCooldown ready.");
        lore.add("");
        lore.add(canIssue
                ? "&aYou are authorized to issue orders."
                : "&8Only the President can issue orders.");
        return buildIcon(Material.BOOK, "&e&lGovernment Information", lore);
    }

    private ItemStack buildBackButton() {
        return buildIcon(Material.ARROW,
                "&e&l⮜ Back",
                "&7Return to your nation's main menu.");
    }

    private ItemStack buildCloseButton() {
        return buildIcon(Material.BARRIER,
                "&c&l✘ Close Menu",
                "&7Return to game.");
    }
}
