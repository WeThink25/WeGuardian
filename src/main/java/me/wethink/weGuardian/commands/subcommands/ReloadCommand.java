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
@Description("Reloads the plugin configuration.")
@CommandPermission("weguardian.reload")
public class ReloadCommand extends BaseCommand {

    private final WeGuardian plugin;

    public ReloadCommand(WeGuardian plugin) {
        this.plugin = plugin;
    }

    @CommandAlias("reload")
    @Syntax("")
    public void onReload(CommandSender sender) {
        sender.sendMessage(MessageUtils.colorize("&6⚡ Reloading WeGuardian..."));

        try {
            plugin.reloadConfigurations();
            plugin.getTemplateService().reloadTemplates();

            sender.sendMessage(MessageUtils.colorize("&a✔ Reload complete"));
            sender.sendMessage(MessageUtils.colorize("&7  - config.yml"));
            sender.sendMessage(MessageUtils.colorize("&7  - messages.yml"));
            sender.sendMessage(MessageUtils.colorize("&7  - gui/*.yml"));
            sender.sendMessage(MessageUtils.colorize("&7  - templates.yml"));
        } catch (Exception e) {
            sender.sendMessage(MessageUtils.colorize("&c✖ Error reloading: " + e.getMessage()));
            plugin.getLogger().severe("Error reloading configuration: " + e.getMessage());
        }
    }
}