package dev.hopperfilter.listener;

import dev.hopperfilter.FilterGui;
import dev.hopperfilter.FilterManager;
import dev.hopperfilter.HopperFilterPlugin;
import dev.hopperfilter.util.Text;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;

public final class HopperFilterListener implements Listener {

    private final HopperFilterPlugin plugin;
    private final FilterManager manager;

    public HopperFilterListener(HopperFilterPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getFilterManager();
    }

    /** Elke hopper is een filterhopper: shift-rechtsklik opent het filtermenu. */
    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.HOPPER) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }

        event.setCancelled(true);
        player.openInventory(new FilterGui(plugin, block).getInventory());
        player.sendMessage(Text.colorize("&7Groen = toestaan, zwart = blokkeren. Klik het te filteren item in een slot en sluit om op te slaan."));
    }

    /** Alleen de emerald/blackstone op slot 0 mag wisselen; de rest is het menu. */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof FilterGui gui)) {
            return;
        }
        if (event.getRawSlot() != FilterGui.TOGGLE_SLOT) {
            return;
        }
        event.setCancelled(true);
        ClickType click = event.getClick();
        if (click == ClickType.LEFT || click == ClickType.RIGHT) {
            gui.toggleMode();
        }
    }

    /** Voorkom dat je items op de emerald/blackstone sleept. */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof FilterGui)) {
            return;
        }
        if (event.getRawSlots().contains(FilterGui.TOGGLE_SLOT)) {
            event.setCancelled(true);
        }
    }

    /** Item via de grond laten opnemen alleen als het door de filter mag. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(InventoryPickupItemEvent event) {
        Block block = manager.getHopperBlock(event.getInventory());
        if (block != null && !manager.isAllowed(block, event.getItem().getItemStack())) {
            event.setCancelled(true);
        }
    }

    /** Verplaatsing van container naar hopper filteren. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        Block block = manager.getHopperBlock(event.getDestination());
        if (block != null && !manager.isAllowed(block, event.getItem())) {
            event.setCancelled(true);
        }
    }

    /** GUI sluiten = de getoonde modus opslaan. */
    @EventHandler
    public void onGUIClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof FilterGui gui) {
            gui.commit();
        }
    }

    /** Filterhoppers in een nieuw geladen chunk opnieuw registeren voor de rode rand. */
    @EventHandler(ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        plugin.getFilterVisual().rebuildChunk(event.getChunk());
    }
}