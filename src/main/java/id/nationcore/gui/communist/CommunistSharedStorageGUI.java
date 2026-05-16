package id.nationcore.gui.communist;

import id.nationcore.gui.GUIListener;
import id.nationcore.gui.MainMenuRouter;
import id.nationcore.gui.NationMenuBase;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import id.nationcore.NationCore;
import id.nationcore.models.Nation;
import id.nationcore.models.SharedStorageData;
import id.nationcore.models.SharedStorageEntry;

/**
 * Shared Storage GUI for COMMUNIST nations.
 *
 * <h3>Layout (6 rows — 54 slots)</h3>
 * <pre>
 * Row 0  [BORDER] × 9   ← red glass pane header
 * Row 1  [ITEM]  × 9   ┐
 * Row 2  [ITEM]  × 9   │  45 item slots (storage area)
 * Row 3  [ITEM]  × 9   │
 * Row 4  [ITEM]  × 9   │
 * Row 5  [ITEM]  × 9   ┘  ← includes border left/right + back button slot 49
 * </pre>
 *
 * <p>All 45 content slots (9–53) are open for deposit/withdraw by every
 * nation member with no restrictions. Each item carries lore metadata
 * injected at deposit time (who stored it, when, how many).</p>
 *
 * <h3>Anti-exploit measures</h3>
 * <ol>
 *   <li>Per-player 500 ms cooldown — rejects suspiciously rapid clicks.</li>
 *   <li>Slot-level locking set during the async save tick prevents two players
 *       from modifying the same slot simultaneously.</li>
 *   <li>Items are validated before placement (stack-size capped at vanilla max,
 *       air items rejected).</li>
 *   <li>All take actions clear the metadata entry to prevent ghost items.</li>
 * </ol>
 */
@SuppressWarnings("deprecation")
public class CommunistSharedStorageGUI extends NationMenuBase {

    public static final String TITLE =
            ChatColor.translateAlternateColorCodes('&', "&4&l☭ Shared Storage &8[Communist]");

    /** The single border material for this GUI (matches communist theme). */
    private static final Material BORDER_MAT = Material.RED_STAINED_GLASS_PANE;

    /**
     * Row 0 border slots (top row).
     * Bottom row handled separately with back-button in slot 49.
     */
    private static final int[] BORDER_TOP = {0, 1, 2, 3, 4, 5, 6, 7, 8};

    /**
     * Bottom row slots that are borders (all except back-button slot 49).
     */
    private static final int[] BORDER_BOTTOM = {45, 46, 47, 48, 50, 51, 52, 53};

    /** Slot for the back button — bottom-center. */
    private static final int SLOT_BACK = 49;

    /** Slots available for item storage: rows 1-5 minus borders = slots 9–53. */
    private static final int STORAGE_START = 9;
    private static final int STORAGE_END   = 53; // inclusive

    /**
     * Slots currently "locked" by an in-progress save.
     * Using a synchronized set to avoid concurrent modification.
     */
    private final Set<String> lockedSlots = java.util.Collections.synchronizedSet(new HashSet<>());

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public CommunistSharedStorageGUI(NationCore plugin) {
        super(plugin);
    }

    // =========================================================================
    // GUI opening
    // =========================================================================

    /**
     * Opens the Shared Storage inventory for {@code player}.
     *
     * @param player the player to open it for
     * @param nation the player's communist nation
     */
    public void open(Player player, Nation nation) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // ── Place top border ─────────────────────────────────────────────
        ItemStack border = buildBorder();
        for (int s : BORDER_TOP) {
            inv.setItem(s, border);
        }

        // ── Place bottom border ──────────────────────────────────────────
        for (int s : BORDER_BOTTOM) {
            inv.setItem(s, border);
        }

        // ── Info item (slot 4, centre of top border) ─────────────────────
        inv.setItem(4, buildInfoItem(nation));

        // ── Back button ──────────────────────────────────────────────────
        inv.setItem(SLOT_BACK, buildIcon(Material.SPECTRAL_ARROW,
                "&7&lBack",
                "&7Return to Politburo Hall"));

        // ── Populate storage slots from persisted data ───────────────────
        SharedStorageData storage = nation.getSharedStorageData();
        for (int slot = STORAGE_START; slot <= STORAGE_END; slot++) {
            int storageIndex = slot - STORAGE_START; // 0-based index
            String base64 = storage.getSerializedItem(storageIndex);
            if (base64 != null) {
                ItemStack item = deserialize(base64);
                if (item != null && item.getType() != Material.AIR) {
                    ItemStack decorated = decorateWithMeta(item, storage.getEntry(storageIndex));
                    inv.setItem(slot, decorated);
                }
            }
        }

        player.openInventory(inv);
    }

    // =========================================================================
    // Click handling (called from GUIListener)
    // =========================================================================

    /**
     * Handles a click inside the Shared Storage inventory.
     *
     * @param event    the original InventoryClickEvent (already cancelled)
     * @param player   the clicking player
     * @param nation   the player's nation
     * @param rawSlot  the raw (top-inventory) slot that was clicked
     * @param clicked  the ItemStack that was clicked
     * @param cursor   the item currently on the player's cursor
     */
    public void handleClick(
            org.bukkit.event.inventory.InventoryClickEvent event,
            Player player,
            Nation nation,
            int rawSlot,
            ItemStack clicked,
            ItemStack cursor) {

        SharedStorageData storage = nation.getSharedStorageData();
        boolean isTopInventory = rawSlot < event.getView().getTopInventory().getSize();

        // ── Handle clicks in the player's own inventory ──────────────────
        if (!isTopInventory) {
            if (event.isShiftClick() && clicked != null && clicked.getType() != Material.AIR) {
                event.setCancelled(true);
                handleShiftDeposit(event, player, nation, storage, clicked);
            }
            return; // Allow normal clicks in player inventory (to pick up items)
        }

        // ── Handle clicks in the Top Inventory (Storage GUI) ─────────────
        event.setCancelled(true);

        // ── Back button ──────────────────────────────────────────────────
        if (rawSlot == SLOT_BACK) {
            id.nationcore.gui.MainMenuRouter router =
                    new id.nationcore.gui.MainMenuRouter(plugin);
            router.openFor(player);
            return;
        }

        // ── Border / out-of-bounds clicks are already cancelled by caller ─
        if (rawSlot < STORAGE_START || rawSlot > STORAGE_END) {
            return; // non-storage slot; do nothing
        }

        int storageIndex = rawSlot - STORAGE_START;

        // ── Rate-limit check ─────────────────────────────────────────────
        if (!storage.canInteract(player.getUniqueId())) {
            id.nationcore.utils.MessageUtils.send(player,
                    "<red>Please slow down — shared storage rate limit active.</red>");
            return;
        }

        // ── Slot-lock check ──────────────────────────────────────────────
        String lockKey = nation.getId() + ":" + storageIndex;
        if (lockedSlots.contains(lockKey)) {
            id.nationcore.utils.MessageUtils.send(player,
                    "<red>That slot is currently being updated. Please try again.</red>");
            return;
        }

        // Record interaction time (regardless of what happens next)
        storage.recordInteraction(player.getUniqueId());

        // Determine click type
        boolean hasCursor = cursor != null && cursor.getType() != Material.AIR;
        boolean hasItem   = clicked != null && clicked.getType() != Material.AIR;

        if (event.isShiftClick()) {
            if (hasItem) {
                // Shift-click in storage -> withdraw to inventory
                handleWithdraw(event, player, nation, storage, storageIndex, rawSlot, clicked);
            }
            return;
        }

        if (hasCursor) {
            // ── DEPOSIT: player has item on cursor → place into storage (or swap) ──
            handleDeposit(event, player, nation, storage, storageIndex, rawSlot, cursor, clicked);
        } else if (hasItem) {
            // ── WITHDRAW: player takes from storage ──────────────────────
            handleWithdraw(event, player, nation, storage, storageIndex, rawSlot, clicked);
        }
        // else: both empty — nothing to do
    }

    private void handleShiftDeposit(
            org.bukkit.event.inventory.InventoryClickEvent event,
            Player player,
            Nation nation,
            SharedStorageData storage,
            ItemStack toDeposit) {

        // Find first empty storage slot
        int targetSlot = -1;
        for (int slot = STORAGE_START; slot <= STORAGE_END; slot++) {
            int idx = slot - STORAGE_START;
            if (storage.getSerializedItem(idx) == null) {
                targetSlot = slot;
                break;
            }
        }

        if (targetSlot == -1) {
            id.nationcore.utils.MessageUtils.send(player, "<red>Shared Storage is full!</red>");
            return;
        }

        int storageIndex = targetSlot - STORAGE_START;
        handleDeposit(event, player, nation, storage, storageIndex, targetSlot, toDeposit, null);

        // Remove from player inventory since handleDeposit doesn't know it came from a slot
        event.setCurrentItem(null);
    }

    // =========================================================================
    // Deposit logic
    // =========================================================================

    private void handleDeposit(
            org.bukkit.event.inventory.InventoryClickEvent event,
            Player player,
            Nation nation,
            SharedStorageData storage,
            int storageIndex,
            int rawSlot,
            ItemStack cursor,
            ItemStack existing) {

        // Validate cursor item
        if (cursor == null || cursor.getType() == Material.AIR) return;

        // Cap stack at vanilla maximum
        int maxStack = cursor.getType().getMaxStackSize();
        if (cursor.getAmount() > maxStack) {
            id.nationcore.utils.MessageUtils.send(player,
                    "<red>Invalid item stack size detected.</red>");
            return;
        }

        // If slot already has an item, only allow stacking same material
        if (existing != null && existing.getType() != Material.AIR) {
            // Different types → not allowed on a simple left-click deposit to occupied slot;
            // let the player swap via right-click shift mechanics normally handled by the
            // deposit call below (we just allow it — vanilla swap is fine here).
        }

        // Allow the click to proceed (event stays cancelled; we do it manually)
        String lockKey = nation.getId() + ":" + storageIndex;
        lockedSlots.add(lockKey);

        // What will actually end up in the slot after placement
        ItemStack toStore = stripLoreMeta(cursor.clone());
        String base64 = serialize(toStore);

        if (base64 == null) {
            lockedSlots.remove(lockKey);
            id.nationcore.utils.MessageUtils.send(player,
                    "<red>Failed to serialize item. Deposit cancelled.</red>");
            return;
        }

        // Place item into storage model
        storage.setSlot(
                storageIndex,
                base64,
                player.getUniqueId(),
                player.getName(),
                toStore.getAmount(),
                toStore.getType().name());

        // Update the visual inventory slot with decorated item
        ItemStack decorated = decorateWithMeta(toStore, storage.getEntry(storageIndex));
        event.getInventory().setItem(rawSlot, decorated);

        // Clear or Swap cursor
        if (existing != null && existing.getType() != Material.AIR) {
            // SWAP: Give the previous item (cleaned) back to player
            ItemStack existingClean = stripLoreMeta(existing.clone());
            player.setItemOnCursor(existingClean);
        } else {
            // Simple deposit
            player.setItemOnCursor(null);
        }
        player.updateInventory();

        // Async save
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDataManager().saveNations();
            lockedSlots.remove(lockKey);
        });

        id.nationcore.utils.MessageUtils.send(player,
                "<green>Deposited <white>" + toStore.getAmount() + "x "
                + friendlyName(toStore.getType())
                + "</white> into Shared Storage.</green>");
    }

    // =========================================================================
    // Withdraw logic
    // =========================================================================

    private void handleWithdraw(
            org.bukkit.event.inventory.InventoryClickEvent event,
            Player player,
            Nation nation,
            SharedStorageData storage,
            int storageIndex,
            int rawSlot,
            ItemStack clicked) {

        if (clicked == null || clicked.getType() == Material.AIR) return;

        String lockKey = nation.getId() + ":" + storageIndex;
        lockedSlots.add(lockKey);

        // Strip our injected lore before handing back to player
        ItemStack clean = stripLoreMeta(clicked.clone());

        // Give to player cursor (if cursor is empty and NOT a shift-click)
        if (!event.isShiftClick() && (player.getItemOnCursor() == null
                || player.getItemOnCursor().getType() == Material.AIR)) {
            player.setItemOnCursor(clean);
        } else {
            // Shift-click or cursor occupied — try to add to inventory or drop
            if (!player.getInventory().addItem(clean).isEmpty()) {
                player.getWorld().dropItemNaturally(player.getLocation(), clean);
                id.nationcore.utils.MessageUtils.send(player,
                        "<yellow>Your inventory was full — item dropped at your feet.</yellow>");
            }
        }

        // Clear slot from storage model and visual inventory
        storage.clearSlot(storageIndex);
        event.getInventory().setItem(rawSlot, null);
        player.updateInventory();

        // Async save
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDataManager().saveNations();
            lockedSlots.remove(lockKey);
        });

        id.nationcore.utils.MessageUtils.send(player,
                "<green>Withdrew <white>" + clean.getAmount() + "x "
                + friendlyName(clean.getType())
                + "</white> from Shared Storage.</green>");
    }

    // =========================================================================
    // Drag handling — called from GUIListener.onInventoryDrag
    // =========================================================================

    /**
     * Handles drag events inside the storage GUI.
     * We allow drags only within the storage area; any drag that touches a
     * border/control slot is cancelled.
     *
     * @return {@code true} if the drag was fully handled (and should be cancelled
     *         by the caller), {@code false} if it can be allowed as-is.
     */
    public boolean handleDrag(
            org.bukkit.event.inventory.InventoryDragEvent event,
            Player player,
            Nation nation) {

        // If any dragged slot is outside the storage area, cancel entirely
        for (int slot : event.getRawSlots()) {
            if (slot < STORAGE_START || slot > STORAGE_END) {
                return true; // cancel
            }
        }

        // All slots are within storage area — allow vanilla drag, then sync storage model
        Bukkit.getScheduler().runTask(plugin, () -> {
            // After drag completes, sync all affected slots to the storage model
            Inventory inv = player.getOpenInventory().getTopInventory();
            SharedStorageData storage = nation.getSharedStorageData();

            for (int slot : event.getRawSlots()) {
                if (slot < STORAGE_START || slot > STORAGE_END) continue;
                int storageIndex = slot - STORAGE_START;
                ItemStack item = inv.getItem(slot);
                if (item == null || item.getType() == Material.AIR) {
                    storage.clearSlot(storageIndex);
                } else {
                    ItemStack clean = stripLoreMeta(item.clone());
                    String base64 = serialize(clean);
                    if (base64 != null) {
                        storage.setSlot(storageIndex, base64,
                                player.getUniqueId(), player.getName(),
                                clean.getAmount(), clean.getType().name());
                        // Re-decorate with metadata lore
                        inv.setItem(slot, decorateWithMeta(clean, storage.getEntry(storageIndex)));
                    }
                }
            }
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    plugin.getDataManager().saveNations());
        });

        return false; // allow the drag
    }

    // =========================================================================
    // Item helpers
    // =========================================================================

    /** Builds the red-glass-pane border item. */
    private ItemStack buildBorder() {
        ItemStack pane = new ItemStack(BORDER_MAT);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    /** Builds the info / title item displayed in the centre of the top border. */
    private ItemStack buildInfoItem(Nation nation) {
        SharedStorageData storage = nation.getSharedStorageData();
        int occupied = storage.getOccupiedSlotCount();

        return buildIcon(Material.CHEST,
                "&4&l☭ Shared Storage",
                "",
                "&7Nation: &c" + nation.getName(),
                "&7Slots used: &f" + occupied + " &8/ &f" + SharedStorageData.STORAGE_SIZE,
                "",
                "&7All nation members can freely",
                "&7deposit and withdraw items.",
                "",
                "&8Items are tracked with depositor",
                "&8name, time, and quantity.");
    }

    /**
     * Injects audit lore into an item copy — the original item's lore is
     * preserved; our metadata lines are appended below a separator.
     */
    private ItemStack decorateWithMeta(ItemStack base, SharedStorageEntry entry) {
        if (base == null) return null;
        ItemStack copy = base.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) return copy;

        List<String> lore = meta.getLore() != null
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>();

        // Remove any previously injected meta lines before re-adding
        lore = stripInjectedLore(lore);

        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        if (entry != null) {
            String time = DATE_FORMAT.format(new Date(entry.getDepositedAt()));
            lore.add(ChatColor.GRAY + "Deposited by: " + ChatColor.YELLOW + entry.getDepositorName());
            lore.add(ChatColor.GRAY + "Time: " + ChatColor.WHITE + time);
            lore.add(ChatColor.GRAY + "Amount: " + ChatColor.WHITE + entry.getDepositedAmount() + "x");
        } else {
            lore.add(ChatColor.GRAY + "Deposited by: " + ChatColor.DARK_GRAY + "Unknown");
        }
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        meta.setLore(lore);
        copy.setItemMeta(meta);
        return copy;
    }

    /**
     * Removes our injected lore lines (between the ▬ separators) from a lore
     * list so the clean item is returned to players without plugin metadata.
     */
    private ItemStack stripLoreMeta(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return item;
        meta.setLore(stripInjectedLore(lore));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Returns a copy of {@code lore} with our injected ▬-block removed.
     */
    private List<String> stripInjectedLore(List<String> lore) {
        if (lore == null) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        boolean inBlock = false;
        for (String line : lore) {
            String stripped = ChatColor.stripColor(line);
            if (stripped != null && stripped.startsWith("▬▬▬")) {
                inBlock = !inBlock;
                continue;
            }
            if (!inBlock) {
                result.add(line);
            }
        }
        return result;
    }

    // =========================================================================
    // Serialization / deserialization (Base64)
    // =========================================================================

    /**
     * Serializes an {@link ItemStack} to a Base64 string using modern byte serialization.
     *
     * @return Base64 string, or {@code null} on error.
     */
    public static String serialize(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        try {
            byte[] bytes = item.serializeAsBytes();
            return java.util.Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Deserializes a Base64 string back into an {@link ItemStack} using modern byte serialization.
     *
     * @return the ItemStack, or {@code null} on error.
     */
    public static ItemStack deserialize(String base64) {
        if (base64 == null || base64.isBlank()) return null;
        try {
            byte[] bytes = java.util.Base64.getDecoder().decode(base64);
            return ItemStack.deserializeBytes(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    // =========================================================================
    // Utility helpers
    // =========================================================================

    /** Converts a {@link Material} name to a more human-readable form. */
    private static String friendlyName(Material mat) {
        return mat.name().replace('_', ' ').toLowerCase();
    }

    /** Returns {@code true} if the given inventory title matches this GUI. */
    public static boolean isStorageTitle(String title) {
        return TITLE.equals(title);
    }

    /** Returns {@code true} if {@code slot} is a storage content slot. */
    public static boolean isStorageSlot(int slot) {
        return slot >= STORAGE_START && slot <= STORAGE_END;
    }

    /** Returns {@code true} if {@code slot} is a border / control slot. */
    public static boolean isBorderSlot(int slot) {
        if (slot == SLOT_BACK) return false; // handled separately
        for (int s : BORDER_TOP) if (s == slot) return true;
        for (int s : BORDER_BOTTOM) if (s == slot) return true;
        return slot == 4; // info item
    }
}
