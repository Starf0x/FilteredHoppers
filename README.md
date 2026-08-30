# FilteredHoppers

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

Walk up to a hopper and **shift-right-click** it (sneak + right-click) with an **empty main hand**. The filter menu opens. A normal right-click still opens the regular hopper screen.

> With any item in your main hand, shift-right-clicking keeps its vanilla behaviour: hold a hopper and shift-right-click a hopper to **place** it, instead of opening the filter menu.

### 2. Reading the menu

The menu has **two rows (18 slots)**:

| Slot | Meaning |
|------|---------|
| **Slot 0** | The mode indicator. **Green emerald** = *allow* (green mode). **Black blackstone** = *block* (black mode). |
| **Slot 1-17** | The filter items of the currently displayed mode (max 17 items per mode). |

The name and description of the indicator always tell you which mode you are editing.

### 3. Adding filter items

- Make sure the menu is on the mode you want (see point 4).
- **Click the items you want to filter from your own inventory into the slots**. Each slot holds a single item (stack of 1), and every item type appears only once (duplicates are dropped).
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

As soon as a hopper has an **active filter** (green or black filled), the **edges of the block turn red** (a solid red wireframe). You see in one glance which hoppers are doing something special.

- No particles: the rim is a real red border around the block's edges.
- The rim appears and disappears on its own when you save or clear a filter.
- The rim hides beyond ~16 blocks and never shows in unloaded areas, so it costs almost nothing.

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
3. Copy `FilteredHoppers.jar` into your server's `plugins/` folder.
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

- One hopper holds at most **1 item per mode** (so 1 green + 1 black).
- The filter is tied to the **block**, not to a player: anyone with access to the hopper can set filters on it.
- Removed filter hoppers lose their filter (the block is gone).

---

## Building & releasing

Requires **Java 21** and the [GitHub CLI](https://cli.github.com) (authenticated). The version lives in `gradle.properties`; the jar name and `plugin.yml` take it from there. **Every rebuild automatically bumps to a newer version.**

```bash
./gradlew build         # bumps the patch version and builds the new jar
./release.sh            # bumps, builds AND publishes a GitHub release
./release.sh 0.6.0      # release a specific version instead
```

Each release run:

1. bumps the patch version in `gradle.properties` (unless you pass one),
2. builds `build/libs/FilteredHoppers-<version>.jar` with `<version>` embedded in `plugin.yml`,
3. commits, tags (`v<version>`) and pushes to GitHub,
4. creates a GitHub release and uploads the versioned jar.

The latest release is always available at [releases/latest](https://github.com/Starf0x/FilteredHoppers/releases/latest).

---

## Version

See [releases](https://github.com/Starf0x/FilteredHoppers/releases) — the version is bumped automatically with every build.

Want to add something or found a bug? Open an issue in this repository.