package id.nationcore.gui.communist;

import org.bukkit.Material;
import id.nationcore.NationCore;
import id.nationcore.gui.AbstractSettingsMenu;

public class CommunistSettingsMenu extends AbstractSettingsMenu {

    public static final String TITLE = "§c§l⚙ Communist Settings";

    public CommunistSettingsMenu(NationCore plugin) {
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
}
