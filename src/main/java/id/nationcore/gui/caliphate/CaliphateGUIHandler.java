package id.nationcore.gui.caliphate;

import id.nationcore.gui.GUIListener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;
import id.nationcore.models.CaliphateGovernment;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

/**
 * Handles GUI clicks specific to Caliphate nations:
 * main menu, caliphate court, executive orders, Bayt al-Mal.
 */
public class CaliphateGUIHandler {

    private final NationCore plugin;
    private final GUIListener gui;

    public CaliphateGUIHandler(NationCore plugin, GUIListener gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    public void handleMainMenu(Player player, ItemStack clicked, int slot) {
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (CaliphateMainMenu.isFiller(clicked.getType()))
            return;

        MessageUtils.playSound(player, org.bukkit.Sound.UI_BUTTON_CLICK);

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        CaliphateGovernment cg = nation != null ? nation.getCaliphateGovernment() : null;
        boolean isCaliph = cg != null && cg.hasCaliph() && cg.getCaliphUUID().equals(player.getUniqueId());
        boolean isShura = cg != null && cg.isShuraMember(player.getUniqueId());
        boolean isScholar = cg != null && cg.isScholar(player.getUniqueId());
        boolean isAdmin = player.hasPermission("nation.admin");
        boolean inLeadership = isCaliph || isShura || isScholar || isAdmin;

        if (slot == CaliphateMainMenu.getSlot("CALIPH")) {
            if (!inLeadership) {
                MessageUtils.send(player,
                        "<red>Only the Caliph, Shura Council, and State Scholars may enter the Caliphate Court.</red>");
                return;
            }
            gui.openGovernmentGUI(player);
            return;
        }

        if (slot == CaliphateMainMenu.getSlot("TREASURY")) {
            if (!inLeadership) {
                MessageUtils.send(player,
                        "<red>Only the Caliph, Shura Council, and State Scholars may inspect Bayt al-Mal.</red>");
                return;
            }
            gui.caliphateTreasuryMenu.open(player, nation);
            return;
        }

        if (slot == CaliphateMainMenu.getSlot("ZAKAT")) {
            gui.caliphateZakahMenu.open(player);
            return;
        }

        if (slot == CaliphateMainMenu.getSlot("RESEARCH")) {
            gui.researchGUI.openMain(player);
            return;
        }

        if (slot == CaliphateMainMenu.getSlot("CAPITAL_CITY")) {
            if (nation == null) {
                MessageUtils.send(player, "<red>You are not in a nation.</red>");
                player.closeInventory();
                return;
            }
            if (!nation.hasCapital()) {
                MessageUtils.send(player, "<red>Your nation has not established a capital yet.</red>");
                player.closeInventory();
                return;
            }

            long now = System.currentTimeMillis();
            long cooldownMs = 15L * 60 * 1000;
            Long lastTeleport = gui.capitalTeleportCooldowns.get(player.getUniqueId());
            if (lastTeleport != null && (now - lastTeleport) < cooldownMs && !player.hasPermission("nation.admin")) {
                long remaining = cooldownMs - (now - lastTeleport);
                MessageUtils.send(player, "<red>Capital teleport is on cooldown. Remaining: <white>"
                        + MessageUtils.formatTime(remaining) + "</white></red>");
                player.closeInventory();
                return;
            }

            double cost = 100.0;
            if (!plugin.getVaultHook().has(player.getUniqueId(), cost) && !player.hasPermission("nation.admin")) {
                MessageUtils.send(player,
                        "<red>You do not have enough money to teleport. Cost: <gold>$" + cost + "</gold></red>");
                player.closeInventory();
                return;
            }

            // Calculate chunk middle
            Nation.CapitalLocation cap = nation.getCapital();
            org.bukkit.World world = Bukkit.getWorld(cap.getWorld());
            if (world == null) {
                MessageUtils.send(player, "<red>Could not find the Capital world. Is it loaded?</red>");
                player.closeInventory();
                return;
            }

            // Charge Vault balance (except for admins/permission bypass)
            if (!player.hasPermission("nation.admin")) {
                plugin.getVaultHook().withdraw(player.getUniqueId(), cost);
            }

            int chunkX = ((int) Math.floor(cap.getX())) >> 4;
            int chunkZ = ((int) Math.floor(cap.getZ())) >> 4;
            double midX = (chunkX * 16) + 8.5;
            double midZ = (chunkZ * 16) + 8.5;
            double y = cap.getY();

            org.bukkit.Location targetLoc = new org.bukkit.Location(world, midX, y, midZ, cap.getYaw(), cap.getPitch());

            player.closeInventory();
            player.teleport(targetLoc);

            // Set cooldown
            gui.capitalTeleportCooldowns.put(player.getUniqueId(), now);

            MessageUtils.send(player, "<green>Teleported to the Capital of <gold>" + nation.getName()
                    + "</gold> for <gold>$" + cost + "</gold>.</green>");
            MessageUtils.playSound(player, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT);
            return;
        }

        if (slot == CaliphateMainMenu.getSlot("HUB")) {
            gui.openHubGUI(player);
            return;
        }
    }

    public void handleGovernmentGUI(Player player, ItemStack clicked, int slot) {
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.LIME_STAINED_GLASS_PANE)
            return;

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null)
            return;

        MessageUtils.playSound(player, org.bukkit.Sound.UI_BUTTON_CLICK);

        if (slot == 16 && clicked.getType() == Material.COMMAND_BLOCK_MINECART) {
            gui.openSettingsGUI(player);
            return;
        }

        if (slot == 37 || clicked.getType() == Material.CHEST_MINECART) {
            gui.caliphateSalaryMenu.open(player);
            return;
        }

        if (slot == 43 || clicked.getType() == Material.SPECTRAL_ARROW) {
            gui.mainMenuRouter.openFor(player);
            return;
        }

        if (slot == 10) {
            CaliphateGovernment cg = nation.getCaliphateGovernment();
            boolean isCaliph = cg != null && cg.hasCaliph() && cg.getCaliphUUID().equals(player.getUniqueId());
            if (!isCaliph && !player.hasPermission("nation.admin")) {
                MessageUtils.send(player, "<red>Only the Caliph can set the announcement message.</red>");
                return;
            }

            long lastAnn = nation.getLastAnnouncementTime();
            long timeSinceLastAnn = System.currentTimeMillis() - lastAnn;
            long cooldownDurationAnn = 12L * 60 * 60 * 1000;
            if (timeSinceLastAnn < cooldownDurationAnn) {
                long remainingAnn = cooldownDurationAnn - timeSinceLastAnn;
                MessageUtils.send(player, "<red>The announcement message is on cooldown. Remaining: <white>"
                        + MessageUtils.formatTime(remainingAnn) + "</white></red>");
                return;
            }

            player.closeInventory();
            id.nationcore.listeners.ChatListener.pendingAnnouncementMessages.put(player.getUniqueId(), nation);
            MessageUtils.send(player, "<yellow>Please type the announcement message in chat. Type 'cancel' to abort.</yellow>");
            return;
        }

        if (slot == 32) {
            gui.caliphateDiplomacyMenu.openManagement(player, nation);
            return;
        }

        if (slot == 30) {
            gui.caliphateBorderMenu.open(player, nation);
            return;
        }

        if (slot == 13) {
            gui.openOrdersGUI(player);
            return;
        }

        if (slot == 48) {
            CaliphateGovernment cg = nation.getCaliphateGovernment();
            boolean isCaliph = cg != null && cg.hasCaliph() && cg.getCaliphUUID().equals(player.getUniqueId());
            if (!isCaliph && !player.hasPermission("nation.admin")) {
                MessageUtils.send(player, "§cOnly the Caliph can rename the caliphate.");
                return;
            }
            player.closeInventory();
            plugin.getNationManager().setPendingRename(player.getUniqueId(), nation);
            MessageUtils.send(player, "§ePlease type the new name for your caliphate in chat. Type 'cancel' to abort.");
            return;
        }

        if (slot == 49) {
            gui.confirmActionGUI.open(player, "Leave your caliphate", () -> player.performCommand("nc leave"));
            return;
        }

        if (slot == 50) {
            CaliphateGovernment cg = nation.getCaliphateGovernment();
            boolean isCaliph = cg != null && cg.hasCaliph() && cg.getCaliphUUID().equals(player.getUniqueId());
            boolean isAdmin = player.hasPermission("nation.admin");

            if (!isCaliph && !isAdmin) {
                MessageUtils.send(player, "<red>Only the Caliph or an admin can disband the nation.</red>");
                return;
            }

            gui.confirmActionGUI.open(player, "Dissolve your caliphate", () -> {
                id.nationcore.managers.NationManager.Result result = plugin.getNationManager().disbandNation(player);
                if (result.isSuccess()) {
                    MessageUtils.send(player, "<green>" + result.getMessage() + "</green>");
                    player.closeInventory();
                } else {
                    MessageUtils.send(player, "<red>" + result.getMessage() + "</red>");
                }
            });
            return;
        }

        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
        }
    }

    public void handleOrdersGUI(Player player, ItemStack clicked, int slot) {
        if (slot == CaliphateExecutiveOrdersMenu.SLOT_CLOSE || clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }
        if (slot == CaliphateExecutiveOrdersMenu.SLOT_BACK || clicked.getType() == Material.SPECTRAL_ARROW) {
            gui.mainMenuRouter.openFor(player);
            return;
        }

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null)
            return;

        id.nationcore.models.ExecutiveOrder.ExecutiveOrderType eoType =
                id.nationcore.managers.ExecutiveOrderManager.orderAtSlot(
                        plugin.getExecutiveOrderManager().getOrdersForPosition(nation.getType(), CaliphateExecutiveOrdersMenu.POSITION_KEY),
                        CaliphateExecutiveOrdersMenu.ORDER_SLOTS, slot);
        if (eoType == null)
            return;

        if (clicked.getType() != Material.WRITABLE_BOOK) {
            MessageUtils.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        player.closeInventory();
        plugin.getExecutiveOrderManager().issueOrderForNation(player, eoType);
    }

    public void handleTreasuryGUI(Player player, ItemStack clicked) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null)
            return;

        if (clicked.getType() == Material.ARROW) {
            gui.mainMenuRouter.openFor(player);
            return;
        }
        if (clicked.getType() == Material.BOOK) {
            gui.caliphateTreasuryMenu.openTransactions(player, nation);
        }
    }

    public void handleTreasuryLogsGUI(Player player, ItemStack clicked) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null)
            return;
        if (clicked.getType() == Material.ARROW) {
            gui.caliphateTreasuryMenu.open(player, nation);
        }
    }
}
