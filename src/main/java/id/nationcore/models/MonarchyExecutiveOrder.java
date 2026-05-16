package id.nationcore.models;

import java.util.UUID;

/**
 * Executive Order issued by the King of a MONARCHY nation.
 *
 * Same data shape as the other variants — the difference is purely identity:
 * the order is signed by the King and the issuing-cooldown logic in the
 * Monarchy manager skips the standard delay so the King can issue orders
 * freely, reflecting the absolute power of the monarch.
 */
public class MonarchyExecutiveOrder extends ExecutiveOrder {
    public MonarchyExecutiveOrder(ExecutiveOrderType type, UUID issuedBy, long durationMillis) {
        super(type, issuedBy, durationMillis);
    }
}
