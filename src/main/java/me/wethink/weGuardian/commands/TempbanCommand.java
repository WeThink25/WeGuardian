package me.wethink.weGuardian.commands;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.services.PunishmentService;
import me.wethink.weGuardian.services.TemplateService;
import me.wethink.weGuardian.utils.MessageUtils;
import me.wethink.weGuardian.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TempbanCommand implements CommandExecutor, TabCompleter {

    private final WeGuardian plugin;
    private final PunishmentService punishmentService;
    private final TemplateService templateService;

    public TempbanCommand(WeGuardian plugin, PunishmentService punishmentService) {
        this.plugin = plugin;
        this.punishmentService = punishmentService;
        this.templateService = plugin.getTemplateService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("weguardian.tempban")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(MessageUtils.colorize("&cUsage: /tempban <player> <duration> <reason> [-s] [-t template]"));
            sender.sendMessage(MessageUtils.colorize("&7Examples:"));
            sender.sendMessage(MessageUtils.colorize("&7  /tempban Player123 1d Griefing"));
            sender.sendMessage(MessageUtils.colorize("&7  /tempban Player123 2h Spam -s"));
            sender.sendMessage(MessageUtils.colorize("&7  /tempban Player123 1w Hacking -t cheating"));
            sender.sendMessage(MessageUtils.colorize("&7Options:"));
            sender.sendMessage(MessageUtils.colorize("&7  -s: Silent tempban (no broadcast)"));
            sender.sendMessage(MessageUtils.colorize("&7  -t <template>: Use punishment template"));
            return true;
        }

        String targetName = args[0];
        String duration = args[1];
        boolean silent = Arrays.asList(args).contains("-s");
        String template = null;

        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("-t") && i + 1 < args.length) {
                template = args[i + 1];
                break;
            }
        }
        final String finalTemplate = template;

        List<String> reasonParts = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-s")) continue;
            if (arg.equals("-t")) {
                i++;
                continue;
            }
            reasonParts.add(arg);
        }

        if (reasonParts.isEmpty()) {
            sender.sendMessage(MessageUtils.colorize("&cYou must provide a reason for the tempban."));
            return true;
        }

        String reason = String.join(" ", reasonParts);

        if (!TimeUtils.isValidTimeFormat(duration)) {
            sender.sendMessage(MessageUtils.colorize("&cInvalid time format. Use formats like: 1h, 2d, 1w, 1m, 1y"));
            return true;
        }

        punishmentService.getPlayerUUID(targetName).thenCompose(uuid -> {
            if (uuid == null) {
                sender.sendMessage(MessageUtils.colorize("&cPlayer '" + targetName + "' not found."));
                return CompletableFuture.completedFuture(Boolean.FALSE);
            }

            return plugin.getDatabaseManager().isPlayerBanned(uuid).thenCompose(isBanned -> {
                if (isBanned) {
                    sender.sendMessage(MessageUtils.colorize("&cPlayer " + targetName + " is already banned."));
                    return CompletableFuture.completedFuture(Boolean.FALSE);
                }

                return plugin.getPunishmentService().canPunish(sender, uuid).thenCompose(canPunish -> {
                    if (!canPunish) {
                        sender.sendMessage(MessageUtils.colorize("&cYou cannot punish this player."));
                        return CompletableFuture.completedFuture(Boolean.FALSE);
                    }

                    String staffName = sender.getName();

                    if (finalTemplate != null && templateService.hasTemplate(finalTemplate)) {
                        return templateService.executePunishmentFromTemplate(uuid, targetName, staffName, finalTemplate, duration)
                                .thenApply(success -> {
                                    if (success) {
                                        sender.sendMessage(MessageUtils.colorize("&aSuccessfully applied template '" + finalTemplate +
                                                "' punishment to " + targetName + " for " + duration));
                                    } else {
                                        sender.sendMessage(MessageUtils.colorize("&cFailed to apply template punishment."));
                                    }
                                    return success;
                                });
                    } else {
                        return punishmentService.tempban(targetName, staffName, reason, duration).thenApply(success -> {
                            if (success) {
                                String message = "&aSuccessfully tempbanned " + targetName + " for " + duration + ": &e" + reason;
                                if (silent) {
                                    message += " &7(Silent)";
                                }
                                sender.sendMessage(MessageUtils.colorize(message));

                                plugin.getLogger().info(staffName + " tempbanned " + targetName + " for " + duration +
                                        ": " + reason + (silent ? " (Silent)" : ""));
                            } else {
                                sender.sendMessage(MessageUtils.colorize("&cFailed to tempban " + targetName +
                                        ". Player may not exist or is already banned."));
                            }
                            return success;
                        });
                    }
                });
            });
        }).exceptionally(throwable -> {
            sender.sendMessage(MessageUtils.colorize("&cError processing tempban command: " + throwable.getMessage()));
            plugin.getLogger().severe("Error in tempban command: " + throwable.getMessage());
            return Boolean.FALSE;
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
        } else if (args.length == 2) {
            completions.addAll(Arrays.asList("1h", "2h", "6h", "12h", "1d", "2d", "3d", "7d", "14d", "30d", "1w", "2w", "1m", "3m", "6m", "1y"));
        } else if (args.length > 2) {
            String lastArg = args[args.length - 1];

            if (args.length > 3 && args[args.length - 2].equals("-t")) {
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

                completions.addAll(Arrays.asList(
                        "Hacking", "Griefing", "Toxic behavior", "Spam", "Advertising",
                        "Inappropriate language", "Cheating", "Rule violation", "X-ray",
                        "Duping", "Exploiting", "Alt account"
                ));
            }
        }

        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
