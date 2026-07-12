package id.nationcore.models;

import org.bukkit.Material;

/**
 * Daftar jenis pemerintahan yang tersedia di Nation Core.
 *
 * REPUBLIC  : sistem demokrasi (mantan DemocracyCore) — pemilu, kabinet,
 *             approval rate, recall, transparansi.
 * COMMUNIST : sistem partai tunggal — Sekretaris Jenderal & Politbiro,
 *             pajak progresif, subsidi harian, kontrol penuh atas anggota.
 * MONARCHY  : sistem kerajaan absolut — King memimpin selamanya, High Council
 *             (Chancellor / Marshal / Saint / Herald) menjalankan order, tidak
 *             ada pemilu maupun cooldown executive order.
 * CALIPHATE : sistem khilafah — Caliph memimpin selamanya tanpa kabinet/menteri.
 *             Shura Council (3) & State Scholars (5) hanya badan penasihat
 *             tanpa kewenangan order; hanya executive order Caliph yang aktif.
 */
public enum GovernmentType {

    REPUBLIC(
            "Republic",
            "&b",
            Material.PAPER,
            "Democratic system: regular elections, transparent cabinet,",
            new String[] {
                    "&7• Leader: &fPresident (elected)",
                    "&7• Ministers: &fEconomy, Defense, Social",
                    "&7• Buff: &fEXP/Money bonus from citizen approval",
                    "&7• Treasury: &fTransparent, public logs, anti-corruption",
                    "&7• Recall: &fActive — citizens can overthrow president"
            }),

    COMMUNIST(
            "Communist",
            "&c",
            Material.RED_BANNER,
            "Single-party system: power centralized in Politburo,",
            new String[] {
                    "&7• Leader: &fSecretary General (party elected)",
                    "&7• Ministers: &fPropaganda, Defense, Production",
                    "&7• Buff: &fDaily free money & EXP for all members",
                    "&7• Treasury: &fProgressive tax, automatic subsidies",
                    "&7• Order: &fNationalization, Media Censorship, Gulag"
            }),

    MONARCHY(
            "Monarchy",
            "&e",
            Material.GOLDEN_HORSE_ARMOR,
            "Absolute kingdom: the King rules forever with unchecked power,",
            new String[] {
                    "&7• Leader: &fKing (rules for life, no elections)",
                    "&7• High Council: &fChancellor, Marshal, Saint, Herald",
                    "&7• Power: &fAbsolute — King may issue any order freely",
                    "&7• Treasury: &fRoyal Treasury controlled by Chancellor",
                    "&7• Order: &fNo cooldown for the King's executive orders"
            }),

    CALIPHATE(
            "Caliphate",
            "&2",
            Material.TURTLE_HELMET,
            "Theocratic caliphate: the Caliph leads for life with no cabinet,",
            new String[] {
                    "&7• Leader: &fCaliph (rules for life, no elections)",
                    "&7• Advisory: &fShura Council (3), State Scholars (5)",
                    "&7• Cabinet: &fNone — no ministers, no minister orders",
                    "&7• Treasury: &fBayt al-Mal funded by Jizya tax",
                    "&7• Order: &fOnly the Caliph's executive orders apply"
            });

    private final String displayName;
    private final String colorCode;
    private final Material iconMaterial;
    private final String shortDescription;
    private final String[] highlights;

    GovernmentType(String displayName, String colorCode, Material iconMaterial,
                   String shortDescription, String[] highlights) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.iconMaterial = iconMaterial;
        this.shortDescription = shortDescription;
        this.highlights = highlights;
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

    public Material getIconMaterial() {
        return iconMaterial;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String[] getHighlights() {
        return highlights;
    }

    /**
     * Sebutan untuk pemimpin negara — dipakai pada title/chat/GUI.
     */
    public String getLeaderTitle() {
        return switch (this) {
            case REPUBLIC -> "President";
            case COMMUNIST -> "Secretary General";
            case MONARCHY -> "King";
            case CALIPHATE -> "Caliph";
        };
    }

    /**
     * Collective term for ordinary members.
     */
    public String getCitizenTitle() {
        return switch (this) {
            case REPUBLIC -> "Citizen";
            case COMMUNIST -> "Party Member";
            case MONARCHY -> "Royal Subject";
            case CALIPHATE -> "Citizen";
        };
    }

    /**
     * Parse string dengan toleransi case & alias (mis. "republik", "republic", "komunis", "communist").
     * Mengembalikan null jika tidak dikenali.
     */
    public static GovernmentType fromString(String input) {
        if (input == null) return null;
        String key = input.trim().toLowerCase();
        return switch (key) {
            case "republic", "republik" -> REPUBLIC;
            case "communist", "komunis", "communism", "komunisme" -> COMMUNIST;
            case "monarchy", "monarki", "kingdom", "kerajaan" -> MONARCHY;
            case "caliphate", "khilafah", "kalifah", "caliph" -> CALIPHATE;
            default -> null;
        };
    }

    public String getConfigFileName() {
        return switch (this) {
            case REPUBLIC -> "republic.yaml";
            case COMMUNIST -> "comunist.yaml";
            case MONARCHY -> "monarcy.yaml";
            case CALIPHATE -> "caliphate.yaml";
        };
    }
}
