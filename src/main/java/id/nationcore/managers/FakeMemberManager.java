package id.nationcore.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import id.nationcore.NationCore;
import id.nationcore.models.FakeMember;
import id.nationcore.models.Nation;
import id.nationcore.models.Nation.NationRole;
import id.nationcore.models.Treasury.TransactionType;
import id.nationcore.utils.MessageUtils;

/**
 * Manager untuk seluruh operasi fake member (NPC) dalam sistem nation.
 *
 * NPC adalah anggota virtual yang:
 *  • terdaftar dalam nation tanpa memiliki tubuh pemain nyata
 *  • membayar pajak melalui saldo simulasi (taxBalance)
 *  • bisa dikick dan diangkat sebagai officer
 *  • menerima benefit dari kas nation
 */
public class FakeMemberManager {

    private final NationCore plugin;

    public FakeMemberManager(NationCore plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------
    // Invite / Create NPC
    // ---------------------------------------------------------------

    /**
     * Mendaftarkan NPC baru ke dalam sebuah nation.
     *
     * @param nationId ID nation tujuan
     * @param npcName  Nama display NPC (unik dalam nation)
     * @return FakeMember yang berhasil dibuat, atau null jika gagal
     *         (alasan kegagalan di-log via MessageUtils)
     */
    public Result inviteNpc(String nationId, String npcName) {
        Nation nation = plugin.getNationManager().getNation(nationId);
        if (nation == null) {
            return Result.fail("Nation with ID '" + nationId + "' not found.");
        }

        if (npcName == null || npcName.isBlank()) {
            return Result.fail("NPC name cannot be empty.");
        }

        // Name can only be alphanumeric + underscore
        if (!npcName.matches("[A-Za-z0-9_]+")) {
            return Result.fail("NPC name can only contain letters, numbers, and underscores.");
        }

        if (npcName.length() < 2 || npcName.length() > 16) {
            return Result.fail("NPC name must be between 2 and 16 characters.");
        }

        // Check duplicate name in the same nation
        if (nation.getFakeMemberByName(npcName) != null) {
            return Result.fail("An NPC with the name '" + npcName + "' already exists in this nation.");
        }

        FakeMember npc = new FakeMember(npcName, nationId);
        nation.addFakeMember(npc);

        plugin.getDataManager().saveNations();

        plugin.getLogger().info("[FakeMember] NPC '" + npcName + "' added to nation '"
                + nation.getName() + "' (UUID: " + npc.getId() + ")");

        return Result.ok("NPC '" + npcName + "' successfully invited to nation " + nation.getName() + ".", npc);
    }

    // ---------------------------------------------------------------
    // Kick NPC
    // ---------------------------------------------------------------

    /**
     * Mengeluarkan NPC dari nation berdasarkan nama NPC.
     */
    public Result kickNpc(String nationId, String npcName) {
        Nation nation = plugin.getNationManager().getNation(nationId);
        if (nation == null) {
            return Result.fail("Nation with ID '" + nationId + "' not found.");
        }

        FakeMember npc = nation.getFakeMemberByName(npcName);
        if (npc == null) {
            return Result.fail("NPC '" + npcName + "' not found in nation " + nation.getName() + ".");
        }

        nation.removeFakeMember(npc.getId());
        plugin.getDataManager().saveNations();

        plugin.getLogger().info("[FakeMember] NPC '" + npcName + "' removed from nation '"
                + nation.getName() + "'");

        return Result.ok("NPC '" + npcName + "' successfully kicked from nation " + nation.getName() + ".", npc);
    }

    /**
     * Overload: kick menggunakan UUID langsung.
     */
    public Result kickNpcByUUID(String nationId, UUID npcUUID) {
        Nation nation = plugin.getNationManager().getNation(nationId);
        if (nation == null) {
            return Result.fail("Nation with ID '" + nationId + "' not found.");
        }

        FakeMember npc = nation.getFakeMember(npcUUID);
        if (npc == null) {
            return Result.fail("NPC with that UUID not found in nation " + nation.getName() + ".");
        }

        nation.removeFakeMember(npcUUID);
        plugin.getDataManager().saveNations();

        return Result.ok("NPC '" + npc.getName() + "' successfully kicked from nation " + nation.getName() + ".", npc);
    }

    // ---------------------------------------------------------------
    // Set Role
    // ---------------------------------------------------------------

    /**
     * Mengubah role NPC. NPC hanya bisa CITIZEN atau OFFICER —
     * LEADER dicadangkan untuk pemain nyata.
     */
    public Result setNpcRole(String nationId, String npcName, NationRole newRole) {
        Nation nation = plugin.getNationManager().getNation(nationId);
        if (nation == null) {
            return Result.fail("Nation with ID '" + nationId + "' not found.");
        }

        if (newRole == NationRole.LEADER) {
            return Result.fail("NPCs cannot be LEADER. Choose CITIZEN or OFFICER.");
        }

        FakeMember npc = nation.getFakeMemberByName(npcName);
        if (npc == null) {
            return Result.fail("NPC '" + npcName + "' not found in nation " + nation.getName() + ".");
        }

        NationRole oldRole = npc.getRole();
        npc.setRole(newRole);
        plugin.getDataManager().saveNations();

        plugin.getLogger().info("[FakeMember] NPC '" + npcName + "' role in nation '"
                + nation.getName() + "' changed from " + oldRole + " to " + newRole);

        return Result.ok("NPC '" + npcName + "' role successfully changed to " + newRole + ".", npc);
    }

    // ---------------------------------------------------------------
    // Tax Collection
    // ---------------------------------------------------------------

    /**
     * Memungut pajak dari satu NPC dan memasukkan hasilnya ke kas nation.
     * Dipanggil oleh TaxManager saat siklus pajak berjalan.
     *
     * @param npc    NPC yang akan dikenai pajak
     * @param nation Nation tempat NPC terdaftar
     * @param amount Total pajak yang harus dibayar
     * @return Jumlah yang benar-benar terbayar (bisa parsial)
     */
    public double collectTax(FakeMember npc, Nation nation, double amount) {
        // Tambahkan hutang lama ke total tagihan
        double totalOwed = amount + npc.getTaxDebt();

        double paid = npc.payTax(totalOwed);

        if (paid > 0) {
            plugin.getTreasuryManager().deposit(
                    nation,
                    TransactionType.TAX_INCOME,
                    paid,
                    "Nation tax from NPC " + npc.getName(),
                    null // tidak ada UUID pemain asli
            );
        }

        if (paid < totalOwed) {
            plugin.getLogger().info("[FakeMember] NPC '" + npc.getName()
                    + "' has insufficient balance. Paid: " + MessageUtils.formatNumber(paid)
                    + " / Total: " + MessageUtils.formatNumber(totalOwed)
                    + " | Debt: " + MessageUtils.formatNumber(npc.getTaxDebt()));
        }

        return paid;
    }

    // ---------------------------------------------------------------
    // Benefit Distribution
    // ---------------------------------------------------------------

    /**
     * Memberikan benefit (stipend/salary) kepada NPC OFFICER dari kas nation.
     * Benefit nyata: mengurangi kas nation dan menambah taxBalance NPC.
     *
     * @param npc    NPC penerima benefit
     * @param nation Nation yang membayar benefit
     * @param amount Jumlah benefit
     * @return true jika kas cukup dan benefit berhasil diberikan
     */
    public boolean applyBenefit(FakeMember npc, Nation nation, double amount) {
        if (nation.getTreasury().getBalance() < amount) {
            plugin.getLogger().info("[FakeMember] Nation treasury of " + nation.getName()
                    + " is insufficient for NPC benefit '" + npc.getName() + "'");
            return false;
        }

        // Kurangi kas nation
        plugin.getTreasuryManager().withdraw(
                nation,
                TransactionType.MISC_EXPENSE,
                amount,
                "Benefit untuk NPC " + npc.getName(),
                null
        );

        // Tambah saldo simulasi NPC
        npc.addBalance(amount);

        plugin.getDataManager().saveNations();

        plugin.getLogger().info("[FakeMember] NPC '" + npc.getName()
                + "' received benefit $" + MessageUtils.formatNumber(amount)
                + " from nation " + nation.getName());

        return true;
    }

    // ---------------------------------------------------------------
    // Query helpers
    // ---------------------------------------------------------------

    /**
     * Mendapatkan semua NPC dalam sebuah nation.
     */
    public List<FakeMember> getAllNpcsInNation(Nation nation) {
        return new ArrayList<>(nation.getAllFakeMembers());
    }

    /**
     * Mendapatkan semua NPC di semua nation.
     */
    public List<FakeMember> getAllNpcs() {
        List<FakeMember> all = new ArrayList<>();
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            all.addAll(nation.getAllFakeMembers());
        }
        return all;
    }

    /**
     * Mencari NPC berdasarkan UUID-nya di seluruh nation.
     * Mengembalikan null jika tidak ditemukan.
     */
    public FakeMember findNpcByUUID(UUID uuid) {
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            FakeMember npc = nation.getFakeMember(uuid);
            if (npc != null) return npc;
        }
        return null;
    }

    /**
     * Mendapatkan nation tempat sebuah NPC terdaftar.
     * Mengembalikan null jika NPC tidak ditemukan di mana pun.
     */
    public Nation getNationOfNpc(UUID npcUUID) {
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            if (nation.isFakeMember(npcUUID)) return nation;
        }
        return null;
    }

    // ---------------------------------------------------------------
    // Result type
    // ---------------------------------------------------------------

    /** Hasil operasi FakeMemberManager. */
    public static class Result {
        private final boolean success;
        private final String message;
        private final FakeMember npc;

        private Result(boolean success, String message, FakeMember npc) {
            this.success = success;
            this.message = message;
            this.npc     = npc;
        }

        public static Result ok(String message, FakeMember npc) {
            return new Result(true, message, npc);
        }

        public static Result fail(String message) {
            return new Result(false, message, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public FakeMember getNpc()  { return npc; }
    }
}
