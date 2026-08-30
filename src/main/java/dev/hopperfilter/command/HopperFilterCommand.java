package dev.hopperfilter.command;

import dev.hopperfilter.HopperFilterPlugin;
import dev.hopperfilter.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class HopperFilterCommand implements CommandExecutor {

    private final HopperFilterPlugin plugin;

    public HopperFilterCommand(HopperFilterPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.getSettings().reload();
            plugin.getFilterVisual().stop();
            plugin.getFilterVisual().start();
            sender.sendMessage(Text.colorize("&aConfig reloaded."));
            return true;
        }
        sender.sendMessage(Text.colorize("&cUsage: /hopperfilter reload"));
        return true;
    }
}