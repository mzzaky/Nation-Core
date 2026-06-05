package id.nationcore.listeners;

import id.nationcore.NationCore;
import id.nationcore.models.CommunistGovernment;
import id.nationcore.models.Government;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.Nation;
import id.nationcore.models.PlayerData;
import id.nationcore.models.TaxRecord;
import id.nationcore.utils.MessageUtils;
import id.nationcore.utils.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerListener implements Listener {

    private final NationCore plugin;

    public PlayerListener(NationCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Initialize or load player data
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        data.updateLastSeen();

        // Apply buffs if president or cabinet
        plugin.getBuffManager().handlePlayerJoin(player);

        // Apply any active cabinet decision effects
        plugin.getCabinetManager().applyEffectsToPlayer(player);

        // Check for pending rewards
        double pendingRewards = data.getPendingReward();
        if (pendingRewards > 0) {
            plugin.getVaultHook().deposit(player.getUniqueId(), pendingRewards);
            data.clearPendingReward();
            MessageUtils.send(player,
                    "<gold>You received $" + MessageUtils.formatNumber(pendingRewards) + " in pending rewards!");
        }

        // Show government info — prioritize nation info for player if exists,
        // fallback to global Government legacy.
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation != null) {
            MessageUtils.send(player, "<gray>Nation Anda: <gold>" + nation.getName() +
                    "</gold> <gray>(" + nation.getType().getDisplayName() + ")");
            if (nation.getType() == GovernmentType.REPUBLIC) {
                Government natGov = nation.getRepublicGovernment();
                if (natGov != null && natGov.hasPresident()) {
                    String presidentName = Bukkit.getOfflinePlayer(natGov.getPresidentUUID()).getName();
                    MessageUtils.send(player, "<gray>" + nation.getType().getLeaderTitle() +
                            ": <gold>" + presidentName);
                }
                if (plugin.getElectionManager().isElectionActive(nation)) {
                    MessageUtils.send(player,
                            "<yellow>📢 Election is ongoing in " + nation.getName() +
                                    "! Open the menu for details.");
                }
                if (nation.getRecallPetition() != null) {
                    MessageUtils.send(player, "<red>📜 Recall petition active in " + nation.getName() +
                            "! Use <white>/dc recall <red>for info.");
                }
            } else if (nation.getType() == GovernmentType.COMMUNIST) {
                CommunistGovernment cg = nation.getCommunistGovernment();
                if (cg != null && cg.hasSecretaryGeneral()) {
                    String sekjenName = Bukkit.getOfflinePlayer(cg.getSecretaryGeneralUUID()).getName();
                    MessageUtils.send(player, "<gray>" + nation.getType().getLeaderTitle() +
                            ": <gold>" + sekjenName);
                    MessageUtils.send(player, "<gray>Party Members: <white>" +
                            cg.getPartyMemberCount() + "/" + nation.getMemberCount());
                }
            }
        } else {
            Government gov = plugin.getDataManager().getGovernment();
            if (gov.hasPresident()) {
                String presidentName = Bukkit.getOfflinePlayer(gov.getPresidentUUID()).getName();
                MessageUtils.send(player, "<gray>Current President: <gold>" + presidentName);
            }
            if (plugin.getElectionManager().isElectionActive()) {
                MessageUtils.send(player,
                        "<yellow>📢 An election is currently in progress! Open the menu for details.");
            }
            if (plugin.getRecallManager().hasPetitionActive()) {
                MessageUtils.send(player,
                        "<red>📜 A recall petition is active! Use <white>/dc recall <red>for info.");
            }
        }

        // Check for active arena
        if (nation != null && plugin.getArenaManager().isArenaActive(nation)) {
            MessageUtils.send(player,
                    "<red>⚔ <yellow>Presidential Arena Games are active! Open the Presidential Arena menu from your nation menu to participate!");
        }

        // Check for outstanding tax debt
        if (plugin.getTaxManager().isEnabled()) {
            TaxRecord.PlayerTaxData taxData = plugin.getTaxManager().getTaxRecord()
                    .getPlayerTaxData(player.getUniqueId().toString());
            if (taxData != null && taxData.getOutstandingDebt() > 0) {
                MessageUtils.send(player, "<red>💲 You have an outstanding tax debt of <gold>$" +
                         MessageUtils.formatNumber(taxData.getOutstandingDebt()) +
                        "</gold>! <red>Use <white>/dc tax pay <red>to settle your debt.");
            }
        }

        plugin.getDataManager().saveAll();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Calculate and accumulate playtime for this session
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        if (data.getLastSeen() > 0) {
            long sessionDuration = System.currentTimeMillis() - data.getLastSeen();
            // Cap at 24 hours per session as a safeguard against corrupt data
            long maxSession = 24L * 60 * 60 * 1000;
            data.addPlaytime(Math.min(sessionDuration, maxSession));
        }

        // Update last seen
        data.updateLastSeen();

        // Handle buff cleanup
        plugin.getBuffManager().handlePlayerQuit(player);

        // Leave arena if in it
        if (plugin.getArenaManager().isInArena(player.getUniqueId())) {
            plugin.getArenaManager().leaveArena(player.getUniqueId());
        }

        plugin.getDataManager().saveAll();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player killer = player.getKiller();

        // Handle arena death
        if (plugin.getArenaManager().isInArena(player.getUniqueId())) {
            // Keep inventory in arena
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);

            if (killer != null && plugin.getArenaManager().isInArena(killer.getUniqueId())) {
                plugin.getArenaManager().handleKill(killer, player);
            }

            plugin.getArenaManager().handleDeath(player);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Reapply buffs after respawn
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getBuffManager().handlePlayerJoin(player);
            plugin.getCabinetManager().applyEffectsToPlayer(player);
        }, 5L);
    }
}
