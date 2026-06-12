package id.nationcore.gui.republic;

import org.bukkit.Material;

import id.nationcore.NationCore;
import id.nationcore.gui.AbstractTaxMenu;

/**
 * Invoice-based State Tax menu for REPUBLIC nations.
 * Layout and behaviour live in {@link AbstractTaxMenu}; this class only
 * supplies the republic theming.
 */
public class RepublicTaxMenu extends AbstractTaxMenu {

    public static final String TITLE = "§3§l⚖ State Tax Office";

    public RepublicTaxMenu(NationCore plugin) {
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

    @Override
    protected String taxTerm() {
        return "Tax";
    }

    @Override
    protected String backTargetName() {
        return "Republic Council";
    }
}
