package id.nationcore.models;

import java.util.*;

public class Government {

    private UUID presidentUUID;
    private String presidentName;
    private long termStartTime;
    private int consecutiveTerms;
    private Map<CabinetPosition, CabinetMember> cabinet;
    private long lastDailyReward;
    private long lastPresidentActivity;
    private List<ApprovalRating> approvalRatings;
    private double currentApprovalRating;

    private long lastBroadcastTime;

    public Government() {
        this.cabinet = new EnumMap<>(CabinetPosition.class);
        this.approvalRatings = new ArrayList<>();
        this.consecutiveTerms = 0;
        this.currentApprovalRating = 3.0;
        this.totalSalaryPayouts = 0.0;
        this.lastBroadcastTime = 0;
    }

    private double totalSalaryPayouts;

    public double getTotalSalaryPayouts() {
        return totalSalaryPayouts;
    }

    public void setTotalSalaryPayouts(double totalSalaryPayouts) {
        this.totalSalaryPayouts = totalSalaryPayouts;
    }

    public void addSalaryPayout(double amount) {
        this.totalSalaryPayouts += amount;
    }

    public enum CabinetPosition {
        DEFENSE("Minister of Defense", "&4[MoD]"),
        TREASURY("Minister of Treasury", "&2[MoT]"),
        HEALTH("Minister of Health", "&d[MoH]");

        private final String displayName;
        private final String prefix;

        CabinetPosition(String displayName, String prefix) {
            this.displayName = displayName;
            this.prefix = prefix;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getPrefix() {
            return prefix;
        }
    }

    public static class CabinetMember {
        private UUID uuid;
        private String name;
        private CabinetPosition position;
        private long appointedTime;
        private long lastDecisionTime;
        private int decisionsUsed;
        private long lastDailyReward;

        public CabinetMember(UUID uuid, String name, CabinetPosition position) {
            this.uuid = uuid;
            this.name = name;
            this.position = position;
            this.appointedTime = System.currentTimeMillis();
            this.lastDecisionTime = 0;
            this.decisionsUsed = 0;
            this.lastDailyReward = 0;
        }

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

        public CabinetPosition getPosition() {
            return position;
        }

        public void setPosition(CabinetPosition position) {
            this.position = position;
        }

        public long getAppointedTime() {
            return appointedTime;
        }

        public void setAppointedTime(long appointedTime) {
            this.appointedTime = appointedTime;
        }

        public long getLastDecisionTime() {
            return lastDecisionTime;
        }

        public void setLastDecisionTime(long lastDecisionTime) {
            this.lastDecisionTime = lastDecisionTime;
        }

        public int getDecisionsUsed() {
            return decisionsUsed;
        }

        public void setDecisionsUsed(int decisionsUsed) {
            this.decisionsUsed = decisionsUsed;
        }

        public long getLastDailyReward() {
            return lastDailyReward;
        }

        public void setLastDailyReward(long lastDailyReward) {
            this.lastDailyReward = lastDailyReward;
        }
    }

    public static class ApprovalRating {
        private UUID voterUUID;
        private int rating;
        private long timestamp;

        public ApprovalRating(UUID voterUUID, int rating) {
            this.voterUUID = voterUUID;
            this.rating = rating;
            this.timestamp = System.currentTimeMillis();
        }

        public UUID getVoterUUID() {
            return voterUUID;
        }

        public int getRating() {
            return rating;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setRating(int rating) {
            this.rating = rating;
            this.timestamp = System.currentTimeMillis();
        }
    }

    // Getters and Setters
    public UUID getPresidentUUID() {
        return presidentUUID;
    }

    public void setPresidentUUID(UUID presidentUUID) {
        this.presidentUUID = presidentUUID;
    }

    public String getPresidentName() {
        return presidentName;
    }

    public void setPresidentName(String presidentName) {
        this.presidentName = presidentName;
    }

    public long getTermStartTime() {
        return termStartTime;
    }

    public void setTermStartTime(long termStartTime) {
        this.termStartTime = termStartTime;
    }

    public int getConsecutiveTerms() {
        return consecutiveTerms;
    }

    public void setConsecutiveTerms(int consecutiveTerms) {
        this.consecutiveTerms = consecutiveTerms;
    }

    public Map<CabinetPosition, CabinetMember> getCabinet() {
        return cabinet;
    }

    public void setCabinet(Map<CabinetPosition, CabinetMember> cabinet) {
        this.cabinet = cabinet;
    }

    public long getLastDailyReward() {
        return lastDailyReward;
    }

    public void setLastDailyReward(long lastDailyReward) {
        this.lastDailyReward = lastDailyReward;
    }

    public long getLastPresidentActivity() {
        return lastPresidentActivity;
    }

    public void setLastPresidentActivity(long lastPresidentActivity) {
        this.lastPresidentActivity = lastPresidentActivity;
    }

    public List<ApprovalRating> getApprovalRatings() {
        return approvalRatings;
    }

    public void setApprovalRatings(List<ApprovalRating> approvalRatings) {
        this.approvalRatings = approvalRatings;
    }

    public double getCurrentApprovalRating() {
        return currentApprovalRating;
    }

    public void setCurrentApprovalRating(double currentApprovalRating) {
        this.currentApprovalRating = currentApprovalRating;
    }

    public long getLastBroadcastTime() {
        return lastBroadcastTime;
    }

    public void setLastBroadcastTime(long lastBroadcastTime) {
        this.lastBroadcastTime = lastBroadcastTime;
    }

    public boolean hasPresident() {
        return presidentUUID != null;
    }

    public int getCurrentTerm() {
        return consecutiveTerms;
    }

    public long getTermEndTime() {
        // Term berlangsung 14 hari (2 minggu)
        long termDuration = 14L * 24 * 60 * 60 * 1000; // 14 days in milliseconds
        return termStartTime + termDuration;
    }

    public double getApprovalRating() {
        return currentApprovalRating;
    }

    public UUID getCabinetMember(CabinetPosition position) {
        CabinetMember member = cabinet.get(position);
        return member != null ? member.getUuid() : null;
    }

    public void calculateApprovalRating() {
        if (approvalRatings.isEmpty()) {
            currentApprovalRating = 3.0;
            return;
        }
        double sum = 0;
        for (ApprovalRating rating : approvalRatings) {
            sum += rating.getRating();
        }
        currentApprovalRating = sum / approvalRatings.size();
    }

    public CabinetMember getCabinetMemberObject(CabinetPosition position) {
        return cabinet.get(position);
    }

    public void appointCabinet(CabinetPosition position, CabinetMember member) {
        cabinet.put(position, member);
    }

    public void removeCabinet(CabinetPosition position) {
        cabinet.remove(position);
    }

    public CabinetMember getCabinetMemberByUUID(UUID uuid) {
        for (CabinetMember member : cabinet.values()) {
            if (member.getUuid().equals(uuid)) {
                return member;
            }
        }
        return null;
    }

    public CabinetPosition getPositionByUUID(UUID uuid) {
        for (Map.Entry<CabinetPosition, CabinetMember> entry : cabinet.entrySet()) {
            if (entry.getValue().getUuid().equals(uuid)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
