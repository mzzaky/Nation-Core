package id.nationcore.gui.communist;

import id.nationcore.gui.NationMenuBase;
import id.nationcore.models.CommunistGovernment;
import id.nationcore.models.Nation;
import id.nationcore.models.Treasury;
import id.nationcore.models.Treasury.Transaction;
import id.nationcore.models.Treasury.TransactionType;
import id.nationcore.utils.MessageUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Party Treasury menu for COMMUNIST nations.
 * Features centralized funds, subsidy tracking, and restricted logs.
 */
@SuppressWarnings("deprecation")
public class CommunistTreasuryMenu extends NationMenuBase {

    public static final String TITLE = "§4§l☭ PARTY TREASURY §8[Communist]";
    public static final String LOGS_TITLE = "§4§l📜 TRANSACTION LOGS §8[Communist]";

    public CommunistTreasuryMenu(id.nationcore.NationCore plugin) {
        super(plugin);
    }

    // ═══════════════════════════════════════════════════════════════
    // Main menu — 54 slots
    // ═══════════════════════════════════════════════════════════════

    /**
     * Opens the treasury menu for a COMMUNIST nation.
     */
    public void open(Player player, Nation nation) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // ── FILLER ─────────────────────────────────────────────────
        ItemStack filler = makeFiller();
        int[] fillerSlots = {0,1,2,3,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,49,50,51,52,53};
        for (int s : fillerSlots) inv.setItem(s, filler);

        // ── SLOT 4: Balance / Treasury Overview ─────────────────────
        inv.setItem(4, buildBalanceInfoItem(nation));

        // ── SLOT 20: Daily Subsidy ───────────────────────────────────
        inv.setItem(20, buildDailySubsidyItem(nation));

        // ── SLOT 21: Total Income ────────────────────────────────────
        inv.setItem(21, buildTotalIncomeItem(nation));

        // ── SLOT 22: Food Subsidy ────────────────────────────────────
        inv.setItem(22, buildFoodSubsidyItem(nation));

        // ── SLOT 23: Donate / Contribute ─────────────────────────────
        inv.setItem(23, buildDonateItem(nation));

        // ── SLOT 24: Progressive Tax ─────────────────────────────────
        inv.setItem(24, buildProgressiveTaxItem(nation));

        // ── SLOT 30: Total Expenses ──────────────────────────────────
        inv.setItem(30, buildTotalExpensesItem(nation));

        // ── SLOT 32: Transaction Logs ────────────────────────────────
        inv.setItem(32, buildTransactionLogsItem(player, nation));

        // ── SLOT 43: Back ────────────────────────────────────────────
        inv.setItem(43, buildBackItem());

        player.openInventory(inv);
    }

    /**
     * Opens the treasury menu. Falls back to search if player has a nation.
     */
    public void open(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation != null) {
            open(player, nation);
        } else {
            player.sendMessage("§cYou are not part of a Communist nation.");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Transaction Logs menu — 54 slots
    // ═══════════════════════════════════════════════════════════════

    /**
     * Opens transaction logs for a COMMUNIST nation.
     */
    public void openTransactions(Player player, Nation nation) {
        CommunistGovernment cg = nation.getCommunistGovernment();
        boolean isSekjen    = cg != null && player.getUniqueId().equals(cg.getSecretaryGeneralUUID());
        boolean isPolitburo = cg != null && cg.getPositionByUUID(player.getUniqueId()) != null;
        boolean isAdmin     = player.hasPermission("nation.admin");

        if (!isSekjen && !isPolitburo && !isAdmin) {
            MessageUtils.send(player,
                    "<red>Only the General Secretary and Politburo can view transaction logs.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, LOGS_TITLE);
        fillTransactionSlots(inv, nation.getTreasury());

        // Back button at slot 49 (bottom row center)
        ItemStack back = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName("§e§l⚐ Back to Treasury");
        List<String> bl = new ArrayList<>();
        bl.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        bl.add("§7Return to the Party Treasury menu.");
        bl.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        bl.add("§eClick §7→ Treasury");
        bm.setLore(bl);
        back.setItemMeta(bm);
        inv.setItem(49, back);

        fillEmptySlots(inv, pane(Material.RED_STAINED_GLASS_PANE));
        player.openInventory(inv);
    }

    /**
     * Opens transaction logs. Falls back to search if player has a nation.
     */
    public void openTransactions(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation != null) {
            openTransactions(player, nation);
        } else {
            player.sendMessage("§cYou are not part of a Communist nation.");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Item builders
    // ═══════════════════════════════════════════════════════════════

    private ItemStack buildBalanceInfoItem(Nation nation) {
        ItemStack item = new ItemStack(Material.GLOW_ITEM_FRAME);
        ItemMeta meta = item.getItemMeta();

        double balance      = nation.getTreasury().getBalance();
        double totalIncome  = nation.getTreasury().getTotalIncome();
        double totalExpenses = nation.getTreasury().getTotalExpenses();

        CommunistGovernment cg = nation.getCommunistGovernment();
        double totalSubsidi = cg != null ? cg.getTotalSubsidyPayouts() : 0.0;

        String secretaryName = "§cVacant";
        if (cg != null && cg.hasSecretaryGeneral()) {
            String sn = cg.getSecretaryGeneralName();
            secretaryName = "§f" + (sn != null ? sn : "Unknown");
        }

        meta.setDisplayName("§4§l" + nation.getName() + " §8— §7Party Treasury");

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7 Nation§8:     §f" + nation.getName() + " §8[§f" + nation.getTag() + "§8]");
        lore.add("§7 Members§8:    §f" + nation.getMemberCount());
        lore.add("§7 Gen. Secretary§8: " + secretaryName);
        lore.add("");
        lore.add("§6 Party Balance§8:  §a$" + MessageUtils.formatNumber(balance));
        lore.add("§7 Total Subsidies§8: §a$" + MessageUtils.formatNumber(totalSubsidi));
        lore.add("§7 Total Income§8:    §a$" + MessageUtils.formatNumber(totalIncome));
        lore.add("§7 Total Expenses§8:  §c$" + MessageUtils.formatNumber(totalExpenses));
        lore.add("");
        lore.add("§7 The treasury represents the collective");
        lore.add("§7 wealth of the proletariat, managed by");
        lore.add("§7 the Politburo for public welfare.");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        meta.setLore(lore);
        item.setItemMeta(meta);
        addGlow(item);
        return item;
    }

    private ItemStack buildTotalIncomeItem(Nation nation) {
        ItemStack item = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a§l📈 Party Income");

        double total = nation.getTreasury().getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.TAX_INCOME
                        || t.getType() == TransactionType.GLOBAL_TAX_INCOME
                        || t.getType() == TransactionType.TAX_PENALTY_INCOME
                        || t.getType() == TransactionType.FINE_INCOME
                        || t.getType() == TransactionType.DONATION
                        || t.getType() == TransactionType.TERM_START_FUND
                        || t.getType() == TransactionType.VOTER_REWARD
                        || t.getType() == TransactionType.DEPOSIT_REFUND)
                .mapToDouble(t -> Math.max(0, t.getAmount()))
                .sum();

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7 Sources§8: §fProgressive Tax, Nationalization,");
        lore.add("§7          §fand Proletariat Contributions.");
        lore.add("§7 Cumulative Revenue§8:");
        lore.add("§8  └ §a$" + MessageUtils.formatNumber(total));
        lore.add("");
        lore.add("§7 Last 3 Income Entries§8:");

        List<Transaction> incomes = nation.getTreasury().getTransactions().stream()
                .filter(t -> t.getAmount() > 0)
                .collect(Collectors.toList());
        int start = Math.max(0, incomes.size() - 3);
        List<Transaction> recent = new ArrayList<>(incomes.subList(start, incomes.size()));
        Collections.reverse(recent);

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
        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c§l📉 Party Expenses");

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7 Sources§8: §fDaily Subsidies, Politburo Salaries,");
        lore.add("§7          §fWelfare Programs, etc.");
        lore.add("§7 Total Spent§8:");
        lore.add("§8  └ §c$" + MessageUtils.formatNumber(nation.getTreasury().getTotalExpenses()));
        lore.add("");
        lore.add("§7 Last 3 Expense Entries§8:");

        List<Transaction> expenses = nation.getTreasury().getTransactions().stream()
                .filter(t -> t.getAmount() < 0)
                .collect(Collectors.toList());
        int start = Math.max(0, expenses.size() - 3);
        List<Transaction> recent = new ArrayList<>(expenses.subList(start, expenses.size()));
        Collections.reverse(recent);

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
        meta.setDisplayName("§e§l☭ Contribute to Party");

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Contribute your private funds to");
        lore.add("§7the collective Party state treasury.");
        lore.add("");
        lore.add("§7 Party Balance§8: §a$" + MessageUtils.formatNumber(nation.getTreasury().getBalance()));
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§eClick §7→ Enter contribution amount");
        meta.setLore(lore);
        item.setItemMeta(meta);
        addGlow(item);
        return item;
    }

    private ItemStack buildDailySubsidyItem(Nation nation) {
        ItemStack item = new ItemStack(Material.BREAD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a§l🍞 Daily Subsidy");

        CommunistGovernment cg = nation.getCommunistGovernment();
        long sinceSubsidi = cg != null ? System.currentTimeMillis() - cg.getLastSubsidyDistribution() : 0;
        String lastSubsidy = (cg != null && cg.getLastSubsidyDistribution() > 0)
                ? formatRemaining(sinceSubsidi) + " ago"
                : "never";

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7 Last distribution: §f" + lastSubsidy);
        lore.add("");
        lore.add("§7 Subsidies are automatically distributed");
        lore.add("§7 to all registered members of the Party.");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildFoodSubsidyItem(Nation nation) {
        ItemStack item = new ItemStack(Material.COOKED_BEEF);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§l🍲 Food Subsidy");

        CommunistGovernment cg = nation.getCommunistGovernment();
        long sinceFood = cg != null ? System.currentTimeMillis() - cg.getLastFreeFoodDistribution() : 0;
        String lastFood = (cg != null && cg.getLastFreeFoodDistribution() > 0)
                ? formatRemaining(sinceFood) + " ago"
                : "never";

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7 Last food distribution: §f" + lastFood);
        lore.add("");
        lore.add("§7 Free food distributions are processed");
        lore.add("§7 regularly to feed the working class.");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildProgressiveTaxItem(Nation nation) {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a§l⚡ Progressive Tax");

        CommunistGovernment cg = nation.getCommunistGovernment();
        long sinceTax = cg != null ? System.currentTimeMillis() - cg.getLastTaxPhase() : 0;
        String lastTax = (cg != null && cg.getLastTaxPhase() > 0)
                ? formatRemaining(sinceTax) + " ago"
                : "never";

        String intensification = (cg != null && cg.getTaxIntensificationPhasesLeft() > 0)
                ? "§c⚡ Intensification active: §f" + cg.getTaxIntensificationPhasesLeft() + " phase(s)"
                : "§8Intensification inactive";
        String exemption = (cg != null && cg.getDistributionProgramPhasesLeft() > 0)
                ? "§a✔ Tax exemption: §f" + cg.getDistributionProgramPhasesLeft() + " phase(s)"
                : "§8Exemption inactive";

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7 Last tax phase: §f" + lastTax);
        lore.add("");
        lore.add("§7 Status:");
        lore.add("§8  ├ " + intensification);
        lore.add("§8  └ " + exemption);
        lore.add("");
        lore.add("§7 Taxes are collected progressively to fund");
        lore.add("§7 state operations and welfare subsidies.");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildTransactionLogsItem(Player player, Nation nation) {
        CommunistGovernment cg = nation.getCommunistGovernment();
        boolean isSekjen  = cg != null && player.getUniqueId().equals(cg.getSecretaryGeneralUUID());
        boolean isPolitburo = cg != null && cg.getPositionByUUID(player.getUniqueId()) != null;
        boolean isAdmin   = player.hasPermission("nation.admin");

        if (isSekjen || isPolitburo || isAdmin) {
            ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§d§l📋 Party Transaction Logs");

            List<String> lore = new ArrayList<>();
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            lore.add("§7Browse the full history of party");
            lore.add("§7treasury transactions. Restricted to");
            lore.add("§7Secretary General and Politburo.");
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            lore.add("§eClick §7→ Open logs");
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        } else {
            ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§8§l🔒 Restricted Logs");

            List<String> lore = new ArrayList<>();
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            lore.add("§7Only the General Secretary and");
            lore.add("§7Politburo members can access");
            lore.add("§7the Party state transaction logs.");
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        }
    }

    private ItemStack buildBackItem() {
        ItemStack item = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§l⚐ Back to Soviet Council");

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Return to the main Communist");
        lore.add("§7Soviet council menu.");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§eClick §7→ Soviet Council");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeFiller() {
        ItemStack filler = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);
        return filler;
    }

    private void addGlow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    // ═══════════════════════════════════════════════════════════════
    // Transaction log slot filler
    // ═══════════════════════════════════════════════════════════════

    /** Fills slots 0–44 with transaction items. Slot 45–53 left for navigation. */
    private void fillTransactionSlots(Inventory inv, Treasury treasury) {
        List<Treasury.Transaction> transactions = treasury.getRecentTransactions(45);
        // Show most recent first
        Collections.reverse(transactions);
        int slot = 0;

        for (Treasury.Transaction tx : transactions) {
            if (slot >= 45) break;

            boolean isDeposit = tx.getType().name().contains("INCOME")
                    || tx.getType().name().contains("DEPOSIT")
                    || tx.getType().name().contains("DONATION")
                    || tx.getType().name().contains("FUND")
                    || tx.getType().name().contains("REFUND");

            Material mat = isDeposit ? Material.LIME_DYE : Material.RED_DYE;
            String prefix = isDeposit ? "§a+" : "§c-";
            String sign = isDeposit ? "§a" : "§c";

            List<String> lore = new ArrayList<>();
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            lore.add("§7 Amount§8: " + sign + prefix + MessageUtils.formatNumber(Math.abs(tx.getAmount())));
            lore.add("§7 Type§8:   §f" + tx.getType().getDisplayName());
            lore.add("§7 Time§8:   §f" + formatRemaining(System.currentTimeMillis() - tx.getTimestamp()) + " ago");
            if (tx.getRelatedPlayer() != null) {
                String pName = Bukkit.getOfflinePlayer(tx.getRelatedPlayer()).getName();
                lore.add("§7 Player§8: §e" + (pName != null ? pName : "Unknown"));
            }
            if (tx.getDescription() != null && !tx.getDescription().isBlank()) {
                lore.add("");
                lore.add("§7 " + tx.getDescription());
            }
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

            String title = (isDeposit ? "§a§l+ " : "§c§l- ") + prefix
                    + "$" + MessageUtils.formatNumber(Math.abs(tx.getAmount()))
                    + " §8— §7" + tx.getType().getDisplayName();

            inv.setItem(slot, buildIcon(mat, title, lore.toArray(new String[0])));
            slot++;
        }

        if (transactions.isEmpty()) {
            inv.setItem(22, buildIcon(Material.PAPER,
                    "§7§lNo Transactions Yet",
                    "§7Treasury has no",
                    "§7transaction records yet."));
        }
    }
}
