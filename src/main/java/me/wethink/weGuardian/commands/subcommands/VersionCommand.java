package me.wethink.weGuardian.commands.subcommands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.utils.MessageUtils;
import org.bukkit.command.CommandSender;

@CommandAlias("weguardian|wg")
@Description("Shows the plugin version and server information.")
@CommandPermission("weguardian.version")
public class VersionCommand extends BaseCommand {

    private final WeGuardian plugin;

    public VersionCommand(WeGuardian plugin) {
        this.plugin = plugin;
    }

    @CommandAlias("version")
    @Syntax("")
    public void onVersion(CommandSender sender) {
        sender.sendMessage(MessageUtils.colorize("&6WeGuardian &7v" + plugin.getPluginMeta().getVersion()));
        sender.sendMessage(MessageUtils.colorize("&7Running on: &f" + plugin.getServer().getName() + " " + plugin.getServer().getVersion()));
        sender.sendMessage(MessageUtils.colorize("&7Java Version: &f" + System.getProperty("java.version")));

        String dbType = plugin.getConfig().getBoolean("database.mysql.enabled", false) ? "MySQL" : "SQLite";
        sender.sendMessage(MessageUtils.colorize("&7Database: &f" + dbType));

        boolean placeholderAPI = plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
        sender.sendMessage(MessageUtils.colorize("&7PlaceholderAPI: " + (placeholderAPI ? "&aEnabled" : "&cDisabled")));
    }
}