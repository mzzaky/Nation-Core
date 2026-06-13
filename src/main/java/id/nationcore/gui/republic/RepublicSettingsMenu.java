package id.nationcore.gui.republic;

import org.bukkit.Material;
import id.nationcore.NationCore;
import id.nationcore.gui.AbstractSettingsMenu;

public class RepublicSettingsMenu extends AbstractSettingsMenu {

    public static final String TITLE = "§3§l⚙ Republic Settings";

    public RepublicSettingsMenu(NationCore plugin) {
        super(plugin);
    }

    @Override
    protected Material fillerMaterial() {
        return Material.LIGHT_BLUE_STAINED_GLASS_PANE;
    }

    @Override
    protected String accent() {
        return "§b";
    }

    @Override
    public String menuTitle() {
        return TITLE;
    }
}
