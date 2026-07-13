package id.nationcore;

import java.io.File;
import java.util.Map;
import java.util.EnumMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import id.nationcore.models.PlayerData;
import id.nationcore.models.GovernmentType;

import id.nationcore.commands.NationCommand;
import id.nationcore.gui.GUIListener;
import id.nationcore.integration.ApartmentTaxIntegration;
import id.nationcore.integration.FactoryTaxIntegration;
import id.nationcore.listeners.ArenaListener;
import id.nationcore.listeners.ChatListener;
import id.nationcore.listeners.DamageListener;
import id.nationcore.listeners.EconomyListener;
import id.nationcore.listeners.PlayerListener;
import id.nationcore.managers.ArenaManager;
import id.nationcore.managers.BorderVisualizationManager;
import id.nationcore.managers.BuffManager;
import id.nationcore.managers.CabinetManager;
import id.nationcore.managers.CommunistManager;
import id.nationcore.managers.MonarchyManager;
import id.nationcore.managers.CaliphateManager;
import id.nationcore.managers.DataManager;
import id.nationcore.managers.ElectionManager;
import id.nationcore.managers.ExecutiveOrderManager;
import id.nationcore.managers.FakeMemberManager;
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
    private BorderVisualizationManager borderVisualizationManager;
    private CommunistManager communistManager;
    private MonarchyManager monarchyManager;
    private CaliphateManager caliphateManager;
    private id.nationcore.managers.DiplomacyManager diplomacyManager;
    private ResearchManager researchManager;
    private FakeMemberManager fakeMemberManager;
    private FactoryTaxIntegration factoryTaxIntegration;
    private ApartmentTaxIntegration apartmentTaxIntegration;
    private VaultHook vaultHook;
    private GUIListener guiListener;
    private YamlConfiguration languageConfig;
    private final Map<GovernmentType, YamlConfiguration> nationConfigs = new EnumMap<>(GovernmentType.class);
    /** Central executive-order catalogue loaded from order.yaml. */
    private YamlConfiguration orderConfig;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        if (!new File(getDataFolder(), "language.yml").exists()) {
            saveResource("language.yml", false);
        }
        languageConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "language.yml"));

        reloadNationConfigs();

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
        borderVisualizationManager = new BorderVisualizationManager(this);
        communistManager = new CommunistManager(this);
        monarchyManager = new MonarchyManager(this);
        caliphateManager = new CaliphateManager(this);
        diplomacyManager = new id.nationcore.managers.DiplomacyManager(this);
        researchManager = new ResearchManager(this);
        fakeMemberManager = new FakeMemberManager(this);

        // Soft-depend integration bridges (consumer plugin -> NationCore centralized tax).
        // Created unconditionally; each only does work when its consumer calls into it
        // via NationCoreAPI and the operator has enabled it in config.yml.
        factoryTaxIntegration = new FactoryTaxIntegration(this);
        apartmentTaxIntegration = new ApartmentTaxIntegration(this);

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
        if (borderVisualizationManager != null) {
            borderVisualizationManager.stop();
        }
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
                "  &7"));
        console.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &ePlugin Info:"));
        console.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &8• &aVersion: &f" + version));
        console.sendMessage(
                ChatColor.translateAlternateColorCodes('&', "  &8• &aServer: &f" + getServer().getVersion()));
        console.sendMessage(
                ChatColor.translateAlternateColorCodes('&', "  &8• &aStatus: &a&lSUCCESSFULLY LOADED \u2714"));
        console.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &7"));
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

        // Start the nation border particle visualizer (renders only nations whose
        // borders are toggled on, and only near online players).
        borderVisualizationManager.start();
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

    public BorderVisualizationManager getBorderVisualizationManager() {
        return borderVisualizationManager;
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

    public FakeMemberManager getFakeMemberManager() {
        return fakeMemberManager;
    }

    /**
     * @return the FactoryCore tax integration handler. Exposed for
     *         {@link id.nationcore.api.NationCoreAPI}; consumer plugins should go
     *         through that API rather than calling this directly.
     */
    public FactoryTaxIntegration getFactoryTaxIntegration() {
        return factoryTaxIntegration;
    }

    /**
     * @return the apartment-tax integration bridge backing
     *         {@link id.nationcore.api.NationCoreAPI}; consumer plugins should go
     *         through that API rather than calling this directly.
     */
    public ApartmentTaxIntegration getApartmentTaxIntegration() {
        return apartmentTaxIntegration;
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

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        reloadNationConfigs();
    }

    public void reloadNationConfigs() {
        File nationsFolder = new File(getDataFolder(), "nations");
        if (!nationsFolder.exists()) {
            nationsFolder.mkdirs();
        }

        int loadedCount = 0;
        for (GovernmentType type : GovernmentType.values()) {
            String fileName = type.getConfigFileName();
            File file = new File(nationsFolder, fileName);
            if (!file.exists()) {
                saveResource("nations/" + fileName, false);
            }
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            nationConfigs.put(type, config);
            loadedCount++;
        }
        getLogger().info("Loaded " + loadedCount + " nation type configurations from the nations/ folder.");

        // Central executive-order catalogue (order.yaml in the data folder root).
        File orderFile = new File(getDataFolder(), "order.yaml");
        if (!orderFile.exists()) {
            saveResource("order.yaml", false);
        }
        orderConfig = YamlConfiguration.loadConfiguration(orderFile);
        getLogger().info("Loaded executive-order catalogue from order.yaml ("
                + orderConfig.getKeys(false).size() + " orders).");

        // Merge Republic (republic.yaml) specific president/games/approval settings into the main configuration instance
        YamlConfiguration republicConfig = nationConfigs.get(GovernmentType.REPUBLIC);
        if (republicConfig != null) {
            for (String key : new String[]{"president", "presidential-games", "approval"}) {
                if (republicConfig.contains(key)) {
                    getConfig().set(key, republicConfig.get(key));
                }
            }
        }
    }

    public double getNationCreationCost(GovernmentType type) {
        YamlConfiguration config = nationConfigs.get(type);
        if (config == null) return 500000.0;
        return config.getDouble("creation.cost", 500000.0);
    }

    public double getNationCreationMinPlaytime(GovernmentType type) {
        YamlConfiguration config = nationConfigs.get(type);
        if (config == null) return 0.0;
        return config.getDouble("creation.min-playtime-hours", 0.0);
    }

    public double getNationCreationStartingTreasuryPercent(GovernmentType type) {
        YamlConfiguration config = nationConfigs.get(type);
        if (config == null) return 80.0;
        return config.getDouble("creation.starting-treasury-percent", 80.0);
    }

    public String getNationCreationDisplayName(GovernmentType type) {
        YamlConfiguration config = nationConfigs.get(type);
        if (config == null || !config.contains("creation.display")) {
            return type.getColorCode() + "&l" + type.getDisplayName().toUpperCase();
        }
        return config.getString("creation.display");
    }

    public java.util.List<String> getNationCreationDescription(GovernmentType type) {
        YamlConfiguration config = nationConfigs.get(type);
        if (config == null || !config.contains("creation.description")) {
            java.util.List<String> defaultLore = new java.util.ArrayList<>();
            defaultLore.add("&7" + type.getShortDescription());
            defaultLore.add("");
            for (String line : type.getHighlights()) {
                defaultLore.add(line);
            }
            return defaultLore;
        }
        return config.getStringList("creation.description");
    }

    public long getRecallCooldownDays() {
        YamlConfiguration config = nationConfigs.get(GovernmentType.REPUBLIC);
        if (config == null) return 15;
        return config.getLong("recall.cooldown-days", 15);
    }

    public double getRecallSignatureDeposit() {
        YamlConfiguration config = nationConfigs.get(GovernmentType.REPUBLIC);
        if (config == null) return 50000;
        return config.getDouble("recall.signature-deposit", 50000);
    }

    public long getRecallCollectionDays() {
        YamlConfiguration config = nationConfigs.get(GovernmentType.REPUBLIC);
        if (config == null) return 7;
        return config.getLong("recall.collection-days", 7);
    }

    public long getRecallVotingDays() {
        YamlConfiguration config = nationConfigs.get(GovernmentType.REPUBLIC);
        if (config == null) return 3;
        return config.getLong("recall.voting-days", 3);
    }

    public double getRecallRequiredPercentage() {
        YamlConfiguration config = nationConfigs.get(GovernmentType.REPUBLIC);
        if (config == null) return 60;
        return config.getDouble("recall.required-percentage", 60);
    }

    public double getRecallRequiredSignaturePercentage() {
        YamlConfiguration config = nationConfigs.get(GovernmentType.REPUBLIC);
        if (config == null) return 30;
        return config.getDouble("recall.required-signature-percentage", 30);
    }

    public long getCommunistElectionCycleDays() {
        YamlConfiguration config = nationConfigs.get(GovernmentType.COMMUNIST);
        if (config == null) return 7;
        return config.getLong("communist.election-cycle-days", 7);
    }

    public long getCommunistTaxPhaseMinutes() {
        YamlConfiguration config = nationConfigs.get(GovernmentType.COMMUNIST);
        if (config == null) return 140;
        return config.getLong("communist.tax-phase-minutes", 140);
    }

    public double getCommunistTaxAmount() {
        YamlConfiguration config = nationConfigs.get(GovernmentType.COMMUNIST);
        if (config == null) return 50.0;
        return config.getDouble("communist.tax-amount", 50.0);
    }

    public long getCommunistFreeFoodIntervalHours() {
        YamlConfiguration config = nationConfigs.get(GovernmentType.COMMUNIST);
        if (config == null) return 24;
        return config.getLong("communist.free-food-interval-hours", 24);
    }

    public double getCommunistFreeFoodCostPerPlayer() {
        YamlConfiguration config = nationConfigs.get(GovernmentType.COMMUNIST);
        if (config == null) return 1000.0;
        return config.getDouble("communist.free-food-cost-per-player", 1000.0);
    }

    public int getCommunistFreeFoodBread() {
        YamlConfiguration config = nationConfigs.get(GovernmentType.COMMUNIST);
        if (config == null) return 16;
        return config.getInt("communist.free-food-bread", 16);
    }

    public double getCommunistMinisterWeeklySalary() {
        YamlConfiguration config = nationConfigs.get(GovernmentType.COMMUNIST);
        if (config == null) return 10000.0;
        return config.getDouble("communist.minister-weekly-salary", 10000.0);
    }

    public double getCommunistMarketEventBonus() {
        YamlConfiguration config = nationConfigs.get(GovernmentType.COMMUNIST);
        if (config == null) return 25.0;
        return config.getDouble("communist.market-event-bonus", 25.0);
    }

    public YamlConfiguration getNationConfig(GovernmentType type) {
        return nationConfigs.get(type);
    }

    /**
     * The central executive-order catalogue (order.yaml). Never null after
     * {@link #reloadNationConfigs()} has run.
     */
    public YamlConfiguration getOrderConfig() {
        return orderConfig;
    }
}
