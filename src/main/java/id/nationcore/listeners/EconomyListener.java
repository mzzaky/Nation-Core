package id.nationcore.listeners;

import id.nationcore.NationCore;
import id.nationcore.models.ExecutiveOrder;
import id.nationcore.models.Nation;
import id.nationcore.models.Treasury;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;


/**
 * This listener would integrate with economy plugins to handle:
 * - President vault bonus
 * - Cabinet treasury bonus
 * - Tax collection
 * - Economic executive orders
 * 
 * Note: Actual integration depends on the economy plugin being used.
 * This provides the logic framework for when economy events are available.
 */
public class EconomyListener implements Listener {

    private final NationCore plugin;

    public EconomyListener(NationCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Calculate and apply vault bonus for a player
     * Called when player receives money through various means
     */
    public double applyVaultBonus(Player player, double amount) {
        if (amount <= 0)
            return amount;

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        double bonus = 0;

        if (nation != null) {
            // Leader vault bonus
            if (nation.getLeaderUUID().equals(player.getUniqueId())) {
                bonus += (plugin.getConfig().getDouble("president.buffs.vault-multiplier", 1.20) - 1.0);
            }

            // Politburo/Cabinet bonuses could be added here if needed

            // Golden Age executive order
            if (plugin.getExecutiveOrderManager().isOrderActive(nation, ExecutiveOrder.ExecutiveOrderType.GOLDEN_AGE)) {
                bonus += 0.25;
            }

            // Economic Recovery executive order
            if (plugin.getExecutiveOrderManager().isOrderActive(nation, ExecutiveOrder.ExecutiveOrderType.ECONOMIC_RECOVERY)) {
                bonus += 0.10;
            }
        }

        return amount * (1.0 + bonus);
    }

    /**
     * Calculate tax amount for a transaction
     */
    public double calculateTax(Player player, double amount) {
        // Check for tax holiday (cabinet decision)
        if (plugin.getCabinetManager().isTaxHolidayActive()) {
            return 0;
        }

        double taxRate = plugin.getConfig().getDouble("treasury.tax-rate", 0.05);
        return amount * taxRate;
    }

    /**
     * Process a transaction with tax
     */
    public void processTransaction(Player from, Player to, double amount, String reason) {
        double tax = calculateTax(from, amount);
        // netAmount would be used when wiring to the economy plugin
        // double netAmount = amount - tax;

        // Collect tax to treasury
        if (tax > 0) {
            plugin.getTreasuryManager().deposit(Treasury.TransactionType.TAX_INCOME, tax, "Transaction tax: " + reason,
                    null);
        }

        // The actual transfer would be handled by the economy plugin
        // This is for tracking purposes
    }

    /**
     * Get shop discount for a player
     */
    public double getShopDiscount(Player player) {
        double discount = 0;

        // Cabinet decision discounts
        discount = Math.max(discount, 1.0 - plugin.getCabinetManager().getShopDiscount(player.getUniqueId()));

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation != null) {
            // Economic Recovery order
            if (plugin.getExecutiveOrderManager().isOrderActive(nation, ExecutiveOrder.ExecutiveOrderType.ECONOMIC_RECOVERY)) {
                discount = Math.max(discount, 0.25);
            }
        }

        return discount;
    }

    /**
     * Check if player should receive double rewards (War Economy)
     */
    public boolean hasDoubleRewards(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        return nation != null && plugin.getExecutiveOrderManager().isOrderActive(nation, ExecutiveOrder.ExecutiveOrderType.WAR_ECONOMY);
    }

    /**
     * Get XP multiplier for a player
     */
    public double getXpMultiplier(Player player) {
        double multiplier = 1.0;

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation != null) {
            // Leader XP bonus
            if (nation.getLeaderUUID().equals(player.getUniqueId())) {
                multiplier += (plugin.getConfig().getDouble("president.buffs.xp-multiplier", 1.10) - 1.0);
            }

            // Golden Age order
            if (plugin.getExecutiveOrderManager().isOrderActive(nation, ExecutiveOrder.ExecutiveOrderType.GOLDEN_AGE)) {
                multiplier += 0.25;
            }

            // Education Initiative order
            if (plugin.getExecutiveOrderManager().isOrderActive(nation, ExecutiveOrder.ExecutiveOrderType.EDUCATION_ADVANCEMENT)) {
                multiplier += 2.0; // 3x total
            }
        }

        // Double XP cabinet decision (Legacy or update needed)
        multiplier *= plugin.getCabinetManager().getGlobalXpMultiplier();

        return multiplier;
    }

    /**
     * Get drop multiplier for a player
     */
    public double getDropMultiplier(Player player) {
        double multiplier = 1.0;

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation != null) {
            // Golden Age order
            if (plugin.getExecutiveOrderManager().isOrderActive(nation, ExecutiveOrder.ExecutiveOrderType.GOLDEN_AGE)) {
                multiplier += 0.25;
            }

            // Environmental Protection order
            if (plugin.getExecutiveOrderManager().isOrderActive(nation, ExecutiveOrder.ExecutiveOrderType.ENVIRONMENTAL_PROTECTION)) {
                multiplier += 2.0; // 3x for farming/fishing
            }
        }

        // Resource Boom cabinet decision (Legacy or update needed)
        multiplier *= plugin.getCabinetManager().getGlobalDropMultiplier();

        return multiplier;
    }
}
