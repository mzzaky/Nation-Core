package id.nationcore.models;

import id.nationcore.models.CommunistGovernment.PolitburoPosition;

/**
 * 20 decisions executive khusus pemerintahan Komunis (Phase 4C).
 *
 *   • PROPAGANDA  (5) — Siaran Global, Siaran Nasional, Glorifikasi Pemimpin,
 *                       Sensor Media, Mobilisasi Propaganda
 *   • DEFENSE     (5) — Declare War, Military Draft, Defense Protocol,
 *                       Offense Protocol, Military Emergency
 *   • TREASURY    (5) — Distribution Program, Economic Stimulus, Education
 *                       Program, Tax Intensification, Market Event
 *   • HEALTH      (5) — Quarantine Protocol, Field Medicine, Vaccination
 *                       Drive, Emergency Rations, Plague
 *
 * Cooldown decision per-pemain mirip Republic's CabinetDecision (cross-nation).
 * Phase 4C menggunakan {@code CommunistManager.decisionCooldowns} untuk tracking.
 */
public enum CommunistDecisionType {

    // ========================================================
    // PROPAGANDA
    // ========================================================
    PROP_GLOBAL_BROADCAST(
            PolitburoPosition.PROPAGANDA,
            "Siaran Global",
            "Pesan broadcast warna merah ke seluruh server atas nama negara.",
            50_000, 0L),

    PROP_NATIONAL_BROADCAST(
            PolitburoPosition.PROPAGANDA,
            "Siaran Nasional",
            "Pesan sistem ke setiap anggota nation.",
            50_000, 0L),

    PROP_LEADER_GLORIFICATION(
            PolitburoPosition.PROPAGANDA,
            "Glorifikasi Pemimpin",
            "Anggota Partai mendapat Strength I + Resistance I selama 30 menit.",
            300_000, 30L * 60 * 1000),

    PROP_MEDIA_CENSORSHIP(
            PolitburoPosition.PROPAGANDA,
            "Sensor Media",
            "Mengganti pesan pemain di global chat dengan kalimat tertentu " +
                    "(satu pesan per anggota target).",
            250_000, 24L * 60 * 60 * 1000),

    PROP_MOBILIZATION(
            PolitburoPosition.PROPAGANDA,
            "Mobilisasi Propaganda",
            "Memanggil paksa seluruh anggota nation ke lokasi menteri.",
            150_000, 0L),

    // ========================================================
    // DEFENSE
    // ========================================================
    DEF_DECLARE_WAR(
            PolitburoPosition.DEFENSE,
            "Declare War",
            "Memulai perang dengan nation lain (akan fully functional di Phase 5).",
            50_000, 0L),

    DEF_MILITARY_DRAFT(
            PolitburoPosition.DEFENSE,
            "Military Draft",
            "Memberikan semua anggota nation peralatan perang dasar.",
            50_000, 0L),

    DEF_DEFENSE_PROTOCOL(
            PolitburoPosition.DEFENSE,
            "Defense Protocol",
            "Atribut -20% damage taken ke semua anggota nation selama 30 menit.",
            50_000, 30L * 60 * 1000),

    DEF_OFFENSE_PROTOCOL(
            PolitburoPosition.DEFENSE,
            "Offense Protocol",
            "Atribut +20% damage dealt ke semua anggota nation selama 30 menit.",
            50_000, 30L * 60 * 1000),

    DEF_MILITARY_EMERGENCY(
            PolitburoPosition.DEFENSE,
            "Military Emergency",
            "Reveal koordinat penyusup di teritori (glow + Weakness II selama 5 menit).",
            50_000, 5L * 60 * 1000),

    // ========================================================
    // TREASURY
    // ========================================================
    TRE_DISTRIBUTION_PROGRAM(
            PolitburoPosition.TREASURY,
            "Distribution Program",
            "Membebaskan pajak Komunis selama 3 fase ke depan.",
            50_000, 0L),

    TRE_ECONOMIC_STIMULUS(
            PolitburoPosition.TREASURY,
            "Economic Stimulus",
            "Memberikan $10.000 ke semua anggota nation.",
            50_000, 0L),

    TRE_EDUCATION_PROGRAM(
            PolitburoPosition.TREASURY,
            "Education Program",
            "Meningkatkan 10 level XP ke semua anggota nation.",
            50_000, 0L),

    TRE_TAX_INTENSIFICATION(
            PolitburoPosition.TREASURY,
            "Tax Intensification",
            "Menggandakan pajak Komunis (200%) selama 3 fase ke depan.",
            50_000, 0L),

    TRE_MARKET_EVENT(
            PolitburoPosition.TREASURY,
            "Market Event",
            "Setiap transaksi villager memberi anggota bonus $25 selama 30 menit.",
            50_000, 30L * 60 * 1000),

    // ========================================================
    // HEALTH
    // ========================================================
    HEA_QUARANTINE_PROTOCOL(
            PolitburoPosition.HEALTH,
            "Quarantine Protocol",
            "Menutup akses masuk teritori (non-anggota tidak bisa masuk) selama 10 menit.",
            500_000, 10L * 60 * 1000),

    HEA_FIELD_MEDICINE(
            PolitburoPosition.HEALTH,
            "Field Medicine",
            "Anggota mendapat Regeneration II selama 5 menit. Cooldown 2 jam.",
            500_000, 5L * 60 * 1000),

    HEA_VACCINATION_DRIVE(
            PolitburoPosition.HEALTH,
            "Vaccination Drive",
            "Anggota kebal poison & wither selama 1 jam.",
            500_000, 60L * 60 * 1000),

    HEA_EMERGENCY_RATIONS(
            PolitburoPosition.HEALTH,
            "Emergency Rations",
            "Bagikan makanan dari kas ke anggota dengan hunger bar di bawah 50%.",
            500_000, 0L),

    HEA_PLAGUE(
            PolitburoPosition.HEALTH,
            "Plague",
            "Musuh masuk teritori dapat Weakness II + Hunger selama 30 detik. Aktif 10 menit.",
            500_000, 10L * 60 * 1000);

    private final PolitburoPosition position;
    private final String displayName;
    private final String description;
    private final int cost;
    /** Durasi efek dalam ms; 0 berarti instant (tidak ada state tracking). */
    private final long durationMillis;

    CommunistDecisionType(PolitburoPosition position, String displayName,
                          String description, int cost, long durationMillis) {
        this.position = position;
        this.displayName = displayName;
        this.description = description;
        this.cost = cost;
        this.durationMillis = durationMillis;
    }

    public PolitburoPosition getPosition() { return position; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getCost() { return cost; }
    public long getDurationMillis() { return durationMillis; }
    public boolean isInstant() { return durationMillis == 0; }
}
