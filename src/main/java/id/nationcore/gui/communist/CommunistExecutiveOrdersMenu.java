package id.nationcore.gui.communist;

import id.nationcore.gui.GUIListener;
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
import id.nationcore.models.CommunistDecisionType;
import id.nationcore.models.CommunistGovernment;
import id.nationcore.models.CommunistGovernment.PolitburoMember;
import id.nationcore.models.CommunistGovernment.PolitburoPosition;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

public class CommunistExecutiveOrdersMenu extends NationMenuBase {

    public static final String TITLE = ChatColor.translateAlternateColorCodes('&',
            "&c&l☭ &4&lPolitburo Decisions");

    public static final int SLOT_BACK = 43;
    public static final int SLOT_CLOSE = 53; // kept for backwards compatibility in GUIListener

    private static final int[] FILLER_SLOTS = {
            0,1,2,3,5,6,7,8,9,17,18,26,27,35,36,44,45,46,52,53
    };

    private static final int[] DECISION_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42
    };

    private static final Map<UUID, CommunistOrderTab> activeTabs = new HashMap<>();

    public enum CommunistOrderTab {
        PROPAGANDA(PolitburoPosition.PROPAGANDA, "Minister of Propaganda", Material.TOTEM_OF_UNDYING, 47),
        TREASURY(PolitburoPosition.TREASURY, "Minister of Treasury", getYellowBundleOrBundle(), 48),
        SEKJEN(null, "Secretary General", Material.NETHER_STAR, 49),
        DEFENSE(PolitburoPosition.DEFENSE, "Minister of Defense", Material.GOLDEN_CHESTPLATE, 50),
        HEALTH(PolitburoPosition.HEALTH, "Minister of Health", Material.ENCHANTED_GOLDEN_APPLE, 51);

        private final PolitburoPosition pos;
        private final String displayName;
        private final Material material;
        private final int slot;

        CommunistOrderTab(PolitburoPosition pos, String displayName, Material material, int slot) {
            this.pos = pos;
            this.displayName = displayName;
            this.material = material;
            this.slot = slot;
        }

        public PolitburoPosition getPosition() { return pos; }
        public String getDisplayName() { return displayName; }
        public Material getMaterial() { return material; }
        public int getSlot() { return slot; }

        public static CommunistOrderTab getBySlot(int slot) {
            for (CommunistOrderTab tab : values()) {
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

    public CommunistExecutiveOrdersMenu(NationCore plugin) {
        super(plugin);
    }

    public static boolean handleTabClick(Player player, int slot) {
        CommunistOrderTab tab = CommunistOrderTab.getBySlot(slot);
        if (tab != null) {
            activeTabs.put(player.getUniqueId(), tab);
            return true;
        }
        return false;
    }

    public static CommunistDecisionType getDecisionAtSlot(Player player, int slot) {
        CommunistOrderTab tab = activeTabs.getOrDefault(player.getUniqueId(), CommunistOrderTab.SEKJEN);
        
        int index = -1;
        for (int i = 0; i < DECISION_SLOTS.length; i++) {
            if (DECISION_SLOTS[i] == slot) {
                index = i;
                break;
            }
        }
        if (index == -1) return null;

        List<CommunistDecisionType> decisions = getDecisionsForTab(tab);
        if (index < decisions.size()) {
            return decisions.get(index);
        }
        return null;
    }

    public static id.nationcore.models.ExecutiveOrder.ExecutiveOrderType getExecutiveOrderAtSlot(Player player, int slot) {
        CommunistOrderTab tab = activeTabs.getOrDefault(player.getUniqueId(), CommunistOrderTab.SEKJEN);
        if (tab != CommunistOrderTab.SEKJEN) return null;
        
        int index = -1;
        for (int i = 0; i < DECISION_SLOTS.length; i++) {
            if (DECISION_SLOTS[i] == slot) {
                index = i;
                break;
            }
        }
        if (index == -1) return null;

        id.nationcore.models.ExecutiveOrder.ExecutiveOrderType[] types = id.nationcore.models.ExecutiveOrder.ExecutiveOrderType.values();
        if (index < types.length) {
            return types[index];
        }
        return null;
    }

    private static List<CommunistDecisionType> getDecisionsForTab(CommunistOrderTab tab) {
        List<CommunistDecisionType> list = new ArrayList<>();
        if (tab == CommunistOrderTab.SEKJEN) {
            // SEKJEN displays Executive Orders instead
        } else {
            for (CommunistDecisionType type : CommunistDecisionType.values()) {
                if (type.getPosition() == tab.getPosition()) list.add(type);
            }
        }
        return list;
    }

    public void open(Player player, Nation nation) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        CommunistGovernment cg = nation.getCommunistGovernment();
        CommunistOrderTab activeTab = activeTabs.getOrDefault(player.getUniqueId(), CommunistOrderTab.SEKJEN);

        // 1. Filler
        ItemStack filler = pane(Material.RED_STAINED_GLASS_PANE);
        for (int slot : FILLER_SLOTS) {
            inv.setItem(slot, filler);
        }

        // 4. Stat negara (slot 4)
        inv.setItem(4, buildNationProfile(nation, cg));

        // 2. Back button
        inv.setItem(SLOT_BACK, buildIcon(Material.SPECTRAL_ARROW, "&e&l⮜ Back", "&7Return to main menu"));

        // Navigation Menu Session
        for (CommunistOrderTab tab : CommunistOrderTab.values()) {
            inv.setItem(tab.getSlot(), buildTabIcon(tab, activeTab == tab));
        }

        // Executive Orders or Decisions in empty slots
        if (activeTab == CommunistOrderTab.SEKJEN) {
            id.nationcore.models.ExecutiveOrder.ExecutiveOrderType[] types = id.nationcore.models.ExecutiveOrder.ExecutiveOrderType.values();
            for (int i = 0; i < types.length; i++) {
                if (i < DECISION_SLOTS.length) {
                    inv.setItem(DECISION_SLOTS[i], buildExecutiveOrderCard(nation, cg, player, types[i]));
                }
            }
        } else {
            List<CommunistDecisionType> decisions = getDecisionsForTab(activeTab);
            for (int i = 0; i < decisions.size(); i++) {
                if (i < DECISION_SLOTS.length) {
                    inv.setItem(DECISION_SLOTS[i], buildDecisionCard(nation, cg, player, decisions.get(i)));
                }
            }
        }

        player.openInventory(inv);
    }

    private ItemStack buildNationProfile(Nation nation, CommunistGovernment cg) {
        int memberCount = nation.getMemberCount();
        int partyCount = cg != null ? cg.getPartyMemberCount() : 0;

        return buildIcon(Material.GLOW_ITEM_FRAME,
                "&c&l" + nation.getName(),
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&7Government : &fCommunist",
                "&7Tag        : &f[" + nation.getTag() + "]",
                "&7Members    : &f" + memberCount,
                "&7Party      : &f" + partyCount,
                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&8Displays information about the nation");
    }

    private ItemStack buildTabIcon(CommunistOrderTab tab, boolean isActive) {
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

        ItemStack item = buildIcon(tab.getMaterial(), (isActive ? "&a&l" : "&c&l") + tab.getDisplayName(), lore);
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

    private ItemStack buildDecisionCard(Nation nation, CommunistGovernment cg,
                                        Player viewer, CommunistDecisionType type) {
        boolean isAdmin = viewer.hasPermission("nation.admin");
        PolitburoMember member = cg != null ? cg.getPolitburoMember(type.getPosition()) : null;
        boolean isHolder = member != null && member.getUuid().equals(viewer.getUniqueId());
        boolean canIssue = isHolder || isAdmin;

        long cooldownRemaining = plugin.getCommunistManager()
                .getDecisionCooldownRemaining(nation, viewer.getUniqueId(), type);
        boolean onCooldown = cooldownRemaining > 0;
        boolean canAfford = plugin.getTreasuryManager().canAfford(nation, type.getCost());
        boolean active = cg != null && isCommunistDecisionStateActive(cg, type);

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
        if (member != null) {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(member.getUuid());
            if (op.getName() != null) {
                ministerName = op.getName();
            }
        }

        List<String> lore = new ArrayList<>();
        lore.add("&7" + type.getDescription());
        lore.add("");
        lore.add("&7Sector: &f" + (type.getPosition() != null ? type.getPosition().getDisplayName() : "General"));
        lore.add("&7Minister: &f" + ministerName);
        lore.add("&7Cost: &6$" + MessageUtils.formatNumber(type.getCost()));
        lore.add("&7Duration: &f" + (type.isInstant()
                ? "instant"
                : MessageUtils.formatTimeShort(type.getDurationMillis())));
        lore.add("");

        if (active) {
            lore.add("&aActive in this nation.");
        } else if (onCooldown) {
            lore.add("&cCooldown: &f" + MessageUtils.formatTime(cooldownRemaining));
        } else if (!canIssue) {
            lore.add("&8Only the " + (type.getPosition() != null ? type.getPosition().getDisplayName() : "Secretary General"));
            lore.add("&8can execute this.");
        } else if (!canAfford) {
            lore.add("&cInsufficient funds in " + nation.getName() + ".");
        } else {
            lore.add("&aClick &7→ Execute decision");
        }

        return buildIcon(material, "&6&l" + type.getDisplayName() + " " + status, lore);
    }

    private ItemStack buildExecutiveOrderCard(Nation nation, CommunistGovernment cg, Player viewer, id.nationcore.models.ExecutiveOrder.ExecutiveOrderType type) {
        boolean isSekjen = cg != null && cg.hasSecretaryGeneral()
                && cg.getSecretaryGeneralUUID().equals(viewer.getUniqueId());
        boolean isAdmin = viewer.hasPermission("nation.admin");
        boolean canIssue = isSekjen || isAdmin;

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

        String sekjenName = "Vacant";
        if (cg != null && cg.hasSecretaryGeneral()) {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(cg.getSecretaryGeneralUUID());
            if (op.getName() != null) {
                sekjenName = op.getName();
            }
        }

        List<String> lore = new ArrayList<>();
        lore.add("&7" + type.getFlavorText());
        lore.add("");
        lore.add("&7Secretary General: &f" + sekjenName);
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
            lore.add("&8Only the Secretary General can issue orders.");
        } else if (!canAfford) {
            lore.add("&cInsufficient funds to issue.");
        } else {
            lore.add("&aClick &7→ Issue executive order");
        }

        return buildIcon(material, "&6&l" + type.getDisplayName() + " " + status, lore);
    }

    private boolean isCommunistDecisionStateActive(CommunistGovernment cg, CommunistDecisionType type) {
        return switch (type) {
            case PROP_LEADER_GLORIFICATION -> cg.isGlorificationActive();
            case PROP_MEDIA_CENSORSHIP -> cg.isSensorMediaActive();
            case DEF_DEFENSE_PROTOCOL -> cg.isDefenseProtocolActive();
            case DEF_OFFENSE_PROTOCOL -> cg.isOffenseProtocolActive();
            case DEF_MILITARY_EMERGENCY -> cg.isMilitaryEmergencyActive();
            case TRE_MARKET_EVENT -> cg.isMarketEventActive();
            case TRE_DISTRIBUTION_PROGRAM -> cg.getDistributionProgramPhasesLeft() > 0;
            case TRE_TAX_INTENSIFICATION -> cg.getTaxIntensificationPhasesLeft() > 0;
            case HEA_QUARANTINE_PROTOCOL -> cg.isQuarantineActive();
            case HEA_VACCINATION_DRIVE -> cg.isVaccinationActive();
            case HEA_PLAGUE -> cg.isPlagueActive();
            default -> false;
        };
    }
}
