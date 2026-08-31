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
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tints filter hoppers redstone red: an active filter places a thin translucent
 * red glass cap (a block display) on the top of the hopper block, so the hopper
 * looks red from above while it keeps working normally.
 */
public final class FilterVisual {

    private static final float VIEW_RANGE = 1.0f;
    private static final long CLEANUP_PERIOD = 20L;
    // A thin red glass plate, exactly the size of the hopper block, resting on
    // the top edge: half of its thickness is pressed into the hopper, half
    // sticks out above it, so it never z-fights.
    private static final float PX = 1f / 16f;
    private static final Vector3f SCALE = new Vector3f(1f, PX, 1f);
    private static final Vector3f TRANSLATION = new Vector3f(0f, 1f - PX / 2f, 0f);

    private final HopperFilterPlugin plugin;
    private final Map<Location, BlockDisplay> overlays = new ConcurrentHashMap<>();
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
        for (BlockDisplay display : overlays.values()) {
            display.remove();
        }
        overlays.clear();
    }

    /** Places or removes the red tint based on whether the hopper is filtered. */
    public void update(Block block) {
        Location loc = block.getLocation();
        if (plugin.getSettings().isVisualEnabled() && plugin.getFilterManager().isFilterHopper(block)) {
            if (!overlays.containsKey(loc)) {
                spawnOverlay(block);
            }
        } else {
            removeOverlay(loc);
        }
    }

    /** Restores the tints of a newly loaded chunk; existing overlays live on. */
    public void rebuildChunk(Chunk chunk) {
        if (!plugin.getSettings().isVisualEnabled()) {
            return;
        }
        for (BlockState state : chunk.getTileEntities(false)) {
            if (state instanceof Hopper hopper) {
                Block block = hopper.getBlock();
                Location loc = block.getLocation();
                if (!overlays.containsKey(loc) && plugin.getFilterManager().isFilterHopper(block)) {
                    spawnOverlay(block);
                }
            }
        }
    }

    /** Broken or de-filtered hoppers lose their red tint. */
    private void cleanup() {
        if (overlays.isEmpty()) {
            return;
        }
        for (Iterator<Map.Entry<Location, BlockDisplay>> it = overlays.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Location, BlockDisplay> entry = it.next();
            Block block = entry.getKey().getBlock();
            if (block.getType() != Material.HOPPER || !plugin.getFilterManager().isFilterHopper(block)) {
                entry.getValue().remove();
                it.remove();
            }
        }
    }

    private void spawnOverlay(Block block) {
        BlockDisplay display = block.getWorld().spawn(block.getLocation(), BlockDisplay.class);
        display.setBlock(Material.RED_STAINED_GLASS.createBlockData());
        display.setViewRange(VIEW_RANGE);
        display.setTransformation(new Transformation(TRANSLATION, new Quaternionf(), SCALE, new Quaternionf()));
        overlays.put(block.getLocation().clone(), display);
    }

    private void removeOverlay(Location loc) {
        BlockDisplay display = overlays.remove(loc);
        if (display != null) {
            display.remove();
        }
    }
}