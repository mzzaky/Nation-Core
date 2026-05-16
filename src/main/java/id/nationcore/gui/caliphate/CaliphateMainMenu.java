package id.nationcore.gui.caliphate;

import id.nationcore.gui.NationMenuBase;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import id.nationcore.NationCore;
import id.nationcore.models.CaliphateGovernment;
import id.nationcore.models.Nation;

/**
 * Main menu for CALIPHATE nations.
 *
 * 6-row layout (54 slots) following the user-supplied specification:
 *   • LIME_STAINED_GLASS_PANE filler.
 *   • Profile/Caliph/Treasury/Research are functional; the rest are
 *     advisory displays or "coming soon" placeholders per spec.
 */
public class CaliphateMainMenu extends NationMenuBase {

    public static final String TITLE = ChatColor.translateAlternateColorCodes('&',
            "&2&l☪ &a&lCaliphate Court");

    // ── Slot constants (per spec) ────────────────────────────────────
    private static final int SLOT_NATION_PROFILE  = 4;   // PROFILE NATION
    private static final int SLOT_SHURA_COUNCIL   = 10;  // SHURA COUNCIL (display)
    private static final int SLOT_CALIPH          = 13;  // CALIPH (opens government)
    private static final int SLOT_STATE_SCHOLARS  = 16;  // STATE SCHOLARS (display)
    private static final int SLOT_CALIPH_ANNOUNCE = 28;  // CALIPH ANNOUNCEMENT (coming soon)
    private static final int SLOT_HALL_OF_FAME    = 30;  // HALL OF FAME (coming soon)
    private static final int SLOT_TREASURY        = 31;  // TREASURY (opens caliphate treasury)
    private static final int SLOT_ZAKAT           = 32;  // ZAKAT (coming soon)
    private static final int SLOT_GRAND_EVENT     = 34;  // GRAND EVENT (coming soon)
    private static final int SLOT_GUIDE_BOOK      = 37;  // GUIDE BOOK (coming soon)
    private static final int SLOT_CAPITAL_CITY    = 39;  // CAPITAL CITY (coming soon)
    private static final int SLOT_RESEARCH        = 40;  // RESEARCH (opens research)
    private static final int SLOT_SHARIA_LAW      = 41;  // SHARIA LAW (coming soon)
    private static final int SLOT_HUB             = 43;  // HUB

    // ── Filler slots (per spec) ──────────────────────────────────────
    private static final int[] FILLER_SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8,
            9, 17, 18, 26, 27, 35, 36, 44,
            45, 46, 47, 48, 49, 50, 51, 52, 53
    };

    private static final Material PRIMARY_FILLER = Material.LIME_STAINED_GLASS_PANE;

    public CaliphateMainMenu(NationCore plugin) {
        super(plugin);
    }

    public void open(Player player, Nation nation) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        ItemStack filler = pane(PRIMARY_FILLER);
        for (int s : FILLER_SLOTS) {
            inv.setItem(s, filler);
        }

        CaliphateGovernment cg = nation.getCaliphateGovernment();

        inv.setItem(SLOT_NATION_PROFILE, buildNationProfile(nation, cg));
        inv.setItem(SLOT_SHURA_COUNCIL, buildShuraCouncilCard(cg));
        inv.setItem(SLOT_CALIPH, buildCaliphCard(nation, cg));
        inv.setItem(SLOT_STATE_SCHOLARS, buildStateScholarsCard(cg));
        inv.setItem(SLOT_CALIPH_ANNOUNCE, buildComingSoon(Material.BELL, "&e&lCaliph Announcement"));
        inv.setItem(SLOT_HALL_OF_FAME, buildComingSoon(Material.WRITABLE_BOOK, "&6&lHall of Fame"));
        inv.setItem(SLOT_TREASURY, buildTreasuryCard(nation, cg));
        inv.setItem(SLOT_ZAKAT, buildComingSoon(Material.CHEST, "&a&lZakat"));
        inv.setItem(SLOT_GRAND_EVENT, buildComingSoon(Material.CHEST, "&6&lGrand Event"));
        inv.setItem(SLOT_GUIDE_BOOK, buildComingSoon(Material.KNOWLEDGE_BOOK, "&b&lGuide Book"));
        inv.setItem(SLOT_CAPITAL_CITY, buildComingSoon(Material.LODESTONE, "&e&lCapital City"));
        inv.setItem(SLOT_RESEARCH, buildResearchCard());
        inv.setItem(SLOT_SHARIA_LAW, buildComingSoon(Material.SPAWNER, "&2&lSharia Law"));
        inv.setItem(SLOT_HUB, buildHubButton());

        player.openInventory(inv);
    }

    // ── Slot lookup ────────────────────────────────────────────────────

    public static int getSlot(String key) {
        return switch (key) {
            case "NATION_PROFILE"   -> SLOT_NATION_PROFILE;
            case "SHURA_COUNCIL"    -> SLOT_SHURA_COUNCIL;
            case "CALIPH"           -> SLOT_CALIPH;
            case "STATE_SCHOLARS"   -> SLOT_STATE_SCHOLARS;
            case "CALIPH_ANNOUNCE"  -> SLOT_CALIPH_ANNOUNCE;
            case "HALL_OF_FAME"     -> SLOT_HALL_OF_FAME;
            case "TREASURY"         -> SLOT_TREASURY;
            case "ZAKAT"            -> SLOT_ZAKAT;
            case "GRAND_EVENT"      -> SLOT_GRAND_EVENT;
            case "GUIDE_BOOK"       -> SLOT_GUIDE_BOOK;
            case "CAPITAL_CITY"     -> SLOT_CAPITAL_CITY;
            case "RESEARCH"         -> SLOT_RESEARCH;
            case "SHARIA_LAW"       -> SLOT_SHARIA_LAW;
            case "HUB"              -> SLOT_HUB;
            default -> -1;
        };
    }

    public static boolean isFiller(Material m) {
        return m == PRIMARY_FILLER;
    }

    // ===================================================================
    // Builders
    // ===================================================================

    private ItemStack buildNationProfile(Nation nation, CaliphateGovernment cg) {
        int memberCount = nation.getMemberCount();
        int shuraCount = cg != null ? cg.getShuraCount() : 0;
        int scholarsCount = cg != null ? cg.getScholarCount() : 0;

        return buildIcon(Material.GLOW_ITEM_FRAME,
                "&2&l" + nation.getName(),
                "",
                "&7Government     : &fCaliphate",
                "&7Tag            : &f[" + nation.getTag() + "]",
                "&7Citizens       : &f" + memberCount,
                "&7Shura Council  : &f" + shuraCount + "&8/&f" + CaliphateGovernment.MAX_SHURA,
                "&7State Scholars : &f" + scholarsCount + "&8/&f" + CaliphateGovernment.MAX_SCHOLARS,
                "",
                "&8displays information about the caliphate");
    }

    private ItemStack buildShuraCouncilCard(CaliphateGovernment cg) {
        int filled = cg != null ? cg.getShuraCount() : 0;

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7Advisory body to the Caliph,");
        lore.add("&7sharing counsel on state matters.");
        lore.add("");
        lore.add("&7Seats Filled : &f" + filled + "&8/&f" + CaliphateGovernment.MAX_SHURA);
        lore.add("");
        if (cg != null && filled > 0) {
            for (UUID memberId : cg.getShuraCouncil()) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(memberId);
                String pName = op.getName() != null ? op.getName() : memberId.toString().substring(0, 8);
                lore.add("&7• &a" + pName);
            }
        } else {
            lore.add("&8(no members appointed)");
        }
        lore.add("");
        lore.add("&8Display only — no command authority.");

        ItemStack item = buildIcon(Material.EMERALD, "&a&lShura Council", lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildCaliphCard(Nation nation, CaliphateGovernment cg) {
        if (cg == null || !cg.hasCaliph()) {
            return buildIcon(Material.TURTLE_HELMET,
                    "&c&lCaliph",
                    "",
                    "&7Status : &8Vacant seat",
                    "",
                    "&7No Caliph has been invested yet. An admin",
                    "&7must invest the next Caliph.",
                    "");
        }
        OfflinePlayer caliph = Bukkit.getOfflinePlayer(cg.getCaliphUUID());
        String name = cg.getCaliphName() != null
                ? cg.getCaliphName()
                : (caliph.getName() != null ? caliph.getName() : "Unknown");
        long sinceAscension = System.currentTimeMillis() - cg.getAscensionTime();

        return buildIcon(Material.TURTLE_HELMET,
                "&2&l" + name,
                "",
                "&7Title    : &fCaliph of " + nation.getName(),
                "&7Reign    : &flifelong",
                "&7Invested : &f" + formatRemaining(sinceAscension) + " ago",
                "",
                "&aClick &7→ Open the Caliphate Government Menu");
    }

    private ItemStack buildStateScholarsCard(CaliphateGovernment cg) {
        int filled = cg != null ? cg.getScholarCount() : 0;

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7Religious & legal scholars who");
        lore.add("&7advise the Caliph on Sharia matters.");
        lore.add("");
        lore.add("&7Seats Filled : &f" + filled + "&8/&f" + CaliphateGovernment.MAX_SCHOLARS);
        lore.add("");
        if (cg != null && filled > 0) {
            for (UUID memberId : cg.getStateScholars()) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(memberId);
                String pName = op.getName() != null ? op.getName() : memberId.toString().substring(0, 8);
                lore.add("&7• &b" + pName);
            }
        } else {
            lore.add("&8(no scholars ordained)");
        }
        lore.add("");
        lore.add("&8Display only — no command authority.");

        ItemStack item = buildIcon(Material.ENDER_PEARL, "&b&lState Scholars", lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildTreasuryCard(Nation nation, CaliphateGovernment cg) {
        double balance = nation.getTreasury().getBalance();
        double subsidi = cg != null ? cg.getTotalSubsidyPayouts() : 0.0;

        Material mat;
        try {
            mat = Material.valueOf("LIME_BUNDLE");
        } catch (IllegalArgumentException e) {
            mat = Material.BUNDLE;
        }

        return buildIcon(mat,
                "&a&lBayt al-Mal",
                "",
                "&7Current Balance : &a$" + formatMoney(balance),
                "&7Total Zakat     : &a$" + formatMoney(subsidi),
                "&7Source          : &fJizya tax & charitable income",
                "",
                "&aClick &7→ Open the Caliphate Treasury");
    }

    private ItemStack buildResearchCard() {
        return buildIcon(Material.ENCHANTING_TABLE,
                "&b&lCaliphate Research",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&7Pursue scholarly research that",
                "&7benefits every citizen of the caliphate.",
                "",
                "&7Categories : &fEconomy, Technology, War",
                "&7Started by : &fThe Caliph",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&aClick &7→ Open Research");
    }

    private ItemStack buildHubButton() {
        return buildIcon(Material.SPECTRAL_ARROW,
                "&e&l⚐ Nation Hub",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&7Browse all nations on the server",
                "&7and manage diplomacy.",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&eClick &7→ Open Nation Hub");
    }

    private ItemStack buildComingSoon(Material material, String title) {
        return buildIcon(material, title,
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&7This feature is currently",
                "&7under development.",
                "",
                "&c⚠ Coming Soon",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
}
