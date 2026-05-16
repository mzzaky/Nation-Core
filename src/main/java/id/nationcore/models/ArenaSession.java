package id.nationcore.models;

import java.util.*;

public class ArenaSession {
    
    private UUID activatedBy;
    private long startTime;
    private long endTime;
    private boolean active;
    private Map<UUID, ArenaStats> playerStats;
    private List<DailyLeaderboard> dailyLeaderboards;
    
    public ArenaSession(UUID activatedBy, long durationDays) {
        this.activatedBy = activatedBy;
        this.startTime = System.currentTimeMillis();
        this.endTime = startTime + (durationDays * 24 * 60 * 60 * 1000L);
        this.active = true;
        this.playerStats = new HashMap<>();
        this.dailyLeaderboards = new ArrayList<>();
    }
    
    public static class ArenaStats {
        private UUID playerUUID;
        private String playerName;
        private int kills;
        private int deaths;
        private int currentStreak;
        private int bestStreak;
        private long lastKillTime;
        private double vaultEarned;
        
        public ArenaStats(UUID playerUUID, String playerName) {
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.kills = 0;
            this.deaths = 0;
            this.currentStreak = 0;
            this.bestStreak = 0;
            this.vaultEarned = 0;
        }

        public UUID getPlayerUUID() { return playerUUID; }
        public String getPlayerName() { return playerName; }
        public int getKills() { return kills; }
        public void setKills(int kills) { this.kills = kills; }
        public int getDeaths() { return deaths; }
        public void setDeaths(int deaths) { this.deaths = deaths; }
        public int getCurrentStreak() { return currentStreak; }
        public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }
        public int getBestStreak() { return bestStreak; }
        public void setBestStreak(int bestStreak) { this.bestStreak = bestStreak; }
        public long getLastKillTime() { return lastKillTime; }
        public void setLastKillTime(long lastKillTime) { this.lastKillTime = lastKillTime; }
        public double getVaultEarned() { return vaultEarned; }
        public void setVaultEarned(double vaultEarned) { this.vaultEarned = vaultEarned; }
        
        public void addKill() {
            kills++;
            currentStreak++;
            lastKillTime = System.currentTimeMillis();
            if (currentStreak > bestStreak) {
                bestStreak = currentStreak;
            }
        }
        
        public void addDeath() {
            deaths++;
            currentStreak = 0;
        }
        
        public void addVaultEarned(double amount) {
            vaultEarned += amount;
        }
        
        public double getKDR() {
            if (deaths == 0) return kills;
            return (double) kills / deaths;
        }
    }
    
    public static class DailyLeaderboard {
        private int day;
        private long timestamp;
        private List<LeaderboardEntry> entries;
        
        public DailyLeaderboard(int day) {
            this.day = day;
            this.timestamp = System.currentTimeMillis();
            this.entries = new ArrayList<>();
        }

        public int getDay() { return day; }
        public long getTimestamp() { return timestamp; }
        public List<LeaderboardEntry> getEntries() { return entries; }
        public void setEntries(List<LeaderboardEntry> entries) { this.entries = entries; }
    }
    
    public static class LeaderboardEntry {
        private UUID playerUUID;
        private String playerName;
        private int kills;
        private int rank;
        
        public LeaderboardEntry(UUID playerUUID, String playerName, int kills, int rank) {
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.kills = kills;
            this.rank = rank;
        }

        public UUID getPlayerUUID() { return playerUUID; }
        public String getPlayerName() { return playerName; }
        public int getKills() { return kills; }
        public int getRank() { return rank; }
    }

    public UUID getActivatedBy() { return activatedBy; }
    public void setActivatedBy(UUID activatedBy) { this.activatedBy = activatedBy; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Map<UUID, ArenaStats> getPlayerStats() { return playerStats; }
    public void setPlayerStats(Map<UUID, ArenaStats> playerStats) { this.playerStats = playerStats; }
    public List<DailyLeaderboard> getDailyLeaderboards() { return dailyLeaderboards; }
    public void setDailyLeaderboards(List<DailyLeaderboard> dailyLeaderboards) { this.dailyLeaderboards = dailyLeaderboards; }
    
    public boolean isExpired() {
        return System.currentTimeMillis() > endTime;
    }
    
    public long getRemainingTime() {
        return Math.max(0, endTime - System.currentTimeMillis());
    }
    
    public ArenaStats getOrCreateStats(UUID playerUUID, String playerName) {
        return playerStats.computeIfAbsent(playerUUID, k -> new ArenaStats(playerUUID, playerName));
    }
    
    public List<ArenaStats> getTopPlayers(int limit) {
        return playerStats.values().stream()
            .sorted((a, b) -> Integer.compare(b.getKills(), a.getKills()))
            .limit(limit)
            .toList();
    }
    
    public int getCurrentDay() {
        long elapsed = System.currentTimeMillis() - startTime;
        return (int) (elapsed / (24 * 60 * 60 * 1000L)) + 1;
    }
}
