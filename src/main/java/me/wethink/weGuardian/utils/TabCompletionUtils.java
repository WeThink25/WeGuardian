package me.wethink.weGuardian.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TabCompletionUtils {

    public static List<String> getPlayerNameCompletions(String input, int maxResults) {
        if (input == null || input.isEmpty()) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .limit(maxResults)
                    .collect(Collectors.toList());
        }

        String lowerInput = input.toLowerCase();
        List<String> exactMatches = new ArrayList<>();
        List<String> prefixMatches = new ArrayList<>();
        List<String> containsMatches = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            String playerName = player.getName();
            String lowerPlayerName = playerName.toLowerCase();

            if (lowerPlayerName.equals(lowerInput)) {
                exactMatches.add(playerName);
            } else if (lowerPlayerName.startsWith(lowerInput)) {
                prefixMatches.add(playerName);
            } else if (lowerPlayerName.contains(lowerInput)) {
                containsMatches.add(playerName);
            }
        }

        prefixMatches.sort(String.CASE_INSENSITIVE_ORDER);
        containsMatches.sort(Comparator.comparingInt(name -> name.toLowerCase().indexOf(lowerInput)));

        List<String> result = new ArrayList<>(exactMatches);
        result.addAll(prefixMatches);
        result.addAll(containsMatches);

        return result.stream().limit(maxResults).collect(Collectors.toList());
    }

    public static List<String> getPlayerNameCompletions(String input) {
        return getPlayerNameCompletions(input, 20);
    }
}