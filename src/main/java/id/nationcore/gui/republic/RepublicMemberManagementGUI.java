package id.nationcore.gui.republic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import id.nationcore.NationCore;
import id.nationcore.models.FakeMember;
import id.nationcore.models.Government;
import id.nationcore.models.Nation;
import id.nationcore.models.Nation.NationMember;
import id.nationcore.models.Nation.NationRole;
import id.nationcore.utils.MessageUtils;

@SuppressWarnings({"deprecation", "unused"})
public class RepublicMemberManagementGUI {

    public static final String TITLE = "§6§lMEMBER MANAGEMENT";
    public static final String ACTION_TITLE_PREFIX = "§6§l[ACTION] §f";

    private static final int MEMBERS_PER_PAGE = 27;
    private static final int[] MEMBER_SLOTS;

    static {
        List<Integer> slots = new ArrayList<>();
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                int slot = row * 9 + col;
                if (slot != 43) {
                    slots.add(slot);
                }
            }
        }
        MEMBER_SLOTS = slots.stream().mapToInt(Integer::intValue).toArray();
    }

    private final NationCore plugin;

    public RepublicMemberManagementGUI(NationCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, Nation nation, int page) {
        if (nation == null) return;
        Government gov = nation.getRepublicGovernment();

        List<NationMember> members = new ArrayList<>(nation.getMembers().values());
        for (FakeMember npc : nation.getAllFakeMembers()) {
            members.add(new NationMember(npc.getId(), npc.getName(), npc.getRole()));
        }
        // Sort hierarchy: 0=President, 1=Senator, 2=Minister, 3=Member
        members.sort((a, b) -> {
            int roleA = getRoleWeight(a.getUuid(), gov);
            int roleB = getRoleWeight(b.getUuid(), gov);
            return Integer.compare(roleA, roleB);
        });

        int totalPages = Math.max(1, (int) Math.ceil(members.size() / (double) MEMBERS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // Glass border (uses light blue or gold for republic style)
        ItemStack border = createItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ");
        int[] borderSlots = {0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53};
        for (int slot : borderSlots) {
            inv.setItem(slot, border);
        }

        // Header info
        inv.setItem(4, createItem(Material.WRITABLE_BOOK,
                "§6§l" + nation.getName() + " — Member Management",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Total Members : §f" + nation.getMemberCount(),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Click a member to manage."));

        // Member heads
        int startIndex = page * MEMBERS_PER_PAGE;
        for (int i = 0; i < MEMBERS_PER_PAGE; i++) {
            int memberIndex = startIndex + i;
            if (memberIndex >= members.size()) break;

            NationMember member = members.get(memberIndex);
            inv.setItem(MEMBER_SLOTS[i], buildMemberHead(member, gov));
        }

        // Navigation
        if (page > 0) {
            inv.setItem(48, createItem(Material.ARROW,
                    "§e§l← Previous Page",
                    "§7Page " + page + " / " + totalPages));
        }

        inv.setItem(49, createItem(Material.PAPER,
                "§e§lPage " + (page + 1) + " / " + totalPages,
                "§7" + members.size() + " members total"));

        if (page < totalPages - 1) {
            inv.setItem(50, createItem(Material.ARROW,
                    "§e§lNext Page →",
                    "§7Page " + (page + 2) + " / " + totalPages));
        }

        inv.setItem(43, createItem(Material.SPECTRAL_ARROW,
                "§6§l← Back",
                "§7Return to Government Menu"));

        viewer.openInventory(inv);
    }

    public void openActionMenu(Player viewer, Nation nation, UUID targetUUID) {
        if (nation == null) return;
        NationMember member = nation.getMember(targetUUID);
        boolean isNpc = false;
        if (member == null && FakeMember.isNpcUUID(targetUUID)) {
            FakeMember npc = nation.getFakeMember(targetUUID);
            if (npc != null) {
                member = new NationMember(npc.getId(), npc.getName(), npc.getRole());
                isNpc = true;
            }
        }
        if (member == null) {
            MessageUtils.send(viewer, "§cMember not found.");
            return;
        }

        Government gov = nation.getRepublicGovernment();
        String name = member.getName();
        String title = ACTION_TITLE_PREFIX + name;

        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Filler border
        ItemStack border = createItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ");
        int[] borderSlots = {0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53};
        for (int slot : borderSlots) inv.setItem(slot, border);

        boolean isPresident = gov != null && gov.hasPresident() && gov.getPresidentUUID().equals(targetUUID);
        boolean isLeader = nation.getLeaderUUID() != null && nation.getLeaderUUID().equals(targetUUID);

        // Slot 4 — Player Head with profile info
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skull = (SkullMeta) head.getItemMeta();
        skull.setOwningPlayer(Bukkit.getOfflinePlayer(targetUUID));
        skull.setDisplayName("§e§l" + name);

        // Current cabinet position (if any)
        String cabinetPosText = "§7None";
        boolean isSenator = gov != null && gov.getSenators().contains(targetUUID);
        boolean isMinister = false;
        if (gov != null) {
            Government.CabinetPosition pos = gov.getPositionByUUID(targetUUID);
            if (pos != null) {
                cabinetPosText = "§6" + pos.getDisplayName();
                isMinister = true;
            }
        }

        String roleText;
        if (isPresident) roleText = "§6§lPresident";
        else if (isSenator) roleText = "§d§lSenator";
        else if (isMinister) roleText = "§b§lMinister";
        else roleText = "§7Member";

        List<String> profileLore = new ArrayList<>();
        profileLore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        profileLore.add("§7Nation Role  : " + roleText);
        profileLore.add("§7Cabinet Pos  : " + cabinetPosText);
        if (isNpc) {
            FakeMember npc = nation.getFakeMember(targetUUID);
            if (npc != null) {
                profileLore.add("§7Balance      : §e$" + MessageUtils.formatNumber(npc.getTaxBalance()));
                profileLore.add("§7Debt         : §c$" + MessageUtils.formatNumber(npc.getTaxDebt()));
            }
        }
        profileLore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        skull.setLore(profileLore);
        head.setItemMeta(skull);
        inv.setItem(4, head);

        // Slot 21 — Senator Action Button (NPCs allowed)
        if (isPresident) {
            inv.setItem(21, createItem(Material.BARRIER,
                    "§7§lAppoint as SENATOR",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7The President cannot be a Senator.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        } else if (isMinister) {
            inv.setItem(21, createItem(Material.BARRIER,
                    "§7§lAppoint as SENATOR",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§cThis member is a Minister and",
                    "§ccannot be appointed as a Senator.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        } else if (isSenator) {
            inv.setItem(21, createItem(Material.BARRIER,
                    "§c§lRemove SENATOR Role",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Remove §f" + name + " §7from the Senator role.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§eClick to remove."));
        } else {
            inv.setItem(21, createItem(Material.CRIMSON_SIGN,
                    "§c§lAppoint as SENATOR",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Appoint §f" + name + " §7as a Senator.",
                    "§7Senators balance the power of",
                    "§7the President.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§eClick to appoint."));
        }

        // Slot 22 — Send Message (1-hour cooldown per target)
        inv.setItem(22, createItem(Material.WRITABLE_BOOK,
                "§b§l✉ Send Message",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Send a presidential message to §f" + name + "§7.",
                "§7The message will be prefixed with your",
                "§7presidential title.",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§e⏰ Cooldown: §f1 hour per member",
                "§bClick to compose message."));

        // Slot 23 — Kick Member
        if (!isLeader && !isPresident) {
            inv.setItem(23, createItem(Material.BOOK,
                    "§c§l✖ Kick Member",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Remove §f" + name + " §7from the nation.",
                    "§c⚠ This action cannot be undone!",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§cClick to kick this member."));
        } else {
            inv.setItem(23, createItem(Material.BARRIER,
                    "§7§l✖ Kick Unavailable",
                    "§7Cannot kick the nation leader or President."));
        }

        // Helper: get current holder name for a cabinet position
        java.util.function.Function<Government.CabinetPosition, String> currentHolder = (pos) -> {
            if (gov == null) return "§7None";
            UUID holderUUID = gov.getCabinetMember(pos);
            if (holderUUID == null) return "§7None";
            String holderName = org.bukkit.Bukkit.getOfflinePlayer(holderUUID).getName();
            return holderName != null ? "§f" + holderName : "§7Unknown";
        };

        // Slot 30 — Appoint as Minister of Treasury
        if (isSenator) {
            inv.setItem(30, createItem(Material.BARRIER,
                    "§7§lAppoint as Minister of Treasury",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§cThis member is a Senator and",
                    "§ccannot be appointed as a Minister.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        } else if (isPresident) {
            inv.setItem(30, createItem(Material.BARRIER,
                    "§7§lAppoint as Minister of Treasury",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§cThe President cannot be a Minister.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        } else {
            inv.setItem(30, createItem(Material.WRITTEN_BOOK,
                    "§6§l💰 Appoint as Minister of Treasury",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Appoint §f" + name,
                    "§7as the §6Minister of Treasury§7.",
                    "§7Current: " + currentHolder.apply(Government.CabinetPosition.TREASURY),
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§eClick to appoint."));
        }

        // Slot 31 — Appoint as Minister of Defense
        if (isSenator) {
            inv.setItem(31, createItem(Material.BARRIER,
                    "§7§lAppoint as Minister of Defense",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§cThis member is a Senator and",
                    "§ccannot be appointed as a Minister.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        } else if (isPresident) {
            inv.setItem(31, createItem(Material.BARRIER,
                    "§7§lAppoint as Minister of Defense",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§cThe President cannot be a Minister.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        } else {
            inv.setItem(31, createItem(Material.WRITTEN_BOOK,
                    "§c§l🛡 Appoint as Minister of Defense",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Appoint §f" + name,
                    "§7as the §cMinister of Defense§7.",
                    "§7Current: " + currentHolder.apply(Government.CabinetPosition.DEFENSE),
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§eClick to appoint."));
        }

        // Slot 32 — Appoint as Minister of Health
        if (isSenator) {
            inv.setItem(32, createItem(Material.BARRIER,
                    "§7§lAppoint as Minister of Health",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§cThis member is a Senator and",
                    "§ccannot be appointed as a Minister.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        } else if (isPresident) {
            inv.setItem(32, createItem(Material.BARRIER,
                    "§7§lAppoint as Minister of Health",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§cThe President cannot be a Minister.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        } else {
            inv.setItem(32, createItem(Material.WRITTEN_BOOK,
                    "§d§l💉 Appoint as Minister of Health",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Appoint §f" + name,
                    "§7as the §dMinister of Health§7.",
                    "§7Current: " + currentHolder.apply(Government.CabinetPosition.HEALTH),
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§eClick to appoint."));
        }

        // Slot 43 — Back
        inv.setItem(43, createItem(Material.SPECTRAL_ARROW,
                "§6§l← Back",
                "§7Return to member list."));

        viewer.openInventory(inv);
    }

    private ItemStack buildMemberHead(NationMember member, Government gov) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(member.getUuid()));

        boolean isPresident = gov != null && gov.hasPresident() && gov.getPresidentUUID().equals(member.getUuid());

        boolean isSenator = gov != null && gov.getSenators().contains(member.getUuid());
        boolean isMinister = gov != null && gov.getPositionByUUID(member.getUuid()) != null;

        String prefix;
        String roleText;
        if (isPresident) {
            prefix = "§6👑 ";
            roleText = "§6§lPresident";
        } else if (isSenator) {
            prefix = "§d⚖ ";
            roleText = "§d§lSenator";
        } else if (isMinister) {
            prefix = "§b★ ";
            roleText = "§b§lMinister";
        } else {
            prefix = "§7· ";
            roleText = "§7Member";
        }

        meta.setDisplayName(prefix + member.getName());

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Role      : " + roleText);
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§eClick to manage this member.");

        meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        if (lore.length > 0) {
            List<String> loreList = new ArrayList<>();
            for (String line : lore) loreList.add(line);
            meta.setLore(loreList);
        }
        item.setItemMeta(meta);
        return item;
    }


    private int getRoleWeight(UUID uuid, Government gov) {
        if (gov != null) {
            if (gov.hasPresident() && gov.getPresidentUUID().equals(uuid)) {
                return 0; // President
            }
            if (gov.getSenators().contains(uuid)) {
                return 1; // Senator
            }
            if (gov.getPositionByUUID(uuid) != null) {
                return 2; // Minister
            }
        }
        return 3; // Member
    }

    public static String extractNameFromActionTitle(String title, String prefix) {
        if (title == null || !title.startsWith(prefix)) return null;
        return title.substring(prefix.length());
    }
}
