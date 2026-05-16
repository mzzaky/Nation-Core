package id.nationcore.gui.monarchy;

import id.nationcore.gui.NationMenuBase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import id.nationcore.NationCore;
import id.nationcore.models.MonarchyDecisionType;
import id.nationcore.models.MonarchyGovernment;
import id.nationcore.models.MonarchyGovernment.HighCouncilMember;
import id.nationcore.models.MonarchyGovernment.HighCouncilPosition;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

public class MonarchyExecutiveOrdersMenu extends NationMenuBase {

    public static final String TITLE = ChatColor.translateAlternateColorCodes('&',
            "&e&l👑 &6&lRoyal Decisions");

    public static final int SLOT_BACK = 43;
    public static final int SLOT_CLOSE = 53;

    private static final int[] FILLER_SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 52, 53
    };

    private static final int[] DECISION_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42
    };

    private static final Map<UUID, RoyalOrderTab> activeTabs = new HashMap<>();

    public enum RoyalOrderTab {
        HERALD(HighCouncilPosition.HERALD, "Herald", Material.PALE_OAK_HANGING_SIGN, 47),
        CHANCELLOR(HighCouncilPosition.CHANCELLOR, "Chancellor", chestMaterial(), 48),
        KING(null, "King", Material.GOLDEN_HORSE_ARMOR, 49),
        MARSHAL(HighCouncilPosition.MARSHAL, "Marshal", Material.GOLDEN_CHESTPLATE, 50),
        SAINT(HighCouncilPosition.SAINT, "Saint", Material.ENCHANTED_GOLDEN_APPLE, 51);

        private final HighCouncilPosition pos;
        private final String displayName;
        private final Material material;
        private final int slot;

        RoyalOrderTab(HighCouncilPosition pos, String displayName, Material material, int slot) {
            this.pos = pos;
            this.displayName = displayName;
            this.material = material;
            this.slot = slot;
        }

        public HighCouncilPosition getPosition() { return pos; }
        public String getDisplayName() { return displayName; }
        public Material getMaterial() { return material; }
        public int getSlot() { return slot; }

        public static RoyalOrderTab getBySlot(int slot) {
            for (RoyalOrderTab t : values()) {
                if (t.getSlot() == slot) return t;
            }
            return null;
        }

        private static Material chestMaterial() {
            try {
                return Material.valueOf("YELLOW_BUNDLE");
            } catch (Exception e) {
                return Material.BUNDLE;
            }
        }
    }

    public MonarchyExecutiveOrdersMenu(NationCore plugin) {
        super(plugin);
    }

    public static boolean handleTabClick(Player player, int slot) {
        RoyalOrderTab tab = RoyalOrderTab.getBySlot(slot);
        if (tab != null) {
            activeTabs.put(player.getUniqueId(), tab);
            return true;
        }
        return false;
    }

    public static MonarchyDecisionType getDecisionAtSlot(Player player, int slot) {
        RoyalOrderTab tab = activeTabs.getOrDefault(player.getUniqueId(), RoyalOrderTab.KING);
        int index = -1;
        for (int i = 0; i < DECISION_SLOTS.length; i++) {
            if (DECISION_SLOTS[i] == slot) { index = i; break; }
        }
        if (index == -1) return null;
        List<MonarchyDecisionType> decisions = getDecisionsForTab(tab);
        return index < decisions.size() ? decisions.get(index) : null;
    }

    public static id.nationcore.models.ExecutiveOrder.ExecutiveOrderType getExecutiveOrderAtSlot(Player player, int slot) {
        RoyalOrderTab tab = activeTabs.getOrDefault(player.getUniqueId(), RoyalOrderTab.KING);
        if (tab != RoyalOrderTab.KING) return null;
        int index = -1;
        for (int i = 0; i < DECISION_SLOTS.length; i++) {
            if (DECISION_SLOTS[i] == slot) { index = i; break; }
        }
        if (index == -1) return null;
        var types = id.nationcore.models.ExecutiveOrder.ExecutiveOrderType.values();
        return index < types.length ? types[index] : null;
    }

    private static List<MonarchyDecisionType> getDecisionsForTab(RoyalOrderTab tab) {
        List<MonarchyDecisionType> list = new ArrayList<>();
        if (tab == RoyalOrderTab.KING) return list; // King tab shows executive orders instead
        for (MonarchyDecisionType type : MonarchyDecisionType.values()) {
            if (type.getPosition() == tab.getPosition()) list.add(type);
        }
        return list;
    }

    public void open(Player player, Nation nation) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        MonarchyGovernment mg = nation.getMonarchyGovernment();
        RoyalOrderTab activeTab = activeTabs.getOrDefault(player.getUniqueId(), RoyalOrderTab.KING);

        ItemStack filler = pane(Material.YELLOW_STAINED_GLASS_PANE);
        for (int slot : FILLER_SLOTS) inv.setItem(slot, filler);

        inv.setItem(4, buildNationProfile(nation, mg));
        inv.setItem(SLOT_BACK, buildIcon(Material.SPECTRAL_ARROW, "&e&l⮜ Back", "&7Return to main menu"));

        for (RoyalOrderTab tab : RoyalOrderTab.values()) {
            inv.setItem(tab.getSlot(), buildTabIcon(tab, activeTab == tab));
        }

        if (activeTab == RoyalOrderTab.KING) {
            var types = id.nationcore.models.ExecutiveOrder.ExecutiveOrderType.values();
            for (int i = 0; i < types.length && i < DECISION_SLOTS.length; i++) {
                inv.setItem(DECISION_SLOTS[i], buildExecutiveOrderCard(nation, mg, player, types[i]));
            }
        } else {
            List<MonarchyDecisionType> decisions = getDecisionsForTab(activeTab);
            for (int i = 0; i < decisions.size() && i < DECISION_SLOTS.length; i++) {
                inv.setItem(DECISION_SLOTS[i], buildDecisionCard(nation, mg, player, decisions.get(i)));
            }
        }

        player.openInventory(inv);
    }

    private ItemStack buildNationProfile(Nation nation, MonarchyGovernment mg) {
        int memberCount = nation.getMemberCount();
        int soldiers = mg != null ? mg.getRoyalSoldierCount() : 0;
        return buildIcon(Material.GLOW_ITEM_FRAME,
                "&e&l" + nation.getName(),
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&7Government : &fMonarchy",
                "&7Tag        : &f[" + nation.getTag() + "]",
                "&7Subjects   : &f" + memberCount,
                "&7Soldiers   : &f" + soldiers,
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&8Displays information about the kingdom");
    }

    private ItemStack buildTabIcon(RoyalOrderTab tab, boolean isActive) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Category: &f" + tab.getDisplayName());
        if (isActive) {
            lore.add("");
            lore.add("&a▶ Currently viewing");
        } else {
            lore.add("");
            lore.add("&eClick to view " + tab.getDisplayName() + " orders");
        }
        ItemStack item = buildIcon(tab.getMaterial(),
                (isActive ? "&a&l" : "&6&l") + tab.getDisplayName(), lore);
        if (isActive) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    private ItemStack buildDecisionCard(Nation nation, MonarchyGovernment mg,
                                        Player viewer, MonarchyDecisionType type) {
        boolean isAdmin = viewer.hasPermission("nation.admin");
        boolean isKing = mg != null && mg.hasKing() && mg.getKingUUID().equals(viewer.getUniqueId());
        HighCouncilMember member = mg != null ? mg.getCouncilMember(type.getPosition()) : null;
        boolean isHolder = member != null && member.getUuid().equals(viewer.getUniqueId());
        boolean canIssue = isHolder || isKing || isAdmin;

        long cooldownRemaining = plugin.getMonarchyManager()
                .getDecisionCooldownRemaining(nation, viewer.getUniqueId(), type);
        boolean onCooldown = cooldownRemaining > 0;
        boolean canAfford = plugin.getTreasuryManager().canAfford(nation, type.getCost());
        boolean active = mg != null && plugin.getMonarchyManager().isDecisionStateActive(mg, type);

        Material material;
        String status;
        if (active) {
            material = Material.LIME_CONCRETE;
            status = "&a[ACTIVE]";
        } else if (onCooldown) {
            material = Material.RED_CONCRETE;
            status = "&c[COOLDOWN]";
        } else if (!canIssue) {
            material = Material.LIGHT_BLUE_CONCRETE;
            status = "&8[NO ACCESS]";
        } else if (!canAfford) {
            material = Material.GRAY_CONCRETE;
            status = "&8[INSUFFICIENT FUNDS]";
        } else {
            material = Material.YELLOW_CONCRETE;
            status = "&e[AVAILABLE]";
        }

        String holderName = "Vacant";
        if (member != null) {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(member.getUuid());
            if (op.getName() != null) holderName = op.getName();
        }

        List<String> lore = new ArrayList<>();
        lore.add("&7" + type.getDescription());
        lore.add("");
        lore.add("&7Council Seat: &f" + (type.getPosition() != null ? type.getPosition().getDisplayName() : "General"));
        lore.add("&7Holder: &f" + holderName);
        lore.add("&7Cost: &6$" + MessageUtils.formatNumber(type.getCost()));
        lore.add("&7Duration: &f" + (type.isInstant()
                ? "instant"
                : MessageUtils.formatTimeShort(type.getDurationMillis())));
        lore.add("");

        if (active) {
            lore.add("&aActive in this kingdom.");
        } else if (onCooldown) {
            lore.add("&cCooldown: &f" + MessageUtils.formatTime(cooldownRemaining));
        } else if (!canIssue) {
            lore.add("&8Only the " + (type.getPosition() != null ? type.getPosition().getDisplayName() : "King"));
            lore.add("&8(or the King) can issue this.");
        } else if (!canAfford) {
            lore.add("&cInsufficient funds in " + nation.getName() + ".");
        } else {
            lore.add("&aClick &7→ Issue royal decision");
        }

        return buildIcon(material, "&6&l" + type.getDisplayName() + " " + status, lore);
    }

    private ItemStack buildExecutiveOrderCard(Nation nation, MonarchyGovernment mg, Player viewer,
                                              id.nationcore.models.ExecutiveOrder.ExecutiveOrderType type) {
        boolean isKing = mg != null && mg.hasKing() && mg.getKingUUID().equals(viewer.getUniqueId());
        boolean isAdmin = viewer.hasPermission("nation.admin");
        boolean canIssue = isKing || isAdmin;

        boolean active = plugin.getExecutiveOrderManager().isOrderActive(nation, type);
        // Absolute power: the King ignores the per-order cooldown.
        boolean onCooldown = false;
        long cost = plugin.getConfig().getLong("executive-orders.cost", 1_000_000);
        boolean canAfford = plugin.getTreasuryManager().canAfford(nation, cost);
        id.nationcore.models.ExecutiveOrder activeOrder = active
                ? plugin.getExecutiveOrderManager().getActiveOrder(nation, type) : null;

        Material material;
        String status;
        if (active) {
            material = Material.LIME_CONCRETE;
            status = "&a[ACTIVE]";
        } else if (onCooldown) {
            material = Material.RED_CONCRETE;
            status = "&c[COOLDOWN]";
        } else if (!canIssue) {
            material = Material.LIGHT_BLUE_CONCRETE;
            status = "&8[NO ACCESS]";
        } else if (!canAfford) {
            material = Material.GRAY_CONCRETE;
            status = "&8[INSUFFICIENT FUNDS]";
        } else {
            material = Material.YELLOW_CONCRETE;
            status = "&e[AVAILABLE]";
        }

        String kingName = "Vacant throne";
        if (mg != null && mg.hasKing()) {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(mg.getKingUUID());
            if (op.getName() != null) kingName = op.getName();
        }

        List<String> lore = new ArrayList<>();
        lore.add("&7" + type.getFlavorText());
        lore.add("");
        lore.add("&7King: &f" + kingName);
        lore.add("&6&lEffects:");
        lore.add("&7" + type.getEffectDescription());
        lore.add("");
        lore.add("&7Duration: &f" + (type.getDefaultDuration() == 0
                ? "instant"
                : MessageUtils.formatTimeShort(type.getDefaultDuration())));
        lore.add("&7Cost: &6$" + MessageUtils.formatNumber(cost));
        lore.add("&8Royal prerogative: no cooldown applies.");
        lore.add("");

        if (active && activeOrder != null) {
            lore.add("&aRemaining time: &f" + MessageUtils.formatTime(activeOrder.getRemainingTime()));
        } else if (!canIssue) {
            lore.add("&8Only the King can issue executive orders.");
        } else if (!canAfford) {
            lore.add("&cInsufficient funds to issue.");
        } else {
            lore.add("&aClick &7→ Issue executive order");
        }

        return buildIcon(material, "&6&l" + type.getDisplayName() + " " + status, lore);
    }
}
