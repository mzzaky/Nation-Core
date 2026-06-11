package id.nationcore.gui.monarchy;

import org.bukkit.Material;

import id.nationcore.NationCore;
import id.nationcore.gui.AbstractDiplomacyMenu;
import id.nationcore.models.GovernmentType;

/**
 * Diplomacy Management interface for MONARCHY nations.
 *
 * Themed with the gold palette of the Royal Court. All behaviour is inherited
 * from {@link AbstractDiplomacyMenu}; this class only contributes the
 * monarchy-specific styling and the government-type binding used for access
 * validation.
 */
public class MonarchyDiplomacyMenu extends AbstractDiplomacyMenu {

    public static final String MANAGEMENT_TITLE = "§6§lMONARCHY DIPLOMACY";
    public static final String SELECT_TITLE = "§6§lDIPLOMATIC ENVOY";

    public MonarchyDiplomacyMenu(NationCore plugin) {
        super(plugin);
    }

    @Override
    protected Material fillerMaterial() {
        return Material.YELLOW_STAINED_GLASS_PANE;
    }

    @Override
    protected String accent() {
        return "§6";
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
        return GovernmentType.MONARCHY;
    }
}
