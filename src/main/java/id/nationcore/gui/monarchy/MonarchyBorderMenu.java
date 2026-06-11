package id.nationcore.gui.monarchy;

import org.bukkit.Material;

import id.nationcore.NationCore;
import id.nationcore.gui.AbstractBorderMenu;
import id.nationcore.models.GovernmentType;

/**
 * Border Management interface for MONARCHY nations.
 *
 * Themed with the gold/yellow palette of the Royal Court. All behaviour is
 * inherited from {@link AbstractBorderMenu}.
 */
public class MonarchyBorderMenu extends AbstractBorderMenu {

    public static final String TITLE = "§6§lKINGDOM BORDERS";

    public MonarchyBorderMenu(NationCore plugin) {
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
    public String menuTitle() {
        return TITLE;
    }

    @Override
    protected GovernmentType expectedType() {
        return GovernmentType.MONARCHY;
    }
}
