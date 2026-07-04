package id.nationcore.gui.communist;

import id.nationcore.gui.NationMenuBase;
import id.nationcore.NationCore;
import id.nationcore.models.CommunistDecisionType;
import id.nationcore.models.CommunistGovernment;
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

@SuppressWarnings("deprecation")
public class CommunistHealthOfficeGUI extends NationMenuBase {

    public static final String TITLE = "§c§lMINISTER OF HEALTH OFFICE";

    public CommunistHealthOfficeGUI(NationCore plugin) {
        super(plugin);
    }

    public void open(Player player, Nation nation) {
        if (nation == null) {
            player.closeInventory();
            MessageUtils.send(player, "<red>You are not in a nation.</red>");
            return;
        }

        CommunistGovernment gov = nation.getCommunistGovernment();
        if (gov == null) {
            player.closeInventory();
            MessageUtils.send(player, "<red>This menu is only available for Communist nations.</red>");
            return;
        }

        // Security check: only Secretary General, Health Minister, and Admins can open
        boolean isSekjen = gov.hasSecretaryGeneral() && gov.getSecretaryGeneralUUID().equals(player.getUniqueId());
        boolean isHealthMinister = gov.getPolitburoMember(CommunistGovernment.PolitburoPosition.HEALTH) != null
                && gov.getPolitburoMember(CommunistGovernment.PolitburoPosition.HEALTH).getUuid().equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("nation.admin");
        if (!isSekjen && !isHealthMinister && !isAdmin) {
            MessageUtils.send(player, "<red>You are not authorized to view this office console.</red>");
            player.closeInventory();
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // 1. FILLER (RED_STAINED_GLASS_PANE)
        int[] redSlots = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 17,
            18, 26,
            27, 35,
            36, 44,
            45, 46, 47, 48, 50, 51, 52, 53
        };
        ItemStack filler = pane(Material.RED_STAINED_GLASS_PANE);
        for (int slot : redSlots) {
            inv.setItem(slot, filler);
        }

        // 2. BACK (Slot 49)
        inv.setItem(49, buildIcon(Material.SPECTRAL_ARROW, "&7&l← Back to Government", "&7Return to the main government panel"));

        // 3. EXECUTIVE ORDERS (HEALTH SECTOR)
        // Slots: 20, 22, 24, 30, 32
        inv.setItem(20, buildDecisionItem(nation, CommunistDecisionType.HEA_QUARANTINE_PROTOCOL, player));
        inv.setItem(22, buildDecisionItem(nation, CommunistDecisionType.HEA_FIELD_MEDICINE, player));
        inv.setItem(24, buildDecisionItem(nation, CommunistDecisionType.HEA_VACCINATION_DRIVE, player));
        inv.setItem(30, buildDecisionItem(nation, CommunistDecisionType.HEA_EMERGENCY_RATIONS, player));
        inv.setItem(32, buildDecisionItem(nation, CommunistDecisionType.HEA_PLAGUE, player));

        player.openInventory(inv);
    }

    private ItemStack buildDecisionItem(Nation nation, CommunistDecisionType type, Player viewer) {
        CommunistGovernment gov = nation.getCommunistGovernment();
        if (gov == null) {
            return new ItemStack(Material.AIR);
        }
        UUID ministerUUID = gov.getPolitburoMember(type.getPosition()) != null
                ? gov.getPolitburoMember(type.getPosition()).getUuid()
                : null;

        boolean active = isCommunistDecisionStateActive(gov, type);
        long cooldownRemaining = plugin.getCommunistManager().getDecisionCooldownRemaining(
                nation, ministerUUID != null ? ministerUUID : viewer.getUniqueId(), type);
        boolean onCooldown = cooldownRemaining > 0;

        int cost = type.getCost();
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
        lore.add("&f" + type.getDescription());
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("&7Treasury Cost: &6$" + MessageUtils.formatNumber(cost));
        lore.add("&7Duration: &f" + (type.isInstant()
                ? "Instant"
                : MessageUtils.formatTimeShort(type.getDurationMillis())));

        if (active) {
            long remaining = 0;
            if (type == CommunistDecisionType.HEA_QUARANTINE_PROTOCOL) {
                remaining = gov.getQuarantineUntil() - System.currentTimeMillis();
            } else if (type == CommunistDecisionType.HEA_VACCINATION_DRIVE) {
                remaining = gov.getVaccinationUntil() - System.currentTimeMillis();
            } else if (type == CommunistDecisionType.HEA_PLAGUE) {
                remaining = gov.getPlagueUntil() - System.currentTimeMillis();
            }
            if (remaining > 0) {
                lore.add("&7Remaining Time: &a" + MessageUtils.formatTime(remaining));
            }
        } else if (onCooldown) {
            lore.add("&7Remaining Cooldown: &c" + MessageUtils.formatTime(cooldownRemaining));
        }

        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        boolean isAdmin = viewer.hasPermission("nation.admin");
        boolean isMinister = ministerUUID != null && ministerUUID.equals(viewer.getUniqueId());
        boolean isSekjen = gov != null && gov.hasSecretaryGeneral() && gov.getSecretaryGeneralUUID().equals(viewer.getUniqueId());

        if (active) {
            lore.add("&cThis order is currently active.");
        } else if (onCooldown) {
            lore.add("&cThis order is on cooldown.");
        } else if (!isMinister && !isSekjen && !isAdmin) {
            lore.add("&cOnly the " + type.getPosition().getDisplayName());
            lore.add("&c(or SecGen) can execute this order.");
        } else if (!canAfford) {
            lore.add("&cInsufficient treasury funds.");
        } else {
            lore.add("&eClick to execute order");
        }
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        ItemStack item = buildIcon(material, "&e&l" + type.getDisplayName(), lore);
        if (active) {
            item = glowing(item);
        }
        return item;
    }

    private boolean isCommunistDecisionStateActive(CommunistGovernment cg, CommunistDecisionType type) {
        return switch (type) {
            case HEA_QUARANTINE_PROTOCOL -> cg.isQuarantineActive();
            case HEA_VACCINATION_DRIVE -> cg.isVaccinationActive();
            case HEA_PLAGUE -> cg.isPlagueActive();
            default -> false;
        };
    }
}
