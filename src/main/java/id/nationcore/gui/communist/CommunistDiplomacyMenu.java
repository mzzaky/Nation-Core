package id.nationcore.gui.communist;

import org.bukkit.Material;

import id.nationcore.NationCore;
import id.nationcore.gui.AbstractDiplomacyMenu;
import id.nationcore.models.GovernmentType;

/**
 * Diplomacy Management interface for COMMUNIST nations.
 *
 * Themed with the red palette of the Communist Government. All behaviour is
 * inherited from {@link AbstractDiplomacyMenu}; this class only contributes
 * the communist-specific styling and the government-type binding used for
 * access validation.
 */
public class CommunistDiplomacyMenu extends AbstractDiplomacyMenu {

    public static final String MANAGEMENT_TITLE = "§c§lCOMMUNIST DIPLOMACY";
    public static final String SELECT_TITLE = "§c§lDIPLOMATIC ENVOY";

    public CommunistDiplomacyMenu(NationCore plugin) {
        super(plugin);
    }

    @Override
    protected Material fillerMaterial() {
        return Material.RED_STAINED_GLASS_PANE;
    }

    @Override
    protected String accent() {
        return "§c";
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
        return GovernmentType.COMMUNIST;
    }
}
