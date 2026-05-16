package id.nationcore.models;

import java.util.UUID;

public class CommunistExecutiveOrder extends ExecutiveOrder {
    public CommunistExecutiveOrder(ExecutiveOrderType type, UUID issuedBy, long durationMillis) {
        super(type, issuedBy, durationMillis);
    }
}