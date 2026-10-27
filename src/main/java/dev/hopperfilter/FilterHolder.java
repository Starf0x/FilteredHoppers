package dev.hopperfilter;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.block.TileState;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.function.Consumer;

/**
 * Something that can carry a filter: a hopper block (data in the tile's NBT)
 * or a hopper minecart (data on the entity). The key identifies it in the
 * in-memory cache: the block location or the minecart's UUID.
 */
public final class FilterHolder {

    private final Block block;
    private final HopperMinecart cart;

    private FilterHolder(Block block, HopperMinecart cart) {
        this.block = block;
        this.cart = cart;
    }

    public static FilterHolder of(Block block) {
        return new FilterHolder(block, null);
    }

    public static FilterHolder of(HopperMinecart cart) {
        return new FilterHolder(null, cart);
    }

    /** The holder owning this inventory, or null if it is no hopper. Uses no
     *  block snapshot: this runs for every item a hopper moves. */
    public static FilterHolder of(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        InventoryHolder holder = inventory.getHolder(false);
        if (holder instanceof Hopper hopper) {
            return of(hopper.getBlock());
        }
        if (holder instanceof HopperMinecart cart) {
            return of(cart);
        }
        return null;
    }

    public Object key() {
        return block != null ? block.getLocation() : cart.getUniqueId();
    }

    /** Is the hopper (still) there? */
    public boolean isValid() {
        return block != null ? block.getType() == Material.HOPPER : cart.isValid();
    }

    /** Read access to the stored data; null if there is none. */
    public PersistentDataContainer data() {
        if (block != null) {
            return block.getState() instanceof TileState tile ? tile.getPersistentDataContainer() : null;
        }
        return cart.getPersistentDataContainer();
    }

    /** Changes the stored data and writes it back. */
    public void edit(Consumer<PersistentDataContainer> change) {
        if (block == null) {
            change.accept(cart.getPersistentDataContainer());
        } else if (block.getState() instanceof TileState tile) {
            change.accept(tile.getPersistentDataContainer());
            tile.update();
        }
    }

    /** The hopper block, or null for a minecart. */
    public Block getBlock() {
        return block;
    }

    public Location getLocation() {
        return block != null ? block.getLocation() : cart.getLocation();
    }
}
