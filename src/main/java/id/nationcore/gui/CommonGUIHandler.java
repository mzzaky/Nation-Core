package id.nationcore.gui;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import id.nationcore.NationCore;
import id.nationcore.models.CabinetDecision;
import id.nationcore.models.Government;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.Nation;
import id.nationcore.models.PlayerData;
import id.nationcore.models.RecallPetition;
import id.nationcore.utils.MessageUtils;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Handles GUI clicks shared across all nation types:
 * cabinet, salary, treasury (republic/communist), president history, player stats,
 * leaderboard, help, recall, tax, arena, hub, create nation, research.
 */
public class CommonGUIHandler {

    private final NationCore plugin;
    private final GUIListener gui;

    public CommonGUIHandler(NationCore plugin, GUIListener gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    public void handleSalaryGUI(Player player, ItemStack clicked) {
        if (clicked.getType() == Material.ARROW) {
            gui.openGovernmentGUI(player);
            return;
        }

        if (clicked.getType() == Material.EMERALD_BLOCK) {
            player.closeInventory();
            plugin.getGovernmentManager().claimDailySalary(player);
            return;
        }
    }

    public void handleCabinetGUI(Player player, ItemStack clicked, int slot, ClickType clickType) {
        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.ARROW) {
            gui.openGovernmentGUI(player);
            return;
        }

        Government gov = plugin.getDataManager().getGovernment();
        boolean isPresident = gov.hasPresident() && gov.getPresidentUUID().equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("nation.admin");
        boolean canAppoint = isPresident || isAdmin;

        Government.CabinetPosition posAtSlot = gui.cabinetGUI.getPositionForSlot(slot);
        if (posAtSlot != null) {
            boolean isVacant = gov.getCabinetMember(posAtSlot) == null;
            if (canAppoint && (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT || isVacant)) {
                gui.viewingAppointPosition.put(player.getUniqueId(), posAtSlot);
                gui.cabinetGUI.openAppointMenu(player, posAtSlot);
                return;
            }
            if (!isVacant) {
                CabinetDecision.CabinetPosition decPos = CabinetDecision.CabinetPosition.valueOf(posAtSlot.name());
                gui.viewingCabinetPosition.put(player.getUniqueId(), decPos);
                gui.cabinetGUI.openCabinetDecisionsMenu(player, decPos);
            }
            return;
        }

        if (slot == 29 && (clicked.getType() == Material.BEACON)) {
            java.util.List<CabinetDecision> activeDecisions = plugin.getDataManager().getActiveDecisions();
            if (!activeDecisions.isEmpty()) {
                CabinetDecision first = activeDecisions.get(0);
                gui.viewingCabinetPosition.put(player.getUniqueId(), first.getMinisterPosition());
                gui.cabinetGUI.openCabinetDecisionsMenu(player, first.getMinisterPosition());
            }
            return;
        }

        if (clicked.getType() == Material.DIAMOND) {
            CabinetDecision.CabinetPosition[] positions = CabinetDecision.CabinetPosition.values();
            for (CabinetDecision.CabinetPosition pos : positions) {
                UUID minister = gov.getCabinetMember(Government.CabinetPosition.valueOf(pos.name()));
                if (minister != null && minister.equals(player.getUniqueId())) {
                    gui.viewingCabinetPosition.put(player.getUniqueId(), pos);
                    gui.cabinetGUI.openCabinetDecisionsMenu(player, pos);
                    return;
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void handleCabinetDecisionsGUI(Player player, ItemStack clicked) {
        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.ARROW) {
            gui.cabinetGUI.openCabinetMenu(player);
            return;
        }

        if (clicked.getType() == Material.YELLOW_WOOL) {
            String displayName = clicked.getItemMeta().getDisplayName();

            CabinetDecision.CabinetPosition position = gui.viewingCabinetPosition.get(player.getUniqueId());
            if (position == null)
                return;

            for (CabinetDecision.DecisionType type : CabinetDecision.DecisionType.values()) {
                if (type.getPosition() == position && displayName.contains(type.name())) {
                    player.closeInventory();
                    plugin.getCabinetManager().executeDecision(player.getUniqueId(), type);
                }
            }
        }
    }

    public void handleCabinetAppointGUI(Player player, ItemStack clicked, String title) {
        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.ARROW) {
            gui.cabinetGUI.openCabinetMenu(player);
            return;
        }

        Government.CabinetPosition targetPos = gui.viewingAppointPosition.get(player.getUniqueId());
        if (targetPos == null) {
            gui.cabinetGUI.openCabinetMenu(player);
            return;
        }

        Government gov = plugin.getDataManager().getGovernment();
        boolean isPresident = gov.hasPresident() && gov.getPresidentUUID().equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("nation.admin");

        if (!isPresident && !isAdmin) {
            MessageUtils.send(player, "<red>Only the President can appoint cabinet members!");
            gui.cabinetGUI.openCabinetMenu(player);
            return;
        }

        if (clicked.getType() == Material.RED_CONCRETE) {
            plugin.getGovernmentManager().removeCabinetMember(targetPos);
            MessageUtils.send(player,
                    "<gold>The <yellow>" + targetPos.getDisplayName() + "<gold> position is now vacant.");
            Bukkit.getScheduler().runTaskLater(plugin, () -> gui.cabinetGUI.openCabinetMenu(player), 3L);
            return;
        }

        if (clicked.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) clicked.getItemMeta();
            if (meta == null || meta.getOwningPlayer() == null)
                return;

            UUID targetUUID = meta.getOwningPlayer().getUniqueId();
            UUID currentMinister = gov.getCabinetMember(targetPos);

            if (targetUUID.equals(currentMinister)) {
                plugin.getGovernmentManager().removeCabinetMember(targetPos);
                MessageUtils.send(player, "<gold>Removed <yellow>" + meta.getOwningPlayer().getName()
                        + "<gold> from <yellow>" + targetPos.getDisplayName() + "<gold>.");
            } else {
                plugin.getGovernmentManager().appointCabinetMember(targetPos, targetUUID);
                MessageUtils.send(player, "<gold>Appointed <yellow>" + meta.getOwningPlayer().getName()
                        + "<gold> as <yellow>" + targetPos.getDisplayName() + "<gold>!");
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> gui.cabinetGUI.openCabinetMenu(player), 3L);
        }
    }

    /**
     * Treasury menu shared by Republic and Communist nations.
     *
     * @param isCommunist true if this is the Communist treasury menu.
     */
    public void handleTreasuryGUI(Player player, ItemStack clicked, boolean isCommunist) {
        if (clicked.getType() == Material.ARROW) {
            gui.mainMenuRouter.openFor(player);
            return;
        }

        if (clicked.getType() == Material.HOPPER) {
            player.closeInventory();
            MessageUtils.send(player, "<yellow>Use: <white>/nc treasury donate <amount>");
            return;
        }

        if (clicked.getType() == Material.BOOK) {
            if (isCommunist) {
                gui.communistTreasuryMenu.openTransactions(player,
                        plugin.getNationManager().getNationOf(player.getUniqueId()));
            } else {
                gui.republicTreasuryMenu.openTransactions(player);
            }
        }
    }

    public void handleTreasuryTransactionsGUI(Player player, ItemStack clicked, boolean isCommunist) {
        if (clicked.getType() == Material.ARROW) {
            if (isCommunist) {
                gui.communistTreasuryMenu.open(player, plugin.getNationManager().getNationOf(player.getUniqueId()));
            } else {
                gui.republicTreasuryMenu.open(player);
            }
        }
    }

    public void handleHistoryGUI(Player player, ItemStack clicked, int slot, boolean isDetailView) {
        if (clicked.getType() == Material.ARROW) {
            if (isDetailView) {
                gui.presidentHistoryGUI.openHistoryMenu(player);
            } else {
                Government gov = plugin.getDataManager().getGovernment();
                boolean isPresident = gov.hasPresident() && gov.getPresidentUUID().equals(player.getUniqueId());
                boolean isMinister = gov.getCabinetMemberByUUID(player.getUniqueId()) != null;
                boolean isAdmin = player.hasPermission("nation.admin");

                if (isPresident || isMinister || isAdmin) {
                    gui.openGovernmentGUI(player);
                } else {
                    gui.mainMenuRouter.openFor(player);
                }
            }
            return;
        }

        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (!isDetailView) {
            if (clicked.getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = (SkullMeta) clicked.getItemMeta();
                if (meta != null && meta.getOwningPlayer() != null) {
                    UUID targetUUID = meta.getOwningPlayer().getUniqueId();
                    java.util.List<id.nationcore.models.PresidentHistory.PresidentRecord> history = plugin
                            .getDataManager().getAllPresidentHistory();
                    for (int i = 0; i < history.size(); i++) {
                        if (history.get(i).getPlayerId().equals(targetUUID)) {
                            gui.presidentHistoryGUI.openDetailMenu(player, i);
                            return;
                        }
                    }
                }
            }
        } else {
            if (clicked.getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = (SkullMeta) clicked.getItemMeta();
                if (meta != null && meta.getOwningPlayer() != null) {
                    UUID targetUUID = meta.getOwningPlayer().getUniqueId();
                    java.util.List<id.nationcore.models.PresidentHistory.PresidentRecord> history = plugin
                            .getDataManager().getAllPresidentHistory();
                    for (int i = 0; i < history.size(); i++) {
                        if (history.get(i).getPlayerId().equals(targetUUID)) {
                            gui.presidentHistoryGUI.openDetailMenu(player, i);
                            return;
                        }
                    }
                }
            }
        }
    }

    public void handlePlayerStatsGUI(Player player, ItemStack clicked, int slot) {
        if (clicked.getType() == Material.ARROW) {
            gui.mainMenuRouter.openFor(player);
            return;
        }

        if (clicked.getType() == Material.GOLD_INGOT) {
            gui.playerStatsGUI.openLeaderboard(player);
            return;
        }

        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }
    }

    public void handleLeaderboardGUI(Player player, ItemStack clicked, int slot) {
        if (clicked.getType() == Material.ARROW) {
            gui.mainMenuRouter.openFor(player);
            return;
        }

        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) clicked.getItemMeta();
            if (meta.getOwningPlayer() != null) {
                UUID targetUUID = meta.getOwningPlayer().getUniqueId();
                gui.viewingPlayerStats.put(player.getUniqueId(), targetUUID);
                gui.playerStatsGUI.openPlayerStats(player, targetUUID);
            }
        }
    }

    public void handleHelpMenuGUI(Player player, ItemStack clicked, int slot) {
        if (clicked.getType() == Material.ARROW) {
            gui.mainMenuRouter.openFor(player);
            return;
        }

        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (slot == 19 && clicked.getType() == Material.PAPER) {
            gui.helpGUI.openElectionHelp(player);
            return;
        }

        if (slot == 21 && clicked.getType() == Material.GOLDEN_HELMET) {
            gui.helpGUI.openPresidentHelp(player);
            return;
        }

        if (slot == 23 && clicked.getType() == Material.LECTERN) {
            gui.helpGUI.openCabinetHelp(player);
            return;
        }

        if (slot == 25 && clicked.getType() == Material.WRITABLE_BOOK) {
            gui.helpGUI.openOrdersHelp(player);
            return;
        }

        if (slot == 29 && clicked.getType() == Material.IRON_SWORD) {
            gui.helpGUI.openArenaHelp(player);
            return;
        }

        if (slot == 31 && clicked.getType() == Material.GOLD_BLOCK) {
            gui.helpGUI.openTreasuryHelp(player);
            return;
        }

        if (slot == 33 && clicked.getType() == Material.REDSTONE_TORCH) {
            gui.recallGUI.openRecallMenu(player);
            return;
        }

        if (slot == 40 && clicked.getType() == Material.COMMAND_BLOCK) {
            player.closeInventory();
            showCommandList(player);
        }
    }

    public void handleHelpSubMenuGUI(Player player, ItemStack clicked) {
        if (clicked.getType() == Material.ARROW) {
            gui.helpGUI.openHelpMenu(player);
        }
    }

    public void handleRecallMenuGUI(Player player, ItemStack clicked, int slot) {
        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.ARROW) {
            gui.mainMenuRouter.openFor(player);
            return;
        }

        if (clicked.getType() == Material.CLOCK) {
            gui.recallGUI.openRecallMenu(player);
            return;
        }

        RecallPetition petition = plugin.getDataManager().getRecallPetition();
        boolean hasActivePetition = petition != null
                && petition.getPhase() != RecallPetition.RecallPhase.COMPLETED
                && petition.getPhase() != RecallPetition.RecallPhase.FAILED;

        if (slot == 22 && !hasActivePetition) {
            if (clicked.getType() == Material.REDSTONE_BLOCK) {
                gui.recallGUI.openConfirmPetition(player);
                return;
            }
        }

        if (!hasActivePetition)
            return;

        if (petition != null && petition.getPhase() == RecallPetition.RecallPhase.COLLECTING) {
            if (slot == 38 && clicked.getType() == Material.LIME_CONCRETE) {
                player.closeInventory();
                plugin.getRecallManager().signPetition(player.getUniqueId());
                Bukkit.getScheduler().runTaskLater(plugin, () -> gui.recallGUI.openRecallMenu(player), 5L);
                return;
            }

            if (slot == 38 && clicked.getType() == Material.ORANGE_CONCRETE) {
                player.closeInventory();
                plugin.getRecallManager().withdrawSignature(player.getUniqueId());
                Bukkit.getScheduler().runTaskLater(plugin, () -> gui.recallGUI.openRecallMenu(player), 5L);
                return;
            }

            if (slot == 40 && clicked.getType() == Material.PAPER) {
                player.closeInventory();
                showRecallInfo(player);
                return;
            }
        }

        if (petition != null && petition.getPhase() == RecallPetition.RecallPhase.VOTING) {
            if (slot == 38 && clicked.getType() == Material.RED_CONCRETE) {
                player.closeInventory();
                plugin.getRecallManager().castRecallVote(player.getUniqueId(), true);
                Bukkit.getScheduler().runTaskLater(plugin, () -> gui.recallGUI.openRecallMenu(player), 5L);
                return;
            }

            if (slot == 42 && clicked.getType() == Material.LIME_CONCRETE) {
                player.closeInventory();
                plugin.getRecallManager().castRecallVote(player.getUniqueId(), false);
                Bukkit.getScheduler().runTaskLater(plugin, () -> gui.recallGUI.openRecallMenu(player), 5L);
                return;
            }
        }
    }

    public void handleRecallConfirmGUI(Player player, ItemStack clicked) {
        if (clicked.getType() == Material.LIME_CONCRETE) {
            player.closeInventory();
            boolean success = plugin.getRecallManager().startPetition(
                    player.getUniqueId(), "Player-initiated recall");
            if (success) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> gui.recallGUI.openRecallMenu(player), 10L);
            } else {
                MessageUtils.send(player, "<red>Cannot start recall petition! Check requirements.");
                Bukkit.getScheduler().runTaskLater(plugin, () -> gui.recallGUI.openRecallMenu(player), 10L);
            }
            return;
        }

        if (clicked.getType() == Material.RED_CONCRETE) {
            gui.recallGUI.openRecallMenu(player);
            return;
        }
    }

    public void handleRecallVoteGUI(Player player, ItemStack clicked) {
        if (clicked.getType() == Material.ARROW) {
            gui.recallGUI.openRecallMenu(player);
        }
    }

    public void handleTaxMenuGUI(Player player, ItemStack clicked, int slot) {
        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.ARROW) {
            gui.mainMenuRouter.openFor(player);
            return;
        }

        if (clicked.getType() == Material.CLOCK) {
            gui.taxGUI.openTaxMenu(player);
            return;
        }

        if (slot == 30 && clicked.getType() == Material.EMERALD_BLOCK) {
            player.closeInventory();
            plugin.getTaxManager().payDebt(player);
            Bukkit.getScheduler().runTaskLater(plugin, () -> gui.taxGUI.openTaxMenu(player), 10L);
            return;
        }

        if (slot == 24 && clicked.getType() == Material.BOOK) {
            gui.taxGUI.openTaxHistory(player);
            return;
        }

        if (slot == 32 && clicked.getType() == Material.SKELETON_SKULL) {
            gui.taxGUI.openDebtorList(player);
            return;
        }
    }

    public void handleTaxHistoryGUI(Player player, ItemStack clicked) {
        if (clicked.getType() == Material.ARROW) {
            gui.taxGUI.openTaxMenu(player);
        }
    }

    public void handleTaxDebtorsGUI(Player player, ItemStack clicked) {
        if (clicked.getType() == Material.ARROW) {
            gui.taxGUI.openTaxMenu(player);
        }
    }

    public void handleArenaGUI(Player player, ItemStack clicked, int slot) {
        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.ARROW) {
            gui.mainMenuRouter.openFor(player);
            return;
        }

        if (slot == 49 && clicked.getType() == Material.CLOCK) {
            gui.arenaGUI.openArenaMenu(player);
            return;
        }

        if (slot == 40 && clicked.getType() == Material.GOLD_BLOCK) {
            gui.arenaGUI.openLeaderboard(player);
            return;
        }

        if (slot == 16 && clicked.getType() == Material.NETHER_STAR) {
            gui.arenaGUI.openLeaderboard(player);
            return;
        }

        if (slot == 25 && clicked.getType() == Material.IRON_CHESTPLATE) {
            gui.arenaGUI.openKitInfo(player);
            return;
        }

        if (slot == 37) {
            if (clicked.getType() == Material.LIME_CONCRETE) {
                player.closeInventory();
                plugin.getArenaManager().joinArena(player);
            } else if (clicked.getType() == Material.RED_CONCRETE) {
                player.closeInventory();
                if (!plugin.getArenaManager().leaveArena(player.getUniqueId())) {
                    MessageUtils.send(player, "<red>You are not in the arena!");
                }
            }
            return;
        }

        if (slot == 43) {
            if (clicked.getType() == Material.BEACON) {
                if (!plugin.getArenaManager().startArena(player.getUniqueId())) {
                    MessageUtils.send(player, "<red>Cannot start arena! Check requirements.");
                } else {
                    gui.arenaGUI.openArenaMenu(player);
                }
            } else if (clicked.getType() == Material.TNT) {
                plugin.getArenaManager().endArena();
                MessageUtils.send(player, "<gold>Arena ended. Final rewards distributed.");
                gui.arenaGUI.openArenaMenu(player);
            }
        }
    }

    public void handleArenaLeaderboardGUI(Player player, ItemStack clicked) {
        if (clicked.getType() == Material.ARROW) {
            gui.arenaGUI.openArenaMenu(player);
            return;
        }
        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
        }
    }

    public void handleArenaKitGUI(Player player, ItemStack clicked) {
        if (clicked.getType() == Material.ARROW) {
            gui.arenaGUI.openArenaMenu(player);
            return;
        }
        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
        }
    }

    public void handleHubGUI(Player player, ItemStack clicked, int slot, ClickType click) {
        Material type = clicked.getType();

        if (slot == HubGUI.ACTION_BUTTON_SLOT) {
            if (type == Material.NETHER_STAR) {
                gui.openCreateNationGUI(player);
                return;
            }
            if (type == Material.WHITE_BANNER || type == Material.RED_BANNER
                    || type == Material.WRITABLE_BOOK) {
                gui.mainMenuRouter.openFor(player);
                return;
            }
        }

        if (slot == HubGUI.REFRESH_BUTTON_SLOT && type == Material.SUNFLOWER) {
            gui.openHubGUI(player);
            return;
        }

        if (slot >= 0 && slot <= 44 && clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
            String displayName = PlainTextComponentSerializer.plainText()
                    .serialize(clicked.getItemMeta().displayName());
            if (displayName == null || displayName.isBlank())
                return;
            Nation nation = plugin.getNationManager().getNationByName(displayName);
            if (nation == null)
                return;

            if (click.isRightClick()) {
                player.closeInventory();
                showNationDetail(player, nation);
                return;
            }

            PlayerData data = plugin.getDataManager().getOrCreatePlayerData(
                    player.getUniqueId(), player.getName());
            if (data.hasNation()) {
                MessageUtils.send(player, "<red>You are already a member of a nation. " +
                        "Leave first to join another nation.</red>");
                return;
            }

            var result = plugin.getNationManager().joinNation(player, nation);
            if (result.isSuccess()) {
                MessageUtils.send(player, "<green>" + result.getMessage() + "</green>");
                gui.openHubGUI(player);
            } else {
                MessageUtils.send(player, "<red>" + result.getMessage() + "</red>");
            }
        }
    }

    public void handleCreateNationGUI(Player player, ItemStack clicked, int slot) {
        if (slot == CreateNationGUI.SLOT_BACK) {
            gui.openHubGUI(player);
            return;
        }

        GovernmentType selected = null;
        if (slot == CreateNationGUI.SLOT_REPUBLIC && clicked.getType() == GovernmentType.REPUBLIC.getIconMaterial()) {
            selected = GovernmentType.REPUBLIC;
        } else if (slot == CreateNationGUI.SLOT_COMMUNIST
                && clicked.getType() == GovernmentType.COMMUNIST.getIconMaterial()) {
            selected = GovernmentType.COMMUNIST;
        } else if (slot == CreateNationGUI.SLOT_MONARCHY
                && clicked.getType() == GovernmentType.MONARCHY.getIconMaterial()) {
            selected = GovernmentType.MONARCHY;
        } else if (slot == CreateNationGUI.SLOT_CALIPHATE
                && clicked.getType() == GovernmentType.CALIPHATE.getIconMaterial()) {
            selected = GovernmentType.CALIPHATE;
        }
        if (selected == null)
            return;

        if (plugin.getNationManager().hasNation(player.getUniqueId())) {
            MessageUtils.send(player, "<red>You are already a member of a nation. Leave first " +
                    "to create a new nation.</red>");
            player.closeInventory();
            return;
        }
        double cost = plugin.getConfig().getDouble("nation.creation.cost", 1_000_000);
        if (plugin.getVaultHook().getBalance(player.getUniqueId()) < cost) {
            MessageUtils.send(player, "<red>You need at least $" +
                    String.format("%,.0f", cost) + " to establish a nation.</red>");
            player.closeInventory();
            return;
        }

        plugin.getNationManager().setPendingCreation(player.getUniqueId(), selected);
        player.closeInventory();

        MessageUtils.send(player, "");
        MessageUtils.send(player, "<gold></gold>");
        MessageUtils.send(player, "<yellow>You selected government type: " +
                selected.getColoredName() + "</yellow>");
        MessageUtils.send(player, "<gold>Type your nation name in chat now.</gold>");
        MessageUtils.send(player,
                "<gray>(3-24 characters, alphanumeric and spaces only. Type 'cancel' to abort.)</gray>");
        MessageUtils.send(player, "<gold></gold>");
    }

    public void handleResearchGUI(Player player, ItemStack clicked, int slot, String title) {
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == ResearchGUI.FILLER)
            return;

        MessageUtils.playSound(player, org.bukkit.Sound.UI_BUTTON_CLICK);

        if (title.equals(ResearchGUI.MAIN_TITLE)) {
            handleResearchMain(player, slot);
            return;
        }
        if (title.startsWith(ResearchGUI.ERA_TITLE_PREFIX)) {
            handleResearchEra(player, clicked, slot, title);
            return;
        }
        if (title.startsWith(ResearchGUI.DETAIL_TITLE_PREFIX)) {
            handleResearchDetail(player, clicked, slot, title);
        }
    }

    private void handleResearchMain(Player player, int slot) {
        if (slot == ResearchGUI.SLOT_BACK) {
            gui.mainMenuRouter.openFor(player);
            return;
        }

        if (slot == ResearchGUI.SLOT_ACTIVE) {
            Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
            if (nation != null && nation.getResearchData().hasActive()) {
                String typeId = nation.getResearchData().getActive().getTypeId();
                for (id.nationcore.models.ResearchType type : id.nationcore.models.ResearchType.values()) {
                    if (type.getId().equals(typeId)) {
                        gui.researchGUI.openDetail(player, type);
                        return;
                    }
                }
            }
            return;
        }

        if (slot == ResearchGUI.SLOT_ERA_I) {
            gui.researchGUI.openEra(player, ResearchGUI.ResearchEra.BASIC);
            return;
        }
        if (slot == ResearchGUI.SLOT_ERA_II
                || slot == ResearchGUI.SLOT_ERA_III
                || slot == ResearchGUI.SLOT_ERA_IV
                || slot == ResearchGUI.SLOT_ERA_V) {
            MessageUtils.send(player, "<gray>This era is not yet available. Coming soon.</gray>");
            MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        if (slot == ResearchGUI.SLOT_CANCEL_ACTIVE) {
            Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
            if (nation == null)
                return;
            id.nationcore.managers.ResearchManager.ActionResult res = plugin.getResearchManager().cancelResearch(player,
                    nation);
            MessageUtils.send(player, (res.ok ? "<green>" : "<red>") + res.message);
            gui.researchGUI.openMain(player);
        }
    }

    private void handleResearchEra(Player player, ItemStack clicked, int slot, String title) {
        ResearchGUI.EraTitleInfo info = ResearchGUI.parseEraTitle(title);
        if (info == null) {
            gui.researchGUI.openMain(player);
            return;
        }

        if (slot == ResearchGUI.ERA_SLOT_BACK) {
            gui.researchGUI.openMain(player);
            return;
        }

        if (slot == ResearchGUI.ERA_SLOT_ACTIVE) {
            Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
            if (nation != null && nation.getResearchData().hasActive()) {
                String typeId = nation.getResearchData().getActive().getTypeId();
                for (id.nationcore.models.ResearchType type : id.nationcore.models.ResearchType.values()) {
                    if (type.getId().equals(typeId)) {
                        gui.researchGUI.openDetail(player, type);
                        return;
                    }
                }
            }
            return;
        }

        if (slot == ResearchGUI.ERA_SLOT_NAV_WAR) {
            gui.researchGUI.openEra(player, info.era, id.nationcore.models.ResearchCategory.WAR);
            return;
        }
        if (slot == ResearchGUI.ERA_SLOT_NAV_TECHNOLOGY) {
            gui.researchGUI.openEra(player, info.era, id.nationcore.models.ResearchCategory.TECHNOLOGY);
            return;
        }
        if (slot == ResearchGUI.ERA_SLOT_NAV_ECONOMY) {
            gui.researchGUI.openEra(player, info.era, id.nationcore.models.ResearchCategory.ECONOMY);
            return;
        }

        if (clicked.getItemMeta() == null)
            return;
        String stripped = clicked.getItemMeta().hasDisplayName()
                ? PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName())
                : "";
        if (stripped.isEmpty())
            return;

        for (id.nationcore.models.ResearchType type : id.nationcore.models.ResearchType.values()) {
            if (type.getCategory() != info.category)
                continue;
            if (ResearchGUI.getEraOf(type) != info.era)
                continue;

            String typeName = type.getDisplayName();
            if (stripped.equalsIgnoreCase(typeName)) {
                gui.researchGUI.openDetail(player, type);
                return;
            }
        }
    }

    private void handleResearchDetail(Player player, ItemStack clicked, int slot, String title) {
        if (slot == ResearchGUI.DETAIL_SLOT_BACK || clicked.getType() == Material.ARROW) {
            id.nationcore.models.ResearchType type = ResearchGUI.parseTypeFromTitle(title);
            if (type != null) {
                gui.researchGUI.openEra(player, ResearchGUI.getEraOf(type), type.getCategory());
            } else {
                gui.researchGUI.openMain(player);
            }
            return;
        }
        if (slot == ResearchGUI.DETAIL_SLOT_START
                && clicked.getType() == Material.LIME_CONCRETE) {
            id.nationcore.models.ResearchType type = ResearchGUI.parseTypeFromTitle(title);
            if (type == null)
                return;
            Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
            if (nation == null) {
                MessageUtils.send(player, "<red>You must belong to a nation.</red>");
                return;
            }
            id.nationcore.managers.ResearchManager.ActionResult res = plugin.getResearchManager().startResearch(player,
                    nation, type);
            MessageUtils.send(player, (res.ok ? "<green>" : "<red>") + res.message);
            gui.researchGUI.openDetail(player, type);
        }
    }

    public void showArenaInfo(Player player) {
        gui.arenaGUI.openArenaMenu(player);
    }

    void showRecallInfo(Player player) {
        player.closeInventory();

        var petition = plugin.getDataManager().getRecallPetition();
        boolean isActive = petition != null &&
                petition.getPhase() != RecallPetition.RecallPhase.COMPLETED &&
                petition.getPhase() != RecallPetition.RecallPhase.FAILED;

        MessageUtils.send(player,
                "<gold>•••••••••••••••••••••••••••••••••••••••");
        MessageUtils.send(player, "<yellow>     š  RECALL SYSTEM š ");
        MessageUtils.send(player,
                "<gold>•••••••••••••••••••••••••••••••••••••••");

        if (isActive && petition != null) {
            MessageUtils.send(player, "<red>Recall petition is ACTIVE!");
            MessageUtils.send(player, "<gray>Phase: <white>" + petition.getPhase().name());
            MessageUtils.send(player, "<gray>Signatures: <white>" + petition.getSignatureCount());
        } else {
            MessageUtils.send(player, "<gray>No active recall petition.");
        }

        MessageUtils.send(player, "");
        MessageUtils.send(player, "<yellow>Commands:");
        MessageUtils.send(player, "<white>/dc recall start <gray>- Start petition (50k deposit)");
        MessageUtils.send(player, "<white>/dc recall sign <gray>- Sign petition");
        MessageUtils.send(player, "<white>/dc recall vote <yes/no> <gray>- Vote recall");
        MessageUtils.send(player, "<white>/dc recall status <gray>- View status");
        MessageUtils.send(player,
                "<gold>•••••••••••••••••••••••••••••••••••••••");
    }

    void showCommandList(Player player) {
        MessageUtils.send(player,
                "<gold>•••••••••••••••••••••••••••••••••••••••");
        MessageUtils.send(player, "<yellow>     ðŸ’» COMMAND LIST ðŸ’»");
        MessageUtils.send(player,
                "<gold>•••••••••••••••••••••••••••••••••••••••");
        MessageUtils.send(player, "<aqua>Menu & Info:");
        MessageUtils.send(player, "<white>/dc menu <gray>- Open main menu");
        MessageUtils.send(player, "<white>/dc info <gray>- Government info");
        MessageUtils.send(player, "<white>/dc help <gray>- Command help");
        MessageUtils.send(player, "");
        MessageUtils.send(player, "<aqua>Election:");
        MessageUtils.send(player, "<white>/dc election <gray>- Election status");
        MessageUtils.send(player, "<white>/dc candidates <gray>- Candidate list");
        MessageUtils.send(player, "<white>/dc vote <player> <gray>- Vote for a candidate");
        MessageUtils.send(player, "<white>/dc register <gray>- Candidate list");
        MessageUtils.send(player, "<white>/dc endorse <player> <gray>- Endorse a candidate");
        MessageUtils.send(player, "");
        MessageUtils.send(player, "<aqua>Others:");
        MessageUtils.send(player, "<white>/dc treasury <gray>- Treasury info");
        MessageUtils.send(player, "<white>/dc rate <1-5> <gray>- Rate president");
        MessageUtils.send(player, "<white>/dc stats [player] <gray>- Statistics");
        MessageUtils.send(player, "<white>/dc history <gray>- History of presidents");
        MessageUtils.send(player,
                "<gold>•••••••••••••••••••••••••••••••••••••••");
    }

    private void showNationDetail(Player player, Nation nation) {
        MessageUtils.send(player,
                "<gold></gold>");
        MessageUtils.send(player, "<yellow>Nation Detail: <white>" + nation.getName() + "</white></yellow>");
        MessageUtils.send(player,
                "<gold></gold>");
        MessageUtils.send(player, "<gray>Type: <white>" +
                (nation.getType() != null ? nation.getType().getDisplayName() : "Unknown"));
        MessageUtils.send(player, "<gray>Tag: <white>[" + nation.getTag() + "]");
        MessageUtils.send(player, "<gray>Leader: <white>" +
                (nation.getLeaderName() != null ? nation.getLeaderName() : "-"));
        MessageUtils.send(player, "<gray>Members: <white>" + nation.getMemberCount());
        MessageUtils.send(player, "<gray>Treasury: <white>$" +
                String.format("%,.0f", nation.getTreasury().getBalance()));
        if (nation.hasCapital()) {
            MessageUtils.send(player, "<gray>Capital: <white>" + nation.getCapital().getWorld() +
                    " (" + (int) nation.getCapital().getX() + ", " +
                    (int) nation.getCapital().getY() + ", " +
                    (int) nation.getCapital().getZ() + ")");
        } else {
            MessageUtils.send(player, "<gray>Capital: <yellow>not set");
        }
        MessageUtils.send(player,
                "<gold>•••••••••••••••••••••••••••••••••••••••</gold>");
    }
}
