package id.nationcore.managers;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import id.nationcore.NationCore;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.Nation;

/**
 * Renders the particle outline of every nation whose borders are toggled on.
 *
 * Design goals (per the Border Management spec):
 *   • Connected — only the OUTER perimeter of a nation's chunk cluster is drawn;
 *     shared internal edges are skipped, so multi-chunk territory reads as a
 *     single continuous border.
 *   • Distinct capital — the capital chunk always draws its full square outline
 *     with a brighter, larger particle plus rising sparkles, so it stands out
 *     even when fully surrounded by owned land.
 *   • Visible to everyone — particles are spawned into the world with the
 *     {@code force} flag, so all nearby players see them regardless of their
 *     client particle settings.
 *   • Lag-aware — work is throttled, culled to chunks near online players, and
 *     never touches unloaded chunks (height sampling is clamped in-chunk).
 */
public class BorderVisualizationManager {

    /** How often the outline is re-drawn. 10 ticks ≈ twice per second. */
    private static final long PERIOD_TICKS = 10L;
    /** Particle spacing along an edge, in blocks. Smaller = denser line. */
    private static final double STEP = 0.5;
    /** Only render chunks within this Chebyshev chunk-distance of a player. */
    private static final int RENDER_CHUNK_RADIUS = 4;

    private static final Color CAPITAL_COLOR = Color.fromRGB(255, 215, 0); // gold

    private final NationCore plugin;
    private BukkitTask task;

    public BorderVisualizationManager(NationCore plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, PERIOD_TICKS, PERIOD_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            if (nation == null || !nation.isBorderVisible()) continue;
            renderNation(nation);
        }
    }

    private void renderNation(Nation nation) {
        TerritoryManager territory = plugin.getTerritoryManager();
        // Snapshot to avoid concurrent-modification if a claim happens mid-tick.
        for (String key : new ArrayList<>(nation.getTerritory())) {
            String[] parts = key.split(";");
            if (parts.length != 3) continue;

            World world = Bukkit.getWorld(parts[0]);
            if (world == null) continue;

            int cx, cz;
            try {
                cx = Integer.parseInt(parts[1]);
                cz = Integer.parseInt(parts[2]);
            } catch (NumberFormatException ex) {
                continue;
            }

            if (!world.isChunkLoaded(cx, cz)) continue;
            if (!hasNearbyPlayer(world, cx, cz)) continue;

            String worldName = world.getName();
            boolean capital = territory.isCapitalChunk(nation, worldName, cx, cz);

            if (capital) {
                // Full square outline so the capital is always marked.
                renderEdge(world, cx, cz, Edge.NORTH, nation, true);
                renderEdge(world, cx, cz, Edge.SOUTH, nation, true);
                renderEdge(world, cx, cz, Edge.WEST, nation, true);
                renderEdge(world, cx, cz, Edge.EAST, nation, true);
            } else {
                // Only edges facing unowned chunks form the connected perimeter.
                if (!territory.ownsChunkAt(nation, worldName, cx, cz - 1)) renderEdge(world, cx, cz, Edge.NORTH, nation, false);
                if (!territory.ownsChunkAt(nation, worldName, cx, cz + 1)) renderEdge(world, cx, cz, Edge.SOUTH, nation, false);
                if (!territory.ownsChunkAt(nation, worldName, cx - 1, cz)) renderEdge(world, cx, cz, Edge.WEST, nation, false);
                if (!territory.ownsChunkAt(nation, worldName, cx + 1, cz)) renderEdge(world, cx, cz, Edge.EAST, nation, false);
            }
        }
    }

    private enum Edge { NORTH, SOUTH, WEST, EAST }

    private void renderEdge(World world, int cx, int cz, Edge edge, Nation nation, boolean capital) {
        final int minX = cx << 4;
        final int minZ = cz << 4;
        final int maxX = minX + 16;
        final int maxZ = minZ + 16;

        boolean varyX = (edge == Edge.NORTH || edge == Edge.SOUTH);
        double fixedX = (edge == Edge.WEST) ? minX : maxX;
        double fixedZ = (edge == Edge.NORTH) ? minZ : maxZ;

        Particle.DustOptions dust = capital
                ? new Particle.DustOptions(CAPITAL_COLOR, 1.7f)
                : new Particle.DustOptions(colorFor(nation.getType()), 1.0f);

        int steps = (int) Math.round(16 / STEP);
        for (int i = 0; i <= steps; i++) {
            double t = i * STEP;
            double px = varyX ? (minX + t) : fixedX;
            double pz = varyX ? fixedZ : (minZ + t);

            // Sample terrain height from within THIS chunk only, so boundary
            // points never force-load a neighbouring chunk.
            int sx = clamp((int) Math.floor(px), minX, maxX - 1);
            int sz = clamp((int) Math.floor(pz), minZ, maxZ - 1);
            double baseY = world.getHighestBlockYAt(sx, sz) + 1.0;

            world.spawnParticle(Particle.DUST, px, baseY, pz, 1, 0, 0, 0, 0, dust, true);
            world.spawnParticle(Particle.DUST, px, baseY + 1.0, pz, 1, 0, 0, 0, 0, dust, true);

            if (capital && i % 8 == 0) {
                world.spawnParticle(Particle.END_ROD, px, baseY + 1.4, pz, 1, 0, 0, 0, 0.0, null, true);
            }
        }
    }

    private boolean hasNearbyPlayer(World world, int cx, int cz) {
        for (Player p : world.getPlayers()) {
            int pcx = p.getLocation().getBlockX() >> 4;
            int pcz = p.getLocation().getBlockZ() >> 4;
            if (Math.abs(pcx - cx) <= RENDER_CHUNK_RADIUS && Math.abs(pcz - cz) <= RENDER_CHUNK_RADIUS) {
                return true;
            }
        }
        return false;
    }

    private Color colorFor(GovernmentType type) {
        if (type == null) return Color.WHITE;
        return switch (type) {
            case REPUBLIC -> Color.fromRGB(85, 170, 255);  // light blue
            case COMMUNIST -> Color.fromRGB(255, 60, 60);   // red
            case MONARCHY -> Color.fromRGB(255, 170, 0);    // amber
            case CALIPHATE -> Color.fromRGB(90, 220, 90);   // green
        };
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
