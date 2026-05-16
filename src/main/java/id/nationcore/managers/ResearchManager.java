package id.nationcore.managers;

import java.io.File;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import id.nationcore.NationCore;
import id.nationcore.models.Nation;
import id.nationcore.models.NationResearchData;
import id.nationcore.models.NationResearchData.ActiveResearch;
import id.nationcore.models.ResearchType;
import id.nationcore.models.Treasury;
import id.nationcore.utils.MessageUtils;

/**
 * Sentral kontrol untuk semua riset per-nation.
 *
 * Tanggung jawab:
 *   • memuat dan men-cache config research.yml (cost, durasi, max-level, efek)
 *   • memvalidasi pre-condition saat sebuah riset dimulai (leader-only,
 *     vault cukup, max-level belum tercapai, slot riset kosong)
 *   • men-tick proyek riset aktif setiap menit dan menyelesaikannya begitu
 *     waktunya habis (level naik 1, broadcast ke nation, audit ke kas)
 *   • menerapkan modifier permanen yang membutuhkan "apply" eksplisit
 *     (Health Expansion → Attribute.MAX_HEALTH).
 *
 * Riset yang berbasis chance/percentage dibaca on-demand oleh listener;
 * tidak ada modifier yang perlu di-apply lewat manager ini untuk kategori
 * Economy/Combat/Projectile/Mob.
 */
public class ResearchManager {

    private static final String HEALTH_MODIFIER_KEY = "nation_research_health";

    private final NationCore plugin;
    private FileConfiguration researchConfig;

    public ResearchManager(NationCore plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    // ================================================================
    //  Config loading
    // ================================================================

    public void loadConfig() {
        File file = new File(plugin.getDataFolder(), "research.yml");
        if (!file.exists()) {
            plugin.saveResource("research.yml", false);
        }
        researchConfig = YamlConfiguration.loadConfiguration(file);
    }

    public void reloadConfig() {
        loadConfig();
    }

    public boolean isFeatureEnabled() {
        return researchConfig.getBoolean("research.enabled", true);
    }

    private String path(ResearchType type, String key) {
        return "research.types." + type.getId() + "." + key;
    }

    public double getCost(ResearchType type, int targetLevel) {
        if (type == null) return Double.POSITIVE_INFINITY;
        double base = researchConfig.getDouble(path(type, "cost"), type.getBaseCost());
        double scaling = researchConfig.getDouble(path(type, "cost-scaling"), 1.0);
        // targetLevel is 1-based: level 1 costs base, level 2 costs base*scaling, etc.
        double multiplier = Math.pow(scaling, Math.max(0, targetLevel - 1));
        return base * multiplier;
    }

    public long getDurationMillis(ResearchType type, int targetLevel) {
        if (type == null) return 0L;
        int baseSec = researchConfig.getInt(path(type, "time-seconds"), type.getBaseDurationSeconds());
        double scaling = researchConfig.getDouble(path(type, "time-scaling"), 1.0);
        double multiplier = Math.pow(scaling, Math.max(0, targetLevel - 1));
        return Math.round(baseSec * multiplier * 1000L);
    }

    public int getMaxLevel(ResearchType type) {
        if (type == null) return 0;
        return researchConfig.getInt(path(type, "max-level"), type.getMaxLevel());
    }

    public double getEffectPerLevel(ResearchType type) {
        if (type == null) return 0.0;
        return researchConfig.getDouble(path(type, "effect-per-level"), type.getEffectPerLevel());
    }

    public boolean isTypeEnabled(ResearchType type) {
        if (type == null) return false;
        return researchConfig.getBoolean(path(type, "enabled"), true);
    }

    public double getCancelRefundPercent() {
        return researchConfig.getDouble("research.cancel-refund-percent", 50.0);
    }

    // ================================================================
    //  Pre-condition checks & start
    // ================================================================

    /** Result type with a small payload so callers can show a friendly message. */
    public static class ActionResult {
        public final boolean ok;
        public final String message;

        private ActionResult(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }

        public static ActionResult ok(String msg) { return new ActionResult(true, msg); }
        public static ActionResult fail(String msg) { return new ActionResult(false, msg); }
    }

    public boolean isLeader(Nation nation, UUID uuid) {
        if (nation == null || uuid == null) return false;
        return uuid.equals(nation.getLeaderUUID());
    }

    public ActionResult startResearch(Player initiator, Nation nation, ResearchType type) {
        if (!isFeatureEnabled()) {
            return ActionResult.fail("The research system is currently disabled.");
        }
        if (initiator == null || nation == null || type == null) {
            return ActionResult.fail("Invalid request.");
        }
        if (!isTypeEnabled(type)) {
            return ActionResult.fail("This research has been disabled by the server administrator.");
        }
        if (!isLeader(nation, initiator.getUniqueId()) && !initiator.hasPermission("nation.admin")) {
            return ActionResult.fail("Only the nation leader can start a research project.");
        }

        NationResearchData data = nation.getResearchData();
        if (data.hasActive()) {
            return ActionResult.fail("Your nation already has an active research project. Wait for it to finish or cancel it first.");
        }

        int currentLevel = data.getLevel(type);
        int maxLevel = getMaxLevel(type);
        if (currentLevel >= maxLevel) {
            return ActionResult.fail("This research has already reached its maximum level (" + maxLevel + ").");
        }

        int targetLevel = currentLevel + 1;
        double cost = getCost(type, targetLevel);
        Treasury treasury = nation.getTreasury();
        if (treasury.getBalance() < cost) {
            return ActionResult.fail("The national treasury cannot afford this research. Required: $"
                    + MessageUtils.formatNumber(cost));
        }

        // Withdraw cost via TreasuryManager so the transaction is logged.
        boolean withdrawn = plugin.getTreasuryManager().withdraw(
                nation,
                Treasury.TransactionType.MISC_EXPENSE,
                cost,
                "Research: " + type.getDisplayName() + " — Level " + targetLevel,
                initiator.getUniqueId());
        if (!withdrawn) {
            return ActionResult.fail("Treasury withdrawal failed. Try again later.");
        }
        data.addVaultSpent(cost);

        long now = System.currentTimeMillis();
        long ends = now + getDurationMillis(type, targetLevel);
        ActiveResearch active = new ActiveResearch(type, targetLevel, now, ends, cost,
                initiator.getUniqueId(), initiator.getName());
        data.setActive(active);

        plugin.getDataManager().saveNations();

        MessageUtils.sendToNation(nation,
                "<gray>📚 Research started: <yellow>" + type.getDisplayName() + " <gray>(Level <white>"
                        + targetLevel + "</white>) by <gold>" + initiator.getName() + "</gold>.");
        return ActionResult.ok("Research started successfully.");
    }

    public ActionResult cancelResearch(Player initiator, Nation nation) {
        if (initiator == null || nation == null) return ActionResult.fail("Invalid request.");
        if (!isLeader(nation, initiator.getUniqueId()) && !initiator.hasPermission("nation.admin")) {
            return ActionResult.fail("Only the nation leader can cancel a research project.");
        }
        NationResearchData data = nation.getResearchData();
        if (!data.hasActive()) {
            return ActionResult.fail("There is no active research to cancel.");
        }
        ActiveResearch active = data.getActive();
        double refundPct = Math.max(0, Math.min(100, getCancelRefundPercent())) / 100.0;
        double refund = active.getPaidCost() * refundPct;
        if (refund > 0) {
            plugin.getTreasuryManager().deposit(
                    nation,
                    Treasury.TransactionType.DEPOSIT_REFUND,
                    refund,
                    "Research refund: " + active.getType().getDisplayName(),
                    initiator.getUniqueId());
        }
        data.setActive(null);
        plugin.getDataManager().saveNations();

        MessageUtils.sendToNation(nation,
                "<gray>📚 Research cancelled: <yellow>" + active.getType().getDisplayName()
                        + "</yellow>. Refunded <gold>$" + MessageUtils.formatNumber(refund) + "</gold> to the treasury.");
        return ActionResult.ok("Research cancelled. Refund issued: $" + MessageUtils.formatNumber(refund));
    }

    // ================================================================
    //  Tick / completion
    // ================================================================

    /** Called on a fixed schedule (every minute) to finalize finished research. */
    public void tick() {
        if (!isFeatureEnabled()) return;
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            NationResearchData data = nation.getResearchData();
            if (!data.hasActive()) continue;
            ActiveResearch active = data.getActive();
            if (!active.isFinished()) continue;
            completeResearch(nation, active);
        }
    }

    private void completeResearch(Nation nation, ActiveResearch active) {
        ResearchType type = active.getType();
        if (type == null) {
            // Defensive: malformed save. Drop the orphaned project.
            nation.getResearchData().setActive(null);
            return;
        }
        NationResearchData data = nation.getResearchData();
        int newLevel = Math.min(active.getTargetLevel(), getMaxLevel(type));
        data.setLevel(type, newLevel);
        data.setActive(null);
        data.setLastCompletedAt(System.currentTimeMillis());
        data.incrementCompleted();

        plugin.getDataManager().saveNations();

        MessageUtils.sendToNation(nation,
                "<green>✔ Research completed: <yellow>" + type.getDisplayName()
                        + " <green>(Level <white>" + newLevel + "</white>)!</green>");

        // Refresh attribute-based effects for currently online members.
        if (type == ResearchType.HEALTH_EXPANSION_I) {
            for (Player member : Bukkit.getOnlinePlayers()) {
                if (nation.isMember(member.getUniqueId())) {
                    applyHealthAttribute(member, nation);
                }
            }
        }
    }

    // ================================================================
    //  Persistent attribute application — Health Expansion
    // ================================================================

    /**
     * Re-applies the Health Expansion attribute modifier to the player based on
     * their nation's current research level. Safe to call multiple times — old
     * modifiers with our key are removed first to avoid stacking on rejoin.
     */
    public void applyHealthAttribute(Player player, Nation nation) {
        if (player == null) return;
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) return;
        NamespacedKey key = new NamespacedKey(plugin, HEALTH_MODIFIER_KEY);

        // Strip any prior modifier first.
        for (AttributeModifier mod : maxHealth.getModifiers()) {
            if (mod.getKey().equals(key)) {
                maxHealth.removeModifier(mod);
            }
        }

        if (nation == null) return;
        int level = nation.getResearchData().getLevel(ResearchType.HEALTH_EXPANSION_I);
        if (level <= 0) return;

        double bonus = level * getEffectPerLevel(ResearchType.HEALTH_EXPANSION_I);
        if (bonus <= 0) return;

        AttributeModifier modifier = new AttributeModifier(
                key, bonus, AttributeModifier.Operation.ADD_NUMBER);
        maxHealth.addModifier(modifier);
    }

    public void removeHealthAttribute(Player player) {
        if (player == null) return;
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) return;
        NamespacedKey key = new NamespacedKey(plugin, HEALTH_MODIFIER_KEY);
        for (AttributeModifier mod : maxHealth.getModifiers()) {
            if (mod.getKey().equals(key)) {
                maxHealth.removeModifier(mod);
            }
        }
        if (player.getHealth() > maxHealth.getValue()) {
            player.setHealth(maxHealth.getValue());
        }
    }

    // ================================================================
    //  Effect query helpers (consumed by listeners)
    // ================================================================

    /** Total effect = level * per-level magnitude. */
    public double getCumulativeEffect(Nation nation, ResearchType type) {
        if (nation == null || type == null) return 0.0;
        int level = nation.getResearchData().getLevel(type);
        if (level <= 0) return 0.0;
        return level * getEffectPerLevel(type);
    }

    public int getLevel(Nation nation, ResearchType type) {
        if (nation == null || type == null) return 0;
        return nation.getResearchData().getLevel(type);
    }

    public Nation getNationOf(UUID uuid) {
        return plugin.getNationManager().getNationOf(uuid);
    }
}
