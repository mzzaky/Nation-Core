package id.nationcore.gui.caliphate;

import id.nationcore.gui.NationMenuBase;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import id.nationcore.NationCore;
import id.nationcore.models.CaliphateGovernment;
import id.nationcore.models.Nation;
import id.nationcore.models.Treasury;
import id.nationcore.utils.MessageUtils;

/**
 * Bayt al-Mal (treasury) menu for CALIPHATE nations.
 *
 * Mirrors the Communist treasury layout but reads its state from the
 * CaliphateGovernment object so the Caliph's tax/zakat timestamps display
 * correctly, with terminology adapted to the caliphate concept.
 */
public class CaliphateTreasuryMenu extends NationMenuBase {

    public static final String TITLE = "§2§l☪ BAYT AL-MAL §8[Caliphate]";
    public static final String LOGS_TITLE = "§2§l📜 TREASURY LOGS §8[Caliphate]";

    public CaliphateTreasuryMenu(NationCore plugin) {
        super(plugin);
    }

    public void open(Player player, Nation nation) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE);
        Treasury treasury = nation.getTreasury();
        CaliphateGovernment cg = nation.getCaliphateGovernment();

        double totalSubsidi = cg != null ? cg.getTotalSubsidyPayouts() : 0.0;
        long sinceSubsidi = cg != null ? System.currentTimeMillis() - cg.getLastSubsidyDistribution() : 0;
        long sinceTax    = cg != null ? System.currentTimeMillis() - cg.getLastTaxPhase() : 0;
        long sinceZakat  = cg != null ? System.currentTimeMillis() - cg.getLastZakatDistribution() : 0;

        inv.setItem(13, buildIcon(Material.EMERALD_BLOCK,
                "§2§lBayt al-Mal Balance",
                "§7Current balance:",
                "§a$" + MessageUtils.formatNumber(treasury.getBalance()),
                "",
                "§7Total Zakat distributed:",
                "§a$" + MessageUtils.formatNumber(totalSubsidi),
                "",
                "§8Held in trust for the Caliph and the people."));

        inv.setItem(20, buildIcon(Material.GOLD_INGOT,
                "§a§lTotal Income",
                "§7Jizya tax, charitable income",
                "§7and tribute:",
                "§a$" + MessageUtils.formatNumber(treasury.getTotalIncome())));

        inv.setItem(24, buildIcon(Material.REDSTONE,
                "§c§lTotal Expenses",
                "§7Zakat distributions and",
                "§7state expenditures:",
                "§c$" + MessageUtils.formatNumber(treasury.getTotalExpenses())));

        inv.setItem(21, buildIcon(Material.BREAD,
                "§a§lBayt al-Mal Subsidy",
                "§7Last distribution:",
                "§f" + (cg != null && cg.getLastSubsidyDistribution() > 0
                        ? formatRemaining(sinceSubsidi) + " ago"
                        : "§8never"),
                "",
                "§7Subsidies are paid directly",
                "§7from Bayt al-Mal."));

        inv.setItem(23, buildIcon(Material.EMERALD,
                "§a§lJizya Tax",
                "§7Last tax phase:",
                "§f" + (cg != null && cg.getLastTaxPhase() > 0
                        ? formatRemaining(sinceTax) + " ago"
                        : "§8never"),
                "",
                cg != null && cg.getTaxLevyPhasesLeft() > 0
                        ? "§c⚖ Special Levy active: §f" + cg.getTaxLevyPhasesLeft() + " phase(s)"
                        : "§8Special Levy inactive",
                cg != null && cg.getTaxReliefPhasesLeft() > 0
                        ? "§a✔ Zakat Relief: §f" + cg.getTaxReliefPhasesLeft() + " phase(s)"
                        : ""));

        inv.setItem(22, buildIcon(Material.COOKED_BEEF,
                "§2§lZakat Distribution",
                "§7Last Zakat distribution:",
                "§f" + (cg != null && cg.getLastZakatDistribution() > 0
                        ? formatRemaining(sinceZakat) + " ago"
                        : "§8never"),
                "",
                "§7Free bread for every",
                "§7citizen of the caliphate."));

        boolean isCaliph = cg != null && player.getUniqueId().equals(cg.getCaliphUUID());
        boolean isShura  = cg != null && cg.isShuraMember(player.getUniqueId());
        boolean isScholar = cg != null && cg.isScholar(player.getUniqueId());
        boolean isAdmin  = player.hasPermission("nation.admin");
        boolean canViewLogs = isCaliph || isShura || isScholar || isAdmin;

        if (canViewLogs) {
            inv.setItem(31, buildIcon(Material.BOOK,
                    "§2§lTreasury Logs",
                    "§7Treasury history — viewable",
                    "§7by the Caliph, Shura Council,",
                    "§7and State Scholars.",
                    "",
                    "§eClick to open"));
        } else {
            inv.setItem(31, buildIcon(Material.WRITTEN_BOOK,
                    "§8§lTransaction Logs",
                    "§8Only the Caliph, Shura Council, and",
                    "§8State Scholars can read treasury logs."));
        }

        inv.setItem(36, buildIcon(Material.ARROW,
                "§7§lBack",
                "§7Back to previous menu"));

        fillEmptySlots(inv, pane(Material.GRAY_STAINED_GLASS_PANE));
        player.openInventory(inv);
    }

    public void openTransactions(Player player, Nation nation) {
        CaliphateGovernment cg = nation.getCaliphateGovernment();
        boolean isCaliph = cg != null && player.getUniqueId().equals(cg.getCaliphUUID());
        boolean isShura  = cg != null && cg.isShuraMember(player.getUniqueId());
        boolean isScholar = cg != null && cg.isScholar(player.getUniqueId());
        boolean isAdmin  = player.hasPermission("nation.admin");

        if (!isCaliph && !isShura && !isScholar && !isAdmin) {
            MessageUtils.send(player,
                    "<red>Only the Caliph, Shura Council, and State Scholars can view treasury logs.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, LOGS_TITLE);
        Treasury treasury = nation.getTreasury();
        fillTransactionSlots(inv, treasury);

        inv.setItem(49, buildIcon(Material.ARROW,
                "§7§lBack", "§7Back to Bayt al-Mal"));

        fillEmptySlots(inv, pane(Material.GRAY_STAINED_GLASS_PANE));
        player.openInventory(inv);
    }

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

            inv.setItem(slot, buildIcon(mat, "§2Transaction #" + (slot + 1), lore.toArray(new String[0])));
            slot++;
        }

        if (transactions.isEmpty()) {
            inv.setItem(22, buildIcon(Material.PAPER,
                    "§7§lNo Transactions Yet",
                    "§7Bayt al-Mal has no",
                    "§7transaction records yet."));
        }
    }
}
