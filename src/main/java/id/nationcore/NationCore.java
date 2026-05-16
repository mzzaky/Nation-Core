package id.nationcore;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import id.nationcore.models.PlayerData;

import id.nationcore.commands.NationCommand;
import id.nationcore.gui.GUIListener;
import id.nationcore.listeners.ArenaListener;
import id.nationcore.listeners.ChatListener;
import id.nationcore.listeners.DamageListener;
import id.nationcore.listeners.EconomyListener;
import id.nationcore.listeners.PlayerListener;
import id.nationcore.managers.ArenaManager;
import id.nationcore.managers.BuffManager;
import id.nationcore.managers.CabinetManager;
import id.nationcore.managers.CommunistManager;
import id.nationcore.managers.MonarchyManager;
import id.nationcore.managers.CaliphateManager;
import id.nationcore.managers.DataManager;
import id.nationcore.managers.ElectionManager;
import id.nationcore.managers.ExecutiveOrderManager;
import id.nationcore.managers.GovernmentManager;
import id.nationcore.managers.NationManager;
import id.nationcore.managers.RecallManager;
import id.nationcore.managers.ResearchManager;
import id.nationcore.managers.TaxManager;
import id.nationcore.managers.TerritoryManager;
import id.nationcore.managers.TreasuryManager;
import id.nationcore.utils.MessageUtils;
import id.nationcore.utils.VaultHook;

public class NationCore extends JavaPlugin {

    private static NationCore instance;

    private DataManager dataManager;
    private GovernmentManager governmentManager;
    private ElectionManager electionManager;
    private TreasuryManager treasuryManager;
    private ExecutiveOrderManager executiveOrderManager;
    private CabinetManager cabinetManager;
    private ArenaManager arenaManager;
    private BuffManager buffManager;
    private RecallManager recallManager;
    private TaxManager taxManager;
    private NationManager nationManager;
    private TerritoryManager territoryManager;
    private CommunistManager communistManager;
    private MonarchyManager monarchyManager;
    private CaliphateManager caliphateManager;
    private id.nationcore.managers.DiplomacyManager diplomacyManager;
    private ResearchManager researchManager;
    private VaultHook vaultHook;
    private GUIListener guiListener;
    private YamlConfiguration languageConfig;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        if (!new File(getDataFolder(), "language.yml").exists()) {
            saveResource("language.yml", false);
        }
        languageConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "language.yml"));

        MessageUtils.loadLanguage();

        // Initialize Vault
        vaultHook = new VaultHook(this);
        if (!vaultHook.setupEconomy()) {
            getLogger().severe("Vault not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize managers
        dataManager = new DataManager(this);
        treasuryManager = new TreasuryManager(this);
        buffManager = new BuffManager(this);
        governmentManager = new GovernmentManager(this);
        electionManager = new ElectionManager(this);
        executiveOrderManager = new ExecutiveOrderManager(this);
        cabinetManager = new CabinetManager(this);
        arenaManager = new ArenaManager(this);
        recallManager = new RecallManager(this);
        taxManager = new TaxManager(this);
        nationManager = new NationManager(this);
        territoryManager = new TerritoryManager(this);
        communistManager = new CommunistManager(this);
        monarchyManager = new MonarchyManager(this);
        caliphateManager = new CaliphateManager(this);
        diplomacyManager = new id.nationcore.managers.DiplomacyManager(this);
        researchManager = new ResearchManager(this);

        // Load data
        dataManager.loadAll();

        // Register commands
        getCommand("nationcore").setExecutor(new NationCommand(this));
        getCommand("nationcore").setTabCompleter(new NationCommand(this));

        // Register listeners
        registerListeners();

        // Start scheduled tasks
        startScheduledTasks();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new id.nationcore.placeholders.NationExpansion(this).register();
            getLogger().info("PlaceholderAPI found! Registered placeholders.");
        }

        sendSplashScreen();
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAll();
        }
        getLogger().info("NationCore has been disabled!");
    }

    private void sendSplashScreen() {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        String version = getDescription().getVersion();

        console.sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
        console.sendMessage(
                ChatColor.translateAlternateColorCodes('&', "  &b_____                                              "));
        console.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &b|  __ \\                                            "));
        console.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &b| |  | | ___ _ __ ___   ___   ___ _ __ __ _  ___ _   _ "));
        console.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &b| |  | |/ _ \\ '_ ` _ \\ / _ \\ / __| '__/ _` |/ __| | | |"));
        console.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &b| |__| |  __/ | | | | | (_) | (__| | | (_| | (__| |_| |"));
        console.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &b|_____/ \\___|_| |_| |_|\\___/ \\___|_|  \\__,_|\\___|\\__, |"));
        console.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "            &3Core                 &fPlugin          &b__/ |"));
        console.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "                                                  &b|___/ "));
        console.sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
        console.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &7===================================================="));
        console.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &ePlugin Info:"));
        console.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &8• &aVersion: &f" + version));
        console.sendMessage(
                ChatColor.translateAlternateColorCodes('&', "  &8• &aServer: &f" + getServer().getVersion()));
        console.sendMessage(
                ChatColor.translateAlternateColorCodes('&', "  &8• &aStatus: &a&lSUCCESSFULLY LOADED \u2714"));
        console.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &7===================================================="));
        console.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &3Thank you for using NationCore!"));
        console.sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new EconomyListener(this), this);
        guiListener = new GUIListener(this);
        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(new ArenaListener(this), this);
        getServer().getPluginManager().registerEvents(
                new id.nationcore.listeners.CapitalListener(this), this);
        getServer().getPluginManager().registerEvents(
                new id.nationcore.listeners.CommunistListener(this), this);
        getServer().getPluginManager().registerEvents(
                new id.nationcore.listeners.ResearchListener(this), this);
    }

    private void startScheduledTasks() {
        // Check election phases every minute (legacy global + semua nation REPUBLIC)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            electionManager.checkAllPhaseTransitions();
        }, 20L * 60, 20L * 60);

        // Apply buffs every 30 seconds
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            buffManager.refreshAllBuffs();
        }, 20L * 30, 20L * 30);

        // Check daily rewards - Moved to Manual Claim in GUI

        // Check president activity every hour (legacy + per-nation REPUBLIC)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            governmentManager.checkPresidentActivity();
            for (id.nationcore.models.Nation n : nationManager.getAllNations()) {
                governmentManager.checkPresidentActivity(n);
            }
        }, 20L * 60 * 60, 20L * 60 * 60);

        // Broadcast campaign messages every 5 minutes during campaign phase
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            electionManager.broadcastAllCampaignMessages();
        }, 20L * 60 * 5, 20L * 60 * 5);

        // Check recall petition status every minute (legacy + per-nation)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            recallManager.checkAllPetitionStatus();
        }, 20L * 60, 20L * 60);

        // Communist scheduler: cek tax phase, party election, free food setiap menit.
        // Tiap manager tahu interval-nya sendiri, jadi loop ini ringan.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            communistManager.checkAllPartyElections();
            communistManager.checkAllTaxPhases();
            communistManager.checkAllFreeFoodDistributions();
        }, 20L * 60, 20L * 60);

        // Politbiro salary: cek tiap 1 jam, bayar 10k vault/week ke setiap menteri
        // Politbiro yang sudah lewat 7 hari sejak gaji terakhir.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            communistManager.checkPolitburoSalaries();
        }, 20L * 60 * 60, 20L * 60 * 60);

        // Monarchy schedulers: royal tax phase, royal alms, council stipends.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            monarchyManager.checkAllTaxPhases();
            monarchyManager.checkAllAlmsDistributions();
        }, 20L * 60, 20L * 60);
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            monarchyManager.checkCouncilSalaries();
        }, 20L * 60 * 60, 20L * 60 * 60);

        // Caliphate schedulers: Jizya tax phase, Zakat distribution.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            caliphateManager.checkAllTaxPhases();
            caliphateManager.checkAllZakatDistributions();
        }, 20L * 60, 20L * 60);

        // Check executive order expirations every minute
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            executiveOrderManager.checkExpirations();
        }, 20L * 60, 20L * 60);

        // Check cabinet decision expirations every minute
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            cabinetManager.checkExpiredDecisions();
        }, 20L * 60, 20L * 60);

        // Tick nation research projects every minute — completes any whose
        // duration has elapsed and applies the resulting level effects.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            researchManager.tick();
        }, 20L * 30, 20L * 30);

        // Save data every 5 minutes
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            dataManager.saveAll();
        }, 20L * 60 * 5, 20L * 60 * 5);

        // Accumulate playtime for all online players every minute
        // This acts as a crash-safe fallback in addition to onPlayerQuit tracking
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                PlayerData data = dataManager.getPlayerData(onlinePlayer.getUniqueId());
                if (data != null) {
                    data.addPlaytime(60_000L); // +1 minute
                }
            }
        }, 20L * 60, 20L * 60); // every 60 seconds

        // Start global tax collection scheduler
        taxManager.startTaxScheduler();
    }

    public static NationCore getInstance() {
        return instance;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public GovernmentManager getGovernmentManager() {
        return governmentManager;
    }

    public ElectionManager getElectionManager() {
        return electionManager;
    }

    public TreasuryManager getTreasuryManager() {
        return treasuryManager;
    }

    public ExecutiveOrderManager getExecutiveOrderManager() {
        return executiveOrderManager;
    }

    public CabinetManager getCabinetManager() {
        return cabinetManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public BuffManager getBuffManager() {
        return buffManager;
    }

    public RecallManager getRecallManager() {
        return recallManager;
    }

    public TaxManager getTaxManager() {
        return taxManager;
    }

    public NationManager getNationManager() {
        return nationManager;
    }

    public TerritoryManager getTerritoryManager() {
        return territoryManager;
    }

    public CommunistManager getCommunistManager() {
        return communistManager;
    }

    public MonarchyManager getMonarchyManager() {
        return monarchyManager;
    }

    public CaliphateManager getCaliphateManager() {
        return caliphateManager;
    }

    public id.nationcore.managers.DiplomacyManager getDiplomacyManager() {
        return diplomacyManager;
    }

    public ResearchManager getResearchManager() {
        return researchManager;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public GUIListener getGUIListener() {
        return guiListener;
    }

    public String getPrefix() {
        return getConfig().getString("general.prefix", "&6[NationCore]&r");
    }

    public void reloadLanguage() {
        languageConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "language.yml"));
        MessageUtils.reloadLanguage();
    }

    public YamlConfiguration getLanguageConfig() {
        return languageConfig;
    }

    /**
     * Stub yang dipertahankan untuk backward-compat dengan command admin reload.
     * Sistem GUI sekarang sepenuhnya kode-driven (no YAML), sehingga tidak ada
     * config GUI yang perlu di-reload.
     */
    public void reloadGUI() {
        getLogger().info("GUI sekarang hardcoded — tidak ada YAML untuk di-reload.");
    }
}
