package id.nationcore.gui.communist;

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
import id.nationcore.models.CommunistGovernment;
import id.nationcore.models.GovernmentType;
import id.nationcore.utils.MessageUtils;

public class CommunistSalaryMenu {

    private final NationCore plugin;

    public CommunistSalaryMenu(NationCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§2§l💰 SALARY & REWARDS 💰");
        id.nationcore.models.Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        CommunistGovernment cg = nation != null && nation.getType() == id.nationcore.models.GovernmentType.COMMUNIST
                ? nation.getCommunistGovernment()
                : null;

        if (cg == null) {
            player.closeInventory();
            return;
        }

        long cooldown = plugin.getCommunistManager().getSalaryCooldown(player);
        boolean isSekjen = cg.hasSecretaryGeneral() && cg.getSecretaryGeneralUUID().equals(player.getUniqueId());
        CommunistGovernment.PolitburoMember member = cg.getPolitburoMemberByUUID(player.getUniqueId());

        ItemStack claimItem;
        if (cooldown == 0) {
            List<String> lore = new ArrayList<>();
            lore.add("§7Your daily salary is ready!");
            lore.add("");
            lore.add("§8§m------------------------");
            lore.add("§b§lRewards:");

            if (isSekjen) {
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
                CommunistGovernment.PolitburoPosition position = member.getPosition();
                String configPath = switch (position) {
                    case PROPAGANDA -> "politbiro.propaganda.daily-vault";
                    case DEFENSE -> "politbiro.defense.daily-vault";
                    case TREASURY -> "politbiro.treasury.daily-vault";
                    case HEALTH -> "politbiro.health.daily-vault";
                };

                YamlConfiguration nationConfig = plugin.getNationConfig(GovernmentType.COMMUNIST);
                double vaultPoints = nationConfig != null ? nationConfig.getDouble(configPath, 25000) : 25000;
                double salary = nationConfig != null ? nationConfig.getDouble("politbiro.daily-salary", 20000) : 20000;
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
                    "§7Only Secretary General and",
                    "§7Politburo may claim.");
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
