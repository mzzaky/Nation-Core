package id.nationcore.gui.communist;

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
import id.nationcore.models.CommunistGovernment;
import id.nationcore.models.CommunistGovernment.PolitburoPosition;
import id.nationcore.models.FakeMember;
import id.nationcore.models.Nation;
import id.nationcore.models.Nation.NationMember;
import id.nationcore.models.Nation.NationRole;
import id.nationcore.utils.MessageUtils;

@SuppressWarnings({"deprecation", "unused"})
public class CommunistMemberManagementGUI {

    public static final String TITLE = "§c§l⚑ MEMBER MANAGEMENT ⚑";
    public static final String ACTION_TITLE_PREFIX = "§c§l[ACTION] §f";
    public static final String POLITBURO_PICK_TITLE_PREFIX = "§c§l[POLITBURO] §f";

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

    public CommunistMemberManagementGUI(NationCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, Nation nation, int page) {
        if (nation == null) return;
        CommunistGovernment cg = nation.getCommunistGovernment();

        List<NationMember> members = new ArrayList<>(nation.getMembers().values());
        for (FakeMember npc : nation.getAllFakeMembers()) {
            members.add(new NationMember(npc.getId(), npc.getName(), npc.getRole()));
        }
        // Sort hierarchy: 0=Secretary General, 1=Politburo, 2=Party Member, 3=Citizen
        members.sort((a, b) -> {
            int roleA = getRoleWeight(a.getUuid(), cg);
            int roleB = getRoleWeight(b.getUuid(), cg);
            return Integer.compare(roleA, roleB);
        });

        int totalPages = Math.max(1, (int) Math.ceil(members.size() / (double) MEMBERS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // Glass border (uses red stained glass for communist style)
        ItemStack border = createItem(Material.RED_STAINED_GLASS_PANE, " ");
        int[] borderSlots = {0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53};
        for (int slot : borderSlots) {
            inv.setItem(slot, border);
        }

        // Header info
        int partyMembers = cg != null ? cg.getPartyMemberCount() : 0;
        int politburoCount = cg != null ? cg.getPolitburo().size() : 0;

        inv.setItem(4, createItem(Material.WRITABLE_BOOK,
                "§c§l⚑ " + nation.getName() + " — Member Management",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Total Members  : §f" + nation.getMemberCount(),
                "§7Party Members  : §f" + partyMembers + " / 5",
                "§7Politburo      : §f" + politburoCount + " / 4",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Click a member to manage."));

        // Member heads
        int startIndex = page * MEMBERS_PER_PAGE;
        for (int i = 0; i < MEMBERS_PER_PAGE; i++) {
            int memberIndex = startIndex + i;
            if (memberIndex >= members.size()) break;

            NationMember member = members.get(memberIndex);
            inv.setItem(MEMBER_SLOTS[i], buildMemberHead(member, cg));
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
                "§c§l← Back",
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

        CommunistGovernment cg = nation.getCommunistGovernment();
        String name = member.getName();
        String title = ACTION_TITLE_PREFIX + name;

        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Filler border
        ItemStack border = createItem(Material.RED_STAINED_GLASS_PANE, " ");
        int[] borderSlots = {0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53};
        for (int slot : borderSlots) inv.setItem(slot, border);

        boolean isSekjen = cg != null && cg.hasSecretaryGeneral() && cg.getSecretaryGeneralUUID().equals(targetUUID);
        boolean isParty = cg != null && cg.isPartyMember(targetUUID);
        boolean isPolitburo = cg != null && cg.getPolitburoMemberByUUID(targetUUID) != null;
        boolean isLeader = nation.getLeaderUUID() != null && nation.getLeaderUUID().equals(targetUUID);

        // Slot 4 — Player Head with profile info
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skull = (SkullMeta) head.getItemMeta();
        skull.setOwningPlayer(Bukkit.getOfflinePlayer(targetUUID));
        skull.setDisplayName("§e§l" + name);

        // Current cabinet position (if any)
        String cabinetPosText = "§7None";
        if (cg != null) {
            CommunistGovernment.PolitburoPosition pos = cg.getPositionByUUID(targetUUID);
            if (pos != null) {
                cabinetPosText = "§6" + pos.getDisplayName();
            }
        }

        String roleText;
        if (isSekjen) roleText = "§c§lSecretary General";
        else if (isPolitburo) roleText = "§6§lPolitburo";
        else if (isParty) roleText = "§a§lParty Member";
        else roleText = "§7Citizen";

        List<String> profileLore = new ArrayList<>();
        profileLore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        profileLore.add("§7Nation Role  : " + roleText);
        profileLore.add("§7Cabinet Pos  : " + cabinetPosText);
        profileLore.add("§7Party Member : " + (isParty ? "§a✔ Yes" : "§c✘ No"));
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

        // Slot 21 — Party Member Action Button
        if (isSekjen) {
            inv.setItem(21, createItem(Material.BARRIER,
                    "§7§lAppoint as Party Member",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7The Secretary General is already in the Party.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        } else if (isPolitburo) {
            inv.setItem(21, createItem(Material.BARRIER,
                    "§7§lAppoint as Party Member",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§cThis member is in the Politburo and",
                    "§ccannot be removed from the Party.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        } else if (isParty) {
            inv.setItem(21, createItem(Material.BARRIER,
                    "§c§lRemove Party Member Role",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Remove §f" + name + " §7from the Party.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§eClick to remove."));
        } else {
            inv.setItem(21, createItem(Material.CRIMSON_SIGN,
                    "§c§lAppoint as Party Member",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Appoint §f" + name + " §7as a Party Member.",
                    "§7Party members can vote in",
                    "§7Secretary General elections.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§eClick to appoint."));
        }

        // Slot 22 — Send Message
        long now = System.currentTimeMillis();
        long cooldownMs = 60L * 60 * 1000;
        java.util.Map<UUID, Long> cooldowns = plugin.getGUIListener().memberMessageCooldowns.get(viewer.getUniqueId());
        Long lastSent = cooldowns != null ? cooldowns.get(targetUUID) : null;
        long remaining = (lastSent != null && (now - lastSent) < cooldownMs) ? (cooldownMs - (now - lastSent)) : 0;

        List<String> msgLore = new ArrayList<>();
        msgLore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        msgLore.add("§7Send a Secretary General message to §f" + name + "§7.");
        msgLore.add("§7The message will be prefixed with your");
        msgLore.add("§7Secretary General title.");
        msgLore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        if (remaining > 0) {
            msgLore.add("§e⏰ Cooldown: §c" + MessageUtils.formatTime(remaining));
            msgLore.add("§7Wait until the cooldown ends.");
        } else {
            msgLore.add("§e⏰ Cooldown: §f1 hour per member");
            msgLore.add("§bClick to compose message.");
        }

        inv.setItem(22, createItem(Material.WRITABLE_BOOK,
                "§b§l✉ Send Message",
                msgLore.toArray(new String[0])));

        // Slot 23 — Kick Member
        if (!isLeader && !isSekjen) {
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
                    "§7Cannot kick the nation leader or Secretary General."));
        }

        // Helper: get current holder name for a cabinet position
        java.util.function.Function<CommunistGovernment.PolitburoPosition, String> currentHolder = (pos) -> {
            if (cg == null) return "§7None";
            CommunistGovernment.PolitburoMember holder = cg.getPolitburoMember(pos);
            if (holder == null) return "§7None";
            String holderName = holder.getName();
            return holderName != null ? "§f" + holderName : "§7Unknown";
        };

        // Slot 29 — Appoint as Minister of Propaganda
        if (isSekjen) {
            inv.setItem(29, createItem(Material.BARRIER,
                    "§7§lAppoint as Minister of Propaganda",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§cThe Secretary General cannot be a Minister.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        } else if (!isParty) {
            inv.setItem(29, createItem(Material.BARRIER,
                    "§7§lAppoint as Minister of Propaganda",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§cMust be a Party member first.",
                    "§cAdd to Party before appointing",
                    "§cto the Politburo.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        } else {
            inv.setItem(29, createItem(Material.WRITTEN_BOOK,
                    "§d§l📢 Appoint as Minister of Propaganda",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Appoint §f" + name,
                    "§7as the §dMinister of Propaganda§7.",
                    "§7Current: " + currentHolder.apply(CommunistGovernment.PolitburoPosition.PROPAGANDA),
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§eClick to appoint."));
        }

        // Slot 30 — Appoint as Minister of Defense
        if (isSekjen) {
            inv.setItem(30, createItem(Material.BARRIER,
                    "§7§lAppoint as Minister of Defense",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§cThe Secretary General cannot be a Minister.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        } else if (!isParty) {
            inv.setItem(30, createItem(Material.BARRIER,
                    "§7§lAppoint as Minister of Defense",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§cMust be a Party member first.",
                    "§cAdd to Party before appointing",
                    "§cto the Politburo.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        } else {
            inv.setItem(30, createItem(Material.WRITTEN_BOOK,
                    "§c§l🛡 Appoint as Minister of Defense",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Appoint §f" + name,
                    "§7as the §cMinister of Defense§7.",
                    "§7Current: " + currentHolder.apply(CommunistGovernment.PolitburoPosition.DEFENSE),
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§eClick to appoint."));
        }

        // Slot 32 — Appoint as Minister of Treasury
        if (isSekjen) {
            inv.setItem(32, createItem(Material.BARRIER,
                    "§7§lAppoint as Minister of Treasury",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§cThe Secretary General cannot be a Minister.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        } else if (!isParty) {
            inv.setItem(32, createItem(Material.BARRIER,
                    "§7§lAppoint as Minister of Treasury",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§cMust be a Party member first.",
                    "§cAdd to Party before appointing",
                    "§cto the Politburo.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        } else {
            inv.setItem(32, createItem(Material.WRITTEN_BOOK,
                    "§6§l💰 Appoint as Minister of Treasury",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Appoint §f" + name,
                    "§7as the §6Minister of Treasury§7.",
                    "§7Current: " + currentHolder.apply(CommunistGovernment.PolitburoPosition.TREASURY),
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§eClick to appoint."));
        }

        // Slot 33 — Appoint as Minister of Health
        if (isSekjen) {
            inv.setItem(33, createItem(Material.BARRIER,
                    "§7§lAppoint as Minister of Health",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§cThe Secretary General cannot be a Minister.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        } else if (!isParty) {
            inv.setItem(33, createItem(Material.BARRIER,
                    "§7§lAppoint as Minister of Health",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§cMust be a Party member first.",
                    "§cAdd to Party before appointing",
                    "§cto the Politburo.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        } else {
            inv.setItem(33, createItem(Material.WRITTEN_BOOK,
                    "§a§l💉 Appoint as Minister of Health",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Appoint §f" + name,
                    "§7as the §aMinister of Health§7.",
                    "§7Current: " + currentHolder.apply(CommunistGovernment.PolitburoPosition.HEALTH),
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§eClick to appoint."));
        }

        // Slot 43 — Back
        inv.setItem(43, createItem(Material.SPECTRAL_ARROW,
                "§c§l← Back",
                "§7Return to member list."));

        viewer.openInventory(inv);
    }

    private ItemStack buildMemberHead(NationMember member, CommunistGovernment cg) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(member.getUuid()));

        boolean isSekjen = cg != null && cg.hasSecretaryGeneral() && cg.getSecretaryGeneralUUID().equals(member.getUuid());
        boolean isPolitburo = cg != null && cg.getPolitburoMemberByUUID(member.getUuid()) != null;
        boolean isParty = cg != null && cg.isPartyMember(member.getUuid());
        boolean isOnline = Bukkit.getPlayer(member.getUuid()) != null;

        String prefix;
        String roleText;
        if (isSekjen) {
            prefix = "§c☆ ";
            roleText = "§c§lSecretary General";
        } else if (isPolitburo) {
            prefix = "§6★ ";
            roleText = "§6§lPolitburo";
        } else if (isParty) {
            prefix = "§a▶ ";
            roleText = "§a§lParty Member";
        } else {
            prefix = "§7· ";
            roleText = "§7Citizen";
        }

        meta.setDisplayName(prefix + member.getName());

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Status    : " + (isOnline ? "§aOnline" : "§8Offline"));
        lore.add("§7Role      : " + roleText);
        if (isPolitburo && cg != null) {
            lore.add("§7Position  : §6" + getPolitburoPositionName(cg, member.getUuid()));
        }
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

    private int getRoleWeight(UUID uuid, CommunistGovernment cg) {
        if (cg != null) {
            if (cg.hasSecretaryGeneral() && cg.getSecretaryGeneralUUID().equals(uuid)) {
                return 0; // Secretary General
            }
            if (cg.getPolitburoMemberByUUID(uuid) != null) {
                return 1; // Politburo Minister
            }
            if (cg.isPartyMember(uuid)) {
                return 2; // Party Member
            }
        }
        return 3; // Citizen / Ordinary member
    }

    private String getPolitburoPositionName(CommunistGovernment cg, UUID uuid) {
        if (cg == null) return "Unknown";
        PolitburoPosition pos = cg.getPositionByUUID(uuid);
        return pos != null ? pos.getDisplayName() : "Unknown";
    }

    public static String extractNameFromActionTitle(String title, String prefix) {
        if (title == null || !title.startsWith(prefix)) return null;
        return title.substring(prefix.length());
    }
}
