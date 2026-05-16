package id.nationcore.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent state for a Communist nation's Shared Storage.
 *
 * <p>The physical inventory contents are represented as a list of Base64-encoded
 * {@link org.bukkit.inventory.ItemStack} strings (one per slot, null entries for
 * empty slots). The companion {@link SharedStorageEntry} list tracks audit
 * metadata for every non-empty slot.</p>
 *
 * <p>Serialization strategy: Gson stores both lists as plain JSON arrays inside
 * the parent {@link Nation} object — no custom type adapters required.</p>
 *
 * <h3>Anti-exploit / safety design</h3>
 * <ul>
 *   <li>A per-player cooldown (default 500 ms) prevents click-spam duplication.</li>
 *   <li>Slot locking is applied while a player's transaction is in flight.</li>
 *   <li>All slot mutations go through {@link #setSlot} which atomically updates
 *       both the Base64 content list and the entry metadata list.</li>
 * </ul>
 */
public class SharedStorageData {

    /** Number of storage slots — 5 rows × 9 = 45 item slots. */
    public static final int STORAGE_SIZE = 45;

    /**
     * Base64-encoded ItemStack per slot. Index == slot number. Null element
     * means the slot is empty. Length is always {@value #STORAGE_SIZE}.
     */
    private List<String> serializedItems;

    /** Audit metadata for each occupied slot. Key == slot index. */
    private Map<Integer, SharedStorageEntry> entries;

    /**
     * Per-player rate-limit. Key = player UUID string, value = last interaction
     * timestamp. Transient — not really meaningful across server restarts, but
     * kept in persistent data so Gson round-trips cleanly.
     */
    private Map<String, Long> lastInteraction;

    /** Minimum milliseconds between successive interactions from the same player. */
    private static final long INTERACTION_COOLDOWN_MS = 500L;

    public SharedStorageData() {
        this.serializedItems = new ArrayList<>();
        for (int i = 0; i < STORAGE_SIZE; i++) {
            serializedItems.add(null);
        }
        this.entries = new HashMap<>();
        this.lastInteraction = new HashMap<>();
    }

    // ── Integrity / lazy init ─────────────────────────────────────────────

    /**
     * Ensures internal lists are valid (guards against Gson deserializing with
     * wrong sizes after data migrations or partial JSON).
     */
    public void ensureSize() {
        if (serializedItems == null) {
            serializedItems = new ArrayList<>();
        }
        while (serializedItems.size() < STORAGE_SIZE) {
            serializedItems.add(null);
        }
        if (entries == null) entries = new HashMap<>();
        if (lastInteraction == null) lastInteraction = new HashMap<>();
    }

    // ── Slot access ───────────────────────────────────────────────────────

    /**
     * Returns the Base64-encoded ItemStack string for the given slot,
     * or {@code null} if the slot is empty or the index is out of range.
     */
    public String getSerializedItem(int slot) {
        ensureSize();
        if (slot < 0 || slot >= STORAGE_SIZE) return null;
        return serializedItems.get(slot);
    }

    /**
     * Atomically updates a slot's serialized content and its audit entry.
     *
     * @param slot          slot index (0-based)
     * @param base64        Base64 ItemStack string, or {@code null} to clear the slot
     * @param depositor     UUID of the player performing this action
     * @param depositorName player name at the time of the action
     * @param amount        stack size (for audit display)
     * @param itemType      material name (for audit display)
     */
    public void setSlot(int slot, String base64, UUID depositor,
                        String depositorName, int amount, String itemType) {
        ensureSize();
        if (slot < 0 || slot >= STORAGE_SIZE) return;
        serializedItems.set(slot, base64);
        if (base64 == null) {
            entries.remove(slot);
        } else {
            entries.put(slot, new SharedStorageEntry(slot, depositor, depositorName, amount, itemType));
        }
    }

    /**
     * Clears a slot without updating audit (used internally for take actions
     * where we still record who took it via the entry before clearing).
     */
    public void clearSlot(int slot) {
        ensureSize();
        if (slot < 0 || slot >= STORAGE_SIZE) return;
        serializedItems.set(slot, null);
        entries.remove(slot);
    }

    // ── Rate-limit / anti-exploit ─────────────────────────────────────────

    /**
     * Checks whether the player is allowed to interact (cooldown elapsed).
     *
     * @return {@code true} if the player may proceed; {@code false} if they
     *         must wait (potential spam / dupe attempt).
     */
    public boolean canInteract(UUID playerUUID) {
        ensureSize();
        String key = playerUUID.toString();
        long last = lastInteraction.getOrDefault(key, 0L);
        return (System.currentTimeMillis() - last) >= INTERACTION_COOLDOWN_MS;
    }

    /** Records the current timestamp as the last interaction time for {@code playerUUID}. */
    public void recordInteraction(UUID playerUUID) {
        ensureSize();
        lastInteraction.put(playerUUID.toString(), System.currentTimeMillis());
    }

    // ── Metadata queries ──────────────────────────────────────────────────

    /** Returns the audit entry for the given slot, or {@code null} if no metadata exists. */
    public SharedStorageEntry getEntry(int slot) {
        if (entries == null) return null;
        return entries.get(slot);
    }

    /** Returns all audit entries as a snapshot list (safe for iteration). */
    public List<SharedStorageEntry> getAllEntries() {
        if (entries == null) return new ArrayList<>();
        return new ArrayList<>(entries.values());
    }

    /** Returns the total number of non-empty slots. */
    public int getOccupiedSlotCount() {
        ensureSize();
        int count = 0;
        for (String s : serializedItems) {
            if (s != null) count++;
        }
        return count;
    }

    // ── Getters / setters (for Gson) ──────────────────────────────────────

    public List<String> getSerializedItems() {
        ensureSize();
        return serializedItems;
    }

    public void setSerializedItems(List<String> items) {
        this.serializedItems = items;
    }

    public Map<Integer, SharedStorageEntry> getEntries() {
        if (entries == null) entries = new HashMap<>();
        return entries;
    }

    public void setEntries(Map<Integer, SharedStorageEntry> entries) {
        this.entries = entries;
    }

    public Map<String, Long> getLastInteraction() {
        if (lastInteraction == null) lastInteraction = new HashMap<>();
        return lastInteraction;
    }

    public void setLastInteraction(Map<String, Long> lastInteraction) {
        this.lastInteraction = lastInteraction;
    }
}
