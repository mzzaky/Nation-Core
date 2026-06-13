package id.nationcore.gui.monarchy;

import org.bukkit.Material;
import id.nationcore.NationCore;
import id.nationcore.gui.AbstractSettingsMenu;

public class MonarchySettingsMenu extends AbstractSettingsMenu {

    public static final String TITLE = "§6§l⚙ Monarchy Settings";

    public MonarchySettingsMenu(NationCore plugin) {
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
}
