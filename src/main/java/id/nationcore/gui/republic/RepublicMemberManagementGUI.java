package id.nationcore.gui.republic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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

public class RepublicMemberManagementGUI {

    public static final String TITLE = "§6§lMEMBER MANAGEMENT";
    public static final String ACTION_TITLE_PREFIX = "§6§l[ACTION] §f";

    private static final int MEMBERS_PER_PAGE = 28;
    private static final int[] MEMBER_SLOTS;

    static {
        List<Integer> slots = new ArrayList<>();
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                slots.add(row * 9 + col);
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
        // Sort: leader first, then officers, then citizens
        members.sort((a, b) -> a.getRole().ordinal() - b.getRole().ordinal());

        int totalPages = Math.max(1, (int) Math.ceil(members.size() / (double) MEMBERS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // Glass border (uses light blue or gold for republic style)
        ItemStack border = createItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, border);
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
            inv.setItem(45, createItem(Material.ARROW,
                    "§e§l← Previous Page",
                    "§7Page " + page + " / " + totalPages));
        }

        inv.setItem(49, createItem(Material.PAPER,
                "§e§lPage " + (page + 1) + " / " + totalPages,
                "§7" + members.size() + " members total"));

        if (page < totalPages - 1) {
            inv.setItem(53, createItem(Material.ARROW,
                    "§e§lNext Page →",
                    "§7Page " + (page + 2) + " / " + totalPages));
        }

        inv.setItem(47, createItem(Material.SPECTRAL_ARROW,
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

        Inventory inv = Bukkit.createInventory(null, 27, title);

        ItemStack border = createItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        boolean isPresident = gov != null && gov.hasPresident() && gov.getPresidentUUID().equals(targetUUID);
        boolean isLeader = nation.getLeaderUUID() != null && nation.getLeaderUUID().equals(targetUUID);
        boolean isOfficer = member.getRole() == NationRole.OFFICER;

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skull = (SkullMeta) head.getItemMeta();
        skull.setOwningPlayer(Bukkit.getOfflinePlayer(targetUUID));
        skull.setDisplayName("§e§l" + name);

        List<String> profileLore = new ArrayList<>();
        profileLore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        profileLore.add("§7Nation Role  : " + formatRole(member.getRole()));
        profileLore.add("§7President    : " + (isPresident ? "§a✔ Yes" : "§c✘ No"));
        if (isNpc) {
            FakeMember npc = nation.getFakeMember(targetUUID);
            if (npc != null) {
                profileLore.add("§7Type         : §dFake Member (NPC)");
                profileLore.add("§7Balance      : §e$" + MessageUtils.formatNumber(npc.getTaxBalance()));
                profileLore.add("§7Debt         : §c$" + MessageUtils.formatNumber(npc.getTaxDebt()));
            }
        }
        profileLore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        skull.setLore(profileLore);
        head.setItemMeta(skull);
        inv.setItem(4, head);

        // Actions:
        // 1. Kick (slot 11) - disabled for leader/president
        if (!isLeader && !isPresident) {
            inv.setItem(11, createItem(Material.RED_CONCRETE,
                    "§c§l✖ Kick from Nation",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Remove §f" + name + " §7from the nation.",
                    "§c⚠ This action cannot be undone!",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§cClick to kick this player."));
        } else {
            inv.setItem(11, createItem(Material.BARRIER,
                    "§7§l✖ Kick Unavailable",
                    "§7Cannot kick the nation leader / President."));
        }

        // 2. Promote/Demote (slot 15)
        if (!isLeader && !isPresident) {
            if (!isOfficer) {
                inv.setItem(15, createItem(Material.LIME_CONCRETE,
                        "§a§l★ Promote to Officer",
                        "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                        "§7Promote §f" + name + " §7to Officer.",
                        "§7Officers can help manage the nation.",
                        "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                        "§aClick to promote."));
            } else {
                inv.setItem(15, createItem(Material.ORANGE_CONCRETE,
                        "§c§lDemote to Citizen",
                        "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                        "§7Demote §f" + name + " §7to Citizen.",
                        "§7They will lose officer privileges.",
                        "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                        "§cClick to demote."));
            }
        } else {
            inv.setItem(15, createItem(Material.BARRIER,
                    "§7§l★ Role Permanent",
                    "§7This member holds a permanent/highest role."));
        }

        inv.setItem(22, createItem(Material.SPECTRAL_ARROW,
                "§c§l← Back",
                "§7Return to member list."));

        viewer.openInventory(inv);
    }

    private ItemStack buildMemberHead(NationMember member, Government gov) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(member.getUuid()));

        boolean isPresident = gov != null && gov.hasPresident() && gov.getPresidentUUID().equals(member.getUuid());
        boolean isOfficer = member.getRole() == NationRole.OFFICER;
        boolean isOnline = Bukkit.getPlayer(member.getUuid()) != null;
        boolean isNpc = FakeMember.isNpcUUID(member.getUuid());

        String prefix;
        if (isPresident) prefix = "§6👑 ";
        else if (isOfficer) prefix = "§b★ ";
        else prefix = "§7· ";

        meta.setDisplayName(prefix + member.getName());

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        if (isNpc) {
            lore.add("§7Status    : §dFake Member (NPC)");
        } else {
            lore.add("§7Status    : " + (isOnline ? "§aOnline" : "§8Offline"));
        }
        lore.add("§7Role      : " + formatRole(member.getRole()));
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

    private String formatRole(NationRole role) {
        return switch (role) {
            case LEADER  -> "§6§lPresident";
            case OFFICER -> "§bOfficer";
            case CITIZEN -> "§7Citizen";
        };
    }

    public static String extractNameFromActionTitle(String title, String prefix) {
        if (title == null || !title.startsWith(prefix)) return null;
        return title.substring(prefix.length());
    }
}
