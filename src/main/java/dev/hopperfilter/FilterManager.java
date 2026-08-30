package dev.hopperfilter;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.block.TileState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Stores the "allow" list and the "block" list per hopper in the NBT of the
 * block, so both modes are known at the same time and survive a server
 * restart.
 */
public final class FilterManager {

    private final HopperFilterPlugin plugin;
    private final NamespacedKey keyAllow;
    private final NamespacedKey keyBlock;
    private final NamespacedKey keyMode;

    public FilterManager(HopperFilterPlugin plugin) {
        this.plugin = plugin;
        this.keyAllow = new NamespacedKey(plugin, "hopper-filter-allow");
        this.keyBlock = new NamespacedKey(plugin, "hopper-filter-block");
        this.keyMode = new NamespacedKey(plugin, "hopper-filter-mode");
    }

    /** Items of a mode (empty if there are none). */
    public List<ItemStack> getItems(Block block, FilterMode mode) {
        if (!(block.getState() instanceof TileState tile)) {
            return List.of();
        }
        PersistentDataContainer pdc = tile.getPersistentDataContainer();
        NamespacedKey key = mode == FilterMode.ALLOW ? keyAllow : keyBlock;
        List<String> encoded = pdc.get(key, PersistentDataType.LIST.strings());
        if (encoded == null) {
            return List.of();
        }
        List<ItemStack> items = new ArrayList<>();
        for (String s : encoded) {
            try {
                items.add(ItemStack.deserializeBytes(Base64.getDecoder().decode(s)));
            } catch (Exception ignored) {
            }
        }
        return items;
    }

    /** Overwrites the items of a mode; duplicates are dropped; empty removes them. */
    public void saveFilter(Block block, FilterMode mode, List<ItemStack> items) {
        if (!(block.getState() instanceof TileState tile)) {
            return;
        }
        PersistentDataContainer pdc = tile.getPersistentDataContainer();
        NamespacedKey key = mode == FilterMode.ALLOW ? keyAllow : keyBlock;
        boolean matchByType = plugin.getSettings().isMatchByType();
        List<ItemStack> unique = new ArrayList<>();
        if (items != null) {
            for (ItemStack item : items) {
                if (item == null || item.getType() == Material.AIR) {
                    continue;
                }
                boolean duplicate = false;
                for (ItemStack stored : unique) {
                    if (matches(stored, item, matchByType)) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    ItemStack copy = item.clone();
                    copy.setAmount(1);
                    unique.add(copy);
                }
            }
        }
        if (unique.isEmpty()) {
            pdc.remove(key);
        } else {
            List<String> encoded = new ArrayList<>();
            for (ItemStack item : unique) {
                encoded.add(Base64.getEncoder().encodeToString(item.serializeAsBytes()));
            }
            pdc.set(key, PersistentDataType.LIST.strings(), encoded);
        }
        tile.update();
        plugin.getFilterVisual().update(block);
    }

    /** Removes both filter lists: the hopper becomes 'normal' again. */
    public void clearFilter(Block block) {
        if (!(block.getState() instanceof TileState tile)) {
            return;
        }
        PersistentDataContainer pdc = tile.getPersistentDataContainer();
        pdc.remove(keyAllow);
        pdc.remove(keyBlock);
        pdc.remove(keyMode);
        tile.update();
        plugin.getFilterVisual().update(block);
    }

    /** Does this hopper have an active filter (one of the lists filled)? */
    public boolean isFilterHopper(Block block) {
        return block != null
                && (!getItems(block, FilterMode.ALLOW).isEmpty()
                || !getItems(block, FilterMode.BLOCK).isEmpty());
    }

    /** The mode the GUI showed last (default: ALLOW). */
    public FilterMode getMode(Block block) {
        if (block.getState() instanceof TileState tile) {
            Boolean blockMode = tile.getPersistentDataContainer().get(keyMode, PersistentDataType.BOOLEAN);
            if (Boolean.TRUE.equals(blockMode)) {
                return FilterMode.BLOCK;
            }
        }
        return FilterMode.ALLOW;
    }

    public void setMode(Block block, FilterMode mode) {
        if (!(block.getState() instanceof TileState tile)) {
            return;
        }
        tile.getPersistentDataContainer().set(keyMode, PersistentDataType.BOOLEAN, mode == FilterMode.BLOCK);
        tile.update();
    }

    /**
     * May the item enter the hopper? The block list always wins; if the allow
     * list has items, the item also has to match one of them.
     */
    public boolean isAllowed(Block block, ItemStack item) {
        if (item == null) {
            return true;
        }
        boolean matchByType = plugin.getSettings().isMatchByType();
        for (ItemStack blocked : getItems(block, FilterMode.BLOCK)) {
            if (matches(blocked, item, matchByType)) {
                return false;
            }
        }
        List<ItemStack> allow = getItems(block, FilterMode.ALLOW);
        if (allow.isEmpty()) {
            return true;
        }
        for (ItemStack allowed : allow) {
            if (matches(allowed, item, matchByType)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(ItemStack filterItem, ItemStack item, boolean matchByType) {
        return matchByType ? filterItem.getType() == item.getType() : filterItem.isSimilar(item);
    }

    /** Is this inventory owned by a hopper? If so, return the block. */
    public Block getHopperBlock(Inventory inventory) {
        if (inventory != null && inventory.getHolder() instanceof Hopper hopper) {
            return hopper.getBlock();
        }
        return null;
    }
}