package id.nationcore.gui.caliphate;

import org.bukkit.Material;

import id.nationcore.NationCore;
import id.nationcore.gui.AbstractDiplomacyMenu;
import id.nationcore.models.GovernmentType;

/**
 * Diplomacy Management interface for CALIPHATE nations.
 *
 * Themed with the lime palette of the Caliphate Court. All behaviour is
 * inherited from {@link AbstractDiplomacyMenu}; this class only contributes
 * the caliphate-specific styling and the government-type binding used for
 * access validation.
 */
public class CaliphateDiplomacyMenu extends AbstractDiplomacyMenu {

    public static final String MANAGEMENT_TITLE = "§2§lCALIPHATE DIPLOMACY";
    public static final String SELECT_TITLE = "§2§lDIPLOMATIC ENVOY";

    public CaliphateDiplomacyMenu(NationCore plugin) {
        super(plugin);
    }

    @Override
    protected Material fillerMaterial() {
        return Material.LIME_STAINED_GLASS_PANE;
    }

    @Override
    protected String accent() {
        return "§2";
    }

    @Override
    public String managementTitle() {
        return MANAGEMENT_TITLE;
    }

    @Override
    public String selectTitle() {
        return SELECT_TITLE;
    }

    @Override
    protected GovernmentType expectedType() {
        return GovernmentType.CALIPHATE;
    }
}
