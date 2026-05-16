package id.nationcore.gui.caliphate;

import id.nationcore.gui.GUIListener;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;
import id.nationcore.models.CaliphateGovernment;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

/**
 * Handles GUI clicks specific to Caliphate nations:
 * main menu, caliphate court, executive orders, Bayt al-Mal.
 */
public class CaliphateGUIHandler {

    private final NationCore plugin;
    private final GUIListener gui;

    public CaliphateGUIHandler(NationCore plugin, GUIListener gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    public void handleMainMenu(Player player, ItemStack clicked, int slot) {
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (CaliphateMainMenu.isFiller(clicked.getType()))
            return;

        MessageUtils.playSound(player, org.bukkit.Sound.UI_BUTTON_CLICK);

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        CaliphateGovernment cg = nation != null ? nation.getCaliphateGovernment() : null;
        boolean isCaliph = cg != null && cg.hasCaliph() && cg.getCaliphUUID().equals(player.getUniqueId());
        boolean isShura = cg != null && cg.isShuraMember(player.getUniqueId());
        boolean isScholar = cg != null && cg.isScholar(player.getUniqueId());
        boolean isAdmin = player.hasPermission("nation.admin");
        boolean inLeadership = isCaliph || isShura || isScholar || isAdmin;

        if (slot == CaliphateMainMenu.getSlot("CALIPH")) {
            if (!inLeadership) {
                MessageUtils.send(player,
                        "<red>Only the Caliph, Shura Council, and State Scholars may enter the Caliphate Court.</red>");
                return;
            }
            gui.openGovernmentGUI(player);
            return;
        }

        if (slot == CaliphateMainMenu.getSlot("TREASURY")) {
            if (!inLeadership) {
                MessageUtils.send(player,
                        "<red>Only the Caliph, Shura Council, and State Scholars may inspect Bayt al-Mal.</red>");
                return;
            }
            gui.caliphateTreasuryMenu.open(player, nation);
            return;
        }

        if (slot == CaliphateMainMenu.getSlot("RESEARCH")) {
            gui.researchGUI.openMain(player);
            return;
        }

        if (slot == CaliphateMainMenu.getSlot("HUB")) {
            gui.openHubGUI(player);
            return;
        }
    }

    public void handleGovernmentGUI(Player player, ItemStack clicked, int slot) {
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.LIME_STAINED_GLASS_PANE)
            return;

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null)
            return;

        MessageUtils.playSound(player, org.bukkit.Sound.UI_BUTTON_CLICK);

        if (slot == 43 || clicked.getType() == Material.SPECTRAL_ARROW) {
            gui.mainMenuRouter.openFor(player);
            return;
        }

        if (slot == 13) {
            gui.openOrdersGUI(player);
            return;
        }

        if (slot == 48) {
            CaliphateGovernment cg = nation.getCaliphateGovernment();
            boolean isCaliph = cg != null && cg.hasCaliph() && cg.getCaliphUUID().equals(player.getUniqueId());
            if (!isCaliph && !player.hasPermission("nation.admin")) {
                MessageUtils.send(player, "§cOnly the Caliph can rename the caliphate.");
                return;
            }
            player.closeInventory();
            plugin.getNationManager().setPendingRename(player.getUniqueId(), nation);
            MessageUtils.send(player, "§ePlease type the new name for your caliphate in chat. Type 'cancel' to abort.");
            return;
        }

        if (slot == 49) {
            gui.confirmActionGUI.open(player, "Leave your caliphate", () -> player.performCommand("nc leave"));
            return;
        }

        if (slot == 50) {
            CaliphateGovernment cg = nation.getCaliphateGovernment();
            boolean isCaliph = cg != null && cg.hasCaliph() && cg.getCaliphUUID().equals(player.getUniqueId());
            boolean isAdmin = player.hasPermission("nation.admin");

            if (!isCaliph && !isAdmin) {
                MessageUtils.send(player, "<red>Only the Caliph or an admin can disband the nation.</red>");
                return;
            }

            gui.confirmActionGUI.open(player, "Dissolve your caliphate", () -> {
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

        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
        }
    }

    public void handleOrdersGUI(Player player, ItemStack clicked, int slot) {
        if (slot == CaliphateExecutiveOrdersMenu.SLOT_CLOSE || clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }
        if (slot == CaliphateExecutiveOrdersMenu.SLOT_BACK || clicked.getType() == Material.SPECTRAL_ARROW) {
            gui.mainMenuRouter.openFor(player);
            return;
        }

        id.nationcore.models.ExecutiveOrder.ExecutiveOrderType eoType = CaliphateExecutiveOrdersMenu
                .getExecutiveOrderAtSlot(slot);
        if (eoType == null)
            return;

        if (clicked.getType() != Material.LIME_CONCRETE_POWDER) {
            MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        player.closeInventory();
        plugin.getExecutiveOrderManager().issueOrderForNation(player, eoType);
    }

    public void handleTreasuryGUI(Player player, ItemStack clicked) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null)
            return;

        if (clicked.getType() == Material.ARROW) {
            gui.mainMenuRouter.openFor(player);
            return;
        }
        if (clicked.getType() == Material.BOOK) {
            gui.caliphateTreasuryMenu.openTransactions(player, nation);
        }
    }

    public void handleTreasuryLogsGUI(Player player, ItemStack clicked) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null)
            return;
        if (clicked.getType() == Material.ARROW) {
            gui.caliphateTreasuryMenu.open(player, nation);
        }
    }
}
