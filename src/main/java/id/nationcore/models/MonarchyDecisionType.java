package id.nationcore.models;

import id.nationcore.models.MonarchyGovernment.HighCouncilPosition;

/**
 * 20 royal decisions executable by the High Council of a MONARCHY nation.
 *
 * The catalogue mirrors {@link CommunistDecisionType} (the spec requires
 * "mekanisme executive order dan jenis order menteri tetap sama dengan nation
 * komunis") — only the wording is rephrased to fit the kingdom flavour, and
 * each entry is mapped to a {@link HighCouncilPosition} instead of a
 * Politburo seat.
 *
 *   • HERALD     (5) — Royal Proclamation, Royal Decree, Glorify the King,
 *                      Censor the Press, Royal Mobilisation
 *   • MARSHAL    (5) — Declare War, Royal Conscription, Defense Order,
 *                      Offense Order, Military Vigilance
 *   • CHANCELLOR (5) — Royal Almsgiving, Royal Stimulus, Royal Tutelage,
 *                      Tax Intensification, Royal Market Festival
 *   • SAINT      (5) — Royal Quarantine, Royal Healing, Holy Vaccination,
 *                      Royal Provisions, Cursed Plague
 *
 * Cooldown handling is implemented in MonarchyManager and skipped entirely
 * when the issuer is the King (absolute power).
 */
public enum MonarchyDecisionType {

    // ========================================================
    // HERALD (Royal communications)
    // ========================================================
    HER_ROYAL_PROCLAMATION(
            HighCouncilPosition.HERALD,
            "Royal Proclamation",
            "A red broadcast in the King's name to every subject on the server.",
            50_000, 0L),

    HER_ROYAL_DECREE(
            HighCouncilPosition.HERALD,
            "Royal Decree",
            "Send an official decree to every member of the kingdom.",
            50_000, 0L),

    HER_GLORIFY_KING(
            HighCouncilPosition.HERALD,
            "Glorify the King",
            "Royal Soldiers gain Strength I + Resistance I for 30 minutes.",
            300_000, 30L * 60 * 1000),

    HER_CENSOR_PRESS(
            HighCouncilPosition.HERALD,
            "Censor the Press",
            "Replace the next public chat message of every targeted subject (one per subject).",
            250_000, 24L * 60 * 60 * 1000),

    HER_ROYAL_MOBILISATION(
            HighCouncilPosition.HERALD,
            "Royal Mobilisation",
            "Summon every member of the kingdom to the Herald's location.",
            150_000, 0L),

    // ========================================================
    // MARSHAL (Defense)
    // ========================================================
    MAR_DECLARE_WAR(
            HighCouncilPosition.MARSHAL,
            "Declare War",
            "Open hostilities against another nation (full diplomacy in Phase 5).",
            50_000, 0L),

    MAR_ROYAL_CONSCRIPTION(
            HighCouncilPosition.MARSHAL,
            "Royal Conscription",
            "Issue royal armaments to every nation member.",
            50_000, 0L),

    MAR_DEFENSE_ORDER(
            HighCouncilPosition.MARSHAL,
            "Defense Order",
            "Grants every nation member -20% damage taken for 30 minutes.",
            50_000, 30L * 60 * 1000),

    MAR_OFFENSE_ORDER(
            HighCouncilPosition.MARSHAL,
            "Offense Order",
            "Grants every nation member +20% damage dealt for 30 minutes.",
            50_000, 30L * 60 * 1000),

    MAR_MILITARY_VIGILANCE(
            HighCouncilPosition.MARSHAL,
            "Military Vigilance",
            "Reveal trespassers in royal territory (glow + Weakness II for 5 minutes).",
            50_000, 5L * 60 * 1000),

    // ========================================================
    // CHANCELLOR (Treasury)
    // ========================================================
    CHA_ROYAL_ALMSGIVING(
            HighCouncilPosition.CHANCELLOR,
            "Royal Almsgiving",
            "Suspend the royal tax for the next 3 phases.",
            50_000, 0L),

    CHA_ROYAL_STIMULUS(
            HighCouncilPosition.CHANCELLOR,
            "Royal Stimulus",
            "Pay $10,000 from the treasury to every member of the kingdom.",
            50_000, 0L),

    CHA_ROYAL_TUTELAGE(
            HighCouncilPosition.CHANCELLOR,
            "Royal Tutelage",
            "Grants +10 XP levels to every nation member.",
            50_000, 0L),

    CHA_TAX_INTENSIFICATION(
            HighCouncilPosition.CHANCELLOR,
            "Tax Intensification",
            "Double the royal tax (200%) for the next 3 phases.",
            50_000, 0L),

    CHA_ROYAL_MARKET(
            HighCouncilPosition.CHANCELLOR,
            "Royal Market Festival",
            "Every villager trade rewards a $25 bonus to nation members for 30 minutes.",
            50_000, 30L * 60 * 1000),

    // ========================================================
    // SAINT (Health)
    // ========================================================
    SAI_ROYAL_QUARANTINE(
            HighCouncilPosition.SAINT,
            "Royal Quarantine",
            "Seal the borders — non-members cannot enter royal territory for 10 minutes.",
            500_000, 10L * 60 * 1000),

    SAI_ROYAL_HEALING(
            HighCouncilPosition.SAINT,
            "Royal Healing",
            "Grants every nation member Regeneration II for 5 minutes (2h cooldown).",
            500_000, 5L * 60 * 1000),

    SAI_HOLY_VACCINATION(
            HighCouncilPosition.SAINT,
            "Holy Vaccination",
            "Grants every nation member immunity to poison & wither for 1 hour.",
            500_000, 60L * 60 * 1000),

    SAI_ROYAL_PROVISIONS(
            HighCouncilPosition.SAINT,
            "Royal Provisions",
            "Distribute treasury bread to every member whose hunger is below 50%.",
            500_000, 0L),

    SAI_CURSED_PLAGUE(
            HighCouncilPosition.SAINT,
            "Cursed Plague",
            "Enemies in royal territory suffer Weakness II + Hunger for 30 seconds. Active 10 minutes.",
            500_000, 10L * 60 * 1000);

    private final HighCouncilPosition position;
    private final String displayName;
    private final String description;
    private final int cost;
    /** Effect duration in ms; 0 means instant (no state tracking). */
    private final long durationMillis;

    MonarchyDecisionType(HighCouncilPosition position, String displayName,
                         String description, int cost, long durationMillis) {
        this.position = position;
        this.displayName = displayName;
        this.description = description;
        this.cost = cost;
        this.durationMillis = durationMillis;
    }

    public HighCouncilPosition getPosition() { return position; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getCost() { return cost; }
    public long getDurationMillis() { return durationMillis; }
    public boolean isInstant() { return durationMillis == 0; }

    /**
     * Convert this royal decision to its Communist counterpart so existing
     * effect handlers in {@link id.nationcore.managers.CommunistManager} can
     * be reused. Returns null only if the catalogue gets out of sync.
     */
    public CommunistDecisionType asCommunistEquivalent() {
        return switch (this) {
            case HER_ROYAL_PROCLAMATION   -> CommunistDecisionType.PROP_GLOBAL_BROADCAST;
            case HER_ROYAL_DECREE         -> CommunistDecisionType.PROP_NATIONAL_BROADCAST;
            case HER_GLORIFY_KING         -> CommunistDecisionType.PROP_LEADER_GLORIFICATION;
            case HER_CENSOR_PRESS         -> CommunistDecisionType.PROP_MEDIA_CENSORSHIP;
            case HER_ROYAL_MOBILISATION   -> CommunistDecisionType.PROP_MOBILIZATION;
            case MAR_DECLARE_WAR          -> CommunistDecisionType.DEF_DECLARE_WAR;
            case MAR_ROYAL_CONSCRIPTION   -> CommunistDecisionType.DEF_MILITARY_DRAFT;
            case MAR_DEFENSE_ORDER        -> CommunistDecisionType.DEF_DEFENSE_PROTOCOL;
            case MAR_OFFENSE_ORDER        -> CommunistDecisionType.DEF_OFFENSE_PROTOCOL;
            case MAR_MILITARY_VIGILANCE   -> CommunistDecisionType.DEF_MILITARY_EMERGENCY;
            case CHA_ROYAL_ALMSGIVING     -> CommunistDecisionType.TRE_DISTRIBUTION_PROGRAM;
            case CHA_ROYAL_STIMULUS       -> CommunistDecisionType.TRE_ECONOMIC_STIMULUS;
            case CHA_ROYAL_TUTELAGE       -> CommunistDecisionType.TRE_EDUCATION_PROGRAM;
            case CHA_TAX_INTENSIFICATION  -> CommunistDecisionType.TRE_TAX_INTENSIFICATION;
            case CHA_ROYAL_MARKET         -> CommunistDecisionType.TRE_MARKET_EVENT;
            case SAI_ROYAL_QUARANTINE     -> CommunistDecisionType.HEA_QUARANTINE_PROTOCOL;
            case SAI_ROYAL_HEALING        -> CommunistDecisionType.HEA_FIELD_MEDICINE;
            case SAI_HOLY_VACCINATION     -> CommunistDecisionType.HEA_VACCINATION_DRIVE;
            case SAI_ROYAL_PROVISIONS     -> CommunistDecisionType.HEA_EMERGENCY_RATIONS;
            case SAI_CURSED_PLAGUE        -> CommunistDecisionType.HEA_PLAGUE;
        };
    }
}
