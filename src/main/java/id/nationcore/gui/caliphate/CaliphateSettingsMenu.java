package id.nationcore.gui.caliphate;

import org.bukkit.Material;
import id.nationcore.NationCore;
import id.nationcore.gui.AbstractSettingsMenu;

public class CaliphateSettingsMenu extends AbstractSettingsMenu {

    public static final String TITLE = "§2§l⚙ Caliphate Settings";

    public CaliphateSettingsMenu(NationCore plugin) {
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
}
