package id.nationcore.gui;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import id.nationcore.NationCore;
import id.nationcore.models.ExecutiveOrder.ExecutiveOrderType;
import id.nationcore.models.PlayerData;
import id.nationcore.models.TaxRecord;
import id.nationcore.models.TaxRecord.PlayerTaxData;
import id.nationcore.models.TaxRecord.TaxTransaction;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

@SuppressWarnings("deprecation")
public class TaxGUI {

    private final NationCore plugin;

    public static final String TAX_MENU_TITLE = "\u00a76\u00a7l\ud83d\udcb0 GLOBAL TAX SYSTEM \ud83d\udcb0";
    public static final String TAX_HISTORY_TITLE = "\u00a76\u00a7l\ud83d\udcdc TAX HISTORY \ud83d\udcdc";
    public static final String TAX_DEBTORS_TITLE = "\u00a7c\u00a7l\u26a0 TAX DEBTORS \u26a0";

    public TaxGUI(NationCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Open the main Tax GUI for a player
     */
    public void openTaxMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TAX_MENU_TITLE);
        TaxRecord record = plugin.getTaxManager().getTaxRecord();
        String uuidStr = player.getUniqueId().toString();
        PlayerTaxData playerTax = record.getOrCreatePlayerTaxData(uuidStr, player.getName());

        // === ROW 1: Tax System Overview ===

        // Tax System Status (Slot 4)
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        ItemStack statusItem = createStatusItem(record, nation);
        inv.setItem(4, statusItem);

        // === ROW 2: Tax Info ===

        // Tax Rate Info (Slot 10)
        ItemStack rateItem = createTaxRateItem(nation);
        inv.setItem(10, rateItem);

        // Treasury Income (Slot 12)
        ItemStack incomeItem = createTreasuryIncomeItem(record);
        inv.setItem(12, incomeItem);

        // Next Collection (Slot 14)
        ItemStack nextItem = createNextCollectionItem();
        inv.setItem(14, nextItem);

        // Collection Stats (Slot 16)
        ItemStack statsItem = createCollectionStatsItem(record);
        inv.setItem(16, statsItem);

        // === ROW 4: Executive Order Status ===

        // Executive Order Tax Banner (Slot 28)
        ItemStack eoItem = createExecutiveOrderTaxItem(nation);
        inv.setItem(28, eoItem);

        // === ROW 3: Player Tax Info ===

        // Player Head - My Tax Status (Slot 20)
        ItemStack playerHead = createPlayerTaxHead(player, playerTax);
        inv.setItem(20, playerHead);

        // Debt Status (Slot 22)
        ItemStack debtItem = createDebtItem(playerTax);
        inv.setItem(22, debtItem);

        // Payment History (Slot 24)
        ItemStack historyItem = createItem(Material.BOOK, "\u00a75\u00a7l\ud83d\udcdc TAX HISTORY",
                "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac",
                "\u00a77View recent tax transactions",
                "\u00a77and collection history",
                "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac",
                "\u00a7aClick to view history");
        inv.setItem(24, historyItem);

        // === ROW 4: Actions ===

        // Pay Debt Button (Slot 30) - Only show if player has debt
        if (playerTax.getOutstandingDebt() > 0) {
            ItemStack payItem = new ItemStack(Material.EMERALD_BLOCK);
            ItemMeta payMeta = payItem.getItemMeta();
            payMeta.setDisplayName("\u00a7a\u00a7l\ud83d\udcb5 PAY TAX DEBT");
            List<String> payLore = new ArrayList<>();
            payLore.add(
                    "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
            payLore.add(
                    "\u00a77Outstanding Debt: \u00a7c$" + MessageUtils.formatNumber(playerTax.getOutstandingDebt()));
            payLore.add("\u00a77Your Balance: \u00a76$" + MessageUtils.formatNumber(
                    plugin.getVaultHook().getBalance(player.getUniqueId())));
            payLore.add(
                    "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
            payLore.add("\u00a7e\u26a0 Click to pay your debt!");
            payMeta.setLore(payLore);
            payItem.setItemMeta(payMeta);
            addGlow(payItem);
            inv.setItem(30, payItem);
        }

        // Debtor List (Slot 32) - Show list of players with debts
        ItemStack debtorsItem = createItem(Material.SKELETON_SKULL, "\u00a7c\u00a7l\u26a0 TAX DEBTORS",
                "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac",
                "\u00a77Debtors: \u00a7c" + plugin.getTaxManager().getDebtorCount(),
                "\u00a77Total Debt: \u00a7c$"
                        + MessageUtils.formatNumber(plugin.getTaxManager().getTotalOutstandingDebt()),
                "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac",
                "\u00a7aClick to view debtors");
        inv.setItem(32, debtorsItem);

        // === ROW 6: Navigation ===

        // Back to Main Menu
        ItemStack backItem = createItem(Material.ARROW, "\u00a7e\u2190 Back to Main Menu");
        inv.setItem(45, backItem);

        // Refresh
        ItemStack refreshItem = createItem(Material.CLOCK, "\u00a7a\u21bb Refresh");
        inv.setItem(49, refreshItem);

        // Close
        ItemStack closeItem = createItem(Material.BARRIER, "\u00a7c\u00a7lClose Menu", "\u00a77Click to close");
        inv.setItem(53, closeItem);

        // Fill empty slots
        fillGlass(inv);

        player.openInventory(inv);
    }

    /**
     * Open tax transaction history
     */
    public void openTaxHistory(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TAX_HISTORY_TITLE);
        TaxRecord record = plugin.getTaxManager().getTaxRecord();

        List<TaxTransaction> recent = record.getRecentTransactions(45);
        Collections.reverse(recent);

        int slot = 0;
        for (TaxTransaction tx : recent) {
            if (slot >= 45)
                break;

            Material mat;
            String color;
            switch (tx.getType()) {
                case TAX_PAID:
                    mat = Material.EMERALD;
                    color = "\u00a7a";
                    break;
                case LATE_PENALTY:
                    mat = Material.REDSTONE;
                    color = "\u00a7c";
                    break;
                case DEBT_PAYMENT:
                    mat = Material.DIAMOND;
                    color = "\u00a7b";
                    break;
                case PUNISHMENT_APPLIED:
                    mat = Material.TNT;
                    color = "\u00a74";
                    break;
                case TAX_EXEMPTION:
                    mat = Material.GOLDEN_APPLE;
                    color = "\u00a76";
                    break;
                case DEBT_FORGIVEN:
                    mat = Material.ENCHANTED_GOLDEN_APPLE;
                    color = "\u00a7d";
                    break;
                default:
                    mat = Material.PAPER;
                    color = "\u00a77";
                    break;
            }

            ItemStack txItem = new ItemStack(mat);
            ItemMeta meta = txItem.getItemMeta();
            meta.setDisplayName(color + tx.getType().getDisplayName());

            List<String> lore = new ArrayList<>();
            lore.add("\u00a77Player: \u00a7f" + tx.getPlayerName());
            lore.add("\u00a77Amount: \u00a76$" + MessageUtils.formatNumber(tx.getAmount()));
            lore.add("\u00a77Description: \u00a7f" + tx.getDescription());
            lore.add("\u00a77Time: \u00a7f" + MessageUtils.formatTime(
                    System.currentTimeMillis() - tx.getTimestamp()) + " ago");
            meta.setLore(lore);
            txItem.setItemMeta(meta);

            inv.setItem(slot, txItem);
            slot++;
        }

        if (recent.isEmpty()) {
            ItemStack noData = createItem(Material.GRAY_STAINED_GLASS_PANE, "\u00a77No tax history yet");
            inv.setItem(22, noData);
        }

        // Navigation
        ItemStack backItem = createItem(Material.ARROW, "\u00a7e\u2190 Back to Tax Menu");
        inv.setItem(49, backItem);

        player.openInventory(inv);
    }

    /**
     * Open debtor list
     */
    public void openDebtorList(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TAX_DEBTORS_TITLE);
        TaxRecord record = plugin.getTaxManager().getTaxRecord();

        // Sort debtors by debt amount (highest first)
        List<Map.Entry<String, PlayerTaxData>> debtors = record.getPlayerTaxData().entrySet().stream()
                .filter(e -> e.getValue().getOutstandingDebt() > 0)
                .sorted((a, b) -> Double.compare(b.getValue().getOutstandingDebt(), a.getValue().getOutstandingDebt()))
                .toList();

        int slot = 0;
        for (Map.Entry<String, PlayerTaxData> entry : debtors) {
            if (slot >= 45)
                break;

            PlayerTaxData taxData = entry.getValue();

            ItemStack debtorItem = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) debtorItem.getItemMeta();

            try {
                UUID uuid = UUID.fromString(entry.getKey());
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
            } catch (IllegalArgumentException ignored) {
            }

            meta.setDisplayName("\u00a7c" + taxData.getPlayerName());

            List<String> lore = new ArrayList<>();
            lore.add(
                    "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
            lore.add("\u00a77Outstanding Debt: \u00a7c$" + MessageUtils.formatNumber(taxData.getOutstandingDebt()));
            lore.add("\u00a77Missed Payments: \u00a7c" + taxData.getMissedPayments());
            lore.add("\u00a77Total Paid: \u00a7a$" + MessageUtils.formatNumber(taxData.getTotalAmountPaid()));
            lore.add("\u00a77Penalties Paid: \u00a7e$" + MessageUtils.formatNumber(taxData.getTotalPenaltiesPaid()));
            lore.add("\u00a77Punishments: \u00a74" + taxData.getPunishmentHistory().size());
            lore.add(
                    "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
            if (taxData.isExempt()) {
                lore.add("\u00a76\u2605 Tax Exempt");
            }

            meta.setLore(lore);
            debtorItem.setItemMeta(meta);
            inv.setItem(slot, debtorItem);
            slot++;
        }

        if (debtors.isEmpty()) {
            ItemStack noDebtors = createItem(Material.LIME_STAINED_GLASS_PANE,
                    "\u00a7aNo debtors! Everyone is paid up.");
            inv.setItem(22, noDebtors);
        }

        // Summary (Slot 49)
        ItemStack summaryItem = createItem(Material.GOLD_INGOT, "\u00a76\u00a7lDEBT SUMMARY",
                "\u00a77Total Debtors: \u00a7c" + debtors.size(),
                "\u00a77Total Debt: \u00a7c$"
                        + MessageUtils.formatNumber(plugin.getTaxManager().getTotalOutstandingDebt()));
        inv.setItem(48, summaryItem);

        // Navigation
        ItemStack backItem = createItem(Material.ARROW, "\u00a7e\u2190 Back to Tax Menu");
        inv.setItem(49, backItem);

        player.openInventory(inv);
    }

    // === Item Creation Helpers ===

    private ItemStack createStatusItem(TaxRecord record, Nation nation) {
        boolean enabled = plugin.getTaxManager().isEnabled();
        boolean suspended = plugin.getExecutiveOrderManager().isTaxSuspended(nation);
        boolean surge = nation != null && plugin.getExecutiveOrderManager().isOrderActive(nation, ExecutiveOrderType.TAX_SURGE);

        Material mat;
        String title;
        if (suspended) {
            mat = Material.LIGHT_BLUE_CONCRETE;
            title = "\u00a7b\u00a7l\u26a1 TAX SYSTEM - SUSPENDED";
        } else if (surge) {
            mat = Material.ORANGE_CONCRETE;
            title = "\u00a76\u00a7l\u26a1 TAX SYSTEM - SURGE ACTIVE";
        } else {
            mat = enabled ? Material.BEACON : Material.BEDROCK;
            title = enabled ? "\u00a7a\u00a7l\u2714 TAX SYSTEM ACTIVE" : "\u00a7c\u00a7l\u2716 TAX SYSTEM DISABLED";
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(title);

        List<String> lore = new ArrayList<>();
        lore.add(
                "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
        lore.add("\u00a77Status: " + (enabled ? "\u00a7aEnabled" : "\u00a7cDisabled"));
        if (suspended) {
            lore.add("\u00a7b\u00a7lExecutive Order: TAX SUSPENDED");
        } else if (surge) {
            lore.add("\u00a76\u00a7lExecutive Order: TAX SURGE (5x)");
        }
        lore.add("\u00a77Collection Cycles: \u00a7f" + record.getTotalCollectionCycles());
        lore.add("\u00a77Total Tax Collected: \u00a7a$" + MessageUtils.formatNumber(record.getTotalTaxCollected()));
        lore.add("\u00a77Total Penalties: \u00a7c$" + MessageUtils.formatNumber(record.getTotalPenaltiesCollected()));
        lore.add(
                "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
        meta.setLore(lore);
        item.setItemMeta(meta);

        if (enabled || suspended || surge)
            addGlow(item);
        return item;
    }

    private ItemStack createTaxRateItem(Nation nation) {
        boolean suspended = plugin.getExecutiveOrderManager().isTaxSuspended(nation);
        boolean surge = nation != null && plugin.getExecutiveOrderManager().isOrderActive(nation, ExecutiveOrderType.TAX_SURGE);
        double multiplier = plugin.getExecutiveOrderManager().getTaxMultiplier(nation);

        ItemStack item = new ItemStack(suspended ? Material.LIGHT_BLUE_STAINED_GLASS
                : (surge ? Material.ORANGE_STAINED_GLASS : Material.GOLD_NUGGET));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7e\u00a7l\ud83d\udcb2 TAX RATE");

        double baseAmount = plugin.getTaxManager().getTaxAmount();
        double effectiveAmount = suspended ? 0 : baseAmount * multiplier;
        long intervalHours = plugin.getConfig().getLong("global-tax.collection-interval-hours", 24);
        double penaltyRate = plugin.getTaxManager().getLatePenaltyRate();
        int inactiveDays = (int) plugin.getTaxManager().getInactiveDaysThreshold();

        List<String> lore = new ArrayList<>();
        lore.add(
                "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
        lore.add("\u00a77Base Tax Amount: \u00a76$" + MessageUtils.formatNumber(baseAmount));
        if (suspended) {
            lore.add("\u00a7b\u00a7lEffective Rate: $0 (SUSPENDED)");
        } else if (surge) {
            lore.add("\u00a76\u00a7lEffective Rate: $" + MessageUtils.formatNumber(effectiveAmount) + " (5x SURGE)");
        } else {
            lore.add("\u00a77Effective Rate: \u00a7f$" + MessageUtils.formatNumber(effectiveAmount));
        }
        lore.add("\u00a77Collection Interval: \u00a7f" + intervalHours + " hours");
        lore.add("\u00a77Late Penalty: \u00a7c" + String.format("%.0f", penaltyRate * 100) + "% of debt");
        lore.add("\u00a77Inactive Exempt: \u00a7f" + inactiveDays + "+ days offline");
        lore.add(
                "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createExecutiveOrderTaxItem(Nation nation) {
        boolean suspended = plugin.getExecutiveOrderManager().isTaxSuspended(nation);
        boolean surge = nation != null && plugin.getExecutiveOrderManager().isOrderActive(nation, ExecutiveOrderType.TAX_SURGE);

        if (suspended) {
            // Tax Suspension active
            id.nationcore.models.ExecutiveOrder suspensionOrder = plugin.getExecutiveOrderManager()
                    .getActiveOrder(nation, ExecutiveOrderType.TAX_SUSPENSION);

            ItemStack item = new ItemStack(Material.LIGHT_BLUE_CONCRETE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("\u00a7b\u00a7l\u26a1 EXECUTIVE ORDER: TAX SUSPENSION");

            List<String> lore = new ArrayList<>();
            lore.add(
                    "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
            lore.add("\u00a77\u00a7lStatus: \u00a7b\u00a7lACTIVE");
            lore.add("");
            lore.add("\u00a77All tax collection has been");
            lore.add("\u00a77halted by presidential decree.");
            lore.add("");
            if (suspensionOrder != null) {
                lore.add(
                        "\u00a77Time Remaining: \u00a7f" + MessageUtils.formatTime(suspensionOrder.getRemainingTime()));
            }
            lore.add(
                    "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
            meta.setLore(lore);
            item.setItemMeta(meta);
            addGlow(item);
            return item;

        } else if (surge) {
            // Tax Surge active
            id.nationcore.models.ExecutiveOrder surgeOrder = plugin.getExecutiveOrderManager()
                    .getActiveOrder(nation, ExecutiveOrderType.TAX_SURGE);
            double baseAmount = plugin.getTaxManager().getTaxAmount();

            ItemStack item = new ItemStack(Material.ORANGE_CONCRETE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("\u00a76\u00a7l\u26a1 EXECUTIVE ORDER: TAX SURGE");

            List<String> lore = new ArrayList<>();
            lore.add(
                    "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
            lore.add("\u00a77\u00a7lStatus: \u00a76\u00a7lACTIVE");
            lore.add("");
            lore.add("\u00a77Tax rates have been raised to");
            lore.add("\u00a765x\u00a77 the base rate by presidential decree.");
            lore.add("");
            lore.add("\u00a77Base Rate: \u00a7f$" + MessageUtils.formatNumber(baseAmount));
            lore.add("\u00a76Surge Rate: \u00a76$" + MessageUtils.formatNumber(baseAmount * 5));
            lore.add("");
            if (surgeOrder != null) {
                lore.add("\u00a77Time Remaining: \u00a7f" + MessageUtils.formatTime(surgeOrder.getRemainingTime()));
            }
            lore.add(
                    "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
            meta.setLore(lore);
            item.setItemMeta(meta);
            addGlow(item);
            return item;

        } else {
            // No active tax executive orders
            return createItem(Material.PAPER, "\u00a77\u00a7lNo Active Tax Executive Orders",
                    "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac",
                    "\u00a77No presidential executive orders",
                    "\u00a77are currently affecting the tax system.",
                    "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
        }
    }

    private ItemStack createTreasuryIncomeItem(TaxRecord record) {
        ItemStack item = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a76\u00a7l\ud83c\udfe6 TREASURY TAX INCOME");

        List<String> lore = new ArrayList<>();
        lore.add(
                "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
        lore.add("\u00a77Tax Revenue: \u00a7a$" + MessageUtils.formatNumber(record.getTotalTaxCollected()));
        lore.add("\u00a77Penalty Revenue: \u00a7e$" + MessageUtils.formatNumber(record.getTotalPenaltiesCollected()));
        lore.add("\u00a77Total Revenue: \u00a76$" + MessageUtils.formatNumber(
                record.getTotalTaxCollected() + record.getTotalPenaltiesCollected()));
        lore.add(
                "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
        lore.add("\u00a77All tax goes to the");
        lore.add("\u00a77national treasury");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNextCollectionItem() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7b\u00a7l\u23f0 NEXT COLLECTION");

        long remaining = plugin.getTaxManager().getTimeUntilNextCollection();
        int taxable = plugin.getTaxManager().getTaxablePlayerCount();

        List<String> lore = new ArrayList<>();
        lore.add(
                "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
        if (remaining > 0) {
            lore.add("\u00a77Time Remaining: \u00a7f" + MessageUtils.formatTime(remaining));
        } else {
            lore.add("\u00a7eCollection pending...");
        }
        lore.add("\u00a77Taxable Players: \u00a7f" + taxable);
        lore.add("\u00a77Expected Revenue: \u00a76$" + MessageUtils.formatNumber(
                taxable * plugin.getTaxManager().getTaxAmount()));
        lore.add(
                "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCollectionStatsItem(TaxRecord record) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7d\u00a7l\ud83d\udcca COLLECTION STATISTICS");

        List<String> lore = new ArrayList<>();
        lore.add(
                "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
        lore.add("\u00a77Total Cycles: \u00a7f" + record.getTotalCollectionCycles());
        lore.add("\u00a77Active Debtors: \u00a7c" + plugin.getTaxManager().getDebtorCount());
        lore.add("\u00a77Outstanding Debt: \u00a7c$" + MessageUtils.formatNumber(
                plugin.getTaxManager().getTotalOutstandingDebt()));
        lore.add("\u00a77Punishment Threshold: \u00a74" +
                plugin.getTaxManager().getMaxMissedBeforePunishment() + " missed payments");
        lore.add(
                "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPlayerTaxHead(Player player, PlayerTaxData taxData) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName("\u00a7b\u00a7l\ud83d\udc64 MY TAX STATUS");

        List<String> lore = new ArrayList<>();
        lore.add(
                "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
        lore.add("\u00a77Total Taxes Paid: \u00a7f" + taxData.getTotalTaxesPaid());
        lore.add("\u00a77Total Amount Paid: \u00a7a$" + MessageUtils.formatNumber(taxData.getTotalAmountPaid()));
        lore.add("\u00a77Missed Payments: \u00a7c" + taxData.getMissedPayments());
        lore.add("\u00a77Outstanding Debt: " + (taxData.getOutstandingDebt() > 0
                ? "\u00a7c$" + MessageUtils.formatNumber(taxData.getOutstandingDebt())
                : "\u00a7a$0"));
        lore.add("\u00a77Penalties Paid: \u00a7e$" + MessageUtils.formatNumber(taxData.getTotalPenaltiesPaid()));

        if (taxData.isExempt()) {
            lore.add("");
            lore.add("\u00a76\u2605 You are tax exempt!");
        }

        if (taxData.getLastPaymentTime() > 0) {
            lore.add("");
            lore.add("\u00a77Last Payment: \u00a7f" + MessageUtils.formatTime(
                    System.currentTimeMillis() - taxData.getLastPaymentTime()) + " ago");
        }

        lore.add(
                "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
        meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createDebtItem(PlayerTaxData taxData) {
        boolean hasDebt = taxData.getOutstandingDebt() > 0;

        Material mat = hasDebt ? Material.RED_CONCRETE : Material.LIME_CONCRETE;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if (hasDebt) {
            meta.setDisplayName("\u00a7c\u00a7l\u26a0 OUTSTANDING DEBT");
            List<String> lore = new ArrayList<>();
            lore.add(
                    "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
            lore.add("\u00a77Debt: \u00a7c$" + MessageUtils.formatNumber(taxData.getOutstandingDebt()));
            lore.add("\u00a77Missed Payments: \u00a7c" + taxData.getMissedPayments());
            lore.add("");
            lore.add("\u00a7cPay your debt to avoid");
            lore.add("\u00a7cfurther penalties!");
            lore.add(
                    "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
            int threshold = plugin.getTaxManager().getMaxMissedBeforePunishment();
            lore.add("\u00a74Punishment at " + threshold + " missed payments");
            meta.setLore(lore);
        } else {
            meta.setDisplayName("\u00a7a\u00a7l\u2714 NO OUTSTANDING DEBT");
            List<String> lore = new ArrayList<>();
            lore.add(
                    "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
            lore.add("\u00a7aYou are in good standing!");
            lore.add("\u00a77Keep paying your taxes");
            lore.add("\u00a77to stay debt-free.");
            lore.add(
                    "\u00a78\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac");
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
        return item;
    }

    // === Utility Methods ===

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) {
            meta.setLore(Arrays.asList(lore));
        }
        item.setItemMeta(meta);
        return item;
    }

    private void addGlow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    private void fillGlass(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);

        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                inv.setItem(i, glass);
            }
        }
    }
}
