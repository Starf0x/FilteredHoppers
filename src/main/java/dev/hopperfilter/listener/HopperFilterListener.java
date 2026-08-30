package dev.hopperfilter.listener;

import dev.hopperfilter.FilterGui;
import dev.hopperfilter.FilterHopperGui;
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
import org.bukkit.inventory.ItemStack;

public final class HopperFilterListener implements Listener {

    private final HopperFilterPlugin plugin;
    private final FilterManager manager;

    public HopperFilterListener(HopperFilterPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getFilterManager();
    }

    /** Shift-right-click opens the filter menu; a normal right-click on an
     *  active filter hopper opens the hopper screen with the red frame. */
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
            // Normal open: active filter hoppers get the red-framed screen.
            if (manager.isFilterHopper(block)) {
                ItemStack held = event.getItem();
                if (held == null || held.getType() == Material.AIR || !held.getType().isBlock()) {
                    event.setCancelled(true);
                    player.openInventory(new FilterHopperGui(plugin, block).getInventory());
                }
            }
            return;
        }

        // Shift-right-click opens the filter menu, but only with an empty
        // hand. With any item in hand vanilla behaviour stays intact.
        ItemStack held = event.getItem();
        if (held != null && held.getType() != Material.AIR) {
            return;
        }

        event.setCancelled(true);
        player.openInventory(new FilterGui(plugin, block).getInventory());
        player.sendMessage(Text.colorize("&7Green = allow, black = block. Click the items to filter into the slots and close to save."));
    }

    /** Slot 0 toggles the mode; slots 1-17 freely hold the filter items. */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof FilterGui gui)) {
            return;
        }
        int raw = event.getRawSlot();
        if (raw >= gui.getInventory().getSize()) {
            return;
        }
        if (raw == FilterGui.TOGGLE_SLOT) {
            event.setCancelled(true);
            ClickType click = event.getClick();
            if (click == ClickType.LEFT || click == ClickType.RIGHT) {
                gui.toggleMode();
            }
        }
    }

    /** Prevent dragging items onto the mode indicator. */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof FilterGui)) {
            return;
        }
        if (event.getRawSlots().contains(FilterGui.TOGGLE_SLOT)) {
            event.setCancelled(true);
        }
    }

    /** The red frame of the hopper screen cannot be touched. */
    @EventHandler(ignoreCancelled = true)
    public void onFrameClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof FilterHopperGui gui)) {
            return;
        }
        int raw = event.getRawSlot();
        if (raw < gui.getInventory().getSize() && gui.isDecoration(raw)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFrameDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof FilterHopperGui gui)) {
            return;
        }
        for (int slot : event.getRawSlots()) {
            if (gui.isDecoration(slot)) {
                event.setCancelled(true);
                return;
            }
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

    /** Closing a GUI saves the displayed mode / merges items back. */
    @EventHandler
    public void onGUIClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof FilterGui gui) {
            gui.commit();
        } else if (event.getInventory().getHolder() instanceof FilterHopperGui hopperGui) {
            hopperGui.commit();
        }
    }
}