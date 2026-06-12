package id.nationcore.gui.caliphate;

import org.bukkit.Material;

import id.nationcore.NationCore;
import id.nationcore.gui.AbstractTaxMenu;

/**
 * Invoice-based Zakah menu for CALIPHATE nations — the caliphate's take on
 * the nation tax system, named Zakah per specification.
 * Layout and behaviour live in {@link AbstractTaxMenu}; this class only
 * supplies the caliphate theming.
 */
public class CaliphateZakahMenu extends AbstractTaxMenu {

    public static final String TITLE = "§2§l☪ Zakah Office";

    public CaliphateZakahMenu(NationCore plugin) {
        super(plugin);
    }

    @Override
    protected Material fillerMaterial() {
        return Material.LIME_STAINED_GLASS_PANE;
    }

    @Override
    protected String accent() {
        return "§a";
    }

    @Override
    public String menuTitle() {
        return TITLE;
    }

    @Override
    protected String taxTerm() {
        return "Zakah";
    }

    @Override
    protected String backTargetName() {
        return "Caliphate Court";
    }
}
