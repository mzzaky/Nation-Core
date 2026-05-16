package id.nationcore.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;
import id.nationcore.models.Government;
import id.nationcore.utils.MessageUtils;

public class GovernmentSalaryMenu {

    private final NationCore plugin;

    public GovernmentSalaryMenu(NationCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§2§l💰 SALARY & REWARDS 💰");
        Government gov = plugin.getDataManager().getGovernment();

        long cooldown = plugin.getGovernmentManager().getSalaryCooldown(player);
        boolean isPresident = gov.hasPresident() && gov.getPresidentUUID().equals(player.getUniqueId());
        Government.CabinetMember cabinetMember = gov.getCabinetMemberByUUID(player.getUniqueId());

        ItemStack claimItem;
        if (cooldown == 0) {
            List<String> lore = new ArrayList<>();
            lore.add("§7Your daily salary is ready!");
            lore.add("");
            lore.add("§8§m------------------------");
            lore.add("§b§lRewards:");

            if (isPresident) {
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
            } else if (cabinetMember != null) {
                Government.CabinetPosition position = cabinetMember.getPosition();
                String configPath = switch (position) {
                    case DEFENSE -> "cabinet.defense.daily-vault";
                    case TREASURY -> "cabinet.treasury-minister.daily-vault";
                    default -> "cabinet.daily-salary";
                };

                double vaultPoints = plugin.getConfig().getDouble(configPath, 30000);
                double salary = plugin.getConfig().getDouble("cabinet.daily-salary", 20000);
                double totalPay = vaultPoints + salary;

                lore.add("§7• §6" + MessageUtils.formatNumber((long) totalPay) + " §eVault Points");

                switch (position) {
                    case DEFENSE -> {
                        lore.add("§7• §b3x Diamond");
                        lore.add("§7• §e5x Golden Apple");
                    }
                    case TREASURY -> lore.add("§7• §a10x Emerald Block");
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
                    "§7Only President and",
                    "§7Cabinet Ministers may claim.");
        }
        inv.setItem(13, claimItem);

        inv.setItem(4, GovernmentGUIUtils.createItem(Material.PAPER, "§e§lAdministration Stats",
                "§7Total Salary Paid:",
                "§6" + MessageUtils.formatNumber((long) gov.getTotalSalaryPayouts())));

        inv.setItem(18, GovernmentGUIUtils.createItem(Material.ARROW, "§7§lBack", "§7Back to government menu"));

        GovernmentGUIUtils.fillGlass(inv);
        player.openInventory(inv);
    }
}
