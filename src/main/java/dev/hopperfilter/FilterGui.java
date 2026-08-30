package dev.hopperfilter;

import dev.hopperfilter.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 9-slot filter menu: slot 0 is a green emerald (allow) that switches to a
 * black blackstone (block) on click. Slots 1-8 hold the items of that mode.
 * Both modes stay on the hopper at the same time.
 */
public final class FilterGui implements InventoryHolder {

    public static final int TOGGLE_SLOT = 0;
    public static final int FIRST_SLOT = 1;
    public static final int ITEM_SLOTS = 8;
    public static final int GUI_SIZE = 9;

    private final HopperFilterPlugin plugin;
    private final Block block;
    private final Inventory inventory;
    private FilterMode mode;

    public FilterGui(HopperFilterPlugin plugin, Block block) {
        this.plugin = plugin;
        this.block = block;
        this.mode = plugin.getFilterManager().getMode(block);
        this.inventory = Bukkit.createInventory(this, GUI_SIZE,
                Text.colorize(plugin.getSettings().getGuiTitle()));
        refresh();
    }

    /** Fills slot 0 (mode indicator) and slots 1-8 (items of the mode). */
    public void refresh() {
        inventory.clear();
        inventory.setItem(TOGGLE_SLOT, createModeItem());
        List<ItemStack> items = plugin.getFilterManager().getItems(block, mode);
        int added = 0;
        for (ItemStack item : items) {
            if (added >= ITEM_SLOTS) {
                break;
            }
            inventory.setItem(FIRST_SLOT + added, item.clone());
            added++;
        }
    }

    public FilterMode getMode() {
        return mode;
    }

    /** Click on the emerald/blackstone: saves the current mode and switches. */
    public void toggleMode() {
        saveCurrentMode();
        mode = mode == FilterMode.ALLOW ? FilterMode.BLOCK : FilterMode.ALLOW;
        plugin.getFilterManager().setMode(block, mode);
        refresh();
    }

    /** Closing the GUI saves the displayed mode. */
    public void commit() {
        saveCurrentMode();
    }

    private void saveCurrentMode() {
        List<ItemStack> items = new ArrayList<>();
        for (int i = FIRST_SLOT; i < GUI_SIZE; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                ItemStack toStore = item.clone();
                toStore.setAmount(1);
                items.add(toStore);
            }
        }
        plugin.getFilterManager().saveFilter(block, mode, items);
    }

    private ItemStack createModeItem() {
        boolean blockMode = mode == FilterMode.BLOCK;
        ItemStack item = new ItemStack(blockMode ? Material.BLACKSTONE : Material.EMERALD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        if (blockMode) {
            meta.displayName(Text.colorize("&4BLOCK MODE"));
            meta.lore(List.of(
                    Text.colorize("&7The items below are &cblocked&7."),
                    Text.colorize("&8Click to switch back to green (allow)")));
        } else {
            meta.displayName(Text.colorize("&2FILTER MODE (allow)"));
            meta.lore(List.of(
                    Text.colorize("&7The items below are &aallowed&7."),
                    Text.colorize("&8Click to switch to black (block)")));
        }
        item.setItemMeta(meta);
        return item;
    }

    public Block getBlock() {
        return block;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}