package id.nationcore.api;

import java.util.UUID;

import org.bukkit.Location;

import id.nationcore.NationCore;
import id.nationcore.integration.FactoryTaxIntegration;

/**
 * Stable, public integration surface for NationCore.
 *
 * <p>This is the single entry point other plugins use to talk to NationCore. It
 * is deliberately built from only JDK and Bukkit types ({@link Location},
 * {@code double}, {@link UUID}, {@link String}) so a consumer such as
 * FactoryCore can bind to it <em>reflectively</em> &mdash; giving a genuine soft
 * dependency with no shared build artifact and no load-order coupling beyond a
 * {@code softdepend} entry in the consumer's {@code plugin.yml}.
 *
 * <p>Every method is null-safe and never throws: if NationCore is mid-disable or
 * not yet fully initialised, calls simply degrade to "no nation / nothing
 * routed". Keeping this contract frozen is what lets the two plugins version
 * independently.
 *
 * <h2>Reflective usage from a consumer plugin</h2>
 * <pre>{@code
 * Class<?> api = Class.forName("id.nationcore.api.NationCoreAPI");
 * double routed = (double) api
 *     .getMethod("depositFactoryTax", Location.class, double.class, UUID.class, String.class)
 *     .invoke(null, location, amount, payerUuid, "factory-id");
 * }</pre>
 */
public final class NationCoreAPI {

    private NationCoreAPI() {
    }

    private static FactoryTaxIntegration integration() {
        NationCore plugin = NationCore.getInstance();
        return plugin != null ? plugin.getFactoryTaxIntegration() : null;
    }

    /**
     * @return {@code true} when the factory-tax integration is switched on in
     *         NationCore's {@code config.yml} and the plugin is initialised.
     */
    public static boolean isFactoryTaxIntegrationEnabled() {
        try {
            FactoryTaxIntegration i = integration();
            return i != null && i.isEnabled();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * @param loc a world location (typically the centre of a factory region)
     * @return the owning nation's name at {@code loc}, or {@code null} if the
     *         land is unclaimed or NationCore is unavailable.
     */
    public static String getNationNameAt(Location loc) {
        try {
            FactoryTaxIntegration i = integration();
            return i != null ? i.getNationNameAt(loc) : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Routes a factory tax payment into the treasury of the nation that owns the
     * land at {@code loc}.
     *
     * @param loc         representative location of the paying factory
     * @param amount      the gross tax amount the player just paid
     * @param payer       the paying player's UUID (recorded in the ledger; nullable)
     * @param sourceLabel short identifier of the paying factory (for the ledger)
     * @return the amount actually banked into a nation treasury; {@code 0.0} when
     *         nothing was routed. Never throws.
     * @see FactoryTaxIntegration#depositFactoryTax(Location, double, UUID, String)
     */
    public static double depositFactoryTax(Location loc, double amount, UUID payer, String sourceLabel) {
        try {
            FactoryTaxIntegration i = integration();
            return i != null ? i.depositFactoryTax(loc, amount, payer, sourceLabel) : 0.0;
        } catch (Throwable t) {
            return 0.0;
        }
    }
}
