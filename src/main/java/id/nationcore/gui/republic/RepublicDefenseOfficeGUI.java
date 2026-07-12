package id.nationcore.gui.republic;

import id.nationcore.gui.NationMenuBase;
import id.nationcore.NationCore;
import id.nationcore.models.CabinetDecision;
import id.nationcore.models.Government;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RepublicDefenseOfficeGUI extends NationMenuBase {

    public static final String TITLE = "§9§lMINISTER OF DEFENSE OFFICE";

    public RepublicDefenseOfficeGUI(NationCore plugin) {
        super(plugin);
    }

    public void open(Player player, Nation nation) {
        if (nation == null) {
            player.closeInventory();
            MessageUtils.send(player, "<red>You are not in a nation.</red>");
            return;
        }

        Government gov = nation.getRepublicGovernment();
        if (gov == null) {
            player.closeInventory();
            MessageUtils.send(player, "<red>This menu is only available for Republic nations.</red>");
            return;
        }

        // Security check: only President, Defense Minister, and Admins can open
        boolean isPresident = gov.hasPresident() && gov.getPresidentUUID().equals(player.getUniqueId());
        boolean isDefenseMinister = gov.getCabinetMember(Government.CabinetPosition.DEFENSE) != null && gov.getCabinetMember(Government.CabinetPosition.DEFENSE).equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("nation.admin");
        if (!isPresident && !isDefenseMinister && !isAdmin) {
            MessageUtils.send(player, "<red>You are not authorized to view this office console.</red>");
            player.closeInventory();
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // 1. FILLER
        int[] lightBlueSlots = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 17,
            18, 26,
            27, 35,
            36, 44,
            45, 46, 47, 48, 50, 51, 52, 53
        };
        ItemStack filler = pane(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        for (int slot : lightBlueSlots) {
            inv.setItem(slot, filler);
        }

        // 2. BACK (Slot 49)
        inv.setItem(49, buildIcon(Material.SPECTRAL_ARROW, "&7&l← Back to Government", "&7Return to the main government panel"));

        // 3. EXECUTIVE ORDERS (DEFENSE SECTOR)
        // Slots: 20, 22, 24, 30, 32
        org.bukkit.configuration.file.YamlConfiguration config = plugin.getNationConfig(nation.getType());
        List<String> orderKeys = config != null ? config.getStringList("executive_order.minister_of_defence") : new ArrayList<>();
        int[] decisionSlots = {20, 22, 24, 30, 32};
        int index = 0;
        for (String key : orderKeys) {
            if (index >= decisionSlots.length) break;
            try {
                CabinetDecision.DecisionType type = CabinetDecision.DecisionType.valueOf(key.trim().toUpperCase());
                placeDecision(inv, decisionSlots[index], nation, type, player);
                index++;
            } catch (IllegalArgumentException e) {
                // skip
            }
        }

        // 4. EXECUTIVE ORDERS this office may issue (from executive_order.minister_of_defence)
        boolean isMinister = gov.getCabinetMember(Government.CabinetPosition.DEFENSE) != null
                && gov.getCabinetMember(Government.CabinetPosition.DEFENSE).equals(player.getUniqueId());
        boolean canIssue = isMinister || player.hasPermission("nation.admin");
        int[] eoSlots = id.nationcore.managers.ExecutiveOrderManager.OFFICE_ORDER_SLOTS;
        List<id.nationcore.models.ExecutiveOrder.ExecutiveOrderType> orders =
                plugin.getExecutiveOrderManager().getOrdersForPosition(nation.getType(), "minister_of_defence");
        for (int i = 0; i < orders.size() && i < eoSlots.length; i++) {
            inv.setItem(eoSlots[i], buildExecutiveOrderCard(nation, orders.get(i), canIssue, "Minister of Defence", "&b"));
        }

        player.openInventory(inv);
     }

     /** Only render a sector decision that is enabled in order.yaml and listed under this office. */
     private void placeDecision(Inventory inv, int slot, Nation nation, CabinetDecision.DecisionType type, Player player) {
         if (plugin.getExecutiveOrderManager().isSectorOrderVisible(
                 nation.getType(), "minister_of_defence", type.name().toLowerCase())) {
             inv.setItem(slot, buildDecisionItem(nation, type, player));
         }
     }

    private ItemStack buildDecisionItem(Nation nation, CabinetDecision.DecisionType type, Player viewer) {
        Government gov = nation.getRepublicGovernment();
        UUID ministerUUID = gov != null ? gov.getCabinetMember(type.getPosition().toGovernmentPosition()) : null;

        boolean active = plugin.getCabinetManager().isDecisionActive(nation, type);
        long cooldownRemaining = plugin.getCabinetManager().getRemainingCooldown(
                ministerUUID != null ? ministerUUID : viewer.getUniqueId(), type);
        boolean onCooldown = cooldownRemaining > 0;

        int cost = plugin.getCabinetManager().getDecisionCost(type);
        boolean canAfford = plugin.getTreasuryManager().canAfford(nation, cost);

        Material material;
        String statusText;
        if (active) {
            material = Material.ENCHANTED_BOOK;
            statusText = "&a● Active";
        } else if (onCooldown) {
            material = Material.BOOK;
            statusText = "&c● Cooldown";
        } else {
            material = Material.WRITABLE_BOOK;
            statusText = "&e● Available";
        }

        List<String> lore = new ArrayList<>();
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("&7Sector: &f" + type.getPosition().getDisplayName());
        lore.add("&7Status: " + statusText);
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("&7Description:");
        for (String descLine : plugin.getExecutiveOrderManager().getOrderLore(
                type.name().toLowerCase(), java.util.List.of(type.getDescription()))) {
            lore.add("&f" + descLine);
        }
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("&7Treasury Cost: &6$" + MessageUtils.formatNumber(cost));
        lore.add("&7Duration: &f" + (type.getDurationMillis() == 0
                ? "Instant"
                : MessageUtils.formatTimeShort(type.getDurationMillis())));

        if (active) {
            CabinetDecision activeDecision = nation.getActiveDecisions().stream()
                    .filter(d -> d.getType() == type && d.isActive() && !d.isExpired())
                    .findFirst().orElse(null);
            long remaining = activeDecision != null ? activeDecision.getRemainingTime() : 0;
            if (remaining > 0) {
                lore.add("&7Remaining Time: &a" + MessageUtils.formatTime(remaining));
            }
        } else if (onCooldown) {
            lore.add("&7Remaining Cooldown: &c" + MessageUtils.formatTime(cooldownRemaining));
        }

        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        boolean isAdmin = viewer.hasPermission("nation.admin");
        boolean isMinister = ministerUUID != null && ministerUUID.equals(viewer.getUniqueId());

        if (active) {
            lore.add("&cThis order is currently active.");
        } else if (onCooldown) {
            lore.add("&cThis order is on cooldown.");
        } else if (!isMinister && !isAdmin) {
            lore.add("&cOnly the " + type.getPosition().getDisplayName());
            lore.add("&ccan execute this order.");
        } else if (!canAfford) {
            lore.add("&cInsufficient treasury funds.");
        } else {
            lore.add("&eClick to execute order");
        }
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        String display = plugin.getExecutiveOrderManager().getOrderDisplay(type.name().toLowerCase(), type.getDisplayName());
        ItemStack item = buildIcon(material, "&e&l" + display, lore);
        if (active) {
            item = glowing(item);
        }
        return item;
    }
}
