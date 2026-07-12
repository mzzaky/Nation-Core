package id.nationcore.gui.republic;

import id.nationcore.gui.GUIListener;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

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

    public void handleGovernmentGUI(Player player, ItemStack clicked, int slot) {
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null)
            return;

        MessageUtils.playSound(player, org.bukkit.Sound.UI_BUTTON_CLICK);

        if (slot == 37 && clicked.getType() == Material.COMMAND_BLOCK_MINECART) {
            gui.openSettingsGUI(player);
            return;
        }

        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (slot == 43 || clicked.getType() == Material.PALE_OAK_DOOR) {
            gui.mainMenuRouter.openFor(player);
            return;
        }

        if (slot == 28) {
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

        if (slot == 34 || clicked.getType() == Material.KNOWLEDGE_BOOK) {
            gui.salaryMenu.open(player);
            return;
        }

        if (slot == 12) {
            if (isAuthorizedForOffice(player, nation, Government.CabinetPosition.HEALTH)) {
                gui.republicHealthOfficeGUI.open(player, nation);
            } else {
                MessageUtils.send(player, "<red>Only the President and the Minister of Health can open this office console.</red>");
                MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            }
            return;
        }

        if (slot == 14) {
            if (isAuthorizedForOffice(player, nation, Government.CabinetPosition.TREASURY)) {
                gui.republicTreasuryOfficeGUI.open(player, nation);
            } else {
                MessageUtils.send(player, "<red>Only the President and the Minister of Treasury can open this office console.</red>");
                MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            }
            return;
        }

        if (slot == 13) {
            if (isAuthorizedForOffice(player, nation, Government.CabinetPosition.DEFENSE)) {
                gui.republicDefenseOfficeGUI.open(player, nation);
            } else {
                MessageUtils.send(player, "<red>Only the President and the Minister of Defense can open this office console.</red>");
                MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            }
            return;
        }

        if (slot == 40) {
            boolean isActive = plugin.getArenaManager().isArenaActive(nation);
            if (isActive) {
                gui.arenaGUI.openArenaMenu(player);
            } else {
                Government gov = nation.getRepublicGovernment();
                boolean isPresident = gov != null && gov.hasPresident()
                        && gov.getPresidentUUID().equals(player.getUniqueId());
                if (isPresident && clicked.getType() == Material.WRITABLE_BOOK) {
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

        if (slot == 39) {
            gui.openOrdersGUI(player);
            return;
        }

        if (slot == 31) {
            int page = gui.memberListPage.getOrDefault(player.getUniqueId(), 0);
            gui.republicMemberManagementGUI.open(player, nation, page);
            return;
        }

        if (slot == 41) {
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

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) {
            MessageUtils.send(player, "<red>You are not a member of any nation.</red>");
            player.closeInventory();
            return;
        }

        ExecutiveOrder.ExecutiveOrderType eoType = id.nationcore.managers.ExecutiveOrderManager.orderAtSlot(
                plugin.getExecutiveOrderManager().getOrdersForPosition(nation.getType(), RepublicExecutiveOrdersMenu.POSITION_KEY),
                RepublicExecutiveOrdersMenu.ORDER_SLOTS, slot);
        if (eoType == null)
            return;

        if (clicked.getType() != Material.WRITABLE_BOOK) {
            MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        gui.confirmActionGUI.open(player, "Issue Decree: " + plugin.getExecutiveOrderManager().getOrderDisplay(eoType), () -> {
            plugin.getExecutiveOrderManager().issueOrderForNation(player, eoType);
        });
    }

    /** Confirm + issue an executive order clicked from a minister office console. */
    private void handleOfficeExecutiveOrder(Player player, ExecutiveOrder.ExecutiveOrderType eoType) {
        MessageUtils.playSound(player, org.bukkit.Sound.UI_BUTTON_CLICK);
        gui.confirmActionGUI.open(player, "Issue Decree: " + plugin.getExecutiveOrderManager().getOrderDisplay(eoType), () -> {
            plugin.getExecutiveOrderManager().issueOrderForNation(player, eoType);
        });
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

        boolean isPresident = gov != null && gov.hasPresident() && gov.getPresidentUUID().equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("nation.admin");
        if (!isPresident && !isAdmin) {
            MessageUtils.send(player, "§cOnly the President can manage members.");
            player.closeInventory();
            return;
        }

        // Slot 21 — Senator Action Button (Appoint / Remove)
        if (slot == 21) {
            if (gov != null && gov.hasPresident() && gov.getPresidentUUID().equals(targetUUID)) {
                MessageUtils.send(player, "<red>The President cannot be a Senator.</red>");
                return;
            }

            boolean isSenator = gov != null && gov.getSenators().contains(targetUUID);
            if (!isSenator && gov != null && gov.getPositionByUUID(targetUUID) != null) {
                MessageUtils.send(player, "<red>This member is a Minister and cannot be a Senator.</red>");
                return;
            }

            String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();
            if (targetName == null && FakeMember.isNpcUUID(targetUUID)) {
                FakeMember npc = nation.getFakeMember(targetUUID);
                if (npc != null) targetName = npc.getName();
            }
            final String finalTargetName = targetName != null ? targetName : "Unknown";

            if (isSenator) {
                gui.confirmActionGUI.open(player,
                        "Remove " + finalTargetName + " from Senators",
                        () -> {
                            if (gov != null) {
                                gov.getSenators().remove(targetUUID);
                                plugin.getDataManager().saveNations();
                                MessageUtils.send(player, "<green>Successfully removed " + finalTargetName + " from Senators.</green>");
                            }
                            gui.republicMemberManagementGUI.openActionMenu(player, nation, targetUUID);
                        });
            } else {
                if (gov != null && gov.getSenators().size() >= 5) {
                    MessageUtils.send(player, "<red>The Senator slots are full (maximum 5).</red>");
                    return;
                }
                gui.confirmActionGUI.open(player,
                        "Appoint " + finalTargetName + " as Senator",
                        () -> {
                            if (gov != null) {
                                gov.getSenators().add(targetUUID);
                                plugin.getDataManager().saveNations();
                                MessageUtils.send(player, "<green>Successfully appointed " + finalTargetName + " as Senator.</green>");
                            }
                            gui.republicMemberManagementGUI.openActionMenu(player, nation, targetUUID);
                        });
            }
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
                MessageUtils.send(player, "<red>⏰ You must wait <white>"
                        + MessageUtils.formatTime(remaining) + "</white> before messaging this member again.</red>");
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
            if (gov != null && gov.hasPresident() && gov.getPresidentUUID().equals(targetUUID)) {
                MessageUtils.send(player, "<red>The President cannot be a Minister.</red>");
                return;
            }
            if (gov != null && gov.getSenators().contains(targetUUID)) {
                MessageUtils.send(player, "<red>This member is a Senator and cannot be a Minister.</red>");
                return;
            }
            final Government.CabinetPosition finalPos = appointPos;
            String memberName = Bukkit.getOfflinePlayer(targetUUID).getName();
            if (memberName == null && FakeMember.isNpcUUID(targetUUID)) {
                FakeMember npc = nation.getFakeMember(targetUUID);
                if (npc != null) memberName = npc.getName();
            }
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

    public void handleHealthOfficeGUI(Player player, ItemStack clicked, int slot) {
        if (clicked == null || clicked.getType() == org.bukkit.Material.AIR)
            return;

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null)
            return;

        if (slot == 49) {
            MessageUtils.playSound(player, org.bukkit.Sound.UI_BUTTON_CLICK);
            gui.republicGovernmentGUI.open(player, nation);
            return;
        }

        ExecutiveOrder.ExecutiveOrderType eoType = id.nationcore.managers.ExecutiveOrderManager.orderAtSlot(
                plugin.getExecutiveOrderManager().getOrdersForPosition(nation.getType(), "minister_of_health"),
                id.nationcore.managers.ExecutiveOrderManager.OFFICE_ORDER_SLOTS, slot);
        if (eoType != null) {
            handleOfficeExecutiveOrder(player, eoType);
            return;
        }

        org.bukkit.configuration.file.YamlConfiguration config = plugin.getNationConfig(nation.getType());
        List<String> orderKeys = config != null ? config.getStringList("executive_order.minister_of_health") : new ArrayList<>();
        int[] decisionSlots = {20, 22, 24, 30, 32};
        CabinetDecision.DecisionType type = null;
        for (int i = 0; i < decisionSlots.length && i < orderKeys.size(); i++) {
            if (slot == decisionSlots[i]) {
                try {
                    type = CabinetDecision.DecisionType.valueOf(orderKeys.get(i).trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    // skip
                }
                break;
            }
        }

        if (type != null) {
            handleOfficeDecisionClick(player, nation, type);
        }
    }

    public void handleDefenseOfficeGUI(Player player, ItemStack clicked, int slot) {
        if (clicked == null || clicked.getType() == org.bukkit.Material.AIR)
            return;

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null)
            return;

        if (slot == 49) {
            MessageUtils.playSound(player, org.bukkit.Sound.UI_BUTTON_CLICK);
            gui.republicGovernmentGUI.open(player, nation);
            return;
        }

        ExecutiveOrder.ExecutiveOrderType eoType = id.nationcore.managers.ExecutiveOrderManager.orderAtSlot(
                plugin.getExecutiveOrderManager().getOrdersForPosition(nation.getType(), "minister_of_defence"),
                id.nationcore.managers.ExecutiveOrderManager.OFFICE_ORDER_SLOTS, slot);
        if (eoType != null) {
            handleOfficeExecutiveOrder(player, eoType);
            return;
        }

        org.bukkit.configuration.file.YamlConfiguration config = plugin.getNationConfig(nation.getType());
        List<String> orderKeys = config != null ? config.getStringList("executive_order.minister_of_defence") : new ArrayList<>();
        int[] decisionSlots = {20, 22, 24, 30, 32};
        CabinetDecision.DecisionType type = null;
        for (int i = 0; i < decisionSlots.length && i < orderKeys.size(); i++) {
            if (slot == decisionSlots[i]) {
                try {
                    type = CabinetDecision.DecisionType.valueOf(orderKeys.get(i).trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    // skip
                }
                break;
            }
        }

        if (type != null) {
            handleOfficeDecisionClick(player, nation, type);
        }
    }

    public void handleTreasuryOfficeGUI(Player player, ItemStack clicked, int slot) {
        if (clicked == null || clicked.getType() == org.bukkit.Material.AIR)
            return;

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null)
            return;

        if (slot == 49) {
            MessageUtils.playSound(player, org.bukkit.Sound.UI_BUTTON_CLICK);
            gui.republicGovernmentGUI.open(player, nation);
            return;
        }

        ExecutiveOrder.ExecutiveOrderType eoType = id.nationcore.managers.ExecutiveOrderManager.orderAtSlot(
                plugin.getExecutiveOrderManager().getOrdersForPosition(nation.getType(), "minister_of_treasury"),
                id.nationcore.managers.ExecutiveOrderManager.OFFICE_ORDER_SLOTS, slot);
        if (eoType != null) {
            handleOfficeExecutiveOrder(player, eoType);
            return;
        }

        org.bukkit.configuration.file.YamlConfiguration config = plugin.getNationConfig(nation.getType());
        List<String> orderKeys = config != null ? config.getStringList("executive_order.minister_of_treasury") : new ArrayList<>();
        int[] decisionSlots = {20, 22, 24, 30, 32};
        CabinetDecision.DecisionType type = null;
        for (int i = 0; i < decisionSlots.length && i < orderKeys.size(); i++) {
            if (slot == decisionSlots[i]) {
                try {
                    type = CabinetDecision.DecisionType.valueOf(orderKeys.get(i).trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    // skip
                }
                break;
            }
        }

        if (type != null) {
            handleOfficeDecisionClick(player, nation, type);
        }
    }

    private void handleOfficeDecisionClick(Player player, Nation nation, CabinetDecision.DecisionType type) {
        Government gov = nation.getRepublicGovernment();
        if (gov == null) return;

        Government.CabinetPosition requiredPosition = type.getPosition().toGovernmentPosition();
        UUID ministerUUID = gov.getCabinetMember(requiredPosition);

        boolean isMinister = ministerUUID != null && ministerUUID.equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("nation.admin");

        if (!isMinister && !isAdmin) {
            MessageUtils.send(player, "<red>Only the " + requiredPosition.getDisplayName() + " can execute this decision.</red>");
            MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        if (ministerUUID == null && isAdmin) {
            MessageUtils.send(player, "<red>Cannot execute decision because the Minister position is vacant.</red>");
            MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        if (plugin.getCabinetManager().isDecisionActive(nation, type)) {
            MessageUtils.send(player, "<red>This decision is already active.</red>");
            MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        long cooldownRemaining = plugin.getCabinetManager().getRemainingCooldown(
                ministerUUID != null ? ministerUUID : player.getUniqueId(), type);
        if (cooldownRemaining > 0) {
            MessageUtils.send(player, "<red>This decision is on cooldown.</red>");
            MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        int cost = plugin.getCabinetManager().getDecisionCost(type);
        if (!plugin.getTreasuryManager().canAfford(nation, cost)) {
            MessageUtils.send(player, "<red>Insufficient treasury funds.</red>");
            MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        MessageUtils.playSound(player, org.bukkit.Sound.UI_BUTTON_CLICK);
        gui.confirmActionGUI.open(player, "Execute: " + type.getDisplayName(), () -> {
            UUID targetId = isAdmin && !isMinister ? ministerUUID : player.getUniqueId();
            boolean success = plugin.getCabinetManager().executeDecision(nation, targetId, type);
            if (success) {
                MessageUtils.send(player, "<green>Decision successfully executed!</green>");
                MessageUtils.playSound(player, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP);
            } else {
                MessageUtils.send(player, "<red>Failed to execute decision. Check requirements and cooldown.</red>");
                MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            }
        });
    }

    private boolean isAuthorizedForOffice(Player player, Nation nation, Government.CabinetPosition position) {
        if (nation == null) return false;
        Government gov = nation.getRepublicGovernment();
        if (gov == null) return false;

        boolean isPresident = gov.hasPresident() && gov.getPresidentUUID().equals(player.getUniqueId());
        boolean isThisMinister = gov.getCabinetMember(position) != null && gov.getCabinetMember(position).equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("nation.admin");

        return isPresident || isThisMinister || isAdmin;
    }
}
