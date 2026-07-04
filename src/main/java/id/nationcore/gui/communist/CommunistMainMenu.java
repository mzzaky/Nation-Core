package id.nationcore.gui.communist;

import id.nationcore.gui.NationMenuBase;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;
import id.nationcore.models.CommunistGovernment;
import id.nationcore.models.CommunistGovernment.PolitburoMember;
import id.nationcore.models.CommunistGovernment.PolitburoPosition;
import id.nationcore.models.Nation;
import id.nationcore.models.ExecutiveOrder;

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
@SuppressWarnings("deprecation")
public class CommunistMainMenu extends NationMenuBase {

    public static final String TITLE = "§c§l☭ §4§lPolitburo Hall";

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
        inv.setItem(SLOT_PARTY_POLICY, buildPartyPolicyCard(nation, cg));
        inv.setItem(SLOT_CABINET, buildCabinetCard(cg));
        inv.setItem(SLOT_SEKJEN, buildSekjenCard(cg, nation));
        inv.setItem(SLOT_PARTY_MEMBERS, buildPartyMembersCard(cg, nation));
        inv.setItem(SLOT_TREASURY, buildTreasuryCard(nation, cg));

        // ── Row 3: Secondary buttons ────────────────────────────────────
        inv.setItem(SLOT_NOTIFICATION, buildNotificationCard(nation));
        inv.setItem(SLOT_SHARED_STORAGE, buildSharedStorageCard(nation));
        inv.setItem(SLOT_TAX, buildTaxCard(player));
        inv.setItem(SLOT_AID, buildAidCard());
        inv.setItem(SLOT_GRAND_EVENT, buildGrandEventCard());

        // ── Row 4: Tertiary buttons ─────────────────────────────────────
        inv.setItem(SLOT_GUIDE, buildGuideCard());
        inv.setItem(SLOT_CAPITAL, buildCapitalCard(player, nation));
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
        int realCount = nation.getRealMemberCount();
        int fakeCount = nation.getFakeMemberCount();
        int partyCount = cg != null ? cg.getPartyMemberCount() : 0;
        double balance = nation.getTreasury().getBalance();

        return buildIcon(Material.GLOW_ITEM_FRAME,
                "&c&l☭ &4&l" + nation.getName(),
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&7Explore the status, resources, and",
                "&7structure of your sovereign territory.",
                "",
                "&c&lSYSTEM PROFILE",
                "&8• &7Government: &fCommunist",
                "&8• &7Tag: &c[" + nation.getTag() + "]",
                "&8• &7Capital: &f" + (nation.hasCapital() ? "&aClaimed" : "&cUnclaimed"),
                "&8• &7Leader: &f" + (nation.getLeaderName() != null ? nation.getLeaderName() : "None"),
                "",
                "&c&lSTATE STATISTICS",
                "&8• &7Treasury: &a$" + formatMoney(balance),
                "&8• &7Territory: &f" + nation.getTerritorySize() + " Chunks",
                "&8• &7Population: &f" + memberCount + " &7(" + realCount + " Players, " + fakeCount + " NPCs)",
                "&8• &7Party Members: &f" + partyCount + " &8/ &f5",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&8General info & statistics");
    }

    // ── 2. Party Policy (slot 10) — PALE_OAK_HANGING_SIGN ──────────────

    private ItemStack buildPartyPolicyCard(Nation nation, CommunistGovernment cg) {
        List<String> lore = new ArrayList<>();
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("&7View and monitor all active executive");
        lore.add("&7directives and policies currently affecting");
        lore.add("&7your nation.");
        lore.add("");

        // Active Orders
        lore.add("&c&lACTIVE DIRECTIVES");
        int activeCount = 0;

        // Leader orders
        if (nation != null) {
            List<ExecutiveOrder> activeLeaderOrders = plugin.getExecutiveOrderManager().getActiveOrders(nation);
            for (ExecutiveOrder order : activeLeaderOrders) {
                lore.add(" &8• &e" + order.getType().getDisplayName() + " &8(&dLeader&8)");
                activeCount++;
            }
        }

        // Minister orders
        if (cg != null) {
            if (cg.isGlorificationActive()) {
                lore.add(" &8• &aLeader Glorification &8(&6Minister - Propaganda&8)");
                activeCount++;
            }
            if (cg.isSensorMediaActive()) {
                lore.add(" &8• &aMedia Censorship &8(&6Minister - Propaganda&8)");
                activeCount++;
            }
            if (cg.isDefenseProtocolActive()) {
                lore.add(" &8• &aDefense Protocol &8(&6Minister - Defense&8)");
                activeCount++;
            }
            if (cg.isOffenseProtocolActive()) {
                lore.add(" &8• &aOffense Protocol &8(&6Minister - Defense&8)");
                activeCount++;
            }
            if (cg.isMilitaryEmergencyActive()) {
                lore.add(" &8• &aMilitary Emergency &8(&6Minister - Defense&8)");
                activeCount++;
            }
            if (cg.getDistributionProgramPhasesLeft() > 0) {
                lore.add(" &8• &aDistribution Program &8(&6Minister - Treasury&8)");
                activeCount++;
            }
            if (cg.getTaxIntensificationPhasesLeft() > 0) {
                lore.add(" &8• &aTax Intensification &8(&6Minister - Treasury&8)");
                activeCount++;
            }
            if (cg.isMarketEventActive()) {
                lore.add(" &8• &aMarket Event &8(&6Minister - Treasury&8)");
                activeCount++;
            }
            if (cg.isQuarantineActive()) {
                lore.add(" &8• &aQuarantine Protocol &8(&6Minister - Health&8)");
                activeCount++;
            }
            if (cg.isVaccinationActive()) {
                lore.add(" &8• &aVaccination Drive &8(&6Minister - Health&8)");
                activeCount++;
            }
            if (cg.isPlagueActive()) {
                lore.add(" &8• &aPlague &8(&6Minister - Health&8)");
                activeCount++;
            }
        }

        if (activeCount == 0) {
            lore.add(" &8• &7No active directives.");
        }

        lore.add("");
        lore.add("&c&lRECENT HISTORY");
        if (cg != null && !cg.getOrderHistory().isEmpty()) {
            int idx = 1;
            for (String entry : cg.getOrderHistory()) {
                String cleanEntry = entry;
                if (entry.endsWith("(Leader)")) {
                    String name = entry.substring(0, entry.indexOf(" (Leader)"));
                    cleanEntry = "&f" + name + " &8(&dLeader&8)";
                } else if (entry.contains(" (Minister - ")) {
                    int startIdx = entry.indexOf(" (Minister - ");
                    String name = entry.substring(0, startIdx);
                    String dept = entry.substring(startIdx + 13, entry.length() - 1);
                    cleanEntry = "&f" + name + " &8(&6Minister - " + dept + "&8)";
                }
                lore.add(" &8• &7" + idx + ". " + cleanEntry);
                idx++;
            }
        } else {
            lore.add(" &8• &7No activation history.");
        }
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        return buildIcon(Material.PALE_OAK_HANGING_SIGN,
                "&5&lParty Policy",
                lore);
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
        lore.add("&7Appointed permanent party members");
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

    private ItemStack buildNotificationCard(Nation nation) {
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
            creatorName = "The Secretary General";
        }
        lore.add("&7Posted by: &e" + creatorName);

        String timeStr = new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm").format(new java.util.Date(nation.getAnnouncementCreatedAt()));
        lore.add("&7Posted at: &f" + timeStr);
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        return buildIcon(Material.BELL, "&e&lAnnouncement", lore);
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

    private ItemStack buildTaxCard(Player player) {
        var profile = plugin.getTaxManager().getProfile(player.getUniqueId());
        int openInvoices = profile != null ? profile.getOutstandingInvoices().size() : 0;
        double due = profile != null ? profile.getOutstandingTotal() : 0;

        return buildIcon(Material.BUNDLE,
                "&a&lNational Tax",
                "",
                "&7Invoice-based comrade taxation.",
                "&7Settle your bills before they double.",
                "",
                "&7Your Open Invoices : &f" + openInvoices,
                "&7Amount Due         : " + (due > 0 ? "&c$" + formatMoney(due) : "&a$0"),
                "",
                "&eClick &7→ Open Tax Bureau");
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
