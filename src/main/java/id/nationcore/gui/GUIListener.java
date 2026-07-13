package id.nationcore.gui;

import id.nationcore.gui.caliphate.CaliphateExecutiveOrdersMenu;
import id.nationcore.gui.caliphate.CaliphateGovernmentGUI;
import id.nationcore.gui.caliphate.CaliphateGUIHandler;
import id.nationcore.gui.caliphate.CaliphateMainMenu;
import id.nationcore.gui.caliphate.CaliphateTreasuryMenu;
import id.nationcore.gui.communist.CommunistLeaderOrdersMenu;
import id.nationcore.gui.communist.CommunistGovernmentGUI;
import id.nationcore.gui.communist.CommunistGUIHandler;
import id.nationcore.gui.communist.CommunistMainMenu;
import id.nationcore.gui.communist.CommunistMemberManagementGUI;
import id.nationcore.gui.communist.CommunistSharedStorageGUI;
import id.nationcore.gui.communist.CommunistTreasuryMenu;
import id.nationcore.gui.monarchy.MonarchyExecutiveOrdersMenu;
import id.nationcore.gui.monarchy.MonarchyGovernmentGUI;
import id.nationcore.gui.monarchy.MonarchyGUIHandler;
import id.nationcore.gui.monarchy.MonarchyMainMenu;
import id.nationcore.gui.monarchy.MonarchyTreasuryMenu;
import id.nationcore.gui.republic.RepublicElectionRegistrationGUI;
import id.nationcore.gui.republic.RepublicExecutiveOrdersMenu;
import id.nationcore.gui.republic.RepublicGovernmentGUI;
import id.nationcore.gui.republic.RepublicGUIHandler;
import id.nationcore.gui.republic.RepublicCabinetGUI;
import id.nationcore.gui.republic.RepublicMainMenu;
import id.nationcore.gui.republic.RepublicTaxMenu;
import id.nationcore.gui.republic.RepublicTreasuryMenu;
import id.nationcore.gui.republic.RepublicHealthOfficeGUI;
import id.nationcore.gui.republic.RepublicDefenseOfficeGUI;
import id.nationcore.gui.republic.RepublicTreasuryOfficeGUI;
import id.nationcore.gui.caliphate.CaliphateZakahMenu;
import id.nationcore.gui.communist.CommunistTaxMenu;
import id.nationcore.gui.monarchy.MonarchyTaxMenu;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import id.nationcore.gui.republic.RepublicPresidentHistoryGUI;
import id.nationcore.gui.republic.RepublicRecallGUI;
import id.nationcore.gui.republic.RepublicArenaGUI;
import id.nationcore.gui.republic.RepublicMemberManagementGUI;
import id.nationcore.gui.republic.RepublicSalaryMenu;
import id.nationcore.gui.caliphate.CaliphateDiplomacyMenu;
import id.nationcore.gui.communist.CommunistDiplomacyMenu;
import id.nationcore.gui.monarchy.MonarchyDiplomacyMenu;
import id.nationcore.gui.republic.RepublicDiplomacyMenu;
import id.nationcore.gui.caliphate.CaliphateBorderMenu;
import id.nationcore.gui.communist.CommunistBorderMenu;
import id.nationcore.gui.monarchy.MonarchyBorderMenu;
import id.nationcore.gui.republic.RepublicBorderMenu;
import id.nationcore.gui.republic.RepublicSettingsMenu;
import id.nationcore.gui.communist.CommunistSettingsMenu;
import id.nationcore.gui.monarchy.MonarchySettingsMenu;
import id.nationcore.gui.caliphate.CaliphateSettingsMenu;
import id.nationcore.gui.communist.CommunistSalaryMenu;
import id.nationcore.gui.monarchy.MonarchySalaryMenu;
import id.nationcore.gui.caliphate.CaliphateSalaryMenu;
import id.nationcore.gui.communist.CommunistHealthOfficeGUI;
import id.nationcore.gui.communist.CommunistDefenseOfficeGUI;
import id.nationcore.gui.communist.CommunistTreasuryOfficeGUI;
import id.nationcore.gui.communist.CommunistPropagandaOfficeGUI;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;
import id.nationcore.models.CabinetDecision;
import id.nationcore.models.Government;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

/**
 * Routes Bukkit inventory events to the per-nation and common handler classes.
 * State maps and GUI references live here so handlers in the same package
 * can share them. Per-nation logic lives in:
 * {@link RepublicGUIHandler}, {@link CommunistGUIHandler},
 * {@link MonarchyGUIHandler}, {@link CaliphateGUIHandler}.
 * Cross-nation logic lives in {@link CommonGUIHandler}.
 */
public class GUIListener implements Listener {

    private final NationCore plugin;

    // GUI instances (public so handler classes in subpackages can use them).
    public final VotingGUI votingGUI;
    public final RepublicGovernmentGUI republicGovernmentGUI;
    public final CommunistGovernmentGUI communistGovernmentGUI;
    public final MonarchyGovernmentGUI monarchyGovernmentGUI;
    public final CaliphateGovernmentGUI caliphateGovernmentGUI;
    public final RepublicTreasuryMenu republicTreasuryMenu;
    public final CommunistTreasuryMenu communistTreasuryMenu;
    public final MonarchyTreasuryMenu monarchyTreasuryMenu;
    public final CaliphateTreasuryMenu caliphateTreasuryMenu;
    public final RepublicSalaryMenu salaryMenu;
    public final CommunistSalaryMenu communistSalaryMenu;
    public final MonarchySalaryMenu monarchySalaryMenu;
    public final CaliphateSalaryMenu caliphateSalaryMenu;
    public final RepublicCabinetGUI cabinetGUI;
    public final MainMenuRouter mainMenuRouter;
    public final PlayerStatsGUI playerStatsGUI;
    public final HelpGUI helpGUI;
    public final RepublicRecallGUI recallGUI;
    public final RepublicElectionRegistrationGUI republicElectionRegistrationGUI;
    public final RepublicTaxMenu republicTaxMenu;
    public final CommunistTaxMenu communistTaxMenu;
    public final MonarchyTaxMenu monarchyTaxMenu;
    public final CaliphateZakahMenu caliphateZakahMenu;
    public final RepublicPresidentHistoryGUI presidentHistoryGUI;
    public final RepublicArenaGUI arenaGUI;
    public final HubGUI hubGUI;
    public final CreateNationGUI createNationGUI;
    public final ResearchGUI researchGUI;
    public final ConfirmActionGUI confirmActionGUI;
    public final CommunistMemberManagementGUI communistMemberManagementGUI;
    public final CommunistSharedStorageGUI communistSharedStorageGUI;
    public final RepublicMemberManagementGUI republicMemberManagementGUI;
    public final CaliphateDiplomacyMenu caliphateDiplomacyMenu;
    public final CommunistDiplomacyMenu communistDiplomacyMenu;
    public final MonarchyDiplomacyMenu monarchyDiplomacyMenu;
    public final RepublicDiplomacyMenu republicDiplomacyMenu;
    public final CaliphateBorderMenu caliphateBorderMenu;
    public final CommunistBorderMenu communistBorderMenu;
    public final MonarchyBorderMenu monarchyBorderMenu;
    public final RepublicBorderMenu republicBorderMenu;
    public final RepublicSettingsMenu republicSettingsMenu;
    public final CommunistSettingsMenu communistSettingsMenu;
    public final MonarchySettingsMenu monarchySettingsMenu;
    public final CaliphateSettingsMenu caliphateSettingsMenu;
    public final RepublicHealthOfficeGUI republicHealthOfficeGUI;
    public final RepublicDefenseOfficeGUI republicDefenseOfficeGUI;
    public final RepublicTreasuryOfficeGUI republicTreasuryOfficeGUI;
    public final CommunistHealthOfficeGUI communistHealthOfficeGUI;
    public final CommunistDefenseOfficeGUI communistDefenseOfficeGUI;
    public final CommunistTreasuryOfficeGUI communistTreasuryOfficeGUI;
    public final CommunistPropagandaOfficeGUI communistPropagandaOfficeGUI;

    // Shared per-player state.
    public final Map<UUID, UUID> viewingCandidate = new HashMap<>();
    public final Map<UUID, CabinetDecision.CabinetPosition> viewingCabinetPosition = new HashMap<>();
    public final Map<UUID, Government.CabinetPosition> viewingAppointPosition = new HashMap<>();
    public final Map<UUID, UUID> viewingPlayerStats = new HashMap<>();
    public final Map<UUID, UUID> viewingManagedMember = new HashMap<>();
    public final Map<UUID, Integer> memberListPage = new HashMap<>();
    /**
     * Diplomacy Sub Menu Select state: viewer player → target nation id currently
     * opened.
     */
    public final Map<UUID, String> viewingDiplomacyTarget = new HashMap<>();
    /**
     * Cooldown tracking for "Send Message to Member" action: presidentUUID →
     * (targetUUID → lastSentTimestamp)
     */
    public final Map<UUID, Map<UUID, Long>> memberMessageCooldowns = new HashMap<>();
    /**
     * Cooldown tracking for Capital Teleport action: playerUUID →
     * lastTeleportTimestamp
     */
    public final Map<UUID, Long> capitalTeleportCooldowns = new HashMap<>();
    /**
     * Transient set of players who have toggled the candidacy-agreement document
     * ON in the presidential registration menu. Cleared on successful submission
     * (it is a per-session consent, never persisted).
     */
    public final java.util.Set<UUID> registrationAgreementAccepted = java.util.concurrent.ConcurrentHashMap.newKeySet();

    // Per-nation and common handlers.
    private final RepublicGUIHandler republic;
    private final CommunistGUIHandler communist;
    private final MonarchyGUIHandler monarchy;
    private final CaliphateGUIHandler caliphate;
    private final CommonGUIHandler common;

    public GUIListener(NationCore plugin) {
        this.plugin = plugin;
        this.votingGUI = new VotingGUI(plugin);
        this.republicGovernmentGUI = new RepublicGovernmentGUI(plugin);
        this.communistGovernmentGUI = new CommunistGovernmentGUI(plugin);
        this.monarchyGovernmentGUI = new MonarchyGovernmentGUI(plugin);
        this.caliphateGovernmentGUI = new CaliphateGovernmentGUI(plugin);
        this.republicTreasuryMenu = new RepublicTreasuryMenu(plugin);
        this.communistTreasuryMenu = new CommunistTreasuryMenu(plugin);
        this.monarchyTreasuryMenu = new MonarchyTreasuryMenu(plugin);
        this.caliphateTreasuryMenu = new CaliphateTreasuryMenu(plugin);
        this.salaryMenu = new RepublicSalaryMenu(plugin);
        this.communistSalaryMenu = new CommunistSalaryMenu(plugin);
        this.monarchySalaryMenu = new MonarchySalaryMenu(plugin);
        this.caliphateSalaryMenu = new CaliphateSalaryMenu(plugin);
        this.cabinetGUI = new RepublicCabinetGUI(plugin);
        this.mainMenuRouter = new MainMenuRouter(plugin);
        this.playerStatsGUI = new PlayerStatsGUI(plugin);
        this.helpGUI = new HelpGUI(plugin);
        this.recallGUI = new RepublicRecallGUI(plugin);
        this.republicElectionRegistrationGUI = new RepublicElectionRegistrationGUI(plugin);
        this.republicTaxMenu = new RepublicTaxMenu(plugin);
        this.communistTaxMenu = new CommunistTaxMenu(plugin);
        this.monarchyTaxMenu = new MonarchyTaxMenu(plugin);
        this.caliphateZakahMenu = new CaliphateZakahMenu(plugin);
        this.presidentHistoryGUI = new RepublicPresidentHistoryGUI(plugin);
        this.arenaGUI = new RepublicArenaGUI(plugin);
        this.hubGUI = new HubGUI(plugin);
        this.createNationGUI = new CreateNationGUI(plugin);
        this.researchGUI = new ResearchGUI(plugin);
        this.confirmActionGUI = new ConfirmActionGUI(plugin);
        this.communistMemberManagementGUI = new CommunistMemberManagementGUI(plugin);
        this.communistSharedStorageGUI = new CommunistSharedStorageGUI(plugin);
        this.republicMemberManagementGUI = new RepublicMemberManagementGUI(plugin);
        this.caliphateDiplomacyMenu = new CaliphateDiplomacyMenu(plugin);
        this.communistDiplomacyMenu = new CommunistDiplomacyMenu(plugin);
        this.monarchyDiplomacyMenu = new MonarchyDiplomacyMenu(plugin);
        this.republicDiplomacyMenu = new RepublicDiplomacyMenu(plugin);
        this.caliphateBorderMenu = new CaliphateBorderMenu(plugin);
        this.communistBorderMenu = new CommunistBorderMenu(plugin);
        this.monarchyBorderMenu = new MonarchyBorderMenu(plugin);
        this.republicBorderMenu = new RepublicBorderMenu(plugin);
        this.republicSettingsMenu = new RepublicSettingsMenu(plugin);
        this.communistSettingsMenu = new CommunistSettingsMenu(plugin);
        this.monarchySettingsMenu = new MonarchySettingsMenu(plugin);
        this.caliphateSettingsMenu = new CaliphateSettingsMenu(plugin);
        this.republicHealthOfficeGUI = new RepublicHealthOfficeGUI(plugin);
        this.republicDefenseOfficeGUI = new RepublicDefenseOfficeGUI(plugin);
        this.republicTreasuryOfficeGUI = new RepublicTreasuryOfficeGUI(plugin);
        this.communistHealthOfficeGUI = new CommunistHealthOfficeGUI(plugin);
        this.communistDefenseOfficeGUI = new CommunistDefenseOfficeGUI(plugin);
        this.communistTreasuryOfficeGUI = new CommunistTreasuryOfficeGUI(plugin);
        this.communistPropagandaOfficeGUI = new CommunistPropagandaOfficeGUI(plugin);

        this.republic = new RepublicGUIHandler(plugin, this);
        this.communist = new CommunistGUIHandler(plugin, this);
        this.monarchy = new MonarchyGUIHandler(plugin, this);
        this.caliphate = new CaliphateGUIHandler(plugin, this);
        this.common = new CommonGUIHandler(plugin, this);
    }

    public ResearchGUI getResearchGUI() {
        return researchGUI;
    }

    public HubGUI getHubGUI() {
        return hubGUI;
    }

    public CreateNationGUI getCreateNationGUI() {
        return createNationGUI;
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        String title = event.getView().getTitle();
        ItemStack clicked = event.getCurrentItem();

        // ── Communist Shared Storage Special Handling ───────────────────
        // We handle this early because we need to process clicks on EMPTY slots
        // (for depositing items).
        if (CommunistSharedStorageGUI.isStorageTitle(title)) {
            communist.handleSharedStorage(event, player, clicked, event.getRawSlot());
            return;
        }

        // ── Border Management GUIs (per-nation) ─────────────────────────
        // Handled before the generic AIR early-return so that clicks on the
        // intentionally-empty frame slots are still cancelled (preventing item
        // insertion / loss). Raw slots ignore the player's own inventory, and the
        // ClickType lets the Welcome Message button tell left (set) from right
        // (clear).
        AbstractBorderMenu borderMenu = borderMenuFor(title);
        if (borderMenu != null) {
            event.setCancelled(true);
            if (clicked == null || clicked.getType() == Material.AIR)
                return;
            borderMenu.handleClick(this, player, clicked, event.getRawSlot(), event.getClick());
            return;
        }

        // ── Tax / Zakah menus (per-nation) ───────────────────────────────
        // Routed before the generic AIR early-return so clicks on empty
        // frame slots are always cancelled (preventing item insertion).
        AbstractTaxMenu taxMenu = taxMenuFor(title);
        if (taxMenu != null) {
            event.setCancelled(true);
            taxMenu.handleClick(this, player, clicked, event.getRawSlot());
            return;
        }

        // ── Settings menus (per-nation) ───────────────────────────────
        AbstractSettingsMenu settingsMenu = settingsMenuFor(title);
        if (settingsMenu != null) {
            event.setCancelled(true);
            settingsMenu.handleClick(this, player, clicked, event.getRawSlot(), event.getClick());
            return;
        }

        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE ||
                clicked.getType() == Material.ORANGE_STAINED_GLASS_PANE) {
            event.setCancelled(true);
            return;
        }

        // ── Republic GUIs ───────────────────────────────────────────────
        if (title.equals(RepublicMainMenu.TITLE)) {
            event.setCancelled(true);
            republic.handleMainMenu(player, clicked, event.getSlot());
        } else if (title.equals(VotingGUI.VOTING_GUI_TITLE)) {
            event.setCancelled(true);
            republic.handleVotingGUI(player, clicked);
        } else if (title.startsWith(VotingGUI.CANDIDATE_GUI_TITLE)) {
            event.setCancelled(true);
            republic.handleCandidateGUI(player, clicked, title);
        } else if (title.equals(RepublicGovernmentGUI.TITLE)) {
            event.setCancelled(true);
            republic.handleGovernmentGUI(player, clicked, event.getSlot());
        } else if (title.equals(RepublicExecutiveOrdersMenu.TITLE)) {
            event.setCancelled(true);
            republic.handleOrdersGUI(player, clicked, event.getSlot());
        } else if (title.equals(RepublicHealthOfficeGUI.TITLE)) {
            event.setCancelled(true);
            republic.handleHealthOfficeGUI(player, clicked, event.getSlot());
        } else if (title.equals(RepublicDefenseOfficeGUI.TITLE)) {
            event.setCancelled(true);
            republic.handleDefenseOfficeGUI(player, clicked, event.getSlot());
        } else if (title.equals(RepublicTreasuryOfficeGUI.TITLE)) {
            event.setCancelled(true);
            republic.handleTreasuryOfficeGUI(player, clicked, event.getSlot());
        } else if (title.equals(RepublicMemberManagementGUI.TITLE)) {
            event.setCancelled(true);
            republic.handleMemberManagementGUI(player, clicked, event.getSlot());
        } else if (title.startsWith(RepublicMemberManagementGUI.ACTION_TITLE_PREFIX)) {
            event.setCancelled(true);
            republic.handleMemberActionGUI(player, clicked, event.getSlot(), title);
        } else if (title.equals(RepublicElectionRegistrationGUI.TITLE)) {
            event.setCancelled(true);
            // Only act on top-inventory clicks; a click in the player's own
            // inventory returns an overlapping getSlot() that could otherwise
            // collide with an action slot (e.g. 31 = campaign message).
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                republic.handleRegistrationGUI(player, clicked, event.getSlot());
            }

            // ── Communist GUIs ──────────────────────────────────────────────
        } else if (title.equals(CommunistMainMenu.TITLE)) {
            event.setCancelled(true);
            communist.handleMainMenu(player, clicked, event.getSlot());
        } else if (title.equals(CommunistGovernmentGUI.TITLE)) {
            event.setCancelled(true);
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                communist.handleGovernmentGUI(player, clicked, event.getSlot());
            }
        } else if (title.equals(CommunistLeaderOrdersMenu.TITLE)) {
            event.setCancelled(true);
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                communist.handleLeaderOrdersGUI(player, clicked, event.getSlot());
            }
        } else if (title.equals(CommunistMemberManagementGUI.TITLE)) {
            event.setCancelled(true);
            communist.handleMemberManagementGUI(player, clicked, event.getSlot());
        } else if (title.startsWith(CommunistMemberManagementGUI.ACTION_TITLE_PREFIX)) {
            event.setCancelled(true);
            communist.handleMemberActionGUI(player, clicked, event.getSlot(), title);
        } else if (title.startsWith(CommunistMemberManagementGUI.POLITBURO_PICK_TITLE_PREFIX)) {
            event.setCancelled(true);
            communist.handlePolitburoPickerGUI(player, clicked, event.getSlot(), title);
        } else if (title.equals(CommunistHealthOfficeGUI.TITLE)) {
            event.setCancelled(true);
            communist.handleHealthOfficeGUI(player, clicked, event.getSlot());
        } else if (title.equals(CommunistDefenseOfficeGUI.TITLE)) {
            event.setCancelled(true);
            communist.handleDefenseOfficeGUI(player, clicked, event.getSlot());
        } else if (title.equals(CommunistTreasuryOfficeGUI.TITLE)) {
            event.setCancelled(true);
            communist.handleTreasuryOfficeGUI(player, clicked, event.getSlot());
        } else if (title.equals(CommunistPropagandaOfficeGUI.TITLE)) {
            event.setCancelled(true);
            communist.handlePropagandaOfficeGUI(player, clicked, event.getSlot());

            // ── Monarchy GUIs ───────────────────────────────────────────────
        } else if (title.equals(MonarchyMainMenu.TITLE)) {
            event.setCancelled(true);
            monarchy.handleMainMenu(player, clicked, event.getSlot());
        } else if (title.equals(MonarchyGovernmentGUI.TITLE)) {
            event.setCancelled(true);
            monarchy.handleGovernmentGUI(player, clicked, event.getSlot());
        } else if (title.equals(MonarchyExecutiveOrdersMenu.TITLE)) {
            event.setCancelled(true);
            monarchy.handleOrdersGUI(player, clicked, event.getSlot());
        } else if (title.equals(MonarchyTreasuryMenu.TITLE)) {
            event.setCancelled(true);
            monarchy.handleTreasuryGUI(player, clicked);
        } else if (title.equals(MonarchyTreasuryMenu.LOGS_TITLE)) {
            event.setCancelled(true);
            monarchy.handleTreasuryLogsGUI(player, clicked);

            // ── Caliphate GUIs ──────────────────────────────────────────────
        } else if (title.equals(CaliphateMainMenu.TITLE)) {
            event.setCancelled(true);
            caliphate.handleMainMenu(player, clicked, event.getSlot());
        } else if (title.equals(CaliphateGovernmentGUI.TITLE)) {
            event.setCancelled(true);
            caliphate.handleGovernmentGUI(player, clicked, event.getSlot());
        } else if (title.equals(CaliphateExecutiveOrdersMenu.TITLE)) {
            event.setCancelled(true);
            caliphate.handleOrdersGUI(player, clicked, event.getSlot());
        } else if (title.equals(CaliphateTreasuryMenu.TITLE)) {
            event.setCancelled(true);
            caliphate.handleTreasuryGUI(player, clicked);
        } else if (title.equals(CaliphateTreasuryMenu.LOGS_TITLE)) {
            event.setCancelled(true);
            caliphate.handleTreasuryLogsGUI(player, clicked);

            // ── Diplomacy GUIs (per-nation) ─────────────────────────────────
            // Raw slots are used so clicks inside the player's own inventory
            // (which never overlap the top inventory's raw indices) are ignored.
        } else if (title.equals(RepublicDiplomacyMenu.MANAGEMENT_TITLE)) {
            event.setCancelled(true);
            republicDiplomacyMenu.handleManagementClick(this, player, clicked, event.getRawSlot());
        } else if (title.equals(RepublicDiplomacyMenu.SELECT_TITLE)) {
            event.setCancelled(true);
            republicDiplomacyMenu.handleSelectClick(this, player, clicked, event.getRawSlot());
        } else if (title.equals(CommunistDiplomacyMenu.MANAGEMENT_TITLE)) {
            event.setCancelled(true);
            communistDiplomacyMenu.handleManagementClick(this, player, clicked, event.getRawSlot());
        } else if (title.equals(CommunistDiplomacyMenu.SELECT_TITLE)) {
            event.setCancelled(true);
            communistDiplomacyMenu.handleSelectClick(this, player, clicked, event.getRawSlot());
        } else if (title.equals(MonarchyDiplomacyMenu.MANAGEMENT_TITLE)) {
            event.setCancelled(true);
            monarchyDiplomacyMenu.handleManagementClick(this, player, clicked, event.getRawSlot());
        } else if (title.equals(MonarchyDiplomacyMenu.SELECT_TITLE)) {
            event.setCancelled(true);
            monarchyDiplomacyMenu.handleSelectClick(this, player, clicked, event.getRawSlot());
        } else if (title.equals(CaliphateDiplomacyMenu.MANAGEMENT_TITLE)) {
            event.setCancelled(true);
            caliphateDiplomacyMenu.handleManagementClick(this, player, clicked, event.getRawSlot());
        } else if (title.equals(CaliphateDiplomacyMenu.SELECT_TITLE)) {
            event.setCancelled(true);
            caliphateDiplomacyMenu.handleSelectClick(this, player, clicked, event.getRawSlot());

            // ── Common / cross-nation GUIs ──────────────────────────────────
        } else if (title.equals("§2§l💰 SALARY & REWARDS 💰")) {
            event.setCancelled(true);
            common.handleSalaryGUI(player, clicked);
        } else if (title.equals(RepublicCabinetGUI.CABINET_GUI_TITLE)) {
            event.setCancelled(true);
            common.handleCabinetGUI(player, clicked, event.getSlot(), event.getClick());
        } else if (title.startsWith(RepublicCabinetGUI.CABINET_APPOINT_TITLE)) {
            event.setCancelled(true);
            common.handleCabinetAppointGUI(player, clicked, title);
        } else if (title.equals(RepublicCabinetGUI.CABINET_DECISIONS_TITLE)) {
            event.setCancelled(true);
            common.handleCabinetDecisionsGUI(player, clicked);
        } else if (title.equals(RepublicTreasuryMenu.TITLE)) {
            event.setCancelled(true);
            common.handleTreasuryGUI(player, clicked, event.getSlot(), false);
        } else if (title.equals(CommunistTreasuryMenu.TITLE)) {
            event.setCancelled(true);
            common.handleTreasuryGUI(player, clicked, event.getSlot(), true);
        } else if (title.contains("TREASURY") && !title.contains("LOGS")) {
            // Legacy fallback (global treasury)
            event.setCancelled(true);
            common.handleTreasuryGUI(player, clicked, event.getSlot(), false);
        } else if (title.equals(RepublicTreasuryMenu.LOGS_TITLE)) {
            event.setCancelled(true);
            common.handleTreasuryTransactionsGUI(player, clicked, false);
        } else if (title.equals(CommunistTreasuryMenu.LOGS_TITLE)) {
            event.setCancelled(true);
            common.handleTreasuryTransactionsGUI(player, clicked, true);
        } else if (title.equals("§6§l📜 TREASURY LOGS 📜")) {
            // Legacy fallback
            event.setCancelled(true);
            common.handleTreasuryTransactionsGUI(player, clicked, false);
        } else if (title.equals(RepublicPresidentHistoryGUI.HISTORY_TITLE)) {
            event.setCancelled(true);
            common.handleHistoryGUI(player, clicked, event.getSlot(), false);
        } else if (title.equals(RepublicPresidentHistoryGUI.DETAIL_TITLE)) {
            event.setCancelled(true);
            common.handleHistoryGUI(player, clicked, event.getSlot(), true);
        } else if (title.equals(PlayerStatsGUI.STATS_GUI_TITLE)) {
            event.setCancelled(true);
            common.handlePlayerStatsGUI(player, clicked, event.getSlot());
        } else if (title.equals(HelpGUI.HELP_MENU_TITLE)) {
            event.setCancelled(true);
            common.handleHelpMenuGUI(player, clicked, event.getSlot());
        } else if (title.equals(HelpGUI.HELP_ELECTION_TITLE) ||
                title.equals(HelpGUI.HELP_PRESIDENT_TITLE) ||
                title.equals(HelpGUI.HELP_CABINET_TITLE) ||
                title.equals(HelpGUI.HELP_ORDERS_TITLE) ||
                title.equals(HelpGUI.HELP_ARENA_TITLE) ||
                title.equals(HelpGUI.HELP_TREASURY_TITLE)) {
            event.setCancelled(true);
            common.handleHelpSubMenuGUI(player, clicked);
        } else if (title.equals(RepublicRecallGUI.RECALL_MENU_TITLE)) {
            event.setCancelled(true);
            common.handleRecallMenuGUI(player, clicked, event.getSlot());
        } else if (title.equals(RepublicRecallGUI.RECALL_CONFIRM_TITLE)) {
            event.setCancelled(true);
            common.handleRecallConfirmGUI(player, clicked);
        } else if (title.equals(RepublicRecallGUI.RECALL_VOTE_TITLE)) {
            event.setCancelled(true);
            common.handleRecallVoteGUI(player, clicked);
        } else if (title.equals(RepublicArenaGUI.ARENA_MENU_TITLE)) {
            event.setCancelled(true);
            common.handleArenaGUI(player, clicked, event.getSlot());
        } else if (title.equals(RepublicArenaGUI.ARENA_LEADERBOARD_TITLE)) {
            event.setCancelled(true);
            common.handleArenaLeaderboardGUI(player, clicked);
        } else if (title.equals(RepublicArenaGUI.ARENA_KIT_TITLE)) {
            event.setCancelled(true);
            common.handleArenaKitGUI(player, clicked);
        } else if (title.equals(HubGUI.HUB_TITLE)) {
            event.setCancelled(true);
            common.handleHubGUI(player, clicked, event.getSlot(), event.getClick());
        } else if (title.equals(CreateNationGUI.CREATE_TITLE)) {
            event.setCancelled(true);
            common.handleCreateNationGUI(player, clicked, event.getSlot());
        } else if (ResearchGUI.isResearchTitle(title)) {
            event.setCancelled(true);
            common.handleResearchGUI(player, clicked, event.getSlot(), title);
        } else if (title.equals(ConfirmActionGUI.TITLE)) {
            event.setCancelled(true);
            if (clicked.getType() == Material.LIME_CONCRETE) {
                confirmActionGUI.handleConfirm(player);
            } else if (clicked.getType() == Material.RED_CONCRETE) {
                confirmActionGUI.handleCancel(player);
            }
        }
    }

    /**
     * @return the per-nation Tax/Zakah menu matching this title, or null.
     */
    private AbstractTaxMenu taxMenuFor(String title) {
        if (RepublicTaxMenu.TITLE.equals(title))
            return republicTaxMenu;
        if (CommunistTaxMenu.TITLE.equals(title))
            return communistTaxMenu;
        if (MonarchyTaxMenu.TITLE.equals(title))
            return monarchyTaxMenu;
        if (CaliphateZakahMenu.TITLE.equals(title))
            return caliphateZakahMenu;
        return null;
    }

    /**
     * @return the per-nation Settings menu matching this title, or null.
     */
    private AbstractSettingsMenu settingsMenuFor(String title) {
        if (RepublicSettingsMenu.TITLE.equals(title))
            return republicSettingsMenu;
        if (CommunistSettingsMenu.TITLE.equals(title))
            return communistSettingsMenu;
        if (MonarchySettingsMenu.TITLE.equals(title))
            return monarchySettingsMenu;
        if (CaliphateSettingsMenu.TITLE.equals(title))
            return caliphateSettingsMenu;
        return null;
    }

    /**
     * @return the per-nation Border Management menu matching this title, or null.
     */
    private AbstractBorderMenu borderMenuFor(String title) {
        if (RepublicBorderMenu.TITLE.equals(title))
            return republicBorderMenu;
        if (CommunistBorderMenu.TITLE.equals(title))
            return communistBorderMenu;
        if (MonarchyBorderMenu.TITLE.equals(title))
            return monarchyBorderMenu;
        if (CaliphateBorderMenu.TITLE.equals(title))
            return caliphateBorderMenu;
        return null;
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();

        if (title.equals(RepublicMainMenu.TITLE) ||
                title.equals(CommunistMainMenu.TITLE) ||
                title.equals(MonarchyMainMenu.TITLE) ||
                title.equals(CaliphateMainMenu.TITLE) ||
                title.equals(VotingGUI.VOTING_GUI_TITLE) ||
                title.startsWith(VotingGUI.CANDIDATE_GUI_TITLE) ||
                title.equals(RepublicGovernmentGUI.TITLE) || title.equals(CommunistGovernmentGUI.TITLE) ||
                title.equals(MonarchyGovernmentGUI.TITLE) ||
                title.equals(CaliphateGovernmentGUI.TITLE) ||
                title.equals("§2§l💰 SALARY & REWARDS 💰") ||
                title.equals(RepublicExecutiveOrdersMenu.TITLE) ||
                title.equals(RepublicHealthOfficeGUI.TITLE) ||
                title.equals(RepublicDefenseOfficeGUI.TITLE) ||
                title.equals(RepublicTreasuryOfficeGUI.TITLE) ||
                title.equals(CommunistHealthOfficeGUI.TITLE) ||
                title.equals(CommunistDefenseOfficeGUI.TITLE) ||
                title.equals(CommunistTreasuryOfficeGUI.TITLE) ||
                title.equals(CommunistPropagandaOfficeGUI.TITLE) ||
                title.equals(CommunistLeaderOrdersMenu.TITLE) ||
                title.equals(MonarchyExecutiveOrdersMenu.TITLE) ||
                title.equals(CaliphateExecutiveOrdersMenu.TITLE) ||
                title.equals(RepublicCabinetGUI.CABINET_GUI_TITLE) ||
                title.startsWith(RepublicCabinetGUI.CABINET_APPOINT_TITLE) ||
                title.equals(RepublicCabinetGUI.CABINET_DECISIONS_TITLE) ||
                title.contains("TREASURY") ||
                title.equals(RepublicTreasuryMenu.TITLE) ||
                title.equals(CommunistTreasuryMenu.TITLE) ||
                title.equals(MonarchyTreasuryMenu.TITLE) ||
                title.equals(CaliphateTreasuryMenu.TITLE) ||
                title.equals(RepublicTreasuryMenu.LOGS_TITLE) ||
                title.equals(CommunistTreasuryMenu.LOGS_TITLE) ||
                title.equals(MonarchyTreasuryMenu.LOGS_TITLE) ||
                title.equals(CaliphateTreasuryMenu.LOGS_TITLE) ||
                title.equals(RepublicDiplomacyMenu.MANAGEMENT_TITLE) ||
                title.equals(RepublicDiplomacyMenu.SELECT_TITLE) ||
                title.equals(CommunistDiplomacyMenu.MANAGEMENT_TITLE) ||
                title.equals(CommunistDiplomacyMenu.SELECT_TITLE) ||
                title.equals(MonarchyDiplomacyMenu.MANAGEMENT_TITLE) ||
                title.equals(MonarchyDiplomacyMenu.SELECT_TITLE) ||
                title.equals(CaliphateDiplomacyMenu.MANAGEMENT_TITLE) ||
                title.equals(CaliphateDiplomacyMenu.SELECT_TITLE) ||
                title.equals(RepublicBorderMenu.TITLE) ||
                title.equals(CommunistBorderMenu.TITLE) ||
                title.equals(MonarchyBorderMenu.TITLE) ||
                title.equals(CaliphateBorderMenu.TITLE) ||
                title.equals(RepublicPresidentHistoryGUI.HISTORY_TITLE) ||
                title.equals(RepublicPresidentHistoryGUI.DETAIL_TITLE) ||
                title.equals(PlayerStatsGUI.STATS_GUI_TITLE) ||
                title.equals(PlayerStatsGUI.LEADERBOARD_TITLE) ||
                title.equals(HelpGUI.HELP_MENU_TITLE) ||
                title.contains("PANDUAN") ||
                title.equals(RepublicRecallGUI.RECALL_MENU_TITLE) ||
                title.equals(RepublicRecallGUI.RECALL_CONFIRM_TITLE) ||
                title.equals(RepublicRecallGUI.RECALL_VOTE_TITLE) ||
                title.equals(RepublicElectionRegistrationGUI.TITLE) ||
                title.equals(RepublicTaxMenu.TITLE) ||
                title.equals(CommunistTaxMenu.TITLE) ||
                title.equals(MonarchyTaxMenu.TITLE) ||
                title.equals(CaliphateZakahMenu.TITLE) ||
                title.equals(RepublicSettingsMenu.TITLE) ||
                title.equals(CommunistSettingsMenu.TITLE) ||
                title.equals(MonarchySettingsMenu.TITLE) ||
                title.equals(CaliphateSettingsMenu.TITLE) ||
                title.equals(RepublicArenaGUI.ARENA_MENU_TITLE) ||
                title.equals(RepublicArenaGUI.ARENA_LEADERBOARD_TITLE) ||
                title.equals(RepublicArenaGUI.ARENA_KIT_TITLE) ||
                title.equals(HubGUI.HUB_TITLE) ||
                title.equals(CreateNationGUI.CREATE_TITLE) ||
                title.equals(ConfirmActionGUI.TITLE) ||
                title.equals(CommunistMemberManagementGUI.TITLE) ||
                title.startsWith(CommunistMemberManagementGUI.ACTION_TITLE_PREFIX) ||
                title.startsWith(CommunistMemberManagementGUI.POLITBURO_PICK_TITLE_PREFIX) ||
                ResearchGUI.isResearchTitle(title)) {
            event.setCancelled(true);
        }

        // Shared Storage allows drags within storage area
        if (CommunistSharedStorageGUI.isStorageTitle(title)) {
            Player player2 = (Player) event.getWhoClicked();
            Nation ssNation = plugin.getNationManager().getNationOf(player2.getUniqueId());
            if (ssNation != null) {
                boolean cancelDrag = communistSharedStorageGUI.handleDrag(event, player2, ssNation);
                if (cancelDrag)
                    event.setCancelled(true);
            } else {
                event.setCancelled(true);
            }
        }
    }

    // ==========================================================
    // Public API — called by commands, chat listeners, MainMenuRouter, etc.
    // ==========================================================

    public void openMainMenu(Player player) {
        mainMenuRouter.openFor(player);
    }

    public void openVotingGUI(Player player) {
        votingGUI.openVotingMenu(player);
    }

    public void openGovernmentGUI(Player player) {
        Nation n = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (n != null && n.getType() == GovernmentType.MONARCHY) {
            monarchyGovernmentGUI.open(player, n);
        } else if (n != null && n.getType() == GovernmentType.COMMUNIST) {
            communistGovernmentGUI.open(player, n);
        } else if (n != null && n.getType() == GovernmentType.CALIPHATE) {
            caliphateGovernmentGUI.open(player, n);
        } else {
            republicGovernmentGUI.open(player, n);
        }
    }

    public void openOrdersGUI(Player player) {
        Nation n = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (n != null && n.getType() == GovernmentType.MONARCHY) {
            new MonarchyExecutiveOrdersMenu(plugin).open(player, n);
        } else if (n != null && n.getType() == GovernmentType.COMMUNIST) {
            // Deleted CommunistExecutiveOrdersMenu - Party Policy displays active directives directly in lore
        } else if (n != null && n.getType() == GovernmentType.CALIPHATE) {
            new CaliphateExecutiveOrdersMenu(plugin).open(player, n);
        } else {
            new RepublicExecutiveOrdersMenu(plugin).open(player, n);
        }
    }

    public void openCabinetGUI(Player player) {
        cabinetGUI.openCabinetMenu(player);
    }

    public void openTreasuryGUI(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation != null && nation.getType() == GovernmentType.MONARCHY) {
            monarchyTreasuryMenu.open(player, nation);
        } else if (nation != null && nation.getType() == GovernmentType.COMMUNIST) {
            communistTreasuryMenu.open(player, nation);
        } else if (nation != null && nation.getType() == GovernmentType.CALIPHATE) {
            caliphateTreasuryMenu.open(player, nation);
        } else {
            republicTreasuryMenu.open(player);
        }
    }

    public void openHistoryGUI(Player player) {
        presidentHistoryGUI.openHistoryMenu(player);
    }

    public void openPlayerStatsGUI(Player player, UUID targetUUID) {
        viewingPlayerStats.put(player.getUniqueId(), targetUUID);
        playerStatsGUI.openPlayerStats(player, targetUUID);
    }

    public void openHelpGUI(Player player) {
        helpGUI.openHelpMenu(player);
    }

    public void openRecallGUI(Player player) {
        recallGUI.openRecallMenu(player);
    }

    public void openTaxGUI(Player player) {
        Nation n = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (n != null && n.getType() == GovernmentType.MONARCHY) {
            monarchyTaxMenu.open(player);
        } else if (n != null && n.getType() == GovernmentType.COMMUNIST) {
            communistTaxMenu.open(player);
        } else if (n != null && n.getType() == GovernmentType.CALIPHATE) {
            caliphateZakahMenu.open(player);
        } else {
            republicTaxMenu.open(player);
        }
    }

    public void openSettingsGUI(Player player) {
        Nation n = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (n == null) return;
        if (n.getType() == GovernmentType.MONARCHY) {
            monarchySettingsMenu.open(player);
        } else if (n.getType() == GovernmentType.COMMUNIST) {
            communistSettingsMenu.open(player);
        } else if (n.getType() == GovernmentType.CALIPHATE) {
            caliphateSettingsMenu.open(player);
        } else {
            republicSettingsMenu.open(player);
        }
    }

    public void openArenaGUI(Player player) {
        arenaGUI.openArenaMenu(player);
    }

    public void openHubGUI(Player player) {
        hubGUI.open(player);
    }

    public void openCreateNationGUI(Player player) {
        createNationGUI.open(player);
    }

    /**
     * Execute a GUI action generically based on {@link GUIAction}.
     *
     * @param player     The player executing the action
     * @param action     The GUIAction to execute
     * @param itemKey    The item key from gui.yml (for getting command value)
     * @param currentGUI The title of current GUI (for refresh action)
     * @param state      Optional state (for stateful items)
     */
    public void executeGUIAction(Player player, GUIAction action, String itemKey, String currentGUI, String state) {
        switch (action) {
            case OPEN_GUI_MAIN_MENU:
                mainMenuRouter.openFor(player);
                break;
            case OPEN_GUI_PLAYER_STATS:
                viewingPlayerStats.put(player.getUniqueId(), player.getUniqueId());
                playerStatsGUI.openPlayerStats(player, player.getUniqueId());
                break;
            case OPEN_GUI_GOVERNMENT:
                openGovernmentGUI(player);
                break;
            case OPEN_GUI_VOTING:
                votingGUI.openVotingMenu(player);
                break;
            case OPEN_GUI_CABINET:
                cabinetGUI.openCabinetMenu(player);
                break;
            case OPEN_GUI_TREASURY:
                openTreasuryGUI(player);
                break;
            case OPEN_GUI_EXECUTIVE_ORDERS:
                openOrdersGUI(player);
                break;
            case OPEN_GUI_RECALL:
                recallGUI.openRecallMenu(player);
                break;
            case OPEN_GUI_HISTORY:
                presidentHistoryGUI.openHistoryMenu(player);
                break;

            case OPEN_GUI_HELP:
                helpGUI.openHelpMenu(player);
                break;
            case OPEN_GUI_TAX:
                openTaxGUI(player);
                break;
            case OPEN_GUI_ARENA:
                arenaGUI.openArenaMenu(player);
                break;
            case OPEN_GUI_ARENA_LEADERBOARD:
                arenaGUI.openLeaderboard(player);
                break;
            case OPEN_GUI_ARENA_KIT:
                arenaGUI.openKitInfo(player);
                break;
            case OPEN_GUI_CABINET_APPOINT:
                cabinetGUI.openCabinetMenu(player);
                break;

            case ACTION_REGISTER_CANDIDATE:
                player.closeInventory();
                plugin.getElectionManager().registerCandidate(player, "");
                break;
            case ACTION_RATE_PRESIDENT:
                player.closeInventory();
                MessageUtils.send(player, "<yellow>Use: <white>/dc rate <1-5> <gray>to give rating for president");
                break;
            case ACTION_CLOSE_INVENTORY:
                player.closeInventory();
                break;
            case ACTION_ARENA_INFO:
                common.showArenaInfo(player);
                break;
            case ACTION_ENDORSE_CANDIDATE:
                player.closeInventory();
                MessageUtils.send(player, "<yellow>Use: <white>/dc endorse <candidate_name>");
                break;
            case ACTION_DONATE_TREASURY:
                player.closeInventory();
                MessageUtils.send(player, "<yellow>Use: <white>/dc treasury donate <value>");
                break;
            case ACTION_JOIN_ARENA:
                player.closeInventory();
                plugin.getArenaManager().joinArena(player);
                break;
            case ACTION_ARENA_START:
                player.closeInventory(); {
                id.nationcore.models.Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
                if (nation == null || !plugin.getArenaManager().startArena(nation, player.getUniqueId())) {
                    MessageUtils.send(player, "<red>Cannot start arena! Check requirements.");
                } else {
                    arenaGUI.openArenaMenu(player);
                }
            }
                break;
            case ACTION_ARENA_END:
                player.closeInventory(); {
                id.nationcore.models.Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
                if (nation != null) {
                    plugin.getArenaManager().endArena(nation);
                    MessageUtils.send(player, "<gold>Arena ended. Final rewards distributed.");
                } else {
                    MessageUtils.send(player, "<red>You do not belong to a nation.");
                }
            }
                break;
            case ACTION_ARENA_LEAVE:
                player.closeInventory();
                if (!plugin.getArenaManager().leaveArena(player.getUniqueId())) {
                    MessageUtils.send(player, "<red>You are not in the arena!");
                }
                break;

            case ACTION_REFRESH_GUI:
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (currentGUI.equals(RepublicMainMenu.TITLE)
                            || currentGUI.equals(CommunistMainMenu.TITLE)
                            || currentGUI.equals(MonarchyMainMenu.TITLE)
                            || currentGUI.equals(CaliphateMainMenu.TITLE)) {
                        mainMenuRouter.openFor(player);
                    } else if (currentGUI.equals(VotingGUI.VOTING_GUI_TITLE)) {
                        votingGUI.openVotingMenu(player);
                    } else if (currentGUI.equals(RepublicGovernmentGUI.TITLE)
                            || currentGUI.equals(CommunistGovernmentGUI.TITLE)
                            || currentGUI.equals(MonarchyGovernmentGUI.TITLE)
                            || currentGUI.equals(CaliphateGovernmentGUI.TITLE)) {
                        openGovernmentGUI(player);
                    } else if (currentGUI.equals(RepublicCabinetGUI.CABINET_GUI_TITLE)) {
                        cabinetGUI.openCabinetMenu(player);
                    } else if (currentGUI.equals(RepublicRecallGUI.RECALL_MENU_TITLE)) {
                        recallGUI.openRecallMenu(player);
                    } else if (currentGUI.equals(PlayerStatsGUI.STATS_GUI_TITLE)) {
                        UUID targetUUID = viewingPlayerStats.getOrDefault(player.getUniqueId(), player.getUniqueId());
                        playerStatsGUI.openPlayerStats(player, targetUUID);

                    } else if (currentGUI.equals(HelpGUI.HELP_MENU_TITLE)) {
                        helpGUI.openHelpMenu(player);
                    } else if (taxMenuFor(currentGUI) != null) {
                        openTaxGUI(player);
                    }
                }, 1L);
                break;

            case ACTION_CONSOLE_COMMAND:
                player.closeInventory();
                MessageUtils.send(player,
                        "<gray>The 'console_command' action is no longer available in the new menu.</gray>");
                break;

            case UNKNOWN:
            default:
                plugin.getLogger().warning("Unknown GUI action: " + action);
                break;
        }
    }

    public void executeGUIAction(Player player, GUIAction action, String itemKey, String currentGUI) {
        executeGUIAction(player, action, itemKey, currentGUI, null);
    }
}
