package me.wethink.weGuardian.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.database.DatabaseManager;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.models.PunishmentType;
import me.wethink.weGuardian.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;

public class ChatListener implements Listener {

    private final WeGuardian plugin;
    private final DatabaseManager databaseManager;

    public ChatListener(WeGuardian plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("weguardian.bypass")) {
            return;
        }

        try {
            List<Punishment> punishments = databaseManager.getActivePunishments(player.getUniqueId()).join();
            for (Punishment punishment : punishments) {
                if (punishment.isExpired()) {
                    continue;
                }

                if (punishment.getType() == PunishmentType.MUTE || punishment.getType() == PunishmentType.TEMPMUTE) {
                    event.setCancelled(true);

                    String muteMessage = getMuteMessage(punishment);
                    if (!muteMessage.isEmpty()) {
                        Component component = MessageUtils.toComponent(muteMessage);
                        player.sendMessage(component);
                    }

                    String actionBarMessage = getActionBarMessage(punishment);
                    if (!actionBarMessage.isEmpty()) {
                        Component actionBarComponent = MessageUtils.toComponent(actionBarMessage);
                        player.sendActionBar(actionBarComponent);
                    }

                    return;
                }
            }

            String playerIP = player.getAddress().getAddress().getHostAddress();
            if (databaseManager.isIPMuted(playerIP).join()) {
                event.setCancelled(true);
                player.sendMessage(MessageUtils.toComponent("&6🔇 &cYour IP address is muted!"));
                player.sendActionBar(MessageUtils.toComponent("&6🔇 IP Muted"));
                return;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error checking mute status for " + player.getName() + ": " + e.getMessage());
            event.setCancelled(true);
            player.sendMessage(Component.text("§cAn error occurred while checking your mute status. Please try again."));
        }
    }

    private String getMuteMessage(Punishment punishment) {
        String template = switch (punishment.getType()) {
            case MUTE -> plugin.getMessage("screen.mute",
                    "&6🔇 &cYou are muted!\n&7Reason: &f{reason}\n&7Staff: &f{staff}\n&7Date: &f{date}\n\n&6Appeal at: &e{appeal-url}");
            case TEMPMUTE -> plugin.getMessage("screen.tempmute",
                    "&6🔇 &eYou are temporarily muted!\n&7Reason: &f{reason}\n&7Staff: &f{staff}\n&7Expires: &f{expires}\n&7Time Left: &f{time-left}\n\n&6Appeal at: &e{appeal-url}");
            default -> "&6🔇 You are muted: {reason}";
        };
        
        String appealUrl = plugin.getAppealUrl();
        
        return MessageUtils.formatPunishmentMessage(template, punishment, appealUrl);
    }

    private String getActionBarMessage(Punishment punishment) {
        String key = "actionbar." + punishment.getType().name().toLowerCase();
        String template = plugin.getMessage(key,
                "&6🔇 Muted: &f{reason} &7| &fTime Left: {time-left}");
        return MessageUtils.formatPunishmentMessage(template, punishment);
    }
}
