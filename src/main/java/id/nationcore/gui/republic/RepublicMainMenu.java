package id.nationcore.gui.republic;

import id.nationcore.gui.GUIListener;
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
import id.nationcore.models.PlayerData;
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
        int[] fillerSlots = {0,1,2,3,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,49,50,51,52,53};
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
        inv.setItem(SLOT_SENATOR, buildSenatorCard());

        inv.setItem(SLOT_ANNOUNCEMENT, buildAnnouncementCard());
        inv.setItem(SLOT_RECALL, buildRecallCard(nation));
        inv.setItem(SLOT_TAX, buildTaxCard());
        inv.setItem(SLOT_ELECTION, buildElectionCard(election, player));
        inv.setItem(SLOT_ARENA, buildArenaCard());

        inv.setItem(SLOT_HELP, buildHelpCard());
        inv.setItem(SLOT_CAPITAL, buildCapitalCard(nation));
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
                    List<CabinetDecision> active = plugin.getCabinetManager().getActiveDecisionsByPosition(nation, CabinetDecision.CabinetPosition.valueOf(pos.name()));
                    for (CabinetDecision decision : active) {
                        String timeStr = formatRemaining(decision.getRemainingTime());
                        lore.add("&a  ↳ Active: &e" + plugin.getCabinetManager().getDecisionDisplayName(decision.getType()) + " &7(&a" + timeStr + "&7)");
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

    private ItemStack buildSenatorCard() {
        return buildIcon(Material.CRIMSON_HANGING_SIGN,
                "&c&lSenator",
                "&7Coming Soon!");
    }

    private ItemStack buildAnnouncementCard() {
        return buildIcon(Material.BELL,
                "&e&lAnnouncement",
                "&7Coming Soon!");
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

    private ItemStack buildTaxCard() {
        return buildIcon(Material.LIGHT_BLUE_BUNDLE,
                "&a&lState Tax",
                "&7Open progressive tax system.",
                "&7Can be audited by all citizens.",
                "",
                "&eClick &7→ Open tax panel");
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

    private ItemStack buildCapitalCard(Nation nation) {
        return buildIcon(Material.LODESTONE,
                "&a&lCapital City",
                "&7Coming Soon!");
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
