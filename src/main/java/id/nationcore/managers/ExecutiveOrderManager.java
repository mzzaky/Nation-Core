package id.nationcore.managers;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import id.nationcore.NationCore;
import id.nationcore.models.ExecutiveOrder;
import id.nationcore.models.ExecutiveOrder.ExecutiveOrderType;
import id.nationcore.models.ExecutiveOrder.NationType;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.Nation;
import id.nationcore.models.RepublicExecutiveOrder;
import id.nationcore.models.PlayerData;
import id.nationcore.models.PresidentHistory.PresidentRecord;
import id.nationcore.models.Treasury.TransactionType;
import id.nationcore.utils.MessageUtils;

public class ExecutiveOrderManager {

    private final NationCore plugin;

    public ExecutiveOrderManager(NationCore plugin) {
        this.plugin = plugin;
    }

    public List<ExecutiveOrder> getActiveOrders() {
        return plugin.getDataManager().getActiveOrders();
    }

    public boolean isOrderActive(ExecutiveOrderType type) {
        return getActiveOrders().stream()
                .anyMatch(o -> o.getType() == type && o.isActive() && !o.isExpired());
    }

    public boolean isOrderOnCooldown(ExecutiveOrderType type) {
        long cooldownDays = plugin.getConfig().getLong("executive-orders.cooldown-days", 7);
        long cooldownMillis = cooldownDays * 24 * 60 * 60 * 1000;
        long lastOrderTime = plugin.getDataManager().getLastExecutiveOrderTime();
        return System.currentTimeMillis() - lastOrderTime < cooldownMillis;
    }

    public long getOrderCooldownRemaining(ExecutiveOrderType type) {
        long cooldownDays = plugin.getConfig().getLong("executive-orders.cooldown-days", 7);
        long cooldownMillis = cooldownDays * 24 * 60 * 60 * 1000;
        long lastOrderTime = plugin.getDataManager().getLastExecutiveOrderTime();
        long elapsed = System.currentTimeMillis() - lastOrderTime;
        return Math.max(0, cooldownMillis - elapsed);
    }

    public ExecutiveOrder getActiveOrder(ExecutiveOrderType type) {
        return getActiveOrders().stream()
                .filter(o -> o.getType() == type && o.isActive() && !o.isExpired())
                .findFirst()
                .orElse(null);
    }

    public boolean issueOrder(Player president, ExecutiveOrderType type) {
        UUID uuid = president.getUniqueId();

        // Check if president
        if (!plugin.getGovernmentManager().isPresident(uuid)) {
            MessageUtils.send(president, "executive_orders.only_president");
            return false;
        }

        // Check cooldown
        long cooldownDays = plugin.getConfig().getLong("executive-orders.cooldown-days", 7);
        long cooldownMillis = cooldownDays * 24 * 60 * 60 * 1000;
        long lastOrderTime = plugin.getDataManager().getLastExecutiveOrderTime();

        if (System.currentTimeMillis() - lastOrderTime < cooldownMillis) {
            long remaining = cooldownMillis - (System.currentTimeMillis() - lastOrderTime);
            MessageUtils.send(president, "executive_orders.cooldown", "time", MessageUtils.formatTime(remaining));
            return false;
        }

        // Check if order already active
        if (isOrderActive(type)) {
            MessageUtils.send(president, "executive_orders.already_active");
            return false;
        }

        // Check treasury
        double cost = plugin.getConfig().getDouble("executive-orders.cost", 1000000);
        if (!plugin.getTreasuryManager().canAfford(cost)) {
            MessageUtils.send(president, "executive_orders.insufficient_funds", "amount",
                    plugin.getVaultHook().format(cost));
            return false;
        }

        // Withdraw from treasury
        plugin.getTreasuryManager().withdraw(TransactionType.EXECUTIVE_ORDER, cost,
                "Executive Order: " + type.getDisplayName(), uuid);

        // Create and activate order
        ExecutiveOrder order = new RepublicExecutiveOrder(type, uuid, type.getDefaultDuration());
        getActiveOrders().add(order);
        plugin.getDataManager().setLastExecutiveOrderTime(System.currentTimeMillis());

        // Update history record
        PresidentRecord record = plugin.getDataManager().getPresidentHistory().getLatestRecord();
        if (record != null) {
            record.setExecutiveOrdersIssued(record.getExecutiveOrdersIssued() + 1);
        }

        // Apply effects (legacy path — no nation context)
        applyOrderEffects(null, order);

        // Broadcast
        MessageUtils.broadcastAnnouncement("EXECUTIVE ORDER: " + type.getDisplayName(),
                "<italic><yellow>\"" + type.getFlavorText() + "\"</yellow></italic>\n\n" +
                        "<gray>Effect: " + type.getEffectDescription() + "</gray>\n" +
                        "<gray>Duration: " + MessageUtils.formatTimeShort(type.getDefaultDuration()) + "</gray>");

        MessageUtils.broadcastTitle("<gold>⚡ EXECUTIVE ORDER ⚡</gold>",
                "<yellow>" + type.getDisplayName() + "</yellow>", 20, 100, 20);
        MessageUtils.broadcastSound(Sound.ENTITY_ENDER_DRAGON_GROWL);

        // Spawn lightning effect at president's location for dramatic effect
        president.getWorld().strikeLightningEffect(president.getLocation());

        return true;
    }

    private void applyOrderEffects(Nation nation, ExecutiveOrder order) {
        if (nation == null) return;
        
        switch (order.getType()) {
            case GOLDEN_AGE -> {
                // Buffs applied through buff manager - usually per player anyway
            }

            case WAR_ECONOMY -> {
                MessageUtils.sendToNation(nation, "<red>War Economy is now active for " + nation.getName() + "! PvP rewards doubled!</red>");
            }
            case ECONOMIC_RECOVERY -> {
                // Give stimulus to all online members of this nation
                double stimulus = 50000;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (nation.isMember(player.getUniqueId())) {
                        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(
                                player.getUniqueId(), player.getName());
                        if (!data.isClaimedStimulus()) {
                            plugin.getVaultHook().deposit(player.getUniqueId(), stimulus);
                            data.setClaimedStimulus(true);
                            MessageUtils.send(player, "managers.executive_order.stimulus_received", "amount",
                                    plugin.getVaultHook().format(stimulus));
                        }
                    }
                }
            }

            case ENVIRONMENTAL_PROTECTION -> {
                // Multipliers handled in listeners
            }
            case EDUCATION_ADVANCEMENT -> {
                // Multipliers handled in listeners
            }
            case PURGE_PROTOCOL -> {
                MessageUtils.sendToNationTitle(nation, "<dark_red>⚠️ PURGE ACTIVE ⚠️</dark_red>",
                        "<red>Full PvP enabled for " + nation.getName() + "!</red>", 20, 100, 20);
            }
            case PRESIDENTIAL_PARDON -> {
                // Clear one punishment from all online members of this nation
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (nation.isMember(player.getUniqueId())) {
                        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
                        if (data != null && !data.getPunishments().isEmpty()) {
                            data.getPunishments().remove(data.getPunishments().size() - 1);
                            MessageUtils.send(player, "managers.executive_order.punishment_cleared");
                        }
                    }
                }
            }
            case TAX_SUSPENSION -> {
                MessageUtils.sendToNation(nation, "<green>Tax Suspension is now active for " + nation.getName() + "! All tax collection has been halted!</green>");
            }
            case TAX_SURGE -> {
                MessageUtils.sendToNation(nation, "<red>Tax Surge is now active for " + nation.getName() + "! Tax rates are raised to 5x the base rate!</red>");
            }
        }
    }

    public void checkExpirations() {
        // Legacy global cleanup
        List<ExecutiveOrder> globalOrders = getActiveOrders();
        globalOrders.removeIf(order -> {
            if (order.isExpired() && order.isActive()) {
                order.setActive(false);
                // legacy global has no nation context
                return true;
            }
            return false;
        });

        // Per-nation cleanup
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            List<ExecutiveOrder> orders = nation.getActiveOrders();
            orders.removeIf(order -> {
                if (order.isExpired() && order.isActive()) {
                    order.setActive(false);
                    expireOrder(nation, order);
                    return true;
                }
                return false;
            });
        }
    }

    private void expireOrder(Nation nation, ExecutiveOrder order) {
        MessageUtils.sendToNation(nation, "<gray>Executive Order <yellow>" + order.getType().getDisplayName() +
                "</yellow> has expired.</gray>");

        // Remove effects
        switch (order.getType()) {

            case PURGE_PROTOCOL -> {
                MessageUtils.sendToNation(nation, "<green>The Purge has ended for " + nation.getName() + ". Normal rules restored.</green>");
            }
            case TAX_SUSPENSION -> {
                MessageUtils.sendToNation(nation, "<yellow>Tax Suspension has ended for " + nation.getName() + ". Normal tax collection has resumed.</yellow>");
            }
            case TAX_SURGE -> {
                MessageUtils.sendToNation(nation, "<yellow>Tax Surge has ended for " + nation.getName() + ". Tax rates have returned to normal.</yellow>");
            }
            default -> {
            }
        }
    }

    // Effect getters for listeners (Context-aware)
    public double getXPMultiplier(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) return 1.0;
        
        double multiplier = 1.0;
        if (isOrderActive(nation, ExecutiveOrderType.GOLDEN_AGE)) {
            multiplier *= 1.25;
        }
        if (isOrderActive(nation, ExecutiveOrderType.EDUCATION_ADVANCEMENT)) {
            multiplier *= 3.0;
        }
        return multiplier;
    }

    public double getVaultMultiplier(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) return 1.0;
        
        double multiplier = 1.0;
        if (isOrderActive(nation, ExecutiveOrderType.GOLDEN_AGE)) {
            multiplier *= 1.25;
        }
        return multiplier;
    }

    public double getRareDropMultiplier(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) return 1.0;
        
        double multiplier = 1.0;
        if (isOrderActive(nation, ExecutiveOrderType.GOLDEN_AGE)) {
            multiplier *= 1.15;
        }
        return multiplier;
    }

    public double getFarmingMultiplier(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation != null && isOrderActive(nation, ExecutiveOrderType.ENVIRONMENTAL_PROTECTION)) {
            return 3.0;
        }
        return 1.0;
    }

    public boolean isPurgeActive(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        return nation != null && isOrderActive(nation, ExecutiveOrderType.PURGE_PROTOCOL);
    }

    public double getPvPDamageMultiplier(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation != null && isOrderActive(nation, ExecutiveOrderType.WAR_ECONOMY)) {
            return 1.5;
        }
        return 1.0;
    }

    public double getShopDiscount(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation != null && isOrderActive(nation, ExecutiveOrderType.ECONOMIC_RECOVERY)) {
            return 0.25; // 25% discount
        }
        return 0.0;
    }

    public boolean isTaxSuspended(Nation nation) {
        return nation != null && isOrderActive(nation, ExecutiveOrderType.TAX_SUSPENSION);
    }

    public double getTaxMultiplier(Nation nation) {
        if (isOrderActive(nation, ExecutiveOrderType.TAX_SUSPENSION)) {
            return 0.0; // Tax fully suspended
        }
        if (isOrderActive(nation, ExecutiveOrderType.TAX_SURGE)) {
            return 5.0; // 5x base tax rate
        }
        return 1.0;
    }

    public boolean stopOrder(Nation nation, ExecutiveOrderType type) {
        ExecutiveOrder order = getActiveOrder(nation, type);
        if (order == null) {
            return false;
        }

        order.setActive(false);
        expireOrder(nation, order);
        nation.getActiveOrders().remove(order);
        return true;
    }

    // ==========================================================
    // Context-aware (per-nation) overloads — Phase 2
    // ==========================================================

    public List<ExecutiveOrder> getActiveOrders(Nation nation) {
        return nation != null ? nation.getActiveOrders() : getActiveOrders();
    }

    public boolean isOrderActive(Nation nation, ExecutiveOrderType type) {
        return getActiveOrders(nation).stream()
                .anyMatch(o -> o.getType() == type && o.isActive() && !o.isExpired());
    }

    public boolean isOrderOnCooldown(Nation nation, ExecutiveOrderType type) {
        // Royal prerogative: monarchies have no cooldown between orders.
        if (nation != null && nation.getType() == GovernmentType.MONARCHY) return false;
        long cooldownDays = plugin.getConfig().getLong("executive-orders.cooldown-days", 7);
        long cooldownMillis = cooldownDays * 24 * 60 * 60 * 1000;
        long lastOrderTime = nation != null ? nation.getLastExecutiveOrderTime()
                : plugin.getDataManager().getLastExecutiveOrderTime();
        return System.currentTimeMillis() - lastOrderTime < cooldownMillis;
    }

    public long getOrderCooldownRemaining(Nation nation, ExecutiveOrderType type) {
        long cooldownDays = plugin.getConfig().getLong("executive-orders.cooldown-days", 7);
        long cooldownMillis = cooldownDays * 24 * 60 * 60 * 1000;
        long lastOrderTime = nation != null ? nation.getLastExecutiveOrderTime()
                : plugin.getDataManager().getLastExecutiveOrderTime();
        long elapsed = System.currentTimeMillis() - lastOrderTime;
        return Math.max(0, cooldownMillis - elapsed);
    }

    public ExecutiveOrder getActiveOrder(Nation nation, ExecutiveOrderType type) {
        return getActiveOrders(nation).stream()
                .filter(o -> o.getType() == type && o.isActive() && !o.isExpired())
                .findFirst()
                .orElse(null);
    }

/**
      * Mengeluarkan executive order dengan kas & cooldown nation pemain.
      * Bila pemain bukan presiden nation atau nation null, fallback ke
      * legacy {@link #issueOrder(Player, ExecutiveOrderType)}.
      */
    public boolean issueOrderForNation(Player president, ExecutiveOrderType type) {
        Nation nation = plugin.getNationManager().getNationOf(president.getUniqueId());
        if (nation == null) return issueOrder(president, type);

        boolean isRepublicLeader = plugin.getGovernmentManager().isPresident(nation, president.getUniqueId());
        boolean isCommunistLeader = plugin.getCommunistManager().isSecretaryGeneral(nation, president.getUniqueId());
        boolean isKing = plugin.getMonarchyManager() != null
                && plugin.getMonarchyManager().isKing(nation, president.getUniqueId());
        boolean isCaliph = plugin.getCaliphateManager() != null
                && plugin.getCaliphateManager().isCaliph(nation, president.getUniqueId());

        if (!isRepublicLeader && !isCommunistLeader && !isKing && !isCaliph) {
            MessageUtils.send(president, "executive_orders.only_leader");
            return false;
        }

        NationType nationType = switch (nation.getType()) {
            case REPUBLIC  -> NationType.REPUBLIC;
            case COMMUNIST -> NationType.COMMUNIST;
            case MONARCHY  -> NationType.MONARCHY;
            case CALIPHATE -> NationType.CALIPHATE;
        };

        // Royal prerogative: the King may issue executive orders freely with
        // no cooldown between orders. Republic and Communist nations still
        // honour the configured cooldown.
        if (nation.getType() != GovernmentType.MONARCHY) {
            long cooldownDays = plugin.getConfig().getLong("executive-orders.cooldown-days", 7);
            long cooldownMillis = cooldownDays * 24 * 60 * 60 * 1000;
            if (System.currentTimeMillis() - nation.getLastExecutiveOrderTime() < cooldownMillis) {
                long remaining = cooldownMillis - (System.currentTimeMillis() - nation.getLastExecutiveOrderTime());
                MessageUtils.send(president, "executive_orders.cooldown",
                        "time", MessageUtils.formatTime(remaining));
                return false;
            }
        }

        if (isOrderActive(nation, type)) {
            MessageUtils.send(president, "executive_orders.already_active");
            return false;
        }

        double cost = plugin.getConfig().getDouble("executive-orders.cost", 1000000);
        if (!plugin.getTreasuryManager().canAfford(nation, cost)) {
            MessageUtils.send(president, "executive_orders.insufficient_funds",
                    "amount", plugin.getVaultHook().format(cost));
            return false;
        }

        plugin.getTreasuryManager().withdraw(nation, TransactionType.EXECUTIVE_ORDER, cost,
                "Executive Order: " + type.getDisplayName(), president.getUniqueId());

        ExecutiveOrder order = ExecutiveOrder.createForNation(nationType, type, president.getUniqueId(), type.getDefaultDuration());
        nation.getActiveOrders().add(order);
        nation.setLastExecutiveOrderTime(System.currentTimeMillis());

        if (nation.getType() == GovernmentType.COMMUNIST && nation.getCommunistGovernment() != null) {
            nation.getCommunistGovernment().addOrderHistory(type.getDisplayName() + " (Leader)");
        }

        applyOrderEffects(nation, order);

        String leaderTitle = nationType.getLeaderTitle();
        MessageUtils.broadcastAnnouncement(leaderTitle + " Executive Order (" + nation.getName() + "): " + type.getDisplayName(),
                "<italic><yellow>\"" + type.getFlavorText() + "\"</yellow></italic>\n\n" +
                        "<gray>Effect: " + type.getEffectDescription() + "</gray>\n" +
                        "<gray>Duration: " + MessageUtils.formatTimeShort(type.getDefaultDuration()) + "</gray>");

        president.getWorld().strikeLightningEffect(president.getLocation());
        plugin.getDataManager().saveNations();
        return true;
    }
}
