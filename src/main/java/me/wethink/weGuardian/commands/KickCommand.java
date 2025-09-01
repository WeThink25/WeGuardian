package me.wethink.weGuardian.commands;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.services.PunishmentService;
import me.wethink.weGuardian.services.TemplateService;
import me.wethink.weGuardian.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class KickCommand implements CommandExecutor, TabCompleter {

    private final WeGuardian plugin;
    private final PunishmentService punishmentService;
    private final TemplateService templateService;

    public KickCommand(WeGuardian plugin, PunishmentService punishmentService) {
        this.plugin = plugin;
        this.punishmentService = punishmentService;
        this.templateService = plugin.getTemplateService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("weguardian.kick")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtils.colorize("&cUsage: /kick <player> <reason> [-s] [-t template] [--ip]"));
            sender.sendMessage(MessageUtils.colorize("&7Options:"));
            sender.sendMessage(MessageUtils.colorize("&7  -s: Silent kick (no broadcast)"));
            sender.sendMessage(MessageUtils.colorize("&7  -t <template>: Use punishment template"));
            sender.sendMessage(MessageUtils.colorize("&7  --ip: Kick all players with same IP"));
            return true;
        }

        String targetName = args[0];
        boolean silent = false;
        boolean ipKick = false;
        final String template;
        List<String> reasonParts = new ArrayList<>();

        String tempTemplate = null;

        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-s")) {
                silent = true;
            } else if (arg.equals("--ip")) {
                ipKick = true;
            } else if (arg.equals("-t") && i + 1 < args.length) {
                tempTemplate = args[i + 1];
                i++;
            } else {
                reasonParts.add(arg);
            }
        }

        final boolean finalSilent = silent;
        template = tempTemplate;

        if (reasonParts.isEmpty()) {
            sender.sendMessage(MessageUtils.colorize("&cYou must provide a reason for the kick."));
            return true;
        }

        String reason = String.join(" ", reasonParts);

        if (ipKick) {
            handleIPKick(sender, targetName, reason, finalSilent, template);
            return true;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(MessageUtils.colorize("&cPlayer '" + targetName + "' is not online."));
            return true;
        }

        UUID targetUuid = target.getUniqueId();

        plugin.getPunishmentService().canPunish(sender, targetUuid).thenAccept(canPunish -> {
            if (!canPunish) {
                sender.sendMessage(MessageUtils.colorize("&cYou cannot kick this player due to permission hierarchy."));
                return;
            }

            String staffName = sender.getName();

            if (template != null && templateService.hasTemplate(template)) {
                templateService.executePunishmentFromTemplate(target.getUniqueId(), target.getName(),
                        staffName, template, (String) reason).thenAccept(success -> {
                    if (success) {
                        sender.sendMessage(MessageUtils.colorize("&aSuccessfully applied template '" + template +
                                "' punishment to " + target.getName()));
                    } else {
                        sender.sendMessage(MessageUtils.colorize("&cFailed to apply template punishment."));
                    }
                });
            } else {
                punishmentService.kick(target.getName(), staffName, reason).thenAccept(success -> {
                    if (success) {
                        String message = "&aSuccessfully kicked " + target.getName() + " for: &e" + reason;
                        if (finalSilent) {
                            message += " &7(Silent)";
                        }
                        sender.sendMessage(MessageUtils.colorize(message));

                        plugin.getLogger().info(staffName + " kicked " + target.getName() + " for: " + reason +
                                (finalSilent ? " (Silent)" : ""));
                    } else {
                        sender.sendMessage(MessageUtils.colorize("&cFailed to kick " + target.getName() +
                                ". Player may have disconnected."));
                    }
                }).exceptionally(throwable -> {
                    sender.sendMessage(MessageUtils.colorize("&cError processing kick: " + throwable.getMessage()));
                    plugin.getLogger().severe("Error in kick command: " + throwable.getMessage());
                    return null;
                });
            }
        });

        return true;
    }

    private void handleIPKick(CommandSender sender, String targetName, String reason, boolean silent, String template) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(MessageUtils.colorize("&cPlayer '" + targetName + "' is not online."));
            return;
        }

        if (!sender.hasPermission("weguardian.ipkick")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use IP kick."));
            return;
        }

        UUID targetUuid = target.getUniqueId();

        plugin.getPunishmentService().canPunish(sender, targetUuid).thenAccept(canPunish -> {
            if (!canPunish) {
                sender.sendMessage(MessageUtils.colorize("&cYou cannot kick this player due to permission hierarchy."));
                return;
            }

            String targetIP = target.getAddress().getAddress().getHostAddress();
            String staffName = sender.getName();

            List<Player> playersToKick = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getAddress().getAddress().getHostAddress().equals(targetIP)) {
                    plugin.getPunishmentService().canPunish(sender, player.getUniqueId()).thenAccept(canPunishPlayer -> {
                        if (canPunishPlayer) {
                            playersToKick.add(player);
                        }
                    });
                }
            }

            if (playersToKick.isEmpty()) {
                sender.sendMessage(MessageUtils.colorize("&cNo kickable players found with IP: " + targetIP));
                return;
            }

            for (Player player : playersToKick) {
                punishmentService.kick(player.getName(), staffName, reason + " (IP Kick)").thenAccept(success -> {
                    if (success) {
                        String message = "&aIP kicked " + player.getName() + " (" + targetIP + ") for: &e" + reason;
                        if (silent) {
                            message += " &7(Silent)";
                        }
                        sender.sendMessage(MessageUtils.colorize(message));

                        plugin.getLogger().info(staffName + " IP kicked " + player.getName() +
                                " (" + targetIP + ") for: " + reason + (silent ? " (Silent)" : ""));
                    }
                });
            }

            sender.sendMessage(MessageUtils.colorize("&aKicked " + playersToKick.size() +
                    " player(s) with IP: " + targetIP));
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length > 1) {
            String lastArg = args[args.length - 1];

            if (args.length > 2 && args[args.length - 2].equals("-t")) {
                templateService.getAllTemplates().forEach(template -> {
                    if (template.getName().toLowerCase().startsWith(lastArg.toLowerCase())) {
                        completions.add(template.getName());
                    }
                });
            } else {
                if (!Arrays.asList(args).contains("-s")) {
                    completions.add("-s");
                }
                if (!Arrays.asList(args).contains("-t")) {
                    completions.add("-t");
                }
                if (!Arrays.asList(args).contains("--ip") && sender.hasPermission("weguardian.ipkick")) {
                    completions.add("--ip");
                }

                completions.addAll(Arrays.asList(
                        "Inappropriate behavior", "Spam", "Disrespect", "AFK", "Lag",
                        "Rule violation", "Trolling", "Reconnect"
                ));
            }
        }

        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
