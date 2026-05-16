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
import id.nationcore.models.Nation;
import id.nationcore.models.PlayerData;

/**
 * Hub Menu — list of all nations registered on the server.
 *
 * Players can:
 *   • view name, government type, member count & leader of each nation
 *   • click nation → join (if they don't have a nation yet)
 *   • buttons at the bottom:
 *       - "Create Nation" if the player has not joined yet
 *       - "My Nation Menu" if the player has already joined
 */
public class HubGUI {

    public static final String HUB_TITLE = ChatColor.translateAlternateColorCodes('&', "&8» &bNation Hub");

    /** Slot where the action button (Create / My Nation) is placed. */
    public static final int ACTION_BUTTON_SLOT = 49;
    public static final int FILTER_BUTTON_SLOT = 45;
    public static final int REFRESH_BUTTON_SLOT = 53;

    private static final int LIST_AREA_END = 44; // slot 0..44 for nation list (45 slots)

    private final NationCore plugin;

    public HubGUI(NationCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, HUB_TITLE);

        List<Nation> nations = plugin.getNationManager().getNationsSortedByMembers();

        int slot = 0;
        for (Nation nation : nations) {
            if (slot > LIST_AREA_END) break; // melebihi area list
            inv.setItem(slot, buildNationIcon(nation));
            slot++;
        }

        // Bottom border
        ItemStack pane = makePane();
        for (int i = 45; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, pane);
        }

        // Filter / refresh buttons
        inv.setItem(FILTER_BUTTON_SLOT, buildInfoBoard(nations.size()));
        inv.setItem(REFRESH_BUTTON_SLOT, buildRefreshButton());

        // Tombol Create / My Nation
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        if (data.hasNation() && plugin.getNationManager().getNation(data.getNationId()) != null) {
            inv.setItem(ACTION_BUTTON_SLOT, buildMyNationButton(plugin.getNationManager().getNation(data.getNationId())));
        } else {
            inv.setItem(ACTION_BUTTON_SLOT, buildCreateButton());
        }

        player.openInventory(inv);
    }

    private ItemStack buildNationIcon(Nation nation) {
        Material mat = nation.getType() != null ? nation.getType().getIconMaterial() : Material.OAK_SIGN;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String typeColor = nation.getType() != null ? nation.getType().getColorCode() : "&f";
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                    typeColor + "&l" + nation.getName()));

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&',
                    "&7Type: " + (nation.getType() != null
                            ? nation.getType().getColoredName()
                            : "&fUnknown")));
            lore.add(ChatColor.translateAlternateColorCodes('&',
                    "&7Leader: &f" + (nation.getLeaderName() != null ? nation.getLeaderName() : "-")));
            lore.add(ChatColor.translateAlternateColorCodes('&',
                    "&7Members: &f" + nation.getMemberCount()));
            lore.add(ChatColor.translateAlternateColorCodes('&',
                    "&7Tag: &f[" + nation.getTag() + "]"));
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&eLeft click &7to join"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&eRight click &7to view details"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildCreateButton() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a&l+ Create New Nation"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Establish a new nation and become its leader."));
            lore.add("");
            double cost = plugin.getConfig().getDouble("nation.creation.cost", 1_000_000);
            double minHours = plugin.getConfig().getDouble("nation.creation.min-playtime-hours", 0);
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Requirements:"));
            lore.add(ChatColor.translateAlternateColorCodes('&',
                    "&7• Balance: &6$" + String.format("%,.0f", cost)));
            if (minHours > 0) {
                lore.add(ChatColor.translateAlternateColorCodes('&',
                        "&7• Playtime: &6" + minHours + " hours"));
            }
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&eClick to choose government type"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildMyNationButton(Nation nation) {
        // Material adjusted to government type so players immediately
        // know which main menu will open.
        Material icon = nation.getType() == id.nationcore.models.GovernmentType.COMMUNIST
                ? Material.RED_BANNER
                : Material.WHITE_BANNER;
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String accent = nation.getType() == id.nationcore.models.GovernmentType.COMMUNIST
                    ? "&c"
                    : "&b";
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                    accent + "&l⚐ My Nation Menu"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Nation: &f" + nation.getName()));
            lore.add(ChatColor.translateAlternateColorCodes('&',
                    "&7Type: " + (nation.getType() != null ? nation.getType().getColoredName() : "&fUnknown")));
            lore.add(ChatColor.translateAlternateColorCodes('&',
                    "&7Leader: &f" + (nation.getLeaderName() != null ? nation.getLeaderName() : "-")));
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&',
                    "&eClick &7→ open government dashboard"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildInfoBoard(int count) {
        ItemStack item = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6&lNation List"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Total active nations: &f" + count));
            lore.add(ChatColor.translateAlternateColorCodes('&',
                    "&7Sorted by: &fmember count"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildRefreshButton() {
        ItemStack item = new ItemStack(Material.SUNFLOWER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e&lRefresh"));
            meta.setLore(List.of(ChatColor.translateAlternateColorCodes('&',
                    "&7Reload the nation list.")));
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
