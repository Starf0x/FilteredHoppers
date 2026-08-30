package dev.hopperfilter;

import dev.hopperfilter.command.HopperFilterCommand;
import dev.hopperfilter.listener.HopperFilterListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class HopperFilterPlugin extends JavaPlugin {

    private static HopperFilterPlugin instance;

    private Settings settings;
    private FilterManager filterManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        settings = new Settings(this);
        filterManager = new FilterManager(this);

        getCommand("hopperfilter").setExecutor(new HopperFilterCommand(this));
        getServer().getPluginManager().registerEvents(new HopperFilterListener(this), this);

        getLogger().log(Level.INFO, "FilteredHoppers loaded.");
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    public static HopperFilterPlugin getInstance() {
        return instance;
    }

    public Settings getSettings() {
        return settings;
    }

    public FilterManager getFilterManager() {
        return filterManager;
    }
}