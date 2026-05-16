package id.nationcore.models;

import java.util.UUID;

public class DiplomacyRequest {
    private String senderNationId;
    private String targetNationId;
    private DiplomacyStatus requestedStatus;
    private long requestTime;
    private UUID requestedBy;

    public DiplomacyRequest(String senderNationId, String targetNationId, DiplomacyStatus requestedStatus, UUID requestedBy) {
        this.senderNationId = senderNationId;
        this.targetNationId = targetNationId;
        this.requestedStatus = requestedStatus;
        this.requestedBy = requestedBy;
        this.requestTime = System.currentTimeMillis();
    }

    public String getSenderNationId() {
        return senderNationId;
    }

    public String getTargetNationId() {
        return targetNationId;
    }

    public DiplomacyStatus getRequestedStatus() {
        return requestedStatus;
    }

    public long getRequestTime() {
        return requestTime;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }
}
