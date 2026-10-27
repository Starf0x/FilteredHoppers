package dev.hopperfilter;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores the "allow" list, the "block" list and the match mode per hopper in
 * the NBT of the block (or on the hopper minecart), so both modes are known at
 * the same time and survive a server restart. The decoded lists are cached in
 * memory, so the per-item hopper checks (pickup and transfer) never touch NBT
 * again once a hopper is loaded; only saves change the cache. Cache entries are
 * dropped when their chunk or minecart unloads.
 */
public final class FilterManager {

    private final HopperFilterPlugin plugin;
    private final NamespacedKey keyAllow;
    private final NamespacedKey keyBlock;
    private final NamespacedKey keyMode;
    private final NamespacedKey keyMatch;

    private final Map<Object, FilterCache> caches = new ConcurrentHashMap<>();

    public FilterManager(HopperFilterPlugin plugin) {
        this.plugin = plugin;
        this.keyAllow = new NamespacedKey(plugin, "hopper-filter-allow");
        this.keyBlock = new NamespacedKey(plugin, "hopper-filter-block");
        this.keyMode = new NamespacedKey(plugin, "hopper-filter-mode");
        this.keyMatch = new NamespacedKey(plugin, "hopper-filter-match");
    }

    /** Items of a mode (empty if there are none); returns the cached copy. */
    public List<ItemStack> getItems(FilterHolder holder, FilterMode mode) {
        if (holder == null || !holder.isValid()) {
            return List.of();
        }
        FilterCache cache = cache(holder);
        return mode == FilterMode.ALLOW ? cache.allow : cache.block;
    }

    /** Overwrites the items of a mode; duplicates are dropped; empty removes them. */
    public void saveFilter(FilterHolder holder, FilterMode mode, List<ItemStack> items) {
        MatchMode match = getMatch(holder);
        List<ItemStack> unique = new ArrayList<>();
        if (items != null) {
            for (ItemStack item : items) {
                if (item == null || item.getType() == Material.AIR) {
                    continue;
                }
                boolean duplicate = false;
                for (ItemStack stored : unique) {
                    if (ItemMatcher.sameEntry(stored, item, match)) {
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
        NamespacedKey key = mode == FilterMode.ALLOW ? keyAllow : keyBlock;
        holder.edit(pdc -> {
            if (unique.isEmpty()) {
                pdc.remove(key);
            } else {
                List<String> encoded = new ArrayList<>();
                for (ItemStack item : unique) {
                    encoded.add(Base64.getEncoder().encodeToString(item.serializeAsBytes()));
                }
                pdc.set(key, PersistentDataType.LIST.strings(), encoded);
            }
        });
        // Reload the cache from the saved data so the hot path never re-reads NBT.
        caches.remove(holder.key());
        updateVisual(holder);
    }

    /** Removes all filter data: the hopper becomes 'normal' again. */
    public void clearFilter(FilterHolder holder) {
        caches.remove(holder.key());
        holder.edit(pdc -> {
            pdc.remove(keyAllow);
            pdc.remove(keyBlock);
            pdc.remove(keyMode);
            pdc.remove(keyMatch);
        });
        updateVisual(holder);
    }

    /** Forgets the cached data, e.g. when a new hopper is placed at a location. */
    public void forget(Object key) {
        caches.remove(key);
    }

    /** Forgets the cached data of every hopper block in an unloaded chunk. */
    public void forgetChunk(Chunk chunk) {
        caches.keySet().removeIf(key -> key instanceof Location loc
                && loc.getWorld() == chunk.getWorld()
                && loc.getBlockX() >> 4 == chunk.getX()
                && loc.getBlockZ() >> 4 == chunk.getZ());
    }

    /** Does this hopper have an active filter (one of the lists filled)? */
    public boolean isFilterHopper(FilterHolder holder) {
        return !getItems(holder, FilterMode.ALLOW).isEmpty() || !getItems(holder, FilterMode.BLOCK).isEmpty();
    }

    public boolean isFilterHopper(Block block) {
        return block != null && isFilterHopper(FilterHolder.of(block));
    }

    /** The mode the GUI showed last (default: ALLOW). */
    public FilterMode getMode(FilterHolder holder) {
        return cache(holder).mode;
    }

    public void setMode(FilterHolder holder, FilterMode mode) {
        holder.edit(pdc -> pdc.set(keyMode, PersistentDataType.BOOLEAN, mode == FilterMode.BLOCK));
        cache(holder).mode = mode;
    }

    /** The match mode of this hopper; the config default if none was chosen. */
    public MatchMode getMatch(FilterHolder holder) {
        MatchMode match = cache(holder).match;
        if (match != null) {
            return match;
        }
        return plugin.getSettings().isMatchByType() ? MatchMode.TYPE : MatchMode.EXACT;
    }

    public void setMatch(FilterHolder holder, MatchMode match) {
        holder.edit(pdc -> pdc.set(keyMatch, PersistentDataType.STRING, match.name()));
        caches.remove(holder.key());
    }

    /** May the item enter the hopper? The block list always wins; if the allow
     *  list has items, the item also has to match one of them. */
    public boolean isAllowed(FilterHolder holder, ItemStack item) {
        if (item == null || holder == null || !holder.isValid()) {
            return true;
        }
        FilterCache cache = cache(holder);
        MatchMode match = getMatch(holder);
        for (Entry blocked : cache.blockEntries) {
            if (blocked.matches(item, match)) {
                return false;
            }
        }
        if (cache.allowEntries.isEmpty()) {
            return true;
        }
        for (Entry allowed : cache.allowEntries) {
            if (allowed.matches(item, match)) {
                return true;
            }
        }
        return false;
    }

    private void updateVisual(FilterHolder holder) {
        if (holder.getBlock() != null) {
            plugin.getFilterVisual().update(holder.getBlock());
        }
    }

    /** Returns (and populates) the in-memory copy of a hopper's filter data. */
    private FilterCache cache(FilterHolder holder) {
        return caches.computeIfAbsent(holder.key(), key -> load(holder));
    }

    private FilterCache load(FilterHolder holder) {
        FilterCache cache = new FilterCache();
        PersistentDataContainer pdc = holder.data();
        if (pdc == null) {
            return cache;
        }
        cache.allow = readList(pdc, keyAllow);
        cache.block = readList(pdc, keyBlock);
        cache.allowEntries = compile(cache.allow);
        cache.blockEntries = compile(cache.block);
        if (Boolean.TRUE.equals(pdc.get(keyMode, PersistentDataType.BOOLEAN))) {
            cache.mode = FilterMode.BLOCK;
        }
        String match = pdc.get(keyMatch, PersistentDataType.STRING);
        if (match != null) {
            try {
                cache.match = MatchMode.valueOf(match);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return cache;
    }

    private List<ItemStack> readList(PersistentDataContainer pdc, NamespacedKey key) {
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

    /** Resolves "#tag" filter items once, so the per-item checks stay cheap. */
    private static List<Entry> compile(List<ItemStack> items) {
        List<Entry> entries = new ArrayList<>();
        for (ItemStack item : items) {
            boolean isTag = ItemMatcher.tagKey(item) != null;
            entries.add(new Entry(item, isTag, isTag ? ItemMatcher.tag(item) : null));
        }
        return entries;
    }

    private record Entry(ItemStack item, boolean isTag, Tag<Material> tag) {
        boolean matches(ItemStack other, MatchMode match) {
            if (isTag) {
                return tag != null && tag.isTagged(other.getType());
            }
            return match == MatchMode.TYPE ? item.getType() == other.getType() : item.isSimilar(other);
        }
    }

    private static final class FilterCache {
        List<ItemStack> allow = List.of();
        List<ItemStack> block = List.of();
        List<Entry> allowEntries = List.of();
        List<Entry> blockEntries = List.of();
        FilterMode mode = FilterMode.ALLOW;
        MatchMode match;
    }
}
