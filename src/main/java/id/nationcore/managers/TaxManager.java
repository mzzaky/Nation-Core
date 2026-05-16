package id.nationcore.managers;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import id.nationcore.NationCore;
import id.nationcore.models.PlayerData;
import id.nationcore.models.TaxRecord;
import id.nationcore.models.TaxRecord.PlayerTaxData;
import id.nationcore.models.TaxRecord.TaxTransaction;
import id.nationcore.models.TaxRecord.TaxTransactionType;
import id.nationcore.models.Treasury.TransactionType;
import id.nationcore.utils.MessageUtils;

public class TaxManager {

    private final NationCore plugin;
    private int taskId = -1;

    public TaxManager(NationCore plugin) {
        this.plugin = plugin;
    }

    public TaxRecord getTaxRecord() {
        return plugin.getDataManager().getTaxRecord();
    }

    /**
     * Start the scheduled tax collection task
     */
    public void startTaxScheduler() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        // Check every minute if tax collection is due
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::checkTaxCollection, 20L * 60, 20L * 60).getTaskId();
    }

    /**
     * Stop the scheduled tax collection task
     */
    public void stopTaxScheduler() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    /**
     * Check if it's time to collect taxes
     */
    public void checkTaxCollection() {
        if (!isEnabled()) return;

        TaxRecord record = getTaxRecord();
        long now = System.currentTimeMillis();
        long interval = getCollectionIntervalMillis();

        // First-time initialization
        if (record.getLastCollectionTime() == 0) {
            record.setLastCollectionTime(now);
            return;
        }

        long timeSinceLastCollection = now - record.getLastCollectionTime();

        if (timeSinceLastCollection >= interval) {
            collectTaxes();
        }
    }

    /**
     * Perform tax collection for all eligible players
     */
    public void collectTaxes() {
        if (!isEnabled()) return;

        TaxRecord record = getTaxRecord();
        double taxAmount = getTaxAmount();
        long inactiveDays = getInactiveDaysThreshold();
        long inactiveThreshold = System.currentTimeMillis() - (inactiveDays * 24L * 60 * 60 * 1000);
        double penaltyRate = getLatePenaltyRate();
        int maxMissedBeforePunishment = getMaxMissedBeforePunishment();

        Collection<PlayerData> allPlayers = plugin.getDataManager().getAllPlayerData();
        int taxedCount = 0;
        int penalizedCount = 0;
        int exemptCount = 0;
        double totalCollected = 0;

        for (PlayerData playerData : allPlayers) {
            UUID playerUUID = playerData.getUuid();
            String uuidStr = playerUUID.toString();

            // Skip players who have been offline too long
            if (playerData.getLastSeen() < inactiveThreshold) {
                continue;
            }

            PlayerTaxData taxData = record.getOrCreatePlayerTaxData(uuidStr, playerData.getName());
            taxData.setPlayerName(playerData.getName());

            // Skip exempt players
            if (taxData.isExempt()) {
                exemptCount++;
                record.addTransaction(new TaxTransaction(uuidStr, playerData.getName(),
                        TaxTransactionType.TAX_EXEMPTION, 0, "Player is tax exempt"));
                continue;
            }

            // Calculate total owed (tax + outstanding debt + late penalty if applicable)
            double totalOwed = taxAmount;

            // Add late penalty if they have outstanding debt
            if (taxData.getOutstandingDebt() > 0) {
                double penalty = taxData.getOutstandingDebt() * penaltyRate;
                totalOwed += penalty;
                record.addTransaction(new TaxTransaction(uuidStr, playerData.getName(),
                        TaxTransactionType.LATE_PENALTY, penalty,
                        String.format("Late penalty (%.0f%% of $%s debt)",
                                penaltyRate * 100, MessageUtils.formatNumber(taxData.getOutstandingDebt()))));
                record.addPenaltyCollected(penalty);
                taxData.recordPenalty(penalty);
                penalizedCount++;
            }

            // Add outstanding debt to total owed
            totalOwed += taxData.getOutstandingDebt();

            // Try to collect from player's balance
            double playerBalance = plugin.getVaultHook().getBalance(playerUUID);

            if (playerBalance >= totalOwed) {
                // Full payment
                plugin.getVaultHook().withdraw(playerUUID, totalOwed);
                plugin.getTreasuryManager().deposit(TransactionType.GLOBAL_TAX_INCOME, totalOwed,
                        "Global tax from " + playerData.getName(), playerUUID);

                taxData.recordPayment(totalOwed);
                taxData.clearDebt();
                record.addTaxCollected(totalOwed);
                totalCollected += totalOwed;

                record.addTransaction(new TaxTransaction(uuidStr, playerData.getName(),
                        TaxTransactionType.TAX_PAID, totalOwed, "Full tax payment"));

                // Notify online player
                Player onlinePlayer = Bukkit.getPlayer(playerUUID);
                if (onlinePlayer != null && onlinePlayer.isOnline()) {
                    MessageUtils.send(onlinePlayer,
                            "<yellow>You have been charged <gold>$" + MessageUtils.formatNumber(totalOwed) +
                                    "</gold> in global taxes. Payment deposited to national treasury.");
                    MessageUtils.playSound(onlinePlayer, Sound.BLOCK_NOTE_BLOCK_PLING);
                }

                taxedCount++;
            } else if (playerBalance > 0) {
                // Partial payment - take what they have, remainder becomes debt
                double paid = playerBalance;
                double remainder = totalOwed - paid;

                plugin.getVaultHook().withdraw(playerUUID, paid);
                plugin.getTreasuryManager().deposit(TransactionType.GLOBAL_TAX_INCOME, paid,
                        "Partial global tax from " + playerData.getName(), playerUUID);

                taxData.recordPayment(paid);
                taxData.clearDebt();
                taxData.addDebt(remainder);
                taxData.addMissedPayment();
                record.addTaxCollected(paid);
                totalCollected += paid;

                record.addTransaction(new TaxTransaction(uuidStr, playerData.getName(),
                        TaxTransactionType.TAX_PAID, paid, "Partial tax payment"));
                record.addTransaction(new TaxTransaction(uuidStr, playerData.getName(),
                        TaxTransactionType.LATE_PENALTY, remainder,
                        "Outstanding debt added (insufficient funds)"));

                // Notify online player
                Player onlinePlayer = Bukkit.getPlayer(playerUUID);
                if (onlinePlayer != null && onlinePlayer.isOnline()) {
                    MessageUtils.send(onlinePlayer,
                            "<red>You could not afford the full tax of <gold>$" + MessageUtils.formatNumber(totalOwed) +
                                    "</gold>. <red>Paid <gold>$" + MessageUtils.formatNumber(paid) +
                                    "</gold>, <red>remaining debt: <gold>$" + MessageUtils.formatNumber(remainder));
                    MessageUtils.playSound(onlinePlayer, Sound.ENTITY_VILLAGER_NO);
                }

                penalizedCount++;
            } else {
                // No payment - full amount becomes debt
                taxData.addDebt(totalOwed);
                taxData.addMissedPayment();

                record.addTransaction(new TaxTransaction(uuidStr, playerData.getName(),
                        TaxTransactionType.LATE_PENALTY, totalOwed,
                        "Full debt added (no funds available)"));

                // Notify online player
                Player onlinePlayer = Bukkit.getPlayer(playerUUID);
                if (onlinePlayer != null && onlinePlayer.isOnline()) {
                    MessageUtils.send(onlinePlayer,
                            "<red>You have no funds to pay the global tax of <gold>$" + MessageUtils.formatNumber(totalOwed) +
                                    "</gold>. <red>This has been added to your debt. Total debt: <gold>$" +
                                    MessageUtils.formatNumber(taxData.getOutstandingDebt()));
                    MessageUtils.playSound(onlinePlayer, Sound.ENTITY_VILLAGER_NO);
                }

                penalizedCount++;
            }

            // Check for punishment threshold
            if (taxData.getMissedPayments() >= maxMissedBeforePunishment) {
                applyPunishment(playerUUID, playerData, taxData, record);
            }
        }

        // Update cycle
        record.incrementCycle();

        // Broadcast tax collection summary
        if (taxedCount > 0 || penalizedCount > 0) {
            MessageUtils.broadcast("<gold>=======================================");
            MessageUtils.broadcast("<yellow>     GLOBAL TAX COLLECTION");
            MessageUtils.broadcast("<gold>=======================================");
            MessageUtils.broadcast("<gray>Tax Amount: <gold>$" + MessageUtils.formatNumber(taxAmount));
            MessageUtils.broadcast("<gray>Players Taxed: <green>" + taxedCount);
            if (penalizedCount > 0) {
                MessageUtils.broadcast("<gray>Players Penalized: <red>" + penalizedCount);
            }
            if (exemptCount > 0) {
                MessageUtils.broadcast("<gray>Players Exempt: <aqua>" + exemptCount);
            }
            MessageUtils.broadcast("<gray>Total Collected: <gold>$" + MessageUtils.formatNumber(totalCollected));
            MessageUtils.broadcast("<gold>=======================================");
        }

        plugin.getLogger().info("Tax collection completed: " + taxedCount + " taxed, " +
                penalizedCount + " penalized, " + exemptCount + " exempt. Total: $" +
                String.format("%.2f", totalCollected));
    }

    /**
     * Apply punishment for excessive missed payments
     */
    private void applyPunishment(UUID playerUUID, PlayerData playerData,
                                  PlayerTaxData taxData, TaxRecord record) {
        String uuidStr = playerUUID.toString();

        // Apply punishment based on config
        List<String> punishments = plugin.getConfig().getStringList("global-tax.punishment.commands");
        String punishmentDesc = plugin.getConfig().getString("global-tax.punishment.description",
                "Tax evasion penalty");

        if (punishments.isEmpty()) {
            // Default: add a punishment record
            playerData.getPunishments().add(
                    new PlayerData.Punishment("TAX_EVASION", punishmentDesc, 0));
        } else {
            // Execute configured punishment commands
            Player onlinePlayer = Bukkit.getPlayer(playerUUID);
            String playerName = playerData.getName();

            for (String cmd : punishments) {
                String processedCmd = cmd
                        .replace("{player}", playerName)
                        .replace("{debt}", String.format("%.0f", taxData.getOutstandingDebt()))
                        .replace("{missed}", String.valueOf(taxData.getMissedPayments()));

                Bukkit.getScheduler().runTask(plugin, () ->
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCmd));
            }
        }

        // Record punishment
        taxData.getPunishmentHistory().add(String.valueOf(System.currentTimeMillis()));
        record.addTransaction(new TaxTransaction(uuidStr, playerData.getName(),
                TaxTransactionType.PUNISHMENT_APPLIED, taxData.getOutstandingDebt(),
                punishmentDesc + " (Missed " + taxData.getMissedPayments() + " payments)"));

        // Reset missed counter after punishment
        int resetTo = plugin.getConfig().getInt("global-tax.punishment.reset-missed-after-punishment", 0);
        taxData.setMissedPayments(resetTo);

        // Notify player
        Player onlinePlayer = Bukkit.getPlayer(playerUUID);
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            MessageUtils.send(onlinePlayer,
                    "<dark_red><bold>TAX EVASION PENALTY!</bold></dark_red> <red>You have been punished for missing " +
                            "too many tax payments. Outstanding debt: <gold>$" +
                            MessageUtils.formatNumber(taxData.getOutstandingDebt()));
            MessageUtils.sendTitle(onlinePlayer,
                    "<red><bold>TAX PENALTY",
                    "<yellow>You have been punished for tax evasion!",
                    10, 60, 20);
            MessageUtils.playSound(onlinePlayer, Sound.ENTITY_WITHER_SPAWN);
        }
    }

    /**
     * Pay outstanding tax debt manually
     */
    public boolean payDebt(Player player) {
        TaxRecord record = getTaxRecord();
        String uuidStr = player.getUniqueId().toString();
        PlayerTaxData taxData = record.getPlayerTaxData(uuidStr);

        if (taxData == null || taxData.getOutstandingDebt() <= 0) {
            MessageUtils.send(player, "<green>You have no outstanding tax debt!");
            return false;
        }

        double debt = taxData.getOutstandingDebt();
        double balance = plugin.getVaultHook().getBalance(player.getUniqueId());

        if (balance < debt) {
            MessageUtils.send(player, "<red>You need <gold>$" + MessageUtils.formatNumber(debt) +
                    "</gold> to pay your debt. Current balance: <gold>$" + MessageUtils.formatNumber(balance));
            return false;
        }

        plugin.getVaultHook().withdraw(player.getUniqueId(), debt);
        plugin.getTreasuryManager().deposit(TransactionType.GLOBAL_TAX_INCOME, debt,
                "Debt payment from " + player.getName(), player.getUniqueId());

        taxData.clearDebt();
        taxData.setMissedPayments(0);
        record.addTaxCollected(debt);

        record.addTransaction(new TaxTransaction(uuidStr, player.getName(),
                TaxTransactionType.DEBT_PAYMENT, debt, "Manual debt payment"));

        MessageUtils.send(player, "<green>Successfully paid off your tax debt of <gold>$" +
                MessageUtils.formatNumber(debt) + "</gold>!");
        MessageUtils.playSound(player, Sound.ENTITY_PLAYER_LEVELUP);

        return true;
    }

    /**
     * Set a player's tax exempt status (admin only)
     */
    public void setExempt(UUID playerUUID, String playerName, boolean exempt) {
        TaxRecord record = getTaxRecord();
        PlayerTaxData taxData = record.getOrCreatePlayerTaxData(playerUUID.toString(), playerName);
        taxData.setExempt(exempt);

        record.addTransaction(new TaxTransaction(playerUUID.toString(), playerName,
                TaxTransactionType.TAX_EXEMPTION, 0,
                exempt ? "Player exempted from taxes" : "Tax exemption removed"));
    }

    /**
     * Forgive a player's debt (admin only)
     */
    public void forgiveDebt(UUID playerUUID, String playerName) {
        TaxRecord record = getTaxRecord();
        PlayerTaxData taxData = record.getPlayerTaxData(playerUUID.toString());

        if (taxData != null && taxData.getOutstandingDebt() > 0) {
            double forgiven = taxData.getOutstandingDebt();
            taxData.clearDebt();
            taxData.setMissedPayments(0);

            record.addTransaction(new TaxTransaction(playerUUID.toString(), playerName,
                    TaxTransactionType.DEBT_FORGIVEN, forgiven, "Debt forgiven by admin"));
        }
    }

    /**
     * Get next collection time
     */
    public long getNextCollectionTime() {
        TaxRecord record = getTaxRecord();
        if (record.getLastCollectionTime() == 0) return 0;
        return record.getLastCollectionTime() + getCollectionIntervalMillis();
    }

    /**
     * Get time remaining until next collection
     */
    public long getTimeUntilNextCollection() {
        long next = getNextCollectionTime();
        if (next == 0) return 0;
        return Math.max(0, next - System.currentTimeMillis());
    }

    // === Config Helpers ===

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("global-tax.enabled", true) && getTaxRecord().isEnabled();
    }

    public double getTaxAmount() {
        return plugin.getConfig().getDouble("global-tax.amount", 50);
    }

    public long getCollectionIntervalMillis() {
        long hours = plugin.getConfig().getLong("global-tax.collection-interval-hours", 24);
        return hours * 60L * 60 * 1000;
    }

    public long getInactiveDaysThreshold() {
        return plugin.getConfig().getLong("global-tax.inactive-days-exempt", 3);
    }

    public double getLatePenaltyRate() {
        return plugin.getConfig().getDouble("global-tax.late-penalty-rate", 0.10);
    }

    public int getMaxMissedBeforePunishment() {
        return plugin.getConfig().getInt("global-tax.punishment.missed-threshold", 3);
    }

    /**
     * Get total number of taxable players (active within threshold)
     */
    public int getTaxablePlayerCount() {
        long inactiveThreshold = System.currentTimeMillis() -
                (getInactiveDaysThreshold() * 24L * 60 * 60 * 1000);
        return (int) plugin.getDataManager().getAllPlayerData().stream()
                .filter(p -> p.getLastSeen() >= inactiveThreshold)
                .count();
    }

    /**
     * Get total outstanding debt across all players
     */
    public double getTotalOutstandingDebt() {
        return getTaxRecord().getPlayerTaxData().values().stream()
                .mapToDouble(PlayerTaxData::getOutstandingDebt)
                .sum();
    }

    /**
     * Get number of players with outstanding debt
     */
    public int getDebtorCount() {
        return (int) getTaxRecord().getPlayerTaxData().values().stream()
                .filter(d -> d.getOutstandingDebt() > 0)
                .count();
    }
}
