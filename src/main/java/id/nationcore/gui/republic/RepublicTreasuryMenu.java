package id.nationcore.gui.republic;

import id.nationcore.gui.NationMenuBase;
import id.nationcore.models.Treasury.TransactionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import id.nationcore.NationCore;
import id.nationcore.models.Nation;
import id.nationcore.models.Treasury;
import id.nationcore.models.Treasury.Transaction;
import id.nationcore.utils.MessageUtils;

/**
 * State Treasury menu for REPUBLIC nations.
 * Features transparency, donations, and public audit logs.
 */
@SuppressWarnings("deprecation")
public class RepublicTreasuryMenu extends NationMenuBase {

    public static final String TITLE    = "§6§l⚖ STATE TREASURY §8[Republic]";
    public static final String LOGS_TITLE = "§6§l📜 TRANSACTION LOGS §8[Republic]";

    public RepublicTreasuryMenu(NationCore plugin) {
        super(plugin);
    }

    // ═══════════════════════════════════════════════════════════════
    // Main menu — 54 slots
    // ═══════════════════════════════════════════════════════════════

    /**
     * Opens the treasury menu for a REPUBLIC nation.
     */
    public void open(Player player, Nation nation) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // ── FILLER ─────────────────────────────────────────────────
        ItemStack filler = makeFiller();
        int[] fillerSlots = {0,1,2,3,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,49,50,51,52,53};
        for (int s : fillerSlots) inv.setItem(s, filler);

        // ── SLOT 4: Balance / Treasury Overview ─────────────────────
        inv.setItem(4, buildBalanceInfoItem(nation));

        // ── SLOT 21: Total Income ────────────────────────────────────
        inv.setItem(21, buildTotalIncomeItem(nation));

        // ── SLOT 23: Donate ──────────────────────────────────────────
        inv.setItem(23, buildDonateItem(nation));

        // ── SLOT 30: Total Expenses ──────────────────────────────────
        inv.setItem(30, buildTotalExpensesItem(nation));

        // ── SLOT 32: Transaction Logs ────────────────────────────────
        inv.setItem(32, buildTransactionLogsItem());

        // ── SLOT 43: Back ────────────────────────────────────────────
        inv.setItem(43, buildBackItem());

        player.openInventory(inv);
    }

    /**
     * Opens the treasury menu. Falls back to legacy if player has no nation.
     */
    public void open(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) {
            openLegacy(player);
            return;
        }
        open(player, nation);
    }

    // ═══════════════════════════════════════════════════════════════
    // Transaction Logs menu — 54 slots
    // ═══════════════════════════════════════════════════════════════

    /**
     * Opens transaction logs for a REPUBLIC nation.
     */
    public void openTransactions(Player player, Nation nation) {
        Inventory inv = Bukkit.createInventory(null, 54, LOGS_TITLE);
        fillTransactionSlots(inv, nation.getTreasury());

        // Back button at slot 49 (bottom row center)
        ItemStack back = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName("§e§l⚐ Back to Treasury");
        List<String> bl = new ArrayList<>();
        bl.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        bl.add("§7Return to the State Treasury menu.");
        bl.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        bl.add("§eClick §7→ Treasury");
        bm.setLore(bl);
        back.setItemMeta(bm);
        inv.setItem(49, back);

        player.openInventory(inv);
    }

    /**
     * Opens transaction logs. Falls back to legacy if player has no nation.
     */
    public void openTransactions(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) {
            openTransactionsLegacy(player);
            return;
        }
        openTransactions(player, nation);
    }

    // ═══════════════════════════════════════════════════════════════
    // Legacy fallbacks (no-nation players)
    // ═══════════════════════════════════════════════════════════════

    private void openLegacy(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§l TREASURY ");
        Treasury treasury = plugin.getDataManager().getTreasury();

        ItemStack filler = makeFiller();
        int[] fillerSlots = {0,1,2,3,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,49,50,51,52,53};
        for (int s : fillerSlots) inv.setItem(s, filler);

        inv.setItem(4,  buildBalanceInfoItemLegacy(treasury));
        inv.setItem(21, buildTotalIncomeItemLegacy(treasury));
        inv.setItem(23, buildDonateItemLegacy(treasury));
        inv.setItem(30, buildTotalExpensesItemLegacy(treasury));
        inv.setItem(32, buildTransactionLogsItem());
        inv.setItem(43, buildBackItem());

        player.openInventory(inv);
    }

    private void openTransactionsLegacy(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§l📜 TREASURY LOGS 📜");
        fillTransactionSlots(inv, plugin.getDataManager().getTreasury());

        ItemStack back = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName("§e§l⚐ Back to Treasury");
        bm.setLore(List.of("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬", "§eClick §7→ Treasury"));
        back.setItemMeta(bm);
        inv.setItem(49, back);

        player.openInventory(inv);
    }

    // ═══════════════════════════════════════════════════════════════
    // Item builders — Nation variant
    // ═══════════════════════════════════════════════════════════════

    private ItemStack buildBalanceInfoItem(Nation nation) {
        ItemStack item = new ItemStack(Material.GLOW_ITEM_FRAME);
        ItemMeta meta = item.getItemMeta();

        double balance      = nation.getTreasury().getBalance();
        double totalIncome  = nation.getTreasury().getTotalIncome();
        double totalExpenses = nation.getTreasury().getTotalExpenses();

        String presidentName = "§cVacant";
        if (nation.getRepublicGovernment() != null && nation.getRepublicGovernment().hasPresident()) {
            String pn = nation.getRepublicGovernment().getPresidentName();
            presidentName = "§f" + (pn != null ? pn : "Unknown");
        }

        meta.setDisplayName("§b§l" + nation.getName() + " §8— §7State Treasury");

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7 Nation§8:     §f" + nation.getName() + " §8[§f" + nation.getTag() + "§8]");
        lore.add("§7 Members§8:    §f" + nation.getMemberCount());
        lore.add("§7 President§8:  " + presidentName);
        lore.add("");
        lore.add("§6 Treasury Balance§8: §a$" + MessageUtils.formatNumber(balance));
        lore.add("§7 Total Income§8:    §a$" + MessageUtils.formatNumber(totalIncome));
        lore.add("§7 Total Expenses§8:  §c$" + MessageUtils.formatNumber(totalExpenses));
        lore.add("");
        lore.add("§7 The treasury is open and transparent");
        lore.add("§7 and can be audited by all citizens.");
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
        lore.add("§7 Sources§8: §fTaxes, Donations, Fines, etc.");
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
        ItemStack item = new ItemStack(Material.NETHER_WART_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c§l📉 Total Expenses");

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7 Sources§8: §fSalaries, Executive Orders,");
        lore.add("§7         §fPresidential Game, etc.");
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
        meta.setDisplayName("§e§l❤ Donate to Treasury");

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Contribute your own funds to");
        lore.add("§7the nation's state treasury.");
        lore.add("");
        lore.add("§7 Treasury Balance§8: §a$" + MessageUtils.formatNumber(nation.getTreasury().getBalance()));
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§eClick §7→ Enter donation amount");
        meta.setLore(lore);
        item.setItemMeta(meta);
        addGlow(item);
        return item;
    }

    // ═══════════════════════════════════════════════════════════════
    // Item builders — Legacy variant
    // ═══════════════════════════════════════════════════════════════

    private ItemStack buildBalanceInfoItemLegacy(Treasury treasury) {
        ItemStack item = new ItemStack(Material.GLOW_ITEM_FRAME);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b§lGlobal Treasury");

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§6 Treasury Balance§8: §a$" + MessageUtils.formatNumber(treasury.getBalance()));
        lore.add("§7 Total Income§8:    §a$" + MessageUtils.formatNumber(treasury.getTotalIncome()));
        lore.add("§7 Total Expenses§8:  §c$" + MessageUtils.formatNumber(treasury.getTotalExpenses()));
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        meta.setLore(lore);
        item.setItemMeta(meta);
        addGlow(item);
        return item;
    }

    private ItemStack buildTotalIncomeItemLegacy(Treasury treasury) {
        ItemStack item = new ItemStack(Material.WARPED_WART_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a§l📈 Total Income");

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7 Cumulative Revenue§8:");
        lore.add("§8  └ §a$" + MessageUtils.formatNumber(treasury.getTotalIncome()));

        List<Transaction> incomes = treasury.getTransactions().stream()
                .filter(t -> t.getAmount() > 0).collect(Collectors.toList());
        int start = Math.max(0, incomes.size() - 3);
        List<Transaction> recent = new ArrayList<>(incomes.subList(start, incomes.size()));
        Collections.reverse(recent);

        lore.add("");
        lore.add("§7 Last 3 Income Entries§8:");
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

    private ItemStack buildTotalExpensesItemLegacy(Treasury treasury) {
        ItemStack item = new ItemStack(Material.NETHER_WART_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c§l📉 Total Expenses");

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7 Total Spent§8:");
        lore.add("§8  └ §c$" + MessageUtils.formatNumber(treasury.getTotalExpenses()));

        List<Transaction> expenses = treasury.getTransactions().stream()
                .filter(t -> t.getAmount() < 0).collect(Collectors.toList());
        int start = Math.max(0, expenses.size() - 3);
        List<Transaction> recent = new ArrayList<>(expenses.subList(start, expenses.size()));
        Collections.reverse(recent);

        lore.add("");
        lore.add("§7 Last 3 Expense Entries§8:");
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

    private ItemStack buildDonateItemLegacy(Treasury treasury) {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§l❤ Donate to Treasury");

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Contribute your own funds to");
        lore.add("§7the state treasury.");
        lore.add("");
        lore.add("§7 Treasury Balance§8: §a$" + MessageUtils.formatNumber(treasury.getBalance()));
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§eClick §7→ Enter donation amount");
        meta.setLore(lore);
        item.setItemMeta(meta);
        addGlow(item);
        return item;
    }

    // ═══════════════════════════════════════════════════════════════
    // Shared item builders
    // ═══════════════════════════════════════════════════════════════

    private ItemStack buildTransactionLogsItem() {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d§l📋 Transaction Logs");

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Browse the full history of");
        lore.add("§7treasury transactions including");
        lore.add("§7income, expenses, and donations.");
        lore.add("§7Available to all citizens");
        lore.add("§7for transparency.");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§eClick §7→ Open logs");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildBackItem() {
        ItemStack item = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§l⚐ Back to Nation Hub");

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Return to the main Republic");
        lore.add("§7council menu.");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§eClick §7→ Main Menu");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeFiller() {
        ItemStack filler = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
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

            boolean isDeposit = tx.getAmount() > 0;
            Material mat    = isDeposit ? Material.LIME_DYE : Material.RED_DYE;
            String   prefix = isDeposit ? "§a+" : "§c-";
            String   sign   = isDeposit ? "§a" : "§c";

            List<String> lore = new ArrayList<>();
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            lore.add("§7 Amount§8: " + sign + "$" + MessageUtils.formatNumber(Math.abs(tx.getAmount())));
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
