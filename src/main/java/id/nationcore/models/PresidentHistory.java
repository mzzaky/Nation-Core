package id.nationcore.models;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import id.nationcore.models.Government.CabinetPosition;

public class PresidentHistory {
    
    private List<PresidentRecord> records;
    
    public PresidentHistory() {
        this.records = new ArrayList<>();
    }
    
    public static class PresidentRecord {
        private UUID uuid;
        private String name;
        private long termStart;
        private long termEnd;
        private double finalApprovalRating;
        private int executiveOrdersIssued;
        private int presidentialGamesHosted;
        private Map<CabinetPosition, String> cabinetMembers;
        private List<String> accomplishments;
        private String endReason; // "TERM_END", "RECALL", "INACTIVE", "ADMIN_REMOVAL"
        
        public PresidentRecord(UUID uuid, String name, long termStart) {
            this.uuid = uuid;
            this.name = name;
            this.termStart = termStart;
            this.cabinetMembers = new EnumMap<>(CabinetPosition.class);
            this.accomplishments = new ArrayList<>();
        }

        public UUID getUuid() { return uuid; }
        public void setUuid(UUID uuid) { this.uuid = uuid; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public long getTermStart() { return termStart; }
        public void setTermStart(long termStart) { this.termStart = termStart; }
        public long getTermEnd() { return termEnd; }
        public void setTermEnd(long termEnd) { this.termEnd = termEnd; }
        public double getFinalApprovalRating() { return finalApprovalRating; }
        public void setFinalApprovalRating(double finalApprovalRating) { this.finalApprovalRating = finalApprovalRating; }
        public int getExecutiveOrdersIssued() { return executiveOrdersIssued; }
        public void setExecutiveOrdersIssued(int executiveOrdersIssued) { this.executiveOrdersIssued = executiveOrdersIssued; }
        public int getPresidentialGamesHosted() { return presidentialGamesHosted; }
        public void setPresidentialGamesHosted(int presidentialGamesHosted) { this.presidentialGamesHosted = presidentialGamesHosted; }
        public Map<CabinetPosition, String> getCabinetMembers() { return cabinetMembers; }
        public void setCabinetMembers(Map<CabinetPosition, String> cabinetMembers) { this.cabinetMembers = cabinetMembers; }
        public List<String> getAccomplishments() { return accomplishments; }
        public void setAccomplishments(List<String> accomplishments) { this.accomplishments = accomplishments; }
        public String getEndReason() { return endReason; }
        public void setEndReason(String endReason) { this.endReason = endReason; }

        // Alias methods for GUI compatibility
        public UUID getPlayerId() { return getUuid(); }
        public int getTerm() { return getTermDurationDays(); }
        public double getApproval() { return getFinalApprovalRating(); }
        public int getOrders() { return getExecutiveOrdersIssued(); }
        public int getGames() { return getPresidentialGamesHosted(); }
        public String getReason() { return getEndReason(); }
        
        public long getTermDuration() {
            if (termEnd == 0) return System.currentTimeMillis() - termStart;
            return termEnd - termStart;
        }
        
        public int getTermDurationDays() {
            return (int) (getTermDuration() / (24 * 60 * 60 * 1000L));
        }
    }

    public List<PresidentRecord> getRecords() { return records; }
    public void setRecords(List<PresidentRecord> records) { this.records = records; }
    
    public void addRecord(PresidentRecord record) {
        records.add(record);
    }
    
    public PresidentRecord getLatestRecord() {
        if (records.isEmpty()) return null;
        return records.get(records.size() - 1);
    }
    
    public List<PresidentRecord> getRecordsByPlayer(UUID uuid) {
        return records.stream()
            .filter(r -> r.getUuid().equals(uuid))
            .toList();
    }
    
    public int getTermCount(UUID uuid) {
        return (int) records.stream()
            .filter(r -> r.getUuid().equals(uuid))
            .count();
    }
    
    public PresidentRecord getHighestApproval() {
        return records.stream()
            .max(Comparator.comparingDouble(PresidentRecord::getFinalApprovalRating))
            .orElse(null);
    }
    
    public PresidentRecord getLongestTerm() {
        return records.stream()
            .max(Comparator.comparingLong(PresidentRecord::getTermDuration))
            .orElse(null);
    }
    
    public PresidentRecord getMostOrdersIssued() {
        return records.stream()
            .max(Comparator.comparingInt(PresidentRecord::getExecutiveOrdersIssued))
            .orElse(null);
    }
}
