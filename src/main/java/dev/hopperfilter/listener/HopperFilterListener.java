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
import org.bukkit.inventory.Inventory;
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

    /** Slot 0 toggles the mode; slots 1-17 hold the filter items. Additions
     *  that would create a duplicate item are rejected on the spot. */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof FilterGui gui)) {
            return;
        }
        ClickType click = event.getClick();
        int raw = event.getRawSlot();
        if (raw == FilterGui.TOGGLE_SLOT) {
            event.setCancelled(true);
            if (click == ClickType.LEFT || click == ClickType.RIGHT) {
                gui.toggleMode();
            }
            return;
        }
        if (raw >= gui.getInventory().getSize()) {
            // Player inventory: shift- or number-click moves an item into the filter.
            boolean movesIn = event.isShiftClick() || click == ClickType.NUMBER_KEY;
            if (movesIn && hasItem(event.getCurrentItem())
                    && isDuplicateInFilter(gui.getInventory(), event.getCurrentItem(), -1)) {
                event.setCancelled(true);
            }
            return;
        }
        if (raw >= FilterGui.FIRST_SLOT && raw < FilterGui.FIRST_SLOT + FilterGui.ITEM_SLOTS) {
            ItemStack incoming = click == ClickType.NUMBER_KEY
                    ? event.getView().getBottomInventory().getItem(event.getHotbarButton())
                    : event.getCursor();
            if (hasItem(incoming) && isDuplicateInFilter(gui.getInventory(), incoming, raw)) {
                event.setCancelled(true);
            }
        }
    }

    /** Prevent dragging items onto the mode indicator or into duplicates. */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof FilterGui)) {
            return;
        }
        if (event.getRawSlots().contains(FilterGui.TOGGLE_SLOT)) {
            event.setCancelled(true);
            return;
        }
        ItemStack dragged = event.getOldCursor();
        if (!hasItem(dragged)) {
            return;
        }
        Inventory inv = event.getInventory();
        int emptyTargets = 0;
        for (int slot : event.getRawSlots()) {
            if (slot >= FilterGui.FIRST_SLOT && slot < FilterGui.FIRST_SLOT + FilterGui.ITEM_SLOTS
                    && !hasItem(inv.getItem(slot))) {
                emptyTargets++;
            }
        }
        // Dropping one stack over multiple slots would duplicate the item.
        if (emptyTargets > 1) {
            event.setCancelled(true);
            return;
        }
        for (int i = FilterGui.FIRST_SLOT; i < FilterGui.FIRST_SLOT + FilterGui.ITEM_SLOTS; i++) {
            if (event.getRawSlots().contains(i)) {
                continue;
            }
            if (matches(inv.getItem(i), dragged)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private boolean hasItem(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    private boolean matches(ItemStack a, ItemStack b) {
        if (!hasItem(a) || !hasItem(b)) {
            return false;
        }
        return plugin.getSettings().isMatchByType() ? a.getType() == b.getType() : a.isSimilar(b);
    }

    private boolean isDuplicateInFilter(Inventory inv, ItemStack item, int exceptSlot) {
        for (int i = FilterGui.FIRST_SLOT; i < FilterGui.FIRST_SLOT + FilterGui.ITEM_SLOTS; i++) {
            if (i == exceptSlot) {
                continue;
            }
            if (matches(inv.getItem(i), item)) {
                return true;
            }
        }
        return false;
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