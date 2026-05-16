package id.nationcore.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;
import id.nationcore.managers.ResearchManager;
import id.nationcore.models.Nation;
import id.nationcore.models.NationResearchData;
import id.nationcore.models.NationResearchData.ActiveResearch;
import id.nationcore.models.ResearchCategory;
import id.nationcore.models.ResearchType;
import id.nationcore.utils.MessageUtils;

/**
 * GUI surface for the Nation Research feature.
 *
 * Three layered menus, all 54 slots so they share the same border treatment:
 * 1. Main Menu — era selection (I-V) + overview + active project banner
 * 2. Era Research Menu — list of research within an era, switchable by category
 * 3. Detail Menu — full breakdown of a single research with Start/Cancel
 *
 * Menu titles double as routing keys decoded by {@link GUIListener}; the
 * era/category and detail titles embed enough information for the listener
 * to decode without a separate state map.
 */
public class ResearchGUI {

    public static final String MAIN_TITLE = ChatColor.translateAlternateColorCodes('&',
            "&5&l⚛ &d&lNation Research");
    public static final String ERA_TITLE_PREFIX = ChatColor.translateAlternateColorCodes('&',
            "&5&l⚛ &d&lEra: ");
    public static final String DETAIL_TITLE_PREFIX = ChatColor.translateAlternateColorCodes('&',
            "&5&l⚛ &d&lProject: ");

    /** Separator embedded in era research titles between era and category. */
    private static final String ERA_TITLE_SEP = " — ";

    // ── Main menu slots ────────────────────────────────────────────────
    public static final int SLOT_ACTIVE = 4;
    public static final int SLOT_ERA_I = 20;
    public static final int SLOT_ERA_II = 30;
    public static final int SLOT_ERA_III = 22;
    public static final int SLOT_ERA_IV = 32;
    public static final int SLOT_ERA_V = 24;
    public static final int SLOT_OVERVIEW = 37;
    public static final int SLOT_BACK = 43;
    public static final int SLOT_CANCEL_ACTIVE = 49;

    private static final int[] MAIN_FILLER_SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36,
            44, 45, 46, 47, 48, 50, 51, 52, 53
    };

    // ── Era research menu slots ────────────────────────────────────────
    public static final int ERA_SLOT_ACTIVE = 4;
    public static final int ERA_SLOT_BACK = 43;
    public static final int ERA_SLOT_NAV_WAR = 48;
    public static final int ERA_SLOT_NAV_TECHNOLOGY = 49;
    public static final int ERA_SLOT_NAV_ECONOMY = 50;

    private static final int[] ERA_FILLER_SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36,
            44, 45, 46, 47, 51, 52, 53
    };

    /** Slots that host the actual research entries inside the era research menu. */
    private static final int[] ERA_LIST_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42
    };

    // ── Detail menu slots (unchanged) ──────────────────────────────────
    public static final int DETAIL_SLOT_INFO = 13;
    public static final int DETAIL_SLOT_LEVEL = 11;
    public static final int DETAIL_SLOT_NEXT = 15;
    public static final int DETAIL_SLOT_START = 31;
    public static final int DETAIL_SLOT_BACK = 49;

    /** Filler used across every research GUI screen. Listener mirrors this. */
    public static final Material FILLER = Material.RED_STAINED_GLASS_PANE;

    private final NationCore plugin;

    public ResearchGUI(NationCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Research eras. Only {@link #BASIC} is implemented in this build; the
     * remaining eras render as locked "coming soon" placeholders.
     */
    public enum ResearchEra {
        BASIC("I", "Basic", "&a"),
        ADVANCED("II", "Advanced", "&b"),
        EXPANSION("III", "Expansion", "&e"),
        MODERN("IV", "Modern", "&6"),
        FUTURISTIC("V", "Futuristic", "&d");

        private final String numeral;
        private final String label;
        private final String colorCode;

        ResearchEra(String numeral, String label, String colorCode) {
            this.numeral = numeral;
            this.label = label;
            this.colorCode = colorCode;
        }

        public String getNumeral() { return numeral; }
        public String getLabel() { return label; }
        public String getColorCode() { return colorCode; }

        /** Combined display label, e.g. {@code "I - Basic"}. */
        public String getFullName() {
            return numeral + " - " + label;
        }

        public boolean isAvailable() {
            return this == BASIC;
        }

        public static ResearchEra fromLabel(String label) {
            if (label == null) return null;
            for (ResearchEra e : values()) {
                if (e.label.equalsIgnoreCase(label)) return e;
            }
            return null;
        }
    }

    /**
     * Resolves which era a research type belongs to. Until the additional eras
     * are built out, the suffix on the enum constant is the source of truth:
     *   *_I  → BASIC, *_II → ADVANCED, ... and so on.
     */
    public static ResearchEra getEraOf(ResearchType type) {
        if (type == null) return ResearchEra.BASIC;
        String name = type.name();
        if (name.endsWith("_V"))   return ResearchEra.FUTURISTIC;
        if (name.endsWith("_IV"))  return ResearchEra.MODERN;
        if (name.endsWith("_III")) return ResearchEra.EXPANSION;
        if (name.endsWith("_II"))  return ResearchEra.ADVANCED;
        return ResearchEra.BASIC;
    }

    // =================================================================
    // Main menu — era selection
    // =================================================================

    public void openMain(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) {
            MessageUtils.send(player, "<red>You must belong to a nation to access the research lab.</red>");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, MAIN_TITLE);
        fillSlots(inv, MAIN_FILLER_SLOTS, FILLER);

        inv.setItem(SLOT_ACTIVE, buildActiveIcon(nation));
        inv.setItem(SLOT_OVERVIEW, buildOverviewIcon(nation));
        inv.setItem(SLOT_BACK, buildBackToNationIcon());

        if (nation.getResearchData().hasActive()
                && plugin.getResearchManager().isLeader(nation, player.getUniqueId())) {
            inv.setItem(SLOT_CANCEL_ACTIVE, buildCancelIcon());
        } else {
            inv.setItem(SLOT_CANCEL_ACTIVE, simple(FILLER, " "));
        }

        inv.setItem(SLOT_ERA_I,   buildEraIcon(nation, ResearchEra.BASIC));
        inv.setItem(SLOT_ERA_II,  buildEraIcon(nation, ResearchEra.ADVANCED));
        inv.setItem(SLOT_ERA_III, buildEraIcon(nation, ResearchEra.EXPANSION));
        inv.setItem(SLOT_ERA_IV,  buildEraIcon(nation, ResearchEra.MODERN));
        inv.setItem(SLOT_ERA_V,   buildEraIcon(nation, ResearchEra.FUTURISTIC));

        player.openInventory(inv);
    }

    // =================================================================
    // Era research menu — list filtered by era + category
    // =================================================================

    /** Convenience: open the default Technology category for the given era. */
    public void openEra(Player player, ResearchEra era) {
        openEra(player, era, ResearchCategory.TECHNOLOGY);
    }

    public void openEra(Player player, ResearchEra era, ResearchCategory category) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) {
            MessageUtils.send(player, "<red>You must belong to a nation to access the research lab.</red>");
            return;
        }
        if (era == null) {
            openMain(player);
            return;
        }
        if (!era.isAvailable()) {
            MessageUtils.send(player, "<gray>Era " + era.getFullName() + " is not yet available.</gray>");
            openMain(player);
            return;
        }
        if (category == null) {
            category = ResearchCategory.TECHNOLOGY;
        }

        String title = color(ERA_TITLE_PREFIX + era.getColorCode() + era.getFullName()
                + "&d" + ERA_TITLE_SEP
                + category.getColorCode() + category.getDisplayName());
        Inventory inv = Bukkit.createInventory(null, 54, title);
        fillSlots(inv, ERA_FILLER_SLOTS, FILLER);

        inv.setItem(ERA_SLOT_ACTIVE, buildActiveIcon(nation));
        inv.setItem(ERA_SLOT_BACK, simple(Material.SPECTRAL_ARROW, "&e&l← Back",
                "&7Return to era selection."));

        inv.setItem(ERA_SLOT_NAV_WAR,
                buildNavButton(Material.FLETCHING_TABLE, ResearchCategory.WAR, category));
        inv.setItem(ERA_SLOT_NAV_TECHNOLOGY,
                buildNavButton(Material.ENCHANTING_TABLE, ResearchCategory.TECHNOLOGY, category));
        inv.setItem(ERA_SLOT_NAV_ECONOMY,
                buildNavButton(Material.CARTOGRAPHY_TABLE, ResearchCategory.ECONOMY, category));

        // Populate research entries for this era + category in the empty area.
        int idx = 0;
        for (ResearchType type : ResearchType.values()) {
            if (type.getCategory() != category) continue;
            if (getEraOf(type) != era) continue;
            if (idx >= ERA_LIST_SLOTS.length) break;
            inv.setItem(ERA_LIST_SLOTS[idx++], buildResearchSummary(nation, type));
        }

        player.openInventory(inv);
    }

    // =================================================================
    // Detail menu (kept identical to previous behavior)
    // =================================================================

    public void openDetail(Player player, ResearchType type) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null || type == null) {
            openMain(player);
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54,
                color(DETAIL_TITLE_PREFIX + type.getCategory().getColorCode() + type.getDisplayName()));
        fill(inv, FILLER);

        ResearchManager rm = plugin.getResearchManager();
        int currentLevel = rm.getLevel(nation, type);
        int maxLevel = rm.getMaxLevel(type);

        inv.setItem(DETAIL_SLOT_INFO, buildResearchSummary(nation, type));
        inv.setItem(DETAIL_SLOT_LEVEL, buildLevelCard(currentLevel, maxLevel));

        if (currentLevel < maxLevel) {
            int nextLevel = currentLevel + 1;
            double cost = rm.getCost(type, nextLevel);
            long durationMs = rm.getDurationMillis(type, nextLevel);
            inv.setItem(DETAIL_SLOT_NEXT, buildNextLevelCard(type, nextLevel, cost, durationMs));
            inv.setItem(DETAIL_SLOT_START, buildStartButton(player, nation, type, cost));
        } else {
            inv.setItem(DETAIL_SLOT_NEXT, simple(Material.ENCHANTED_BOOK,
                    "&6&l✦ Maxed Out",
                    "&7This research has reached its maximum level."));
        }

        inv.setItem(DETAIL_SLOT_BACK, simple(Material.ARROW, "&e&l← Back",
                "&7Return to " + type.getCategory().getDisplayName() + " category."));
        player.openInventory(inv);
    }

    // =================================================================
    // Item builders
    // =================================================================

    private ItemStack buildOverviewIcon(Nation nation) {
        NationResearchData data = nation.getResearchData();
        int totalLevels = 0;
        int unlocked = 0;
        for (ResearchType type : ResearchType.values()) {
            int lv = data.getLevel(type);
            totalLevels += lv;
            if (lv > 0)
                unlocked++;
        }

        List<String> lore = new ArrayList<>();
        lore.add(color(""));
        lore.add(color("&7Nation       : &f" + nation.getName()));
        lore.add(color("&7Total Levels : &f" + totalLevels));
        lore.add(color("&7Researched   : &f" + unlocked + "&8/&f" + ResearchType.values().length));
        lore.add(color("&7Completed    : &f" + data.getTotalProjectsCompleted()));
        lore.add(color("&7Vault Spent  : &6$" + MessageUtils.formatNumber(data.getTotalVaultSpent())));
        lore.add(color(""));
        lore.add(color("&8Research benefits all nation members."));

        return buildItem(Material.WRITABLE_BOOK,
                color("&d&l⚛ Research Overview"), lore);
    }

    private ItemStack buildActiveIcon(Nation nation) {
        NationResearchData data = nation.getResearchData();
        if (!data.hasActive()) {
            return buildItem(Material.BREWING_STAND,
                    color("&7&lNo Active Research"),
                    color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"),
                    color("&7Your nation is not currently"),
                    color("&7running any research project."),
                    "",
                    color("&7Pick an era below to start one."),
                    color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        }

        ActiveResearch active = data.getActive();
        ResearchType type = active.getType();
        long remaining = active.getRemainingMillis();
        double pct = active.getProgressPercent();

        List<String> lore = new ArrayList<>();
        lore.add(color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        lore.add(color("&7Project    : &f" + (type != null ? type.getDisplayName() : active.getTypeId())));
        lore.add(color("&7Target Lvl : &f" + active.getTargetLevel()));
        lore.add(color("&7Started By : &f" + active.getStartedByName()));
        lore.add(color("&7Time Left  : &f" + formatDuration(remaining)));
        lore.add(color("&7Progress   : &a" + buildProgressBar(pct) + " &f" + String.format("%.1f%%", pct)));
        lore.add(color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        return buildItem(Material.BREWING_STAND,
                color("&a&l⏳ Active Research"), lore);
    }

    private ItemStack buildBackToNationIcon() {
        return buildItem(Material.SPECTRAL_ARROW,
                color("&e&l← Back to Nation"),
                color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"),
                color("&7Return to your nation's main menu."),
                color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
    }

    private ItemStack buildCancelIcon() {
        return buildItem(Material.RED_DYE,
                color("&c&l✖ Cancel Research"),
                color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"),
                color("&7Cancel the active research project."),
                color("&7A " + (int) plugin.getResearchManager().getCancelRefundPercent()
                        + "% refund will return to the treasury."),
                "",
                color("&eClick to cancel"),
                color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
    }

    private ItemStack buildEraIcon(Nation nation, ResearchEra era) {
        if (!era.isAvailable()) {
            return buildItem(Material.ENCHANTED_BOOK,
                    color(era.getColorCode() + "&lEra " + era.getFullName()),
                    color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"),
                    color("&7Status : &c&lComing Soon"),
                    color("&7This era is not yet available."),
                    color("&7Stay tuned for future updates."),
                    color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        }

        ResearchManager rm = plugin.getResearchManager();
        int total = 0;
        int leveled = 0;
        for (ResearchType type : ResearchType.values()) {
            if (getEraOf(type) != era) continue;
            total++;
            if (rm.getLevel(nation, type) > 0)
                leveled++;
        }

        return buildItem(Material.ENCHANTED_BOOK,
                color(era.getColorCode() + "&lEra " + era.getFullName()),
                color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"),
                color("&7Status     : &aAvailable"),
                color("&7Researched : &f" + leveled + "&8/&f" + total),
                "",
                color("&7Browse research projects from"),
                color("&7this era across all categories."),
                color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"),
                color("&eClick to open"));
    }

    private ItemStack buildNavButton(Material mat, ResearchCategory target, ResearchCategory current) {
        boolean active = target == current;
        String prefix = active ? "&a&l▶ " : "&7";
        List<String> lore = new ArrayList<>();
        lore.add(color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        lore.add(color("&7" + target.getDescription()));
        lore.add("");
        lore.add(active
                ? color("&aCurrently viewing")
                : color("&eClick to view"));
        lore.add(color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        return buildItem(mat,
                color(prefix + target.getColorCode() + target.getDisplayName()),
                lore);
    }

    private ItemStack buildResearchSummary(Nation nation, ResearchType type) {
        ResearchManager rm = plugin.getResearchManager();
        int level = rm.getLevel(nation, type);
        int maxLevel = rm.getMaxLevel(type);
        boolean isActive = nation.getResearchData().hasActive()
                && type.getId().equals(nation.getResearchData().getActive().getTypeId());

        List<String> lore = new ArrayList<>();
        lore.add(color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        lore.add(color("&7Category : " + type.getCategory().getColorCode() + type.getCategory().getDisplayName()));
        lore.add(color("&7Level    : &f" + level + "&8/&f" + maxLevel));
        if (level > 0) {
            lore.add(color("&7Effect   : &a" + describeEffect(type, level)));
        }
        lore.add("");
        lore.add(color("&7" + type.getDescription()));
        lore.add("");
        if (isActive) {
            ActiveResearch a = nation.getResearchData().getActive();
            lore.add(color("&a⏳ In progress: &f" + formatDuration(a.getRemainingMillis()) + " left"));
        } else if (level >= maxLevel) {
            lore.add(color("&6✦ Max level reached"));
        } else {
            int next = level + 1;
            lore.add(color("&7Next Cost : &6$" + MessageUtils.formatNumber(rm.getCost(type, next))));
            lore.add(color("&7Next Time : &f" + formatDuration(rm.getDurationMillis(type, next))));
        }
        lore.add(color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        lore.add(color("&eClick for details"));

        return buildItem(type.getIcon(),
                color(type.getCategory().getColorCode() + "&l" + type.getDisplayName()), lore);
    }

    private ItemStack buildLevelCard(int level, int maxLevel) {
        StringBuilder bar = new StringBuilder();
        for (int i = 1; i <= maxLevel; i++) {
            bar.append(i <= level ? color("&a■ ") : color("&8■ "));
        }
        return buildItem(Material.EXPERIENCE_BOTTLE,
                color("&b&lCurrent Level"),
                color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"),
                color("&7Level : &f" + level + "&8/&f" + maxLevel),
                color("&7Tier  : " + bar.toString().trim()),
                color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
    }

    private ItemStack buildNextLevelCard(ResearchType type, int nextLevel, double cost, long duration) {
        return buildItem(Material.PAPER,
                color("&e&lNext Level: " + nextLevel),
                color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"),
                color("&7Cost     : &6$" + MessageUtils.formatNumber(cost)),
                color("&7Duration : &f" + formatDuration(duration)),
                color("&7Result   : &a" + describeEffect(type, nextLevel)),
                color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
    }

    private ItemStack buildStartButton(Player player, Nation nation, ResearchType type, double cost) {
        ResearchManager rm = plugin.getResearchManager();
        boolean isLeader = rm.isLeader(nation, player.getUniqueId())
                || player.hasPermission("nation.admin");
        boolean hasActive = nation.getResearchData().hasActive();
        boolean canAfford = nation.getTreasury().getBalance() >= cost;

        if (!isLeader) {
            return buildItem(Material.BARRIER,
                    color("&c&l🔒 Leader Only"),
                    color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"),
                    color("&7Only the nation leader can start"),
                    color("&7a research project."),
                    color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        }
        if (hasActive) {
            return buildItem(Material.BARRIER,
                    color("&c&l⛔ Slot Occupied"),
                    color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"),
                    color("&7Another research project is"),
                    color("&7already in progress."),
                    color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        }
        if (!canAfford) {
            return buildItem(Material.BARRIER,
                    color("&c&l💰 Insufficient Funds"),
                    color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"),
                    color("&7The treasury cannot afford this."),
                    color("&7Required : &6$" + MessageUtils.formatNumber(cost)),
                    color("&7Treasury : &6$" + MessageUtils.formatNumber(nation.getTreasury().getBalance())),
                    color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        }
        return buildItem(Material.LIME_CONCRETE,
                color("&a&l▶ Start Research"),
                color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"),
                color("&7Withdraws &6$" + MessageUtils.formatNumber(cost)),
                color("&7from the national treasury."),
                "",
                color("&eClick to confirm"),
                color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
    }

    // =================================================================
    // Routing helpers — used by GUIListener to decode menu titles
    // =================================================================

    /** Result of decoding an era research menu title. */
    public static final class EraTitleInfo {
        public final ResearchEra era;
        public final ResearchCategory category;
        public EraTitleInfo(ResearchEra era, ResearchCategory category) {
            this.era = era;
            this.category = category;
        }
    }

    /**
     * Decodes the title of an era research menu into its era and category.
     * Returns {@code null} when the title is not an era research title or
     * cannot be parsed.
     */
    public static EraTitleInfo parseEraTitle(String title) {
        if (title == null) return null;
        String stripped = ChatColor.stripColor(title);
        String prefixStripped = ChatColor.stripColor(ERA_TITLE_PREFIX);
        if (!stripped.startsWith(prefixStripped)) return null;

        String rest = stripped.substring(prefixStripped.length()).trim();
        String sep = ERA_TITLE_SEP.trim();
        int sepIdx = rest.indexOf(sep);
        if (sepIdx < 0) return null;

        String eraPart = rest.substring(0, sepIdx).trim();
        String catPart = rest.substring(sepIdx + sep.length()).trim();

        ResearchEra era = null;
        // eraPart looks like "I - Basic"
        int dash = eraPart.indexOf(" - ");
        String eraLabel = dash >= 0 ? eraPart.substring(dash + 3).trim() : eraPart;
        era = ResearchEra.fromLabel(eraLabel);

        ResearchCategory cat = null;
        for (ResearchCategory c : ResearchCategory.values()) {
            if (catPart.equalsIgnoreCase(c.getDisplayName())
                    || catPart.equalsIgnoreCase(c.name())) {
                cat = c;
                break;
            }
        }
        if (era == null || cat == null) return null;
        return new EraTitleInfo(era, cat);
    }

    public static ResearchType parseTypeFromTitle(String title) {
        if (title == null)
            return null;
        String stripped = ChatColor.stripColor(title);
        String prefixStripped = ChatColor.stripColor(DETAIL_TITLE_PREFIX);
        if (!stripped.startsWith(prefixStripped))
            return null;

        String rest = stripped.substring(prefixStripped.length()).trim();
        for (ResearchType t : ResearchType.values()) {
            if (rest.equalsIgnoreCase(t.getDisplayName()))
                return t;
        }
        return null;
    }

    public static boolean isResearchTitle(String title) {
        if (title == null)
            return false;
        return title.equals(MAIN_TITLE)
                || title.startsWith(ERA_TITLE_PREFIX)
                || title.startsWith(DETAIL_TITLE_PREFIX);
    }

    // =================================================================
    // Utilities
    // =================================================================

    private void fill(Inventory inv, Material mat) {
        ItemStack pane = simple(mat, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, pane);
        }
    }

    private void fillSlots(Inventory inv, int[] slots, Material mat) {
        ItemStack pane = simple(mat, " ");
        for (int s : slots) {
            inv.setItem(s, pane);
        }
    }

    private ItemStack simple(Material mat, String name, String... lore) {
        return buildItem(mat, color(name), toLore(lore));
    }

    private List<String> toLore(String[] lines) {
        if (lines == null || lines.length == 0)
            return new ArrayList<>();
        List<String> out = new ArrayList<>(lines.length);
        for (String line : lines)
            out.add(color(line));
        return out;
    }

    private static ItemStack buildItem(Material material, String displayName, String... loreLines) {
        ItemStack item = new ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            if (loreLines != null && loreLines.length > 0) {
                List<String> lore = new ArrayList<>(loreLines.length);
                for (String line : loreLines)
                    lore.add(line);
                meta.setLore(lore);
            }
            for (org.bukkit.inventory.ItemFlag flag : org.bukkit.inventory.ItemFlag.values()) {
                meta.addItemFlags(flag);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack buildItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            if (lore != null)
                meta.setLore(lore);
            for (org.bukkit.inventory.ItemFlag flag : org.bukkit.inventory.ItemFlag.values()) {
                meta.addItemFlags(flag);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }

    private String formatDuration(long ms) {
        if (ms <= 0)
            return "finished";
        long total = ms / 1000;
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        if (h > 0)
            return h + "h " + m + "m";
        if (m > 0)
            return m + "m " + s + "s";
        return s + "s";
    }

    private String buildProgressBar(double percent) {
        int filled = (int) Math.round(percent / 10.0);
        if (filled < 0)
            filled = 0;
        if (filled > 10)
            filled = 10;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(i < filled ? "█" : "░");
        }
        return sb.toString();
    }

    private String describeEffect(ResearchType type, int level) {
        double effect = level * plugin.getResearchManager().getEffectPerLevel(type);
        return switch (type) {
            case AGRICULTURE_PROGRAM_I, HUSBANDRY_BOOST_I, MINING_LUCK_I ->
                String.format("%.0f%% chance for 1–2 bonus drops", effect * 100);
            case MAGIC_EDUCATION_I ->
                String.format("+%.0f%% XP from all sources", effect * 100);
            case HEALTH_EXPANSION_I ->
                String.format("+%.1f max HP (%.1f hearts)", effect, effect / 2.0);
            case HUNTING_SKILL_I ->
                String.format("+%.0f%% damage to hostile mobs", effect * 100);
            case ATTACK_EXERCISE_I ->
                String.format("+%.0f%% damage to enemy players", effect * 100);
            case DEFENSIVE_TACTIC_I ->
                String.format("-%.0f%% damage from enemy players", effect * 100);
            case ARCHERY_TRAINING_I ->
                String.format("+%.0f%% bow & crossbow damage", effect * 100);
        };
    }
}
