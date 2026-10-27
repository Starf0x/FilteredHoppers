package dev.hopperfilter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

/**
 * Compares filter items with items. A filter item renamed to "#tag" (e.g.
 * "#logs" or "#minecraft:wool") matches every item in that item tag, whatever
 * the match mode.
 */
public final class ItemMatcher {

    private ItemMatcher() {
    }

    /** The item tag a filter item stands for, or null if it is a normal item. */
    public static NamespacedKey tagKey(ItemStack filterItem) {
        if (filterItem == null || !filterItem.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = filterItem.getItemMeta();
        Component name = meta.displayName();
        if (name == null) {
            return null;
        }
        String plain = PlainTextComponentSerializer.plainText().serialize(name).trim();
        if (!plain.startsWith("#") || plain.length() < 2) {
            return null;
        }
        return NamespacedKey.fromString(plain.substring(1).toLowerCase());
    }

    /** The item tag a filter item stands for, or null if unknown or none. */
    public static Tag<Material> tag(ItemStack filterItem) {
        NamespacedKey key = tagKey(filterItem);
        return key == null ? null : Bukkit.getTag(Tag.REGISTRY_ITEMS, key, Material.class);
    }

    /** Does the item match this filter item? */
    public static boolean matches(ItemStack filterItem, ItemStack item, MatchMode mode) {
        if (filterItem == null || item == null) {
            return false;
        }
        if (tagKey(filterItem) != null) {
            Tag<Material> tag = tag(filterItem);
            return tag != null && tag.isTagged(item.getType());
        }
        return mode == MatchMode.TYPE ? filterItem.getType() == item.getType() : filterItem.isSimilar(item);
    }

    /** Would these two filter items filter the same thing (a duplicate)? */
    public static boolean sameEntry(ItemStack a, ItemStack b, MatchMode mode) {
        if (a == null || b == null || a.getType() == Material.AIR || b.getType() == Material.AIR) {
            return false;
        }
        NamespacedKey tagA = tagKey(a);
        NamespacedKey tagB = tagKey(b);
        if (tagA != null || tagB != null) {
            return Objects.equals(tagA, tagB);
        }
        return mode == MatchMode.TYPE ? a.getType() == b.getType() : a.isSimilar(b);
    }
}
