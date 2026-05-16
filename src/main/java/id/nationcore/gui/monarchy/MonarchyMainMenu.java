package id.nationcore.gui.monarchy;

import id.nationcore.gui.NationMenuBase;
import java.util.ArrayList;
import java.util.List;

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
import id.nationcore.models.MonarchyGovernment;
import id.nationcore.models.MonarchyGovernment.HighCouncilMember;
import id.nationcore.models.MonarchyGovernment.HighCouncilPosition;
import id.nationcore.models.Nation;

/**
 * Main menu for MONARCHY nations.
 *
 * 6-row layout (54 slots) — highlighting the absolute monarchy:
 * the King, the High Council, and royal services.
 */
public class MonarchyMainMenu extends NationMenuBase {

    public static final String TITLE = ChatColor.translateAlternateColorCodes('&',
            "&e&l👑 &6&lRoyal Court");

    // ── Slot constants (per spec) ────────────────────────────────────
    private static final int SLOT_NATION_PROFILE = 4;   // PROFILE NATION
    private static final int SLOT_HIGH_COUNCIL   = 10;  // HIGH COUNCIL (display)
    private static final int SLOT_KING           = 13;  // KING (opens government)
    private static final int SLOT_ROYAL_SOLDIER  = 16;  // ROYAL SOLDIER (display)
    private static final int SLOT_ROYAL_ANNOUNCE = 28;  // ROYAL ANNOUNCEMENT (coming soon)
    private static final int SLOT_HALL_OF_FAME   = 30;  // HALL OF FAME (coming soon)
    private static final int SLOT_TREASURY       = 31;  // ROYAL TREASURY (opens treasury)
    private static final int SLOT_TAX            = 32;  // TAX (opens tax menu)
    private static final int SLOT_GRAND_EVENT    = 34;  // GRAND EVENT (coming soon)
    private static final int SLOT_GUIDE_BOOK     = 37;  // GUIDE BOOK (coming soon)
    private static final int SLOT_CAPITAL_CITY   = 39;  // CAPITAL CITY (coming soon)
    private static final int SLOT_RESEARCH       = 40;  // RESEARCH (opens research)
    private static final int SLOT_MONARCH_LAW    = 41;  // MONARCH LAW (coming soon)
    private static final int SLOT_HUB            = 43;  // HUB

    // ── Filler slots (per spec) ──────────────────────────────────────
    private static final int[] FILLER_SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8,
            9, 17, 18, 26, 27, 35, 36, 44,
            45, 46, 47, 48, 50, 51, 52, 53
    };

    private static final Material PRIMARY_FILLER = Material.YELLOW_STAINED_GLASS_PANE;

    public MonarchyMainMenu(NationCore plugin) {
        super(plugin);
    }

    public void open(Player player, Nation nation) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        ItemStack filler = pane(PRIMARY_FILLER);
        for (int s : FILLER_SLOTS) {
            inv.setItem(s, filler);
        }

        MonarchyGovernment mg = nation.getMonarchyGovernment();

        inv.setItem(SLOT_NATION_PROFILE, buildNationProfile(nation, mg));
        inv.setItem(SLOT_HIGH_COUNCIL, buildHighCouncilCard(mg));
        inv.setItem(SLOT_KING, buildKingCard(nation, mg));
        inv.setItem(SLOT_ROYAL_SOLDIER, buildRoyalSoldierCard(nation, mg));
        inv.setItem(SLOT_ROYAL_ANNOUNCE, buildComingSoon(Material.BELL, "&e&lRoyal Announcement"));
        inv.setItem(SLOT_HALL_OF_FAME, buildComingSoon(Material.WRITABLE_BOOK, "&6&lHall of Fame"));
        inv.setItem(SLOT_TREASURY, buildTreasuryCard(nation, mg));
        inv.setItem(SLOT_TAX, buildTaxCard());
        inv.setItem(SLOT_GRAND_EVENT, buildComingSoon(Material.RAW_GOLD, "&6&lGrand Event"));
        inv.setItem(SLOT_GUIDE_BOOK, buildComingSoon(Material.KNOWLEDGE_BOOK, "&b&lGuide Book"));
        inv.setItem(SLOT_CAPITAL_CITY, buildComingSoon(Material.LODESTONE, "&e&lCapital City"));
        inv.setItem(SLOT_RESEARCH, buildResearchCard());
        inv.setItem(SLOT_MONARCH_LAW, buildComingSoon(Material.SPAWNER, "&6&lMonarch Law"));
        inv.setItem(SLOT_HUB, buildHubButton());

        player.openInventory(inv);
    }

    // ── Slot lookup ────────────────────────────────────────────────────

    public static int getSlot(String key) {
        return switch (key) {
            case "NATION_PROFILE"    -> SLOT_NATION_PROFILE;
            case "HIGH_COUNCIL"      -> SLOT_HIGH_COUNCIL;
            case "KING"              -> SLOT_KING;
            case "ROYAL_SOLDIER"     -> SLOT_ROYAL_SOLDIER;
            case "ROYAL_ANNOUNCE"    -> SLOT_ROYAL_ANNOUNCE;
            case "HALL_OF_FAME"      -> SLOT_HALL_OF_FAME;
            case "TREASURY"          -> SLOT_TREASURY;
            case "TAX"               -> SLOT_TAX;
            case "GRAND_EVENT"       -> SLOT_GRAND_EVENT;
            case "GUIDE_BOOK"        -> SLOT_GUIDE_BOOK;
            case "CAPITAL_CITY"      -> SLOT_CAPITAL_CITY;
            case "RESEARCH"          -> SLOT_RESEARCH;
            case "MONARCH_LAW"       -> SLOT_MONARCH_LAW;
            case "HUB"               -> SLOT_HUB;
            default -> -1;
        };
    }

    public static boolean isFiller(Material m) {
        return m == PRIMARY_FILLER;
    }

    // ===================================================================
    // Builders
    // ===================================================================

    private ItemStack buildNationProfile(Nation nation, MonarchyGovernment mg) {
        int memberCount = nation.getMemberCount();
        int soldierCount = mg != null ? mg.getRoyalSoldierCount() : 0;

        return buildIcon(Material.GLOW_ITEM_FRAME,
                "&e&l" + nation.getName(),
                "",
                "&7Government : &fMonarchy",
                "&7Tag        : &f[" + nation.getTag() + "]",
                "&7Subjects   : &f" + memberCount,
                "&7Soldiers   : &f" + soldierCount,
                "",
                "&8displays information about the nation");
    }

    private ItemStack buildHighCouncilCard(MonarchyGovernment mg) {
        int filled = mg != null ? mg.getHighCouncil().size() : 0;
        int total = HighCouncilPosition.values().length;

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7Members of the High Council");
        lore.add("&7appointed by the King.");
        lore.add("");
        lore.add("&7Seats Filled : &f" + filled + "&8/&f" + total);
        lore.add("");
        if (mg != null) {
            for (HighCouncilPosition pos : HighCouncilPosition.values()) {
                HighCouncilMember m = mg.getCouncilMember(pos);
                lore.add("&7• &f" + pos.getDisplayName() + " : "
                        + (m != null ? "&a" + m.getName() : "&8(vacant)"));
            }
        }

        ItemStack item = buildIcon(Material.GOLDEN_HELMET, "&6&lHigh Council", lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildKingCard(Nation nation, MonarchyGovernment mg) {
        if (mg == null || !mg.hasKing()) {
            return buildIcon(Material.GOLDEN_HORSE_ARMOR,
                    "&c&lKing",
                    "",
                    "&7Status : &8Vacant throne",
                    "",
                    "&7No King has been crowned yet. An admin",
                    "&7must invest the next monarch.",
                    "");
        }
        OfflinePlayer king = Bukkit.getOfflinePlayer(mg.getKingUUID());
        String name = mg.getKingName() != null
                ? mg.getKingName()
                : (king.getName() != null ? king.getName() : "Unknown");
        long sinceCoronation = System.currentTimeMillis() - mg.getCoronationTime();

        return buildIcon(Material.GOLDEN_HORSE_ARMOR,
                "&6&l" + name,
                "",
                "&7Title    : &fKing of " + nation.getName(),
                "&7Reign    : &flifelong",
                "&7Crowned  : &f" + formatRemaining(sinceCoronation) + " ago",
                "",
                "&eClick &7→ Open Royal Government Menu");
    }

    private ItemStack buildRoyalSoldierCard(Nation nation, MonarchyGovernment mg) {
        int soldiers = mg != null ? mg.getRoyalSoldierCount() : 0;
        int total = nation.getMemberCount();
        int percent = total > 0 ? (int) Math.round(soldiers * 100.0 / total) : 0;

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7Royal Soldiers sworn to defend");
        lore.add("&7the Crown.");
        lore.add("");
        lore.add("&7Soldiers : &f" + soldiers + " &8/ &f" + total + " &7(&6" + percent + "%&7)");
        lore.add("");
        if (mg != null && soldiers > 0) {
            int shown = 0;
            for (java.util.UUID memberId : mg.getRoyalSoldiers()) {
                if (shown >= 8) {
                    lore.add("&8  ... and " + (soldiers - shown) + " more");
                    break;
                }
                OfflinePlayer op = Bukkit.getOfflinePlayer(memberId);
                String pName = op.getName() != null ? op.getName() : memberId.toString().substring(0, 8);
                lore.add("&7• &f" + pName);
                shown++;
            }
        }

        ItemStack item = buildIcon(Material.GOLDEN_SWORD, "&6&lRoyal Soldier", lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildTreasuryCard(Nation nation, MonarchyGovernment mg) {
        double balance = nation.getTreasury().getBalance();
        double subsidi = mg != null ? mg.getTotalSubsidyPayouts() : 0.0;

        Material mat;
        try {
            mat = Material.valueOf("YELLOW_BUNDLE");
        } catch (IllegalArgumentException e) {
            mat = Material.BUNDLE;
        }

        return buildIcon(mat,
                "&6&lRoyal Treasury",
                "",
                "&7Current Balance : &6$" + formatMoney(balance),
                "&7Total Subsidies : &6$" + formatMoney(subsidi),
                "&7Source          : &fRoyal tax & royal grants",
                "",
                "&eClick &7→ Open Royal Treasury");
    }

    private ItemStack buildTaxCard() {
        return buildIcon(Material.CHEST,
                "&a&lRoyal Tax",
                "",
                "&7View tax information and",
                "&7your outstanding tax bills.",
                "",
                "&eClick &7→ Open Tax Menu");
    }

    private ItemStack buildResearchCard() {
        return buildIcon(Material.ENCHANTING_TABLE,
                "&d&lRoyal Research",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&7Pursue research projects that",
                "&7benefit every member of the kingdom.",
                "",
                "&7Categories : &fEconomy, Technology, War",
                "&7Started by : &fThe King",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&eClick &7→ Open Research");
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
