package id.nationcore.models;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Entitas "fake member" (NPC) yang terdaftar sebagai anggota sebuah nation
 * tanpa memiliki tubuh pemain nyata di server.
 *
 * NPC:
 *  • ikut dihitung dalam jumlah anggota nation
 *  • bisa dikick, ditunjuk sebagai officer
 *  • membayar pajak melalui saldo simulasi (taxBalance)
 *  • mendapatkan benefit dari nation (dicatat/dipotong dari kas)
 *
 * UUID NPC dibuat deterministik dari "npc:<name>" sehingga tidak
 * akan bertabrakan dengan UUID pemain asli (yang berasal dari Mojang).
 */
public class FakeMember {

    /** Prefix tetap agar UUID NPC mudah diidentifikasi. */
    public static final String NPC_UUID_NAMESPACE = "npc:";

    private UUID id;
    private String name;
    private String nationId;
    private Nation.NationRole role;
    private long joinedAt;

    /**
     * Saldo simulasi yang digunakan untuk membayar pajak nation.
     * Default 10.000 — cukup untuk beberapa siklus pajak awal.
     */
    private double taxBalance;

    /** Total pajak yang sudah dibayarkan NPC ini sepanjang hidupnya. */
    private double totalTaxPaid;

    /** Total hutang pajak NPC (jika taxBalance tidak cukup). */
    private double taxDebt;

    /** Jumlah siklus pajak yang dilewati tanpa membayar penuh. */
    private int missedTaxCycles;

    public FakeMember() {
        // default constructor untuk Gson deserialization
    }

    /**
     * Buat FakeMember baru dengan saldo awal 10.000.
     *
     * @param name     Nama display NPC (harus unik dalam satu nation)
     * @param nationId ID nation tempat NPC ini terdaftar
     */
    public FakeMember(String name, String nationId) {
        this.id       = generateNpcUUID(name);
        this.name     = name;
        this.nationId = nationId;
        this.role     = Nation.NationRole.CITIZEN;
        this.joinedAt = System.currentTimeMillis();
        this.taxBalance   = 10_000.0;
        this.totalTaxPaid = 0;
        this.taxDebt      = 0;
        this.missedTaxCycles = 0;
    }

    // ---------------------------------------------------------------
    // Static helpers
    // ---------------------------------------------------------------

    /**
     * Membuat UUID deterministik dari nama NPC menggunakan
     * UUID.nameUUIDFromBytes (type 3 / MD5). Hasil selalu sama
     * untuk nama yang sama dan tidak pernah bentrok dengan UUID
     * pemain Minecraft asli (yang berjenis type 4 / random).
     */
    public static UUID generateNpcUUID(String name) {
        String raw = NPC_UUID_NAMESPACE + name.toLowerCase();
        return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Menentukan apakah sebuah UUID adalah milik NPC dengan cara
     * memverifikasi bahwa UUID tersebut identik dengan yang akan
     * dihasilkan generateNpcUUID() untuk nama yang sama.
     * Karena UUID type-3, variant bit bisa diperiksa dari versi-nya.
     */
    public static boolean isNpcUUID(UUID uuid) {
        return uuid != null && uuid.version() == 3;
    }

    // ---------------------------------------------------------------
    // Identity
    // ---------------------------------------------------------------

    /** Selalu true — digunakan sebagai flag penanda oleh manager lain. */
    public boolean isNpc() { return true; }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNationId() { return nationId; }
    public void setNationId(String nationId) { this.nationId = nationId; }

    public Nation.NationRole getRole() { return role; }
    public void setRole(Nation.NationRole role) { this.role = role; }

    public long getJoinedAt() { return joinedAt; }
    public void setJoinedAt(long joinedAt) { this.joinedAt = joinedAt; }

    // ---------------------------------------------------------------
    // Tax simulation
    // ---------------------------------------------------------------

    public double getTaxBalance() { return taxBalance; }
    public void setTaxBalance(double taxBalance) { this.taxBalance = taxBalance; }

    public double getTotalTaxPaid() { return totalTaxPaid; }
    public void setTotalTaxPaid(double totalTaxPaid) { this.totalTaxPaid = totalTaxPaid; }

    public double getTaxDebt() { return taxDebt; }
    public void setTaxDebt(double taxDebt) { this.taxDebt = taxDebt; }

    public int getMissedTaxCycles() { return missedTaxCycles; }
    public void setMissedTaxCycles(int missedTaxCycles) { this.missedTaxCycles = missedTaxCycles; }

    /**
     * Membayar sejumlah pajak dari saldo simulasi.
     * Mengembalikan jumlah yang benar-benar berhasil dibayar.
     */
    public double payTax(double amount) {
        if (taxBalance >= amount) {
            taxBalance    -= amount;
            totalTaxPaid  += amount;
            return amount;
        }
        // Bayar sebagian, sisanya jadi hutang
        double paid = taxBalance;
        double debt = amount - paid;
        totalTaxPaid += paid;
        taxBalance    = 0;
        taxDebt      += debt;
        missedTaxCycles++;
        return paid;
    }

    /** Menambahkan saldo simulasi NPC (mis. dari benefit/stipend). */
    public void addBalance(double amount) {
        this.taxBalance += amount;
    }

    /** Melunasi hutang pajak (jika ada saldo). */
    public double payDebt() {
        if (taxDebt <= 0 || taxBalance <= 0) return 0;
        double pay = Math.min(taxDebt, taxBalance);
        taxBalance -= pay;
        taxDebt    = Math.max(0, taxDebt - pay);
        totalTaxPaid += pay;
        return pay;
    }

    public void clearDebt() {
        this.taxDebt = 0;
        this.missedTaxCycles = 0;
    }
}
