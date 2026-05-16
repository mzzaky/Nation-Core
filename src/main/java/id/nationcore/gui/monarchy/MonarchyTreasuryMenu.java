package id.nationcore.gui.monarchy;

import id.nationcore.gui.NationMenuBase;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import id.nationcore.NationCore;
import id.nationcore.models.MonarchyGovernment;
import id.nationcore.models.Nation;
import id.nationcore.models.Treasury;
import id.nationcore.utils.MessageUtils;

/**
 * Royal Treasury menu for MONARCHY nations.
 * Mirrors the Communist treasury layout but reads its state from the
 * MonarchyGovernment object so the King's tax/alms timestamps display
 * correctly.
 */
public class MonarchyTreasuryMenu extends NationMenuBase {

    public static final String TITLE = "§6§l👑 ROYAL TREASURY §8[Monarchy]";
    public static final String LOGS_TITLE = "§6§l📜 TREASURY LOGS §8[Monarchy]";

    public MonarchyTreasuryMenu(NationCore plugin) {
        super(plugin);
    }

    public void open(Player player, Nation nation) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE);
        Treasury treasury = nation.getTreasury();
        MonarchyGovernment mg = nation.getMonarchyGovernment();

        double totalSubsidi = mg != null ? mg.getTotalSubsidyPayouts() : 0.0;
        long sinceSubsidi = mg != null ? System.currentTimeMillis() - mg.getLastSubsidyDistribution() : 0;
        long sinceTax    = mg != null ? System.currentTimeMillis() - mg.getLastTaxPhase() : 0;
        long sinceAlms   = mg != null ? System.currentTimeMillis() - mg.getLastAlmsDistribution() : 0;

        inv.setItem(13, buildIcon(Material.GOLD_BLOCK,
                "§6§lRoyal Treasury Balance",
                "§7Current balance:",
                "§6$" + MessageUtils.formatNumber(treasury.getBalance()),
                "",
                "§7Total subsidies distributed:",
                "§6$" + MessageUtils.formatNumber(totalSubsidi),
                "",
                "§8Held by the Crown, managed by the Chancellor."));

        inv.setItem(20, buildIcon(Material.EMERALD_BLOCK,
                "§a§lTotal Income",
                "§7Royal tax, royal grants",
                "§7and tribute:",
                "§a$" + MessageUtils.formatNumber(treasury.getTotalIncome())));

        inv.setItem(24, buildIcon(Material.REDSTONE,
                "§c§lTotal Expenses",
                "§7Royal alms, council stipends",
                "§7and royal expenditures:",
                "§c$" + MessageUtils.formatNumber(treasury.getTotalExpenses())));

        inv.setItem(21, buildIcon(Material.BREAD,
                "§a§lRoyal Subsidy",
                "§7Last distribution:",
                "§f" + (mg != null && mg.getLastSubsidyDistribution() > 0
                        ? formatRemaining(sinceSubsidi) + " ago"
                        : "§8never"),
                "",
                "§7Royal subsidies are paid",
                "§7directly by the Crown."));

        inv.setItem(23, buildIcon(Material.EMERALD,
                "§a§lRoyal Tax",
                "§7Last tax phase:",
                "§f" + (mg != null && mg.getLastTaxPhase() > 0
                        ? formatRemaining(sinceTax) + " ago"
                        : "§8never"),
                "",
                mg != null && mg.getTaxIntensificationPhasesLeft() > 0
                        ? "§c⚡ Intensification active: §f" + mg.getTaxIntensificationPhasesLeft() + " phase(s)"
                        : "§8Intensification inactive",
                mg != null && mg.getDistributionProgramPhasesLeft() > 0
                        ? "§a✔ Royal Almsgiving: §f" + mg.getDistributionProgramPhasesLeft() + " phase(s)"
                        : ""));

        inv.setItem(22, buildIcon(Material.COOKED_BEEF,
                "§6§lRoyal Alms",
                "§7Last alms distribution:",
                "§f" + (mg != null && mg.getLastAlmsDistribution() > 0
                        ? formatRemaining(sinceAlms) + " ago"
                        : "§8never"),
                "",
                "§7Free bread for every",
                "§7subject of the Crown."));

        boolean isKing = mg != null && player.getUniqueId().equals(mg.getKingUUID());
        boolean isCouncil = mg != null && mg.getPositionByUUID(player.getUniqueId()) != null;
        boolean isAdmin = player.hasPermission("nation.admin");

        if (isKing || isCouncil || isAdmin) {
            inv.setItem(31, buildIcon(Material.BOOK,
                    "§6§lRoyal Transaction Logs",
                    "§7Treasury history — only viewable",
                    "§7by the King and the High Council.",
                    "",
                    "§eClick to open"));
        } else {
            inv.setItem(31, buildIcon(Material.WRITTEN_BOOK,
                    "§8§lTransaction Logs",
                    "§8Only the King and the High Council",
                    "§8can read royal treasury logs."));
        }

        inv.setItem(36, buildIcon(Material.ARROW,
                "§7§lBack",
                "§7Back to previous menu"));

        fillEmptySlots(inv, pane(Material.GRAY_STAINED_GLASS_PANE));
        player.openInventory(inv);
    }

    public void openTransactions(Player player, Nation nation) {
        MonarchyGovernment mg = nation.getMonarchyGovernment();
        boolean isKing    = mg != null && player.getUniqueId().equals(mg.getKingUUID());
        boolean isCouncil = mg != null && mg.getPositionByUUID(player.getUniqueId()) != null;
        boolean isAdmin   = player.hasPermission("nation.admin");

        if (!isKing && !isCouncil && !isAdmin) {
            MessageUtils.send(player,
                    "<red>Only the King and the High Council can view treasury logs.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, LOGS_TITLE);
        Treasury treasury = nation.getTreasury();
        fillTransactionSlots(inv, treasury);

        inv.setItem(49, buildIcon(Material.ARROW,
                "§7§lBack", "§7Back to Royal Treasury"));

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

            inv.setItem(slot, buildIcon(mat, "§6Transaction #" + (slot + 1), lore.toArray(new String[0])));
            slot++;
        }

        if (transactions.isEmpty()) {
            inv.setItem(22, buildIcon(Material.PAPER,
                    "§7§lNo Transactions Yet",
                    "§7The Royal Treasury has no",
                    "§7transaction records yet."));
        }
    }
}
