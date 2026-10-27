package dev.hopperfilter;

import dev.hopperfilter.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Two-row (18-slot) filter menu: slot 0 is a green emerald (allow) that
 * switches to a black blackstone (block) on a left-click; a right-click
 * switches the match mode (item type / exact). Slots 1-17 hold the items
 * of that mode (each slot holds a single item). Both modes stay on the hopper
 * at the same time.
 */
public final class FilterGui implements InventoryHolder {

    public static final int TOGGLE_SLOT = 0;
    public static final int FIRST_SLOT = 1;
    public static final int ITEM_SLOTS = 17;
    public static final int GUI_SIZE = 18;

    private final HopperFilterPlugin plugin;
    private final FilterHolder holder;
    private final Inventory inventory;
    private FilterMode mode;

    public FilterGui(HopperFilterPlugin plugin, FilterHolder holder) {
        this.plugin = plugin;
        this.holder = holder;
        this.mode = plugin.getFilterManager().getMode(holder);
        this.inventory = Bukkit.createInventory(this, GUI_SIZE,
                Text.colorize(plugin.getSettings().getGuiTitle()));
        refresh();
    }

    /** Fills slot 0 (mode indicator) and slots 1-17 (items of the mode). */
    public void refresh() {
        inventory.clear();
        inventory.setItem(TOGGLE_SLOT, createModeItem());
        List<ItemStack> items = plugin.getFilterManager().getItems(holder, mode);
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
        plugin.getFilterManager().setMode(holder, mode);
        refresh();
    }

    /** Right-click on the emerald/blackstone: item type <-> exact matching. */
    public void toggleMatch() {
        saveCurrentMode();
        FilterManager manager = plugin.getFilterManager();
        manager.setMatch(holder, manager.getMatch(holder) == MatchMode.TYPE ? MatchMode.EXACT : MatchMode.TYPE);
        refresh();
    }

    public MatchMode getMatch() {
        return plugin.getFilterManager().getMatch(holder);
    }

    /** Closing the GUI saves the displayed mode. */
    public void commit() {
        saveCurrentMode();
    }

    private void saveCurrentMode() {
        List<ItemStack> items = new ArrayList<>();
        for (int i = FIRST_SLOT; i < FIRST_SLOT + ITEM_SLOTS; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                ItemStack toStore = item.clone();
                toStore.setAmount(1);
                items.add(toStore);
            }
        }
        plugin.getFilterManager().saveFilter(holder, mode, items);
    }

    private ItemStack createModeItem() {
        boolean blockMode = mode == FilterMode.BLOCK;
        ItemStack item = new ItemStack(blockMode ? Material.BLACKSTONE : Material.EMERALD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        Settings settings = plugin.getSettings();
        String prefix = blockMode ? "block" : "allow";
        meta.displayName(Text.colorize(settings.message(prefix + "-name")));
        List<Component> lore = new ArrayList<>();
        for (String line : settings.messageLines(prefix + "-lore")) {
            lore.add(Text.colorize(line));
        }
        lore.add(Text.colorize(settings.message(getMatch() == MatchMode.TYPE ? "match-type" : "match-exact")));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public FilterHolder getHolder() {
        return holder;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}