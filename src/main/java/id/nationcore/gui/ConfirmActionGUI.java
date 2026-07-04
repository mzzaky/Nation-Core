package id.nationcore.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;

@SuppressWarnings("deprecation")
public class ConfirmActionGUI {

    public static final String TITLE = "§4§lCONFIRM ACTION";
    private final Map<UUID, Runnable> pendingActions = new HashMap<>();

    public ConfirmActionGUI(NationCore plugin) {
    }

    public void open(Player player, String actionDescription, Runnable onConfirm) {
        pendingActions.put(player.getUniqueId(), onConfirm);

        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        ItemStack info = GovernmentGUIUtils.createItem(Material.PAPER, "§e§lAction Info", 
            "§7You are about to:",
            "§f" + actionDescription);
        inv.setItem(13, info);

        ItemStack confirm = GovernmentGUIUtils.createItem(Material.LIME_CONCRETE, "§a§l✔ CONFIRM", 
            "§7Click to proceed with this action.");
        inv.setItem(11, confirm);

        ItemStack cancel = GovernmentGUIUtils.createItem(Material.RED_CONCRETE, "§c§l✗ CANCEL", 
            "§7Click to cancel and close.");
        inv.setItem(15, cancel);

        ItemStack filler = GovernmentGUIUtils.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }

        player.openInventory(inv);
    }

    public void handleConfirm(Player player) {
        Runnable action = pendingActions.remove(player.getUniqueId());
        player.closeInventory();
        if (action != null) {
            action.run();
        }
    }

    public void handleCancel(Player player) {
        pendingActions.remove(player.getUniqueId());
        player.closeInventory();
    }
}
