package id.nationcore.gui.monarchy;

import org.bukkit.Material;

import id.nationcore.NationCore;
import id.nationcore.gui.AbstractTaxMenu;

/**
 * Invoice-based Royal Tax menu for MONARCHY nations.
 * Layout and behaviour live in {@link AbstractTaxMenu}; this class only
 * supplies the monarchy theming.
 */
public class MonarchyTaxMenu extends AbstractTaxMenu {

    public static final String TITLE = "§6§l👑 Royal Tax Office";

    public MonarchyTaxMenu(NationCore plugin) {
        super(plugin);
    }

    @Override
    protected Material fillerMaterial() {
        return Material.YELLOW_STAINED_GLASS_PANE;
    }

    @Override
    protected String accent() {
        return "§e";
    }

    @Override
    public String menuTitle() {
        return TITLE;
    }

    @Override
    protected String taxTerm() {
        return "Tax";
    }

    @Override
    protected String backTargetName() {
        return "Royal Court";
    }
}
