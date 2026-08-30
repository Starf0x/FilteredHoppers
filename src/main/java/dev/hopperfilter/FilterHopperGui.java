package dev.hopperfilter;

import dev.hopperfilter.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * The adapted hopper screen shown on a normal right-click when the hopper has
 * an active filter: the 5 real hopper slots sit inside a red frame. Items are
 * moved into the GUI while open and merged back into the tile on close.
 */
public final class FilterHopperGui implements InventoryHolder {

    public static final int GUI_SIZE = 27;
    public static final int FIRST_HOPPER_SLOT = 11;

    private final HopperFilterPlugin plugin;
    private final Block block;
    private final Inventory inventory;

    public FilterHopperGui(HopperFilterPlugin plugin, Block block) {
        this.plugin = plugin;
        this.block = block;
        this.inventory = Bukkit.createInventory(this, GUI_SIZE, Text.colorize("&0Filter Hopper"));
        load();
    }

    /** Places the tile items into the GUI and decorates the red frame. */
    private void load() {
        inventory.clear();
        for (int i = 0; i < GUI_SIZE; i++) {
            if (isDecoration(i)) {
                inventory.setItem(i, createFrame());
            }
        }
        Hopper hopper = getHopper();
        if (hopper == null) {
            return;
        }
        Inventory tile = hopper.getInventory();
        for (int i = 0; i < 5; i++) {
            ItemStack item = tile.getItem(i);
            if (item != null) {
                inventory.setItem(FIRST_HOPPER_SLOT + i, item);
                tile.setItem(i, null);
            }
        }
    }

    /** Writes the GUI items back into the real hopper inventory. */
    public void commit() {
        Hopper hopper = getHopper();
        if (hopper == null) {
            return;
        }
        Inventory tile = hopper.getInventory();
        for (int i = 0; i < 5; i++) {
            ItemStack item = inventory.getItem(FIRST_HOPPER_SLOT + i);
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            if (tile.getItem(i) != null) {
                int put = tile.firstEmpty();
                if (put == -1) {
                    continue;
                }
                tile.setItem(put, item);
            } else {
                tile.setItem(i, item);
            }
        }
    }

    private Hopper getHopper() {
        return block.getState() instanceof Hopper hopper ? hopper : null;
    }

    public boolean isDecoration(int slot) {
        return slot < GUI_SIZE && (slot <= 10 || slot >= 16);
    }

    private ItemStack createFrame() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.colorize("&cFilter active"));
        meta.lore(List.of(Text.colorize("&7This hopper only lets filtered items pass.")));
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