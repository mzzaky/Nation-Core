package id.nationcore.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import id.nationcore.NationCore;

/**
 * Help GUI - Guide and help for all NationCore features
 */
public class HelpGUI {

    private final NationCore plugin;

    public static final String HELP_MENU_TITLE = "§a§l❓ DEMOCRACY CORE GUIDE ❓";
    public static final String HELP_ELECTION_TITLE = "§b§l📖 ELECTION GUIDE";
    public static final String HELP_PRESIDENT_TITLE = "§6§l📖 PRESIDENT GUIDE";
    public static final String HELP_CABINET_TITLE = "§e§l📖 CABINET GUIDE";
    public static final String HELP_ORDERS_TITLE = "§c§l📖 EXECUTIVE ORDERS GUIDE";
    public static final String HELP_ARENA_TITLE = "§4§l📖 ARENA GUIDE";
    public static final String HELP_TREASURY_TITLE = "§6§l📖 TREASURY GUIDE";

    public HelpGUI(NationCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Open main help menu
     */
    public void openHelpMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, HELP_MENU_TITLE);

        // Header
        ItemStack headerItem = createItem(Material.KNOWLEDGE_BOOK, "§a§l✦ WELCOME ✦",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§fNationCore is a plugin",
                "§ffor nation simulation and",
                "§fgovernment in Minecraft!",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Select a topic below to",
                "§7learn about plugin features");
        inv.setItem(4, headerItem);

        // === Topik Bantuan ===

        // Election
        ItemStack electionHelp = createItem(Material.PAPER, "§b§l🗳 ELECTION SYSTEM",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Learn about:",
                "§f• Election phases",
                "§f• How to register as candidate",
                "§f• How to endorse",
                "§f• How to vote",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§aClick to open guide");
        inv.setItem(19, electionHelp);

        // President
        ItemStack presidentHelp = createItem(Material.GOLDEN_HELMET, "§6§l👑 BECOMING PRESIDENT",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Learn about:",
                "§f• President requirements",
                "§f• President powers",
                "§f• President buffs & rewards",
                "§f• Term length",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§aClick to open guide");
        inv.setItem(21, presidentHelp);

        // Cabinet
        ItemStack cabinetHelp = createItem(Material.LECTERN, "§e§l📋 CABINET SYSTEM",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Learn about:",
                "§f• Cabinet positions",
                "§f• Minister duties",
                "§f• Cabinet decisions",
                "§f• Minister buffs & rewards",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§aClick to open guide");
        inv.setItem(23, cabinetHelp);

        // Executive Orders
        ItemStack ordersHelp = createItem(Material.WRITABLE_BOOK, "§c§l📜 EXECUTIVE ORDERS",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Learn about:",
                "§f• Order types",
                "§f• Effects of each order",
                "§f• Cost & cooldown",
                "§f• How to issue orders",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§aClick to open guide");
        inv.setItem(25, ordersHelp);

        // Arena
        ItemStack arenaHelp = createItem(Material.IRON_SWORD, "§4§l⚔ PRESIDENTIAL ARENA",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Learn about:",
                "§f• How to join arena",
                "§f• Battle rules",
                "§f• Rewards & killstreak",
                "§f• Leaderboard",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§aClick to open guide");
        inv.setItem(29, arenaHelp);

        // Treasury
        ItemStack treasuryHelp = createItem(Material.GOLD_BLOCK, "§6§l💰 TREASURY",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Learn about:",
                "§f• State income sources",
                "§f• Usage of state funds",
                "§f• How to donate",
                "§f• Financial transparency",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§aClick to open guide");
        inv.setItem(31, treasuryHelp);

        // Recall
        ItemStack recallHelp = createItem(Material.REDSTONE_TORCH, "§c§l⚠ RECALL PRESIDENT",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Learn about:",
                "§f• How to start recall",
                "§f• Petition process",
                "§f• Recall voting",
                "§f• Threshold & deposit",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§aClick to open guide");
        inv.setItem(33, recallHelp);

        // Command List
        ItemStack commandsItem = createItem(Material.COMMAND_BLOCK, "§d§l💻 COMMAND LIST",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7View all commands:",
                "§f/dc menu §8- Open main menu",
                "§f/dc vote §8- Vote candidate",
                "§f/dc register §8- Register candidate",
                "§f/dc help §8- Command help",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§aClick for full list");
        inv.setItem(40, commandsItem);

        // Back button
        ItemStack backItem = createItem(Material.ARROW, "§7§lBack", "§7Back to main menu");
        inv.setItem(36, backItem);

        // Close button
        ItemStack closeItem = createItem(Material.BARRIER, "§c§lClose", "§7Close menu");
        inv.setItem(44, closeItem);

        fillGlass(inv);
        player.openInventory(inv);
    }

    /**
     * Open election guide
     */
    public void openElectionHelp(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, HELP_ELECTION_TITLE);

        // Header
        ItemStack header = createItem(Material.PAPER, "§b§l🗳 ELECTION SYSTEM",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Election is the way to",
                "§7choose a new president",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(4, header);

        // Registration Phase
        ItemStack phase1 = createItem(Material.EMERALD, "§a§l1️⃣ REGISTRATION PHASE",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Duration: §f3 days",
                "",
                "§7In this phase players can:",
                "§f• Register as candidate",
                "§f• Give endorsements",
                "",
                "§7Candidate requirements:",
                "§f• Min level 100",
                "§f• Playtime 100+ hours",
                "§f• Balance 500,000+",
                "§f• 10+ endorsements",
                "§f• Registration fee 500,000",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(19, phase1);

        // Campaign Phase
        ItemStack phase2 = createItem(Material.GOLDEN_APPLE, "§6§l2️⃣ CAMPAIGN PHASE",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Duration: §f7 days",
                "",
                "§7In this phase candidates can:",
                "§f• Broadcast campaign messages",
                "§f• Collect endorsements",
                "§f• Build campaign points",
                "",
                "§7Broadcast cost: §f10,000",
                "§7Broadcast cooldown: §f6 hours",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(21, phase2);

        // Voting Phase
        ItemStack phase3 = createItem(Material.LIME_WOOL, "§a§l3️⃣ VOTING PHASE",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Duration: §f3 days",
                "",
                "§7In this phase players can:",
                "§f• Cast votes",
                "§f• Vote cannot be undone!",
                "",
                "§7Vote weight determined by:",
                "§f• Playtime (+0.5 if 200+ hours)",
                "§f• Level (+0.5 if 75+)",
                "§f• Balance (+0.5 if 1M+)",
                "§f• Max weight: 2.5x",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(23, phase3);

        // Inauguration Phase
        ItemStack phase4 = createItem(Material.GOLDEN_HELMET, "§e§l4️⃣ INAUGURATION PHASE",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Duration: §f1 day",
                "",
                "§7In this phase:",
                "§f• Winner announced",
                "§f• New president inaugurated",
                "§f• Rewards given",
                "",
                "§7Voter rewards:",
                "§f• 10,000 participation",
                "§f• 50,000 lottery (10 winners)",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(25, phase4);

        // Back button
        ItemStack backItem = createItem(Material.ARROW, "§7§lBack", "§7Back to help menu");
        inv.setItem(45, backItem);

        fillGlass(inv);
        player.openInventory(inv);
    }

    /**
     * Open president guide
     */
    public void openPresidentHelp(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, HELP_PRESIDENT_TITLE);

        // Header
        ItemStack header = createItem(Material.GOLDEN_HELMET, "§6§l👑 BECOMING PRESIDENT",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7President is the supreme",
                "§7leader of the country",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(4, header);

        // Powers
        ItemStack powers = createItem(Material.DIAMOND, "§b§lPRESIDENT POWERS",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7President can:",
                "§f• Issue Executive Orders",
                "§f• Appoint cabinet ministers",
                "§f• Fire cabinet ministers",
                "§f• Start Presidential Games",
                "§f• Use treasury funds",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(19, powers);

        // Buffs
        ItemStack buffs = createItem(Material.BEACON, "§a§lPRESIDENT BUFFS",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7President gets buffs:",
                "§f• +15% Damage",
                "§f• +12% Defense",
                "§f• +20% Vault earning",
                "§f• +10% XP gain",
                "§f• +2 Extra hearts",
                "§f• Hunger immunity",
                "§f• Night vision",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(21, buffs);

        // Daily Rewards
        ItemStack rewards = createItem(Material.CHEST, "§6§lDAILY REWARDS",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7President receives daily:",
                "§f• 50,000 Vault points",
                "§f• 5 Diamond blocks",
                "§f• 3 Netherite ingots",
                "§f• 10 Enchanted golden apples",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(23, rewards);

        // Term
        ItemStack term = createItem(Material.CLOCK, "§e§lTERM OF OFFICE",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Term information:",
                "§f• Duration: 30 days",
                "§f• Max consecutive: 2 terms",
                "§f• Cooldown: 1 term",
                "",
                "§7President can be recalled if:",
                "§f• Low approval rating",
                "§f• Inactive 7+ days",
                "§f• Recall petition successful",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(25, term);

        // Back button
        ItemStack backItem = createItem(Material.ARROW, "§7§lBack", "§7Back to help menu");
        inv.setItem(45, backItem);

        fillGlass(inv);
        player.openInventory(inv);
    }

    /**
     * Open cabinet guide
     */
    public void openCabinetHelp(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, HELP_CABINET_TITLE);

        // Header
        ItemStack header = createItem(Material.LECTERN, "§e§l📋 CABINET SYSTEM",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Cabinet is the president's",
                "§7helper team in running",
                "§7the government",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(4, header);

        // Defense
        ItemStack defense = createItem(Material.IRON_SWORD, "§4§lMINISTER OF DEFENSE",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Focus: §fMilitary & PvP",
                "",
                "§7Buffs received:",
                "§f• +12% Damage",
                "§f• +10% Defense",
                "§f• +15% Vault",
                "§f• +1.5 Extra hearts",
                "",
                "§7Daily salary: §630,000",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(20, defense);

        // Treasury
        ItemStack treasury = createItem(Material.GOLD_INGOT, "§6§lMINISTER OF FINANCE",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Focus: §fEconomy & Treasury",
                "",
                "§7Buffs received:",
                "§f• +25% Vault earning",
                "§f• +10% Sell bonus",
                "§f• -10% Buy discount",
                "",
                "§7Daily salary: §650,000",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(24, treasury);







        // Cabinet Decisions
        ItemStack decisions = createItem(Material.ENCHANTED_BOOK, "§d§lCABINET DECISIONS",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Each minister can",
                "§7issue decisions",
                "§7in their field",
                "",
                "§7Decision info:",
                "§f• Cost: 500,000",
                "§f• Cooldown: 48 hours",
                "§f• Duration: varies",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(31, decisions);

        // Back button
        ItemStack backItem = createItem(Material.ARROW, "§7§lBack", "§7Back to help menu");
        inv.setItem(45, backItem);

        fillGlass(inv);
        player.openInventory(inv);
    }

    /**
     * Open executive orders guide
     */
    public void openOrdersHelp(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, HELP_ORDERS_TITLE);

        // Header
        ItemStack header = createItem(Material.WRITABLE_BOOK, "§c§l📜 EXECUTIVE ORDERS",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Executive Orders are",
                "§7special decisions that can",
                "§7only be issued by president",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(4, header);

        // Info umum
        ItemStack info = createItem(Material.BOOK, "§e§lGENERAL INFO",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Cost: §61,000,000",
                "§7Cooldown: §f7 days",
                "§7Duration: §fVaries",
                "",
                "§7Orders affect the",
                "§7entire server!",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(22, info);

        // List orders as examples
        ItemStack order1 = createItem(Material.DIAMOND_SWORD, "§c§lMARTIAL LAW",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Effect: §fPvP damage +20%",
                "§7Duration: §f24 hours",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(19, order1);

        ItemStack order2 = createItem(Material.GOLD_BLOCK, "§6§lECONOMIC STIMULUS",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Effect: §fVault bonus +30%",
                "§7Duration: §f48 hours",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(21, order2);

        ItemStack order3 = createItem(Material.EXPERIENCE_BOTTLE, "§a§lEDUCATION REFORM",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Effect: §fXP gain +25%",
                "§7Duration: §f72 hours",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(23, order3);

        ItemStack order4 = createItem(Material.IRON_CHESTPLATE, "§7§lDEFENSE MOBILIZATION",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Effect: §fDefense +15%",
                "§7Duration: §f24 hours",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(25, order4);

        // Back button
        ItemStack backItem = createItem(Material.ARROW, "§7§lBack", "§7Back to help menu");
        inv.setItem(45, backItem);

        fillGlass(inv);
        player.openInventory(inv);
    }

    /**
     * Open arena guide
     */
    public void openArenaHelp(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, HELP_ARENA_TITLE);

        // Header
        ItemStack header = createItem(Material.IRON_SWORD, "§4§l⚔ PRESIDENTIAL ARENA",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7PvP Arena held by",
                "§7the President!",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(4, header);

        // How to Join
        ItemStack join = createItem(Material.LIME_WOOL, "§a§lHOW TO JOIN",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Use command:",
                "§f/dc arena join",
                "",
                "§7Or click button in menu",
                "§7when arena session is active",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(19, join);

        // Rules
        ItemStack rules = createItem(Material.BOOK, "§e§lARENA RULES",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§f• PvP enabled in arena area",
                "§f• Death penalty: 5,000",
                "§f• Safe zone immunity: 10 seconds",
                "§f• AFK kick: 3 minutes",
                "§f• Keep inventory ON",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(21, rules);

        // Rewards
        ItemStack rewards = createItem(Material.GOLD_INGOT, "§6§lKILLSTREAK REWARDS",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§75 kills: §610,000",
                "§710 kills: §625,000",
                "§725 kills: §6100,000",
                "§750 kills: §6250,000",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(23, rewards);

        // Leave
        ItemStack leave = createItem(Material.RED_WOOL, "§c§lLEAVE ARENA",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Use command:",
                "§f/dc arena leave",
                "",
                "§7Or leave the arena",
                "§7area manually",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(25, leave);

        // Back button
        ItemStack backItem = createItem(Material.ARROW, "§7§lBack", "§7Back to help menu");
        inv.setItem(45, backItem);

        fillGlass(inv);
        player.openInventory(inv);
    }

    /**
     * Open treasury guide
     */
    public void openTreasuryHelp(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, HELP_TREASURY_TITLE);

        // Header
        ItemStack header = createItem(Material.GOLD_BLOCK, "§6§l💰 STATE TREASURY",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Treasury is the state's",
                "§7financial funds",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(4, header);

        // Income Sources
        ItemStack income = createItem(Material.EMERALD, "§a§lINCOME SOURCES",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Treasury filled from:",
                "§f• Transaction tax (5%)",
                "§f• Candidate registration fee",
                "§f• Player donations",
                "§f• Event rewards",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(19, income);

        // Expenses
        ItemStack expense = createItem(Material.REDSTONE, "§c§lEXPENSES",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Treasury used for:",
                "§f• Executive Orders",
                "§f• Presidential Games",
                "§f• Cabinet Decisions",
                "§f• Minister salaries",
                "§f• President rewards",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(21, expense);

        // How to Donate
        ItemStack donate = createItem(Material.HOPPER, "§e§lHOW TO DONATE",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Use command:",
                "§f/dc treasury donate <amount>",
                "",
                "§7Your donation helps",
                "§7build the country!",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(23, donate);

        // Transparency
        ItemStack transparency = createItem(Material.GLASS, "§b§lTRANSPARENCY",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7All transactions recorded!",
                "",
                "§7View in Treasury menu:",
                "§f/dc treasury",
                "",
                "§7Or open from main menu",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(25, transparency);

        // Back button
        ItemStack backItem = createItem(Material.ARROW, "§7§lBack", "§7Back to help menu");
        inv.setItem(36, backItem);

        fillGlass(inv);
        player.openInventory(inv);
    }

    // === Utility Methods ===

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) {
            meta.setLore(Arrays.asList(lore));
        }
        item.setItemMeta(meta);
        return item;
    }

    private void fillGlass(Inventory inv) {
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, glass);
            }
        }
    }
}
