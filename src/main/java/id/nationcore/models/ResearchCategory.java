package id.nationcore.models;

import org.bukkit.Material;

/**
 * Top-level grouping of research projects available to a nation.
 *
 * Each category clusters research types that share thematic effects:
 *   • ECONOMY    — passive drop & yield buffs (farming, animals, mining)
 *   • TECHNOLOGY — character/progression buffs (XP, max HP, hunting damage)
 *   • WAR        — combat buffs (PvP damage, defense, projectile damage)
 */
public enum ResearchCategory {

    ECONOMY("Economy", "&a", Material.WHEAT,
            "Passive yield boosts for farming, animal husbandry, and mining."),
    TECHNOLOGY("Technology", "&b", Material.ENCHANTING_TABLE,
            "Character and progression upgrades (XP, max HP, hunting)."),
    WAR("War", "&c", Material.IRON_SWORD,
            "Combat training that improves damage, defense, and archery.");

    private final String displayName;
    private final String colorCode;
    private final Material icon;
    private final String description;

    ResearchCategory(String displayName, String colorCode, Material icon, String description) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.icon = icon;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getColorCode() { return colorCode; }
    public String getColoredName() { return colorCode + displayName; }
    public Material getIcon() { return icon; }
    public String getDescription() { return description; }
}
