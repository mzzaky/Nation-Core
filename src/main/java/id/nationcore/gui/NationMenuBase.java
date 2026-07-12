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
import id.nationcore.models.ExecutiveOrder;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

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

    // ===== Executive Order card (shared across all government menus) =====

    /**
     * Build a unified executive-order card. Every government's executive-order
     * menu (and every minister office console) renders orders with this method
     * so the layout is identical across Republic, Communist, Monarchy and
     * Caliphate. All values (name, lore, cost, cooldown, duration) come from
     * the order.yaml catalogue via the {@code ExecutiveOrderManager}.
     *
     * @param nation      the viewing player's nation (never null)
     * @param type        the executive order to render
     * @param canIssue    whether the viewer's office may issue this order
     * @param issuerLabel display label for the authorized issuer (e.g. "President Steve")
     * @param accent      colour code used for the title/effects header (e.g. "&b")
     */
    protected ItemStack buildExecutiveOrderCard(Nation nation, ExecutiveOrder.ExecutiveOrderType type,
                                                boolean canIssue, String issuerLabel, String accent) {
        var mgr = plugin.getExecutiveOrderManager();
        boolean active = mgr.isOrderActive(nation, type);
        boolean onCooldown = !active && mgr.isOrderOnCooldown(nation, type);
        double cost = mgr.getOrderCost(type);
        long cooldownRemaining = onCooldown ? mgr.getOrderCooldownRemaining(nation, type) : 0L;
        boolean canAfford = plugin.getTreasuryManager().canAfford(nation, cost);
        long duration = mgr.getOrderDurationMillis(type);
        ExecutiveOrder activeOrder = active ? mgr.getActiveOrder(nation, type) : null;

        Material material;
        String status;
        if (active) {
            material = Material.ENCHANTED_BOOK;
            status = "&a[ACTIVE]";
        } else if (onCooldown) {
            material = Material.BOOK;
            status = "&c[COOLDOWN]";
        } else if (!canIssue) {
            material = Material.BOOK;
            status = "&8[NO ACCESS]";
        } else if (!canAfford) {
            material = Material.BOOK;
            status = "&8[INSUFFICIENT FUNDS]";
        } else {
            material = Material.WRITABLE_BOOK;
            status = "&e[AVAILABLE]";
        }

        String bar = "&8" + "▬".repeat(28);
        List<String> lore = new ArrayList<>();
        lore.add(bar);
        lore.add("&7\"" + type.getFlavorText() + "\"");
        lore.add(bar);
        lore.add("&7Issuer: &f" + issuerLabel);
        lore.add("&7Cost: &6$" + MessageUtils.formatNumber((long) cost));
        lore.add("&7Duration: &b" + (duration == 0 ? "Instant" : MessageUtils.formatTimeShort(duration)));
        if (nation.getType() == GovernmentType.MONARCHY) {
            lore.add("&7Cooldown: &bNone &8(royal prerogative)");
        } else {
            lore.add("&7Cooldown: &b" + mgr.getOrderCooldownDays(type) + " day(s)");
        }
        lore.add(bar);
        lore.add(accent + "&lDECREE EFFECTS:");
        for (String line : mgr.getOrderLore(type)) {
            lore.add("&7" + line);
        }
        lore.add(bar);
        if (active && activeOrder != null) {
            lore.add("&a✔ &lORDER ACTIVE");
            lore.add("&7Time Remaining: &a" + MessageUtils.formatTime(activeOrder.getRemainingTime()));
        } else if (onCooldown) {
            lore.add("&c⏳ &lON COOLDOWN");
            lore.add("&7Available in: &c" + MessageUtils.formatTime(cooldownRemaining));
        } else if (!canIssue) {
            lore.add("&c✖ &lNO AUTHORIZATION");
            lore.add("&7You are not authorized to issue this decree.");
        } else if (!canAfford) {
            lore.add("&c✖ &lINSUFFICIENT FUNDS");
            lore.add("&7Treasury requires &6$" + MessageUtils.formatNumber((long) cost));
        } else {
            lore.add("&a⚡ &lREADY TO ISSUE");
            lore.add("&eClick to authorize this decree.");
        }
        lore.add(bar);

        ItemStack item = buildIcon(material, accent + "&l" + mgr.getOrderDisplay(type) + " " + status, lore);
        if (active) {
            item = glowing(item);
        }
        return item;
    }
}
