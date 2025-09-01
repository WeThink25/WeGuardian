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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MuteCommand implements CommandExecutor, TabCompleter {

    private final WeGuardian plugin;
    private final PunishmentService punishmentService;
    private final TemplateService templateService;

    public MuteCommand(WeGuardian plugin, PunishmentService punishmentService) {
        this.plugin = plugin;
        this.punishmentService = punishmentService;
        this.templateService = plugin.getTemplateService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("weguardian.mute")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtils.colorize("&cUsage: /mute <player> <reason> [-s] [-t template] [-d duration] [--ip]"));
            sender.sendMessage(MessageUtils.colorize("&7Options:"));
            sender.sendMessage(MessageUtils.colorize("&7  -s: Silent mute (no broadcast)"));
            sender.sendMessage(MessageUtils.colorize("&7  -t <template>: Use punishment template"));
            sender.sendMessage(MessageUtils.colorize("&7  -d <duration>: Specify mute duration (e.g., 1h, 1d, 1w, 1m, 1y)"));
            sender.sendMessage(MessageUtils.colorize("&7  --ip: Mute IP address instead of player"));
            return true;
        }

        String targetName = args[0];
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

        String duration = null;
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("-d") && i + 1 < args.length) {
                duration = args[i + 1];
                break;
            }
        }
        final String finalDuration = duration;

        List<String> reasonParts = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-s") || arg.equals("--ip")) continue;
            if (arg.equals("-t")) {
                i++;
                continue;
            }
            if (arg.equals("-d")) {
                i++;
                continue;
            }
            reasonParts.add(arg);
        }

        if (reasonParts.isEmpty()) {
            sender.sendMessage(MessageUtils.colorize("&cYou must provide a reason for the mute."));
            return true;
        }

        String reason = String.join(" ", reasonParts);
        final String finalReason = reason;

        if (ipMute) {
            handleIPMute(sender, targetName, reason, finalTemplate, silent);
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
                        sender.sendMessage(MessageUtils.colorize("&cYou cannot mute this player due to permission hierarchy."));
                        return CompletableFuture.completedFuture(Boolean.FALSE);
                    }

                    String staffName = sender.getName();

                    if (finalTemplate != null && templateService.hasTemplate(finalTemplate)) {
                        return templateService.executePunishmentFromTemplate(uuid, targetName, staffName, finalTemplate, finalDuration)
                                .thenApply(success -> {
                                    if (success) {
                                        sender.sendMessage(MessageUtils.colorize("&aSuccessfully applied template '" + finalTemplate +
                                                "' punishment to " + targetName));
                                    } else {
                                        sender.sendMessage(MessageUtils.colorize("&cFailed to apply template punishment."));
                                    }
                                    return Boolean.TRUE;
                                });
                    } else {
                        if (finalDuration != null && !finalDuration.isEmpty()) {
                            return punishmentService.tempmute(targetName, sender.getName(), finalReason, finalDuration);
                        } else {
                            return punishmentService.mute(targetName, sender.getName(), finalReason);
                        }
                    }
                });
            });
        }).exceptionally(throwable -> {
            sender.sendMessage(MessageUtils.colorize("&cError processing mute command: " + throwable.getMessage()));
            plugin.getLogger().severe("Error in mute command: " + throwable.getMessage());
            return Boolean.FALSE;
        });

        return true;
    }

    private void handleIPMute(CommandSender sender, String targetIP, String reason, String templateName, boolean silent) {
        if (!isValidIP(targetIP)) {
            sender.sendMessage(MessageUtils.colorize("&cInvalid IP address format."));
            return;
        }

        plugin.getDatabaseManager().isIPMuted(targetIP).thenCompose(isMuted -> {
            if (isMuted) {
                sender.sendMessage(MessageUtils.colorize("&cIP address " + targetIP + " is already muted."));
                return CompletableFuture.completedFuture(false);
            }

            if (templateName != null) {
                Map<String, String> context = new HashMap<>();
                return plugin.getTemplateService().executeIPTemplate(targetIP, sender.getName(), templateName, context)
                        .thenApply(success -> {
                            if (success) {
                                if (!silent) {
                                    sender.sendMessage(MessageUtils.colorize("&aSuccessfully IP muted " + targetIP + " using template " + templateName));
                                }
                            } else {
                                sender.sendMessage(MessageUtils.colorize("&cFailed to apply template punishment."));
                            }
                            return success;
                        });
            } else {
                return plugin.getPunishmentService().ipmute(targetIP, sender.getName(), reason)
                        .thenApply(success -> {
                            if (success) {
                                if (!silent) {
                                    sender.sendMessage(MessageUtils.colorize("&aSuccessfully IP muted " + targetIP + " for: " + reason));
                                }
                            } else {
                                sender.sendMessage(MessageUtils.colorize("&cFailed to IP mute " + targetIP + "."));
                            }
                            return success;
                        });
            }
        }).exceptionally(throwable -> {
            sender.sendMessage(MessageUtils.colorize("&cError processing IP mute: " + throwable.getMessage()));
            plugin.getLogger().severe("Error in IP mute command: " + throwable.getMessage());
            return false;
        });
    }

    private boolean isValidIP(String ip) {
        return ip.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");
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
                if (!Arrays.asList(args).contains("--ip") && sender.hasPermission("weguardian.ipmute")) {
                    completions.add("--ip");
                }

                completions.addAll(Arrays.asList(
                        "Spam", "Inappropriate language", "Toxic behavior", "Advertising",
                        "Disrespect", "Harassment", "Chat abuse", "Rule violation"
                ));
            }
        }

        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
