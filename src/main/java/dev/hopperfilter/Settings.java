package dev.hopperfilter;

import org.bukkit.configuration.file.FileConfiguration;

public final class Settings {

    private final HopperFilterPlugin plugin;

    private String guiTitle;
    private boolean matchByType;
    private boolean visualEnabled;
    private boolean debug;

    public Settings(HopperFilterPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration cfg = plugin.getConfig();
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
}