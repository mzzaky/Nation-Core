package id.nationcore.models;

import java.util.UUID;

public class RepublicExecutiveOrder extends ExecutiveOrder {
    public RepublicExecutiveOrder(ExecutiveOrderType type, UUID issuedBy, long durationMillis) {
        super(type, issuedBy, durationMillis);
    }
}