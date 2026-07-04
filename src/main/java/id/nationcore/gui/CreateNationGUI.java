package id.nationcore.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import id.nationcore.NationCore;
import id.nationcore.models.GovernmentType;

/**
 * Government type selector during the creation of a new nation.
 *
 * After the player chooses, the GUIListener will save the choice
 * and ask the player to type the nation name in chat (chat-input flow).
 */
public class CreateNationGUI {

    public static final String CREATE_TITLE = ChatColor.translateAlternateColorCodes('&',
            "&8Choose Government Type");

    public static final int SLOT_REPUBLIC = 10;
    public static final int SLOT_COMMUNIST = 12;
    public static final int SLOT_MONARCHY = 14;
    public static final int SLOT_CALIPHATE = 16;
    public static final int SLOT_BACK = 22;

    @SuppressWarnings("unused")
    private final NationCore plugin;

    public CreateNationGUI(NationCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, CREATE_TITLE);

        ItemStack pane = makePane();
        for (int i = 0; i < 27; i++)
            inv.setItem(i, pane);

        inv.setItem(SLOT_REPUBLIC, buildTypeIcon(GovernmentType.REPUBLIC));
        inv.setItem(SLOT_COMMUNIST, buildTypeIcon(GovernmentType.COMMUNIST));
        inv.setItem(SLOT_MONARCHY, buildTypeIcon(GovernmentType.MONARCHY));
        inv.setItem(SLOT_CALIPHATE, buildTypeIcon(GovernmentType.CALIPHATE));
        inv.setItem(SLOT_BACK, buildBackButton());

        player.openInventory(inv);
    }

    private ItemStack buildTypeIcon(GovernmentType type) {
        ItemStack item = new ItemStack(type.getIconMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                    type.getColorCode() + "&l" + type.getDisplayName().toUpperCase()));

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7" + type.getShortDescription()));
            lore.add("");
            for (String line : type.getHighlights()) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&eRequirements & Cost:"));
            double cost = plugin.getNationCreationCost(type);
            double minHours = plugin.getNationCreationMinPlaytime(type);
            double startingTreasury = plugin.getNationCreationStartingTreasuryPercent(type);
            
            lore.add(ChatColor.translateAlternateColorCodes('&', " &8• &7Creation Cost: &6$" + String.format("%,.0f", cost)));
            if (minHours > 0) {
                lore.add(ChatColor.translateAlternateColorCodes('&', " &8• &7Min Playtime: &6" + minHours + " hours"));
            }
            if (startingTreasury > 0) {
                lore.add(ChatColor.translateAlternateColorCodes('&', " &8• &7Starting Treasury: &6" + String.format("%.0f", startingTreasury) + "%"));
            }
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&',
                    "&eClick to choose " + type.getDisplayName() + "."));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildBackButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&lCancel"));
            meta.setLore(List.of(
                    ChatColor.translateAlternateColorCodes('&', "&7Back to Hub without creating a nation.")));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makePane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }
}
