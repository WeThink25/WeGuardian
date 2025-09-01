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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TempmuteCommand implements CommandExecutor, TabCompleter {

    private final WeGuardian plugin;
    private final PunishmentService punishmentService;
    private final TemplateService templateService;

    public TempmuteCommand(WeGuardian plugin, PunishmentService punishmentService) {
        this.plugin = plugin;
        this.punishmentService = punishmentService;
        this.templateService = plugin.getTemplateService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("weguardian.tempmute")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(MessageUtils.colorize("&cUsage: /tempmute <player> <duration> <reason> [-s] [-t template] [--ip]"));
            sender.sendMessage(MessageUtils.colorize("&7Examples:"));
            sender.sendMessage(MessageUtils.colorize("&7  /tempmute Player123 1h Spam"));
            sender.sendMessage(MessageUtils.colorize("&7  /tempmute Player123 2d Toxic behavior -s"));
            sender.sendMessage(MessageUtils.colorize("&7  /tempmute Player123 1w Harassment -t chat_violations"));
            sender.sendMessage(MessageUtils.colorize("&7  /tempmute Player123 1w Harassment --ip"));
            sender.sendMessage(MessageUtils.colorize("&7Options:"));
            sender.sendMessage(MessageUtils.colorize("&7  -s: Silent tempmute (no broadcast)"));
            sender.sendMessage(MessageUtils.colorize("&7  -t <template>: Use punishment template"));
            sender.sendMessage(MessageUtils.colorize("&7  --ip: Tempmute IP address instead of player"));
            return true;
        }

        String targetName = args[0];
        String duration = args[1];
        boolean silent = Arrays.asList(args).contains("-s");
        boolean ipMute = Arrays.asList(args).contains("--ip");
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
            if (arg.equals("-s") || arg.equals("--ip")) continue;
            if (arg.equals("-t")) {
                i++;
                continue;
            }
            reasonParts.add(arg);
        }

        if (reasonParts.isEmpty()) {
            sender.sendMessage(MessageUtils.colorize("&cYou must provide a reason for the tempmute."));
            return true;
        }

        String reason = String.join(" ", reasonParts);

        if (!TimeUtils.isValidTimeFormat(duration)) {
            sender.sendMessage(MessageUtils.colorize("&cInvalid time format. Use formats like: 1h, 2d, 1w, 1m, 1y"));
            return true;
        }

        if (ipMute) {
            handleIPTempmute(sender, targetName, reason, duration, silent, finalTemplate);
            return true;
        }

        punishmentService.getPlayerUUID(targetName).thenCompose(uuid -> {
            if (uuid == null) {
                sender.sendMessage(MessageUtils.colorize("&cPlayer '" + targetName + "' not found."));
                return CompletableFuture.completedFuture(Boolean.FALSE);
            }

            return plugin.getDatabaseManager().isPlayerMuted(uuid).thenCompose(isMuted -> {
                if (isMuted) {
                    sender.sendMessage(MessageUtils.colorize("&cPlayer " + targetName + " is already muted."));
                    return CompletableFuture.completedFuture(Boolean.FALSE);
                }

                return plugin.getPunishmentService().canPunish(sender, uuid).thenCompose(canPunish -> {
                    if (!canPunish) {
                        sender.sendMessage(MessageUtils.colorize("&cYou cannot tempmute this player due to permission hierarchy."));
                        return CompletableFuture.completedFuture(Boolean.FALSE);
                    }

                    String staffName = sender.getName();

                    if (finalTemplate != null && templateService.hasTemplate(finalTemplate)) {
                        Map<String, String> context = new HashMap<>();
                        context.put("duration", duration);

                        return templateService.executePunishmentFromTemplate(uuid, targetName, staffName, finalTemplate, context)
                                .thenApply(success -> {
                                    if (success) {
                                        if (!silent) {
                                            sender.sendMessage(MessageUtils.colorize("&aSuccessfully applied template '" + finalTemplate +
                                                    "' punishment to " + targetName + " for " + duration));
                                        }
                                    } else {
                                        sender.sendMessage(MessageUtils.colorize("&cFailed to apply template punishment."));
                                    }
                                    return Boolean.TRUE;
                                });
                    } else {
                        return punishmentService.tempmute(targetName, staffName, reason, duration).thenApply(success -> {
                            if (success) {
                                String message = "&aSuccessfully tempmuted " + targetName + " for " + duration + ": &e" + reason;
                                if (silent) {
                                    message += " &7(Silent)";
                                }
                                sender.sendMessage(MessageUtils.colorize(message));

                                plugin.getLogger().info(staffName + " tempmuted " + targetName + " for " + duration +
                                        ": " + reason + (silent ? " (Silent)" : ""));
                            } else {
                                sender.sendMessage(MessageUtils.colorize("&cFailed to tempmute " + targetName +
                                        ". Player may not exist or is already muted."));
                            }
                            return Boolean.TRUE;
                        });
                    }
                });
            });
        }).exceptionally(throwable -> {
            sender.sendMessage(MessageUtils.colorize("&cError processing tempmute command: " + throwable.getMessage()));
            plugin.getLogger().severe("Error in tempmute command: " + throwable.getMessage());
            return Boolean.FALSE;
        });

        return true;
    }

    private void handleIPTempmute(CommandSender sender, String targetName, String reason, String duration,
                                  boolean silent, String template) {
        if (!sender.hasPermission("weguardian.iptempmute")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use IP tempmute."));
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        String targetIP;

        if (target != null) {
            targetIP = target.getAddress().getAddress().getHostAddress();
        } else {
            if (targetName.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")) {
                targetIP = targetName;
            } else {
                sender.sendMessage(MessageUtils.colorize("&cPlayer '" + targetName + "' is not online and input is not a valid IP address."));
                return;
            }
        }

        String staffName = sender.getName();

        plugin.getDatabaseManager().isIPMuted(targetIP).thenCompose(isMuted -> {
            if (isMuted) {
                sender.sendMessage(MessageUtils.colorize("&cIP address " + targetIP + " is already muted."));
                return CompletableFuture.completedFuture(Boolean.FALSE);
            }

            if (template != null && templateService.hasTemplate(template)) {
                Map<String, String> context = new HashMap<>();
                context.put("duration", duration);

                return templateService.executePunishmentFromTemplate(null, targetIP, staffName, template, context)
                        .thenApply(success -> {
                            if (success) {
                                sender.sendMessage(MessageUtils.colorize("&aSuccessfully applied template '" + template +
                                        "' punishment to IP " + targetIP + " for " + duration));
                                notifyIPMutedPlayers(targetIP, reason, duration);
                            } else {
                                sender.sendMessage(MessageUtils.colorize("&cFailed to apply template punishment."));
                            }
                            return Boolean.TRUE;
                        });
            } else {
                return punishmentService.ipTempmute(targetIP, staffName, reason, duration).thenApply(success -> {
                    if (success) {
                        String message = "&aSuccessfully IP tempmuted " + targetIP + " for " + duration + ": &e" + reason;
                        if (silent) {
                            message += " &7(Silent)";
                        }
                        sender.sendMessage(MessageUtils.colorize(message));

                        notifyIPMutedPlayers(targetIP, reason, duration);

                        plugin.getLogger().info(staffName + " IP tempmuted " + targetIP + " for " + duration +
                                ": " + reason + (silent ? " (Silent)" : ""));
                    } else {
                        sender.sendMessage(MessageUtils.colorize("&cFailed to IP tempmute " + targetIP + "."));
                    }
                    return Boolean.TRUE;
                });
            }
        }).exceptionally(throwable -> {
            sender.sendMessage(MessageUtils.colorize("&cError processing IP tempmute: " + throwable.getMessage()));
            plugin.getLogger().severe("Error in IP tempmute command: " + throwable.getMessage());
            return Boolean.FALSE;
        });
    }

    private void notifyIPMutedPlayers(String ip, String reason, String duration) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getAddress().getAddress().getHostAddress().equals(ip)) {
                player.sendMessage(MessageUtils.colorize("&cYour IP address has been muted for " + duration + ": &e" + reason));
            }
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
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
                if (!Arrays.asList(args).contains("--ip") && sender.hasPermission("weguardian.iptempmute")) {
                    completions.add("--ip");
                }

                completions.addAll(Arrays.asList(
                        "Spam", "Inappropriate language", "Toxic behavior", "Advertising",
                        "Disrespect", "Harassment", "Chat abuse", "Rule violation",
                        "Excessive caps", "Flooding", "Political discussion"
                ));
            }
        }

        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
