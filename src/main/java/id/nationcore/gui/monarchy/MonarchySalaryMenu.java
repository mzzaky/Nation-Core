package id.nationcore.gui.monarchy;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;
import id.nationcore.gui.GovernmentGUIUtils;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.MonarchyGovernment;
import id.nationcore.utils.MessageUtils;

public class MonarchySalaryMenu {

    private final NationCore plugin;

    public MonarchySalaryMenu(NationCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§2§l💰 SALARY & REWARDS 💰");
        id.nationcore.models.Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        MonarchyGovernment mg = nation != null && nation.getType() == id.nationcore.models.GovernmentType.MONARCHY
                ? nation.getMonarchyGovernment()
                : null;

        if (mg == null) {
            player.closeInventory();
            return;
        }

        long cooldown = plugin.getMonarchyManager().getSalaryCooldown(player);
        boolean isKing = mg.hasKing() && mg.getKingUUID().equals(player.getUniqueId());
        MonarchyGovernment.HighCouncilMember member = mg.getCouncilMemberByUUID(player.getUniqueId());

        ItemStack claimItem;
        if (cooldown == 0) {
            List<String> lore = new ArrayList<>();
            lore.add("§7Your daily salary is ready!");
            lore.add("");
            lore.add("§8§m------------------------");
            lore.add("§b§lRewards:");

            if (isKing) {
                ConfigurationSection rewardsSection = plugin.getConfig()
                        .getConfigurationSection("president.daily-rewards");
                if (rewardsSection != null) {
                    for (String key : rewardsSection.getKeys(false)) {
                        if (key.equalsIgnoreCase("vault-points")) {
                            double vaultPoints = rewardsSection.getDouble(key);
                            if (vaultPoints > 0) {
                                lore.add("§7• §6" + MessageUtils.formatNumber((long) vaultPoints) + " §eVault Points");
                            }
                        } else {
                            int amount = rewardsSection.getInt(key);
                            if (amount > 0) {
                                String[] words = key.split("-");
                                StringBuilder itemName = new StringBuilder();
                                for (String word : words) {
                                    if (!word.isEmpty()) {
                                        itemName.append(Character.toUpperCase(word.charAt(0)))
                                                .append(word.substring(1).toLowerCase()).append(" ");
                                    }
                                }
                                lore.add("§7• §b" + amount + "x " + itemName.toString().trim());
                            }
                        }
                    }
                }
            } else if (member != null) {
                MonarchyGovernment.HighCouncilPosition position = member.getPosition();
                String configPath = switch (position) {
                    case MARSHAL -> "cabinet.defense.daily-vault";
                    case CHANCELLOR -> "cabinet.treasury.daily-vault";
                    case SAINT -> "cabinet.health.daily-vault";
                    default -> "cabinet.daily-salary";
                };

                YamlConfiguration nationConfig = plugin.getNationConfig(GovernmentType.MONARCHY);
                double vaultPoints = nationConfig != null ? nationConfig.getDouble(configPath, 30000) : 30000;
                double salary = nationConfig != null ? nationConfig.getDouble("cabinet.daily-salary", 20000) : 20000;
                double totalPay = vaultPoints + salary;

                lore.add("§7• §6" + MessageUtils.formatNumber((long) totalPay) + " §eVault Points");

                switch (position) {
                    case MARSHAL -> {
                        lore.add("§7• §b3x Diamond");
                        lore.add("§7• §e5x Golden Apple");
                    }
                    case CHANCELLOR -> lore.add("§7• §a10x Emerald Block");
                    default -> {}
                }
            }
            lore.add("§8§m------------------------");
            lore.add("");
            lore.add("§eClick to claim!");

            claimItem = GovernmentGUIUtils.createItem(Material.EMERALD_BLOCK, "§a§lCLAIM SALARY", lore.toArray(new String[0]));
        } else if (cooldown > 0) {
            long hours = cooldown / (60 * 60 * 1000);
            long minutes = (cooldown % (60 * 60 * 1000)) / (60 * 1000);
            claimItem = GovernmentGUIUtils.createItem(Material.RED_CONCRETE, "§c§lNEXT SALARY",
                    "§7Available in:",
                    "§f" + String.format("%02dh %02dm", hours, minutes));
        } else {
            claimItem = GovernmentGUIUtils.createItem(Material.BARRIER, "§c§lNOT ELIGIBLE",
                    "§7Only King and",
                    "§7High Council may claim.");
        }
        inv.setItem(13, claimItem);

        inv.setItem(4, GovernmentGUIUtils.createItem(Material.PAPER, "§e§lAdministration Stats",
                "§7Total Subsidy Paid:",
                "§6" + MessageUtils.formatNumber((long) mg.getTotalSubsidyPayouts())));

        inv.setItem(18, GovernmentGUIUtils.createItem(Material.ARROW, "§7§lBack", "§7Back to government menu"));

        GovernmentGUIUtils.fillGlass(inv);
        player.openInventory(inv);
    }
}
