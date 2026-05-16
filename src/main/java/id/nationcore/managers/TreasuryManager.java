package id.nationcore.managers;

import id.nationcore.NationCore;
import id.nationcore.models.Nation;
import id.nationcore.models.Treasury;
import id.nationcore.models.Treasury.Transaction;
import id.nationcore.models.Treasury.TransactionType;

import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

/**
 * Treasury manager — context-aware sejak Phase 2.
 *
 * Setiap method punya tiga varian:
 *   • {@code getTreasury()} dst → akses kas global legacy (sistem v1.x).
 *   • {@code getTreasury(Nation)} → akses kas suatu nation.
 *   • {@code getTreasury(Player)} → resolve nation pemain dulu, baru akses
 *     kas-nya. Bila pemain belum punya nation, fallback ke kas global.
 *
 * Method tanpa parameter dipertahankan agar callsite lama tetap berjalan.
 * Saat semua callsite sudah migrate ke versi context-aware, varian legacy
 * dapat dihapus.
 */
public class TreasuryManager {

    private final NationCore plugin;

    public TreasuryManager(NationCore plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------
    // Legacy single-global API (backward compat)
    // ---------------------------------------------------------------

    public Treasury getTreasury() {
        return plugin.getDataManager().getTreasury();
    }

    public double getBalance() {
        return getTreasury().getBalance();
    }

    public boolean canAfford(double amount) {
        return getTreasury().canAfford(amount);
    }

    public boolean deposit(TransactionType type, double amount, String description, UUID player) {
        return getTreasury().deposit(type, amount, description, player);
    }

    public boolean withdraw(TransactionType type, double amount, String description, UUID player) {
        return getTreasury().withdraw(type, amount, description, player);
    }

    public void addTax(double originalAmount, UUID player) {
        double taxRate = plugin.getConfig().getDouble("treasury.transaction-tax", 0.05);
        double tax = originalAmount * taxRate;
        deposit(TransactionType.TAX_INCOME, tax, "Transaction tax", player);
    }

    public List<Transaction> getRecentTransactions(int count) {
        return getTreasury().getRecentTransactions(count);
    }

    public double getTotalIncome() {
        return getTreasury().getTotalIncome();
    }

    public double getTotalExpenses() {
        return getTreasury().getTotalExpenses();
    }

    // ---------------------------------------------------------------
    // Per-nation API (Phase 2)
    // ---------------------------------------------------------------

    public Treasury getTreasury(Nation nation) {
        return nation != null ? nation.getTreasury() : getTreasury();
    }

    public Treasury getTreasury(Player player) {
        if (player == null) return getTreasury();
        Nation n = plugin.getNationManager().getNationOf(player.getUniqueId());
        return n != null ? n.getTreasury() : getTreasury();
    }

    public double getBalance(Nation nation) {
        return getTreasury(nation).getBalance();
    }

    public boolean canAfford(Nation nation, double amount) {
        return getTreasury(nation).canAfford(amount);
    }

    public boolean deposit(Nation nation, TransactionType type, double amount,
                           String description, UUID player) {
        return getTreasury(nation).deposit(type, amount, description, player);
    }

    public boolean withdraw(Nation nation, TransactionType type, double amount,
                            String description, UUID player) {
        return getTreasury(nation).withdraw(type, amount, description, player);
    }

    public void addTax(Nation nation, double originalAmount, UUID player) {
        double taxRate = plugin.getConfig().getDouble("treasury.transaction-tax", 0.05);
        double tax = originalAmount * taxRate;
        deposit(nation, TransactionType.TAX_INCOME, tax, "Transaction tax", player);
    }
}
