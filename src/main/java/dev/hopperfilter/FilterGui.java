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
 * 9-slot filtermenu: slot 0 is een groene emerald (toestaan) die met een
 * klik wisselt naar zwarte blackstone (blokkeren). Slot 1 t/m 8 bevatten de
 * items van die modus. Beide modussen blijven tegelijk op de hopper staan.
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

    /** Vult slot 0 (modus-indicator) en slot 1 t/m 8 (items van de modus). */
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

    /** Klik op de emerald/blackstone: slaat de huidige modus op en wisselt. */
    public void toggleMode() {
        saveCurrentMode();
        mode = mode == FilterMode.ALLOW ? FilterMode.BLOCK : FilterMode.ALLOW;
        plugin.getFilterManager().setMode(block, mode);
        refresh();
    }

    /** Sluit van de GUI: slaat de getoonde modus op. */
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
            meta.displayName(Text.colorize("&4BLOK MODUS"));
            meta.lore(List.of(
                    Text.colorize("&7Items hieronder worden &cgeblokkeerd&7."),
                    Text.colorize("&8Klik om terug te schakelen naar groen (toestaan)")));
        } else {
            meta.displayName(Text.colorize("&2FILTER MODUS (toestaan)"));
            meta.lore(List.of(
                    Text.colorize("&7Items hieronder worden &atoegelaten&7."),
                    Text.colorize("&8Klik om te schakelen naar zwart (blokkeren)")));
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