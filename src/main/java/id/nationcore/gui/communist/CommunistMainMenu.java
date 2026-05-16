package id.nationcore.gui.communist;

import id.nationcore.gui.GUIListener;
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
import id.nationcore.models.CommunistGovernment;
import id.nationcore.models.CommunistGovernment.PolitburoMember;
import id.nationcore.models.CommunistGovernment.PolitburoPosition;
import id.nationcore.models.Nation;
import id.nationcore.models.PlayerData;

/**
 * Main menu for COMMUNIST nations.
 *
 * 6-row layout (54 slots) — highlighting single-party elements:
 * Secretary General, Politburo, propaganda control & subsidies.
 *
 * <pre>
 * Row 0  [FILLER] [FILLER] [FILLER] [FILLER] [PROFILE] [FILLER] [FILLER] [FILLER] [FILLER]
 * Row 1  [FILLER] [POLICY] [CABINET] [ ] [SEKJEN] [ ] [MEMBERS] [TREASURY] [FILLER]
 * Row 2  [FILLER] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [FILLER]
 * Row 3  [FILLER] [NOTIFICATION] [ ] [ELECTION] [TAX] [AID] [ ] [GRAND EVENT] [FILLER]
 * Row 4  [FILLER] [GUIDE] [ ] [CAPITAL] [RESEARCH] [LAW] [ ] [HUB] [FILLER]
 * Row 5  [FILLER] [FILLER] [FILLER] [FILLER] [FILLER] [FILLER] [FILLER] [FILLER] [FILLER]
 * </pre>
 */
public class CommunistMainMenu extends NationMenuBase {

    public static final String TITLE = ChatColor.translateAlternateColorCodes('&',
            "&c&l☭ &4&lPolitburo Hall");

    // ── Slot constants ──────────────────────────────────────────────────────
    private static final int SLOT_NATION_PROFILE = 4; // Nation Profile (GLOW_ITEM_FRAME)
    private static final int SLOT_PARTY_POLICY = 10; // Party Policy / Executive Orders
    private static final int SLOT_CABINET = 11; // Cabinet Members
    private static final int SLOT_SEKJEN = 13; // Secretary General
    private static final int SLOT_PARTY_MEMBERS = 15; // Party Members
    private static final int SLOT_TREASURY = 16; // Party Treasury
    private static final int SLOT_NOTIFICATION = 28; // Notifications (coming soon)
    private static final int SLOT_SHARED_STORAGE = 30; // Shared Storage
    private static final int SLOT_TAX = 31; // National Tax
    private static final int SLOT_AID = 32; // National Aid (coming soon)
    private static final int SLOT_GRAND_EVENT = 34; // Grand Event (coming soon)
    private static final int SLOT_GUIDE = 37; // Communist Guide (coming soon)
    private static final int SLOT_CAPITAL = 39; // Capital (coming soon)
    private static final int SLOT_RESEARCH = 40; // National Research (coming soon)
    private static final int SLOT_LAW = 41; // National Law (coming soon)
    private static final int SLOT_HUB = 43; // Nation Hub

    // ── Filler slots ────────────────────────────────────────────────────────
    private static final int[] FILLER_SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8,
            9, 17, 18, 26, 27, 35, 36, 44,
            45, 46, 47, 48, 49, 50, 51, 52, 53
    };

    private static final Material PRIMARY_FILLER = Material.RED_STAINED_GLASS_PANE;

    public CommunistMainMenu(NationCore plugin) {
        super(plugin);
    }

    public void open(Player player, Nation nation) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // ── Place fillers ────────────────────────────────────────────────
        ItemStack filler = pane(PRIMARY_FILLER);
        for (int s : FILLER_SLOTS) {
            inv.setItem(s, filler);
        }

        CommunistGovernment cg = nation.getCommunistGovernment();

        // ── Row 0: Nation Profile ────────────────────────────────────────
        inv.setItem(SLOT_NATION_PROFILE, buildNationProfile(nation, cg));

        // ── Row 1: Core buttons ─────────────────────────────────────────
        inv.setItem(SLOT_PARTY_POLICY, buildPartyPolicyCard(cg));
        inv.setItem(SLOT_CABINET, buildCabinetCard(cg));
        inv.setItem(SLOT_SEKJEN, buildSekjenCard(cg, nation));
        inv.setItem(SLOT_PARTY_MEMBERS, buildPartyMembersCard(cg, nation));
        inv.setItem(SLOT_TREASURY, buildTreasuryCard(nation, cg));

        // ── Row 3: Secondary buttons ────────────────────────────────────
        inv.setItem(SLOT_NOTIFICATION, buildNotificationCard());
        inv.setItem(SLOT_SHARED_STORAGE, buildSharedStorageCard(nation));
        inv.setItem(SLOT_TAX, buildTaxCard(cg));
        inv.setItem(SLOT_AID, buildAidCard());
        inv.setItem(SLOT_GRAND_EVENT, buildGrandEventCard());

        // ── Row 4: Tertiary buttons ─────────────────────────────────────
        inv.setItem(SLOT_GUIDE, buildGuideCard());
        inv.setItem(SLOT_CAPITAL, buildCapitalCard());
        inv.setItem(SLOT_RESEARCH, buildResearchCard());
        inv.setItem(SLOT_LAW, buildLawCard());
        inv.setItem(SLOT_HUB, buildHubButton());

        // ── Fill remaining empty slots with filler ──────────────────────
        // fillEmptySlots(inv, filler); // Removed to keep interior slots as AIR

        player.openInventory(inv);
    }

    // ── Slot lookup (used by GUIListener) ───────────────────────────────────

    public static int getSlot(String key) {
        return switch (key) {
            case "NATION_PROFILE" -> SLOT_NATION_PROFILE;
            case "PARTY_POLICY" -> SLOT_PARTY_POLICY;
            case "CABINET" -> SLOT_CABINET;
            case "SEKJEN" -> SLOT_SEKJEN;
            case "PARTY_MEMBERS" -> SLOT_PARTY_MEMBERS;
            case "TREASURY" -> SLOT_TREASURY;
            case "NOTIFICATION" -> SLOT_NOTIFICATION;
            case "SHARED_STORAGE" -> SLOT_SHARED_STORAGE;
            case "TAX" -> SLOT_TAX;
            case "AID" -> SLOT_AID;
            case "GRAND_EVENT" -> SLOT_GRAND_EVENT;
            case "GUIDE" -> SLOT_GUIDE;
            case "CAPITAL" -> SLOT_CAPITAL;
            case "RESEARCH" -> SLOT_RESEARCH;
            case "LAW" -> SLOT_LAW;
            case "HUB" -> SLOT_HUB;
            default -> -1;
        };
    }

    public static boolean isFiller(Material m) {
        return m == PRIMARY_FILLER;
    }

    // ===================================================================
    // Item builders
    // ===================================================================

    // ── 1. Nation Profile (slot 4) — GLOW_ITEM_FRAME ────────────────────

    private ItemStack buildNationProfile(Nation nation, CommunistGovernment cg) {
        int memberCount = nation.getMemberCount();
        int partyCount = cg != null ? cg.getPartyMemberCount() : 0;

        return buildIcon(Material.GLOW_ITEM_FRAME,
                "&a&lNation Profile",
                "",
                "&8 ● &7name: &c&l" + nation.getName(),
                "&8 ● &7type: &cCommunist",
                "&8 ● &7tag: &8[&e" + nation.getTag() + "&8]",
                "&8 ● &7members: &f" + memberCount,
                "&8 ● &7party members: &f" + partyCount);
    }

    // ── 2. Party Policy (slot 10) — PALE_OAK_HANGING_SIGN ──────────────

    private ItemStack buildPartyPolicyCard(CommunistGovernment cg) {
        int activeFlags = 0;
        if (cg != null) {
            if (cg.isDefenseProtocolActive())
                activeFlags++;
            if (cg.isOffenseProtocolActive())
                activeFlags++;
            if (cg.isQuarantineActive())
                activeFlags++;
            if (cg.isPlagueActive())
                activeFlags++;
            if (cg.isMarketEventActive())
                activeFlags++;
            if (cg.isVaccinationActive())
                activeFlags++;
            if (cg.isMilitaryEmergencyActive())
                activeFlags++;
            if (cg.isGlorificationActive())
                activeFlags++;
            if (cg.isSensorMediaActive())
                activeFlags++;
        }

        return buildIcon(Material.PALE_OAK_HANGING_SIGN,
                "&5&lParty Policy",
                "",
                "&7View active executive orders",
                "&7issued by the party leadership.",
                "",
                "&8 ● &7active orders : &6" + activeFlags,
                "",
                "&cclick to open executive order menu!");
    }

    // ── 3. Cabinet Members (slot 11) — FIRE_CHARGE ─────────────────

    private ItemStack buildCabinetCard(CommunistGovernment cg) {
        int filled = cg != null ? cg.getPolitburo().size() : 0;
        int total = PolitburoPosition.values().length;

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7Government cabinet members");
        lore.add("&7appointed by the Secretary General.");
        lore.add("");
        lore.add("&7Positions Filled : &f" + filled + "&8/&f" + total);
        lore.add("");

        if (cg != null) {
            for (PolitburoPosition pos : PolitburoPosition.values()) {
                PolitburoMember m = cg.getPolitburoMember(pos);
                lore.add("&8 • &7" + pos.getDisplayName() + ": "
                        + (m != null ? "&a" + m.getName() : "&8(vacant)"));
            }
        }

        lore.add("&8");
        return buildIcon(Material.FIRE_CHARGE, 11,
                "&d&lCabinet Members", lore);
    }

    // ── 4. Secretary General (slot 13) — NETHER_STAR ────────────────────

    private ItemStack buildSekjenCard(CommunistGovernment cg, Nation nation) {
        if (cg == null || !cg.hasSecretaryGeneral()) {
            return buildIcon(Material.NETHER_STAR,
                    "&c&lSecretary General",
                    "",
                    "&7Status : &8Vacant",
                    "",
                    "&7No Secretary General has been",
                    "&7appointed yet. Party members may",
                    "&7nominate during the election cycle.",
                    "");
        }

        OfflinePlayer sekjen = Bukkit.getOfflinePlayer(cg.getSecretaryGeneralUUID());
        String name = cg.getSecretaryGeneralName() != null
                ? cg.getSecretaryGeneralName()
                : (sekjen.getName() != null ? sekjen.getName() : "Unknown");

        long sinceTerm = System.currentTimeMillis() - cg.getTermStartTime();

        int activeFlags = 0;
        if (cg.isDefenseProtocolActive())
            activeFlags++;
        if (cg.isOffenseProtocolActive())
            activeFlags++;
        if (cg.isQuarantineActive())
            activeFlags++;
        if (cg.isPlagueActive())
            activeFlags++;
        if (cg.isMarketEventActive())
            activeFlags++;
        if (cg.isVaccinationActive())
            activeFlags++;
        if (cg.isMilitaryEmergencyActive())
            activeFlags++;
        if (cg.isGlorificationActive())
            activeFlags++;
        if (cg.isSensorMediaActive())
            activeFlags++;

        return buildIcon(Material.NETHER_STAR,
                "&6&l" + name,
                "",
                "&8 ● &7title: &fSecretary General of " + nation.getName(),
                "&8 ● &7term: &f#" + cg.getConsecutiveTerms(),
                "&8 ● &7duration: &f" + formatRemaining(sinceTerm),
                "&8 ● &7policies: &f" + activeFlags + " active",
                "",
                "&cclick to open government menu!");
    }

    // ── 5. Party Members (slot 15) — FIREWORK_STAR ──────────────────────

    private ItemStack buildPartyMembersCard(CommunistGovernment cg, Nation nation) {
        int partyCount = cg != null ? cg.getPartyMemberCount() : 0;
        int total = nation.getMemberCount();
        int percent = total > 0 ? (int) Math.round(partyCount * 100.0 / total) : 0;

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7Registered permanent party members");
        lore.add("&7with full voting rights.");
        lore.add("");
        lore.add("&8 ● &7members: &f" + partyCount + " &8/ &f5 &7(&c" + percent + "%&7)");
        lore.add("&8 ● &7limit: &cmaximal 5 members");
        lore.add("");

        if (cg != null && cg.getPartyMemberCount() > 0) {
            lore.add("");
            int shown = 0;
            for (java.util.UUID memberId : cg.getPartyMembers()) {
                if (shown >= 8) {
                    lore.add("&8  ... and " + (partyCount - shown) + " more");
                    break;
                }
                OfflinePlayer op = Bukkit.getOfflinePlayer(memberId);
                String pName = op.getName() != null ? op.getName() : memberId.toString().substring(0, 8);
                lore.add("&8 ● &e" + pName);
                shown++;
            }
        }

        return buildIcon(Material.FIREWORK_STAR, 15,
                "&6&lParty Members", lore);
    }

    // ── 6. Party Treasury (slot 16) — BAMBOO_HANGING_SIGN ───────────────

    private ItemStack buildTreasuryCard(Nation nation, CommunistGovernment cg) {
        double balance = nation.getTreasury().getBalance();
        double subsidi = cg != null ? cg.getTotalSubsidyPayouts() : 0.0;

        return buildIcon(Material.BAMBOO_HANGING_SIGN,
                "&6&lParty Treasury",
                "",
                "&8 ● &7balance: &6$" + formatMoney(balance),
                "&8 ● &7total subsidies: &6$" + formatMoney(subsidi),
                "",
                "&cclick to open treasury menu!");
    }

    // ── 7. Notifications (slot 28) — BELL ───────────────────────────────

    private ItemStack buildNotificationCard() {
        return buildIcon(Material.BELL,
                "&e&lNotifications",
                "",
                "&7This feature is currently",
                "&7under development.",
                "",
                "&c⚠ Coming Soon",
                "");
    }

    // ── 8. Shared Storage (slot 30) — CHEST ─────────────────────────

    private ItemStack buildSharedStorageCard(Nation nation) {
        id.nationcore.models.SharedStorageData storage = nation.getSharedStorageData();
        int occupied = storage.getOccupiedSlotCount();

        return buildIcon(Material.CHEST,
                "&c&l☂ Shared Storage",
                "",
                "&7The communal storage of the nation.",
                "&7All members can freely deposit",
                "&7and withdraw items.",
                "",
                "&8 ● &7slots used: &f" + occupied + " &8/ &f"
                        + id.nationcore.models.SharedStorageData.STORAGE_SIZE,
                "",
                "&cClick &7→ Open Shared Storage");
    }

    // ── 9. National Tax (slot 31) — BUNDLE ──────────────────────────────

    private ItemStack buildTaxCard(CommunistGovernment cg) {
        long sincePhase = cg != null ? System.currentTimeMillis() - cg.getLastTaxPhase() : 0;

        return buildIcon(Material.BUNDLE,
                "&a&lNational Tax",
                "",
                "&7View tax information and",
                "&7your outstanding tax bills.",
                "",
                "&7Last Collection : &f" + (cg != null && cg.getLastTaxPhase() > 0
                        ? formatRemaining(sincePhase) + " ago"
                        : "&8never"),
                "",
                "&eClick &7→ Open Tax Menu");
    }

    // ── 10. National Aid (slot 32) — EXPERIENCE_BOTTLE ──────────────────

    private ItemStack buildAidCard() {
        return buildIcon(Material.EXPERIENCE_BOTTLE,
                "&a&lNational Aid",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&7This feature is currently",
                "&7under development.",
                "",
                "&c⚠ Coming Soon",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    // ── 11. Grand Event (slot 33) — GOLD_BLOCK ──────────────────────────

    private ItemStack buildGrandEventCard() {
        return buildIcon(Material.GOLD_BLOCK,
                "&6&lGrand Event",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&7This feature is currently",
                "&7under development.",
                "",
                "&c⚠ Coming Soon",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    // ── 12. Communist Guide (slot 37) — KNOWLEDGE_BOOK ──────────────────

    private ItemStack buildGuideCard() {
        return buildIcon(Material.KNOWLEDGE_BOOK,
                "&b&lCommunist Guide",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&7This feature is currently",
                "&7under development.",
                "",
                "&c⚠ Coming Soon",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    // ── 13. Capital (slot 39) — LODESTONE ───────────────────────────────

    private ItemStack buildCapitalCard() {
        return buildIcon(Material.LODESTONE,
                "&e&lCapital City",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&7This feature is currently",
                "&7under development.",
                "",
                "&c⚠ Coming Soon",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    // ── 14. National Research (slot 40) — ENCHANTING_TABLE ──────────────

    private ItemStack buildResearchCard() {
        return buildIcon(Material.ENCHANTING_TABLE,
                "&d&lNational Research",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&7Pursue research projects that",
                "&7benefit every nation member.",
                "",
                "&7Categories : &fEconomy, Technology, War",
                "&7Started by : &fNation Leader",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&eClick &7→ Open Research");
    }

    // ── 15. National Law (slot 41) — SPAWNER ────────────────────────────

    private ItemStack buildLawCard() {
        return buildIcon(Material.SPAWNER,
                "&4&lNational Law",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&7This feature is currently",
                "&7under development.",
                "",
                "&c⚠ Coming Soon",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    // ── 16. Nation Hub (slot 43) — SPECTRAL_ARROW ───────────────────────

    private ItemStack buildHubButton() {
        return buildIcon(Material.SPECTRAL_ARROW,
                "&e&l⚐ Nation Hub",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&7Browse all nations on the server",
                "&7and manage diplomacy.",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&eClick &7→ Open Nation Hub");
    }
}
