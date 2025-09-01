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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class BanCommand implements CommandExecutor, TabCompleter {

    private final WeGuardian plugin;
    private final PunishmentService punishmentService;
    private final TemplateService templateService;

    public BanCommand(WeGuardian plugin, PunishmentService punishmentService) {
        this.plugin = plugin;
        this.punishmentService = punishmentService;
        this.templateService = plugin.getTemplateService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("weguardian.ban")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtils.colorize("&cUsage: /ban <player> <reason> [-s] [-t template] [-d duration]"));
            sender.sendMessage(MessageUtils.colorize("&7Options:"));
            sender.sendMessage(MessageUtils.colorize("&7  -s: Silent ban (no broadcast)"));
            sender.sendMessage(MessageUtils.colorize("&7  -t <template>: Use punishment template"));
            sender.sendMessage(MessageUtils.colorize("&7  -d <duration>: Specify ban duration (e.g., 1h, 1d, 1w, 1m, 1y)"));
            return true;
        }

        String targetName = args[0];

        final boolean silent;
        final String templateName;
        final String duration;
        List<String> reasonParts = new ArrayList<>();

        boolean tempSilent = false;
        String tempTemplateName = null;
        String tempDuration = null;

        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-s")) {
                tempSilent = true;
            } else if (arg.equals("-t") && i + 1 < args.length) {
                tempTemplateName = args[i + 1];
                i++;
            } else if (arg.equals("-d") && i + 1 < args.length) {
                tempDuration = args[i + 1];
                i++;
            } else {
                reasonParts.add(arg);
            }
        }

        silent = tempSilent;
        templateName = tempTemplateName;
        duration = tempDuration;

        if (reasonParts.isEmpty()) {
            sender.sendMessage(MessageUtils.colorize("&cYou must provide a reason for the ban."));
            return true;
        }

        String reason = String.join(" ", reasonParts);

        punishmentService.getPlayerUUID(targetName).thenCompose(uuid -> {
            if (uuid == null) {
                sender.sendMessage(MessageUtils.colorize("&cPlayer '" + targetName + "' not found."));
                return CompletableFuture.completedFuture(false);
            }

            return plugin.getDatabaseManager().isPlayerBanned(uuid).thenCompose(isBanned -> {
                if (isBanned) {
                    sender.sendMessage(MessageUtils.colorize("&cPlayer " + targetName + " is already banned."));
                    return CompletableFuture.completedFuture(false);
                }

                return plugin.getPunishmentService().canPunish(sender, uuid).thenCompose(canPunish -> {
                    if (!canPunish) {
                        sender.sendMessage(MessageUtils.colorize("&cYou cannot punish this player."));
                        return CompletableFuture.completedFuture(false);
                    }

                    if (templateName != null) {
                        Map<String, String> context = new HashMap<>();
                        context.put("duration", duration);

                        return plugin.getTemplateService().executePunishmentFromTemplate(uuid, targetName, sender.getName(), templateName, context)
                                .thenApply(success -> {
                                    if (success) {
                                        if (!silent) {
                                            sender.sendMessage(MessageUtils.colorize("&aSuccessfully banned " + targetName + " using template " + templateName));
                                        }
                                    } else {
                                        sender.sendMessage(MessageUtils.colorize("&cFailed to ban " + targetName + " using template " + templateName));
                                    }
                                    return success;
                                });
                    } else {
                        return punishmentService.ban(targetName, sender.getName(), reason)
                                .thenApply(success -> {
                                    if (success) {
                                        if (!silent) {
                                            sender.sendMessage(MessageUtils.colorize("&aSuccessfully banned " + targetName + " for: " + reason));
                                        }
                                    } else {
                                        sender.sendMessage(MessageUtils.colorize("&cFailed to ban " + targetName + " for: " + reason));
                                    }
                                    return success;
                                });
                    }
                });
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error processing ban command: " + throwable.getMessage());
            sender.sendMessage(MessageUtils.colorize("&cAn error occurred while processing the ban command."));
            return false;
        });

        return true;
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
            } else if (args.length > 2 && args[args.length - 2].equals("-d")) {
                completions.addAll(Arrays.asList("1h", "1d", "1w", "1m", "1y"));
            } else {
                if (!Arrays.asList(args).contains("-s")) {
                    completions.add("-s");
                }
                if (!Arrays.asList(args).contains("-t")) {
                    completions.add("-t");
                }
                if (!Arrays.asList(args).contains("-d")) {
                    completions.add("-d");
                }

                completions.addAll(Arrays.asList(
                        "Hacking", "Griefing", "Toxic behavior", "Spam", "Advertising",
                        "Inappropriate language", "Cheating", "Rule violation"
                ));
            }
        }

        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
