package id.nationcore.gui.republic;

import org.bukkit.Material;

import id.nationcore.NationCore;
import id.nationcore.gui.AbstractBorderMenu;
import id.nationcore.models.GovernmentType;

/**
 * Border Management interface for REPUBLIC nations.
 *
 * Themed with the light-blue palette of the Republic Government. All behaviour
 * is inherited from {@link AbstractBorderMenu}; this class only contributes the
 * republic-specific styling and the government-type binding used for access
 * validation.
 */
public class RepublicBorderMenu extends AbstractBorderMenu {

    public static final String TITLE = "§9§lREPUBLIC BORDERS";

    public RepublicBorderMenu(NationCore plugin) {
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
    public String menuTitle() {
        return TITLE;
    }

    @Override
    protected GovernmentType expectedType() {
        return GovernmentType.REPUBLIC;
    }
}
