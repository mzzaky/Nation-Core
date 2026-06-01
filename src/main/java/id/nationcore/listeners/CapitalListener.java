package id.nationcore.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.projectiles.ProjectileSource;

import id.nationcore.NationCore;
import id.nationcore.models.CommunistGovernment;
import id.nationcore.models.Government;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Anti-grief & territory awareness untuk ibukota nation.
 *
 * Aturan default (configurable via {@code nation.capital.protect}):
 *   • Non-anggota tidak boleh break/place blok, akses container/door/redstone,
 *     atau memberi damage ke entity (PvP) di dalam teritori.
 *   • Ledakan (creeper, TNT, dll.) tidak merusak blok di dalam teritori.
 *   • Anggota nation TIDAK saling bisa attack di teritori sendiri (safe-zone).
 *
 * Notifikasi action bar saat pemain crossing teritori — tracker terakhir di-cache
 * via {@link #lastNation} agar tidak spam tiap PlayerMoveEvent.
 */
public class CapitalListener implements Listener {

    private final NationCore plugin;

    /** Track nation terakhir untuk setiap pemain — untuk deteksi enter/leave. */
    private final Map<UUID, String> lastNation = new HashMap<>();

    public CapitalListener(NationCore plugin) {
        this.plugin = plugin;
    }

    private boolean isProtectionEnabled() {
        return plugin.getConfig().getBoolean("nation.capital.protect", true);
    }

    private boolean canModifyAt(Player player, Location loc) {
        if (!isProtectionEnabled()) return true;
        if (player == null) return false;
        if (player.hasPermission("nation.admin")) return true;
        return !plugin.getTerritoryManager().isProtectedFrom(loc, player.getUniqueId());
    }

    // ---------------------------------------------------------------
    // Block protection
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!canModifyAt(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            sendDeniedMessage(event.getPlayer(), event.getBlock().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!canModifyAt(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            sendDeniedMessage(event.getPlayer(), event.getBlock().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!canModifyAt(event.getPlayer(), event.getBlockClicked().getLocation())) {
            event.setCancelled(true);
            sendDeniedMessage(event.getPlayer(), event.getBlockClicked().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!canModifyAt(event.getPlayer(), event.getBlockClicked().getLocation())) {
            event.setCancelled(true);
            sendDeniedMessage(event.getPlayer(), event.getBlockClicked().getLocation());
        }
    }

    // Container, door, button, redstone interaction
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                && event.getAction() != Action.PHYSICAL) return;
        if (event.getClickedBlock() == null) return;

        Material type = event.getClickedBlock().getType();
        boolean sensitive = isSensitiveInteraction(type, event.getClickedBlock().getState());
        if (!sensitive) return;

        if (!canModifyAt(event.getPlayer(), event.getClickedBlock().getLocation())) {
            event.setCancelled(true);
            sendDeniedMessage(event.getPlayer(), event.getClickedBlock().getLocation());
        }
    }

    private boolean isSensitiveInteraction(Material type, BlockState state) {
        if (state instanceof InventoryHolder) return true; // chest, furnace, hopper, dispenser, dll.
        String name = type.name();
        if (name.endsWith("_DOOR") || name.endsWith("_TRAPDOOR") || name.endsWith("_FENCE_GATE")) return true;
        if (name.endsWith("_BUTTON") || name.endsWith("_PRESSURE_PLATE")) return true;
        return switch (type) {
            case LEVER, REPEATER, COMPARATOR, NOTE_BLOCK, JUKEBOX, DAYLIGHT_DETECTOR,
                    ANVIL, CHIPPED_ANVIL, DAMAGED_ANVIL, ENCHANTING_TABLE, BEACON,
                    ENDER_CHEST, BREWING_STAND, GRINDSTONE, SMITHING_TABLE, LOOM,
                    CARTOGRAPHY_TABLE, STONECUTTER, LECTERN, BELL, COMPOSTER,
                    RESPAWN_ANCHOR, DRAGON_EGG, ITEM_FRAME, GLOW_ITEM_FRAME,
                    PAINTING -> true;
            default -> false;
        };
    }

    // ---------------------------------------------------------------
    // Hanging entities (item frames, paintings)
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (!isProtectionEnabled()) return;
        Entity remover = event.getRemover();
        UUID uuid = null;
        if (remover instanceof Player p) {
            uuid = p.getUniqueId();
            if (p.hasPermission("nation.admin")) return;
        } else if (remover instanceof Projectile proj && proj.getShooter() instanceof Player shooter) {
            uuid = shooter.getUniqueId();
            if (shooter.hasPermission("nation.admin")) return;
        }
        if (uuid == null) {
            // Lingkungan (mob, dll.) — block kalau di teritori
            if (plugin.getTerritoryManager().getNationAt(event.getEntity().getLocation()) != null) {
                event.setCancelled(true);
            }
            return;
        }
        if (plugin.getTerritoryManager().isProtectedFrom(event.getEntity().getLocation(), uuid)) {
            event.setCancelled(true);
        }
    }

    // ---------------------------------------------------------------
    // Explosion protection — block damage di dalam teritori
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!isProtectionEnabled()) return;
        event.blockList().removeIf(block ->
                plugin.getTerritoryManager().getNationAt(block.getLocation()) != null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!isProtectionEnabled()) return;
        event.blockList().removeIf(block ->
                plugin.getTerritoryManager().getNationAt(block.getLocation()) != null);
    }

    // ---------------------------------------------------------------
    // PvP — teritori sebagai safe-zone
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!isProtectionEnabled()) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) return;
        if (attacker.hasPermission("nation.admin")) return;

        Nation atLoc = plugin.getTerritoryManager().getNationAt(victim.getLocation());
        if (atLoc == null) return;

        // Default Phase 3: PvP di teritori dilarang TANPA pengecualian.
        // Phase 5 (Diplomacy/War) akan override aturan ini bila status WAR.
        event.setCancelled(true);
        MessageUtils.send(attacker, "<red>PvP dilarang di teritori " + atLoc.getName() + ".</red>");
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj) {
            ProjectileSource src = proj.getShooter();
            if (src instanceof Player p) return p;
        }
        return null;
    }

    // ---------------------------------------------------------------
    // Notifikasi enter/leave teritori (action bar)
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return; // optimasi: hanya proses saat melintasi block boundary
        }
        Player player = event.getPlayer();
        Nation atLoc = plugin.getTerritoryManager().getNationAt(event.getTo());
        String currentId = atLoc != null ? atLoc.getId() : null;
        String previousId = lastNation.get(player.getUniqueId());

        if (java.util.Objects.equals(currentId, previousId)) {
            // Tidak crossing boundary tapi masih di teritori nation Komunis aktif
            // Plague — apply ke non-anggota tiap kali player move (dampener: stamp time).
            applyPlagueTrigger(atLoc, player);
            return;
        }
        lastNation.put(player.getUniqueId(), currentId);

        // Quarantine Protocol (Komunis / Republic): non-anggota tidak boleh masuk teritori
        if (atLoc != null && !atLoc.isMember(player.getUniqueId()) && !player.hasPermission("nation.admin")) {
            if (atLoc.getType() == GovernmentType.COMMUNIST) {
                CommunistGovernment cg = atLoc.getCommunistGovernment();
                if (cg != null && cg.isQuarantineActive()) {
                    event.setCancelled(true);
                    player.sendActionBar(Component.text(
                            "🚧 Quarantine Protocol — akses ke " + atLoc.getName() + " ditutup",
                            NamedTextColor.RED));
                    return;
                }
            } else if (atLoc.getType() == GovernmentType.REPUBLIC) {
                if (plugin.getCabinetManager().isDecisionActive(atLoc, id.nationcore.models.CabinetDecision.DecisionType.QUARANTINE_PROTOCOL)) {
                    event.setCancelled(true);
                    player.sendActionBar(Component.text(
                            "🚧 Quarantine Protocol — akses ke " + atLoc.getName() + " ditutup",
                            NamedTextColor.RED));
                    return;
                }
            }
        }

        if (atLoc != null) {
            String label = atLoc.isMember(player.getUniqueId())
                    ? "Selamat datang di " + atLoc.getName()
                    : "Memasuki teritori " + atLoc.getName() + " — anda non-anggota";
            NamedTextColor color = atLoc.isMember(player.getUniqueId())
                    ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
            player.sendActionBar(Component.text(label, color));

            applyPlagueTrigger(atLoc, player);
        } else if (previousId != null) {
            player.sendActionBar(Component.text("Meninggalkan teritori nation",
                    NamedTextColor.GRAY));
        }
    }

    /**
     * Plague (decision Menteri Health Komunis): musuh masuk teritori dapat
     * Weakness II + Hunger selama 30 detik. Hanya berlaku untuk non-anggota.
     * Throttled via {@link #lastPlagueApply} per pemain agar tidak spam apply.
     */
    private final Map<UUID, Long> lastPlagueApply = new HashMap<>();

    private void applyPlagueTrigger(Nation atLoc, Player player) {
        if (atLoc == null) return;
        if (player.hasPermission("nation.admin")) return;
        if (atLoc.isMember(player.getUniqueId())) return;

        boolean plagueActive = false;
        if (atLoc.getType() == GovernmentType.COMMUNIST) {
            CommunistGovernment cg = atLoc.getCommunistGovernment();
            if (cg != null && cg.isPlagueActive()) {
                plagueActive = true;
            }
        } else if (atLoc.getType() == GovernmentType.REPUBLIC) {
            if (plugin.getCabinetManager().isDecisionActive(atLoc, id.nationcore.models.CabinetDecision.DecisionType.PLAGUE)) {
                plagueActive = true;
            }
        }

        if (!plagueActive) return;

        long now = System.currentTimeMillis();
        Long last = lastPlagueApply.get(player.getUniqueId());
        if (last != null && now - last < 25_000) return; // re-apply tiap ~25 detik
        lastPlagueApply.put(player.getUniqueId(), now);

        int duration = 30 * 20; // 30 detik dalam ticks
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 1, false, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, duration, 0, false, true, true));
        MessageUtils.send(player, "<dark_red>☠ Plague! Anda terinfeksi di teritori " +
                atLoc.getName() + ".</dark_red>");
    }

    // ---------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------

    /** Kirim pesan denied dengan throttle ringan (sekali per detik per pemain). */
    private final Map<UUID, Long> lastDeniedMessage = new HashMap<>();

    private void sendDeniedMessage(Player player, Location loc) {
        if (player == null) return;
        long now = System.currentTimeMillis();
        Long last = lastDeniedMessage.get(player.getUniqueId());
        if (last != null && now - last < 1000) return;
        lastDeniedMessage.put(player.getUniqueId(), now);

        Nation atLoc = plugin.getTerritoryManager().getNationAt(loc);
        if (atLoc != null) {
            MessageUtils.send(player, "<red>Anda tidak bisa beraksi di teritori " +
                    atLoc.getName() + " (anggota saja).</red>");
        }
    }
}
