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
import id.nationcore.models.TaxRecord.PlayerTaxProfile;

/**
 * Main menu for CALIPHATE nations.
 *
 * 6-row layout (54 slots) following the user-supplied specification:
 * • LIME_STAINED_GLASS_PANE filler.
 * • Profile/Caliph/Treasury/Research are functional; the rest are
 * advisory displays or "coming soon" placeholders per spec.
 */
public class CaliphateMainMenu extends NationMenuBase {

    public static final String TITLE = ChatColor.translateAlternateColorCodes('&',
            "&2&l☪ &a&lCaliphate Court");

    // ── Slot constants (per spec) ────────────────────────────────────
    private static final int SLOT_NATION_PROFILE = 4; // PROFILE NATION
    private static final int SLOT_SHURA_COUNCIL = 10; // SHURA COUNCIL (display)
    private static final int SLOT_CALIPH = 13; // CALIPH (opens government)
    private static final int SLOT_STATE_SCHOLARS = 16; // STATE SCHOLARS (display)
    private static final int SLOT_CALIPH_ANNOUNCE = 28; // CALIPH ANNOUNCEMENT (coming soon)
    private static final int SLOT_HALL_OF_FAME = 30; // HALL OF FAME (coming soon)
    private static final int SLOT_TREASURY = 31; // TREASURY (opens caliphate treasury)
    private static final int SLOT_ZAKAT = 32; // ZAKAH (opens zakah menu)
    private static final int SLOT_GRAND_EVENT = 34; // GRAND EVENT (coming soon)
    private static final int SLOT_GUIDE_BOOK = 37; // GUIDE BOOK (coming soon)
    private static final int SLOT_CAPITAL_CITY = 39; // CAPITAL CITY (coming soon)
    private static final int SLOT_RESEARCH = 40; // RESEARCH (opens research)
    private static final int SLOT_SHARIA_LAW = 41; // SHARIA LAW (coming soon)
    private static final int SLOT_HUB = 43; // HUB

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
        inv.setItem(SLOT_CALIPH_ANNOUNCE, buildAnnouncementCard(nation));
        inv.setItem(SLOT_HALL_OF_FAME, buildComingSoon(Material.WRITABLE_BOOK, "&6&lHall of Fame"));
        inv.setItem(SLOT_TREASURY, buildTreasuryCard(nation, cg));
        inv.setItem(SLOT_ZAKAT, buildZakahCard(player));
        inv.setItem(SLOT_GRAND_EVENT, buildComingSoon(Material.CHEST, "&6&lGrand Event"));
        inv.setItem(SLOT_GUIDE_BOOK, buildComingSoon(Material.KNOWLEDGE_BOOK, "&b&lGuide Book"));
        inv.setItem(SLOT_CAPITAL_CITY, buildCapitalCard(player, nation));
        inv.setItem(SLOT_RESEARCH, buildResearchCard());
        inv.setItem(SLOT_SHARIA_LAW, buildComingSoon(Material.SPAWNER, "&2&lSharia Law"));
        inv.setItem(SLOT_HUB, buildHubButton());

        player.openInventory(inv);
    }

    // ── Slot lookup ────────────────────────────────────────────────────

    public static int getSlot(String key) {
        return switch (key) {
            case "NATION_PROFILE" -> SLOT_NATION_PROFILE;
            case "SHURA_COUNCIL" -> SLOT_SHURA_COUNCIL;
            case "CALIPH" -> SLOT_CALIPH;
            case "STATE_SCHOLARS" -> SLOT_STATE_SCHOLARS;
            case "CALIPH_ANNOUNCE" -> SLOT_CALIPH_ANNOUNCE;
            case "HALL_OF_FAME" -> SLOT_HALL_OF_FAME;
            case "TREASURY" -> SLOT_TREASURY;
            case "ZAKAT" -> SLOT_ZAKAT;
            case "GRAND_EVENT" -> SLOT_GRAND_EVENT;
            case "GUIDE_BOOK" -> SLOT_GUIDE_BOOK;
            case "CAPITAL_CITY" -> SLOT_CAPITAL_CITY;
            case "RESEARCH" -> SLOT_RESEARCH;
            case "SHARIA_LAW" -> SLOT_SHARIA_LAW;
            case "HUB" -> SLOT_HUB;
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

    private ItemStack buildZakahCard(Player player) {
        var profile = plugin.getTaxManager().getProfile(player.getUniqueId());
        int openInvoices = profile != null ? profile.getOutstandingInvoices().size() : 0;
        double due = profile != null ? profile.getOutstandingTotal() : 0;

        return buildIcon(Material.CHEST,
                "&a&lZakah",
                "",
                "&7Invoice-based zakah obligation for",
                "&7every citizen of the caliphate.",
                "&7Settle your bills before they double.",
                "",
                "&7Your Open Invoices : &f" + openInvoices,
                "&7Amount Due         : " + (due > 0 ? "&c$" + formatMoney(due) : "&a$0"),
                "",
                "&aClick &7→ Open the Zakah Office");
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

    private ItemStack buildCapitalCard(Player player, Nation nation) {
        if (!nation.hasCapital()) {
            return buildIcon(Material.LODESTONE,
                    "&e&lCapital City",
                    "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "&7Status: &cNot Claimed",
                    "&7The capital city of your nation",
                    "&7has not been established yet.",
                    "&7Use &e/nc capital claim &7in game.",
                    "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        }

        Nation.CapitalLocation cap = nation.getCapital();
        int chunkX = ((int) Math.floor(cap.getX())) >> 4;
        int chunkZ = ((int) Math.floor(cap.getZ())) >> 4;
        double midX = (chunkX * 16) + 8.5;
        double midZ = (chunkZ * 16) + 8.5;
        double y = cap.getY();

        List<String> lore = new ArrayList<>();
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("&7World: &f" + cap.getWorld());
        lore.add("&7Coordinates: &aX: " + (int) midX + " &7| &aY: " + (int) y + " &7| &aZ: " + (int) midZ);
        lore.add("&7Chunk: &f(" + chunkX + ", " + chunkZ + ")");
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("&eRequirements:");
        lore.add("&7• Fee: &6$100 &7(Vault)");
        lore.add("&7• Cooldown: &f15 minutes");
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        long now = System.currentTimeMillis();
        long cooldownMs = 15L * 60 * 1000;
        Long lastTeleport = plugin.getGUIListener().capitalTeleportCooldowns.get(player.getUniqueId());

        if (lastTeleport != null && (now - lastTeleport) < cooldownMs) {
            long remaining = cooldownMs - (now - lastTeleport);
            lore.add("&7Status: &cCOOLDOWN");
            lore.add("&7Available in: &e" + formatRemaining(remaining));
        } else {
            double balance = plugin.getVaultHook().getBalance(player.getUniqueId());
            if (balance < 100.0) {
                lore.add("&7Status: &cINSUFFICIENT FUNDS");
            } else {
                lore.add("&7Status: &aREADY");
                lore.add("&eClick to Teleport");
            }
        }
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        return buildIcon(Material.LODESTONE, "&e&lCapital City", lore);
    }

    private ItemStack buildAnnouncementCard(Nation nation) {
        String message = nation.getAnnouncementMessage();
        if (message == null || message.isBlank()) {
            return buildIcon(Material.BELL,
                    "&e&lAnnouncement",
                    "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "&7No announcement has been",
                    "&7posted yet.",
                    "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        }

        List<String> lore = new ArrayList<>();
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("&f\"&7" + message + "&f\"");
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        String creatorName = nation.getLeaderName();
        if (creatorName == null || creatorName.isBlank()) {
            creatorName = "The Caliph";
        }
        lore.add("&7Posted by: &e" + creatorName);

        String timeStr = new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm").format(new java.util.Date(nation.getAnnouncementCreatedAt()));
        lore.add("&7Posted at: &f" + timeStr);
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        return buildIcon(Material.BELL, "&e&lAnnouncement", lore);
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
