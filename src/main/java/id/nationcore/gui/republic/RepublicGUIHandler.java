package id.nationcore.gui.republic;

import id.nationcore.gui.GUIListener;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import id.nationcore.NationCore;
import id.nationcore.models.FakeMember;
import id.nationcore.models.ExecutiveOrder;
import id.nationcore.models.CabinetDecision;
import id.nationcore.models.Government;
import id.nationcore.models.Nation;
import id.nationcore.models.Nation.NationMember;
import id.nationcore.models.Nation.NationRole;
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

    public void handleGovernmentGUI(Player player, ItemStack clicked, int slot) {
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null)
            return;

        MessageUtils.playSound(player, org.bukkit.Sound.UI_BUTTON_CLICK);

        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (slot == 43 || clicked.getType() == Material.SPECTRAL_ARROW) {
            gui.mainMenuRouter.openFor(player);
            return;
        }

        if (slot == 37 || clicked.getType() == Material.EMERALD) {
            gui.salaryMenu.open(player);
            return;
        }

        if (slot == 48) {
            Government gov = nation.getRepublicGovernment();
            boolean isPresident = gov != null && gov.hasPresident() && gov.getPresidentUUID().equals(player.getUniqueId());
            if (!isPresident && !player.hasPermission("nation.admin")) {
                MessageUtils.send(player, "§cOnly the President can rename the nation.");
                return;
            }
            player.closeInventory();
            plugin.getNationManager().setPendingRename(player.getUniqueId(), nation);
            MessageUtils.send(player, "§ePlease type the new name for your nation in chat. Type 'cancel' to abort.");
            return;
        }

        if (slot == 49) {
            gui.confirmActionGUI.open(player, "Leave your nation", () -> player.performCommand("nc leave"));
            return;
        }

        if (slot == 50) {
            Government gov = nation.getRepublicGovernment();
            boolean isPresident = gov != null && gov.hasPresident() && gov.getPresidentUUID().equals(player.getUniqueId());
            boolean isAdmin = player.hasPermission("nation.admin");

            if (!isPresident && !isAdmin) {
                MessageUtils.send(player, "<red>Only the President or an admin can disband the nation.</red>");
                return;
            }

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

        if (slot == 31 || clicked.getType() == Material.PRIZE_POTTERY_SHERD) {
            gui.arenaGUI.openArenaMenu(player);
            return;
        }

        if (slot == 21 || clicked.getType() == Material.ARMS_UP_POTTERY_SHERD) {
            gui.openCabinetGUI(player);
            return;
        }

        if (slot == 22 || clicked.getType() == Material.FRIEND_POTTERY_SHERD) {
            int page = gui.memberListPage.getOrDefault(player.getUniqueId(), 0);
            gui.republicMemberManagementGUI.open(player, nation, page);
            return;
        }

        if (slot == 23) {
            Government gov = nation.getRepublicGovernment();
            if (gov == null) return;
            boolean isPresident = gov.hasPresident() && gov.getPresidentUUID().equals(player.getUniqueId());
            if (!isPresident && !player.hasPermission("nation.admin")) {
                MessageUtils.send(player, "<red>Only the President can broadcast a message.</red>");
                return;
            }

            long lastBroadcast = gov.getLastBroadcastTime();
            long timeSinceLast = System.currentTimeMillis() - lastBroadcast;
            long cooldownDuration = 6L * 60 * 60 * 1000;
            if (timeSinceLast < cooldownDuration && !player.hasPermission("nation.admin")) {
                long remaining = cooldownDuration - timeSinceLast;
                MessageUtils.send(player, "<red>Broadcast is on cooldown. Remaining: <white>" + MessageUtils.formatTime(remaining) + "</white></red>");
                return;
            }

            player.closeInventory();
            id.nationcore.listeners.ChatListener.pendingNationBroadcasts.put(player.getUniqueId(), nation);
            MessageUtils.send(player, "<yellow>Please type your custom broadcast message in chat. Type 'cancel' to abort.</yellow>");
            return;
        }
    }

    public void handleOrdersGUI(Player player, ItemStack clicked, int slot) {
        if (RepublicExecutiveOrdersMenu.SLOT_CLOSE == slot || clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }
        if (RepublicExecutiveOrdersMenu.SLOT_BACK == slot || clicked.getType() == Material.SPECTRAL_ARROW || clicked.getType() == Material.ARROW) {
            gui.mainMenuRouter.openFor(player);
            return;
        }

        if (RepublicExecutiveOrdersMenu.handleTabClick(player, slot)) {
            gui.openOrdersGUI(player);
            return;
        }

        CabinetDecision.DecisionType type = RepublicExecutiveOrdersMenu.getDecisionAtSlot(player, slot);
        if (type == null) {
            ExecutiveOrder.ExecutiveOrderType eoType = RepublicExecutiveOrdersMenu.getExecutiveOrderAtSlot(player, slot);
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
            MessageUtils.send(player, "<red>You are not a member of any nation.</red>");
            player.closeInventory();
            return;
        }

        player.closeInventory();
        UUID targetId = player.getUniqueId();
        if (player.hasPermission("nation.admin")) {
            Government gov = nation.getRepublicGovernment();
            if (gov != null) {
                UUID ministerUUID = gov.getCabinetMember(type.getPosition().toGovernmentPosition());
                if (ministerUUID != null) {
                    targetId = ministerUUID;
                }
            }
        }

        boolean success = plugin.getCabinetManager().executeDecision(nation, targetId, type);
        if (success) {
            MessageUtils.send(player, "<green>Decision successfully executed!</green>");
        } else {
            MessageUtils.send(player, "<red>Failed to execute decision. Check requirements and cooldown.</red>");
        }
    }

    public void handleMemberManagementGUI(Player player, ItemStack clicked, int slot) {
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.LIGHT_BLUE_STAINED_GLASS_PANE)
            return;

        MessageUtils.playSound(player, org.bukkit.Sound.UI_BUTTON_CLICK);

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) {
            player.closeInventory();
            return;
        }

        int page = gui.memberListPage.getOrDefault(player.getUniqueId(), 0);

        if (slot == 47) {
            gui.memberListPage.remove(player.getUniqueId());
            gui.republicGovernmentGUI.open(player, nation);
            return;
        }

        if (slot == 45 && clicked.getType() == Material.ARROW) {
            page = Math.max(0, page - 1);
            gui.memberListPage.put(player.getUniqueId(), page);
            gui.republicMemberManagementGUI.open(player, nation, page);
            return;
        }

        if (slot == 53 && clicked.getType() == Material.ARROW) {
            page++;
            gui.memberListPage.put(player.getUniqueId(), page);
            gui.republicMemberManagementGUI.open(player, nation, page);
            return;
        }

        if (clicked.getType() == Material.PLAYER_HEAD) {
            SkullMeta skull = (SkullMeta) clicked.getItemMeta();
            if (skull == null || skull.getOwningPlayer() == null)
                return;
            UUID targetUUID = skull.getOwningPlayer().getUniqueId();
            gui.viewingManagedMember.put(player.getUniqueId(), targetUUID);
            gui.republicMemberManagementGUI.openActionMenu(player, nation, targetUUID);
        }
    }

    public void handleMemberActionGUI(Player player, ItemStack clicked, int slot, String title) {
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.LIGHT_BLUE_STAINED_GLASS_PANE)
            return;

        MessageUtils.playSound(player, org.bukkit.Sound.UI_BUTTON_CLICK);

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) {
            player.closeInventory();
            return;
        }

        Government gov = nation.getRepublicGovernment();
        UUID targetUUID = gui.viewingManagedMember.get(player.getUniqueId());

        if (slot == 22 || clicked.getType() == Material.SPECTRAL_ARROW) {
            int page = gui.memberListPage.getOrDefault(player.getUniqueId(), 0);
            gui.republicMemberManagementGUI.open(player, nation, page);
            return;
        }

        if (targetUUID == null) {
            player.closeInventory();
            return;
        }

        boolean isPresident = gov != null && gov.hasPresident() && gov.getPresidentUUID().equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("nation.admin");
        if (!isPresident && !isAdmin) {
            MessageUtils.send(player, "§cOnly the President can manage members.");
            player.closeInventory();
            return;
        }

        if (slot == 11 && clicked.getType() == Material.RED_CONCRETE) {
            String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();
            if (targetName == null && FakeMember.isNpcUUID(targetUUID)) {
                FakeMember npc = nation.getFakeMember(targetUUID);
                if (npc != null) targetName = npc.getName();
            }
            final String finalName = targetName != null ? targetName : "NPC";

            gui.confirmActionGUI.open(player, "Kick " + finalName + " from nation",
                    () -> {
                        if (FakeMember.isNpcUUID(targetUUID)) {
                            var result = plugin.getFakeMemberManager().kickNpcByUUID(nation.getId(), targetUUID);
                            if (result.isSuccess()) {
                                MessageUtils.send(player, "<green>" + result.getMessage() + "</green>");
                                gui.viewingManagedMember.remove(player.getUniqueId());
                                int page = gui.memberListPage.getOrDefault(player.getUniqueId(), 0);
                                gui.republicMemberManagementGUI.open(player, nation, page);
                            } else {
                                MessageUtils.send(player, "<red>" + result.getMessage() + "</red>");
                                gui.republicMemberManagementGUI.openActionMenu(player, nation, targetUUID);
                            }
                            return;
                        }

                        id.nationcore.managers.NationManager.Result result = plugin.getNationManager()
                                .kickMember(player, targetUUID);
                        if (result.isSuccess()) {
                            MessageUtils.send(player, "<green>" + result.getMessage() + "</green>");
                            gui.viewingManagedMember.remove(player.getUniqueId());
                            int page = gui.memberListPage.getOrDefault(player.getUniqueId(), 0);
                            gui.republicMemberManagementGUI.open(player, nation, page);
                        } else {
                            MessageUtils.send(player, "<red>" + result.getMessage() + "</red>");
                            gui.republicMemberManagementGUI.openActionMenu(player, nation, targetUUID);
                        }
                    });
            return;
        }

        if (slot == 15) {
            if (FakeMember.isNpcUUID(targetUUID)) {
                FakeMember npc = nation.getFakeMember(targetUUID);
                if (npc == null) return;
                NationRole newRole = npc.getRole() == NationRole.OFFICER ? NationRole.CITIZEN : NationRole.OFFICER;
                var result = plugin.getFakeMemberManager().setNpcRole(nation.getId(), npc.getName(), newRole);
                if (result.isSuccess()) {
                    MessageUtils.send(player, "<green>" + result.getMessage() + "</green>");
                } else {
                    MessageUtils.send(player, "<red>" + result.getMessage() + "</red>");
                }
                gui.republicMemberManagementGUI.openActionMenu(player, nation, targetUUID);
                return;
            }

            NationMember targetMember = nation.getMember(targetUUID);
            if (targetMember == null) return;
            if (targetMember.getRole() == NationRole.OFFICER) {
                targetMember.setRole(NationRole.CITIZEN);
                plugin.getDataManager().saveNations();
                MessageUtils.send(player, "<green>" + targetMember.getName() + " has been demoted to Citizen.</green>");
            } else {
                targetMember.setRole(NationRole.OFFICER);
                plugin.getDataManager().saveNations();
                MessageUtils.send(player, "<green>" + targetMember.getName() + " has been promoted to Officer.</green>");
            }
            gui.republicMemberManagementGUI.openActionMenu(player, nation, targetUUID);
        }
    }
}
