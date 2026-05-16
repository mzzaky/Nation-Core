package id.nationcore.models;

import java.util.*;

public class Election {
    
    private ElectionPhase currentPhase;
    private long phaseStartTime;
    private Map<UUID, Candidate> candidates;
    private Map<UUID, Vote> votes;
    private Set<UUID> voterRewardsClaimed;
    private List<UUID> lotteryWinners;
    
    public Election() {
        this.currentPhase = ElectionPhase.NONE;
        this.candidates = new HashMap<>();
        this.votes = new HashMap<>();
        this.voterRewardsClaimed = new HashSet<>();
        this.lotteryWinners = new ArrayList<>();
    }
    
    public enum ElectionPhase {
        NONE(0, "No Election"),
        REGISTRATION(3, "Registration"),
        CAMPAIGN(7, "Campaign"),
        VOTING(3, "Voting"),
        INAUGURATION(1, "Inauguration");
        
        private final int durationDays;
        private final String displayName;
        
        ElectionPhase(int durationDays, String displayName) {
            this.durationDays = durationDays;
            this.displayName = displayName;
        }
        
        public int getDurationDays() { return durationDays; }
        public String getDisplayName() { return displayName; }
    }
    
    public static class Candidate {
        private UUID uuid;
        private String name;
        private String slogan;
        private long registrationTime;
        private double depositPaid;
        private Set<UUID> endorsements;
        private int campaignPoints;
        private String campaignMessage;
        private long lastCampaignBroadcast;
        
        public Candidate(UUID uuid, String name, String slogan, double depositPaid) {
            this.uuid = uuid;
            this.name = name;
            this.slogan = slogan;
            this.registrationTime = System.currentTimeMillis();
            this.depositPaid = depositPaid;
            this.endorsements = new HashSet<>();
            this.campaignPoints = 0;
            this.lastCampaignBroadcast = 0;
        }

        public UUID getUuid() { return uuid; }
        public void setUuid(UUID uuid) { this.uuid = uuid; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSlogan() { return slogan; }
        public void setSlogan(String slogan) { this.slogan = slogan; }
        public long getRegistrationTime() { return registrationTime; }
        public void setRegistrationTime(long registrationTime) { this.registrationTime = registrationTime; }
        public double getDepositPaid() { return depositPaid; }
        public void setDepositPaid(double depositPaid) { this.depositPaid = depositPaid; }
        public Set<UUID> getEndorsements() { return endorsements; }
        public void setEndorsements(Set<UUID> endorsements) { this.endorsements = endorsements; }
        public int getCampaignPoints() { return campaignPoints; }
        public void setCampaignPoints(int campaignPoints) { this.campaignPoints = campaignPoints; }
        public String getCampaignMessage() { return campaignMessage; }
        public void setCampaignMessage(String campaignMessage) { this.campaignMessage = campaignMessage; }
        public long getLastCampaignBroadcast() { return lastCampaignBroadcast; }
        public void setLastCampaignBroadcast(long lastCampaignBroadcast) { this.lastCampaignBroadcast = lastCampaignBroadcast; }
        
        public void addEndorsement(UUID uuid) {
            endorsements.add(uuid);
            campaignPoints += 10;
        }
        
        public int getEndorsementCount() {
            return endorsements.size();
        }
    }
    
    public static class Vote {
        private UUID voterUUID;
        private UUID candidateUUID;
        private double voteWeight;
        private long voteTime;
        
        public Vote(UUID voterUUID, UUID candidateUUID, double voteWeight) {
            this.voterUUID = voterUUID;
            this.candidateUUID = candidateUUID;
            this.voteWeight = voteWeight;
            this.voteTime = System.currentTimeMillis();
        }

        public UUID getVoterUUID() { return voterUUID; }
        public UUID getCandidateUUID() { return candidateUUID; }
        public double getVoteWeight() { return voteWeight; }
        public long getVoteTime() { return voteTime; }
    }

    // Getters and Setters
    public ElectionPhase getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(ElectionPhase currentPhase) { this.currentPhase = currentPhase; }
    public long getPhaseStartTime() { return phaseStartTime; }
    public void setPhaseStartTime(long phaseStartTime) { this.phaseStartTime = phaseStartTime; }
    public Map<UUID, Candidate> getCandidates() { return candidates; }
    public void setCandidates(Map<UUID, Candidate> candidates) { this.candidates = candidates; }
    public Map<UUID, Vote> getVotes() { return votes; }
    public void setVotes(Map<UUID, Vote> votes) { this.votes = votes; }
    public Set<UUID> getVoterRewardsClaimed() { return voterRewardsClaimed; }
    public void setVoterRewardsClaimed(Set<UUID> voterRewardsClaimed) { this.voterRewardsClaimed = voterRewardsClaimed; }
    public List<UUID> getLotteryWinners() { return lotteryWinners; }
    public void setLotteryWinners(List<UUID> lotteryWinners) { this.lotteryWinners = lotteryWinners; }
    
    public boolean isActive() {
        return currentPhase != ElectionPhase.NONE;
    }
    
    public void startElection() {
        this.currentPhase = ElectionPhase.REGISTRATION;
        this.phaseStartTime = System.currentTimeMillis();
        this.candidates.clear();
        this.votes.clear();
        this.voterRewardsClaimed.clear();
        this.lotteryWinners.clear();
    }
    
    public void nextPhase() {
        switch (currentPhase) {
            case REGISTRATION -> currentPhase = ElectionPhase.CAMPAIGN;
            case CAMPAIGN -> currentPhase = ElectionPhase.VOTING;
            case VOTING -> currentPhase = ElectionPhase.INAUGURATION;
            case INAUGURATION -> currentPhase = ElectionPhase.NONE;
            default -> {}
        }
        phaseStartTime = System.currentTimeMillis();
    }
    
    public void endElection() {
        currentPhase = ElectionPhase.NONE;
        candidates.clear();
        votes.clear();
        voterRewardsClaimed.clear();
        lotteryWinners.clear();
    }
    
    public double getCandidateVotes(UUID candidateUUID) {
        double total = 0;
        for (Vote vote : votes.values()) {
            if (vote.getCandidateUUID().equals(candidateUUID)) {
                total += vote.getVoteWeight();
            }
        }
        return total;
    }
    
    public Candidate getWinner() {
        Candidate winner = null;
        double maxVotes = 0;
        for (Candidate candidate : candidates.values()) {
            double votes = getCandidateVotes(candidate.getUuid());
            if (votes > maxVotes) {
                maxVotes = votes;
                winner = candidate;
            }
        }
        return winner;
    }
    
    public boolean hasVoted(UUID playerUUID) {
        return votes.containsKey(playerUUID);
    }
    
    public Candidate getCandidate(UUID uuid) {
        return candidates.get(uuid);
    }
    
    public void registerCandidate(Candidate candidate) {
        candidates.put(candidate.getUuid(), candidate);
    }
    
    public void castVote(Vote vote) {
        votes.put(vote.getVoterUUID(), vote);
    }
    
    public ElectionPhase getPhase() {
        return currentPhase;
    }
    
    public long getPhaseEndTime() {
        long duration = currentPhase.getDurationDays() * 24L * 60 * 60 * 1000; // days to milliseconds
        return phaseStartTime + duration;
    }
    
    public int getTotalVotes() {
        return votes.size();
    }
    
    public double getVoteCount(UUID candidateUUID) {
        return getCandidateVotes(candidateUUID);
    }
    
    public int getEndorsementCount(UUID candidateUUID) {
        Candidate candidate = candidates.get(candidateUUID);
        return candidate != null ? candidate.getEndorsementCount() : 0;
    }
}
