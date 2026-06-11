package id.nationcore.models;

public enum DiplomacyStatus {
    PEACE("Peace", "&a", "Default neutral stance. No PvP within each other's territory."),
    ALLIANCE("Alliance", "&b", "Mutual protection. You cannot attack each other and aid one another when attacked."),
    TRUCE("Truce", "&e", "Temporary ceasefire after a war. Hostilities are paused for a set period."),
    WAR("War", "&c", "Open hostilities. PvP is enabled and territory raids/annexation are allowed.");

    private final String displayName;
    private final String colorCode;
    private final String description;

    DiplomacyStatus(String displayName, String colorCode, String description) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorCode() {
        return colorCode;
    }

    public String getColoredName() {
        return colorCode + displayName;
    }

    public String getDescription() {
        return description;
    }

    public static DiplomacyStatus fromString(String input) {
        if (input == null) return null;
        String key = input.trim().toLowerCase();
        return switch (key) {
            case "peace", "damai" -> PEACE;
            case "alliance", "sekutu", "ally" -> ALLIANCE;
            case "truce", "gencatan senjata", "gencatan" -> TRUCE;
            case "war", "perang" -> WAR;
            default -> null;
        };
    }
}
