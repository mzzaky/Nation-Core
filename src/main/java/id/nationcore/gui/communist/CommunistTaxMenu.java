package id.nationcore.gui.communist;

import org.bukkit.Material;

import id.nationcore.NationCore;
import id.nationcore.gui.AbstractTaxMenu;

/**
 * Invoice-based National Tax menu for COMMUNIST nations.
 * Layout and behaviour live in {@link AbstractTaxMenu}; this class only
 * supplies the communist theming.
 */
public class CommunistTaxMenu extends AbstractTaxMenu {

    public static final String TITLE = "§4§l☭ National Tax Bureau";

    public CommunistTaxMenu(NationCore plugin) {
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
    protected String taxTerm() {
        return "Tax";
    }

    @Override
    protected String backTargetName() {
        return "Politburo Hall";
    }
}
