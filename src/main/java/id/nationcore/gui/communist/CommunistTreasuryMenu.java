package id.nationcore.gui.communist;

import id.nationcore.gui.NationMenuBase;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import id.nationcore.NationCore;
import id.nationcore.models.CommunistGovernment;
import id.nationcore.models.Nation;
import id.nationcore.models.Treasury;
import id.nationcore.utils.MessageUtils;

/**
 * Party Treasury menu for COMMUNIST nations.
 * Features centralized funds, subsidy tracking, and restricted logs.
 */
public class CommunistTreasuryMenu extends NationMenuBase {

    public static final String TITLE = "§4§l☭ PARTY TREASURY §8[Communist]";
    public static final String LOGS_TITLE = "§4§l📜 TRANSACTION LOGS §8[Communist]";

    public CommunistTreasuryMenu(NationCore plugin) {
        super(plugin);
    }

    /**
     * Opens the treasury menu for a COMMUNIST nation.
     */
    public void open(Player player, Nation nation) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE);
        Treasury treasury = nation.getTreasury();
        CommunistGovernment cg = nation.getCommunistGovernment();

        double totalSubsidi = cg != null ? cg.getTotalSubsidyPayouts() : 0.0;
        long sinceSubsidi = cg != null ? System.currentTimeMillis() - cg.getLastSubsidyDistribution() : 0;
        long sinceTax    = cg != null ? System.currentTimeMillis() - cg.getLastTaxPhase() : 0;
        long sinceFood   = cg != null ? System.currentTimeMillis() - cg.getLastFreeFoodDistribution() : 0;

        // -- Treasury Balance --
        inv.setItem(13, buildIcon(Material.GOLD_BLOCK,
                "§6§lParty Treasury Balance",
                "§7Current balance:",
                "§6$" + MessageUtils.formatNumber(treasury.getBalance()),
                "",
                "§7Total subsidies distributed:",
                "§6$" + MessageUtils.formatNumber(totalSubsidi),
                "",
                "§8Managed by the Politburo Finance Minister."));

        // -- Income / Expenses --
        inv.setItem(20, buildIcon(Material.EMERALD_BLOCK,
                "§a§lTotal Income",
                "§7Progressive tax, nationalization,",
                "§7and member contributions:",
                "§a$" + MessageUtils.formatNumber(treasury.getTotalIncome())));

        inv.setItem(24, buildIcon(Material.REDSTONE,
                "§c§lTotal Expenses",
                "§7Daily subsidies, Politburo salaries,",
                "§7and party operational costs:",
                "§c$" + MessageUtils.formatNumber(treasury.getTotalExpenses())));

        // -- Daily Subsidy --
        inv.setItem(21, buildIcon(Material.BREAD,
                "§a§lDaily Subsidy",
                "§7Last distribution:",
                "§f" + (cg != null && cg.getLastSubsidyDistribution() > 0
                        ? formatRemaining(sinceSubsidi) + " ago"
                        : "§8never"),
                "",
                "§7Subsidies are automatically distributed",
                "§7to all nation members."));

        // -- Progressive Tax Phase --
        inv.setItem(23, buildIcon(Material.EMERALD,
                "§a§lProgressive Tax",
                "§7Last tax phase:",
                "§f" + (cg != null && cg.getLastTaxPhase() > 0
                        ? formatRemaining(sinceTax) + " ago"
                        : "§8never"),
                "",
                cg != null && cg.getTaxIntensificationPhasesLeft() > 0
                        ? "§c⚡ Intensification active: §f" + cg.getTaxIntensificationPhasesLeft() + " phase(s)"
                        : "§8Intensification inactive",
                cg != null && cg.getDistributionProgramPhasesLeft() > 0
                        ? "§a✔ Tax exemption: §f" + cg.getDistributionProgramPhasesLeft() + " phase(s)"
                        : ""));

        // -- Food Subsidy (Free Food) --
        inv.setItem(22, buildIcon(Material.COOKED_BEEF,
                "§6§lFood Subsidy",
                "§7Last food distribution:",
                "§f" + (cg != null && cg.getLastFreeFoodDistribution() > 0
                        ? formatRemaining(sinceFood) + " ago"
                        : "§8never"),
                "",
                "§7Free food for all",
                "§7party state members."));

        // -- Transaction Logs (General Secretary / Politburo / Admin only) --
        boolean isSekjen  = cg != null && player.getUniqueId().equals(cg.getSecretaryGeneralUUID());
        boolean isPolitburo = cg != null && cg.getPositionByUUID(player.getUniqueId()) != null;
        boolean isAdmin   = player.hasPermission("nation.admin");

        if (isSekjen || isPolitburo || isAdmin) {
            inv.setItem(31, buildIcon(Material.BOOK,
                    "§6§lParty Transaction Logs",
                    "§7Treasury history — only viewable",
                    "§7by the General Secretary and Politburo.",
                    "",
                    "§eClick to open"));
        } else {
            inv.setItem(31, buildIcon(Material.WRITTEN_BOOK,
                    "§8§lTransaction Logs",
                    "§8Only the General Secretary",
                    "§8and Politburo can",
                    "§8access treasury transaction logs."));
        }

        // -- Navigation --
        inv.setItem(36, buildIcon(Material.ARROW,
                "§7§lBack",
                "§7Back to previous menu"));

        fillEmptySlots(inv, pane(Material.GRAY_STAINED_GLASS_PANE));
        player.openInventory(inv);
    }

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
        Treasury treasury = nation.getTreasury();
        fillTransactionSlots(inv, treasury);
        
        inv.setItem(49, buildIcon(Material.ARROW,
                "§7§lBack", "§7Back to Party Treasury"));
        
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
