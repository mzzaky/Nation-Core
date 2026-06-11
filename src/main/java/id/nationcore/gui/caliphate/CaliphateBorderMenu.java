package id.nationcore.gui.caliphate;

import org.bukkit.Material;

import id.nationcore.NationCore;
import id.nationcore.gui.AbstractBorderMenu;
import id.nationcore.models.GovernmentType;

/**
 * Border Management interface for CALIPHATE nations.
 *
 * Themed with the green palette of the Caliphate Court. All behaviour is
 * inherited from {@link AbstractBorderMenu}.
 */
public class CaliphateBorderMenu extends AbstractBorderMenu {

    public static final String TITLE = "§2§lCALIPHATE BORDERS";

    public CaliphateBorderMenu(NationCore plugin) {
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
    public String menuTitle() {
        return TITLE;
    }

    @Override
    protected GovernmentType expectedType() {
        return GovernmentType.CALIPHATE;
    }
}
