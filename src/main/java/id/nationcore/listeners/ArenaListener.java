package id.nationcore.listeners;

import id.nationcore.NationCore;
import id.nationcore.models.ArenaSession;
import id.nationcore.models.PlayerData;
import id.nationcore.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArenaListener implements Listener {
    
    private final NationCore plugin;
    private final Map<UUID, Integer> currentKillstreaks = new HashMap<>();
    
    public ArenaListener(NationCore plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        // Check if in arena
        if (!plugin.getArenaManager().isInArena(victim.getUniqueId())) {
            return;
        }
        
        ArenaSession session = plugin.getArenaManager().getCurrentSession();
        if (session == null) return;
        
        // Handle death through ArenaManager
        plugin.getArenaManager().handleDeath(victim);
        
        // Reset killstreak
        int lostStreak = currentKillstreaks.getOrDefault(victim.getUniqueId(), 0);
        currentKillstreaks.put(victim.getUniqueId(), 0);
        
        // Death penalty
        long deathPenalty = plugin.getConfig().getLong("arena.death-penalty", 5000);
        if (plugin.getVaultHook().getBalance(victim.getUniqueId()) >= deathPenalty) {
            plugin.getVaultHook().withdraw(victim.getUniqueId(), (double) deathPenalty);
            MessageUtils.send(victim, "<red>-" + MessageUtils.formatNumber(deathPenalty) + 
                " (death penalty)");
        }
        
        if (lostStreak >= 5) {
            MessageUtils.broadcast("<yellow>" + victim.getName() + " <gray>kehilangan killstreak <red>" + 
                lostStreak + "!");
        }
        
        // Handle killer rewards
        if (killer != null && plugin.getArenaManager().isInArena(killer.getUniqueId())) {
            // Let ArenaManager handle the kill with full logic
            plugin.getArenaManager().handleKill(killer, victim);
            
            // Update local killstreak tracking
            int streak = currentKillstreaks.getOrDefault(killer.getUniqueId(), 0) + 1;
            currentKillstreaks.put(killer.getUniqueId(), streak);
            
            // Update player data best streak
            PlayerData data = plugin.getDataManager().getPlayerData(killer.getUniqueId());
            if (data != null && streak > data.getBestKillstreak()) {
                data.setBestKillstreak(streak);
                plugin.getDataManager().saveAll();
            }
            
            // Handle streak milestones with additional rewards
            handleStreakMilestone(killer, streak);
        }
        
        // Custom death message
        event.setDeathMessage(null);
        if (killer != null) {
            MessageUtils.broadcast("<red>☠ <yellow>" + victim.getName() + " <gray>dibunuh oleh <green>" + 
                killer.getName() + " <gray>di arena!");
        } else {
            MessageUtils.broadcast("<red>☠ <yellow>" + victim.getName() + " <gray>mati di arena!");
        }
    }
    
    private void handleStreakMilestone(Player killer, int streak) {
        String killerName = killer.getName();
        
        switch (streak) {
            case 5 -> {
                MessageUtils.broadcast("<gold>🔥 <yellow>" + killerName + " <gold>mendapat <white>5 KILLSTREAK!");
                plugin.getVaultHook().deposit(killer.getUniqueId(), 25000.0);
                MessageUtils.send(killer, "<gold>+25,000 Bonus killstreak 5!");
                MessageUtils.playSound(killer, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP);
            }
            case 10 -> {
                MessageUtils.broadcast("<gold>🔥🔥 <red>" + killerName + " <gold>UNSTOPPABLE! <white>10 KILLSTREAK!");
                plugin.getVaultHook().deposit(killer.getUniqueId(), 50000.0);
                MessageUtils.send(killer, "<gold>+50,000 Bonus killstreak 10!");
                MessageUtils.playSound(killer, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE);
                // Give special item
                killer.getInventory().addItem(new org.bukkit.inventory.ItemStack(
                    org.bukkit.Material.GOLDEN_APPLE, 3));
            }
            case 25 -> {
                MessageUtils.broadcast("<dark_red>🔥🔥🔥 <red>" + killerName + " <dark_red>GODLIKE!! <white>25 KILLSTREAK!");
                plugin.getVaultHook().deposit(killer.getUniqueId(), 100000.0);
                MessageUtils.send(killer, "<gold>+100,000 Bonus killstreak 25!");
                MessageUtils.playSound(killer, org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL);
                // Give netherite
                killer.getInventory().addItem(new org.bukkit.inventory.ItemStack(
                    org.bukkit.Material.NETHERITE_INGOT, 1));
            }
            case 50 -> {
                MessageUtils.broadcast("<dark_purple>👑 <light_purple>" + killerName +
                    " <dark_purple>LEGENDARY!!! <white>50 KILLSTREAK!");
                plugin.getVaultHook().deposit(killer.getUniqueId(), 250000.0);
                MessageUtils.send(killer, "<gold>+250,000 Bonus killstreak 50!");

                // Special broadcast with title
                for (Player p : Bukkit.getOnlinePlayers()) {
                    MessageUtils.sendTitle(p, "<dark_purple>LEGENDARY",
                        "<light_purple>" + killerName + " - 50 Kills!", 10, 60, 20);
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        if (!plugin.getArenaManager().isInArena(player.getUniqueId())) {
            return;
        }
        
        // ArenaManager will handle respawn location in its handleDeath method
        // Just ensure player gets kit after respawn
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && plugin.getArenaManager().isInArena(player.getUniqueId())) {
                // Player will be teleported by ArenaManager's respawnInArena method
            }
        }, 20L);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        if (plugin.getArenaManager().isInArena(uuid)) {
            plugin.getArenaManager().leaveArena(uuid);
        }
        
        // Clear killstreak
        currentKillstreaks.remove(uuid);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        
        boolean victimInArena = plugin.getArenaManager().isInArena(victim.getUniqueId());
        boolean attackerInArena = plugin.getArenaManager().isInArena(attacker.getUniqueId());
        
        // Both must be in arena for arena PvP
        if (victimInArena != attackerInArena) {
            // One is in arena, one isn't - cancel
            event.setCancelled(true);
            return;
        }
        
        // If both in arena, allow PvP regardless of other settings
        if (victimInArena && attackerInArena) {
            event.setCancelled(false);
        }
    }
    
    // === PUBLIC METHODS ===
    
    public int getKillstreak(UUID uuid) {
        return currentKillstreaks.getOrDefault(uuid, 0);
    }
    
    public void resetKillstreak(UUID uuid) {
        currentKillstreaks.remove(uuid);
    }
    
    public void resetAllKillstreaks() {
        currentKillstreaks.clear();
    }
}
