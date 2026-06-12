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
            gui.republicTaxMenu.open(player);
            return;
        }
        if (slot == RepublicMainMenu.getSlot("HELP")) {
            gui.helpGUI.openHelpMenu(player);
            return;
        }
        if (slot == RepublicMainMenu.getSlot("CAPITAL")) {
            Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
            if (nation == null) {
                MessageUtils.send(player, "<red>You are not in a nation.</red>");
                player.closeInventory();
                return;
            }
            if (!nation.hasCapital()) {
                MessageUtils.send(player, "<red>Your nation has not established a capital yet.</red>");
                player.closeInventory();
                return;
            }

            long now = System.currentTimeMillis();
            long cooldownMs = 15L * 60 * 1000;
            Long lastTeleport = gui.capitalTeleportCooldowns.get(player.getUniqueId());
            if (lastTeleport != null && (now - lastTeleport) < cooldownMs && !player.hasPermission("nation.admin")) {
                long remaining = cooldownMs - (now - lastTeleport);
                MessageUtils.send(player, "<red>Capital teleport is on cooldown. Remaining: <white>"
                        + MessageUtils.formatTime(remaining) + "</white></red>");
                player.closeInventory();
                return;
            }

            double cost = 100.0;
            if (!plugin.getVaultHook().has(player.getUniqueId(), cost) && !player.hasPermission("nation.admin")) {
                MessageUtils.send(player,
                        "<red>You do not have enough money to teleport. Cost: <gold>$" + cost + "</gold></red>");
                player.closeInventory();
                return;
            }

            // Calculate chunk middle
            Nation.CapitalLocation cap = nation.getCapital();
            org.bukkit.World world = Bukkit.getWorld(cap.getWorld());
            if (world == null) {
                MessageUtils.send(player, "<red>Could not find the Capital world. Is it loaded?</red>");
                player.closeInventory();
                return;
            }

            // Charge Vault balance (except for admins/permission bypass)
            if (!player.hasPermission("nation.admin")) {
                plugin.getVaultHook().withdraw(player.getUniqueId(), cost);
            }

            int chunkX = ((int) Math.floor(cap.getX())) >> 4;
            int chunkZ = ((int) Math.floor(cap.getZ())) >> 4;
            double midX = (chunkX * 16) + 8.5;
            double midZ = (chunkZ * 16) + 8.5;
            double y = cap.getY();

            org.bukkit.Location targetLoc = new org.bukkit.Location(world, midX, y, midZ, cap.getYaw(), cap.getPitch());

            player.closeInventory();
            player.teleport(targetLoc);

            // Set cooldown
            gui.capitalTeleportCooldowns.put(player.getUniqueId(), now);

            MessageUtils.send(player, "<green>Teleported to the Capital of <gold>" + nation.getName()
                    + "</gold> for <gold>$" + cost + "</gold>.</green>");
            MessageUtils.playSound(player, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT);
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

        if (slot == 10) {
            Government gov = nation.getRepublicGovernment();
            boolean isPresident = gov != null && gov.hasPresident()
                    && gov.getPresidentUUID().equals(player.getUniqueId());
            if (!isPresident && !player.hasPermission("nation.admin")) {
                MessageUtils.send(player, "<red>Only the President can set the announcement message.</red>");
                return;
            }

            long lastAnn = nation.getLastAnnouncementTime();
            long timeSinceLastAnn = System.currentTimeMillis() - lastAnn;
            long cooldownDurationAnn = 12L * 60 * 60 * 1000;
            if (timeSinceLastAnn < cooldownDurationAnn) {
                long remainingAnn = cooldownDurationAnn - timeSinceLastAnn;
                MessageUtils.send(player, "<red>The announcement message is on cooldown. Remaining: <white>"
                        + MessageUtils.formatTime(remainingAnn) + "</white></red>");
                return;
            }

            player.closeInventory();
            id.nationcore.listeners.ChatListener.pendingAnnouncementMessages.put(player.getUniqueId(), nation);
            MessageUtils.send(player, "<yellow>Please type the announcement message in chat. Type 'cancel' to abort.</yellow>");
            return;
        }

        if (slot == 32) {
            gui.republicDiplomacyMenu.openManagement(player, nation);
            return;
        }

        if (slot == 30) {
            gui.republicBorderMenu.open(player, nation);
            return;
        }

        if (slot == 37 || clicked.getType() == Material.EMERALD) {
            gui.salaryMenu.open(player);
            return;
        }

        if (slot == 48) {
            Government gov = nation.getRepublicGovernment();
            boolean isPresident = gov != null && gov.hasPresident()
                    && gov.getPresidentUUID().equals(player.getUniqueId());
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
            boolean isPresident = gov != null && gov.hasPresident()
                    && gov.getPresidentUUID().equals(player.getUniqueId());
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

        if (slot == 31) {
            boolean isActive = plugin.getArenaManager().isArenaActive(nation);
            if (isActive) {
                gui.arenaGUI.openArenaMenu(player);
            } else {
                Government gov = nation.getRepublicGovernment();
                boolean isPresident = gov != null && gov.hasPresident()
                        && gov.getPresidentUUID().equals(player.getUniqueId());
                if (isPresident && clicked.getType() == Material.PRIZE_POTTERY_SHERD) {
                    gui.confirmActionGUI.open(player, "Start Arena Game", () -> {
                        if (!plugin.getArenaManager().startArena(nation, player.getUniqueId())) {
                            MessageUtils.send(player, "<red>Cannot start arena! Check requirements.</red>");
                        } else {
                            gui.arenaGUI.openArenaMenu(player);
                        }
                    });
                } else {
                    MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
                }
            }
            return;
        }

        if (slot == 21 || clicked.getType() == Material.ARMS_UP_POTTERY_SHERD) {
            gui.openOrdersGUI(player);
            return;
        }

        if (slot == 22 || clicked.getType() == Material.FRIEND_POTTERY_SHERD) {
            int page = gui.memberListPage.getOrDefault(player.getUniqueId(), 0);
            gui.republicMemberManagementGUI.open(player, nation, page);
            return;
        }

        if (slot == 23) {
            Government gov = nation.getRepublicGovernment();
            if (gov == null)
                return;
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
                MessageUtils.send(player, "<red>Broadcast is on cooldown. Remaining: <white>"
                        + MessageUtils.formatTime(remaining) + "</white></red>");
                return;
            }

            player.closeInventory();
            id.nationcore.listeners.ChatListener.pendingNationBroadcasts.put(player.getUniqueId(), nation);
            MessageUtils.send(player,
                    "<yellow>Please type your custom broadcast message in chat. Type 'cancel' to abort.</yellow>");
            return;
        }
    }

    public void handleOrdersGUI(Player player, ItemStack clicked, int slot) {
        if (RepublicExecutiveOrdersMenu.SLOT_CLOSE == slot || clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }
        if (RepublicExecutiveOrdersMenu.SLOT_BACK == slot || clicked.getType() == Material.SPECTRAL_ARROW
                || clicked.getType() == Material.ARROW) {
            gui.openGovernmentGUI(player);
            return;
        }

        if (RepublicExecutiveOrdersMenu.handleTabClick(player, slot)) {
            gui.openOrdersGUI(player);
            return;
        }

        CabinetDecision.DecisionType type = RepublicExecutiveOrdersMenu.getDecisionAtSlot(player, slot);
        if (type == null) {
            ExecutiveOrder.ExecutiveOrderType eoType = RepublicExecutiveOrdersMenu.getExecutiveOrderAtSlot(player,
                    slot);
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

        if (slot == 43) {
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

        // Slot 43 — Back to member list
        if (slot == 43 || clicked.getType() == Material.SPECTRAL_ARROW) {
            int page = gui.memberListPage.getOrDefault(player.getUniqueId(), 0);
            gui.republicMemberManagementGUI.open(player, nation, page);
            return;
        }

        if (targetUUID == null) {
            player.closeInventory();
            return;
        }

        // Slot 21 — Warn (Coming Soon)
        if (slot == 21) {
            MessageUtils.send(player, "§d⏳ The Warn feature is coming soon!");
            return;
        }

        boolean isPresident = gov != null && gov.hasPresident() && gov.getPresidentUUID().equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("nation.admin");
        if (!isPresident && !isAdmin) {
            MessageUtils.send(player, "§cOnly the President can manage members.");
            player.closeInventory();
            return;
        }

        // Slot 22 — Send Message (1-hour cooldown per target member)
        if (slot == 22 && clicked.getType() == Material.WRITABLE_BOOK) {
            // Check cooldown: key = presidentUUID, value = map<targetUUID, timestamp>
            long now = System.currentTimeMillis();
            long cooldownMs = 60L * 60 * 1000; // 1 hour
            java.util.Map<UUID, Long> cooldowns = gui.memberMessageCooldowns
                    .computeIfAbsent(player.getUniqueId(), k -> new java.util.HashMap<>());
            Long lastSent = cooldowns.get(targetUUID);
            if (lastSent != null && (now - lastSent) < cooldownMs) {
                long remaining = cooldownMs - (now - lastSent);
                MessageUtils.send(player, "§c⏰ You must wait §f"
                        + MessageUtils.formatTime(remaining) + " §cbefore messaging this member again.");
                return;
            }

            // Get target name for the pending chat map
            String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();
            if (targetName == null) {
                FakeMember npc = nation.getFakeMember(targetUUID);
                targetName = (npc != null) ? npc.getName() : "Unknown";
            }

            player.closeInventory();
            final String finalTargetName = targetName;
            id.nationcore.listeners.ChatListener.pendingMemberMessages.put(
                    player.getUniqueId(),
                    new id.nationcore.listeners.ChatListener.PendingMemberMessage(nation, targetUUID, finalTargetName));
            MessageUtils.send(player, "§bType your presidential message to §f" + finalTargetName
                    + "§b in chat. Type §fcanceled§b to abort.");
            return;
        }

        // Slot 23 — Kick Member
        if (slot == 23 && clicked.getType() == Material.BOOK) {
            String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();
            if (targetName == null && FakeMember.isNpcUUID(targetUUID)) {
                FakeMember npc = nation.getFakeMember(targetUUID);
                if (npc != null)
                    targetName = npc.getName();
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

        // Slot 30/31/32 — Appoint Cabinet Minister
        Government.CabinetPosition appointPos = switch (slot) {
            case 30 -> Government.CabinetPosition.TREASURY;
            case 31 -> Government.CabinetPosition.DEFENSE;
            case 32 -> Government.CabinetPosition.HEALTH;
            default -> null;
        };

        if (appointPos != null && clicked.getType() == Material.WRITTEN_BOOK) {
            if (FakeMember.isNpcUUID(targetUUID)) {
                MessageUtils.send(player, "<red>Fake members (NPCs) cannot be appointed to cabinet positions.</red>");
                return;
            }
            final Government.CabinetPosition finalPos = appointPos;
            String memberName = Bukkit.getOfflinePlayer(targetUUID).getName();
            final String finalMemberName = memberName != null ? memberName : "Unknown";

            gui.confirmActionGUI.open(player,
                    "Appoint " + finalMemberName + " as " + finalPos.getDisplayName(),
                    () -> {
                        plugin.getGovernmentManager().appointCabinet(nation, finalPos, targetUUID, finalMemberName);
                        plugin.getDataManager().saveNations();
                        gui.republicMemberManagementGUI.openActionMenu(player, nation, targetUUID);
                    });
        }
    }
}
