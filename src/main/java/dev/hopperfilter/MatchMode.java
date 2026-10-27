package dev.hopperfilter;

/** How a filter item is compared with the items passing by. */
public enum MatchMode {
    /** Only the item type counts. */
    TYPE,
    /** Name, lore and enchantments count as well. */
    EXACT
}
