package dev.hopperfilter;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws a red square rim around the TOP edges of hoppers with an active
 * filter. Uses block display entities, so there are no particles: only the
 * top rim turns red.
 */
public final class FilterVisual {

    private static final float THICKNESS = 0.05f;
    private static final float VIEW_RANGE = 1.0f;
    private static final long CLEANUP_PERIOD = 20L;

    private final HopperFilterPlugin plugin;
    private final Map<Location, List<Entity>> rims = new ConcurrentHashMap<>();
    private BukkitTask task;

    public FilterVisual(HopperFilterPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (task == null) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanup, CLEANUP_PERIOD, CLEANUP_PERIOD);
        }
        if (!plugin.getSettings().isVisualEnabled()) {
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                rebuildChunk(chunk);
            }
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (List<Entity> list : rims.values()) {
            for (Entity entity : list) {
                entity.remove();
            }
        }
        rims.clear();
    }

    /** Adds or removes the wireframe based on whether the hopper has a filter. */
    public void update(Block block) {
        Location loc = block.getLocation();
        if (plugin.getSettings().isVisualEnabled() && plugin.getFilterManager().isFilterHopper(block)) {
            if (!rims.containsKey(loc)) {
                spawnRim(block);
            }
        } else {
            removeRim(loc);
        }
    }

    /** Re-register wireframes of a newly loaded chunk; existing entities live on. */
    public void rebuildChunk(Chunk chunk) {
        if (!plugin.getSettings().isVisualEnabled()) {
            return;
        }
        for (BlockState state : chunk.getTileEntities(false)) {
            if (state instanceof Hopper hopper) {
                Block block = hopper.getBlock();
                Location loc = block.getLocation();
                if (!rims.containsKey(loc) && plugin.getFilterManager().isFilterHopper(block)) {
                    spawnRim(block);
                }
            }
        }
    }

    /** Broken or de-filtered hoppers lose their wireframe again. */
    private void cleanup() {
        if (rims.isEmpty()) {
            return;
        }
        for (Iterator<Map.Entry<Location, List<Entity>>> it = rims.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Location, List<Entity>> entry = it.next();
            Block block = entry.getKey().getBlock();
            if (block.getType() != Material.HOPPER || !plugin.getFilterManager().isFilterHopper(block)) {
                for (Entity entity : entry.getValue()) {
                    entity.remove();
                }
                it.remove();
            }
        }
    }

    private void spawnRim(Block block) {
        Location base = block.getLocation();
        World world = block.getWorld();
        double bx = base.getX();
        double by = base.getY();
        double bz = base.getZ();
        float t = THICKNESS;
        Vector3f down = new Vector3f(0f, -t / 2f, 0f);

        List<Entity> entities = new ArrayList<>();
        for (int c = 0; c <= 1; c++) {
            double a = c;
            // two long edges along X on front and back of the top rim
            entities.add(edge(world, bx + 0.5, by + 1, bz + a, new Vector3f(1, t, t), down));
            // two long edges along Z on left and right of the top rim
            entities.add(edge(world, bx + a, by + 1, bz + 0.5, new Vector3f(t, t, 1), down));
        }
        rims.put(base.clone(), entities);
    }

    private BlockDisplay edge(World world, double x, double y, double z, Vector3f scale, Vector3f translation) {
        BlockDisplay display = world.spawn(new Location(world, x, y, z), BlockDisplay.class);
        display.setBlock(Material.RED_CONCRETE.createBlockData());
        display.setViewRange(VIEW_RANGE);
        display.setTransformation(new Transformation(translation, new Quaternionf(), scale, new Quaternionf()));
        return display;
    }

    private void removeRim(Location loc) {
        List<Entity> list = rims.remove(loc);
        if (list != null) {
            for (Entity entity : list) {
                entity.remove();
            }
        }
    }
}