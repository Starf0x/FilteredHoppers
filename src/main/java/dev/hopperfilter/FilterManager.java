package dev.hopperfilter;

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
 * Bewaart per hopper de "toestaan"-lijst en de "blokkeren"-lijst in het NBT
 * van het blok, zodat beide modussen tegelijk bekend zijn en het overleeft
 * bij serverherstart.
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

    /** Items van een modus (leeg als er geen item in zit). */
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

    /** Overschrijft de items van een modus; een lege lijst verwijdert ze. */
    public void saveFilter(Block block, FilterMode mode, List<ItemStack> items) {
        if (!(block.getState() instanceof TileState tile)) {
            return;
        }
        PersistentDataContainer pdc = tile.getPersistentDataContainer();
        NamespacedKey key = mode == FilterMode.ALLOW ? keyAllow : keyBlock;
        if (items == null || items.isEmpty()) {
            pdc.remove(key);
        } else {
            List<String> encoded = new ArrayList<>();
            for (ItemStack item : items) {
                encoded.add(Base64.getEncoder().encodeToString(item.serializeAsBytes()));
            }
            pdc.set(key, PersistentDataType.LIST.strings(), encoded);
        }
        tile.update();
        plugin.getFilterVisual().update(block);
    }

    /** Verwijdert beide filterlijsten: de hopper wordt weer 'gewoon'. */
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

    /** Heeft deze hopper een actief filter (een van beide lijsten gevuld)? */
    public boolean isFilterHopper(Block block) {
        return block != null
                && (!getItems(block, FilterMode.ALLOW).isEmpty()
                || !getItems(block, FilterMode.BLOCK).isEmpty());
    }

    /** De modus die de GUI als laatste toonde (default: ALLOW). */
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
     * Mag het item de hopper in? De blok-lijst wint altijd; als de toestaan-
     * lijst items bevat moet het item daarin matchen.
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

    /** Hoort een inventory bij een hopper? Zoja, geef het blok terug. */
    public Block getHopperBlock(Inventory inventory) {
        if (inventory != null && inventory.getHolder() instanceof Hopper hopper) {
            return hopper.getBlock();
        }
        return null;
    }
}