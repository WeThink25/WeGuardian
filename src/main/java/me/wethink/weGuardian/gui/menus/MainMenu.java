package me.wethink.weGuardian.gui.menus;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.gui.MenuHandler;
import me.wethink.weGuardian.utils.MessageUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;


public class MainMenu extends MenuHandler {

    private final MenuManager menuManager;

    public MainMenu(WeGuardian plugin, MenuManager menuManager) {
        super(plugin, "main");
        this.menuManager = menuManager;
    }

    @Override
    protected ConfigurationSection getMenuConfig() {
        FileConfiguration config = plugin.getGUIConfigLoader().getGUIConfig("main");
        if (config == null) {
            plugin.getLogger().severe("Main menu configuration not found!");
            return null;
        }
        return config;
    }

    @Override
    protected void handleItemClick(Player staff, String targetPlayer, String itemKey, ConfigurationSection itemConfig) {
        String action = itemConfig.getString("action", "");
        
        switch (action) {
            case "open_menu":
                handleOpenMenuAction(staff, targetPlayer, itemConfig);
                break;
            case "punishment":
                handlePunishmentAction(staff, targetPlayer, itemKey, itemConfig);
                break;
            case "template":
                handleTemplateAction(staff, targetPlayer, itemKey, itemConfig);
                break;
            case "history":
                staff.sendMessage(MessageUtils.colorize("&eOpening punishment history for &a" + targetPlayer + "&e..."));
                staff.performCommand("history " + targetPlayer);
                staff.closeInventory();
                break;
            case "checkban":
                staff.sendMessage(MessageUtils.colorize("&eChecking ban status for &a" + targetPlayer + "&e..."));
                staff.performCommand("checkban " + targetPlayer);
                staff.closeInventory();
                break;
            case "notes":
                staff.sendMessage(MessageUtils.colorize("&eOpening notes for &a" + targetPlayer + "&e..."));
                menuManager.setActiveMenu(staff, menuManager.getNotesMenu());
                menuManager.getNotesMenu().openMenu(staff, targetPlayer);
                break;
            case "close":
                staff.closeInventory();
                break;
            default:
                staff.sendMessage(MessageUtils.colorize("&cUnknown action: " + action));
                break;
        }
    }

    private void handleOpenMenuAction(Player staff, String targetPlayer, ConfigurationSection itemConfig) {
        String menuType = itemConfig.getString("menu_type", "");
        
        if (menuType.isEmpty()) {
            staff.sendMessage(MessageUtils.colorize("&cInvalid menu configuration - no menu type specified"));
            return;
        }

        MenuHandler targetMenu = switch (menuType.toLowerCase()) {
            case "ban" -> menuManager.getBanMenu();
            case "tempban" -> menuManager.getTempbanMenu();
            case "mute" -> menuManager.getMuteMenu();
            case "tempmute" -> menuManager.getTempmuteMenu();
            case "kick" -> menuManager.getKickMenu();
            case "warn" -> menuManager.getWarnMenu();
            case "unban" -> menuManager.getUnbanMenu();
            case "unmute" -> menuManager.getUnmuteMenu();
            case "notes" -> menuManager.getNotesMenu();
            default -> null;
        };

        if (targetMenu != null) {
            menuManager.setActiveMenu(staff, targetMenu);
            targetMenu.openMenu(staff, targetPlayer);
        } else {
            staff.sendMessage(MessageUtils.colorize("&cUnknown menu type: " + menuType));
        }
    }

    private void handlePunishmentAction(Player staff, String targetPlayer, String itemKey, ConfigurationSection itemConfig) {
        String punishmentType = itemConfig.getString("punishment_type", "");
        String reason = itemConfig.getString("reason", "No reason specified");
        String duration = itemConfig.getString("duration", "");

        if (punishmentType.isEmpty()) {
            staff.sendMessage(MessageUtils.colorize("&cInvalid punishment configuration for " + itemKey));
            return;
        }

        staff.closeInventory();
        
        String confirmMessage = switch (punishmentType.toUpperCase()) {
            case "BAN" -> "&eExecuting permanent ban on &a" + targetPlayer + " &efor: &f" + reason;
            case "TEMPBAN" -> "&eExecuting tempban on &a" + targetPlayer + " &efor &f" + duration + " &efor: &f" + reason;
            case "MUTE" -> "&eExecuting permanent mute on &a" + targetPlayer + " &efor: &f" + reason;
            case "TEMPMUTE" -> "&eExecuting tempmute on &a" + targetPlayer + " &afor &e" + duration + " &efor: &f" + reason;
            case "KICK" -> "&eExecuting kick on &a" + targetPlayer + " &efor: &f" + reason;
            case "WARN" -> "&eExecuting warn on &a" + targetPlayer + " &efor: &f" + reason;
            case "UNBAN" -> "&eExecuting unban on &a" + targetPlayer + "&e...";
            case "UNMUTE" -> "&eExecuting unmute on &a" + targetPlayer + "&e...";
            default -> "&eExecuting punishment on &a" + targetPlayer + "&e...";
        };
        
        staff.sendMessage(MessageUtils.colorize(confirmMessage));

        executePunishment(punishmentType, targetPlayer, staff.getName(), reason, duration)
                .thenAccept(success -> {
                    if (success) {
                        sendSuccessMessage(staff, punishmentType.toLowerCase(), targetPlayer, reason, duration);
                    } else {
                        sendErrorMessage(staff, punishmentType.toLowerCase(), targetPlayer);
                    }
                })
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Error executing " + punishmentType + ": " + throwable.getMessage());
                    staff.sendMessage(MessageUtils.colorize("&cAn error occurred while executing the punishment."));
                    return null;
                });
    }

    private void handleTemplateAction(Player staff, String targetPlayer, String itemKey, ConfigurationSection itemConfig) {
        String templateName = itemConfig.getString("template", "");
        
        if (templateName.isEmpty()) {
            staff.sendMessage(MessageUtils.colorize("&cInvalid template configuration for " + itemKey));
            return;
        }

        staff.closeInventory();
        staff.sendMessage(MessageUtils.colorize("&eApplying &9" + templateName + " &etemplate to &a" + targetPlayer + "&e..."));

        plugin.getTemplateService().executeTemplate(staff, targetPlayer, templateName, null)
                .thenAccept(success -> {
                    if (success) {
                        staff.sendMessage(MessageUtils.colorize("&aSuccessfully applied &9" + templateName + " &atemplate to " + targetPlayer));
                    } else {
                        staff.sendMessage(MessageUtils.colorize("&cFailed to apply template to " + targetPlayer + ". Template may not exist or player may not be found."));
                    }
                })
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Error executing template " + templateName + ": " + throwable.getMessage());
                    staff.sendMessage(MessageUtils.colorize("&cAn error occurred while applying the template."));
                    return null;
                });
    }

    @Override
    protected void sendSuccessMessage(Player staff, String punishmentType, String targetPlayer, String reason, String duration) {
        String message = switch (punishmentType.toLowerCase()) {
            case "ban" -> "&aSuccessfully banned &f" + targetPlayer + " &afor: &e" + reason;
            case "tempban" -> "&aSuccessfully tempbanned &f" + targetPlayer + " &afor &e" + duration + " &afor: &e" + reason;
            case "mute" -> "&aSuccessfully muted &f" + targetPlayer + " &afor: &e" + reason;
            case "tempmute" -> "&aSuccessfully tempmuted &f" + targetPlayer + " &afor &e" + duration + " &afor: &e" + reason;
            case "kick" -> "&aSuccessfully kicked &f" + targetPlayer + " &afor: &e" + reason;
            case "warn" -> "&aSuccessfully warned &f" + targetPlayer + " &afor: &e" + reason;
            case "unban" -> "&aSuccessfully unbanned &f" + targetPlayer;
            case "unmute" -> "&aSuccessfully unmuted &f" + targetPlayer;
            default -> "&aSuccessfully punished &f" + targetPlayer + " &afor: &e" + reason;
        };
        staff.sendMessage(MessageUtils.colorize(message));
    }

    @Override
    protected void sendErrorMessage(Player staff, String punishmentType, String targetPlayer) {
        String message = switch (punishmentType.toLowerCase()) {
            case "ban", "tempban" -> "&cFailed to ban &f" + targetPlayer + "&c. Player may already be banned or not found.";
            case "mute", "tempmute" -> "&cFailed to mute &f" + targetPlayer + "&c. Player may already be muted or not found.";
            case "kick" -> "&cFailed to kick &f" + targetPlayer + "&c. Player may not be online.";
            case "warn" -> "&cFailed to warn &f" + targetPlayer + "&c. Player may not be found.";
            case "unban" -> "&cFailed to unban &f" + targetPlayer + "&c. Player may not be banned.";
            case "unmute" -> "&cFailed to unmute &f" + targetPlayer + "&c. Player may not be muted.";
            default -> "&cFailed to punish &f" + targetPlayer + "&c.";
        };
        staff.sendMessage(MessageUtils.colorize(message));
    }

    @Override
    protected void onMenuOpen(Player staff, String targetPlayer) {
        plugin.getLogger().info("Staff " + staff.getName() + " opened comprehensive punishment center for " + targetPlayer);
    }
}
