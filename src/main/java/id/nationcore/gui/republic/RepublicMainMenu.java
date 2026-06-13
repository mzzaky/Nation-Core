package id.nationcore.gui.republic;

import id.nationcore.gui.NationMenuBase;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;
import id.nationcore.models.CabinetDecision;
import id.nationcore.models.Election;
import id.nationcore.models.Government;
import id.nationcore.models.Nation;
import id.nationcore.models.RecallPetition;

/**
 * Main menu for REPUBLIC nations.
 */
public class RepublicMainMenu extends NationMenuBase {

    public static final String TITLE = "§8§lRepublic Council";

    // Layout slots
    private static final int SLOT_NATION = 4;
    private static final int SLOT_CABINET = 10;
    private static final int SLOT_EXEC_ORDER = 11;
    private static final int SLOT_PRESIDENT = 13;
    private static final int SLOT_TREASURY = 15;
    private static final int SLOT_SENATOR = 16;

    private static final int SLOT_ANNOUNCEMENT = 28;
    private static final int SLOT_RECALL = 30;
    private static final int SLOT_TAX = 31;
    private static final int SLOT_ELECTION = 32;
    private static final int SLOT_ARENA = 34;

    private static final int SLOT_HELP = 37;
    private static final int SLOT_CAPITAL = 39;
    private static final int SLOT_RESEARCH = 40;
    private static final int SLOT_HISTORY = 41;
    private static final int SLOT_HUB = 43;

    private static final Material FILLER = Material.LIGHT_BLUE_STAINED_GLASS_PANE;

    public RepublicMainMenu(NationCore plugin) {
        super(plugin);
    }

    public void open(Player player, Nation nation) {
        Inventory inv = Bukkit.createInventory(null, 54, LegacyComponentSerializer.legacySection().deserialize(TITLE));

        // Fill empty slots with AIR initially
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, new ItemStack(Material.AIR));
        }

        // FILLER
        ItemStack filler = pane(FILLER);
        int[] fillerSlots = { 0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52,
                53 };
        for (int slot : fillerSlots) {
            inv.setItem(slot, filler);
        }

        Government gov = nation.getRepublicGovernment();
        Election election = nation.getElection();

        inv.setItem(SLOT_NATION, buildNationBadge(nation));
        inv.setItem(SLOT_CABINET, buildCabinetCard(gov, nation));
        inv.setItem(SLOT_EXEC_ORDER, buildExecOrderCard(nation, player));
        inv.setItem(SLOT_PRESIDENT, buildPresidentCard(gov, nation));
        inv.setItem(SLOT_TREASURY, buildTreasuryCard(nation));
        inv.setItem(SLOT_SENATOR, buildSenatorCard(nation));

        inv.setItem(SLOT_ANNOUNCEMENT, buildAnnouncementCard(nation));
        inv.setItem(SLOT_RECALL, buildRecallCard(nation));
        inv.setItem(SLOT_TAX, buildTaxCard(player));
        inv.setItem(SLOT_ELECTION, buildElectionCard(election, player));
        inv.setItem(SLOT_ARENA, buildArenaCard());

        inv.setItem(SLOT_HELP, buildHelpCard());
        inv.setItem(SLOT_CAPITAL, buildCapitalCard(player, nation));
        inv.setItem(SLOT_RESEARCH, buildResearchCard());
        inv.setItem(SLOT_HISTORY, buildHistoryCard());
        inv.setItem(SLOT_HUB, buildHubButton());

        player.openInventory(inv);
    }

    public static int getSlot(String key) {
        return switch (key) {
            case "NATION" -> SLOT_NATION;
            case "CABINET" -> SLOT_CABINET;
            case "EXEC_ORDER" -> SLOT_EXEC_ORDER;
            case "PRESIDENT" -> SLOT_PRESIDENT;
            case "TREASURY" -> SLOT_TREASURY;
            case "SENATOR" -> SLOT_SENATOR;
            case "ANNOUNCEMENT" -> SLOT_ANNOUNCEMENT;
            case "RECALL" -> SLOT_RECALL;
            case "TAX" -> SLOT_TAX;
            case "ELECTION" -> SLOT_ELECTION;
            case "ARENA" -> SLOT_ARENA;
            case "HELP" -> SLOT_HELP;
            case "CAPITAL" -> SLOT_CAPITAL;
            case "RESEARCH" -> SLOT_RESEARCH;
            case "HISTORY" -> SLOT_HISTORY;
            case "HUB" -> SLOT_HUB;
            default -> -1;
        };
    }

    private ItemStack buildNationBadge(Nation nation) {
        return buildIcon(Material.GLOW_ITEM_FRAME,
                "&b&l" + nation.getName(),
                "&7Republic &f[" + nation.getTag() + "]",
                "&7Members: &f" + nation.getMemberCount(),
                "",
                "&8System: &fOpen Democracy");
    }

    private ItemStack buildCabinetCard(Government gov, Nation nation) {
        List<String> lore = new ArrayList<>();
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        int filled = 0;
        int total = Government.CabinetPosition.values().length;

        if (gov != null) {
            for (Government.CabinetPosition pos : Government.CabinetPosition.values()) {
                UUID ministerUUID = gov.getCabinetMember(pos);
                String name = "&cVacant";
                if (ministerUUID != null) {
                    filled++;
                    name = "&f" + Bukkit.getOfflinePlayer(ministerUUID).getName();
                }
                lore.add("&7• &b" + pos.getDisplayName() + ": " + name);

                // Get active decisions/orders for this position
                if (ministerUUID != null && nation != null) {
                    List<CabinetDecision> active = plugin.getCabinetManager().getActiveDecisionsByPosition(nation,
                            CabinetDecision.CabinetPosition.valueOf(pos.name()));
                    for (CabinetDecision decision : active) {
                        String timeStr = formatRemaining(decision.getRemainingTime());
                        lore.add("&a  ↳ Active: &e"
                                + plugin.getCabinetManager().getDecisionDisplayName(decision.getType()) + " &7(&a"
                                + timeStr + "&7)");
                    }
                }
            }
        }

        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("&7Slots filled: &f" + filled + " &8/ &f" + total);
        lore.add("&7Appointed by the President to");
        lore.add("&7assist in policy execution.");

        return buildIcon(Material.WARPED_HANGING_SIGN,
                "&3&lMinister Cabinet",
                lore);
    }

    private ItemStack buildExecOrderCard(Nation nation, Player player) {
        List<String> lore = new ArrayList<>();
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        List<id.nationcore.models.ExecutiveOrder> active = nation.getActiveOrders();
        if (active.isEmpty()) {
            lore.add("&7No active executive orders.");
        } else {
            for (id.nationcore.models.ExecutiveOrder order : active) {
                String timeStr = formatRemaining(order.getRemainingTime());
                lore.add("&7• &d" + order.getType().getDisplayName() + " &7(&a" + timeStr + "&7)");
            }
        }
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        return buildIcon(Material.ENCHANTED_BOOK,
                "&d&lExecutive Orders",
                lore);
    }

    private ItemStack buildPresidentCard(Government gov, Nation nation) {
        if (gov == null || !gov.hasPresident()) {
            return buildIcon(Material.BEACON,
                    "&c&lPresidential Throne Vacant",
                    "&7No elected president yet.",
                    "&7Register during election phase",
                    "&7to take control.",
                    "",
                    "&eClick &7→ Government Info");
        }
        OfflinePlayer pres = Bukkit.getOfflinePlayer(gov.getPresidentUUID());
        long remaining = Math.max(0, gov.getTermEndTime() - System.currentTimeMillis());
        return buildIcon(Material.BEACON,
                "&b&l" + (gov.getPresidentName() != null ? gov.getPresidentName() : pres.getName()),
                wrap("&7Title: &fPresident of " + nation.getName(),
                        "&7Term: &f#" + gov.getCurrentTerm(),
                        "&7Remaining Term: &f" + formatRemaining(remaining),
                        "",
                        "&eClick &7→ Government Details"));
    }

    private ItemStack buildTreasuryCard(Nation nation) {
        double balance = nation.getTreasury().getBalance();
        return buildIcon(Material.WRITABLE_BOOK,
                "&6&lState Treasury",
                "&7Balance: &6$" + formatMoney(balance),
                "&7Source: &fCitizen tax & term salaries",
                "",
                "&eClick &7→ View Treasury Details");
    }

    private ItemStack buildSenatorCard(Nation nation) {
        List<String> lore = new ArrayList<>();
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        Government gov = nation.getRepublicGovernment();
        List<UUID> senators = gov != null ? gov.getSenators() : new ArrayList<>();
        if (senators.isEmpty()) {
            lore.add("&7No senators appointed yet.");
        } else {
            for (int i = 0; i < senators.size(); i++) {
                UUID senatorUUID = senators.get(i);
                String name = Bukkit.getOfflinePlayer(senatorUUID).getName();
                if (name == null) name = "Unknown";
                lore.add("&7• &f" + name + " &7(Senator #" + (i + 1) + ")");
            }
        }
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("&7Active Senators: &f" + senators.size() + " &8/ &f5");
        lore.add("&7Senators act as a check on the");
        lore.add("&7President's power.");
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        return buildIcon(Material.CRIMSON_HANGING_SIGN,
                "&c&lSenator",
                lore);
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
            creatorName = "The President";
        }
        lore.add("&7Posted by: &e" + creatorName);

        String timeStr = new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm").format(new java.util.Date(nation.getAnnouncementCreatedAt()));
        lore.add("&7Posted at: &f" + timeStr);
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        return buildIcon(Material.BELL, "&e&lAnnouncement", lore);
    }

    private ItemStack buildRecallCard(Nation nation) {
        RecallPetition petition = nation.getRecallPetition();
        if (petition == null) {
            return buildIcon(Material.ENDER_PEARL,
                    "&c&lImpeachment",
                    "&7No active petition.",
                    "&7Start a petition to overthrow",
                    "&7the president during crisis.",
                    "",
                    "&eClick &7→ Recall panel");
        }
        return glowing(buildIcon(Material.ENDER_PEARL,
                "&c&lImpeachment ACTIVE",
                "&7Status: &cPetition ongoing",
                "&7Signatures: &f" + petition.getSignatureCount(),
                "",
                "&eClick &7→ Review & sign"));
    }

    private ItemStack buildTaxCard(Player player) {
        var profile = plugin.getTaxManager().getProfile(player.getUniqueId());
        int openInvoices = profile != null ? profile.getOutstandingInvoices().size() : 0;
        double due = profile != null ? profile.getOutstandingTotal() : 0;

        return buildIcon(Material.LIGHT_BLUE_BUNDLE,
                "&a&lState Tax",
                "&7Invoice-based citizen taxation.",
                "&7Settle your bills before they double.",
                "",
                "&7Your Open Invoices : &f" + openInvoices,
                "&7Amount Due         : " + (due > 0 ? "&c$" + formatMoney(due) : "&a$0"),
                "",
                "&eClick &7→ Open Tax Office");
    }

    private ItemStack buildElectionCard(Election election, Player player) {
        return buildIcon(Material.ENDER_EYE,
                "&e&lPresident Election",
                "&7Coming Soon!");
    }

    private ItemStack buildArenaCard() {
        return buildIcon(Material.GOLD_BLOCK,
                "&c&lPresidential Game",
                "&7Presidential duel challenges to",
                "&7prove power and authority.",
                "",
                "&eClick &7→ Open arena");
    }

    private ItemStack buildHelpCard() {
        return buildIcon(Material.KNOWLEDGE_BOOK,
                "&b&lRepublic Guide",
                "&7Learn more about election mechanics,",
                "&7cabinet, arena, and recall.",
                "",
                "&eClick &7→ Open guide");
    }

    private ItemStack buildCapitalCard(Player player, Nation nation) {
        if (!nation.hasCapital()) {
            return buildIcon(Material.LODESTONE,
                    "&a&lCapital City",
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

        return buildIcon(Material.LODESTONE, "&a&lCapital City", lore);
    }

    private ItemStack buildResearchCard() {
        return buildIcon(Material.ENCHANTING_TABLE,
                "&d&lResearch Center",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&7Pursue research projects that",
                "&7benefit every citizen of the republic.",
                "",
                "&7Categories : &fEconomy, Technology, War",
                "&7Started by : &fThe President",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&eClick &7→ Open Research");
    }

    private ItemStack buildHistoryCard() {
        return buildIcon(Material.BOOKSHELF,
                "&6&lPresident History",
                "&7List of previous leaders",
                "&7with their ratings & terms.",
                "",
                "&eClick &7→ Open archives");
    }

    private ItemStack buildHubButton() {
        return buildIcon(Material.SPECTRAL_ARROW,
                "&e&l⚐ Nation Hub",
                "&7View all nations on the server",
                "&7and diplomatic information.",
                "",
                "&eClick &7→ Return to Hub");
    }

    public static boolean isFiller(Material m) {
        return m == FILLER;
    }
}
