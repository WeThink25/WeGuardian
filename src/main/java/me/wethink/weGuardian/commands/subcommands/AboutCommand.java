package me.wethink.weGuardian.commands.subcommands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.utils.MessageUtils;
import org.bukkit.command.CommandSender;

@CommandAlias("weguardian|wg")
@Subcommand("about")
@CommandPermission("weguardian.about")
public class AboutCommand extends BaseCommand {

    private final WeGuardian plugin;

    public AboutCommand(WeGuardian plugin) {
        this.plugin = plugin;
    }

    @Subcommand("about")
    public void onAbout(CommandSender sender) {
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
}