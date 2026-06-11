package id.nationcore.gui.republic;

import org.bukkit.Material;

import id.nationcore.NationCore;
import id.nationcore.gui.AbstractDiplomacyMenu;
import id.nationcore.models.GovernmentType;

/**
 * Diplomacy Management interface for REPUBLIC nations.
 *
 * Themed with the light-blue palette of the Republic Government. All behaviour
 * is inherited from {@link AbstractDiplomacyMenu}; this class only contributes
 * the republic-specific styling and the government-type binding used for
 * access validation.
 */
public class RepublicDiplomacyMenu extends AbstractDiplomacyMenu {

    public static final String MANAGEMENT_TITLE = "§9§lREPUBLIC DIPLOMACY";
    public static final String SELECT_TITLE = "§9§lDIPLOMATIC ENVOY";

    public RepublicDiplomacyMenu(NationCore plugin) {
        super(plugin);
    }

    @Override
    protected Material fillerMaterial() {
        return Material.LIGHT_BLUE_STAINED_GLASS_PANE;
    }

    @Override
    protected String accent() {
        return "§9";
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
        return GovernmentType.REPUBLIC;
    }
}
