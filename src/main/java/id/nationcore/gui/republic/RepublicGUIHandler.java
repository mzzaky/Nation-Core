package id.nationcore.gui.republic;

import id.nationcore.gui.GUIListener;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import id.nationcore.NationCore;
import id.nationcore.models.ExecutiveOrder;
import id.nationcore.models.Government;
import id.nationcore.utils.MessageUtils;

/**
 * Handles GUI clicks specific to Republic nations:
 * main menu, voting, candidate info, government, executive orders.
 */
public class RepublicGUIHandler {

    private final NationCore plugin;
    private final GUIListener gui;

    public RepublicGUIHandler(NationCore plugin, GUIListener gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    public void handleMainMenu(Player player, ItemStack clicked, int slot) {
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (RepublicMainMenu.isFiller(clicked.getType()))
            return;

        MessageUtils.playSound(player, org.bukkit.Sound.UI_BUTTON_CLICK);

        if (slot == RepublicMainMenu.getSlot("NATION")) {
            gui.viewingPlayerStats.put(player.getUniqueId(), player.getUniqueId());
            gui.playerStatsGUI.openPlayerStats(player, player.getUniqueId());
            return;
        }
        if (slot == RepublicMainMenu.getSlot("PRESIDENT")) {
            gui.openGovernmentGUI(player);
            return;
        }
        if (slot == RepublicMainMenu.getSlot("TREASURY")) {
            gui.republicTreasuryMenu.open(player);
            return;
        }
        if (slot == RepublicMainMenu.getSlot("ELECTION")) {
            gui.votingGUI.openVotingMenu(player);
            return;
        }
        if (slot == RepublicMainMenu.getSlot("EXEC_ORDER")) {
            gui.openOrdersGUI(player);
            return;
        }
        if (slot == RepublicMainMenu.getSlot("CABINET")) {
            gui.cabinetGUI.openCabinetMenu(player);
            return;
        }
        if (slot == RepublicMainMenu.getSlot("ARENA")) {
            gui.arenaGUI.openArenaMenu(player);
            return;
        }
        if (slot == RepublicMainMenu.getSlot("RECALL")) {
            gui.recallGUI.openRecallMenu(player);
            return;
        }
        if (slot == RepublicMainMenu.getSlot("HISTORY")) {
            gui.presidentHistoryGUI.openHistoryMenu(player);
            return;
        }
        if (slot == RepublicMainMenu.getSlot("TAX")) {
            gui.taxGUI.openTaxMenu(player);
            return;
        }
        if (slot == RepublicMainMenu.getSlot("HELP")) {
            gui.helpGUI.openHelpMenu(player);
            return;
        }
        if (slot == RepublicMainMenu.getSlot("CAPITAL")) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "nationcore capital");
            return;
        }
        if (slot == RepublicMainMenu.getSlot("RESEARCH")) {
            gui.researchGUI.openMain(player);
            return;
        }
        if (slot == RepublicMainMenu.getSlot("HUB")) {
            gui.openHubGUI(player);
            return;
        }
    }

    public void handleVotingGUI(Player player, ItemStack clicked) {
        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
        }

        if (clicked.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) clicked.getItemMeta();
            if (meta.getOwningPlayer() != null) {
                UUID candidateUUID = meta.getOwningPlayer().getUniqueId();
                gui.viewingCandidate.put(player.getUniqueId(), candidateUUID);
                gui.votingGUI.openCandidateInfo(player, candidateUUID);
            }
        }
    }

    public void handleCandidateGUI(Player player, ItemStack clicked, String title) {
        UUID candidateUUID = gui.viewingCandidate.get(player.getUniqueId());

        if (clicked.getType() == Material.ARROW) {
            gui.votingGUI.openVotingMenu(player);
            return;
        }

        if (clicked.getType() == Material.LIME_WOOL && candidateUUID != null) {
            player.closeInventory();
            plugin.getElectionManager().castVote(player, candidateUUID);
            return;
        }

        if (clicked.getType() == Material.GOLDEN_APPLE && candidateUUID != null) {
            player.closeInventory();
            plugin.getElectionManager().endorseCandidate(player, candidateUUID);
        }
    }

    public void handleGovernmentGUI(Player player, ItemStack clicked) {
        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
        }

        if (clicked.getType() == Material.LECTERN) {
            gui.cabinetGUI.openCabinetMenu(player);
            return;
        }

        if (clicked.getType() == Material.WRITABLE_BOOK) {
            gui.openOrdersGUI(player);
            return;
        }

        if (clicked.getType() == Material.GOLD_BLOCK) {
            gui.republicTreasuryMenu.open(player);
            return;
        }

        if (clicked.getType() == Material.PAPER) {
            gui.votingGUI.openVotingMenu(player);
            return;
        }

        if (clicked.getType() == Material.BOOK) {
            gui.presidentHistoryGUI.openHistoryMenu(player);
            return;
        }

        if (clicked.getType() == Material.COMPASS) {
            gui.mainMenuRouter.openFor(player);
            return;
        }

        if (clicked.getType() == Material.EMERALD) {
            gui.salaryMenu.open(player);
            return;
        }

        if (clicked.getType() == Material.NETHER_STAR) {
            Government gov = plugin.getDataManager().getGovernment();
            if (gov.hasPresident() && gov.getPresidentUUID().equals(player.getUniqueId())) {
                player.closeInventory();
                boolean started = plugin.getArenaManager().startArena(player.getUniqueId());
                if (!started) {
                    MessageUtils.send(player,
                            "<red>Cannot start Presidential Games at this time. (Check games limit or treasury balance)");
                }
            } else {
                MessageUtils.send(player, "<red>Only the President can start the Arena Games.");
            }
            return;
        }

        if (clicked.getType() == Material.BELL) {
            Government gov = plugin.getDataManager().getGovernment();
            if (gov.hasPresident() && gov.getPresidentUUID().equals(player.getUniqueId())) {
                long cooldown = 8L * 60 * 60 * 1000;
                long nextAvailable = gov.getLastBroadcastTime() + cooldown;
                if (System.currentTimeMillis() >= nextAvailable) {
                    player.closeInventory();
                    id.nationcore.listeners.ChatListener.pendingBroadcasts.add(player.getUniqueId());
                    MessageUtils.send(player, "government.broadcast_type_prompt");
                } else {
                    MessageUtils.send(player, "government.broadcast_cooldown");
                }
            } else {
                MessageUtils.send(player, "government.broadcast_not_president");
            }
        }
    }

    public void handleOrdersGUI(Player player, ItemStack clicked, int slot) {
        if (RepublicExecutiveOrdersMenu.SLOT_CLOSE == slot || clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }
        if (RepublicExecutiveOrdersMenu.SLOT_BACK == slot || clicked.getType() == Material.ARROW) {
            gui.mainMenuRouter.openFor(player);
            return;
        }
        ExecutiveOrder.ExecutiveOrderType type = RepublicExecutiveOrdersMenu.getOrderAtSlot(slot);
        if (type == null)
            return;

        if (clicked.getType() != Material.YELLOW_CONCRETE) {
            MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }
        player.closeInventory();
        plugin.getExecutiveOrderManager().issueOrderForNation(player, type);
    }
}
