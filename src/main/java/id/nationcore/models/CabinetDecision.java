package id.nationcore.models;

import java.util.UUID;

public class CabinetDecision {
    
    // Re-export CabinetPosition untuk backward compatibility dengan NationCommand
    public static enum CabinetPosition {
        DEFENSE("Minister of Defense", "&4[MoD]"),
        TREASURY("Minister of Treasury", "&2[MoT]"),
        HEALTH("Minister of Health", "&d[MoH]");
        
        private final String displayName;
        private final String prefix;
        
        CabinetPosition(String displayName, String prefix) {
            this.displayName = displayName;
            this.prefix = prefix;
        }
        
        public String getDisplayName() { return displayName; }
        public String getPrefix() { return prefix; }
        
        public Government.CabinetPosition toGovernmentPosition() {
            return Government.CabinetPosition.valueOf(this.name());
        }
    }
    
    public static enum DecisionType {
        // Defense Minister Decisions
        DECLARE_WAR("Declare War Event", CabinetPosition.DEFENSE, 24 * 60 * 60 * 1000L,
            "Opens PvP war zone with doubled loot drops"),
        MILITARY_DRAFT("Military Draft", CabinetPosition.DEFENSE, 0L,
            "Starts mandatory PvP tournament"),
        DEFENSE_PROTOCOL("Defense Protocol", CabinetPosition.DEFENSE, 12 * 60 * 60 * 1000L,
            "-20% damage taken server-wide"),
        ARMORY_DISCOUNT("Armory Discount", CabinetPosition.DEFENSE, 24 * 60 * 60 * 1000L,
            "30% off weapons & armor at NPC shops"),
        BORDER_PATROL("Border Patrol", CabinetPosition.DEFENSE, 48 * 60 * 60 * 1000L,
            "Elite guards protect spawn area"),
        
        // Treasury Minister Decisions
        TAX_HOLIDAY("Tax Holiday", CabinetPosition.TREASURY, 24 * 60 * 60 * 1000L,
            "No server tax for 24 hours"),
        ECONOMIC_STIMULUS("Economic Stimulus", CabinetPosition.TREASURY, 0L,
            "10k vault points to all online players"),
        AUCTION_BOOST("Auction Boost", CabinetPosition.TREASURY, 48 * 60 * 60 * 1000L,
            "Double auction limits, reduced fees"),
        TREASURY_BONUS("Treasury Bonus", CabinetPosition.TREASURY, 12 * 60 * 60 * 1000L,
            "+50% vault points from mobs & mining"),
        MARKET_CRASH("Market Crash", CabinetPosition.TREASURY, 6 * 60 * 60 * 1000L,
            "40% discount at admin shop"),

        // Health Minister Decisions
        QUARANTINE_PROTOCOL("Quarantine Protocol", CabinetPosition.HEALTH, 10 * 60 * 1000L,
            "Close entry access to nation territory for non-members for 10 minutes"),
        FIELD_MEDICINE("Field Medicine", CabinetPosition.HEALTH, 5 * 60 * 1000L,
            "Members receive Regeneration II for 5 minutes"),
        VACCINATION_DRIVE("Vaccination Drive", CabinetPosition.HEALTH, 60 * 60 * 1000L,
            "Members become immune to poison & wither for 1 hour"),
        EMERGENCY_RATIONS("Emergency Rations", CabinetPosition.HEALTH, 0L,
            "Distribute food from treasury to members with hunger below 50%"),
        PLAGUE("Plague", CabinetPosition.HEALTH, 10 * 60 * 1000L,
            "Enemies in territory get Weakness II + Hunger for 30s. Active 10m.");

        
        private final String displayName;
        private final CabinetPosition position;
        private final long durationMillis;
        private final String description;
        
        DecisionType(String displayName, CabinetPosition position, 
                           long durationMillis, String description) {
            this.displayName = displayName;
            this.position = position;
            this.durationMillis = durationMillis;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public CabinetPosition getPosition() { return position; }
        public long getDurationMillis() { return durationMillis; }
        public String getDescription() { return description; }
    }
    
    private DecisionType type;
    private CabinetPosition ministerPosition;
    private UUID issuedBy;
    private long issuedTime;
    private long expirationTime;
    private boolean active;
    
    public CabinetDecision(DecisionType type, CabinetPosition ministerPosition, UUID issuedBy) {
        this.type = type;
        this.ministerPosition = ministerPosition;
        this.issuedBy = issuedBy;
        this.issuedTime = System.currentTimeMillis();
        this.expirationTime = this.issuedTime + type.getDurationMillis();
        this.active = true;
    }

    public DecisionType getType() { return type; }
    public void setType(DecisionType type) { this.type = type; }
    public CabinetPosition getMinisterPosition() { return ministerPosition; }
    public void setMinisterPosition(CabinetPosition ministerPosition) { this.ministerPosition = ministerPosition; }
    public UUID getIssuedBy() { return issuedBy; }
    public void setIssuedBy(UUID issuedBy) { this.issuedBy = issuedBy; }
    public long getIssuedTime() { return issuedTime; }
    public void setIssuedTime(long issuedTime) { this.issuedTime = issuedTime; }
    public long getExpirationTime() { return expirationTime; }
    public void setExpirationTime(long expirationTime) { this.expirationTime = expirationTime; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public boolean isExpired() {
        if (type.getDurationMillis() == 0) return true;
        return System.currentTimeMillis() > expirationTime;
    }
    
    public long getRemainingTime() {
        return Math.max(0, expirationTime - System.currentTimeMillis());
    }
}
