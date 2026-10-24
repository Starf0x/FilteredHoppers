package dev.hopperfilter.listener;

import dev.hopperfilter.FilterGui;
import dev.hopperfilter.FilterHopperGui;
import dev.hopperfilter.FilterManager;
import dev.hopperfilter.FilterMode;
import dev.hopperfilter.HopperFilterPlugin;
import dev.hopperfilter.util.Text;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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

    /** Slot 0 toggles the mode; slots 1-17 hold the filter items. A filter
     *  slot never holds more than one item and never a duplicate: shift-clicks,
     *  number-key moves and drags over a stack are turned into a single-item
     *  placement, and anything that would stack or duplicate is rejected. */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof FilterGui gui)) {
            return;
        }
        Inventory inv = gui.getInventory();
        ClickType click = event.getClick();
        int raw = event.getRawSlot();

        if (raw == FilterGui.TOGGLE_SLOT) {
            event.setCancelled(true);
            if (click == ClickType.LEFT || click == ClickType.RIGHT) {
                gui.toggleMode();
            }
            return;
        }

        if (raw >= inv.getSize()) {
            // Player inventory area: shift-clicking moves exactly one item in.
            if (!event.isShiftClick() || !hasItem(event.getCurrentItem())) {
                return;
            }
            ItemStack source = event.getCurrentItem();
            int slot = firstEmptyFilterSlot(inv);
            if (slot == -1 || isDuplicateInFilter(inv, source, -1)) {
                event.setCancelled(true);
                return;
            }
            event.setCancelled(true);
            inv.setItem(slot, single(source));
            event.setCurrentItem(shrink(source, 1));
            return;
        }

        if (raw < FilterGui.FIRST_SLOT || raw >= FilterGui.FIRST_SLOT + FilterGui.ITEM_SLOTS) {
            return;
        }

        // A filter slot was clicked.
        ItemStack current = event.getCurrentItem();
        boolean occupied = hasItem(current);
        if (event.isShiftClick()) {
            return; // shift-clicking the single filter item takes it out.
        }

        if (click == ClickType.NUMBER_KEY) {
            ItemStack hotbar = event.getView().getBottomInventory().getItem(event.getHotbarButton());
            if (hasItem(hotbar)) {
                event.setCancelled(true);
                if (occupied || isDuplicateInFilter(inv, hotbar, raw)) {
                    return;
                }
                inv.setItem(raw, single(hotbar));
                event.getView().getBottomInventory().setItem(event.getHotbarButton(), shrink(hotbar, 1));
            }
            return;
        }

        ItemStack cursor = event.getCursor();
        if (!hasItem(cursor)) {
            return; // picking the filter item up with an empty cursor.
        }
        if (isDuplicateInFilter(inv, cursor, raw)) {
            event.setCancelled(true);
            return;
        }
        if (occupied) {
            // No stacking on top of the single filter item; only a clean
            // one-for-one swap with a single item passes.
            if (matches(current, cursor) || cursor.getAmount() > 1) {
                event.setCancelled(true);
            }
            return;
        }
        if (click == ClickType.LEFT && cursor.getAmount() > 1) {
            // Left-clicking a whole stack into an empty slot: keep one.
            event.setCancelled(true);
            inv.setItem(raw, single(cursor));
            event.getView().setCursor(shrink(cursor, 1));
        }
    }

    /** Prevent dragging onto the mode indicator, into occupied filter slots or
     *  into more than one empty filter slot; a drag onto a single empty slot
     *  is turned into a single-item placement. */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof FilterGui gui)) {
            return;
        }
        Inventory inv = gui.getInventory();
        if (event.getRawSlots().contains(FilterGui.TOGGLE_SLOT)) {
            event.setCancelled(true);
            return;
        }
        ItemStack dragged = event.getOldCursor();
        if (!hasItem(dragged)) {
            return;
        }
        int target = -1;
        for (int slot : event.getRawSlots()) {
            if (slot >= FilterGui.FIRST_SLOT && slot < FilterGui.FIRST_SLOT + FilterGui.ITEM_SLOTS) {
                if (hasItem(inv.getItem(slot))) {
                    event.setCancelled(true);
                    return;
                }
                if (target != -1) {
                    event.setCancelled(true);
                    return;
                }
                target = slot;
            }
        }
        for (int i = FilterGui.FIRST_SLOT; i < FilterGui.FIRST_SLOT + FilterGui.ITEM_SLOTS; i++) {
            if (i == target || event.getRawSlots().contains(i)) {
                continue;
            }
            if (matches(inv.getItem(i), dragged)) {
                event.setCancelled(true);
                return;
            }
        }
        if (target != -1) {
            event.setCancelled(true);
            inv.setItem(target, single(dragged));
            event.getView().setCursor(shrink(dragged, 1));
        }
    }

    private boolean hasItem(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    private ItemStack single(ItemStack item) {
        ItemStack one = item.clone();
        one.setAmount(1);
        return one;
    }

    private ItemStack shrink(ItemStack item, int by) {
        int remaining = item.getAmount() - by;
        if (remaining <= 0) {
            return null;
        }
        ItemStack rest = item.clone();
        rest.setAmount(remaining);
        return rest;
    }

    private int firstEmptyFilterSlot(Inventory inv) {
        for (int i = FilterGui.FIRST_SLOT; i < FilterGui.FIRST_SLOT + FilterGui.ITEM_SLOTS; i++) {
            if (!hasItem(inv.getItem(i))) {
                return i;
            }
        }
        return -1;
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

    /** Only allow ground pickups that pass the filter. Normal hoppers are
     *  left completely untouched for speed. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(InventoryPickupItemEvent event) {
        Block block = manager.getHopperBlock(event.getInventory());
        if (block == null || !manager.isFilterHopper(block)) {
            return;
        }
        if (!manager.isAllowed(block, event.getItem().getItemStack())) {
            event.setCancelled(true);
        }
    }

    /** Filter container-to-hopper transfers; normal hoppers pass untouched. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        Block block = manager.getHopperBlock(event.getDestination());
        if (block == null || !manager.isFilterHopper(block)) {
            return;
        }
        if (!manager.isAllowed(block, event.getItem())) {
            event.setCancelled(true);
            if (event.getInitiator().equals(event.getDestination())) {
                pullNextAllowed(block, event.getSource(), event.getDestination());
            }
        }
    }

    /** Containers whose every slot a hopper below may pull from. */
    private static final Set<InventoryType> PULLABLE = EnumSet.of(InventoryType.CHEST,
            InventoryType.BARREL, InventoryType.HOPPER, InventoryType.DROPPER,
            InventoryType.DISPENSER, InventoryType.SHULKER_BOX);

    /** Paper ends a hopper's pull at the first rejected stack, so a filter
     *  hopper would wait behind every unwanted item and miss the wanted ones
     *  passing by. Pull one item of the first allowed stack instead. */
    private void pullNextAllowed(Block block, Inventory source, Inventory hopper) {
        if (!PULLABLE.contains(source.getType())) {
            return;
        }
        ItemStack[] contents = source.getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (!hasItem(stack) || !manager.isAllowed(block, stack)) {
                continue;
            }
            if (hopper.addItem(single(stack)).isEmpty()) {
                source.setItem(i, shrink(stack, 1));
                return;
            }
        }
    }

    /** Closing a GUI saves the displayed mode / merges items back. */
    @EventHandler
    public void onGUIClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof FilterGui gui) {
            gui.commit();
        } else if (event.getInventory().getHolder() instanceof FilterHopperGui hopperGui) {
            hopperGui.commit((Player) event.getPlayer());
        }
    }

    /** Re-register the red tint of a newly loaded chunk. */
    @EventHandler(ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        plugin.getFilterVisual().rebuildChunk(event.getChunk());
    }

    /** Entities load after the chunk itself: clear leftover red caps then. */
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        plugin.getFilterVisual().removeStale(event.getEntities());
    }

    /** A broken hopper drops every filter item it held. */
    @EventHandler(ignoreCancelled = true)
    public void onHopperBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.HOPPER) {
            return;
        }
        List<ItemStack> items = new ArrayList<>();
        items.addAll(manager.getItems(block, FilterMode.ALLOW));
        items.addAll(manager.getItems(block, FilterMode.BLOCK));
        manager.clearFilter(block);
        if (items.isEmpty()) {
            return;
        }
        for (ItemStack item : items) {
            block.getWorld().dropItemNaturally(block.getLocation(), item);
        }
    }
}