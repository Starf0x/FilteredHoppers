package dev.hopperfilter;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilterManagerTest {

    private ServerMock server;
    private HopperFilterPlugin plugin;
    private FilterManager manager;
    private FilterHolder hopper;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(HopperFilterPlugin.class);
        plugin.getConfig().set("visual-enabled", false);
        plugin.getSettings().reload();
        manager = plugin.getFilterManager();
        WorldMock world = server.addSimpleWorld("world");
        Block block = world.getBlockAt(0, 64, 0);
        block.setType(Material.HOPPER);
        hopper = FilterHolder.of(block);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static ItemStack item(Material type) {
        return new ItemStack(type);
    }

    private static ItemStack named(Material type, String name) {
        ItemStack stack = new ItemStack(type);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(name));
        stack.setItemMeta(meta);
        return stack;
    }

    @Test
    void emptyFilterAllowsEverything() {
        assertFalse(manager.isFilterHopper(hopper));
        assertTrue(manager.isAllowed(hopper, item(Material.ENDER_EYE)));
    }

    @Test
    void blockListBlocks() {
        manager.saveFilter(hopper, FilterMode.BLOCK, List.of(item(Material.ENDER_EYE)));
        assertTrue(manager.isFilterHopper(hopper));
        assertFalse(manager.isAllowed(hopper, item(Material.ENDER_EYE)));
        assertFalse(manager.isAllowed(hopper, new ItemStack(Material.ENDER_EYE, 64)));
        assertTrue(manager.isAllowed(hopper, item(Material.STONE)));
    }

    @Test
    void allowListOnlyLetsListedItemsThrough() {
        manager.saveFilter(hopper, FilterMode.ALLOW, List.of(item(Material.DIAMOND)));
        assertTrue(manager.isAllowed(hopper, item(Material.DIAMOND)));
        assertFalse(manager.isAllowed(hopper, item(Material.STONE)));
    }

    @Test
    void blockListWinsOverAllowList() {
        manager.saveFilter(hopper, FilterMode.ALLOW, List.of(item(Material.DIAMOND)));
        manager.saveFilter(hopper, FilterMode.BLOCK, List.of(item(Material.DIAMOND)));
        assertFalse(manager.isAllowed(hopper, item(Material.DIAMOND)));
    }

    @Test
    void duplicatesAreDropped() {
        manager.saveFilter(hopper, FilterMode.ALLOW, List.of(item(Material.DIAMOND), item(Material.DIAMOND)));
        assertEquals(1, manager.getItems(hopper, FilterMode.ALLOW).size());
    }

    @Test
    void exactMatchingIsPerHopper() {
        manager.saveFilter(hopper, FilterMode.ALLOW, List.of(named(Material.DIAMOND, "Special")));
        assertTrue(manager.isAllowed(hopper, item(Material.DIAMOND)), "type matching is the default");
        manager.setMatch(hopper, MatchMode.EXACT);
        assertEquals(MatchMode.EXACT, manager.getMatch(hopper));
        assertFalse(manager.isAllowed(hopper, item(Material.DIAMOND)));
        assertTrue(manager.isAllowed(hopper, named(Material.DIAMOND, "Special")));
    }

    @Test
    void cacheIsReloadedFromStoredData() {
        manager.saveFilter(hopper, FilterMode.BLOCK, List.of(item(Material.ENDER_EYE)));
        manager.forget(hopper.key());
        assertFalse(manager.isAllowed(hopper, item(Material.ENDER_EYE)));
    }

    @Test
    void clearedOrRemovedHopperFiltersNothing() {
        manager.saveFilter(hopper, FilterMode.BLOCK, List.of(item(Material.ENDER_EYE)));
        hopper.getBlock().setType(Material.AIR);
        assertTrue(manager.isAllowed(hopper, item(Material.ENDER_EYE)));
        hopper.getBlock().setType(Material.HOPPER);
        manager.forget(hopper.key());
        assertTrue(manager.isAllowed(hopper, item(Material.ENDER_EYE)), "a new hopper starts without filter");
    }

    @Test
    void tagItemsAreNoDuplicatesOfEachOther() {
        ItemStack logs = named(Material.PAPER, "#logs");
        ItemStack wool = named(Material.PAPER, "#wool");
        assertFalse(ItemMatcher.sameEntry(logs, wool, MatchMode.TYPE));
        assertTrue(ItemMatcher.sameEntry(logs, named(Material.STICK, "#minecraft:logs"), MatchMode.TYPE));
        assertFalse(ItemMatcher.sameEntry(logs, item(Material.PAPER), MatchMode.TYPE));
    }

    @Test
    void tagFilterMatchesTheWholeGroup() {
        manager.saveFilter(hopper, FilterMode.ALLOW, List.of(named(Material.PAPER, "#logs")));
        assertTrue(manager.isAllowed(hopper, item(Material.OAK_LOG)));
        assertTrue(manager.isAllowed(hopper, item(Material.BIRCH_LOG)));
        assertFalse(manager.isAllowed(hopper, item(Material.PAPER)));
    }
}
