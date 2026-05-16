package id.nationcore.models;

import java.util.UUID;

public abstract class ExecutiveOrder {

    protected ExecutiveOrderType type;
    protected UUID issuedBy;
    protected long issuedTime;
    protected long expirationTime;
    protected boolean active;

    public ExecutiveOrder(ExecutiveOrderType type, UUID issuedBy, long durationMillis) {
        this.type = type;
        this.issuedBy = issuedBy;
        this.issuedTime = System.currentTimeMillis();
        this.expirationTime = this.issuedTime + durationMillis;
        this.active = true;
    }

    public enum NationType {
        REPUBLIC("Republic", "President"),
        COMMUNIST("Communist Party", "Secretary General"),
        MONARCHY("Kingdom", "King"),
        CALIPHATE("Caliphate", "Caliph");

        private final String displayName;
        private final String leaderTitle;

        NationType(String displayName, String leaderTitle) {
            this.displayName = displayName;
            this.leaderTitle = leaderTitle;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getLeaderTitle() {
            return leaderTitle;
        }
    }

    public enum ExecutiveOrderType {
        GOLDEN_AGE("Golden Age Decree",
                "The President declares a Golden Age of prosperity!",
                48 * 60 * 60 * 1000L,
                "+25% XP gain, +25% vault points, +15% rare drops"),

        WAR_ECONOMY("War Economy",
                "The nation calls for its warriors! To arms!",
                72 * 60 * 60 * 1000L,
                "Double PvP kill rewards, +50% PvP damage"),

        ECONOMIC_RECOVERY("Economic Recovery Plan",
                "The Treasury opens its coffers to aid citizens in need.",
                36 * 60 * 60 * 1000L,
                "50k stimulus per player, 25% shop discount"),

        ENVIRONMENTAL_PROTECTION("Environmental Protection Act",
                "The land's bounty is blessed by presidential decree.",
                48 * 60 * 60 * 1000L,
                "Triple farming/fishing/breeding drops"),

        EDUCATION_ADVANCEMENT("Education Advancement",
                "Knowledge is power! The President invests in your growth.",
                48 * 60 * 60 * 1000L,
                "3x XP gain, -50% enchanting cost, free anvil"),

        PURGE_PROTOCOL("Purge Protocol",
                "Survival of the fittest. May the strongest prevail.",
                6 * 60 * 60 * 1000L,
                "Full PvP enabled, random respawn, no keep inventory"),

        PRESIDENTIAL_PARDON("Presidential Pardon",
                "In the spirit of mercy, past transgressions are forgiven.",
                0L,
                "Clears 1 punishment from all online players"),

        TAX_SUSPENSION("Tax Suspension",
                "By presidential decree, all tax obligations are hereby suspended for the people!",
                48 * 60 * 60 * 1000L,
                "All tax collection is disabled for the duration"),

        TAX_SURGE("Tax Surge",
                "The nation demands more! Tax rates are raised to fund the treasury!",
                24 * 60 * 60 * 1000L,
                "Tax rate increased to 5x the base rate for the duration");

        private final String displayName;
        private final String flavorText;
        private final long defaultDuration;
        private final String effectDescription;

        ExecutiveOrderType(String displayName, String flavorText, long defaultDuration, String effectDescription) {
            this.displayName = displayName;
            this.flavorText = flavorText;
            this.defaultDuration = defaultDuration;
            this.effectDescription = effectDescription;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getFlavorText() {
            return flavorText;
        }

        public long getDefaultDuration() {
            return defaultDuration;
        }

        public String getEffectDescription() {
            return effectDescription;
        }

        public String getDescription() {
            return effectDescription;
        }
    }

    public boolean canBeIssuedBy(NationType nationType, UUID playerUUID) {
        return issuedBy != null && issuedBy.equals(playerUUID);
    }

    public String getIssuedByTitle(NationType nationType) {
        return nationType.getLeaderTitle();
    }

    public ExecutiveOrderType getType() {
        return type;
    }

    public void setType(ExecutiveOrderType type) {
        this.type = type;
    }

    public UUID getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(UUID issuedBy) {
        this.issuedBy = issuedBy;
    }

    public long getIssuedTime() {
        return issuedTime;
    }

    public void setIssuedTime(long issuedTime) {
        this.issuedTime = issuedTime;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isExpired() {
        if (type.getDefaultDuration() == 0)
            return true;
        return System.currentTimeMillis() > expirationTime;
    }

    public long getRemainingTime() {
        return Math.max(0, expirationTime - System.currentTimeMillis());
    }

    public static ExecutiveOrder createForNation(NationType nationType, ExecutiveOrderType type, UUID issuedBy, long durationMillis) {
        return switch (nationType) {
            case REPUBLIC -> new RepublicExecutiveOrder(type, issuedBy, durationMillis);
            case COMMUNIST -> new CommunistExecutiveOrder(type, issuedBy, durationMillis);
            case MONARCHY -> new MonarchyExecutiveOrder(type, issuedBy, durationMillis);
            case CALIPHATE -> new CaliphateExecutiveOrder(type, issuedBy, durationMillis);
        };
    }
}
