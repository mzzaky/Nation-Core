package id.nationcore.integration;

import java.util.Locale;
import java.util.UUID;

import org.bukkit.Location;

import id.nationcore.NationCore;
import id.nationcore.models.Nation;
import id.nationcore.models.Treasury.TransactionType;

/**
 * Server-side handler for the ApartmentCore &harr; NationCore centralized tax
 * integration.
 *
 * <p>ApartmentCore (the consumer) reaches this class indirectly through
 * {@link id.nationcore.api.NationCoreAPI}, which it invokes reflectively so the
 * two plugins never share a compile-time dependency &mdash; a genuine
 * {@code softdepend}. The contract is intentionally narrow: "an apartment whose
 * region sits at {@code loc} just collected {@code grossAmount} in tax from
 * {@code payer}; route the configured share into the treasury of whichever
 * nation owns that land, and report how much was actually banked."
 *
 * <p>All policy (the master enable flag, the revenue share, the per-payment cap)
 * and all of the safety checks live here &mdash; on the side that owns the
 * treasury &mdash; so an outdated or misbehaving consumer can never move more
 * money than the server operator has authorised. This mirrors, and is fully
 * independent from, {@link FactoryTaxIntegration}: apartment and factory tax
 * routing are configured and capped separately.
 */
public class ApartmentTaxIntegration {

    private final NationCore plugin;

    public ApartmentTaxIntegration(NationCore plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------
    // Config helpers
    // ---------------------------------------------------------------

    /** Master switch for the integration on the NationCore side. */
    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("integration.apartment-tax.enabled", true);
    }

    /** Percentage of an apartment tax payment routed to the treasury (clamped to 0-100). */
    private double sharePercent() {
        double pct = plugin.getConfig().getDouble("integration.apartment-tax.share-percent", 100.0);
        if (pct < 0) return 0.0;
        if (pct > 100) return 100.0;
        return pct;
    }

    /** Hard cap on how much a single payment may route ({@code <= 0} disables the cap). */
    private double maxPerPayment() {
        return plugin.getConfig().getDouble("integration.apartment-tax.max-per-payment", 0.0);
    }

    /** Whether each routed payment is written to the server console. */
    private boolean auditLog() {
        return plugin.getConfig().getBoolean("integration.apartment-tax.log", true);
    }

    // ---------------------------------------------------------------
    // Public surface (delegated from NationCoreAPI)
    // ---------------------------------------------------------------

    /**
     * @return the name of the nation that owns the chunk at {@code loc}, or
     *         {@code null} when the land is unclaimed or the input is invalid.
     */
    public String getNationNameAt(Location loc) {
        Nation nation = nationAt(loc);
        return nation != null ? nation.getName() : null;
    }

    /**
     * Routes an apartment tax payment into the owning nation's treasury.
     *
     * @param loc         representative location of the paying apartment (its
     *                    region centre)
     * @param grossAmount the full amount the player just paid in tax
     * @param payer       the player who paid (recorded in the ledger; nullable)
     * @param sourceLabel short identifier of the paying apartment (for the ledger)
     * @return the amount actually deposited into a nation treasury; {@code 0.0}
     *         when the integration is off, the land is unclaimed, or any guard
     *         trips. This method never throws.
     */
    public double depositApartmentTax(Location loc, double grossAmount, UUID payer, String sourceLabel) {
        // Gate 1 — feature must be enabled by the operator.
        if (!isEnabled()) return 0.0;

        // Gate 2 — the amount must be a sane, positive, finite number.
        if (Double.isNaN(grossAmount) || Double.isInfinite(grossAmount) || grossAmount <= 0) {
            return 0.0;
        }

        // Gate 3 — the location must be valid and its world loaded.
        if (loc == null || loc.getWorld() == null) return 0.0;

        // Gate 4 — the land must actually belong to a nation.
        Nation nation = nationAt(loc);
        if (nation == null) return 0.0;

        // Compute the authorised, capped share and strip floating-point dust.
        double amount = grossAmount * (sharePercent() / 100.0);
        double cap = maxPerPayment();
        if (cap > 0 && amount > cap) amount = cap;
        amount = Math.floor(amount * 100.0) / 100.0;
        if (amount <= 0) return 0.0;

        String label = (sourceLabel == null || sourceLabel.isBlank()) ? "apartment" : sourceLabel;
        String description = "Apartment tax (" + label + ")";

        boolean ok;
        try {
            ok = plugin.getTreasuryManager().deposit(
                    nation, TransactionType.TAX_INCOME, amount, description, payer);
        } catch (Exception ex) {
            plugin.getLogger().warning("[ApartmentTax] Failed to deposit routed tax into nation '"
                    + nation.getName() + "': " + ex.getMessage());
            return 0.0;
        }
        if (!ok) return 0.0;

        if (auditLog()) {
            plugin.getLogger().info(String.format(Locale.US,
                    "[ApartmentTax] Routed $%,.2f from apartment '%s' into the treasury of nation '%s'%s.",
                    amount, label, nation.getName(),
                    payer != null ? " (payer " + payer + ")" : ""));
        }
        return amount;
    }

    // ---------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------

    /** Null-safe territory lookup that never propagates an exception. */
    private Nation nationAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        try {
            if (plugin.getTerritoryManager() == null) return null;
            return plugin.getTerritoryManager().getNationAt(loc);
        } catch (Exception ex) {
            return null;
        }
    }
}
