package id.nationcore.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;
import id.nationcore.listeners.ChatListener;
import id.nationcore.managers.TerritoryManager;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

/**
 * Shared foundation for every per-nation Border Management interface.
 *
 * One uniquely named subclass exists per government type (each living in its own
 * UI package). All rendering, navigation and security logic is centralized here
 * so the behaviour — and the safety guarantees — stay identical across Republic,
 * Communist, Monarchy and Caliphate. Subclasses only supply theming (filler
 * colour, accent colour, menu title) and the {@link GovernmentType} they bind to.
 *
 * The 54-slot layout is fixed by the specification:
 *   • Filler (per-nation stained glass) frames the menu.
 *   • Territory Overview (glow_item_frame) on slot 4.
 *   • Toggle Border (dark_oak_hanging_sign) on slot 20.
 *   • Welcome Message (writable_book) on slot 22.
 *   • Reallocate Capital (birch_hanging_sign) on slot 24.
 *   • Disband Territory (mangrove_hanging_sign) on slot 30.
 *   • Claim Territory (pale_oak_hanging_sign) on slot 32.
 *   • Back (spectral_arrow) on slot 49.
 */
public abstract class AbstractBorderMenu {

    protected final NationCore plugin;

    protected AbstractBorderMenu(NationCore plugin) {
        this.plugin = plugin;
    }

    // ── Theming hooks (implemented per nation) ──────────────────────────────

    /** Stained-glass-pane material used as filler, coloured per nation type. */
    protected abstract Material fillerMaterial();

    /** Accent colour code (e.g. "§9") used for headings. */
    protected abstract String accent();

    /** Unique inventory title for this menu. */
    public abstract String menuTitle();

    /** Government type this menu may only be operated by. */
    protected abstract GovernmentType expectedType();

    // ── Layout ──────────────────────────────────────────────────────────────

    private static final int[] FILLER = {
            0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 50, 51, 52, 53
    };
    private static final int INFO = 4;
    private static final int TOGGLE = 20;
    private static final int WELCOME = 22;
    private static final int REALLOCATE = 24;
    private static final int DISBAND = 30;
    private static final int CLAIM = 32;
    private static final int BACK = 49;

    private static final String DIVIDER = "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";

    // ── Rendering ─────────────────────────────────────────────────────────────

    public void open(Player player, Nation nation) {
        if (nation == null) return;

        Inventory inv = Bukkit.createInventory(null, 54, menuTitle());

        ItemStack filler = GovernmentGUIUtils.createItem(fillerMaterial(), " ");
        for (int slot : FILLER) {
            inv.setItem(slot, filler);
        }

        Location loc = player.getLocation();

        inv.setItem(INFO, buildOverview(nation));
        inv.setItem(TOGGLE, buildToggle(nation));
        inv.setItem(WELCOME, buildWelcome(nation));
        inv.setItem(REALLOCATE, buildReallocate(nation, loc));
        inv.setItem(DISBAND, buildDisband(nation, loc));
        inv.setItem(CLAIM, buildClaim(nation, loc));
        inv.setItem(BACK, GovernmentGUIUtils.createItem(Material.SPECTRAL_ARROW, "§c§lBack",
                DIVIDER,
                "§7Return to the Government menu.",
                DIVIDER));

        player.openInventory(inv);
    }

    // ── Click handling ────────────────────────────────────────────────────────

    public void handleClick(GUIListener gui, Player player, ItemStack clicked, int rawSlot, ClickType click) {
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == fillerMaterial()) return;

        Nation nation = guardNation(player);
        if (nation == null) return;

        switch (rawSlot) {
            case BACK -> {
                MessageUtils.playSound(player, Sound.UI_BUTTON_CLICK);
                gui.openGovernmentGUI(player);
            }
            case INFO -> { /* statistics display only */ }
            case TOGGLE -> handleToggle(player, nation);
            case WELCOME -> handleWelcome(player, nation, click);
            case CLAIM -> handleClaim(player, nation);
            case DISBAND -> handleDisband(player, nation);
            case REALLOCATE -> handleReallocate(player, nation);
            default -> { /* empty frame slot */ }
        }
    }

    private void handleToggle(Player player, Nation nation) {
        if (!plugin.getTerritoryManager().canManageTerritory(player, nation)) {
            denyLeaderOnly(player, "manage the border display");
            return;
        }
        MessageUtils.playSound(player, Sound.UI_BUTTON_CLICK);
        boolean now = !nation.isBorderVisible();
        nation.setBorderVisible(now);
        plugin.getDataManager().saveNations();
        if (now) {
            MessageUtils.send(player, "<green>Border display enabled — your territory is now visualized for everyone.</green>");
            MessageUtils.playSound(player, Sound.BLOCK_BEACON_ACTIVATE);
        } else {
            MessageUtils.send(player, "<yellow>Border display disabled.</yellow>");
            MessageUtils.playSound(player, Sound.BLOCK_BEACON_DEACTIVATE);
        }
        open(player, nation);
    }

    private void handleWelcome(Player player, Nation nation, ClickType click) {
        if (!plugin.getTerritoryManager().canManageTerritory(player, nation)) {
            denyLeaderOnly(player, "manage the welcome message");
            return;
        }
        MessageUtils.playSound(player, Sound.UI_BUTTON_CLICK);

        if (click != null && click.isRightClick()) {
            if (!nation.hasWelcomeMessage()) {
                MessageUtils.send(player, "<yellow>There is no welcome message to clear.</yellow>");
                return;
            }
            nation.setWelcomeMessage(null);
            plugin.getDataManager().saveNations();
            MessageUtils.send(player, "<green>Welcome message cleared.</green>");
            open(player, nation);
            return;
        }

        player.closeInventory();
        ChatListener.pendingWelcomeMessages.put(player.getUniqueId(), nation);
        MessageUtils.send(player, "");
        MessageUtils.send(player, "<gold>━━━━━━━━━━ [WELCOME MESSAGE] ━━━━━━━━━━</gold>");
        MessageUtils.send(player, "<yellow>Type the new welcome message in chat.</yellow>");
        MessageUtils.send(player, "<gray>You may use <white>&</white> colour codes. Type <white>cancel</white> to abort.</gray>");
        MessageUtils.send(player, "<gold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</gold>");
    }

    private void handleClaim(Player player, Nation nation) {
        MessageUtils.playSound(player, Sound.UI_BUTTON_CLICK);
        TerritoryManager.Result res = plugin.getTerritoryManager().claimTerritory(player, player.getLocation());
        sendResult(player, res);
        if (res.isSuccess()) MessageUtils.playSound(player, Sound.ENTITY_PLAYER_LEVELUP);
        else MessageUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS);
        open(player, nation);
    }

    private void handleDisband(Player player, Nation nation) {
        MessageUtils.playSound(player, Sound.UI_BUTTON_CLICK);
        TerritoryManager.Result res = plugin.getTerritoryManager().disbandTerritory(player, player.getLocation());
        sendResult(player, res);
        if (res.isSuccess()) MessageUtils.playSound(player, Sound.ENTITY_ITEM_BREAK);
        else MessageUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS);
        open(player, nation);
    }

    private void handleReallocate(Player player, Nation nation) {
        MessageUtils.playSound(player, Sound.UI_BUTTON_CLICK);
        TerritoryManager.Result res = plugin.getTerritoryManager().reallocateCapital(player, player.getLocation());
        sendResult(player, res);
        if (res.isSuccess()) MessageUtils.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE);
        else MessageUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS);
        open(player, nation);
    }

    // ── Security helpers ──────────────────────────────────────────────────────

    /**
     * Re-resolves the clicking player's nation and verifies it still matches the
     * government type this menu is bound to. Prevents acting on a stale inventory
     * after the player left/disbanded/changed nations.
     */
    private Nation guardNation(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null || nation.getType() != expectedType()) {
            MessageUtils.send(player, "<red>You can no longer access this border menu.</red>");
            player.closeInventory();
            return null;
        }
        return nation;
    }

    private void denyLeaderOnly(Player player, String what) {
        MessageUtils.send(player, "<red>Only the " + expectedType().getLeaderTitle()
                + " can " + what + ".</red>");
        MessageUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS);
    }

    private void sendResult(Player player, TerritoryManager.Result res) {
        MessageUtils.send(player, (res.isSuccess() ? "<green>" : "<red>")
                + res.getMessage() + (res.isSuccess() ? "</green>" : "</red>"));
    }

    // ── Item builders ─────────────────────────────────────────────────────────

    private ItemStack buildOverview(Nation nation) {
        List<String> lore = new ArrayList<>();
        lore.add(DIVIDER);
        lore.add("§7Nation      : §f" + nation.getName());
        lore.add("§7Government  : §f" + nation.getType().getDisplayName());
        lore.add("§7Tag         : §f[" + nation.getTag() + "]");
        lore.add("");
        lore.add("§7Claimed Chunks : §f" + nation.getTerritorySize());
        if (nation.hasCapital()) {
            Nation.CapitalLocation cap = nation.getCapital();
            int cx = ((int) Math.floor(cap.getX())) >> 4;
            int cz = ((int) Math.floor(cap.getZ())) >> 4;
            lore.add("§7Capital Chunk  : §f(" + cx + ", " + cz + ")");
            lore.add("§7Capital World  : §f" + cap.getWorld());
        } else {
            lore.add("§7Capital Chunk  : §cNot set");
        }
        lore.add("");
        lore.add("§7Border Display : " + (nation.isBorderVisible() ? "§aVisible" : "§cHidden"));
        lore.add("§7Welcome Message: " + (nation.hasWelcomeMessage() ? "§aSet" : "§8None"));
        lore.add(DIVIDER);
        lore.add("§8An overview of your nation's territory");

        return GovernmentGUIUtils.createItem(Material.GLOW_ITEM_FRAME,
                accent() + "§lTerritory Overview",
                lore.toArray(new String[0]));
    }

    private ItemStack buildToggle(Nation nation) {
        boolean on = nation.isBorderVisible();
        return GovernmentGUIUtils.createItem(Material.DARK_OAK_HANGING_SIGN,
                (on ? "§a§lBorder Display: ON" : "§c§lBorder Display: OFF"),
                DIVIDER,
                "§7Visualize every chunk your nation",
                "§7owns with a particle outline that",
                "§7all players can see in the world.",
                "",
                "§7Capital chunks are highlighted with",
                "§7a distinct, eye-catching effect.",
                "",
                "§7Status : " + (on ? "§aEnabled" : "§cDisabled"),
                DIVIDER,
                "§eClick to " + (on ? "disable" : "enable") + ".");
    }

    private ItemStack buildWelcome(Nation nation) {
        List<String> lore = new ArrayList<>();
        lore.add(DIVIDER);
        lore.add("§7Shown to every player — member or");
        lore.add("§7not — who enters your territory.");
        lore.add("");
        if (nation.hasWelcomeMessage()) {
            lore.add("§7Current message:");
            for (String line : wrap("§f", ChatColor.translateAlternateColorCodes('&', nation.getWelcomeMessage()), 34)) {
                lore.add(line);
            }
        } else {
            lore.add("§7Current message: §8(none set)");
        }
        lore.add("");
        lore.add("§eLeft-Click §7to set a new message.");
        lore.add("§eRight-Click §7to clear it.");
        lore.add(DIVIDER);

        return GovernmentGUIUtils.createItem(Material.WRITABLE_BOOK,
                "§e§lWelcome Message",
                lore.toArray(new String[0]));
    }

    private ItemStack buildReallocate(Nation nation, Location loc) {
        double cost = plugin.getConfig().getDouble("nation.territory.capital-relocate-cost", 50000);
        double cooldownHours = plugin.getConfig().getDouble("nation.territory.capital-relocate-cooldown-hours", 24);

        List<String> lore = new ArrayList<>();
        lore.add(DIVIDER);
        lore.add("§7Move your capital to the chunk you");
        lore.add("§7are currently standing in.");
        lore.add("");
        lore.add("§7Requirements:");
        lore.add("§8• §7Chunk must be owned by your nation");
        lore.add("§8• §7Cost: §6$" + money(cost));
        lore.add("§8• §7Cooldown: §f" + trim(cooldownHours) + "h");
        lore.add("");
        lore.add("§7Your Chunk : §f" + chunkLabel(loc));
        lore.add("§7Status     : " + standingStatus(nation, loc));
        lore.add("§7Cooldown   : " + relocateCooldownStatus(nation));
        lore.add(DIVIDER);
        lore.add("§eClick to relocate the capital here.");

        return GovernmentGUIUtils.createItem(Material.BIRCH_HANGING_SIGN,
                "§6§lReallocate Capital",
                lore.toArray(new String[0]));
    }

    private ItemStack buildDisband(Nation nation, Location loc) {
        return GovernmentGUIUtils.createItem(Material.MANGROVE_HANGING_SIGN,
                "§c§lDisband Territory",
                DIVIDER,
                "§7Release the chunk you are standing",
                "§7in from your nation's territory.",
                "",
                "§c⚠ The capital chunk cannot be released.",
                "§7Relocate the capital first if needed.",
                "",
                "§7Your Chunk : §f" + chunkLabel(loc),
                "§7Status     : " + standingStatus(nation, loc),
                DIVIDER,
                "§eClick to release this chunk.");
    }

    private ItemStack buildClaim(Nation nation, Location loc) {
        double cost = plugin.getConfig().getDouble("nation.territory.claim-cost", 25000);

        List<String> lore = new ArrayList<>();
        lore.add(DIVIDER);
        lore.add("§7Claim the chunk you are standing");
        lore.add("§7in as part of your nation.");
        lore.add("");
        lore.add("§7Requirements:");
        lore.add("§8• §7Cost: §6$" + money(cost));
        lore.add("§8• §7Must border your existing land");
        lore.add("§8• §7Chunk must be unclaimed");
        lore.add("");
        lore.add("§7Your Chunk : §f" + chunkLabel(loc));
        lore.add("§7Biome      : §f" + biomeName(loc));
        lore.add("§7Ownership  : " + ownershipStatus(nation, loc));
        lore.add(DIVIDER);
        lore.add("§eClick to claim this chunk.");

        return GovernmentGUIUtils.createItem(Material.PALE_OAK_HANGING_SIGN,
                "§a§lClaim Territory",
                lore.toArray(new String[0]));
    }

    // ── Lore helpers ────────────────────────────────────────────────────────

    private String chunkLabel(Location loc) {
        if (loc == null || loc.getWorld() == null) return "Unknown";
        return "(" + (loc.getBlockX() >> 4) + ", " + (loc.getBlockZ() >> 4) + ")";
    }

    /** Status of the player's current chunk relative to their OWN nation. */
    private String standingStatus(Nation nation, Location loc) {
        if (loc == null || loc.getWorld() == null) return "§7Unknown";
        TerritoryManager tm = plugin.getTerritoryManager();
        String world = loc.getWorld().getName();
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;

        Nation owner = tm.getNationAt(loc);
        if (owner == null) return "§7Unclaimed";
        if (owner.getId().equals(nation.getId())) {
            return tm.isCapitalChunk(nation, world, cx, cz) ? "§6Your Capital" : "§aYour Territory";
        }
        return "§cClaimed by " + owner.getName();
    }

    /** Ownership of the player's current chunk from anyone's perspective. */
    private String ownershipStatus(Nation nation, Location loc) {
        if (loc == null || loc.getWorld() == null) return "§7Unknown";
        Nation owner = plugin.getTerritoryManager().getNationAt(loc);
        if (owner == null) return "§aUnclaimed";
        if (owner.getId().equals(nation.getId())) return "§eAlready yours";
        return "§cClaimed by " + owner.getName();
    }

    private String relocateCooldownStatus(Nation nation) {
        long cooldownMs = (long) (plugin.getConfig()
                .getDouble("nation.territory.capital-relocate-cooldown-hours", 24) * 3_600_000L);
        long since = System.currentTimeMillis() - nation.getLastCapitalRelocateAt();
        if (nation.getLastCapitalRelocateAt() <= 0 || since >= cooldownMs) {
            return "§aReady";
        }
        return "§c" + MessageUtils.formatTime(cooldownMs - since);
    }

    private String biomeName(Location loc) {
        if (loc == null) return "Unknown";
        try {
            return prettify(loc.getBlock().getBiome().getKey().getKey());
        } catch (Throwable t) {
            return "Unknown";
        }
    }

    private String prettify(String raw) {
        if (raw == null || raw.isEmpty()) return "Unknown";
        StringBuilder sb = new StringBuilder();
        for (String w : raw.replace(':', '_').split("_")) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0)))
              .append(w.substring(1).toLowerCase())
              .append(' ');
        }
        return sb.length() == 0 ? "Unknown" : sb.toString().trim();
    }

    private String money(double value) {
        return String.format("%,.0f", value);
    }

    /** Drops the trailing ".0" from whole-number hour values for tidy lore. */
    private String trim(double value) {
        if (value == Math.floor(value)) return String.valueOf((long) value);
        return String.valueOf(value);
    }

    /** Greedy word-wrap that keeps each lore line below {@code maxLen} chars. */
    private List<String> wrap(String prefix, String text, int maxLen) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() > 0 && line.length() + word.length() + 1 > maxLen) {
                out.add(prefix + line);
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) out.add(prefix + line);
        return out;
    }
}
