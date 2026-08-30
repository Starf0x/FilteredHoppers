package dev.hopperfilter;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Shows a red rim around hoppers with an active filter.
 *
 * Performance: the set is only updated on events (saving, clearing, chunk
 * loading). The periodic task does NO NBT reads: it first checks whether the
 * chunk is loaded (cheap) and whether the block is still hopper block-data
 * (cheap, no tile-entity), and only spawns particles when a player is within
 * 32 blocks.
 */
public final class FilterVisual {

    private static final double PLAYER_RADIUS = 32.0;
    private static final double PLAYER_RADIUS_SQ = PLAYER_RADIUS * PLAYER_RADIUS;
    private static final long PERIOD_TICKS = 10L;
    private static final Particle.DustOptions RED = new Particle.DustOptions(Color.RED, 1f);

    private final HopperFilterPlugin plugin;
    private final Set<Location> locations = new HashSet<>();
    private BukkitTask task;

    public FilterVisual(HopperFilterPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (task != null) {
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, PERIOD_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        locations.clear();
    }

    /** Adds or removes the block based on whether it has an active filter. */
    public void update(Block block) {
        Location loc = block.getLocation();
        if (plugin.getSettings().isVisualEnabled() && plugin.getFilterManager().isFilterHopper(block)) {
            locations.add(loc);
        } else {
            locations.remove(loc);
        }
    }

    /** On chunk load, re-register the existing filter hoppers of this chunk. */
    public void rebuildChunk(Chunk chunk) {
        if (!plugin.getSettings().isVisualEnabled()) {
            return;
        }
        for (BlockState state : chunk.getTileEntities(false)) {
            if (state instanceof Hopper hopper && plugin.getFilterManager().isFilterHopper(hopper.getBlock())) {
                locations.add(hopper.getBlock().getLocation());
            }
        }
    }

    private void tick() {
        if (!plugin.isEnabled() || !plugin.getSettings().isVisualEnabled() || locations.isEmpty()) {
            return;
        }
        for (Iterator<Location> it = locations.iterator(); it.hasNext(); ) {
            Location loc = it.next();
            World world = loc.getWorld();
            int cx = loc.getBlockX() >> 4;
            int cz = loc.getBlockZ() >> 4;
            if (!world.isChunkLoaded(cx, cz)) {
                it.remove();
                continue;
            }
            Block block = loc.getBlock();
            if (!(block.getBlockData() instanceof org.bukkit.block.data.type.Hopper)) {
                it.remove();
                continue;
            }
            List<Player> players = world.getPlayers();
            if (players.isEmpty() || !hasNearbyPlayer(players, loc)) {
                continue;
            }
            spawnRim(block);
        }
    }

    private boolean hasNearbyPlayer(List<Player> players, Location loc) {
        for (Player player : players) {
            if (player.getLocation().distanceSquared(loc) <= PLAYER_RADIUS_SQ) {
                return true;
            }
        }
        return false;
    }

    private void spawnRim(Block block) {
        World world = block.getWorld();
        double x = block.getX();
        double y = block.getY();
        double z = block.getZ();
        double[] s = {0.0, 1.0};
        // 4 edges parallel to X
        for (double c1 : s) {
            for (double c2 : s) {
                line(world, x, y + c1, z + c2, 1.0, 0.0, 0.0);
            }
        }
        // 4 edges parallel to Y
        for (double c1 : s) {
            for (double c2 : s) {
                line(world, x + c1, y, z + c2, 0.0, 1.0, 0.0);
            }
        }
        // 4 edges parallel to Z
        for (double c1 : s) {
            for (double c2 : s) {
                line(world, x + c1, y + c2, z, 0.0, 0.0, 1.0);
            }
        }
    }

    /** Draws a continuous, solid red line of dust particles along an edge. */
    private void line(World world, double x, double y, double z, double dx, double dy, double dz) {
        for (double t = 0.0; t <= 1.0; t += 0.125) {
            double a = Math.min(t, 1.0);
            world.spawnParticle(Particle.DUST, x + dx * a, y + dy * a, z + dz * a, 1, RED);
        }
    }
}