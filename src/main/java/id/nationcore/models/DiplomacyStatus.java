package id.nationcore.models;

public enum DiplomacyStatus {
    PEACE("Damai", "&a", "Status default, tidak bisa PvP di wilayah satu sama lain."),
    ALLIANCE("Sekutu", "&b", "Tidak bisa saling serang dan saling membantu saat diserang."),
    TRUCE("Gencatan Senjata", "&e", "Berhenti saling serang setelah perang untuk waktu tertentu."),
    WAR("Perang", "&c", "PvP aktif, mengizinkan penyerangan teritori dan pencaplokan.");

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
