package id.nationcore.models;

import java.util.Map;
import java.util.UUID;

/**
 * Per-nation research state.
 *
 * Holds the current level of every {@link ResearchType} that has been completed,
 * plus an optional in-progress {@link ActiveResearch}. A nation can run only one
 * research at a time — concurrent research is intentionally disallowed to keep
 * decision-making weighty.
 *
 * Persisted as a sub-object of {@link Nation} via Gson, so all fields must be
 * serializable types (primitives, strings, enums, simple maps).
 */
public class NationResearchData {

    /**
     * Snapshot of a research project that is currently being carried out by the nation.
     *
     * {@code targetLevel} is the level the nation will reach once this project
     * finishes ({@code currentLevel + 1}). {@code paidCost} is the vault amount that
     * was withdrawn at start time and is used for partial refunds when cancelling.
     */
    public static class ActiveResearch {
        private String typeId;
        private int targetLevel;
        private long startedAt;
        private long endsAt;
        private double paidCost;
        private UUID startedBy;
        private String startedByName;

        public ActiveResearch() {}

        public ActiveResearch(ResearchType type, int targetLevel, long startedAt, long endsAt,
                              double paidCost, UUID startedBy, String startedByName) {
            this.typeId = type.getId();
            this.targetLevel = targetLevel;
            this.startedAt = startedAt;
            this.endsAt = endsAt;
            this.paidCost = paidCost;
            this.startedBy = startedBy;
            this.startedByName = startedByName;
        }

        public ResearchType getType() { return ResearchType.fromId(typeId); }
        public String getTypeId() { return typeId; }
        public int getTargetLevel() { return targetLevel; }
        public long getStartedAt() { return startedAt; }
        public long getEndsAt() { return endsAt; }
        public double getPaidCost() { return paidCost; }
        public UUID getStartedBy() { return startedBy; }
        public String getStartedByName() { return startedByName; }

        public long getRemainingMillis() {
            long left = endsAt - System.currentTimeMillis();
            return Math.max(0L, left);
        }

        public boolean isFinished() {
            return System.currentTimeMillis() >= endsAt;
        }

        public double getProgressPercent() {
            long total = endsAt - startedAt;
            if (total <= 0) return 100.0;
            long elapsed = System.currentTimeMillis() - startedAt;
            double pct = (elapsed * 100.0) / total;
            if (pct < 0) pct = 0;
            if (pct > 100) pct = 100;
            return pct;
        }
    }

    private Map<String, Integer> levels;
    private ActiveResearch active;
    private long lastCompletedAt;
    private int totalProjectsCompleted;
    private double totalVaultSpent;

    public NationResearchData() {
        // HashMap<String,Integer> keyed by ResearchType.id. String keys serialize
        // cleanly through Gson without needing a TypeAdapter.
        this.levels = new java.util.HashMap<>();
    }

    public int getLevel(ResearchType type) {
        if (type == null || levels == null) return 0;
        Integer v = levels.get(type.getId());
        return v == null ? 0 : v;
    }

    public void setLevel(ResearchType type, int level) {
        if (type == null) return;
        if (levels == null) levels = new java.util.HashMap<>();
        levels.put(type.getId(), Math.max(0, level));
    }

    public Map<String, Integer> getLevels() {
        if (levels == null) levels = new java.util.HashMap<>();
        return levels;
    }

    public void setLevels(Map<String, Integer> levels) {
        this.levels = levels;
    }

    public ActiveResearch getActive() { return active; }
    public void setActive(ActiveResearch active) { this.active = active; }
    public boolean hasActive() { return active != null; }

    public long getLastCompletedAt() { return lastCompletedAt; }
    public void setLastCompletedAt(long lastCompletedAt) { this.lastCompletedAt = lastCompletedAt; }

    public int getTotalProjectsCompleted() { return totalProjectsCompleted; }
    public void setTotalProjectsCompleted(int totalProjectsCompleted) {
        this.totalProjectsCompleted = totalProjectsCompleted;
    }
    public void incrementCompleted() { this.totalProjectsCompleted++; }

    public double getTotalVaultSpent() { return totalVaultSpent; }
    public void setTotalVaultSpent(double totalVaultSpent) { this.totalVaultSpent = totalVaultSpent; }
    public void addVaultSpent(double amount) { this.totalVaultSpent += amount; }
}
