package id.nationcore.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;
import id.nationcore.models.Nation;
import id.nationcore.models.GovernmentType;
import id.nationcore.listeners.ChatListener;
import id.nationcore.utils.MessageUtils;

@SuppressWarnings("deprecation")
public abstract class AbstractSettingsMenu {

    protected final NationCore plugin;

    protected AbstractSettingsMenu(NationCore plugin) {
        this.plugin = plugin;
    }

    // -- Hooks --
    protected abstract Material fillerMaterial();
    protected abstract String accent();
    public abstract String menuTitle();

    public void open(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) {
            MessageUtils.send(player, "<red>You are not in a nation.</red>");
            player.closeInventory();
            return;
        }

        // Rule 3: Only the nation leader can open the settings menu.
        if (!player.getUniqueId().equals(nation.getLeaderUUID()) && !player.hasPermission("nation.admin")) {
            MessageUtils.send(player, "<red>Only the nation leader can open the Settings menu.</red>");
            player.closeInventory();
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, menuTitle());

        // 1. FILLER
        ItemStack filler = GovernmentGUIUtils.createItem(fillerMaterial(), " ");
        int[] fillerSlots = { 0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 50, 51, 52, 53 };
        for (int slot : fillerSlots) {
            inv.setItem(slot, filler);
        }

        // 2. BACK (Slot 49)
        ItemStack back = GovernmentGUIUtils.createItem(Material.SPECTRAL_ARROW, "§7§l← Back", "§7Return to previous menu");
        inv.setItem(49, back);

        // 3. Information (Slot 4)
        String term = nation.getType() == GovernmentType.CALIPHATE ? "Zakah" : "Tax";
        ItemStack info = GovernmentGUIUtils.createItem(Material.GLOW_ITEM_FRAME, accent() + "§lSettings Information",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Configure your nation's core",
                "§7properties and preferences.",
                "§7",
                "§7• §fRename Nation & TAG",
                "§7  Left Click: Rename Nation",
                "§7  Right Click: Rename 3-letter TAG",
                "§7",
                "§7• §fToggle " + term + " System",
                "§7  Enable/Disable citizen " + term.toLowerCase() + "s",
                "§7",
                "§7• §fDisband Nation",
                "§7  Dissolve this nation forever",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
        );
        inv.setItem(4, info);

        // 4. Coming Soon (Slot 30,31,32)
        ItemStack comingSoon = GovernmentGUIUtils.createItem(Material.BARRIER, "§c§lComing Soon",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7This slot is reserved for",
                "§7future settings configurations.",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
        );
        inv.setItem(30, comingSoon);
        inv.setItem(31, comingSoon);
        inv.setItem(32, comingSoon);

        // 5. Rename Nation (Slot 21)
        ItemStack rename = GovernmentGUIUtils.createItem(Material.WRITABLE_BOOK, "§e§lRename Nation / TAG",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Change name or tag.",
                "§7",
                "§7Current Name: §f" + nation.getName(),
                "§7Current TAG: §f[" + nation.getTag() + "]",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§eLeft Click §7→ Rename Nation Name",
                "§eRight Click §7→ Change Nation TAG",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
        );
        inv.setItem(21, rename);

        // 6. Toggle Tax (Slot 22)
        boolean taxEnabled = nation.isTaxEnabled();
        Material taxMat = taxEnabled ? Material.EMERALD : Material.DEEPSLATE_EMERALD_ORE;
        String taxStatus = taxEnabled ? "§a§lENABLED" : "§c§lDISABLED";
        ItemStack taxToggle = GovernmentGUIUtils.createItem(taxMat, "§a§lToggle " + term + " System",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Configure if citizens should be",
                "§7billed with " + term.toLowerCase() + "s every daily cycle.",
                "§7",
                "§7Status: " + taxStatus,
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§eClick §7→ Toggle " + (taxEnabled ? "OFF" : "ON"),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
        );
        inv.setItem(22, taxToggle);

        // 7. Disband Nation (Slot 23)
        ItemStack disband = GovernmentGUIUtils.createItem(Material.TNT_MINECART, "§c§lDisband Nation",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Dissolve this nation forever.",
                "§7All members will be released and",
                "§7treasury will be lost.",
                "§7",
                "§c⚠ Warning: Irreversible action!",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§cClick §7→ Disband",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
        );
        inv.setItem(23, disband);

        player.openInventory(inv);
    }

    public void handleClick(GUIListener gui, Player player, ItemStack clicked, int rawSlot, ClickType clickType) {
        if (rawSlot < 0 || rawSlot >= 54) return;
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == fillerMaterial()) return;

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) return;

        // Rule 3: Only the nation leader can perform actions here
        if (!player.getUniqueId().equals(nation.getLeaderUUID()) && !player.hasPermission("nation.admin")) {
            MessageUtils.send(player, "<red>Only the nation leader can perform this setting.</red>");
            player.closeInventory();
            return;
        }

        MessageUtils.playSound(player, org.bukkit.Sound.UI_BUTTON_CLICK);

        // 2. BACK (Slot 49)
        if (rawSlot == 49 && clicked.getType() == Material.SPECTRAL_ARROW) {
            gui.openGovernmentGUI(player);
            return;
        }

        // 5. Rename Nation / TAG (Slot 21)
        if (rawSlot == 21 && clicked.getType() == Material.WRITABLE_BOOK) {
            if (clickType.isLeftClick()) {
                player.closeInventory();
                plugin.getNationManager().setPendingRename(player.getUniqueId(), nation);
                MessageUtils.send(player, "<yellow>Please type the new name for your nation in chat. Type 'cancel' to abort.</yellow>");
            } else if (clickType.isRightClick()) {
                player.closeInventory();
                ChatListener.pendingTagRenames.put(player.getUniqueId(), nation);
                MessageUtils.send(player, "<yellow>Please type the new 3-letter TAG for your nation in chat. Type 'cancel' to abort.</yellow>");
            }
            return;
        }

        // 6. Toggle Tax (Slot 22)
        if (rawSlot == 22 && (clicked.getType() == Material.EMERALD || clicked.getType() == Material.DEEPSLATE_EMERALD_ORE)) {
            boolean current = nation.isTaxEnabled();
            nation.setTaxEnabled(!current);
            plugin.getDataManager().saveNations();

            String term = nation.getType() == GovernmentType.CALIPHATE ? "Zakah" : "Tax";
            if (nation.isTaxEnabled()) {
                MessageUtils.send(player, "<green>⚡ " + term + " collection has been <bold>enabled</bold> for your nation.");
                MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING);
            } else {
                MessageUtils.send(player, "<red>⚡ " + term + " collection has been <bold>disabled</bold> for your nation.");
                MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            }
            open(player); // Refresh
            return;
        }

        // 7. Disband Nation (Slot 23)
        if (rawSlot == 23 && clicked.getType() == Material.TNT_MINECART) {
            gui.confirmActionGUI.open(player, "Dissolve your nation", () -> {
                id.nationcore.managers.NationManager.Result result = plugin.getNationManager().disbandNation(player);
                if (result.isSuccess()) {
                    MessageUtils.send(player, "<green>" + result.getMessage() + "</green>");
                    player.closeInventory();
                } else {
                    MessageUtils.send(player, "<red>" + result.getMessage() + "</red>");
                }
            });
            return;
        }
    }
}
