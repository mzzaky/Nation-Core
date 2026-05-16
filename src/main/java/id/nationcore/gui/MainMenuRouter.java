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
 * Entry point tunggal untuk membuka "Main Menu" pemain.
 *
 * Tugas utama:
 *   1. Memastikan pemain tergabung di sebuah nation. Tanpa nation,
 *      pemain diarahkan ke {@link HubGUI} (tempat ia bisa join/create).
 *   2. Memilih implementasi menu yang sesuai dengan {@link GovernmentType}
 *      nation pemain — REPUBLIC → {@link RepublicMainMenu},
 *      COMMUNIST → {@link CommunistMainMenu}.
 *
 * Dengan pemisahan ini, GUIListener cukup memanggil
 * {@code router.openFor(player)} tanpa perlu tahu jenis pemerintahan.
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
     * Buka menu utama untuk pemain. Bila pemain belum punya nation,
     * arahkan ke Nation Hub agar mereka bisa join atau membuat nation.
     */
    public void openFor(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) {
            MessageUtils.send(player, "<red>Anda belum tergabung di nation manapun. " +
                    "Pilih atau buat nation di Hub terlebih dahulu.</red>");
            MessageUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS);
            plugin.getGUIListener().openHubGUI(player);
            return;
        }
        if (nation.getType() == null) {
            MessageUtils.send(player, "<red>Nation Anda tidak memiliki jenis pemerintahan yang valid. " +
                    "Hubungi admin.</red>");
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
            default -> MessageUtils.send(player, "<red>Jenis pemerintahan " +
                    nation.getType() + " belum memiliki menu utama.</red>");
        }
    }
}
