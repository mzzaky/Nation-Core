package id.nationcore.models;

import org.bukkit.Material;

/**
 * Catalog of all research projects available in NationCore.
 *
 * Each entry defines its identity (id, display name, category, icon) and the
 * default tuning values used by {@code ResearchManager} when {@code research.yml}
 * is missing or incomplete:
 *   • baseCost       — vault cost of advancing one level
 *   • baseDuration   — research time in seconds
 *   • maxLevel       — maximum attainable level
 *   • effectPerLevel — magnitude added per level (interpretation depends on type)
 *
 * Values defined here are fallbacks. Server owners override them in
 * {@code research.yml} via {@link id.nationcore.managers.ResearchManager}.
 */
public enum ResearchType {

    // ── Era I — Economy ────────────────────────────────────────────────
    AGRICULTURE_PROGRAM_I(
            "agriculture_program_i",
            "Agriculture Program I",
            ResearchCategory.ECONOMY,
            Material.WHEAT,
            25_000.0, 600, 5, 0.01,
            "Adds a chance for crops to drop 1–2 extra items when harvested."),

    HUSBANDRY_BOOST_I(
            "husbandry_boost_i",
            "Husbandry Boost I",
            ResearchCategory.ECONOMY,
            Material.BEEF,
            25_000.0, 600, 5, 0.01,
            "Adds a chance for livestock to drop 1–2 extra items when killed."),

    MINING_LUCK_I(
            "mining_luck_i",
            "Mining Luck I",
            ResearchCategory.ECONOMY,
            Material.DIAMOND_PICKAXE,
            25_000.0, 600, 5, 0.01,
            "Adds a chance to receive 1–2 extra drops when mining."),

    // ── Era I — Technology ─────────────────────────────────────────────
    MAGIC_EDUCATION_I(
            "magic_education_i",
            "Magic Education I",
            ResearchCategory.TECHNOLOGY,
            Material.EXPERIENCE_BOTTLE,
            35_000.0, 1500, 5, 0.01,
            "Increases XP gained from all sources."),

    HEALTH_EXPANSION_I(
            "health_expansion_i",
            "Health Expansion I",
            ResearchCategory.TECHNOLOGY,
            Material.GOLDEN_APPLE,
            48_000.0, 1920, 5, 0.5,
            "Permanently raises the maximum HP of every nation member."),

    HUNTING_SKILL_I(
            "hunting_skill_i",
            "Hunting Skill I",
            ResearchCategory.TECHNOLOGY,
            Material.BONE,
            50_000.0, 2100, 5, 0.01,
            "Increases damage dealt to hostile mobs."),

    // ── Era I — War ────────────────────────────────────────────────────
    ATTACK_EXERCISE_I(
            "attack_exercise_i",
            "Attack Exercise I",
            ResearchCategory.WAR,
            Material.IRON_SWORD,
            100_000.0, 600, 5, 0.01,
            "Increases damage dealt to other players (allies excluded)."),

    DEFENSIVE_TACTIC_I(
            "defensive_tactic_i",
            "Defensive Tactic I",
            ResearchCategory.WAR,
            Material.SHIELD,
            100_000.0, 600, 5, 0.01,
            "Reduces damage taken from other players (allies excluded)."),

    ARCHERY_TRAINING_I(
            "archery_training_i",
            "Archery Training I",
            ResearchCategory.WAR,
            Material.BOW,
            100_000.0, 600, 5, 0.01,
            "Increases damage dealt by bow and crossbow projectiles.");

    private final String id;
    private final String displayName;
    private final ResearchCategory category;
    private final Material icon;
    private final double baseCost;
    private final int baseDurationSeconds;
    private final int maxLevel;
    private final double effectPerLevel;
    private final String description;

    ResearchType(String id, String displayName, ResearchCategory category, Material icon,
                 double baseCost, int baseDurationSeconds, int maxLevel,
                 double effectPerLevel, String description) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.icon = icon;
        this.baseCost = baseCost;
        this.baseDurationSeconds = baseDurationSeconds;
        this.maxLevel = maxLevel;
        this.effectPerLevel = effectPerLevel;
        this.description = description;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public ResearchCategory getCategory() { return category; }
    public Material getIcon() { return icon; }
    public double getBaseCost() { return baseCost; }
    public int getBaseDurationSeconds() { return baseDurationSeconds; }
    public int getMaxLevel() { return maxLevel; }
    public double getEffectPerLevel() { return effectPerLevel; }
    public String getDescription() { return description; }

    public static ResearchType fromId(String id) {
        if (id == null) return null;
        for (ResearchType t : values()) {
            if (t.id.equalsIgnoreCase(id)) return t;
        }
        return null;
    }
}
