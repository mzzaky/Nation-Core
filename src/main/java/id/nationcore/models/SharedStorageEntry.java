package id.nationcore.models;

import java.util.UUID;

/**
 * Tracks metadata for a single item slot deposited into the Communist nation's
 * Shared Storage. Stored alongside the serialized ItemStack so admins / players
 * can audit who deposited what and when.
 *
 * <p>Instances are persisted as part of {@link SharedStorageData} inside each
 * {@link Nation} via Gson — keep all field types Gson-friendly (primitives,
 * String, UUID via toString).</p>
 */
public class SharedStorageEntry {

    /** Inventory slot index (0-based, within the storage inventory). */
    private int slot;

    /** UUID of the player who last placed/updated this slot. */
    private String depositorUUID;

    /** Display name of the depositor at the time of deposit. */
    private String depositorName;

    /** System.currentTimeMillis() when this slot was last written. */
    private long depositedAt;

    /**
     * Snapshot of how many items were in the stack when deposited.
     * Used for display only — actual item count is in the ItemStack itself.
     */
    private int depositedAmount;

    /** Human-readable item type name (e.g. "DIAMOND") for display in lore. */
    private String itemType;

    public SharedStorageEntry() {}

    public SharedStorageEntry(int slot, UUID depositorUUID, String depositorName,
                               int depositedAmount, String itemType) {
        this.slot = slot;
        this.depositorUUID = depositorUUID.toString();
        this.depositorName = depositorName;
        this.depositedAt = System.currentTimeMillis();
        this.depositedAmount = depositedAmount;
        this.itemType = itemType;
    }

    // ── Getters & setters ─────────────────────────────────────────────────

    public int getSlot() { return slot; }
    public void setSlot(int slot) { this.slot = slot; }

    public String getDepositorUUIDString() { return depositorUUID; }

    public UUID getDepositorUUID() {
        return depositorUUID != null ? UUID.fromString(depositorUUID) : null;
    }

    public void setDepositorUUID(UUID uuid) {
        this.depositorUUID = uuid != null ? uuid.toString() : null;
    }

    public String getDepositorName() { return depositorName; }
    public void setDepositorName(String name) { this.depositorName = name; }

    public long getDepositedAt() { return depositedAt; }
    public void setDepositedAt(long depositedAt) { this.depositedAt = depositedAt; }

    public int getDepositedAmount() { return depositedAmount; }
    public void setDepositedAmount(int amount) { this.depositedAmount = amount; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
}
