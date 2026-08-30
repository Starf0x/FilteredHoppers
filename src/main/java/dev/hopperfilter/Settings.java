package dev.hopperfilter;

import org.bukkit.configuration.file.FileConfiguration;

public final class Settings {

    private final HopperFilterPlugin plugin;

    private String guiTitle;
    private boolean matchByType;

    public Settings(HopperFilterPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration cfg = plugin.getConfig();
        guiTitle = cfg.getString("gui-title", "&0Hopper Filter");
        matchByType = cfg.getBoolean("match-by-type", true);
    }

    public String getGuiTitle() {
        return guiTitle;
    }

    public boolean isMatchByType() {
        return matchByType;
    }
}