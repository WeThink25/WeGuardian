package me.wethink.weGuardian.listeners;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.database.DatabaseManager;
import me.wethink.weGuardian.models.PunishmentTemplate;
import me.wethink.weGuardian.services.TemplateService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AsyncSuggestionsListener implements Listener {

    private final WeGuardian plugin;
    private final DatabaseManager databaseManager;
    private final TemplateService templateService;

    private final Map<String, List<String>> templateSuggestionsCache = new ConcurrentHashMap<>();
    private long lastTemplateCacheUpdate = 0;
    private static final long TEMPLATE_CACHE_DURATION = 60000;

    public AsyncSuggestionsListener(WeGuardian plugin, DatabaseManager databaseManager, TemplateService templateService) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.templateService = templateService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabComplete(TabCompleteEvent event) {
        String buffer = event.getBuffer().trim();
        String[] args = buffer.split("\\s+");

        if (args.length < 2) return;

        String rawCommand = args[0].substring(1).toLowerCase();
        String commandName = rawCommand.contains(":") ? rawCommand.split(":")[1] : rawCommand;
        String currentArg = args[args.length - 1];

        if (isWeGuardianCommand(commandName)) {
            event.getCompletions().clear();
            List<String> suggestions = generateSuggestions(event.getSender(), commandName, args, currentArg);
            if (!suggestions.isEmpty()) {
                event.getCompletions().addAll(suggestions);
            }
        }
    }

    private boolean isWeGuardianCommand(String commandName) {
        return Arrays.asList(
                "ban", "tempban", "mute", "tempmute", "kick", "warn",
                "unban", "unmute", "ipban", "ipmute", "history", "checkban",
                "banlist", "mutelist", "blame", "alts", "warns", "unwarn",
                "banmenu", "tempbanmenu", "mutemenu", "tempmutemenu",
                "kickmenu", "warnmenu", "notesmenu", "unbanmenu", "unmutemenu",
                "punish", "weguardian"
        ).contains(commandName);
    }

    private List<String> generateSuggestions(CommandSender sender, String commandName, String[] args, String currentArg) {
        List<String> suggestions = new ArrayList<>();

        switch (commandName) {
            case "ban", "tempban", "mute", "tempmute", "kick", "warn", "unban", "unmute",
                 "history", "checkban", "blame", "alts", "warns", "unwarn", "punish" -> {
                if (args.length == 2) suggestions.addAll(getPlayerSuggestions(currentArg));
                else if (args.length > 2) suggestions.addAll(getAdvancedSuggestions(sender, commandName, args, currentArg));
            }
            case "ipban", "ipmute" -> {
                if (args.length == 2) suggestions.addAll(getIPSuggestions(currentArg));
                else if (args.length > 2) suggestions.addAll(getReasonSuggestions(currentArg));
            }
            case "banlist", "mutelist" -> {
                if (args.length == 2) suggestions.addAll(getPaginationSuggestions(currentArg));
            }
            case "weguardian" -> {
                if (args.length == 2) suggestions.addAll(getMainCommandSuggestions(currentArg));
            }
        }

        return suggestions.stream()
                .filter(suggestion -> suggestion.toLowerCase().startsWith(currentArg.toLowerCase()))
                .limit(20)
                .collect(Collectors.toList());
    }

    private List<String> getPlayerSuggestions(String arg) {
        String lowerArg = arg.toLowerCase();
        List<String> suggestions = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase().startsWith(lowerArg)) suggestions.add(player.getName());
        }
        return suggestions;
    }

    private List<String> getAdvancedSuggestions(CommandSender sender, String commandName, String[] args, String currentArg) {
        List<String> suggestions = new ArrayList<>();

        if (args.length > 3 && args[args.length - 2].equals("-t")) return getTemplateSuggestions(currentArg);
        if (args.length > 3 && args[args.length - 2].equals("-d")) return getDurationSuggestions();

        if (!Arrays.asList(args).contains("-s")) suggestions.add("-s");
        if (!Arrays.asList(args).contains("-t")) suggestions.add("-t");
        if (!Arrays.asList(args).contains("-d") && (commandName.equals("ban") || commandName.equals("mute"))) suggestions.add("-d");
        if (!Arrays.asList(args).contains("--ip") && hasIPPermission(sender, commandName)) suggestions.add("--ip");

        suggestions.addAll(getReasonSuggestions(currentArg));

        return suggestions;
    }

    private List<String> getTemplateSuggestions(String currentArg) {
        if (System.currentTimeMillis() - lastTemplateCacheUpdate > TEMPLATE_CACHE_DURATION) updateTemplateCache();
        return templateSuggestionsCache.getOrDefault("all", new ArrayList<>()).stream()
                .filter(template -> template.toLowerCase().startsWith(currentArg.toLowerCase()))
                .collect(Collectors.toList());
    }

    private void updateTemplateCache() {
        try {
            Collection<PunishmentTemplate> templates = templateService.getAllTemplates();
            List<String> templateNames = templates.stream()
                    .map(PunishmentTemplate::getName)
                    .toList();
            templateSuggestionsCache.put("all", templateNames);
            lastTemplateCacheUpdate = System.currentTimeMillis();
        } catch (Exception ignored) {}
    }

    private List<String> getDurationSuggestions() {
        return Arrays.asList("1h", "2h", "6h", "12h", "1d", "2d", "3d", "7d", "14d", "30d", "1w", "2w", "1m", "3m", "6m", "1y");
    }

    private List<String> getReasonSuggestions(String currentArg) {
        return Arrays.asList(
                "Hacking", "Griefing", "Toxic behavior", "Spam", "Advertising",
                "Inappropriate language", "Cheating", "Rule violation", "X-ray",
                "Duping", "Exploiting", "Alt account", "Disrespect", "Harassment",
                "Chat abuse", "Excessive caps", "Flooding", "Political discussion"
        );
    }

    private List<String> getIPSuggestions(String currentArg) {
        if (currentArg.matches("^\\d{1,3}$")) return Arrays.asList(currentArg + ".", currentArg + "0", currentArg + "1", currentArg + "2");
        if (currentArg.matches("^\\d{1,3}\\.\\d{1,3}$")) return Arrays.asList(currentArg + ".", currentArg + "0", currentArg + "1", currentArg + "2");
        if (currentArg.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) return Arrays.asList(currentArg + ".", currentArg + "0", currentArg + "1", currentArg + "2");
        return new ArrayList<>();
    }

    private List<String> getPaginationSuggestions(String currentArg) {
        return Arrays.asList("1", "2", "3", "4", "5", "10", "20", "50", "100");
    }

    private List<String> getMainCommandSuggestions(String currentArg) {
        return Arrays.asList("reload", "about", "version", "help");
    }

    private boolean hasIPPermission(CommandSender sender, String commandName) {
        return switch (commandName) {
            case "mute", "tempmute" -> sender.hasPermission("weguardian.ipmute");
            case "kick" -> sender.hasPermission("weguardian.ipkick");
            default -> false;
        };
    }

    public void clearCache() {
        templateSuggestionsCache.clear();
        lastTemplateCacheUpdate = 0;
    }
}
