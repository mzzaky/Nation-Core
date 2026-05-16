package id.nationcore.models;

import java.util.*;

public class PlayerData {

    private UUID uuid;
    private String name;
    private long firstJoin;
    private long totalPlaytime; // in milliseconds
    private long lastSeen;

    // Nation stats
    private int totalVotesCast;
    private int timesRanForPresident;
    private int timesServedAsPresident;
    private int timesServedAsCabinet;
    private int endorsementsGiven;
    private int endorsementsReceived;
    private double totalDonations;

    // Current term tracking
    private Set<UUID> endorsedCandidates;
    private Map<UUID, Long> approvalRatingCooldowns;
    private boolean claimedVoterReward;
    private boolean claimedStimulus;
    private long lastDailyRewardClaim;
    private double pendingReward;

    // President term limit cooldown tracking (number of terms to wait before
    // running again)
    private int presidentCooldownTerms;

    // Nation membership (multi-nation system, sejak v1.5)
    // nationId == null artinya pemain belum tergabung di nation manapun.
    private String nationId;
    private Nation.NationRole nationRole;
    private long nationJoinedAt;

    // Arena stats
    private int arenaKills;
    private int arenaDeaths;
    private int currentKillstreak;
    private int bestKillstreak;

    // Punishment tracking
    private List<Punishment> punishments;

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.firstJoin = System.currentTimeMillis();
        this.totalPlaytime = 0;
        this.lastSeen = System.currentTimeMillis();
        this.endorsedCandidates = new HashSet<>();
        this.approvalRatingCooldowns = new HashMap<>();
        this.punishments = new ArrayList<>();
    }

    public static class Punishment {
        private String type;
        private String reason;
        private long timestamp;
        private long expiration;

        public Punishment(String type, String reason, long expiration) {
            this.type = type;
            this.reason = reason;
            this.timestamp = System.currentTimeMillis();
            this.expiration = expiration;
        }

        public String getType() {
            return type;
        }

        public String getReason() {
            return reason;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public long getExpiration() {
            return expiration;
        }

        public boolean isExpired() {
            return expiration > 0 && System.currentTimeMillis() > expiration;
        }
    }

    // Getters and Setters
    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getFirstJoin() {
        return firstJoin;
    }

    public void setFirstJoin(long firstJoin) {
        this.firstJoin = firstJoin;
    }

    public long getTotalPlaytime() {
        return totalPlaytime;
    }

    public void setTotalPlaytime(long totalPlaytime) {
        this.totalPlaytime = totalPlaytime;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public int getTotalVotesCast() {
        return totalVotesCast;
    }

    public void setTotalVotesCast(int totalVotesCast) {
        this.totalVotesCast = totalVotesCast;
    }

    public int getTimesRanForPresident() {
        return timesRanForPresident;
    }

    public void setTimesRanForPresident(int timesRanForPresident) {
        this.timesRanForPresident = timesRanForPresident;
    }

    public int getTimesServedAsPresident() {
        return timesServedAsPresident;
    }

    public void setTimesServedAsPresident(int timesServedAsPresident) {
        this.timesServedAsPresident = timesServedAsPresident;
    }

    public int getTimesServedAsCabinet() {
        return timesServedAsCabinet;
    }

    public void setTimesServedAsCabinet(int timesServedAsCabinet) {
        this.timesServedAsCabinet = timesServedAsCabinet;
    }

    public int getEndorsementsGiven() {
        return endorsementsGiven;
    }

    public void setEndorsementsGiven(int endorsementsGiven) {
        this.endorsementsGiven = endorsementsGiven;
    }

    public int getEndorsementsReceived() {
        return endorsementsReceived;
    }

    public void setEndorsementsReceived(int endorsementsReceived) {
        this.endorsementsReceived = endorsementsReceived;
    }

    public double getTotalDonations() {
        return totalDonations;
    }

    public void setTotalDonations(double totalDonations) {
        this.totalDonations = totalDonations;
    }

    public Set<UUID> getEndorsedCandidates() {
        return endorsedCandidates;
    }

    public void setEndorsedCandidates(Set<UUID> endorsedCandidates) {
        this.endorsedCandidates = endorsedCandidates;
    }

    public Map<UUID, Long> getApprovalRatingCooldowns() {
        return approvalRatingCooldowns;
    }

    public void setApprovalRatingCooldowns(Map<UUID, Long> approvalRatingCooldowns) {
        this.approvalRatingCooldowns = approvalRatingCooldowns;
    }

    public boolean isClaimedVoterReward() {
        return claimedVoterReward;
    }

    public void setClaimedVoterReward(boolean claimedVoterReward) {
        this.claimedVoterReward = claimedVoterReward;
    }

    public boolean isClaimedStimulus() {
        return claimedStimulus;
    }

    public void setClaimedStimulus(boolean claimedStimulus) {
        this.claimedStimulus = claimedStimulus;
    }

    public long getLastDailyRewardClaim() {
        return lastDailyRewardClaim;
    }

    public void setLastDailyRewardClaim(long lastDailyRewardClaim) {
        this.lastDailyRewardClaim = lastDailyRewardClaim;
    }

    public int getArenaKills() {
        return arenaKills;
    }

    public void setArenaKills(int arenaKills) {
        this.arenaKills = arenaKills;
    }

    public int getArenaDeaths() {
        return arenaDeaths;
    }

    public void setArenaDeaths(int arenaDeaths) {
        this.arenaDeaths = arenaDeaths;
    }

    public int getCurrentKillstreak() {
        return currentKillstreak;
    }

    public void setCurrentKillstreak(int currentKillstreak) {
        this.currentKillstreak = currentKillstreak;
    }

    public int getBestKillstreak() {
        return bestKillstreak;
    }

    public void setBestKillstreak(int bestKillstreak) {
        this.bestKillstreak = bestKillstreak;
    }

    public List<Punishment> getPunishments() {
        return punishments;
    }

    public void setPunishments(List<Punishment> punishments) {
        this.punishments = punishments;
    }

    public double getPlaytimeHours() {
        return totalPlaytime / (1000.0 * 60 * 60);
    }

    public void addPlaytime(long millis) {
        this.totalPlaytime += millis;
    }

    public boolean hasRecentPunishment(int days) {
        long threshold = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000);
        for (Punishment p : punishments) {
            if (p.getTimestamp() > threshold && !p.isExpired()) {
                return true;
            }
        }
        return false;
    }

    public void addEndorsement(UUID candidateUUID) {
        endorsedCandidates.add(candidateUUID);
        endorsementsGiven++;
    }

    public boolean hasEndorsed(UUID candidateUUID) {
        return endorsedCandidates.contains(candidateUUID);
    }

    public void clearElectionData() {
        endorsedCandidates.clear();
        claimedVoterReward = false;
    }

    public void incrementKillstreak() {
        currentKillstreak++;
        arenaKills++;
        if (currentKillstreak > bestKillstreak) {
            bestKillstreak = currentKillstreak;
        }
    }

    public void resetKillstreak() {
        currentKillstreak = 0;
        arenaDeaths++;
    }

    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }

    public double getPendingReward() {
        return pendingReward;
    }

    public void setPendingReward(double pendingReward) {
        this.pendingReward = pendingReward;
    }

    public void clearPendingReward() {
        this.pendingReward = 0;
    }

    public int getPresidentCooldownTerms() {
        return presidentCooldownTerms;
    }

    public void setPresidentCooldownTerms(int presidentCooldownTerms) {
        this.presidentCooldownTerms = presidentCooldownTerms;
    }

    public void decrementPresidentCooldown() {
        if (this.presidentCooldownTerms > 0) {
            this.presidentCooldownTerms--;
        }
    }

    // === Nation membership (v1.5+) ===

    public String getNationId() {
        return nationId;
    }

    public void setNationId(String nationId) {
        this.nationId = nationId;
    }

    public Nation.NationRole getNationRole() {
        return nationRole;
    }

    public void setNationRole(Nation.NationRole nationRole) {
        this.nationRole = nationRole;
    }

    public long getNationJoinedAt() {
        return nationJoinedAt;
    }

    public void setNationJoinedAt(long nationJoinedAt) {
        this.nationJoinedAt = nationJoinedAt;
    }

    public boolean hasNation() {
        return nationId != null && !nationId.isBlank();
    }

    public void clearNation() {
        this.nationId = null;
        this.nationRole = null;
        this.nationJoinedAt = 0;
    }
}
