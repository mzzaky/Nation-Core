package id.nationcore.models;

import java.util.UUID;

/**
 * Executive Order issued by the Caliph of a CALIPHATE nation.
 *
 * Same data shape as the other variants — the only difference is identity
 * (signed by the Caliph). Unlike the Monarchy variant the Caliph is NOT
 * exempt from the standard executive-order cooldown; the cooldown applies
 * exactly as it does for Republic and Communist.
 */
public class CaliphateExecutiveOrder extends ExecutiveOrder {
    public CaliphateExecutiveOrder(ExecutiveOrderType type, UUID issuedBy, long durationMillis) {
        super(type, issuedBy, durationMillis);
    }
}
