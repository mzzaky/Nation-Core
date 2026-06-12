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
            if (!presidentCabinetOrParty) {
                MessageUtils.send(player,
                        "<red>Only the Secretary General, Cabinet, and Party Members can access this menu.</red>");
                return;
            }
            gui.openOrdersGUI(player);
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

        if (slot == 43 || clicked.getType() == Material.SPECTRAL_ARROW) {
            gui.mainMenuRouter.openFor(player);
            return;
        }

        if (slot == 10) {
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

        if (slot == 48) {
            CommunistGovernment cg = nation.getCommunistGovernment();
            boolean isSekjen = cg != null && cg.hasSecretaryGeneral()
                    && cg.getSecretaryGeneralUUID().equals(player.getUniqueId());
            if (!isSekjen && !player.hasPermission("nation.admin")) {
                MessageUtils.send(player, "§cOnly the Secretary General can rename the nation.");
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
            CommunistGovernment cg = nation.getCommunistGovernment();
            boolean isSekjen = cg != null && cg.hasSecretaryGeneral()
                    && cg.getSecretaryGeneralUUID().equals(player.getUniqueId());
            boolean isAdmin = player.hasPermission("nation.admin");

            if (!isSekjen && !isAdmin) {
                MessageUtils.send(player, "<red>Only the Secretary General or an admin can disband the nation.</red>");
                return;
            }

            gui.confirmActionGUI.open(player, "Disband your nation", () -> {
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

        if (slot == 22) {
            int page = gui.memberListPage.getOrDefault(player.getUniqueId(), 0);
            gui.communistMemberManagementGUI.open(player, nation, page);
            return;
        }

        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }
    }

    public void handleOrdersGUI(Player player, ItemStack clicked, int slot) {
        if (CommunistExecutiveOrdersMenu.SLOT_CLOSE == slot || clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }
        if (slot == 43 || clicked.getType() == Material.SPECTRAL_ARROW) {
            gui.mainMenuRouter.openFor(player);
            return;
        }

        if (CommunistExecutiveOrdersMenu.handleTabClick(player, slot)) {
            gui.openOrdersGUI(player);
            return;
        }

        id.nationcore.models.CommunistDecisionType type = CommunistExecutiveOrdersMenu.getDecisionAtSlot(player, slot);
        if (type == null) {
            id.nationcore.models.ExecutiveOrder.ExecutiveOrderType eoType = CommunistExecutiveOrdersMenu
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
            MessageUtils.send(player, "<red>You are not a member of any nation.</red>");
            player.closeInventory();
            return;
        }

        player.closeInventory();
        var result = plugin.getCommunistManager().executeDecision(nation, player, type);
        if (result.isSuccess()) {
            MessageUtils.send(player, "<green>" + result.getMessage() + "</green>");
        } else {
            MessageUtils.send(player, "<red>" + result.getMessage() + "</red>");
        }
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

        if (slot == 47) {
            gui.memberListPage.remove(player.getUniqueId());
            gui.communistGovernmentGUI.open(player, nation);
            return;
        }

        if (slot == 45 && clicked.getType() == Material.ARROW) {
            page = Math.max(0, page - 1);
            gui.memberListPage.put(player.getUniqueId(), page);
            gui.communistMemberManagementGUI.open(player, nation, page);
            return;
        }

        if (slot == 53 && clicked.getType() == Material.ARROW) {
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

        if (slot == 22 || clicked.getType() == Material.SPECTRAL_ARROW) {
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

        if (slot == 10 && clicked.getType() == Material.RED_CONCRETE) {
            gui.confirmActionGUI.open(player, "Kick " + Bukkit.getOfflinePlayer(targetUUID).getName() + " from nation",
                    () -> {
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

        if (slot == 13 && clicked.getType() == Material.LIME_CONCRETE) {
            if (cg == null) {
                MessageUtils.send(player, "§cCommunist government not found.");
                return;
            }
            if (cg.isPartyMember(targetUUID)) {
                MessageUtils.send(player, "§eThis player is already a Party member.");
                gui.communistMemberManagementGUI.openActionMenu(player, nation, targetUUID);
                return;
            }
            if (cg.getPartyMemberCount() >= 5) {
                MessageUtils.send(player, "§cThe Party is full (max 5 members).");
                gui.communistMemberManagementGUI.openActionMenu(player, nation, targetUUID);
                return;
            }
            cg.addPartyMember(targetUUID);
            plugin.getDataManager().saveNations();

            String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();
            MessageUtils.send(player, "<green>" + targetName + " has been added to the Party.</green>");

            org.bukkit.entity.Player targetOnline = Bukkit.getPlayer(targetUUID);
            if (targetOnline != null) {
                MessageUtils.send(targetOnline,
                        "<gold>🚩 You have been admitted to the Party of " + nation.getName() + "!</gold>");
            }

            gui.communistMemberManagementGUI.openActionMenu(player, nation, targetUUID);
            return;
        }

        if (slot == 16 && clicked.getType() == Material.GOLD_BLOCK) {
            gui.communistMemberManagementGUI.openPolitburoPositionPicker(player, nation, targetUUID);
            return;
        }
    }

    @SuppressWarnings("deprecation")
    public void handlePolitburoPickerGUI(Player player, ItemStack clicked, int slot, String title) {
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

        UUID targetUUID = gui.viewingManagedMember.get(player.getUniqueId());

        if (slot == 22 || clicked.getType() == Material.SPECTRAL_ARROW) {
            if (targetUUID != null) {
                gui.communistMemberManagementGUI.openActionMenu(player, nation, targetUUID);
            } else {
                int page = gui.memberListPage.getOrDefault(player.getUniqueId(), 0);
                gui.communistMemberManagementGUI.open(player, nation, page);
            }
            return;
        }

        if (targetUUID == null) {
            player.closeInventory();
            return;
        }

        CommunistGovernment cg = nation.getCommunistGovernment();
        boolean isSekjen = cg != null && cg.hasSecretaryGeneral()
                && cg.getSecretaryGeneralUUID().equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("nation.admin");
        if (!isSekjen && !isAdmin) {
            MessageUtils.send(player, "§cOnly the Secretary General can appoint Politburo.");
            player.closeInventory();
            return;
        }

        CommunistGovernment.PolitburoPosition position = CommunistMemberManagementGUI.getPositionForPickerSlot(slot);
        if (position == null)
            return;

        String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();
        id.nationcore.managers.CommunistManager.Result result = plugin.getCommunistManager().appointPolitburo(nation,
                position, targetUUID, targetName);

        if (result.isSuccess()) {
            MessageUtils.send(player, "<green>" + result.getMessage() + "</green>");
        } else {
            MessageUtils.send(player, "<red>" + result.getMessage() + "</red>");
        }

        gui.communistMemberManagementGUI.openActionMenu(player, nation, targetUUID);
    }

    public void handleSharedStorage(InventoryClickEvent event, Player player, ItemStack clicked, int rawSlot) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) {
            player.closeInventory();
            return;
        }
        gui.communistSharedStorageGUI.handleClick(event, player, nation, rawSlot, clicked, event.getCursor());
    }
}
