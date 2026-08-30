# Paper Filtered Hoppers

A plugin for [Paper](https://papermc.io) (Minecraft **1.21.x**) that turns **every hopper** into a smart filter hopper. You configure per hopper which items it lets through or blocks, so you can build your own sorting setups without any redstone headaches.

This plugin focuses 100% on **functionality**; for the technical details, look at the code or the config.

---

## At a glance

- **Every hopper can be a filter hopper.** A hopper you never configure behaves exactly like vanilla.
- **No tools, no extra items.** Everything is done with shift-right-clicks.
- **Two modes at once per hopper:** an *allow* list (green) and a *block* list (black). The hopper remembers both at the same time and can use them together.
- **Red rim** around filter hoppers, so you can see at a glance which hoppers are active.

---

## How it works

### 1. Opening the filter menu

Walk up to a hopper and **shift-right-click** it (sneak + right-click). The filter menu opens. A normal right-click still opens the regular hopper screen.

### 2. Reading the menu

The menu has **9 slots**:

| Slot | Meaning |
|------|---------|
| **Slot 1** | The mode indicator. **Green emerald** = *allow* (green mode). **Black blackstone** = *block* (black mode). |
| **Slot 2 through 9** | The filter items of the currently displayed mode (max 8 items per mode). |

The name and description of the indicator always tell you which mode you are editing.

### 3. Adding filter items

- Make sure the menu is on the mode you want (see point 4).
- **Click the item you want to filter from your own inventory into an empty slot**.
- Close the screen to save. The hopper knows immediately.

### 4. Switching between the modes

Click the **emerald/blackstone** in slot 1. The hopper saves the current mode, switches to the other one, and shows that mode's items. Both lists are kept, so you can fill the green and black modes at the same time.

### 5. Clearing a filter (partly)

Open the menu, remove the items from the slots, and close the screen. An empty list is the same as "no filter". To clear both modes completely, empty both.

---

## The filter logic

The hopper uses **both lists at the same time** to decide whether an item may enter:

1. **The black list always wins.** Does the item match an item in the *block* mode? Then it is **blocked**, no matter what the green list says.
2. **The green list is a whitelist.** Is the item in the green mode and not in the black? Then it is **allowed**.
3. **An empty green list** means: everything is welcome, except the blocked items.

| Green list | Black list | Behaviour |
|------------|------------|-----------|
| Empty | Empty | Vanilla: everything is allowed |
| Filled | Empty | Only the listed items are allowed (sorting filter) |
| Empty | Filled | Everything is allowed except the listed items (blocklist) |
| Filled | Filled | Only green items that are not black are allowed |

The filter applies to **every way items enter**:
- ground items a hopper picks up,
- items a hopper sucks out of a chest or other container above it,
- items another hopper hands over.

A filter hopper **pushes** everything it contains onward (for example into a chest below), because everything inside it is allowed by definition.

### Why both lists are useful

- **Sorting lines:** put each filter hopper's own item type in the green mode. Every item type then falls into its own hopper's chest below, while the rest keeps moving.
- **Blockades:** want a conveyor to let everything through except one item type? Put that type in the black mode and keep the green mode empty.
- **Precise combinations:** want "all diamonds, except diamond swords"? Green = diamond, black = diamond sword, done.

### Item matching

By default the plugin matches on **item type**: a filter with *iron ingot* lets all iron ingots through, regardless of name or enchantments.

You can switch this in the config to **exact matching**: then name, lore, and enchantments count as well. Useful when you want to single out one specific, named sword.

---

## The red rim

As soon as a hopper has an **active filter** (green or black filled), it gets a **red glowing rim** of particles along the edges of the block. You see in one glance which hoppers are doing something special.

- The rim appears and disappears on its own when you save or clear a filter.
- Particles are only visible when you stand within **~32 blocks**; far away or in unloaded areas it costs nothing.
- Optimized so it barely costs any server performance.
- Can be switched off via the config (`visual-enabled`).

---

## Configuration

All settings live in `config.yml` in the plugin folder. After a change, reload with:

```
/hopperfilter reload
```

| Setting | Default | Meaning |
|---------|---------|---------|
| `gui-title` | `&0Hopper Filter` | Title of the filter menu (colour codes with `&` are allowed). |
| `match-by-type` | `true` | `true` = filter on item type; `false` = exact item matching (name/lore/enchantments). |
| `visual-enabled` | `true` | Shows the red rim around filter hoppers (`true`/`false`). |

---

## Installing

1. Use a **Paper** server of **1.21.x** (the plugin does not work on Spigot).
2. Make sure **Java 21** is installed.
3. Copy `PaperFilteredHoppers.jar` into your server's `plugins/` folder.
4. Restart the server (or use a plugin manager such as PlugMan).
5. You're done: shift-right-click a hopper and set up your filter.

---

## Commands and permissions

| Command | Description | Permission |
|---------|-------------|------------|
| `/hopperfilter reload` | Reloads the config without a server restart. | `hopperfilter.reload` (operators by default) |

Only the config reload has a permission; **every player** can use shift-right-click to manage filters.

---

## Known limitations

- One hopper can hold at most **8 items per mode** (so 8 green + 8 black).
- The filter is tied to the **block**, not to a player: anyone with access to the hopper can set filters on it.
- Removed filter hoppers lose their filter (the block is gone).

---

## Version

**0.5.0** — first public release.

Want to add something or found a bug? Open an issue in this repository.