package me.wethink.weGuardian.commands;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WeGuardianCommand implements CommandExecutor, TabCompleter {

    private final WeGuardian plugin;

    public WeGuardianCommand(WeGuardian plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload" -> handleReload(sender);
            case "about" -> handleAbout(sender);
            case "version" -> handleVersion(sender);
            case "help" -> showHelp(sender);
            default -> showHelp(sender);
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("weguardian.reload")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to reload the plugin."));
            return;
        }

        sender.sendMessage(MessageUtils.colorize("&6⚡ Reloading WeGuardian..."));

        try {
            plugin.reloadConfigurations();

            plugin.getTemplateService().reloadTemplates();

            sender.sendMessage(MessageUtils.colorize("&a✓ Reload complete"));
            sender.sendMessage(MessageUtils.colorize("&7  - config.yml"));
            sender.sendMessage(MessageUtils.colorize("&7  - messages.yml"));
            sender.sendMessage(MessageUtils.colorize("&7  - gui/*.yml"));
            sender.sendMessage(MessageUtils.colorize("&7  - templates.yml"));
        } catch (Exception e) {
            sender.sendMessage(MessageUtils.colorize("&c✗ Error reloading: " + e.getMessage()));
            plugin.getLogger().severe("Error reloading configuration: " + e.getMessage());
        }
    }

    private void handleAbout(CommandSender sender) {
        sender.sendMessage(MessageUtils.colorize("&6&l=== WeGuardian Plugin Information ==="));
        sender.sendMessage(MessageUtils.colorize("&7Version: &f" + plugin.getPluginMeta().getVersion()));
        sender.sendMessage(MessageUtils.colorize("&7Authors: &f" + String.join(", ", plugin.getPluginMeta().getAuthors())));
        sender.sendMessage(MessageUtils.colorize("&7Description: &f" + plugin.getPluginMeta().getDescription()));
        sender.sendMessage(MessageUtils.colorize("&7Website: &f" + plugin.getPluginMeta().getWebsite()));
        sender.sendMessage("");
        sender.sendMessage(MessageUtils.colorize("&6Features:"));
        sender.sendMessage(MessageUtils.colorize("&7  • GUI-driven punishment system"));
        sender.sendMessage(MessageUtils.colorize("&7  • Cross-server MySQL support"));
        sender.sendMessage(MessageUtils.colorize("&7  • ActionBar + Chat notifications"));
        sender.sendMessage(MessageUtils.colorize("&7  • Staff statistics & accountability"));
        sender.sendMessage(MessageUtils.colorize("&7  • PlaceholderAPI integration"));
        sender.sendMessage(MessageUtils.colorize("&7  • Folia & Paper compatible"));
        sender.sendMessage("");
        sender.sendMessage(MessageUtils.colorize("&7Use &f/weguardian help &7for command list"));
    }

    private void handleVersion(CommandSender sender) {
        sender.sendMessage(MessageUtils.colorize("&6WeGuardian &7v" + plugin.getPluginMeta().getVersion()));
        sender.sendMessage(MessageUtils.colorize("&7Running on: &f" + plugin.getServer().getName() + " " + plugin.getServer().getVersion()));
        sender.sendMessage(MessageUtils.colorize("&7Java Version: &f" + System.getProperty("java.version")));

        String dbType = plugin.getConfig().getBoolean("database.mysql.enabled", false) ? "MySQL" : "SQLite";
        sender.sendMessage(MessageUtils.colorize("&7Database: &f" + dbType));

        boolean placeholderAPI = plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
        sender.sendMessage(MessageUtils.colorize("&7PlaceholderAPI: " + (placeholderAPI ? "&aEnabled" : "&cDisabled")));
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(MessageUtils.colorize("&6&l=== WeGuardian Commands ==="));
        sender.sendMessage(MessageUtils.colorize("&f/weguardian reload &7- Reload all configuration files"));
        sender.sendMessage(MessageUtils.colorize("&f/weguardian about &7- Show plugin information"));
        sender.sendMessage(MessageUtils.colorize("&f/weguardian version &7- Show version details"));
        sender.sendMessage(MessageUtils.colorize("&f/weguardian help &7- Show this help menu"));
        sender.sendMessage("");
        sender.sendMessage(MessageUtils.colorize("&6Main Commands:"));
        sender.sendMessage(MessageUtils.colorize("&f/punish <player> &7- Open punishment GUI"));
        sender.sendMessage(MessageUtils.colorize("&f/history <player> &7- View punishment history"));
        sender.sendMessage(MessageUtils.colorize("&f/checkban <player> &7- Check ban/mute status"));
        sender.sendMessage(MessageUtils.colorize("&f/stats &7- View global statistics"));
        sender.sendMessage("");
        sender.sendMessage(MessageUtils.colorize("&7For full command list, see the plugin documentation"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("reload", "about", "version", "help");
            return subCommands.stream()
                    .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
