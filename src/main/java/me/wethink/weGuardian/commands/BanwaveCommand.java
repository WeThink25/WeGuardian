package me.wethink.weGuardian.commands;

import me.wethink.weGuardian.WeGuardian;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BanwaveCommand implements CommandExecutor {

    private final WeGuardian plugin;
    private final ConcurrentHashMap<String, BanwaveEntry> queuedBans = new ConcurrentHashMap<>();

    public BanwaveCommand(WeGuardian plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("weguardian.banwave")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "execute" -> handleExecute(sender);
            case "clear" -> handleClear(sender);
            default -> showHelp(sender);
        }

        return true;
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtils.colorize("&cUsage: /banwave add <player> <reason>"));
            return;
        }

        String targetName = args[1];
        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        UUID staffUuid = null;
        String staffName = "Console";
        if (sender instanceof Player player) {
            staffUuid = player.getUniqueId();
            staffName = player.getName();
        }

        queuedBans.put(targetName.toLowerCase(), new BanwaveEntry(targetName, reason, staffUuid, staffName));
        sender.sendMessage(MessageUtils.colorize("&a✓ Added " + targetName + " to banwave queue"));
        sender.sendMessage(MessageUtils.colorize("&7  Reason: &f" + reason));
        sender.sendMessage(MessageUtils.colorize("&7  Queue size: &f" + queuedBans.size()));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(MessageUtils.colorize("&cUsage: /banwave remove <player>"));
            return;
        }

        String targetName = args[1];
        BanwaveEntry removed = queuedBans.remove(targetName.toLowerCase());

        if (removed != null) {
            sender.sendMessage(MessageUtils.colorize("&a✓ Removed " + targetName + " from banwave queue"));
            sender.sendMessage(MessageUtils.colorize("&7  Queue size: &f" + queuedBans.size()));
        } else {
            sender.sendMessage(MessageUtils.colorize("&c" + targetName + " is not in the banwave queue"));
        }
    }

    private void handleList(CommandSender sender) {
        if (queuedBans.isEmpty()) {
            sender.sendMessage(MessageUtils.colorize("&eBanwave queue is empty"));
            return;
        }

        sender.sendMessage(MessageUtils.colorize("&6&l=== Banwave Queue (" + queuedBans.size() + " players) ==="));

        int count = 1;
        for (BanwaveEntry entry : queuedBans.values()) {
            sender.sendMessage(MessageUtils.colorize("&f" + count + ". &7" + entry.targetName + " &8- &f" + entry.reason));
            sender.sendMessage(MessageUtils.colorize("&8   Staff: " + entry.staffName));
            count++;
        }

        sender.sendMessage("");
        sender.sendMessage(MessageUtils.colorize("&7Use &f/banwave execute &7to apply all bans"));
    }

    private void handleExecute(CommandSender sender) {
        if (queuedBans.isEmpty()) {
            sender.sendMessage(MessageUtils.colorize("&eBanwave queue is empty - nothing to execute"));
            return;
        }

        int queueSize = queuedBans.size();
        sender.sendMessage(MessageUtils.colorize("&6⚡ Executing banwave for " + queueSize + " players..."));

        List<String> successful = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        for (BanwaveEntry entry : queuedBans.values()) {
            plugin.getPunishmentService().banPlayer(entry.staffUuid, entry.staffName, entry.targetName, entry.reason)
                    .thenAccept(success -> {
                        if (success) {
                            successful.add(entry.targetName);
                        } else {
                            failed.add(entry.targetName);
                        }

                        if (successful.size() + failed.size() == queueSize) {
                            sender.sendMessage(MessageUtils.colorize("&6&l=== Banwave Results ==="));
                            sender.sendMessage(MessageUtils.colorize("&a✓ Successfully banned: &f" + successful.size()));
                            sender.sendMessage(MessageUtils.colorize("&c✗ Failed to ban: &f" + failed.size()));

                            if (!failed.isEmpty()) {
                                sender.sendMessage(MessageUtils.colorize("&cFailed players: &f" + String.join(", ", failed)));
                            }

                            queuedBans.clear();
                            sender.sendMessage(MessageUtils.colorize("&7Banwave queue cleared"));
                        }
                    });
        }
    }

    private void handleClear(CommandSender sender) {
        int cleared = queuedBans.size();
        queuedBans.clear();
        sender.sendMessage(MessageUtils.colorize("&a✓ Cleared banwave queue (" + cleared + " players removed)"));
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(MessageUtils.colorize("&6&l=== Banwave Commands ==="));
        sender.sendMessage(MessageUtils.colorize("&f/banwave add <player> <reason> &7- Add player to queue"));
        sender.sendMessage(MessageUtils.colorize("&f/banwave remove <player> &7- Remove player from queue"));
        sender.sendMessage(MessageUtils.colorize("&f/banwave list &7- Show queued players"));
        sender.sendMessage(MessageUtils.colorize("&f/banwave execute &7- Apply all queued bans"));
        sender.sendMessage(MessageUtils.colorize("&f/banwave clear &7- Clear the queue"));
    }



    private static class BanwaveEntry {
        final String targetName;
        final String reason;
        final UUID staffUuid;
        final String staffName;

        BanwaveEntry(String targetName, String reason, UUID staffUuid, String staffName) {
            this.targetName = targetName;
            this.reason = reason;
            this.staffUuid = staffUuid;
            this.staffName = staffName;
        }
    }
}
