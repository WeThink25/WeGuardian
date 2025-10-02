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
@Description("Shows the WeGuardian help menu.")
@CommandPermission("weguardian.help")
public class HelpCommand extends BaseCommand {

    private final WeGuardian plugin;

    public HelpCommand(WeGuardian plugin) {
        this.plugin = plugin;
    }

    @CommandAlias("help")
    @Syntax("")
    public void onHelp(CommandSender sender) {
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
}