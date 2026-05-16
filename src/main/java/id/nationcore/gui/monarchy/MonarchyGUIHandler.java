package id.nationcore.gui.monarchy;

import id.nationcore.gui.GUIListener;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;
import id.nationcore.models.MonarchyGovernment;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

/**
 * Handles GUI clicks specific to Monarchy nations:
 * main menu, royal court, decrees, royal treasury.
 */
public class MonarchyGUIHandler {

    private final NationCore plugin;
    private final GUIListener gui;

    public MonarchyGUIHandler(NationCore plugin, GUIListener gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    public void handleMainMenu(Player player, ItemStack clicked, int slot) {
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (MonarchyMainMenu.isFiller(clicked.getType()))
            return;

        MessageUtils.playSound(player, org.bukkit.Sound.UI_BUTTON_CLICK);

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        MonarchyGovernment mg = nation != null ? nation.getMonarchyGovernment() : null;
        boolean isKing = mg != null && mg.hasKing() && mg.getKingUUID().equals(player.getUniqueId());
        boolean isCouncil = mg != null && mg.getCouncilMemberByUUID(player.getUniqueId()) != null;
        boolean isAdmin = player.hasPermission("nation.admin");
        boolean kingOrCouncil = isKing || isCouncil || isAdmin;

        if (slot == MonarchyMainMenu.getSlot("KING")) {
            if (!kingOrCouncil) {
                MessageUtils.send(player, "<red>Only the King and the High Council can enter the Royal Court.</red>");
                return;
            }
            gui.openGovernmentGUI(player);
            return;
        }

        if (slot == MonarchyMainMenu.getSlot("TREASURY")) {
            if (!kingOrCouncil) {
                MessageUtils.send(player, "<red>Only the King and the High Council can read the Royal Treasury.</red>");
                return;
            }
            gui.monarchyTreasuryMenu.open(player, nation);
            return;
        }

        if (slot == MonarchyMainMenu.getSlot("TAX")) {
            gui.taxGUI.openTaxMenu(player);
            return;
        }

        if (slot == MonarchyMainMenu.getSlot("RESEARCH")) {
            gui.researchGUI.openMain(player);
            return;
        }

        if (slot == MonarchyMainMenu.getSlot("HUB")) {
            gui.openHubGUI(player);
            return;
        }
    }

    public void handleGovernmentGUI(Player player, ItemStack clicked, int slot) {
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.YELLOW_STAINED_GLASS_PANE)
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
            MonarchyGovernment mg = nation.getMonarchyGovernment();
            boolean isKing = mg != null && mg.hasKing() && mg.getKingUUID().equals(player.getUniqueId());
            if (!isKing && !player.hasPermission("nation.admin")) {
                MessageUtils.send(player, "§cOnly the King can rename the kingdom.");
                return;
            }
            player.closeInventory();
            plugin.getNationManager().setPendingRename(player.getUniqueId(), nation);
            MessageUtils.send(player, "§ePlease type the new name for your kingdom in chat. Type 'cancel' to abort.");
            return;
        }

        if (slot == 49) {
            gui.confirmActionGUI.open(player, "Leave your kingdom", () -> player.performCommand("nc leave"));
            return;
        }

        if (slot == 50) {
            MonarchyGovernment mg = nation.getMonarchyGovernment();
            boolean isKing = mg != null && mg.hasKing() && mg.getKingUUID().equals(player.getUniqueId());
            boolean isAdmin = player.hasPermission("nation.admin");

            if (!isKing && !isAdmin) {
                MessageUtils.send(player, "<red>Only the King or an admin can disband the nation.</red>");
                return;
            }

            gui.confirmActionGUI.open(player, "Dissolve your kingdom", () -> {
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
        if (slot == MonarchyExecutiveOrdersMenu.SLOT_CLOSE || clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }
        if (slot == 43 || clicked.getType() == Material.SPECTRAL_ARROW) {
            gui.mainMenuRouter.openFor(player);
            return;
        }

        if (MonarchyExecutiveOrdersMenu.handleTabClick(player, slot)) {
            gui.openOrdersGUI(player);
            return;
        }

        id.nationcore.models.MonarchyDecisionType type = MonarchyExecutiveOrdersMenu.getDecisionAtSlot(player, slot);
        if (type == null) {
            id.nationcore.models.ExecutiveOrder.ExecutiveOrderType eoType = MonarchyExecutiveOrdersMenu
                    .getExecutiveOrderAtSlot(player, slot);
            if (eoType == null)
                return;

            if (clicked.getType() != Material.YELLOW_CONCRETE) {
                MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
                return;
            }
            player.closeInventory();
            plugin.getExecutiveOrderManager().issueOrderForNation(player, eoType);
            return;
        }

        if (clicked.getType() != Material.YELLOW_CONCRETE) {
            MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) {
            MessageUtils.send(player, "<red>You are not a subject of any kingdom.</red>");
            player.closeInventory();
            return;
        }

        player.closeInventory();
        var result = plugin.getMonarchyManager().executeDecision(nation, player, type);
        if (result.isSuccess()) {
            MessageUtils.send(player, "<green>" + result.getMessage() + "</green>");
        } else {
            MessageUtils.send(player, "<red>" + result.getMessage() + "</red>");
        }
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
            gui.monarchyTreasuryMenu.openTransactions(player, nation);
        }
    }

    public void handleTreasuryLogsGUI(Player player, ItemStack clicked) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null)
            return;
        if (clicked.getType() == Material.ARROW) {
            gui.monarchyTreasuryMenu.open(player, nation);
        }
    }
}
