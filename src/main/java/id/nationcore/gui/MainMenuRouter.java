package id.nationcore.gui;

import id.nationcore.gui.caliphate.CaliphateMainMenu;
import id.nationcore.gui.communist.CommunistMainMenu;
import id.nationcore.gui.monarchy.MonarchyMainMenu;
import id.nationcore.gui.republic.RepublicMainMenu;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import id.nationcore.NationCore;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

/**
 * Single entry point to open a player's "Main Menu".
 *
 * Main tasks:
 *   1. Ensure the player is joined to a nation. Without a nation,
 *      the player is redirected to {@link HubGUI} (where they can join/create).
 *   2. Select the menu implementation that matches the {@link GovernmentType}
 *      of the player's nation — REPUBLIC → {@link RepublicMainMenu},
 *      COMMUNIST → {@link CommunistMainMenu}.
 *
 * With this separation, the GUIListener only needs to call
 * {@code router.openFor(player)} without needing to know the government type.
 */
public class MainMenuRouter {

    private final NationCore plugin;
    private final RepublicMainMenu republicMenu;
    private final CommunistMainMenu communistMenu;
    private final MonarchyMainMenu monarchyMenu;
    private final CaliphateMainMenu caliphateMenu;

    public MainMenuRouter(NationCore plugin) {
        this.plugin = plugin;
        this.republicMenu = new RepublicMainMenu(plugin);
        this.communistMenu = new CommunistMainMenu(plugin);
        this.monarchyMenu = new MonarchyMainMenu(plugin);
        this.caliphateMenu = new CaliphateMainMenu(plugin);
    }

    public RepublicMainMenu getRepublicMenu() {
        return republicMenu;
    }

    public CommunistMainMenu getCommunistMenu() {
        return communistMenu;
    }

    public MonarchyMainMenu getMonarchyMenu() {
        return monarchyMenu;
    }

    public CaliphateMainMenu getCaliphateMenu() {
        return caliphateMenu;
    }

    /**
     * Open the main menu for the player. If the player does not have a nation yet,
     * direct them to the Nation Hub so they can join or create a nation.
     */
    public void openFor(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) {
            MessageUtils.send(player, "<red>You are not joined to any nation yet. " +
                    "Choose or create a nation in the Hub first.</red>");
            MessageUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS);
            plugin.getGUIListener().openHubGUI(player);
            return;
        }
        if (nation.getType() == null) {
            MessageUtils.send(player, "<red>Your nation does not have a valid government type. " +
                    "Please contact an administrator.</red>");
            return;
        }

        switch (nation.getType()) {
            case REPUBLIC -> {
                republicMenu.open(player, nation);
                MessageUtils.playSound(player, Sound.UI_TOAST_IN);
            }
            case COMMUNIST -> {
                communistMenu.open(player, nation);
                MessageUtils.playSound(player, Sound.UI_TOAST_IN);
            }
            case MONARCHY -> {
                monarchyMenu.open(player, nation);
                MessageUtils.playSound(player, Sound.UI_TOAST_IN);
            }
            case CALIPHATE -> {
                caliphateMenu.open(player, nation);
                MessageUtils.playSound(player, Sound.UI_TOAST_IN);
            }
            default -> MessageUtils.send(player, "<red>The government type " +
                    nation.getType() + " does not have a main menu yet.</red>");
        }
    }
}
