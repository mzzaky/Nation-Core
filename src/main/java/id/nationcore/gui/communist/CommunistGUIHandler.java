package id.nationcore.gui.communist;

import id.nationcore.gui.GUIListener;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import id.nationcore.NationCore;
import id.nationcore.models.CommunistGovernment;
import id.nationcore.models.FakeMember;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

/**
 * Handles GUI clicks specific to Communist nations:
 * main menu, government, executive orders, member management, shared storage.
 */
public class CommunistGUIHandler {

    private final NationCore plugin;
    private final GUIListener gui;

    public CommunistGUIHandler(NationCore plugin, GUIListener gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    public void handleMainMenu(Player player, ItemStack clicked, int slot) {
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (CommunistMainMenu.isFiller(clicked.getType()))
            return;

        MessageUtils.playSound(player, org.bukkit.Sound.UI_BUTTON_CLICK);

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        CommunistGovernment cg = nation != null ? nation.getCommunistGovernment() : null;

        boolean isSekjen = cg != null && cg.hasSecretaryGeneral()
                && cg.getSecretaryGeneralUUID().equals(player.getUniqueId());
        boolean isCabinet = cg != null && cg.getPositionByUUID(player.getUniqueId()) != null;
        boolean isParty = cg != null && cg.isPartyMember(player.getUniqueId());
        boolean isAdmin = player.hasPermission("nation.admin");

        boolean presidentOrCabinet = isSekjen || isCabinet || isAdmin;
        boolean presidentCabinetOrParty = presidentOrCabinet || isParty;

        if (slot == CommunistMainMenu.getSlot("NATION_PROFILE")) {
            return;
        }

        if (slot == CommunistMainMenu.getSlot("PARTY_POLICY")) {
            return;
        }

        if (slot == CommunistMainMenu.getSlot("CABINET")) {
            return;
        }

        if (slot == CommunistMainMenu.getSlot("SEKJEN")) {
            if (!presidentOrCabinet) {
                MessageUtils.send(player,
                        "<red>Only the Secretary General and Cabinet can access this menu.</red>");
                return;
            }
            gui.openGovernmentGUI(player);
            return;
        }

        if (slot == CommunistMainMenu.getSlot("PARTY_MEMBERS")) {
            return;
        }

        if (slot == CommunistMainMenu.getSlot("TREASURY")) {
            if (!presidentOrCabinet) {
                MessageUtils.send(player,
                        "<red>Only the Secretary General and Cabinet can access this menu.</red>");
                return;
            }
            gui.communistTreasuryMenu.open(player, plugin.getNationManager().getNationOf(player.getUniqueId()));
            return;
        }

        if (slot == CommunistMainMenu.getSlot("NOTIFICATION")) {
            return;
        }

        if (slot == CommunistMainMenu.getSlot("SHARED_STORAGE")) {
            if (nation == null) {
                MessageUtils.send(player, "<red>You are not in a nation.</red>");
                return;
            }
            gui.communistSharedStorageGUI.open(player, nation);
            return;
        }

        if (slot == CommunistMainMenu.getSlot("TAX")) {
            gui.communistTaxMenu.open(player);
            return;
        }

        if (slot == CommunistMainMenu.getSlot("AID")) {
            return;
        }

        if (slot == CommunistMainMenu.getSlot("GRAND_EVENT")) {
            return;
        }

        if (slot == CommunistMainMenu.getSlot("GUIDE")) {
            return;
        }

        if (slot == CommunistMainMenu.getSlot("CAPITAL")) {
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

        if (slot == CommunistMainMenu.getSlot("RESEARCH")) {
            gui.researchGUI.openMain(player);
            return;
        }

        if (slot == CommunistMainMenu.getSlot("LAW")) {
            return;
        }

        if (slot == CommunistMainMenu.getSlot("HUB")) {
            gui.openHubGUI(player);
            return;
        }
    }

    public void handleGovernmentGUI(Player player, ItemStack clicked, int slot) {
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.RED_STAINED_GLASS_PANE)
            return;

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null)
            return;

        MessageUtils.playSound(player, org.bukkit.Sound.UI_BUTTON_CLICK);

        if (slot == 37 && clicked.getType() == Material.COMMAND_BLOCK_MINECART) {
            gui.openSettingsGUI(player);
            return;
        }

        if (slot == 34 || clicked.getType() == Material.KNOWLEDGE_BOOK) {
            gui.communistSalaryMenu.open(player);
            return;
        }

        if (slot == 43 || clicked.getType() == Material.PALE_OAK_DOOR) {
            gui.mainMenuRouter.openFor(player);
            return;
        }

        if (slot == 28) {
            CommunistGovernment cg = nation.getCommunistGovernment();
            boolean isSekjen = cg != null && cg.hasSecretaryGeneral()
                    && cg.getSecretaryGeneralUUID().equals(player.getUniqueId());
            if (!isSekjen && !player.hasPermission("nation.admin")) {
                MessageUtils.send(player, "<red>Only the Secretary General can set the announcement message.</red>");
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
            gui.communistDiplomacyMenu.openManagement(player, nation);
            return;
        }

        if (slot == 30) {
            gui.communistBorderMenu.open(player, nation);
            return;
        }

        if (slot == 31) {
            int page = gui.memberListPage.getOrDefault(player.getUniqueId(), 0);
            gui.communistMemberManagementGUI.open(player, nation, page);
            return;
        }

        if (slot == 39) {
            new CommunistLeaderOrdersMenu(plugin).open(player, nation);
            return;
        }

        if (slot == 41) {
            CommunistGovernment cg = nation.getCommunistGovernment();
            if (cg == null)
                return;
            boolean isSekjen = cg.hasSecretaryGeneral() && cg.getSecretaryGeneralUUID().equals(player.getUniqueId());
            boolean isAdmin = player.hasPermission("nation.admin");
            if (!isSekjen && !isAdmin) {
                MessageUtils.send(player, "<red>Only the Secretary General can broadcast a message.</red>");
                return;
            }

            long lastBroadcast = cg.getLastBroadcastTime();
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

        if (slot == 10) {
            if (isAuthorizedForOffice(player, nation, CommunistGovernment.PolitburoPosition.HEALTH)) {
                gui.communistHealthOfficeGUI.open(player, nation);
            } else {
                MessageUtils.send(player, "<red>Only the Secretary General and the Minister of Health can open this office console.</red>");
                MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            }
            return;
        }

        if (slot == 12) {
            if (isAuthorizedForOffice(player, nation, CommunistGovernment.PolitburoPosition.PROPAGANDA)) {
                gui.communistPropagandaOfficeGUI.open(player, nation);
            } else {
                MessageUtils.send(player, "<red>Only the Secretary General and the Minister of Propaganda can open this office console.</red>");
                MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            }
            return;
        }

        if (slot == 14) {
            if (isAuthorizedForOffice(player, nation, CommunistGovernment.PolitburoPosition.DEFENSE)) {
                gui.communistDefenseOfficeGUI.open(player, nation);
            } else {
                MessageUtils.send(player, "<red>Only the Secretary General and the Minister of Defense can open this office console.</red>");
                MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            }
            return;
        }

        if (slot == 16) {
            if (isAuthorizedForOffice(player, nation, CommunistGovernment.PolitburoPosition.TREASURY)) {
                gui.communistTreasuryOfficeGUI.open(player, nation);
            } else {
                MessageUtils.send(player, "<red>Only the Secretary General and the Minister of Treasury can open this office console.</red>");
                MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            }
            return;
        }

        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }
    }

    public void handleLeaderOrdersGUI(Player player, ItemStack clicked, int slot) {
        if (CommunistLeaderOrdersMenu.SLOT_CLOSE == slot || clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }
        if (CommunistLeaderOrdersMenu.SLOT_BACK == slot || clicked.getType() == Material.SPECTRAL_ARROW
                || clicked.getType() == Material.ARROW) {
            gui.openGovernmentGUI(player);
            return;
        }

        id.nationcore.models.ExecutiveOrder.ExecutiveOrderType eoType = CommunistLeaderOrdersMenu.getExecutiveOrderAtSlot(player, slot);
        if (eoType == null)
            return;

        if (clicked.getType() != Material.WRITABLE_BOOK) {
            MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) {
            MessageUtils.send(player, "<red>You are not a member of any nation.</red>");
            player.closeInventory();
            return;
        }

        // Security check
        CommunistGovernment cg = nation.getCommunistGovernment();
        boolean isSekjen = cg != null && cg.hasSecretaryGeneral()
                && cg.getSecretaryGeneralUUID().equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("nation.admin");
        if (!isSekjen && !isAdmin) {
            MessageUtils.send(player, "<red>Only the Secretary General can issue decrees.</red>");
            player.closeInventory();
            return;
        }

        gui.confirmActionGUI.open(player, "Issue Decree: " + eoType.getDisplayName(), () -> {
            plugin.getExecutiveOrderManager().issueOrderForNation(player, eoType);
        });
    }



    public void handleMemberManagementGUI(Player player, ItemStack clicked, int slot) {
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.RED_STAINED_GLASS_PANE)
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
            gui.communistGovernmentGUI.open(player, nation);
            return;
        }

        if (slot == 48 && clicked.getType() == Material.ARROW) {
            page = Math.max(0, page - 1);
            gui.memberListPage.put(player.getUniqueId(), page);
            gui.communistMemberManagementGUI.open(player, nation, page);
            return;
        }

        if (slot == 50 && clicked.getType() == Material.ARROW) {
            page++;
            gui.memberListPage.put(player.getUniqueId(), page);
            gui.communistMemberManagementGUI.open(player, nation, page);
            return;
        }

        if (clicked.getType() == Material.PLAYER_HEAD) {
            SkullMeta skull = (SkullMeta) clicked.getItemMeta();
            if (skull == null || skull.getOwningPlayer() == null)
                return;
            UUID targetUUID = skull.getOwningPlayer().getUniqueId();
            gui.viewingManagedMember.put(player.getUniqueId(), targetUUID);
            gui.communistMemberManagementGUI.openActionMenu(player, nation, targetUUID);
        }
    }

    public void handleMemberActionGUI(Player player, ItemStack clicked, int slot, String title) {
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.RED_STAINED_GLASS_PANE)
            return;

        MessageUtils.playSound(player, org.bukkit.Sound.UI_BUTTON_CLICK);

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) {
            player.closeInventory();
            return;
        }

        CommunistGovernment cg = nation.getCommunistGovernment();
        UUID targetUUID = gui.viewingManagedMember.get(player.getUniqueId());

        if (slot == 43 || clicked.getType() == Material.SPECTRAL_ARROW) {
            int page = gui.memberListPage.getOrDefault(player.getUniqueId(), 0);
            gui.communistMemberManagementGUI.open(player, nation, page);
            return;
        }

        if (targetUUID == null) {
            player.closeInventory();
            return;
        }

        boolean isSekjen = cg != null && cg.hasSecretaryGeneral()
                && cg.getSecretaryGeneralUUID().equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("nation.admin");
        if (!isSekjen && !isAdmin) {
            MessageUtils.send(player, "§cOnly the Secretary General can manage members.");
            player.closeInventory();
            return;
        }

        // Slot 21 — Party Member Action Button
        if (slot == 21) {
            if (cg == null) return;
            boolean isTargetSekjen = cg.hasSecretaryGeneral() && cg.getSecretaryGeneralUUID().equals(targetUUID);
            if (isTargetSekjen) {
                MessageUtils.send(player, "<red>The Secretary General is already in the Party.</red>");
                return;
            }
            boolean isPolitburo = cg.getPolitburoMemberByUUID(targetUUID) != null;
            if (isPolitburo) {
                MessageUtils.send(player, "<red>This member is in the Politburo and cannot be removed from the Party.</red>");
                return;
            }

            boolean isParty = cg.isPartyMember(targetUUID);
            String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();
            final String finalTargetName = targetName != null ? targetName : "Unknown";

            if (isParty) {
                gui.confirmActionGUI.open(player,
                        "Remove " + finalTargetName + " from Party",
                        () -> {
                            cg.removePartyMember(targetUUID);
                            plugin.getDataManager().saveNations();
                            MessageUtils.send(player, "<green>Successfully removed " + finalTargetName + " from the Party.</green>");
                            
                            org.bukkit.entity.Player targetOnline = Bukkit.getPlayer(targetUUID);
                            if (targetOnline != null) {
                                MessageUtils.send(targetOnline, "<red>🚩 You have been removed from the Party of " + nation.getName() + ".</red>");
                            }
                            gui.communistMemberManagementGUI.openActionMenu(player, nation, targetUUID);
                        });
            } else {
                if (cg.getPartyMemberCount() >= 5) {
                    MessageUtils.send(player, "<red>The Party slots are full (maximum 5).</red>");
                    return;
                }
                gui.confirmActionGUI.open(player,
                        "Appoint " + finalTargetName + " as Party Member",
                        () -> {
                            cg.addPartyMember(targetUUID);
                            plugin.getDataManager().saveNations();
                            MessageUtils.send(player, "<green>Successfully appointed " + finalTargetName + " as Party Member.</green>");
                            
                            org.bukkit.entity.Player targetOnline = Bukkit.getPlayer(targetUUID);
                            if (targetOnline != null) {
                                MessageUtils.send(targetOnline, "<gold>🚩 You have been admitted to the Party of " + nation.getName() + "!</gold>");
                            }
                            gui.communistMemberManagementGUI.openActionMenu(player, nation, targetUUID);
                        });
            }
            return;
        }

        // Slot 22 — Send Message
        if (slot == 22 && clicked.getType() == Material.WRITABLE_BOOK) {
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

            String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();
            final String finalTargetName = targetName != null ? targetName : "Unknown";

            player.closeInventory();
            id.nationcore.listeners.ChatListener.pendingMemberMessages.put(
                    player.getUniqueId(),
                    new id.nationcore.listeners.ChatListener.PendingMemberMessage(nation, targetUUID, finalTargetName));
            MessageUtils.send(player, "§bType your message to §f" + finalTargetName
                    + "§b in chat. Type §fcanceled§b to abort.");
            return;
        }

        // Slot 23 — Kick Member
        if (slot == 23 && clicked.getType() == Material.BOOK) {
            String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();
            if (targetName == null && FakeMember.isNpcUUID(targetUUID)) {
                FakeMember npc = nation.getFakeMember(targetUUID);
                if (npc != null) targetName = npc.getName();
            }
            final String finalName = targetName != null ? targetName : "Unknown";

            gui.confirmActionGUI.open(player, "Kick " + finalName + " from nation",
                    () -> {
                        if (FakeMember.isNpcUUID(targetUUID)) {
                            var result = plugin.getFakeMemberManager().kickNpcByUUID(nation.getId(), targetUUID);
                            if (result.isSuccess()) {
                                MessageUtils.send(player, "<green>" + result.getMessage() + "</green>");
                                gui.viewingManagedMember.remove(player.getUniqueId());
                                int page = gui.memberListPage.getOrDefault(player.getUniqueId(), 0);
                                gui.communistMemberManagementGUI.open(player, nation, page);
                            } else {
                                MessageUtils.send(player, "<red>" + result.getMessage() + "</red>");
                                gui.communistMemberManagementGUI.openActionMenu(player, nation, targetUUID);
                            }
                            return;
                        }

                        id.nationcore.managers.NationManager.Result result = plugin.getNationManager()
                                .kickMember(player, targetUUID);
                        if (result.isSuccess()) {
                            MessageUtils.send(player, "<green>" + result.getMessage() + "</green>");
                            gui.viewingManagedMember.remove(player.getUniqueId());
                            int page = gui.memberListPage.getOrDefault(player.getUniqueId(), 0);
                            gui.communistMemberManagementGUI.open(player, nation, page);
                        } else {
                            MessageUtils.send(player, "<red>" + result.getMessage() + "</red>");
                            gui.communistMemberManagementGUI.openActionMenu(player, nation, targetUUID);
                        }
                    });
            return;
        }

        // Slots 29, 30, 32, 33 — Appoint Politburo Minister
        CommunistGovernment.PolitburoPosition appointPos = switch (slot) {
            case 29 -> CommunistGovernment.PolitburoPosition.PROPAGANDA;
            case 30 -> CommunistGovernment.PolitburoPosition.DEFENSE;
            case 32 -> CommunistGovernment.PolitburoPosition.TREASURY;
            case 33 -> CommunistGovernment.PolitburoPosition.HEALTH;
            default -> null;
        };

        if (appointPos != null && clicked.getType() == Material.WRITTEN_BOOK) {
            if (cg != null && cg.hasSecretaryGeneral() && cg.getSecretaryGeneralUUID().equals(targetUUID)) {
                MessageUtils.send(player, "<red>The Secretary General cannot be a Minister.</red>");
                return;
            }
            if (cg != null && !cg.isPartyMember(targetUUID)) {
                MessageUtils.send(player, "<red>Must be a Party member first.</red>");
                return;
            }
            final CommunistGovernment.PolitburoPosition finalPos = appointPos;
            String memberName = Bukkit.getOfflinePlayer(targetUUID).getName();
            if (memberName == null && FakeMember.isNpcUUID(targetUUID)) {
                FakeMember npc = nation.getFakeMember(targetUUID);
                if (npc != null) memberName = npc.getName();
            }
            final String finalMemberName = memberName != null ? memberName : "Unknown";

            gui.confirmActionGUI.open(player,
                    "Appoint " + finalMemberName + " as " + finalPos.getDisplayName(),
                    () -> {
                        var result = plugin.getCommunistManager().appointPolitburo(nation, finalPos, targetUUID, finalMemberName);
                        if (result.isSuccess()) {
                            MessageUtils.send(player, "<green>" + result.getMessage() + "</green>");
                            plugin.getDataManager().saveNations();
                        } else {
                            MessageUtils.send(player, "<red>" + result.getMessage() + "</red>");
                        }
                        gui.communistMemberManagementGUI.openActionMenu(player, nation, targetUUID);
                    });
        }
    }

    public void handlePolitburoPickerGUI(Player player, ItemStack clicked, int slot, String title) {
        // Obsolete position picker
    }

    public void handleSharedStorage(InventoryClickEvent event, Player player, ItemStack clicked, int rawSlot) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) {
            player.closeInventory();
            return;
        }
        gui.communistSharedStorageGUI.handleClick(event, player, nation, rawSlot, clicked, event.getCursor());
    }

    public void handleHealthOfficeGUI(Player player, ItemStack clicked, int slot) {
        if (clicked == null || clicked.getType() == org.bukkit.Material.AIR)
            return;

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null)
            return;

        if (slot == 49) {
            MessageUtils.playSound(player, org.bukkit.Sound.UI_BUTTON_CLICK);
            gui.communistGovernmentGUI.open(player, nation);
            return;
        }

        id.nationcore.models.CommunistDecisionType type = switch (slot) {
            case 20 -> id.nationcore.models.CommunistDecisionType.HEA_QUARANTINE_PROTOCOL;
            case 22 -> id.nationcore.models.CommunistDecisionType.HEA_FIELD_MEDICINE;
            case 24 -> id.nationcore.models.CommunistDecisionType.HEA_VACCINATION_DRIVE;
            case 30 -> id.nationcore.models.CommunistDecisionType.HEA_EMERGENCY_RATIONS;
            case 32 -> id.nationcore.models.CommunistDecisionType.HEA_PLAGUE;
            default -> null;
        };

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
            gui.communistGovernmentGUI.open(player, nation);
            return;
        }

        id.nationcore.models.CommunistDecisionType type = switch (slot) {
            case 20 -> id.nationcore.models.CommunistDecisionType.DEF_DECLARE_WAR;
            case 22 -> id.nationcore.models.CommunistDecisionType.DEF_MILITARY_DRAFT;
            case 24 -> id.nationcore.models.CommunistDecisionType.DEF_DEFENSE_PROTOCOL;
            case 30 -> id.nationcore.models.CommunistDecisionType.DEF_OFFENSE_PROTOCOL;
            case 32 -> id.nationcore.models.CommunistDecisionType.DEF_MILITARY_EMERGENCY;
            default -> null;
        };

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
            gui.communistGovernmentGUI.open(player, nation);
            return;
        }

        id.nationcore.models.CommunistDecisionType type = switch (slot) {
            case 20 -> id.nationcore.models.CommunistDecisionType.TRE_DISTRIBUTION_PROGRAM;
            case 22 -> id.nationcore.models.CommunistDecisionType.TRE_ECONOMIC_STIMULUS;
            case 24 -> id.nationcore.models.CommunistDecisionType.TRE_EDUCATION_PROGRAM;
            case 30 -> id.nationcore.models.CommunistDecisionType.TRE_TAX_INTENSIFICATION;
            case 32 -> id.nationcore.models.CommunistDecisionType.TRE_MARKET_EVENT;
            default -> null;
        };

        if (type != null) {
            handleOfficeDecisionClick(player, nation, type);
        }
    }

    public void handlePropagandaOfficeGUI(Player player, ItemStack clicked, int slot) {
        if (clicked == null || clicked.getType() == org.bukkit.Material.AIR)
            return;

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null)
            return;

        if (slot == 49) {
            MessageUtils.playSound(player, org.bukkit.Sound.UI_BUTTON_CLICK);
            gui.communistGovernmentGUI.open(player, nation);
            return;
        }

        id.nationcore.models.CommunistDecisionType type = switch (slot) {
            case 20 -> id.nationcore.models.CommunistDecisionType.PROP_GLOBAL_BROADCAST;
            case 22 -> id.nationcore.models.CommunistDecisionType.PROP_NATIONAL_BROADCAST;
            case 24 -> id.nationcore.models.CommunistDecisionType.PROP_LEADER_GLORIFICATION;
            case 30 -> id.nationcore.models.CommunistDecisionType.PROP_MEDIA_CENSORSHIP;
            case 32 -> id.nationcore.models.CommunistDecisionType.PROP_MOBILIZATION;
            default -> null;
        };

        if (type != null) {
            handleOfficeDecisionClick(player, nation, type);
        }
    }

    private void handleOfficeDecisionClick(Player player, Nation nation, id.nationcore.models.CommunistDecisionType type) {
        CommunistGovernment gov = nation.getCommunistGovernment();
        if (gov == null) return;

        CommunistGovernment.PolitburoPosition requiredPosition = type.getPosition();
        CommunistGovernment.PolitburoMember member = gov.getPolitburoMember(requiredPosition);
        UUID ministerUUID = member != null ? member.getUuid() : null;

        boolean isMinister = ministerUUID != null && ministerUUID.equals(player.getUniqueId());
        boolean isSekjen = gov.hasSecretaryGeneral() && gov.getSecretaryGeneralUUID().equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("nation.admin");

        if (!isMinister && !isSekjen && !isAdmin) {
            MessageUtils.send(player, "<red>Only the " + requiredPosition.getDisplayName() + " (or SecGen) can execute this decision.</red>");
            MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        if (ministerUUID == null && isAdmin && !isSekjen) {
            MessageUtils.send(player, "<red>Cannot execute decision because the Minister position is vacant.</red>");
            MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        if (isCommunistDecisionStateActive(gov, type)) {
            MessageUtils.send(player, "<red>This decision is already active.</red>");
            MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        long cooldownRemaining = plugin.getCommunistManager().getDecisionCooldownRemaining(
                nation, ministerUUID != null ? ministerUUID : player.getUniqueId(), type);
        if (cooldownRemaining > 0) {
            MessageUtils.send(player, "<red>This decision is on cooldown.</red>");
            MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        if (!plugin.getTreasuryManager().canAfford(nation, type.getCost())) {
            MessageUtils.send(player, "<red>Insufficient treasury funds.</red>");
            MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        MessageUtils.playSound(player, org.bukkit.Sound.UI_BUTTON_CLICK);
        gui.confirmActionGUI.open(player, "Execute: " + type.getDisplayName(), () -> {
            Player targetPlayer = player;
            if (isAdmin && !isMinister && !isSekjen) {
                org.bukkit.entity.Player onlineMinister = ministerUUID != null ? Bukkit.getPlayer(ministerUUID) : player;
                targetPlayer = onlineMinister != null ? onlineMinister : player;
            }
            var result = plugin.getCommunistManager().executeDecision(nation, targetPlayer, type);
            if (result.isSuccess()) {
                MessageUtils.send(player, "<green>" + result.getMessage() + "</green>");
                MessageUtils.playSound(player, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP);
            } else {
                MessageUtils.send(player, "<red>" + result.getMessage() + "</red>");
                MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            }
        });
    }

    private boolean isAuthorizedForOffice(Player player, Nation nation, CommunistGovernment.PolitburoPosition position) {
        if (nation == null) return false;
        CommunistGovernment gov = nation.getCommunistGovernment();
        if (gov == null) return false;

        boolean isSekjen = gov.hasSecretaryGeneral() && gov.getSecretaryGeneralUUID().equals(player.getUniqueId());
        boolean isMinister = gov.getPolitburoMember(position) != null && gov.getPolitburoMember(position).getUuid().equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("nation.admin");

        return isSekjen || isMinister || isAdmin;
    }

    private boolean isCommunistDecisionStateActive(CommunistGovernment cg, id.nationcore.models.CommunistDecisionType type) {
        return switch (type) {
            case PROP_LEADER_GLORIFICATION -> cg.isGlorificationActive();
            case PROP_MEDIA_CENSORSHIP -> cg.isSensorMediaActive();
            case DEF_DEFENSE_PROTOCOL -> cg.isDefenseProtocolActive();
            case DEF_OFFENSE_PROTOCOL -> cg.isOffenseProtocolActive();
            case DEF_MILITARY_EMERGENCY -> cg.isMilitaryEmergencyActive();
            case TRE_MARKET_EVENT -> cg.isMarketEventActive();
            case TRE_DISTRIBUTION_PROGRAM -> cg.getDistributionProgramPhasesLeft() > 0;
            case TRE_TAX_INTENSIFICATION -> cg.getTaxIntensificationPhasesLeft() > 0;
            case HEA_QUARANTINE_PROTOCOL -> cg.isQuarantineActive();
            case HEA_VACCINATION_DRIVE -> cg.isVaccinationActive();
            case HEA_PLAGUE -> cg.isPlagueActive();
            default -> false;
        };
    }
}
