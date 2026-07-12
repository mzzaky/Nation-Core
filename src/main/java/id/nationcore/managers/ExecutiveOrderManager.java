package id.nationcore.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;

import id.nationcore.NationCore;
import id.nationcore.models.CaliphateGovernment;
import id.nationcore.models.CommunistGovernment;
import id.nationcore.models.ExecutiveOrder;
import id.nationcore.models.ExecutiveOrder.ExecutiveOrderType;
import id.nationcore.models.ExecutiveOrder.NationType;
import id.nationcore.models.Government;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.MonarchyGovernment;
import id.nationcore.models.Nation;
import id.nationcore.models.RepublicExecutiveOrder;
import id.nationcore.models.PlayerData;
import id.nationcore.models.PresidentHistory.PresidentRecord;
import id.nationcore.models.Treasury.TransactionType;
import id.nationcore.utils.MessageUtils;

public class ExecutiveOrderManager {

    private final NationCore plugin;

    /** Office slots reused by every minister-office console for its executive-order row. */
    public static final int[] OFFICE_ORDER_SLOTS = {10, 11, 12, 13, 14, 15, 16};

    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

    public ExecutiveOrderManager(NationCore plugin) {
        this.plugin = plugin;
    }

    // ==========================================================
    // order.yaml catalogue accessors
    // ==========================================================

    private YamlConfiguration cfg() {
        return plugin.getOrderConfig();
    }

    private String key(ExecutiveOrderType type) {
        return type.name().toLowerCase();
    }

    /** Master switch — a disabled order is hidden everywhere and cannot be issued. */
    public boolean isOrderEnabled(ExecutiveOrderType type) {
        YamlConfiguration c = cfg();
        return c == null || c.getBoolean(key(type) + ".status", true);
    }

    /** Configured display name (falls back to the hardcoded default). */
    public String getOrderDisplay(ExecutiveOrderType type) {
        YamlConfiguration c = cfg();
        if (c == null) return type.getDisplayName();
        return c.getString(key(type) + ".display", type.getDisplayName());
    }

    /** Configured descriptive lore lines (falls back to the hardcoded effect text). */
    public List<String> getOrderLore(ExecutiveOrderType type) {
        YamlConfiguration c = cfg();
        if (c != null) {
            List<String> lore = c.getStringList(key(type) + ".lore");
            if (lore != null && !lore.isEmpty()) return lore;
        }
        List<String> fallback = new ArrayList<>();
        fallback.add(type.getEffectDescription());
        return fallback;
    }

    /** Configured treasury cost. */
    public double getOrderCost(ExecutiveOrderType type) {
        YamlConfiguration c = cfg();
        return c == null ? 1_000_000.0 : c.getDouble(key(type) + ".cost", 1_000_000.0);
    }

    /** Configured per-order cooldown in days. */
    public long getOrderCooldownDays(ExecutiveOrderType type) {
        YamlConfiguration c = cfg();
        return c == null ? 7L : c.getLong(key(type) + ".cooldown", 7L);
    }

    /** Configured active duration in millis (attribute.duration, in hours). */
    public long getOrderDurationMillis(ExecutiveOrderType type) {
        YamlConfiguration c = cfg();
        if (c != null && c.contains(key(type) + ".attribute.duration")) {
            double hours = c.getDouble(key(type) + ".attribute.duration", 0);
            return (long) (hours * 60 * 60 * 1000);
        }
        return type.getDefaultDuration();
    }

    /** Read an order-specific tuning attribute (attribute.&lt;name&gt;). */
    public double getOrderAttribute(ExecutiveOrderType type, String attribute, double def) {
        YamlConfiguration c = cfg();
        return c == null ? def : c.getDouble(key(type) + ".attribute." + attribute, def);
    }

    // ----------------------------------------------------------
    // Generic (String-id) catalogue accessors — also used by the
    // minister/council sector-order engines (Cabinet/Politburo/Council)
    // so every order in the plugin is centrally managed through order.yaml.
    // ----------------------------------------------------------

    /** Master switch for any registered order id (fallback: enabled). */
    public boolean isOrderEnabled(String id) {
        YamlConfiguration c = cfg();
        return c == null || c.getBoolean(id + ".status", true);
    }

    /** Configured display name for any registered order id. */
    public String getOrderDisplay(String id, String def) {
        YamlConfiguration c = cfg();
        return c == null ? def : c.getString(id + ".display", def);
    }

    /** Configured lore lines for any registered order id (fallback used if unset). */
    public List<String> getOrderLore(String id, List<String> def) {
        YamlConfiguration c = cfg();
        if (c != null) {
            List<String> lore = c.getStringList(id + ".lore");
            if (lore != null && !lore.isEmpty()) return lore;
        }
        return def;
    }

    /** Configured cost for any registered order id. */
    public double getOrderCost(String id, double def) {
        YamlConfiguration c = cfg();
        return c == null ? def : c.getDouble(id + ".cost", def);
    }

    /** Configured cooldown (days) for any registered order id. */
    public long getOrderCooldownDays(String id, long def) {
        YamlConfiguration c = cfg();
        return c == null ? def : c.getLong(id + ".cooldown", def);
    }

    /** Whether the given order id is listed under an office in the nation config. */
    public boolean isOrderInOffice(GovernmentType government, String office, String id) {
        if (government == null || office == null || id == null) return false;
        YamlConfiguration c = plugin.getNationConfig(government);
        if (c == null) return false;
        for (String s : c.getStringList("executive_order." + office)) {
            if (s != null && s.trim().equalsIgnoreCase(id)) return true;
        }
        return false;
    }

    /**
     * A sector order (minister/council decision) is shown/executable only when it
     * is enabled in order.yaml AND listed under its office in the nation config.
     */
    public boolean isSectorOrderVisible(GovernmentType government, String office, String id) {
        return isOrderEnabled(id) && isOrderInOffice(government, office, id);
    }

    // ==========================================================
    // Per-position configuration (nations/*.yaml -> executive_order.<pos>)
    // ==========================================================

    /**
     * The enabled executive orders a given office may issue in the given
     * government, in the order they are declared in the nation config.
     */
    public List<ExecutiveOrderType> getOrdersForPosition(GovernmentType government, String positionKey) {
        List<ExecutiveOrderType> result = new ArrayList<>();
        if (government == null || positionKey == null) return result;
        YamlConfiguration c = plugin.getNationConfig(government);
        if (c == null) return result;
        for (String id : c.getStringList("executive_order." + positionKey)) {
            if (id == null || id.isBlank()) continue;
            try {
                ExecutiveOrderType type = ExecutiveOrderType.valueOf(id.trim().toUpperCase());
                if (isOrderEnabled(type)) result.add(type);
            } catch (IllegalArgumentException ignored) {
                // Unknown id in config — skip silently.
            }
        }
        return result;
    }

    /**
     * Resolve which office (config position key) a player holds in the given
     * nation, or {@code null} if they hold no order-issuing office.
     */
    public String resolvePositionKey(Nation nation, UUID uuid) {
        if (nation == null || uuid == null) return null;
        switch (nation.getType()) {
            case REPUBLIC -> {
                Government g = nation.getRepublicGovernment();
                if (g == null) return null;
                if (g.hasPresident() && uuid.equals(g.getPresidentUUID())) return "president";
                if (uuid.equals(g.getCabinetMember(Government.CabinetPosition.HEALTH))) return "minister_of_health";
                if (uuid.equals(g.getCabinetMember(Government.CabinetPosition.DEFENSE))) return "minister_of_defence";
                if (uuid.equals(g.getCabinetMember(Government.CabinetPosition.TREASURY))) return "minister_of_treasury";
                return null;
            }
            case COMMUNIST -> {
                CommunistGovernment g = nation.getCommunistGovernment();
                if (g == null) return null;
                if (g.hasSecretaryGeneral() && uuid.equals(g.getSecretaryGeneralUUID())) return "secretary_general";
                if (matchesPolitburo(g, CommunistGovernment.PolitburoPosition.PROPAGANDA, uuid)) return "minister_of_propaganda";
                if (matchesPolitburo(g, CommunistGovernment.PolitburoPosition.DEFENSE, uuid)) return "minister_of_defence";
                if (matchesPolitburo(g, CommunistGovernment.PolitburoPosition.TREASURY, uuid)) return "minister_of_treasury";
                if (matchesPolitburo(g, CommunistGovernment.PolitburoPosition.HEALTH, uuid)) return "minister_of_health";
                return null;
            }
            case MONARCHY -> {
                MonarchyGovernment g = nation.getMonarchyGovernment();
                if (g != null && g.hasKing() && uuid.equals(g.getKingUUID())) return "king";
                return null;
            }
            case CALIPHATE -> {
                CaliphateGovernment g = nation.getCaliphateGovernment();
                if (g != null && g.hasCaliph() && uuid.equals(g.getCaliphUUID())) return "caliph";
                return null;
            }
        }
        return null;
    }

    private boolean matchesPolitburo(CommunistGovernment g, CommunistGovernment.PolitburoPosition pos, UUID uuid) {
        var member = g.getPolitburoMember(pos);
        return member != null && uuid.equals(member.getUuid());
    }

    /** Human-readable title for a config position key. */
    public String prettyPosition(String positionKey) {
        if (positionKey == null) return "Leader";
        return switch (positionKey) {
            case "president" -> "President";
            case "secretary_general" -> "Secretary General";
            case "king" -> "King";
            case "caliph" -> "Caliph";
            case "minister_of_health" -> "Minister of Health";
            case "minister_of_defence" -> "Minister of Defence";
            case "minister_of_treasury" -> "Minister of Treasury";
            case "minister_of_propaganda" -> "Minister of Propaganda";
            default -> positionKey;
        };
    }

    /** Map an inventory slot to an order using the given ordered list + slot layout. */
    public static ExecutiveOrderType orderAtSlot(List<ExecutiveOrderType> orders, int[] slots, int slot) {
        if (orders == null || slots == null) return null;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                return i < orders.size() ? orders.get(i) : null;
            }
        }
        return null;
    }

    // ==========================================================
    // Legacy global helpers (nation == null fallback path)
    // ==========================================================

    public List<ExecutiveOrder> getActiveOrders() {
        return plugin.getDataManager().getActiveOrders();
    }

    public boolean isOrderActive(ExecutiveOrderType type) {
        return getActiveOrders().stream()
                .anyMatch(o -> o.getType() == type && o.isActive() && !o.isExpired());
    }

    public boolean isOrderOnCooldown(ExecutiveOrderType type) {
        long cooldownMillis = getOrderCooldownDays(type) * DAY_MILLIS;
        long lastOrderTime = plugin.getDataManager().getLastExecutiveOrderTime();
        return System.currentTimeMillis() - lastOrderTime < cooldownMillis;
    }

    public long getOrderCooldownRemaining(ExecutiveOrderType type) {
        long cooldownMillis = getOrderCooldownDays(type) * DAY_MILLIS;
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

        if (!plugin.getGovernmentManager().isPresident(uuid)) {
            MessageUtils.send(president, "executive_orders.only_president");
            return false;
        }

        if (!isOrderEnabled(type)) {
            MessageUtils.send(president, "<red>This executive order is currently disabled.</red>");
            return false;
        }

        long cooldownMillis = getOrderCooldownDays(type) * DAY_MILLIS;
        long lastOrderTime = plugin.getDataManager().getLastExecutiveOrderTime();
        if (System.currentTimeMillis() - lastOrderTime < cooldownMillis) {
            long remaining = cooldownMillis - (System.currentTimeMillis() - lastOrderTime);
            MessageUtils.send(president, "executive_orders.cooldown", "time", MessageUtils.formatTime(remaining));
            return false;
        }

        if (isOrderActive(type)) {
            MessageUtils.send(president, "executive_orders.already_active");
            return false;
        }

        double cost = getOrderCost(type);
        if (!plugin.getTreasuryManager().canAfford(cost)) {
            MessageUtils.send(president, "executive_orders.insufficient_funds", "amount",
                    plugin.getVaultHook().format(cost));
            return false;
        }

        plugin.getTreasuryManager().withdraw(TransactionType.EXECUTIVE_ORDER, cost,
                "Executive Order: " + getOrderDisplay(type), uuid);

        ExecutiveOrder order = new RepublicExecutiveOrder(type, uuid, getOrderDurationMillis(type));
        getActiveOrders().add(order);
        plugin.getDataManager().setLastExecutiveOrderTime(System.currentTimeMillis());

        PresidentRecord record = plugin.getDataManager().getPresidentHistory().getLatestRecord();
        if (record != null) {
            record.setExecutiveOrdersIssued(record.getExecutiveOrdersIssued() + 1);
        }

        applyOrderEffects(null, order);
        broadcastOrder("EXECUTIVE ORDER", null, type);
        president.getWorld().strikeLightningEffect(president.getLocation());
        return true;
    }

    // ==========================================================
    // Effect application / expiry
    // ==========================================================

    private void applyOrderEffects(Nation nation, ExecutiveOrder order) {
        if (nation == null) return;

        switch (order.getType()) {
            case GOLDEN_AGE -> {
                // Passive multipliers handled by the effect getters.
            }
            case WAR_ECONOMY ->
                MessageUtils.sendToNation(nation, "<red>War Economy is now active for " + nation.getName() + "! PvP rewards doubled!</red>");
            case ECONOMIC_RECOVERY -> {
                double stimulus = getOrderAttribute(ExecutiveOrderType.ECONOMIC_RECOVERY, "vault", 50000);
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
                // Multipliers handled in listeners.
            }
            case EDUCATION_ADVANCEMENT -> {
                // Multipliers handled in listeners.
            }
            case PURGE_PROTOCOL ->
                MessageUtils.sendToNationTitle(nation, "<dark_red>⚠️ PURGE ACTIVE ⚠️</dark_red>",
                        "<red>Full PvP enabled for " + nation.getName() + "!</red>", 20, 100, 20);
            case PRESIDENTIAL_PARDON -> {
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
            case TAX_SUSPENSION ->
                MessageUtils.sendToNation(nation, "<green>Tax Suspension is now active for " + nation.getName() + "! All tax collection has been halted!</green>");
            case TAX_SURGE ->
                MessageUtils.sendToNation(nation, "<red>Tax Surge is now active for " + nation.getName() + "! Tax rates are raised to 5x the base rate!</red>");
        }
    }

    private void broadcastOrder(String prefix, Nation nation, ExecutiveOrderType type) {
        String label = nation != null ? prefix + " (" + nation.getName() + ")" : prefix;
        StringBuilder effect = new StringBuilder();
        for (String line : getOrderLore(type)) {
            if (effect.length() > 0) effect.append(" ");
            effect.append(line);
        }
        long duration = getOrderDurationMillis(type);
        MessageUtils.broadcastAnnouncement(label + ": " + getOrderDisplay(type),
                "<italic><yellow>\"" + type.getFlavorText() + "\"</yellow></italic>\n\n" +
                        "<gray>Effect: " + effect + "</gray>\n" +
                        "<gray>Duration: " + (duration == 0 ? "Instant" : MessageUtils.formatTimeShort(duration)) + "</gray>");
    }

    public void checkExpirations() {
        // Legacy global cleanup
        List<ExecutiveOrder> globalOrders = getActiveOrders();
        globalOrders.removeIf(order -> {
            if (order.isExpired() && order.isActive()) {
                order.setActive(false);
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
        MessageUtils.sendToNation(nation, "<gray>Executive Order <yellow>" + getOrderDisplay(order.getType()) +
                "</yellow> has expired.</gray>");

        switch (order.getType()) {
            case PURGE_PROTOCOL ->
                MessageUtils.sendToNation(nation, "<green>The Purge has ended for " + nation.getName() + ". Normal rules restored.</green>");
            case TAX_SUSPENSION ->
                MessageUtils.sendToNation(nation, "<yellow>Tax Suspension has ended for " + nation.getName() + ". Normal tax collection has resumed.</yellow>");
            case TAX_SURGE ->
                MessageUtils.sendToNation(nation, "<yellow>Tax Surge has ended for " + nation.getName() + ". Tax rates have returned to normal.</yellow>");
            default -> {
            }
        }
    }

    // ==========================================================
    // Effect getters for listeners (context-aware, config-tunable)
    // ==========================================================

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
            multiplier *= getOrderAttribute(ExecutiveOrderType.GOLDEN_AGE, "vault", 1.25);
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
            return getOrderAttribute(ExecutiveOrderType.WAR_ECONOMY, "damage", 1.5);
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
    // Context-aware (per-nation) overloads
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
        long cooldownMillis = getOrderCooldownDays(type) * DAY_MILLIS;
        long lastOrderTime = nation != null ? nation.getOrderCooldown(type.name())
                : plugin.getDataManager().getLastExecutiveOrderTime();
        return System.currentTimeMillis() - lastOrderTime < cooldownMillis;
    }

    public long getOrderCooldownRemaining(Nation nation, ExecutiveOrderType type) {
        if (nation != null && nation.getType() == GovernmentType.MONARCHY) return 0L;
        long cooldownMillis = getOrderCooldownDays(type) * DAY_MILLIS;
        long lastOrderTime = nation != null ? nation.getOrderCooldown(type.name())
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
     * Issue an executive order on behalf of the player, charged to their
     * nation's treasury. Authorization is per-office: the player may only
     * issue an order that is listed under the office they hold in the nation
     * config (order.yaml + nations/*.yaml). Admins bypass the office check.
     * Falls back to {@link #issueOrder(Player, ExecutiveOrderType)} when the
     * player is not in a nation.
     */
    public boolean issueOrderForNation(Player player, ExecutiveOrderType type) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) return issueOrder(player, type);

        boolean isAdmin = player.hasPermission("nation.admin");

        if (!isOrderEnabled(type)) {
            MessageUtils.send(player, "<red>This executive order is currently disabled.</red>");
            return false;
        }

        String positionKey = resolvePositionKey(nation, player.getUniqueId());
        if (!isAdmin) {
            if (positionKey == null || !getOrdersForPosition(nation.getType(), positionKey).contains(type)) {
                MessageUtils.send(player, "<red>Your office is not authorized to issue this executive order.</red>");
                return false;
            }
        }

        // Cooldown — per order type, tracked per nation. Monarchy is exempt.
        if (nation.getType() != GovernmentType.MONARCHY) {
            long cooldownMillis = getOrderCooldownDays(type) * DAY_MILLIS;
            long last = nation.getOrderCooldown(type.name());
            if (System.currentTimeMillis() - last < cooldownMillis) {
                long remaining = cooldownMillis - (System.currentTimeMillis() - last);
                MessageUtils.send(player, "executive_orders.cooldown",
                        "time", MessageUtils.formatTime(remaining));
                return false;
            }
        }

        if (isOrderActive(nation, type)) {
            MessageUtils.send(player, "executive_orders.already_active");
            return false;
        }

        double cost = getOrderCost(type);
        if (!plugin.getTreasuryManager().canAfford(nation, cost)) {
            MessageUtils.send(player, "executive_orders.insufficient_funds",
                    "amount", plugin.getVaultHook().format(cost));
            return false;
        }

        plugin.getTreasuryManager().withdraw(nation, TransactionType.EXECUTIVE_ORDER, cost,
                "Executive Order: " + getOrderDisplay(type), player.getUniqueId());

        NationType nationType = switch (nation.getType()) {
            case REPUBLIC  -> NationType.REPUBLIC;
            case COMMUNIST -> NationType.COMMUNIST;
            case MONARCHY  -> NationType.MONARCHY;
            case CALIPHATE -> NationType.CALIPHATE;
        };

        ExecutiveOrder order = ExecutiveOrder.createForNation(nationType, type, player.getUniqueId(),
                getOrderDurationMillis(type));
        nation.getActiveOrders().add(order);
        nation.setOrderCooldown(type.name(), System.currentTimeMillis());
        nation.setLastExecutiveOrderTime(System.currentTimeMillis());

        if (nation.getType() == GovernmentType.COMMUNIST && nation.getCommunistGovernment() != null) {
            nation.getCommunistGovernment().addOrderHistory(
                    getOrderDisplay(type) + " (" + prettyPosition(positionKey) + ")");
        }

        applyOrderEffects(nation, order);

        String issuerTitle = prettyPosition(positionKey);
        broadcastOrder(issuerTitle + " Executive Order", nation, type);

        player.getWorld().strikeLightningEffect(player.getLocation());
        plugin.getDataManager().saveNations();
        return true;
    }
}
