package id.nationcore.gui;

import id.nationcore.gui.communist.CommunistMainMenu;
import id.nationcore.gui.republic.RepublicMainMenu;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import id.nationcore.NationCore;

/**
 * Shared foundation for all per-nation main menus.
 *
 * Responsible for:
 *   • ItemStack creation helpers (icons, banners, panes)
 *   • Consistent time/number formatting across menus
 *   • Generic border placement
 *
 * Derived menus ({@link RepublicMainMenu}, {@link CommunistMainMenu}) only
 * need to fill in government-specific content/data.
 */
public abstract class NationMenuBase {

    protected final NationCore plugin;

    protected NationMenuBase(NationCore plugin) {
        this.plugin = plugin;
    }

    // ===== Item builder helpers =====

    protected ItemStack buildIcon(Material material, String displayName, String... loreLines) {
        return buildIcon(material, 0, displayName, loreLines);
    }

    protected ItemStack buildIcon(Material material, int modelData, String displayName, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(displayName));
            if (loreLines != null && loreLines.length > 0) {
                List<String> lore = new ArrayList<>(loreLines.length);
                for (String line : loreLines) {
                    lore.add(color(line));
                }
                meta.setLore(lore);
            }
            if (modelData > 0) {
                meta.setCustomModelData(modelData);
            }
            for (ItemFlag flag : ItemFlag.values()) {
                meta.addItemFlags(flag);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    protected ItemStack buildIcon(Material material, String displayName, List<String> loreLines) {
        return buildIcon(material, 0, displayName, loreLines);
    }

    protected ItemStack buildIcon(Material material, int modelData, String displayName, List<String> loreLines) {
        if (loreLines == null) return buildIcon(material, modelData, displayName);
        return buildIcon(material, modelData, displayName, loreLines.toArray(new String[0]));
    }

    protected ItemStack buildPlayerSkull(Player owner, String displayName, List<String> loreLines) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(owner);
            meta.setDisplayName(color(displayName));
            if (loreLines != null) {
                List<String> processed = new ArrayList<>(loreLines.size());
                for (String line : loreLines) {
                    processed.add(color(line));
                }
                meta.setLore(processed);
            }
            for (ItemFlag flag : ItemFlag.values()) {
                meta.addItemFlags(flag);
            }
            head.setItemMeta(meta);
        }
        return head;
    }

    protected ItemStack buildOfflineSkull(OfflinePlayer owner, String displayName, List<String> loreLines) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(owner);
            meta.setDisplayName(color(displayName));
            if (loreLines != null) {
                List<String> processed = new ArrayList<>(loreLines.size());
                for (String line : loreLines) {
                    processed.add(color(line));
                }
                meta.setLore(processed);
            }
            for (ItemFlag flag : ItemFlag.values()) {
                meta.addItemFlags(flag);
            }
            head.setItemMeta(meta);
        }
        return head;
    }

    protected ItemStack glowing(ItemStack base) {
        ItemMeta meta = base.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            base.setItemMeta(meta);
        }
        return base;
    }

    protected ItemStack pane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    // ===== Layout helpers =====

    /** Mengisi sisa slot kosong dengan filler. */
    protected void fillEmptySlots(Inventory inv, ItemStack filler) {
        int size = inv.getSize();
        for (int i = 0; i < size; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }
    }

    /** Set item ke beberapa slot sekaligus. */
    protected void setSlots(Inventory inv, ItemStack item, int... slots) {
        for (int s : slots) {
            if (s >= 0 && s < inv.getSize()) {
                inv.setItem(s, item);
            }
        }
    }

    // ===== Formatting =====

    protected String color(String input) {
        if (input == null) return "";
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    protected String formatMoney(double amount) {
        if (amount >= 1_000_000_000) return String.format("%.2fB", amount / 1_000_000_000d);
        if (amount >= 1_000_000) return String.format("%.2fM", amount / 1_000_000d);
        if (amount >= 1_000) return String.format("%.2fK", amount / 1_000d);
        return String.format("%,.0f", amount);
    }

    protected String formatRemaining(long millis) {
        if (millis <= 0) return "finished";
        long total = millis / 1000;
        long days = total / 86400;
        long hours = (total % 86400) / 3600;
        long minutes = (total % 3600) / 60;
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + " minutes";
        return total + " seconds";
    }

    protected List<String> wrap(String... lines) {
        return new ArrayList<>(Arrays.asList(lines));
    }
}
