package id.nationcore.gui.republic;

import id.nationcore.gui.NationMenuBase;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;
import id.nationcore.models.Nation;
import id.nationcore.models.Treasury;
import id.nationcore.utils.MessageUtils;

/**
 * State Treasury menu for REPUBLIC nations.
 * Features transparency, donations, and public audit logs.
 */
public class RepublicTreasuryMenu extends NationMenuBase {

    public static final String TITLE = "§6§l⚖ STATE TREASURY §8[Republic]";
    public static final String LOGS_TITLE = "§6§l📜 TRANSACTION LOGS §8[Republic]";

    public RepublicTreasuryMenu(NationCore plugin) {
        super(plugin);
    }

    /**
     * Opens the treasury menu for a REPUBLIC nation.
     */
    public void open(Player player, Nation nation) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE);
        Treasury treasury = nation.getTreasury();

        // -- Row 1: Main Statistics --
        inv.setItem(13, buildIcon(Material.GOLD_BLOCK,
                "§6§lState Treasury Balance",
                "§7Current balance:",
                "§6$" + MessageUtils.formatNumber(treasury.getBalance()),
                "",
                "§8State treasury is transparent",
                "§8and can be audited by all citizens."));

        inv.setItem(20, buildIcon(Material.EMERALD,
                "§a§lTotal Income",
                "§7Accumulated taxes, donations,",
                "§7and other income:",
                "§a$" + MessageUtils.formatNumber(treasury.getTotalIncome())));

        inv.setItem(24, buildIcon(Material.REDSTONE,
                "§c§lTotal Expenses",
                "§7Accumulated salaries, orders,",
                "§7and operational costs:",
                "§c$" + MessageUtils.formatNumber(treasury.getTotalExpenses())));

        // -- Row 2: Actions --
        inv.setItem(22, buildIcon(Material.HOPPER,
                "§e§lDonate to Treasury",
                "§7Support the state treasury with a donation.",
                "",
                "§7Use command:",
                "§f/nc treasury donate <amount>"));

        inv.setItem(31, buildIcon(Material.BOOK,
                "§6§lTransaction Logs",
                "§7View full treasury transaction history.",
                "§7Available to all citizens",
                "§7for transparency.",
                "",
                "§eClick to open"));

        // -- Navigation --
        inv.setItem(36, buildIcon(Material.ARROW,
                "§7§lBack",
                "§7Back to previous menu"));

        fillEmptySlots(inv, pane(Material.GRAY_STAINED_GLASS_PANE));
        player.openInventory(inv);
    }

    /**
     * Opens transaction logs for a REPUBLIC nation.
     */
    public void openTransactions(Player player, Nation nation) {
        Inventory inv = Bukkit.createInventory(null, 54, LOGS_TITLE);
        Treasury treasury = nation.getTreasury();
        fillTransactionSlots(inv, treasury);
        
        inv.setItem(49, buildIcon(Material.ARROW,
                "§7§lBack", "§7Back to State Treasury"));
        
        fillEmptySlots(inv, pane(Material.GRAY_STAINED_GLASS_PANE));
        player.openInventory(inv);
    }

    /**
     * Opens the treasury menu. If the player is in a nation, it opens the nation treasury.
     * Otherwise, it falls back to the legacy global treasury.
     */
    public void open(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) {
            openLegacy(player);
            return;
        }
        open(player, nation);
    }

    /**
     * Opens the transaction logs. If the player is in a nation, it opens the nation treasury logs.
     * Otherwise, it falls back to the legacy global treasury logs.
     */
    public void openTransactions(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) {
            openTransactionsLegacy(player);
            return;
        }
        openTransactions(player, nation);
    }

    private void openLegacy(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, "§6§l TREASURY ");
        Treasury treasury = plugin.getDataManager().getTreasury();

        inv.setItem(13, buildIcon(Material.GOLD_BLOCK,
                "§6§lTreasury Balance",
                "§7" + MessageUtils.formatNumber(treasury.getBalance())));
        inv.setItem(20, buildIcon(Material.EMERALD,
                "§a§lTotal Income",
                "§7" + MessageUtils.formatNumber(treasury.getTotalIncome())));
        inv.setItem(24, buildIcon(Material.REDSTONE,
                "§c§lTotal Expenses",
                "§7" + MessageUtils.formatNumber(treasury.getTotalExpenses())));
        inv.setItem(22, buildIcon(Material.HOPPER,
                "§e§lDonate to Treasury",
                "§7Use command:",
                "§f/nc treasury donate <amount>"));
        inv.setItem(31, buildIcon(Material.BOOK,
                "§6§lTransaction History",
                "§7View full treasury",
                "§7transaction logs",
                "",
                "§eClick to view"));
        inv.setItem(36, buildIcon(Material.ARROW, "§7§lBack"));
        
        fillEmptySlots(inv, pane(Material.GRAY_STAINED_GLASS_PANE));
        player.openInventory(inv);
    }

    private void openTransactionsLegacy(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§l📜 TREASURY LOGS 📜");
        Treasury treasury = plugin.getDataManager().getTreasury();
        fillTransactionSlots(inv, treasury);
        
        inv.setItem(49, buildIcon(Material.ARROW,
                "§7§lBack", "§7Back to Treasury"));
        
        fillEmptySlots(inv, pane(Material.GRAY_STAINED_GLASS_PANE));
        player.openInventory(inv);
    }

    /** Fills slots 0–44 with transaction items. */
    private void fillTransactionSlots(Inventory inv, Treasury treasury) {
        List<Treasury.Transaction> transactions = treasury.getRecentTransactions(45);
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

            List<String> lore = new ArrayList<>();
            lore.add("§7Amount: " + prefix + MessageUtils.formatNumber(Math.abs(tx.getAmount())));
            lore.add("§7Type: §f" + tx.getType().getDisplayName());
            lore.add("§7Time: §f" + formatRemaining(System.currentTimeMillis() - tx.getTimestamp()) + " ago");

            if (tx.getRelatedPlayer() != null) {
                String playerName = Bukkit.getOfflinePlayer(tx.getRelatedPlayer()).getName();
                lore.add("§7Player: §e" + (playerName != null ? playerName : "Unknown"));
            }

            lore.add("");
            lore.add("§7" + tx.getDescription());

            inv.setItem(slot, buildIcon(mat, "§6Transaction #" + (slot + 1), lore.toArray(new String[0])));
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
