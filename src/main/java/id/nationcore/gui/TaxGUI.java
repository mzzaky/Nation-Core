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
import id.nationcore.models.Treasury.Transaction;
import id.nationcore.models.Treasury.TransactionType;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

@SuppressWarnings("deprecation")
public class TaxGUI {

    private final NationCore plugin;

    public static final String TAX_MENU_TITLE = "§6§l💰 NATION TAX SYSTEM 💰";
    public static final String TAX_HISTORY_TITLE = "§6§l📜 TAX HISTORY 📜";
    public static final String TAX_DEBTORS_TITLE = "§c§l⚠ TAX DEBTORS ⚠";

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

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());

        // ── FILLER ──────────────────────────────────────────────────────────
        ItemStack filler = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        int[] fillerSlots = {0,1,2,3,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,49,50,51,52,53};
        for (int s : fillerSlots) {
            inv.setItem(s, filler);
        }

        // ── SLOT 4: Balance / Treasury Overview ─────────────────────────────
        inv.setItem(4, buildBalanceInfoItem(nation));

        // ── SLOT 21: Total Income ────────────────────────────────────────────
        inv.setItem(21, buildTotalIncomeItem(nation));

        // ── SLOT 30: Total Expenses ──────────────────────────────────────────
        inv.setItem(30, buildTotalExpensesItem(nation));

        // ── SLOT 23: Donate ──────────────────────────────────────────────────
        inv.setItem(23, buildDonateItem(nation));

        // ── SLOT 32: Transaction Logs ────────────────────────────────────────
        inv.setItem(32, buildTransactionLogsItem());

        // ── SLOT 43: Back ────────────────────────────────────────────────────
        ItemStack backItem = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName("§e§l⚐ Back to Nation Hub");
        List<String> backLore = new ArrayList<>();
        backLore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        backLore.add("§7Return to the main Republic");
        backLore.add("§7council menu.");
        backLore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        backLore.add("§eClick §7→ Main Menu");
        backMeta.setLore(backLore);
        backItem.setItemMeta(backMeta);
        inv.setItem(43, backItem);

        player.openInventory(inv);
    }

    // ── New item builders for the redesigned menu ────────────────────────────

    private ItemStack buildBalanceInfoItem(Nation nation) {
        ItemStack item = new ItemStack(Material.GLOW_ITEM_FRAME);
        ItemMeta meta = item.getItemMeta();

        if (nation == null) {
            meta.setDisplayName("§c§lTreasury Unavailable");
            List<String> lore = new ArrayList<>();
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            lore.add("§7You are not a member of any nation.");
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        }

        double balance = nation.getTreasury().getBalance();
        double totalIncome = nation.getTreasury().getTotalIncome();
        double totalExpenses = nation.getTreasury().getTotalExpenses();

        // Try to get president name
        String presidentName = "§cVacant";
        if (nation.getRepublicGovernment() != null && nation.getRepublicGovernment().hasPresident()) {
            String pName = nation.getRepublicGovernment().getPresidentName();
            presidentName = "§f" + (pName != null ? pName : "Unknown");
        }

        meta.setDisplayName("§b§l" + nation.getName() + " §8— §7State Treasury");

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7 Nation§8: §f" + nation.getName() + " §8[§f" + nation.getTag() + "§8]");
        lore.add("§7 Members§8: §f" + nation.getMemberCount());
        lore.add("§7 President§8: " + presidentName);
        lore.add("");
        lore.add("§6 Treasury Balance§8: §a$" + MessageUtils.formatNumber(balance));
        lore.add("§7 Total Income§8:    §a$" + MessageUtils.formatNumber(totalIncome));
        lore.add("§7 Total Expenses§8:  §c$" + MessageUtils.formatNumber(totalExpenses));
        lore.add("");
        lore.add("§7 System§8: §fOpen Democracy");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        meta.setLore(lore);
        item.setItemMeta(meta);
        addGlow(item);
        return item;
    }

    private ItemStack buildTotalIncomeItem(Nation nation) {
        ItemStack item = new ItemStack(Material.WARPED_WART_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a§l📈 Total Income");

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        if (nation == null) {
            lore.add("§7No nation data available.");
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        }

        double totalTaxIncome = nation.getTreasury().getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.TAX_INCOME
                        || t.getType() == TransactionType.GLOBAL_TAX_INCOME
                        || t.getType() == TransactionType.TAX_PENALTY_INCOME
                        || t.getType() == TransactionType.FINE_INCOME
                        || t.getType() == TransactionType.DONATION)
                .mapToDouble(t -> Math.max(0, t.getAmount()))
                .sum();

        lore.add("§7 Source§8: §fNation Tax & Donations");
        lore.add("§7 Cumulative Revenue§8:");
        lore.add("§8  └ §a$" + MessageUtils.formatNumber(totalTaxIncome));
        lore.add("");
        lore.add("§7 Last 3 Income Entries§8:");

        List<Transaction> incomeTransactions = nation.getTreasury().getTransactions().stream()
                .filter(t -> t.getAmount() > 0)
                .collect(java.util.stream.Collectors.toList());

        int start = Math.max(0, incomeTransactions.size() - 3);
        List<Transaction> recent = incomeTransactions.subList(start, incomeTransactions.size());
        java.util.Collections.reverse(recent);

        if (recent.isEmpty()) {
            lore.add("§8  └ §7No recent income recorded.");
        } else {
            for (Transaction tx : recent) {
                lore.add("§8  └ §a+$" + MessageUtils.formatNumber(tx.getAmount())
                        + " §8• §7" + tx.getType().getDisplayName());
            }
        }

        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildTotalExpensesItem(Nation nation) {
        ItemStack item = new ItemStack(Material.NETHER_WART_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c§l📉 Total Expenses");

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        if (nation == null) {
            lore.add("§7No nation data available.");
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        }

        lore.add("§7 Sources§8: §fSalaries, Executive Orders,");
        lore.add("§7         §fPresidential Game, etc.");
        lore.add("§7 Total Spent§8:");
        lore.add("§8  └ §c$" + MessageUtils.formatNumber(nation.getTreasury().getTotalExpenses()));
        lore.add("");
        lore.add("§7 Last 3 Expense Entries§8:");

        List<Transaction> expenseTransactions = nation.getTreasury().getTransactions().stream()
                .filter(t -> t.getAmount() < 0)
                .collect(java.util.stream.Collectors.toList());

        int start = Math.max(0, expenseTransactions.size() - 3);
        List<Transaction> recent = expenseTransactions.subList(start, expenseTransactions.size());
        java.util.Collections.reverse(recent);

        if (recent.isEmpty()) {
            lore.add("§8  └ §7No recent expenses recorded.");
        } else {
            for (Transaction tx : recent) {
                lore.add("§8  └ §c-$" + MessageUtils.formatNumber(Math.abs(tx.getAmount()))
                        + " §8• §7" + tx.getType().getDisplayName());
            }
        }

        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildDonateItem(Nation nation) {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§l❤ Donate to Treasury");

        double balance = nation != null ? nation.getTreasury().getBalance() : 0;

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Contribute your own funds to");
        lore.add("§7the nation's state treasury.");
        lore.add("");
        lore.add("§7 Treasury Balance§8: §a$" + MessageUtils.formatNumber(balance));
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§eClick §7→ Enter donation amount");
        meta.setLore(lore);
        item.setItemMeta(meta);
        addGlow(item);
        return item;
    }

    private ItemStack buildTransactionLogsItem() {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d§l📋 Transaction Logs");

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Browse the full history of");
        lore.add("§7treasury transactions including");
        lore.add("§7income, expenses, and donations.");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§eClick §7→ Open logs");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Open tax transaction history
     */
    public void openTaxHistory(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TAX_HISTORY_TITLE);
        TaxRecord record = plugin.getTaxManager().getTaxRecord();
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());

        List<TaxTransaction> recent = record.getRecentTransactions(200);
        Collections.reverse(recent);

        int slot = 0;
        for (TaxTransaction tx : recent) {
            if (slot >= 45)
                break;

            // Filter history to player's nation members only
            if (nation != null) {
                try {
                    UUID txUUID = UUID.fromString(tx.getPlayerUUID());
                    if (!nation.getMembers().containsKey(txUUID)) {
                        continue;
                    }
                } catch (Exception ignored) {}
            }

            Material mat;
            String color;
            switch (tx.getType()) {
                case TAX_PAID:
                    mat = Material.EMERALD;
                    color = "§a";
                    break;
                case LATE_PENALTY:
                    mat = Material.REDSTONE;
                    color = "§c";
                    break;
                case DEBT_PAYMENT:
                    mat = Material.DIAMOND;
                    color = "§b";
                    break;
                case PUNISHMENT_APPLIED:
                    mat = Material.TNT;
                    color = "§4";
                    break;
                case TAX_EXEMPTION:
                    mat = Material.GOLDEN_APPLE;
                    color = "§6";
                    break;
                case DEBT_FORGIVEN:
                    mat = Material.ENCHANTED_GOLDEN_APPLE;
                    color = "§d";
                    break;
                default:
                    mat = Material.PAPER;
                    color = "§7";
                    break;
            }

            ItemStack txItem = new ItemStack(mat);
            ItemMeta meta = txItem.getItemMeta();
            meta.setDisplayName(color + tx.getType().getDisplayName());

            List<String> lore = new ArrayList<>();
            lore.add("§7Player: §f" + tx.getPlayerName());
            lore.add("§7Amount: §6$" + MessageUtils.formatNumber(tx.getAmount()));
            lore.add("§7Description: §f" + tx.getDescription());
            lore.add("§7Time: §f" + MessageUtils.formatTime(
                    System.currentTimeMillis() - tx.getTimestamp()) + " ago");
            meta.setLore(lore);
            txItem.setItemMeta(meta);

            inv.setItem(slot, txItem);
            slot++;
        }

        if (slot == 0) {
            ItemStack noData = createItem(Material.GRAY_STAINED_GLASS_PANE, "§7No tax history yet");
            inv.setItem(22, noData);
        }

        // Navigation
        ItemStack backItem = createItem(Material.ARROW, "§e← Back to Tax Menu");
        inv.setItem(49, backItem);

        player.openInventory(inv);
    }

    /**
     * Open debtor list
     */
    public void openDebtorList(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TAX_DEBTORS_TITLE);
        TaxRecord record = plugin.getTaxManager().getTaxRecord();
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());

        // Sort debtors by debt amount (highest first), restricted to player's nation if applicable
        List<Map.Entry<String, PlayerTaxData>> debtors = record.getPlayerTaxData().entrySet().stream()
                .filter(e -> e.getValue().getOutstandingDebt() > 0)
                .filter(e -> {
                    if (nation == null) return false;
                    try {
                        UUID memberUUID = UUID.fromString(e.getKey());
                        return nation.getMembers().containsKey(memberUUID);
                    } catch (Exception ex) {
                        return false;
                    }
                })
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

            meta.setDisplayName("§c" + taxData.getPlayerName());

            List<String> lore = new ArrayList<>();
            lore.add("§8▬§m------------------");
            lore.add("§7Outstanding Debt: §c$" + MessageUtils.formatNumber(taxData.getOutstandingDebt()));
            lore.add("§7Missed Payments: §c" + taxData.getMissedPayments());
            lore.add("§7Total Paid: §a$" + MessageUtils.formatNumber(taxData.getTotalAmountPaid()));
            lore.add("§7Penalties Paid: §e$" + MessageUtils.formatNumber(taxData.getTotalPenaltiesPaid()));
            lore.add("§7Punishments: §4" + taxData.getPunishmentHistory().size());
            lore.add("§8▬§m------------------");
            if (taxData.isExempt()) {
                lore.add("§6★ Tax Exempt");
            }

            meta.setLore(lore);
            debtorItem.setItemMeta(meta);
            inv.setItem(slot, debtorItem);
            slot++;
        }

        double nationTotalDebt = 0;
        if (nation != null) {
            for (UUID memberUUID : nation.getMembers().keySet()) {
                PlayerTaxData memberTax = record.getPlayerTaxData(memberUUID.toString());
                if (memberTax != null) {
                    nationTotalDebt += memberTax.getOutstandingDebt();
                }
            }
        }

        if (debtors.isEmpty()) {
            ItemStack noDebtors = createItem(Material.LIME_STAINED_GLASS_PANE,
                    "§aNo debtors! Everyone is paid up.");
            inv.setItem(22, noDebtors);
        }

        // Summary (Slot 48)
        ItemStack summaryItem = createItem(Material.GOLD_INGOT, "§6§lDEBT SUMMARY",
                "§7Total Debtors: §c" + debtors.size(),
                "§7Total Debt: §c$" + MessageUtils.formatNumber(nationTotalDebt));
        inv.setItem(48, summaryItem);

        // Navigation
        ItemStack backItem = createItem(Material.ARROW, "§e← Back to Tax Menu");
        inv.setItem(49, backItem);

        player.openInventory(inv);
    }

    // === Item Creation Helpers ===

    private ItemStack createStatusItem(TaxRecord record, Nation nation) {
        boolean enabled = plugin.getTaxManager().isEnabled();

        if (nation == null) {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§c§l✖ NOT IN A NATION");
            List<String> lore = new ArrayList<>();
            lore.add("§8▬§m------------------");
            lore.add("§7You do not belong to any nation.");
            lore.add("§7Taxes only apply to members of a nation.");
            lore.add("§8▬§m------------------");
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        }

        boolean suspended = plugin.getExecutiveOrderManager().isTaxSuspended(nation);
        boolean surge = plugin.getExecutiveOrderManager().isOrderActive(nation, ExecutiveOrderType.TAX_SURGE);

        Material mat;
        String title;
        if (suspended) {
            mat = Material.LIGHT_BLUE_CONCRETE;
            title = "§b§l⚡ TAX SYSTEM - SUSPENDED";
        } else if (surge) {
            mat = Material.ORANGE_CONCRETE;
            title = "§6§l⚡ TAX SYSTEM - SURGE ACTIVE";
        } else {
            mat = enabled ? Material.BEACON : Material.BEDROCK;
            title = enabled ? "§a§l✔ TAX SYSTEM ACTIVE" : "§c§l✖ TAX SYSTEM DISABLED";
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(title);

        List<String> lore = new ArrayList<>();
        lore.add("§8▬§m------------------");
        lore.add("§7Status: " + (enabled ? "§aEnabled" : "§cDisabled"));
        if (suspended) {
            lore.add("§b§lExecutive Order: TAX SUSPENDED");
        } else if (surge) {
            lore.add("§6§lExecutive Order: TAX SURGE (5x)");
        }
        lore.add("§7Collection Cycles: §f" + record.getTotalCollectionCycles());
        lore.add("§8▬§m------------------");
        meta.setLore(lore);
        item.setItemMeta(meta);

        if (enabled || suspended || surge)
            addGlow(item);
        return item;
    }

    private ItemStack createTaxRateItem(Nation nation) {
        if (nation == null) {
            ItemStack item = new ItemStack(Material.GOLD_NUGGET);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e§l💲 TAX RATE");
            List<String> lore = new ArrayList<>();
            lore.add("§8▬§m------------------");
            lore.add("§7Base Tax Amount: §f$50.00");
            lore.add("§7Effective Rate: §7$0.00 (No Nation)");
            lore.add("§8▬§m------------------");
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        }

        boolean suspended = plugin.getExecutiveOrderManager().isTaxSuspended(nation);
        boolean surge = plugin.getExecutiveOrderManager().isOrderActive(nation, ExecutiveOrderType.TAX_SURGE);
        double multiplier = plugin.getExecutiveOrderManager().getTaxMultiplier(nation);

        ItemStack item = new ItemStack(suspended ? Material.LIGHT_BLUE_STAINED_GLASS
                : (surge ? Material.ORANGE_STAINED_GLASS : Material.GOLD_NUGGET));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§l💲 TAX RATE");

        double baseAmount = plugin.getTaxManager().getTaxAmount();
        double effectiveAmount = suspended ? 0 : baseAmount * multiplier;
        double penaltyRate = plugin.getTaxManager().getLatePenaltyRate();
        int inactiveDays = (int) plugin.getTaxManager().getInactiveDaysThreshold();

        List<String> lore = new ArrayList<>();
        lore.add("§8▬§m------------------");
        lore.add("§7Base Tax Amount: §6$" + MessageUtils.formatNumber(baseAmount));
        if (suspended) {
            lore.add("§b§lEffective Rate: $0 (SUSPENDED)");
        } else if (surge) {
            lore.add("§6§lEffective Rate: $" + MessageUtils.formatNumber(effectiveAmount) + " (5x SURGE)");
        } else {
            lore.add("§7Effective Rate: §f$" + MessageUtils.formatNumber(effectiveAmount));
        }
        lore.add("§7Collection Interval: §f24 Minecraft Hours (20m)");
        lore.add("§7Late Penalty: §c" + String.format("%.0f", penaltyRate * 100) + "% of debt");
        lore.add("§7Inactive Exempt: §f" + inactiveDays + "+ days offline");
        lore.add("§8▬§m------------------");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createExecutiveOrderTaxItem(Nation nation) {
        if (nation == null) {
            return createItem(Material.PAPER, "§7§lNo Active Tax Executive Orders",
                    "§8▬§m------------------",
                    "§7Join a nation to see active",
                    "§7presidential decrees.",
                    "§8▬§m------------------");
        }

        boolean suspended = plugin.getExecutiveOrderManager().isTaxSuspended(nation);
        boolean surge = plugin.getExecutiveOrderManager().isOrderActive(nation, ExecutiveOrderType.TAX_SURGE);

        if (suspended) {
            id.nationcore.models.ExecutiveOrder suspensionOrder = plugin.getExecutiveOrderManager()
                    .getActiveOrder(nation, ExecutiveOrderType.TAX_SUSPENSION);

            ItemStack item = new ItemStack(Material.LIGHT_BLUE_CONCRETE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§b§l⚡ EXECUTIVE ORDER: TAX SUSPENSION");

            List<String> lore = new ArrayList<>();
            lore.add("§8▬§m------------------");
            lore.add("§7§lStatus: §b§lACTIVE");
            lore.add("");
            lore.add("§7All tax collection has been");
            lore.add("§7halted by presidential decree.");
            lore.add("");
            if (suspensionOrder != null) {
                lore.add("§7Time Remaining: §f" + MessageUtils.formatTime(suspensionOrder.getRemainingTime()));
            }
            lore.add("§8▬§m------------------");
            meta.setLore(lore);
            item.setItemMeta(meta);
            addGlow(item);
            return item;

        } else if (surge) {
            id.nationcore.models.ExecutiveOrder surgeOrder = plugin.getExecutiveOrderManager()
                    .getActiveOrder(nation, ExecutiveOrderType.TAX_SURGE);
            double baseAmount = plugin.getTaxManager().getTaxAmount();

            ItemStack item = new ItemStack(Material.ORANGE_CONCRETE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§6§l⚡ EXECUTIVE ORDER: TAX SURGE");

            List<String> lore = new ArrayList<>();
            lore.add("§8▬§m------------------");
            lore.add("§7§lStatus: §6§lACTIVE");
            lore.add("");
            lore.add("§7Tax rates have been raised to");
            lore.add("§65x§7 the base rate by presidential decree.");
            lore.add("");
            lore.add("§7Base Rate: §f$" + MessageUtils.formatNumber(baseAmount));
            lore.add("§6Surge Rate: §6$" + MessageUtils.formatNumber(baseAmount * 5));
            lore.add("");
            if (surgeOrder != null) {
                lore.add("§7Time Remaining: §f" + MessageUtils.formatTime(surgeOrder.getRemainingTime()));
            }
            lore.add("§8▬§m------------------");
            meta.setLore(lore);
            item.setItemMeta(meta);
            addGlow(item);
            return item;

        } else {
            return createItem(Material.PAPER, "§7§lNo Active Tax Executive Orders",
                    "§8▬§m------------------",
                    "§7No presidential executive orders",
                    "§7are currently affecting the tax system.",
                    "§8▬§m------------------");
        }
    }

    private ItemStack createTreasuryIncomeItem(TaxRecord record, Nation nation) {
        ItemStack item = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§l🏦 TREASURY TAX INCOME");

        double nationTaxCollected = 0;
        if (nation != null) {
            nationTaxCollected = nation.getTreasury().getTransactions().stream()
                    .filter(t -> t.getType() == TransactionType.TAX_INCOME)
                    .mapToDouble(t -> Math.max(0, t.getAmount()))
                    .sum();
        }

        List<String> lore = new ArrayList<>();
        lore.add("§8▬§m------------------");
        lore.add("§7Nation Tax Revenue: §a$" + MessageUtils.formatNumber(nationTaxCollected));
        lore.add("§8▬§m------------------");
        lore.add("§7All collected tax money goes");
        lore.add("§7into your nation's treasury.");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNextCollectionItem(Nation nation) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b§l⏰ NEXT COLLECTION");

        long remaining = plugin.getTaxManager().getTimeUntilNextCollection();

        int taxable = 0;
        if (nation != null) {
            long inactiveThreshold = System.currentTimeMillis() - (plugin.getTaxManager().getInactiveDaysThreshold() * 24L * 60 * 60 * 1000);
            for (UUID memberUUID : nation.getMembers().keySet()) {
                PlayerData pd = plugin.getDataManager().getPlayerData(memberUUID);
                if (pd != null && pd.getLastSeen() >= inactiveThreshold) {
                    taxable++;
                }
            }
        }

        List<String> lore = new ArrayList<>();
        lore.add("§8▬§m------------------");
        if (remaining > 0) {
            lore.add("§7Time Remaining: §f" + MessageUtils.formatTime(remaining));
        } else {
            lore.add("§eCollection pending...");
        }
        lore.add("§7Taxable Nation Members: §f" + taxable);
        lore.add("§7Expected Nation Revenue: §6$" + MessageUtils.formatNumber(
                taxable * plugin.getTaxManager().getTaxAmount()));
        lore.add("§8▬§m------------------");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCollectionStatsItem(TaxRecord record, Nation nation) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d§l📊 COLLECTION STATISTICS");

        int nationDebtors = 0;
        double nationTotalDebt = 0;
        if (nation != null) {
            for (UUID memberUUID : nation.getMembers().keySet()) {
                PlayerTaxData memberTax = record.getPlayerTaxData(memberUUID.toString());
                if (memberTax != null && memberTax.getOutstandingDebt() > 0) {
                    nationDebtors++;
                    nationTotalDebt += memberTax.getOutstandingDebt();
                }
            }
        }

        List<String> lore = new ArrayList<>();
        lore.add("§8▬§m------------------");
        lore.add("§7Total Cycles: §f" + record.getTotalCollectionCycles());
        lore.add("§7Active Nation Debtors: §c" + nationDebtors);
        lore.add("§7Outstanding Nation Debt: §c$" + MessageUtils.formatNumber(nationTotalDebt));
        lore.add("§7Punishment Threshold: §4" +
                plugin.getTaxManager().getMaxMissedBeforePunishment() + " missed payments");
        lore.add("§8▬§m------------------");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPlayerTaxHead(Player player, PlayerTaxData taxData) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName("§b§l👤 MY TAX STATUS");

        List<String> lore = new ArrayList<>();
        lore.add("§8▬§m------------------");
        lore.add("§7Total Taxes Paid: §f" + taxData.getTotalTaxesPaid());
        lore.add("§7Total Amount Paid: §a$" + MessageUtils.formatNumber(taxData.getTotalAmountPaid()));
        lore.add("§7Missed Payments: §c" + taxData.getMissedPayments());
        lore.add("§7Outstanding Debt: " + (taxData.getOutstandingDebt() > 0
                ? "§c$" + MessageUtils.formatNumber(taxData.getOutstandingDebt())
                : "§a$0"));
        lore.add("§7Penalties Paid: §e$" + MessageUtils.formatNumber(taxData.getTotalPenaltiesPaid()));

        if (taxData.isExempt()) {
            lore.add("");
            lore.add("§6★ You are tax exempt!");
        }

        if (taxData.getLastPaymentTime() > 0) {
            lore.add("");
            lore.add("§7Last Payment: §f" + MessageUtils.formatTime(
                    System.currentTimeMillis() - taxData.getLastPaymentTime()) + " ago");
        }

        lore.add("§8▬§m------------------");
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
            meta.setDisplayName("§c§l⚠ OUTSTANDING DEBT");
            List<String> lore = new ArrayList<>();
            lore.add("§8▬§m------------------");
            lore.add("§7Debt: §c$" + MessageUtils.formatNumber(taxData.getOutstandingDebt()));
            lore.add("§7Missed Payments: §c" + taxData.getMissedPayments());
            lore.add("");
            lore.add("§cPay your debt to avoid");
            lore.add("§cfurther penalties!");
            lore.add("§8▬§m------------------");
            int threshold = plugin.getTaxManager().getMaxMissedBeforePunishment();
            lore.add("§4Punishment at " + threshold + " missed payments");
            meta.setLore(lore);
        } else {
            meta.setDisplayName("§a§l✔ NO OUTSTANDING DEBT");
            List<String> lore = new ArrayList<>();
            lore.add("§8▬§m------------------");
            lore.add("§aYou are in good standing!");
            lore.add("§7Keep paying your taxes");
            lore.add("§7to stay debt-free.");
            lore.add("§8▬§m------------------");
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
