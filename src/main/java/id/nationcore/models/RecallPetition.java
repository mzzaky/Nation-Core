package id.nationcore.models;

import java.util.*;

public class RecallPetition {
    
    private UUID targetId;
    private UUID initiator;
    private String reason;
    private long startTime;
    private long collectionDeadline;
    private long votingEndTime;
    private Set<UUID> signatures;
    private Map<UUID, Double> signatureDeposits;
    private RecallPhase phase;
    private Map<UUID, Boolean> recallVotes; // true = remove, false = keep
    
    public RecallPetition(UUID targetId, UUID initiator, String reason, long collectionDurationMs) {
        this.targetId = targetId;
        this.initiator = initiator;
        this.reason = reason;
        this.startTime = System.currentTimeMillis();
        this.collectionDeadline = this.startTime + collectionDurationMs;
        this.votingEndTime = 0;
        this.signatures = new HashSet<>();
        this.signatureDeposits = new HashMap<>();
        this.phase = RecallPhase.COLLECTING;
        this.recallVotes = new HashMap<>();
        
        // Initiator automatically signs
        signatures.add(initiator);
    }
    
    public enum RecallPhase {
        COLLECTING("Collecting Signatures"),
        VOTING("Recall Voting"),
        COMPLETED("Completed"),
        FAILED("Failed");
        
        private final String displayName;
        
        RecallPhase(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }

    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }
    public UUID getInitiatorId() { return initiator; }
    public UUID getInitiator() { return initiator; }
    public void setInitiator(UUID initiator) { this.initiator = initiator; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getCollectionDeadline() { return collectionDeadline; }
    public void setCollectionDeadline(long collectionDeadline) { this.collectionDeadline = collectionDeadline; }
    public long getVotingEndTime() { return votingEndTime; }
    public void setVotingEndTime(long votingEndTime) { this.votingEndTime = votingEndTime; }
    public Set<UUID> getSignatures() { return signatures; }
    public void setSignatures(Set<UUID> signatures) { this.signatures = signatures; }
    public Map<UUID, Double> getSignatureDeposits() { return signatureDeposits; }
    public Map<UUID, Double> getDeposits() { return signatureDeposits; }
    public void setSignatureDeposits(Map<UUID, Double> signatureDeposits) { this.signatureDeposits = signatureDeposits; }
    public RecallPhase getPhase() { return phase; }
    public void setPhase(RecallPhase phase) { this.phase = phase; }
    public Map<UUID, Boolean> getRecallVotes() { return recallVotes; }
    public void setRecallVotes(Map<UUID, Boolean> recallVotes) { this.recallVotes = recallVotes; }
    
    public void addSignature(UUID player, double deposit) {
        signatures.add(player);
        signatureDeposits.put(player, deposit);
    }
    
    public void removeSignature(UUID player) {
        signatures.remove(player);
        signatureDeposits.remove(player);
    }
    
    public double getDeposit(UUID player) {
        return signatureDeposits.getOrDefault(player, 0.0);
    }
    
    public boolean hasSigned(UUID player) {
        return signatures.contains(player);
    }
    
    public int getSignatureCount() {
        return signatures.size();
    }
    
    public void addVote(UUID player, boolean removePresident) {
        recallVotes.put(player, removePresident);
    }
    
    public void castRecallVote(UUID player, boolean removePresident) {
        recallVotes.put(player, removePresident);
    }
    
    public boolean hasVoted(UUID player) {
        return recallVotes.containsKey(player);
    }
    
    public int getRemoveVotes() {
        return (int) recallVotes.values().stream().filter(v -> v).count();
    }
    
    public int getKeepVotes() {
        return (int) recallVotes.values().stream().filter(v -> !v).count();
    }
    
    public double getRemovePercentage() {
        if (recallVotes.isEmpty()) return 0;
        return (double) getRemoveVotes() / recallVotes.size();
    }
    
    public double getTotalDeposits() {
        return signatureDeposits.values().stream().mapToDouble(d -> d).sum();
    }
    
    public boolean isExpired() {
        if (phase == RecallPhase.COLLECTING) {
            return System.currentTimeMillis() > collectionDeadline;
        } else if (phase == RecallPhase.VOTING && votingEndTime > 0) {
            return System.currentTimeMillis() > votingEndTime;
        }
        return false;
    }
}
