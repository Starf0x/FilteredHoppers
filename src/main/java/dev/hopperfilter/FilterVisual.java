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
 * Toont een rode rand rond hoppers met een actief filter.
 *
 * Performance: de set wordt alleen bijgewerkt bij gebeurtenissen (opslaan,
 * wissen, chunk laden). De periodieke taak doet GEEN NBT-uitlezingen: hij
 * controleert eerst of de chunk geladen is (goedkoop) en of het blok nog een
 * hopper-blokdata is (goedkoop, zonder tile-entity), en spawnt alleen
 * deeltjes als er een speler binnen 32 blokken staat.
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

    /** Voegt toe of verwijdert het blok op basis van of er een actief filter is. */
    public void update(Block block) {
        Location loc = block.getLocation();
        if (plugin.getSettings().isVisualEnabled() && plugin.getFilterManager().isFilterHopper(block)) {
            locations.add(loc);
        } else {
            locations.remove(loc);
        }
    }

    /** Bij chunk laden de bestaande filterhoppers uit dit chunk opnieuw registeren. */
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
        double[] c = {0.0, 1.0};
        for (double dx : c) {
            for (double dy : c) {
                for (double dz : c) {
                    spawn(world, x + dx, y + dy, z + dz);
                }
            }
        }
        for (double a : c) {
            for (double b : c) {
                spawn(world, x + 0.5, y + a, z + b);
                spawn(world, x + a, y + 0.5, z + b);
                spawn(world, x + a, y + b, z + 0.5);
            }
        }
    }

    private void spawn(World world, double x, double y, double z) {
        world.spawnParticle(Particle.DUST, x, y, z, 1, RED);
    }
}