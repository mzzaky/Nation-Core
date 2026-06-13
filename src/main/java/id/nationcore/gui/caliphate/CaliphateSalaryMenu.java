package id.nationcore.gui.caliphate;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;
import id.nationcore.gui.GovernmentGUIUtils;
import id.nationcore.models.CaliphateGovernment;
import id.nationcore.utils.MessageUtils;

public class CaliphateSalaryMenu {

    private final NationCore plugin;

    public CaliphateSalaryMenu(NationCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§2§l💰 SALARY & REWARDS 💰");
        id.nationcore.models.Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        CaliphateGovernment cg = nation != null && nation.getType() == id.nationcore.models.GovernmentType.CALIPHATE
                ? nation.getCaliphateGovernment()
                : null;

        if (cg == null) {
            player.closeInventory();
            return;
        }

        long cooldown = plugin.getCaliphateManager().getSalaryCooldown(player);
        boolean isCaliph = cg.hasCaliph() && cg.getCaliphUUID().equals(player.getUniqueId());

        ItemStack claimItem;
        if (cooldown == 0) {
            List<String> lore = new ArrayList<>();
            lore.add("§7Your daily salary is ready!");
            lore.add("");
            lore.add("§8§m------------------------");
            lore.add("§b§lRewards:");

            if (isCaliph) {
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
                    "§7Only Caliph may claim.");
        }
        inv.setItem(13, claimItem);

        inv.setItem(4, GovernmentGUIUtils.createItem(Material.PAPER, "§e§lAdministration Stats",
                "§7Total Subsidy Paid:",
                "§6" + MessageUtils.formatNumber((long) cg.getTotalSubsidyPayouts())));

        inv.setItem(18, GovernmentGUIUtils.createItem(Material.ARROW, "§7§lBack", "§7Back to government menu"));

        GovernmentGUIUtils.fillGlass(inv);
        player.openInventory(inv);
    }
}
