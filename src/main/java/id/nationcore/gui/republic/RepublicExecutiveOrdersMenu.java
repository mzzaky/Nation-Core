package id.nationcore.gui.republic;

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
import id.nationcore.models.CabinetDecision;
import id.nationcore.models.Government;
import id.nationcore.models.ExecutiveOrder;
import id.nationcore.models.ExecutiveOrder.ExecutiveOrderType;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

public class RepublicExecutiveOrdersMenu extends NationMenuBase {

    public static final String TITLE = ChatColor.translateAlternateColorCodes('&',
            "&9&l⚖ &b&lRepublic Executive Orders");

    public static final int SLOT_BACK = 43;
    public static final int SLOT_CLOSE = 53; // kept for backwards compatibility in GUIListener

    private static final int[] FILLER_SLOTS = {
            0,1,2,3,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,52,53
    };

    private static final int[] DECISION_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42
    };

    private static final Map<UUID, RepublicOrderTab> activeTabs = new HashMap<>();

    public enum RepublicOrderTab {
        DEFENSE(Government.CabinetPosition.DEFENSE, "Minister of Defense", Material.GOLDEN_CHESTPLATE, 48),
        PRESIDENT(null, "President", Material.NETHER_STAR, 49),
        TREASURY(Government.CabinetPosition.TREASURY, "Minister of Treasury", getYellowBundleOrBundle(), 50),
        HEALTH(Government.CabinetPosition.HEALTH, "Minister of Health", Material.ENCHANTED_GOLDEN_APPLE, 51);

        private final Government.CabinetPosition pos;
        private final String displayName;
        private final Material material;
        private final int slot;

        RepublicOrderTab(Government.CabinetPosition pos, String displayName, Material material, int slot) {
            this.pos = pos;
            this.displayName = displayName;
            this.material = material;
            this.slot = slot;
        }

        public Government.CabinetPosition getPosition() { return pos; }
        public String getDisplayName() { return displayName; }
        public Material getMaterial() { return material; }
        public int getSlot() { return slot; }

        public static RepublicOrderTab getBySlot(int slot) {
            for (RepublicOrderTab tab : values()) {
                if (tab.getSlot() == slot) return tab;
            }
            return null;
        }

        private static Material getYellowBundleOrBundle() {
            try {
                return Material.valueOf("YELLOW_BUNDLE");
            } catch (Exception e) {
                return Material.BUNDLE;
            }
        }
    }

    public RepublicExecutiveOrdersMenu(NationCore plugin) {
        super(plugin);
    }

    public static boolean handleTabClick(Player player, int slot) {
        RepublicOrderTab tab = RepublicOrderTab.getBySlot(slot);
        if (tab != null) {
            activeTabs.put(player.getUniqueId(), tab);
            return true;
        }
        return false;
    }

    public static CabinetDecision.DecisionType getDecisionAtSlot(Player player, int slot) {
        RepublicOrderTab tab = activeTabs.getOrDefault(player.getUniqueId(), RepublicOrderTab.PRESIDENT);
        
        int index = -1;
        for (int i = 0; i < DECISION_SLOTS.length; i++) {
            if (DECISION_SLOTS[i] == slot) {
                index = i;
                break;
            }
        }
        if (index == -1) return null;

        List<CabinetDecision.DecisionType> decisions = getDecisionsForTab(tab);
        if (index < decisions.size()) {
            return decisions.get(index);
        }
        return null;
    }

    public static ExecutiveOrderType getExecutiveOrderAtSlot(Player player, int slot) {
        RepublicOrderTab tab = activeTabs.getOrDefault(player.getUniqueId(), RepublicOrderTab.PRESIDENT);
        if (tab != RepublicOrderTab.PRESIDENT) return null;
        
        int index = -1;
        for (int i = 0; i < DECISION_SLOTS.length; i++) {
            if (DECISION_SLOTS[i] == slot) {
                index = i;
                break;
            }
        }
        if (index == -1) return null;

        ExecutiveOrderType[] types = ExecutiveOrderType.values();
        if (index < types.length) {
            return types[index];
        }
        return null;
    }

    private static List<CabinetDecision.DecisionType> getDecisionsForTab(RepublicOrderTab tab) {
        List<CabinetDecision.DecisionType> list = new ArrayList<>();
        if (tab == RepublicOrderTab.PRESIDENT) {
            // PRESIDENT tab displays Executive Orders instead
        } else {
            for (CabinetDecision.DecisionType type : CabinetDecision.DecisionType.values()) {
                if (type.getPosition().name().equals(tab.getPosition().name())) {
                    list.add(type);
                }
            }
        }
        return list;
    }

    public void open(Player player, Nation nation) {
        if (nation == null) {
            player.closeInventory();
            MessageUtils.send(player, "&cYou are not in a nation.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        Government gov = nation.getRepublicGovernment();
        RepublicOrderTab activeTab = activeTabs.getOrDefault(player.getUniqueId(), RepublicOrderTab.PRESIDENT);

        // 1. Filler
        ItemStack filler = pane(Material.BLUE_STAINED_GLASS_PANE);
        for (int slot : FILLER_SLOTS) {
            inv.setItem(slot, filler);
        }

        // 4. Stat negara (slot 4)
        inv.setItem(4, buildNationProfile(nation, gov));

        // 2. Back button
        inv.setItem(SLOT_BACK, buildIcon(Material.SPECTRAL_ARROW, "&e&l⮜ Back", "&7Return to main menu"));

        // Navigation Menu Session
        for (RepublicOrderTab tab : RepublicOrderTab.values()) {
            inv.setItem(tab.getSlot(), buildTabIcon(tab, activeTab == tab));
        }

        // Executive Orders or Decisions in empty slots
        if (activeTab == RepublicOrderTab.PRESIDENT) {
            ExecutiveOrderType[] types = ExecutiveOrderType.values();
            for (int i = 0; i < types.length; i++) {
                if (i < DECISION_SLOTS.length) {
                    inv.setItem(DECISION_SLOTS[i], buildExecutiveOrderCard(nation, gov, player, types[i]));
                }
            }
        } else {
            List<CabinetDecision.DecisionType> decisions = getDecisionsForTab(activeTab);
            for (int i = 0; i < decisions.size(); i++) {
                if (i < DECISION_SLOTS.length) {
                    inv.setItem(DECISION_SLOTS[i], buildDecisionCard(nation, gov, player, decisions.get(i)));
                }
            }
        }

        player.openInventory(inv);
    }

    private ItemStack buildNationProfile(Nation nation, Government gov) {
        int memberCount = nation.getMemberCount();
        int cabinetCount = 0;
        if (gov != null) {
            for (Government.CabinetPosition pos : Government.CabinetPosition.values()) {
                if (gov.getCabinetMember(pos) != null) {
                    cabinetCount++;
                }
            }
        }

        return buildIcon(Material.GLOW_ITEM_FRAME,
                "&b&l" + nation.getName(),
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&7Government : &fRepublic",
                "&7Tag        : &f[" + nation.getTag() + "]",
                "&7Members    : &f" + memberCount,
                "&7Cabinet    : &f" + cabinetCount + " / 3",
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&8Displays information about the nation");
    }

    private ItemStack buildTabIcon(RepublicOrderTab tab, boolean isActive) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Category: &f" + tab.getDisplayName());
        if (isActive) {
            lore.add("");
            lore.add("&a▶ Currently viewing");
        } else {
            lore.add("");
            lore.add("&eClick to view decisions");
            lore.add("&e" + tab.getDisplayName());
        }

        ItemStack item = buildIcon(tab.getMaterial(), (isActive ? "&a&l" : "&b&l") + tab.getDisplayName(), lore);
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

    private ItemStack buildDecisionCard(Nation nation, Government gov,
                                        Player viewer, CabinetDecision.DecisionType type) {
        boolean isAdmin = viewer.hasPermission("nation.admin");
        UUID ministerUUID = gov != null ? gov.getCabinetMember(type.getPosition().toGovernmentPosition()) : null;
        boolean isHolder = ministerUUID != null && ministerUUID.equals(viewer.getUniqueId());
        boolean canIssue = isHolder || (isAdmin && ministerUUID != null);

        long cooldownRemaining = plugin.getCabinetManager().getRemainingCooldown(ministerUUID != null ? ministerUUID : viewer.getUniqueId(), type);
        boolean onCooldown = cooldownRemaining > 0;
        
        int cost = plugin.getCabinetManager().getDecisionCost(type);
        boolean canAfford = plugin.getTreasuryManager().canAfford(nation, cost);
        boolean active = plugin.getCabinetManager().isDecisionActive(nation, type);

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

        String ministerName = "Vacant";
        if (ministerUUID != null) {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(ministerUUID);
            if (op.getName() != null) {
                ministerName = op.getName();
            }
        }

        List<String> lore = new ArrayList<>();
        lore.add("&7" + type.getDescription());
        lore.add("");
        lore.add("&7Sector: &f" + type.getPosition().getDisplayName());
        lore.add("&7Minister: &f" + ministerName);
        lore.add("&7Cost: &6$" + MessageUtils.formatNumber(cost));
        lore.add("&7Duration: &f" + (type.getDurationMillis() == 0
                ? "instant"
                : MessageUtils.formatTimeShort(type.getDurationMillis())));
        lore.add("");

        if (active) {
            CabinetDecision activeDecision = nation != null ? nation.getActiveDecisions().stream()
                    .filter(d -> d.getType() == type && d.isActive() && !d.isExpired())
                    .findFirst().orElse(null) : null;
            long remainingTime = activeDecision != null ? activeDecision.getRemainingTime() : 0;
            lore.add("&aActive in this nation.");
            if (remainingTime > 0) {
                lore.add("&aRemaining time: &f" + MessageUtils.formatTime(remainingTime));
            }
        } else if (onCooldown) {
            lore.add("&cCooldown: &f" + MessageUtils.formatTime(cooldownRemaining));
        } else if (!canIssue) {
            lore.add("&8Only the " + type.getPosition().getDisplayName());
            lore.add("&8can execute this.");
        } else if (!canAfford) {
            lore.add("&cInsufficient funds in " + nation.getName() + ".");
        } else {
            lore.add("&aClick &7→ Execute decision");
        }

        return buildIcon(material, "&6&l" + type.getDisplayName() + " " + status, lore);
    }

    private ItemStack buildExecutiveOrderCard(Nation nation, Government gov, Player viewer, ExecutiveOrderType type) {
        boolean isPresident = gov != null && gov.hasPresident()
                && gov.getPresidentUUID().equals(viewer.getUniqueId());
        boolean isAdmin = viewer.hasPermission("nation.admin");
        boolean canIssue = isPresident || isAdmin;

        boolean active = plugin.getExecutiveOrderManager().isOrderActive(nation, type);
        boolean onCooldown = !active && plugin.getExecutiveOrderManager().isOrderOnCooldown(nation, type);
        long cost = plugin.getConfig().getLong("executive-orders.cost", 1_000_000);
        long cooldownRemaining = onCooldown ? plugin.getExecutiveOrderManager().getOrderCooldownRemaining(nation, type) : 0L;
        boolean canAfford = plugin.getTreasuryManager().canAfford(nation, cost);
        id.nationcore.models.ExecutiveOrder activeOrder = active ? plugin.getExecutiveOrderManager().getActiveOrder(nation, type) : null;

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

        String presidentName = "Vacant";
        if (gov != null && gov.hasPresident()) {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(gov.getPresidentUUID());
            if (op.getName() != null) {
                presidentName = op.getName();
            }
        }

        List<String> lore = new ArrayList<>();
        lore.add("&7" + type.getFlavorText());
        lore.add("");
        lore.add("&7President: &f" + presidentName);
        lore.add("&6&lEffects:");
        lore.add("&7" + type.getEffectDescription());
        lore.add("");
        lore.add("&7Duration: &f" + (type.getDefaultDuration() == 0
                ? "instant"
                : MessageUtils.formatTimeShort(type.getDefaultDuration())));
        lore.add("&7Cost: &6$" + MessageUtils.formatNumber(cost));
        lore.add("");

        if (active && activeOrder != null) {
            lore.add("&aRemaining time: &f" + MessageUtils.formatTime(activeOrder.getRemainingTime()));
        } else if (onCooldown) {
            lore.add("&cCooldown: &f" + MessageUtils.formatTime(cooldownRemaining));
        } else if (!canIssue) {
            lore.add("&8Only the President can issue orders.");
        } else if (!canAfford) {
            lore.add("&cInsufficient funds to issue.");
        } else {
            lore.add("&aClick &7→ Issue executive order");
        }

        return buildIcon(material, "&6&l" + type.getDisplayName() + " " + status, lore);
    }
}
