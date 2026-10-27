package dev.hopperfilter;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public final class Settings {

    private final HopperFilterPlugin plugin;

    private String guiTitle;
    private boolean matchByType;
    private boolean visualEnabled;
    private boolean debug;
    private FileConfiguration cfg;

    public Settings(HopperFilterPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        cfg = plugin.getConfig();
        guiTitle = cfg.getString("gui-title", "&0Hopper Filter");
        matchByType = cfg.getBoolean("match-by-type", true);
        visualEnabled = cfg.getBoolean("visual-enabled", true);
        debug = cfg.getBoolean("debug", false);
    }

    public String getGuiTitle() {
        return guiTitle;
    }

    public boolean isMatchByType() {
        return matchByType;
    }

    public boolean isVisualEnabled() {
        return visualEnabled;
    }

    public boolean isDebug() {
        return debug;
    }

    /** A text from the messages section; missing keys fall back to the defaults
     *  in the bundled config.yml. */
    public String message(String key) {
        String text = cfg.getString("messages." + key);
        return text == null ? "" : text;
    }

    public List<String> messageLines(String key) {
        return cfg.getStringList("messages." + key);
    }
}
