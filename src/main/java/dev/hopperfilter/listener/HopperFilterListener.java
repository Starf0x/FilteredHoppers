package dev.hopperfilter.listener;

import dev.hopperfilter.FilterGui;
import dev.hopperfilter.FilterManager;
import dev.hopperfilter.HopperFilterPlugin;
import dev.hopperfilter.util.Text;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;

public final class HopperFilterListener implements Listener {

    private final HopperFilterPlugin plugin;
    private final FilterManager manager;

    public HopperFilterListener(HopperFilterPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getFilterManager();
    }

    /** Every hopper is a filter hopper: shift-right-click opens the filter menu. */
    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.HOPPER) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }

        // The filter menu only opens with an empty hand. With any item in
        // hand, vanilla behaviour stays intact (e.g. placing a hopper).
        ItemStack held = event.getItem();
        if (held != null && held.getType() != Material.AIR) {
            return;
        }

        event.setCancelled(true);
        player.openInventory(new FilterGui(plugin, block).getInventory());
        player.sendMessage(Text.colorize("&7Green = allow, black = block. Click the item to filter into a slot and close to save."));
    }

    /** Only the emerald/blackstone on slot 0 may switch; the rest is the menu. */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof FilterGui gui)) {
            return;
        }
        if (event.getRawSlot() != FilterGui.TOGGLE_SLOT) {
            return;
        }
        event.setCancelled(true);
        ClickType click = event.getClick();
        if (click == ClickType.LEFT || click == ClickType.RIGHT) {
            gui.toggleMode();
        }
    }

    /** Prevent dragging items onto the emerald/blackstone. */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof FilterGui)) {
            return;
        }
        if (event.getRawSlots().contains(FilterGui.TOGGLE_SLOT)) {
            event.setCancelled(true);
        }
    }

    /** Only allow ground pickups that pass the filter. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(InventoryPickupItemEvent event) {
        Block block = manager.getHopperBlock(event.getInventory());
        if (block != null && !manager.isAllowed(block, event.getItem().getItemStack())) {
            event.setCancelled(true);
        }
    }

    /** Filter container-to-hopper transfers. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        Block block = manager.getHopperBlock(event.getDestination());
        if (block != null && !manager.isAllowed(block, event.getItem())) {
            event.setCancelled(true);
        }
    }

    /** Closing the GUI saves the displayed mode. */
    @EventHandler
    public void onGUIClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof FilterGui gui) {
            gui.commit();
        }
    }

    /** Re-register filter hoppers of a newly loaded chunk for the red rim. */
    @EventHandler(ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        plugin.getFilterVisual().rebuildChunk(event.getChunk());
    }
}