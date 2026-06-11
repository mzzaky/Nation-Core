package id.nationcore.gui.communist;

import org.bukkit.Material;

import id.nationcore.NationCore;
import id.nationcore.gui.AbstractBorderMenu;
import id.nationcore.models.GovernmentType;

/**
 * Border Management interface for COMMUNIST nations.
 *
 * Themed with the red palette of the Communist Government. All behaviour is
 * inherited from {@link AbstractBorderMenu}.
 */
public class CommunistBorderMenu extends AbstractBorderMenu {

    public static final String TITLE = "§c§lCOMMUNIST BORDERS";

    public CommunistBorderMenu(NationCore plugin) {
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
    public String menuTitle() {
        return TITLE;
    }

    @Override
    protected GovernmentType expectedType() {
        return GovernmentType.COMMUNIST;
    }
}
