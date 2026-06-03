package id.nationcore.managers;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import id.nationcore.NationCore;
import id.nationcore.models.ArenaSession;
import id.nationcore.models.Government;
import id.nationcore.models.PlayerData;
import id.nationcore.models.Treasury;
import id.nationcore.utils.MessageUtils;

public class ArenaManager {

    private final NationCore plugin;
    private ArenaSession currentSession;
    private final Map<UUID, Integer> killStreaks = new ConcurrentHashMap<>();
    private final Map<UUID, Location> playerReturnLocations = new HashMap<>();
    private final Set<UUID> arenaPlayers = new HashSet<>();
    private BukkitTask arenaTask;
    private Location arenaSpawn;
    private Location arenaCenter;
    private int arenaRadius = 100;

    // Leaderboard rewards
    private static final int[] DAILY_REWARDS = { 100000, 75000, 60000, 50000, 40000, 35000, 30000, 27500, 25000,
            25000 };
    private static final int[] NETHERITE_REWARDS = { 3, 2, 2, 1, 1, 1, 1, 1, 1, 1 };

    public ArenaManager(NationCore plugin) {
        this.plugin = plugin;
    }

    public boolean canStartArena() {
        Government gov = plugin.getDataManager().getGovernment();
        if (!gov.hasPresident())
            return false;

        // Check if arena is already active
        if (isArenaActive())
            return false;

        // Check games this term
        int maxGames = plugin.getConfig().getInt("presidential-games.max-per-term", 2);
        int gamesThisTerm = plugin.getDataManager().getGamesThisTerm();
        if (gamesThisTerm >= maxGames)
            return false;

        // Check cost
        int cost = plugin.getConfig().getInt("presidential-games.cost", 100000);
        return plugin.getDataManager().getTreasury().getBalance() >= cost;
    }

    public boolean startArena(UUID presidentId) {
        if (!canStartArena())
            return false;

        Government gov = plugin.getDataManager().getGovernment();
        if (!gov.getPresidentUUID().equals(presidentId))
            return false;

        // Deduct cost
        int cost = plugin.getConfig().getInt("presidential-games.cost", 100000);
        plugin.getTreasuryManager().withdraw(Treasury.TransactionType.PRESIDENTIAL_GAMES, (double) cost,
                "Presidential Arena Games", presidentId);

        // Create new session
        long durationDays = plugin.getConfig().getLong("presidential-games.duration-days", 7);
        currentSession = new ArenaSession(presidentId, durationDays);

        // Set arena location (use world spawn or configured location)
        World world = Bukkit.getWorlds().get(0);
        arenaCenter = world.getSpawnLocation();
        arenaSpawn = arenaCenter.clone().add(0, 1, 0);
        arenaRadius = plugin.getConfig().getInt("presidential-games.radius", 100);

        // Increment games count
        plugin.getDataManager().setGamesThisTerm(plugin.getDataManager().getGamesThisTerm() + 1);

        // Broadcast
        Player president = Bukkit.getPlayer(presidentId);
        String presidentName = president != null ? president.getName() : "The President";

        MessageUtils.broadcast("");
        MessageUtils.broadcast("<gold>═══════════════════════════════════════");
        MessageUtils.broadcast("<red>⚔ <gold><bold>PRESIDENTIAL ARENA GAMES</bold> <red>⚔");
        MessageUtils.broadcast("<gold>═══════════════════════════════════════");
        MessageUtils.broadcast("");
        MessageUtils.broadcast("<yellow>Declared by: <white>" + presidentName);
        MessageUtils.broadcast("<yellow>Duration: <white>" + durationDays + " days");
        MessageUtils.broadcast("<yellow>Open the Presidential Arena menu from your nation menu to participate!");
        MessageUtils.broadcast("");
        MessageUtils.broadcast("<gray>Daily rewards for top 10 players!");
        MessageUtils.broadcast("<gray>Grand prize: <gold>1,000,000 vault + exclusive items!");
        MessageUtils.broadcast("");

        // Play sound to all
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        }

        // Start arena task
        startArenaTask();

        plugin.getDataManager().setArenaSession(currentSession);
        plugin.getDataManager().saveAll();

        return true;
    }

    public void endArena() {
        if (currentSession == null)
            return;

        // Calculate final standings
        distributeFinalRewards();

        // Teleport all players out
        for (UUID playerId : new HashSet<>(arenaPlayers)) {
            leaveArena(playerId);
        }

        // Clear data
        killStreaks.clear();
        arenaPlayers.clear();

        // Stop task
        if (arenaTask != null) {
            arenaTask.cancel();
            arenaTask = null;
        }

        // Broadcast end
        MessageUtils.broadcast("");
        MessageUtils.broadcast("<gold>═══════════════════════════════════════");
        MessageUtils.broadcast("<red>⚔ <gold><bold>ARENA GAMES CONCLUDED</bold> <red>⚔");
        MessageUtils.broadcast("<gold>═══════════════════════════════════════");

        // Show top 3
        List<Map.Entry<UUID, ArenaSession.ArenaStats>> topPlayers = currentSession.getPlayerStats().entrySet()
                .stream()
                .sorted((a, b) -> Integer.compare(b.getValue().getKills(), a.getValue().getKills()))
                .limit(3)
                .toList();

        String[] medals = { "🥇", "🥈", "🥉" };
        for (int i = 0; i < topPlayers.size(); i++) {
            Map.Entry<UUID, ArenaSession.ArenaStats> entry = topPlayers.get(i);
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            MessageUtils
                    .broadcast("<yellow>" + medals[i] + " " + name + " - " + entry.getValue().getKills() + " kills");
        }

        MessageUtils.broadcast("");

        currentSession = null;
        plugin.getDataManager().setArenaSession(null);
        plugin.getDataManager().saveAll();
    }

    public boolean joinArena(Player player) {
        if (!isArenaActive()) {
            MessageUtils.send(player, "<red>No arena games are currently active!");
            return false;
        }

        if (arenaPlayers.contains(player.getUniqueId())) {
            MessageUtils.send(player, "<red>You are already in the arena!");
            return false;
        }

        // Store return location
        playerReturnLocations.put(player.getUniqueId(), player.getLocation().clone());

        // Add to arena
        arenaPlayers.add(player.getUniqueId());
        killStreaks.put(player.getUniqueId(), 0);

        // Initialize stats if needed
        currentSession.getOrCreateStats(player.getUniqueId(), player.getName());

        // Teleport to arena
        player.teleport(arenaSpawn);

        // Give arena kit
        giveArenaKit(player);

        // Effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        MessageUtils.send(player, "<gold>⚔ <yellow>You have joined the Arena Games!");
        MessageUtils.send(player, "<gray>Kill other players to earn rewards!");
        MessageUtils.send(player, "<gray>Use the Arena GUI menu to leave.");

        // Announce
        MessageUtils.broadcast("<gold>⚔ <white>" + player.getName() + " <yellow>has joined the Arena Games!");

        return true;
    }

    public boolean leaveArena(UUID playerId) {
        if (!arenaPlayers.contains(playerId))
            return false;

        arenaPlayers.remove(playerId);
        killStreaks.remove(playerId);

        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            // Return to previous location
            Location returnLoc = playerReturnLocations.remove(playerId);
            if (returnLoc != null) {
                player.teleport(returnLoc);
            } else {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }

            // Clear inventory and restore
            player.getInventory().clear();
            player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
            player.setFoodLevel(20);

            MessageUtils.send(player, "<yellow>You have left the Arena Games.");
        }

        return true;
    }

    public void handleKill(Player killer, Player victim) {
        if (!isArenaActive())
            return;
        if (!arenaPlayers.contains(killer.getUniqueId()) || !arenaPlayers.contains(victim.getUniqueId()))
            return;

        UUID killerId = killer.getUniqueId();
        UUID victimId = victim.getUniqueId();

        // Update stats
        ArenaSession.ArenaStats killerStats = currentSession.getOrCreateStats(killerId, killer.getName());
        ArenaSession.ArenaStats victimStats = currentSession.getOrCreateStats(victimId, victim.getName());

        killerStats.addKill();
        victimStats.addDeath();

        // Update killstreak
        int streak = killStreaks.merge(killerId, 1, Integer::sum);
        killStreaks.put(victimId, 0); // Reset victim's streak

        // Base reward
        int baseReward = plugin.getConfig().getInt("presidential-games.kill-reward", 5000);
        int reward = baseReward;

        // Killstreak bonuses
        String streakBonus = "";
        if (streak == 5) {
            reward += 10000;
            streakBonus = " <gold>(+10k Killstreak!)";
            MessageUtils.broadcast("<red>🔥 " + killer.getName() + " is on a 5 KILLSTREAK!");
            giveKillstreakReward(killer, 5);
        } else if (streak == 10) {
            reward += 25000;
            streakBonus = " <gold>(+25k Killstreak!)";
            MessageUtils.broadcast("<red>🔥🔥 " + killer.getName() + " is on a 10 KILLSTREAK!");
            giveKillstreakReward(killer, 10);
        } else if (streak == 25) {
            reward += 50000;
            streakBonus = " <gold>(+50k Killstreak!)";
            MessageUtils.broadcast("<red>🔥🔥🔥 " + killer.getName() + " is UNSTOPPABLE with 25 KILLS!");
            giveKillstreakReward(killer, 25);
        } else if (streak == 50) {
            reward += 100000;
            streakBonus = " <gold>(+100k Killstreak!)";
            MessageUtils.broadcast("<red><bold>☠ " + killer.getName() + " IS A LEGEND WITH 50 KILLS! ☠");
            giveKillstreakReward(killer, 50);
        }

        // Give reward to killer
        plugin.getVaultHook().deposit(killer.getUniqueId(), (double) reward);
        MessageUtils.send(killer, "<green>+$" + MessageUtils.formatNumber(reward) + streakBonus);

        // Death penalty for victim
        int deathPenalty = plugin.getConfig().getInt("presidential-games.death-penalty", 5000);
        if (plugin.getVaultHook().getBalance(victim.getUniqueId()) >= deathPenalty) {
            plugin.getVaultHook().withdraw(victim.getUniqueId(), (double) deathPenalty);
            MessageUtils.send(victim, "<red>-$" + MessageUtils.formatNumber(deathPenalty) + " (Death penalty)");
        }

        // Update player data
        PlayerData killerData = plugin.getDataManager().getPlayerData(killerId);
        PlayerData victimData = plugin.getDataManager().getPlayerData(victimId);
        if (killerData != null)
            killerData.incrementKillstreak();
        if (victimData != null)
            victimData.resetKillstreak();

        plugin.getDataManager().saveAll();
    }

    public void handleDeath(Player player) {
        if (!arenaPlayers.contains(player.getUniqueId()))
            return;

        // Reset killstreak
        killStreaks.put(player.getUniqueId(), 0);

        // Respawn in arena after delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (arenaPlayers.contains(player.getUniqueId()) && player.isOnline()) {
                respawnInArena(player);
            }
        }, 60L); // 3 second delay
    }

    private void respawnInArena(Player player) {
        // Random spawn within arena
        double angle = Math.random() * 2 * Math.PI;
        double distance = Math.random() * (arenaRadius * 0.8);
        Location spawn = arenaCenter.clone().add(
                Math.cos(angle) * distance,
                0,
                Math.sin(angle) * distance);
        spawn.setY(spawn.getWorld().getHighestBlockYAt(spawn) + 1);

        player.teleport(spawn);
        giveArenaKit(player);

        // Brief invulnerability
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 4)); // 3 seconds
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 2));

        MessageUtils.send(player, "<yellow>You have respawned in the arena!");
    }

    private void giveArenaKit(Player player) {
        player.getInventory().clear();

        // Iron armor
        player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));

        // Weapons
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.displayName(MessageUtils.parse("<red>Arena Sword"));
        sword.setItemMeta(swordMeta);
        player.getInventory().addItem(sword);

        ItemStack bow = new ItemStack(Material.BOW);
        player.getInventory().addItem(bow);
        player.getInventory().addItem(new ItemStack(Material.ARROW, 32));

        // Food and utility
        player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 3));
        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));

        // Full health/food
        player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setSaturation(20);
    }

    private void giveKillstreakReward(Player player, int streak) {
        switch (streak) {
            case 5 -> {
                player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 3));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 600, 0));
            }
            case 10 -> {
                player.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD));
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 600, 0));
            }
            case 25 -> {
                ItemStack netheriteAxe = new ItemStack(Material.NETHERITE_AXE);
                ItemMeta meta = netheriteAxe.getItemMeta();
                meta.displayName(MessageUtils.parse("<red><bold>Killstreak Axe"));
                netheriteAxe.setItemMeta(meta);
                player.getInventory().addItem(netheriteAxe);
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 1200, 1));
            }
            case 50 -> {
                // Full netherite upgrade
                player.getInventory().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
                player.getInventory().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
                player.getInventory().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
                player.getInventory().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 2400, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 2400, 1));
            }
        }
    }

    private void startArenaTask() {
        arenaTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (currentSession == null) {
                if (arenaTask != null)
                    arenaTask.cancel();
                return;
            }

            // Check if session expired
            if (currentSession.isExpired()) {
                endArena();
                return;
            }

            // Keep players in bounds
            for (UUID playerId : arenaPlayers) {
                Player player = Bukkit.getPlayer(playerId);
                if (player == null || !player.isOnline())
                    continue;

                if (player.getLocation().distance(arenaCenter) > arenaRadius) {
                    player.teleport(arenaSpawn);
                    MessageUtils.send(player, "<red>You left the arena bounds!");
                }
            }

        }, 20L, 20L); // Every second
    }

    public void distributeDailyRewards() {
        if (currentSession == null)
            return;

        List<Map.Entry<UUID, ArenaSession.ArenaStats>> leaderboard = currentSession.getPlayerStats().entrySet()
                .stream()
                .sorted((a, b) -> Integer.compare(b.getValue().getKills(), a.getValue().getKills()))
                .limit(10)
                .toList();

        MessageUtils.broadcast("");
        MessageUtils.broadcast("<gold>═══════════════════════════════════════");
        MessageUtils.broadcast("<yellow>⚔ <gold>Daily Arena Leaderboard <yellow>⚔");
        MessageUtils.broadcast("<gold>═══════════════════════════════════════");

        for (int i = 0; i < leaderboard.size(); i++) {
            Map.Entry<UUID, ArenaSession.ArenaStats> entry = leaderboard.get(i);
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            int kills = entry.getValue().getKills();
            int reward = DAILY_REWARDS[i];
            int netherite = NETHERITE_REWARDS[i];

            // Give rewards
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                plugin.getVaultHook().deposit(player.getUniqueId(), (double) reward);
                player.getInventory().addItem(new ItemStack(Material.NETHERITE_INGOT, netherite));
                MessageUtils.send(player, "<gold>Daily arena reward: $" + MessageUtils.formatNumber(reward) + " + "
                        + netherite + " netherite!");
            } else {
                // Store for offline player - deposit directly to vault
                plugin.getVaultHook().deposit(entry.getKey(), (double) reward);
            }

            String medal = switch (i) {
                case 0 -> "🥇";
                case 1 -> "🥈";
                case 2 -> "🥉";
                default -> "#" + (i + 1);
            };

            MessageUtils.broadcast("<yellow>" + medal + " <white>" + name + " <gray>- " + kills + " kills <green>($"
                    + MessageUtils.formatNumber(reward) + ")");
        }

        MessageUtils.broadcast("");

        // Note: Daily stats reset is handled by preserving cumulative stats
    }

    private void distributeFinalRewards() {
        if (currentSession == null)
            return;

        List<Map.Entry<UUID, ArenaSession.ArenaStats>> finalStandings = currentSession.getPlayerStats().entrySet()
                .stream()
                .sorted((a, b) -> Integer.compare(b.getValue().getKills(), a.getValue().getKills()))
                .toList();

        if (finalStandings.isEmpty())
            return;

        // Grand prize for #1
        Map.Entry<UUID, ArenaSession.ArenaStats> winner = finalStandings.get(0);
        Player winnerPlayer = Bukkit.getPlayer(winner.getKey());
        String winnerName = Bukkit.getOfflinePlayer(winner.getKey()).getName();

        int grandPrize = plugin.getConfig().getInt("presidential-games.grand-prize", 1000000);

        if (winnerPlayer != null && winnerPlayer.isOnline()) {
            plugin.getVaultHook().deposit(winnerPlayer.getUniqueId(), (double) grandPrize);

            // Trophy item
            ItemStack trophy = new ItemStack(Material.NETHER_STAR);
            ItemMeta meta = trophy.getItemMeta();
            meta.displayName(MessageUtils.parse("<gold><bold>Arena Champion Trophy"));
            meta.lore(List.of(
                    MessageUtils.parse("<gray>Presidential Arena Games Champion"),
                    MessageUtils.parse("<yellow>Total Kills: " + winner.getValue().getKills()),
                    MessageUtils.parse("<gray>Season: " + currentSession.getStartTime())));
            trophy.setItemMeta(meta);
            winnerPlayer.getInventory().addItem(trophy);

            // Exclusive gear
            ItemStack chestplate = new ItemStack(Material.NETHERITE_CHESTPLATE);
            ItemMeta chestMeta = chestplate.getItemMeta();
            chestMeta.displayName(MessageUtils.parse("<red><bold>Champion's Chestplate"));
            chestplate.setItemMeta(chestMeta);
            winnerPlayer.getInventory().addItem(chestplate);

            MessageUtils.send(winnerPlayer, "<gold><bold>CONGRATULATIONS! You are the Arena Champion!");
            MessageUtils.send(winnerPlayer, "<yellow>You received: $1,000,000 + Trophy + Champion's Gear!");
        } else {
            // Deposit directly to vault for offline player
            plugin.getVaultHook().deposit(winner.getKey(), (double) grandPrize);
        }

        MessageUtils.broadcast("<gold>🏆 <yellow>Arena Champion: <white>" + winnerName + " <gray>("
                + winner.getValue().getKills() + " total kills)");
    }

    // Getters
    public boolean isArenaActive() {
        return currentSession != null && !currentSession.isExpired();
    }

    public boolean isInArena(UUID playerId) {
        return arenaPlayers.contains(playerId);
    }

    public ArenaSession getCurrentSession() {
        return currentSession;
    }

    public int getKillStreak(UUID playerId) {
        return killStreaks.getOrDefault(playerId, 0);
    }

    public Set<UUID> getArenaPlayers() {
        return Collections.unmodifiableSet(arenaPlayers);
    }

    public List<Map.Entry<UUID, ArenaSession.ArenaStats>> getLeaderboard(int limit) {
        if (currentSession == null)
            return Collections.emptyList();
        return currentSession.getPlayerStats().entrySet()
                .stream()
                .sorted((a, b) -> Integer.compare(b.getValue().getKills(), a.getValue().getKills()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public void loadSession() {
        currentSession = plugin.getDataManager().getArenaSession();
        if (currentSession != null && !currentSession.isExpired()) {
            startArenaTask();
        } else {
            currentSession = null;
        }
    }

    // Additional method for NationCommand
    public boolean startNewSession(Player player) {
        return startArena(player.getUniqueId());
    }
}
