package id.nationcore.gui;

/**
 * Enum representing all possible GUI actions that can be triggered via on_click
 * in gui.yml
 * 
 * Actions are categorized as:
 * - OPEN_GUI_* : Opens a specific GUI
 * - ACTION_* : Performs a specific action (may close inventory or show message)
 */
public enum GUIAction {
    // GUI Opening Actions
    OPEN_GUI_MAIN_MENU("open_gui_main_menu"),
    OPEN_GUI_PLAYER_STATS("open_gui_player_stats"),
    OPEN_GUI_GOVERNMENT("open_gui_government"),
    OPEN_GUI_VOTING("open_gui_voting"),
    OPEN_GUI_CABINET("open_gui_cabinet"),
    OPEN_GUI_TREASURY("open_gui_treasury"),
    OPEN_GUI_EXECUTIVE_ORDERS("open_gui_executive_orders"),
    OPEN_GUI_RECALL("open_gui_recall"),
    OPEN_GUI_HISTORY("open_gui_history"),
    OPEN_GUI_LEADERBOARD("open_gui_leaderboard"),
    OPEN_GUI_HELP("open_gui_help"),
    OPEN_GUI_TAX("open_gui_tax"),
    OPEN_GUI_ARENA("open_gui_arena"),
    OPEN_GUI_ARENA_LEADERBOARD("open_gui_arena_leaderboard"),
    OPEN_GUI_ARENA_KIT("open_gui_arena_kit"),
    OPEN_GUI_CABINET_APPOINT("open_gui_cabinet_appoint"),

    // Specific Actions
    ACTION_REGISTER_CANDIDATE("action_register_candidate"),
    ACTION_RATE_PRESIDENT("action_rate_president"),
    ACTION_CLOSE_INVENTORY("action_close_inventory"),
    ACTION_ARENA_INFO("action_arena_info"),
    ACTION_ENDORSE_CANDIDATE("action_endorse_candidate"),
    ACTION_DONATE_TREASURY("action_donate_treasury"),
    ACTION_JOIN_ARENA("action_join_arena"),
    ACTION_ARENA_START("action_arena_start"),
    ACTION_ARENA_END("action_arena_end"),
    ACTION_ARENA_LEAVE("action_arena_leave"),
    ACTION_REFRESH_GUI("action_refresh_gui"),
    ACTION_CONSOLE_COMMAND("action_console_command"),

    // Unknown/Default
    UNKNOWN("unknown");

    private final String configKey;

    GUIAction(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }

    /**
     * Parse a GUI action from a config string (e.g., "<open_gui_voting>")
     * 
     * @param configValue The value from gui.yml (e.g., "<open_gui_voting>")
     * @return The corresponding GUIAction enum value
     */
    public static GUIAction fromConfig(String configValue) {
        if (configValue == null || configValue.isEmpty()) {
            return UNKNOWN;
        }

        // Remove angle brackets if present
        String cleanValue = configValue.replace("<", "").replace(">", "").toLowerCase();

        for (GUIAction action : values()) {
            if (action.configKey.equalsIgnoreCase(cleanValue)) {
                return action;
            }
        }

        return UNKNOWN;
    }
}
