package me.wethink.weGuardian;

import com.tcoded.folialib.FoliaLib;
import me.wethink.weGuardian.commands.*;
import me.wethink.weGuardian.database.DatabaseManager;
import me.wethink.weGuardian.database.HikariDatabaseManager;
import me.wethink.weGuardian.database.YamlDatabaseManager;
import me.wethink.weGuardian.gui.GUIConfigLoader;
import me.wethink.weGuardian.gui.PunishmentGUI;
import me.wethink.weGuardian.listeners.AsyncSuggestionsListener;
import me.wethink.weGuardian.listeners.ChatListener;
import me.wethink.weGuardian.listeners.PlayerListener;
import me.wethink.weGuardian.services.CrossServerSyncService;
import me.wethink.weGuardian.services.MenuValidationService;
import me.wethink.weGuardian.services.NotificationService;
import me.wethink.weGuardian.services.PunishmentService;
import me.wethink.weGuardian.services.TemplateService;
import me.wethink.weGuardian.web.WebDashboardService;
import me.wethink.weGuardian.utils.WeGuardianPlaceholderExpansion;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import me.wethink.weGuardian.services.ConsoleCommandService;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class WeGuardian extends JavaPlugin {

    private static final long CACHE_DURATION = 300000;
    private final ConcurrentHashMap<String, String> messageCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private FoliaLib foliaLib;
    private DatabaseManager databaseManager;
    private PunishmentService punishmentService;
    private NotificationService notificationService;
    private TemplateService templateService;
    private CrossServerSyncService crossServerSyncService;
    private MenuValidationService menuValidationService;
    private PunishmentGUI punishmentGUI;
    private GUIConfigLoader guiConfigLoader;
    private WebDashboardService webDashboardService;
    private ConsoleCommandService consoleCommandService;
    private FileConfiguration messagesConfig;
    private FileConfiguration guiConfig;
    private FileConfiguration reasonsConfig;
    private ScheduledExecutorService executorService;

    @Override
    public void onEnable() {
        foliaLib = new FoliaLib(this);
        boolean isFolia = foliaLib.isFolia();
        String platform = isFolia ? "Folia" : "Paper/Spigot/Bukkit";
        debug("Starting WeGuardian on " + platform + " platform");
        int threadPoolSize = isFolia ? 4 : 2;
        executorService = Executors.newScheduledThreadPool(threadPoolSize);
        if (isFolia) {
            debug("Folia detected - Using region-based scheduling optimizations");
        }
        loadConfigurations();
        saveDefaultConfig();
        initializeManagers();
        initializeDatabase();
        initializeServices();
        initializeListeners();
        registerCommands();
        registerPlaceholders();
        initializeMetrics();
        foliaLib.getScheduler().runTimerAsync(this::cleanupCache, 5 * 60 * 20, 5 * 60 * 20);
        sendStartupMessage();
    }

    @Override
    public void onDisable() {
        sendShutdownMessage();
        if (foliaLib != null) {
            foliaLib.getScheduler().cancelAllTasks();
        }
        if (punishmentService != null) {
            punishmentService.shutdown();
        }
        if (notificationService != null) {
            notificationService.shutdown();
        }
        if (crossServerSyncService != null) {
            crossServerSyncService.shutdown();
        }
        if (webDashboardService != null) {
            webDashboardService.stop();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void initializeManagers() {
    }

    private void initializeDatabase() {
        String databaseType = getConfig().getString("database.type", "yaml").toLowerCase();
        boolean useMySQL = getConfig().getBoolean("database.mysql.enabled", false);
        
        if (useMySQL && "mysql".equals(databaseType)) {
            debug("Initializing MySQL database connection...");
            this.databaseManager = new HikariDatabaseManager(this);
        } else {
            debug("Initializing YAML file storage...");
            this.databaseManager = new YamlDatabaseManager(this);
        }
        
        try {
            this.databaseManager.initialize().get();
            debug("Database initialized successfully! Type: " + (useMySQL ? "MySQL" : "YAML"));
        } catch (Exception e) {
            getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void initializeServices() {
        this.guiConfigLoader = new GUIConfigLoader(this);
        this.punishmentService = new PunishmentService(this);
        this.notificationService = new NotificationService(this);
        this.templateService = new TemplateService(this);
        this.crossServerSyncService = new CrossServerSyncService(this);
        this.menuValidationService = new MenuValidationService(this);
        this.punishmentGUI = new PunishmentGUI(this);
        this.webDashboardService = new WebDashboardService(this);
        this.consoleCommandService = new ConsoleCommandService(this);
        
        if (crossServerSyncService.isEnabled()) {
            foliaLib.getScheduler().runNextTick(task -> {
                crossServerSyncService.validateServerConnections();
            });
        }
        if (menuValidationService.isValidationEnabled()) {
            foliaLib.getScheduler().runNextTick(task -> {
                menuValidationService.validateMenuPerformance();
            });
        }
        
        if (getConfig().getBoolean("web-dashboard.enabled", false)) {
            foliaLib.getScheduler().runAsync(task -> {
                webDashboardService.start();
            });
        }
    }

    private void initializeListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this, databaseManager), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this, databaseManager), this);
        getServer().getPluginManager().registerEvents(new AsyncSuggestionsListener(this, databaseManager, templateService), this);
    }

    private void registerCommands() {
        getCommand("punish").setExecutor(new PunishCommand(this, punishmentGUI));
        getCommand("ban").setExecutor(new BanCommand(this, punishmentService));
        getCommand("tempban").setExecutor(new TempbanCommand(this, punishmentService));
        getCommand("mute").setExecutor(new MuteCommand(this, punishmentService));
        getCommand("tempmute").setExecutor(new TempmuteCommand(this, punishmentService));
        getCommand("kick").setExecutor(new KickCommand(this, punishmentService));
        getCommand("warn").setExecutor(new WarnCommand(this, punishmentService));
        getCommand("unban").setExecutor(new UnbanCommand(this, punishmentService));
        getCommand("unmute").setExecutor(new UnmuteCommand(this, punishmentService));
        getCommand("ipban").setExecutor(new IPBanCommand(this, punishmentService));
        getCommand("tempipban").setExecutor(new TempIPBanCommand(this, punishmentService));
        getCommand("ipmute").setExecutor(new IPMuteCommand(punishmentService));
        getCommand("unbanip").setExecutor(new UnbanIPCommand(this, punishmentService));
        getCommand("history").setExecutor(new HistoryCommand(this));
        getCommand("checkban").setExecutor(new CheckbanCommand(this));
        getCommand("banlist").setExecutor(new BanlistCommand(this));
        getCommand("mutelist").setExecutor(new MutelistCommand(this));
        getCommand("blame").setExecutor(new BlameCommand(this));
        getCommand("alts").setExecutor(new AltsCommand(this));
        getCommand("warns").setExecutor(new WarnsCommand(this));
        getCommand("unwarn").setExecutor(new UnwarnCommand(this));
        getCommand("banmenu").setExecutor(new BanMenuCommand(this));
        getCommand("tempbanmenu").setExecutor(new TempbanMenuCommand(this));
        getCommand("mutemenu").setExecutor(new MuteMenuCommand(this));
        getCommand("tempmutemenu").setExecutor(new TempmuteMenuCommand(this));
        getCommand("kickmenu").setExecutor(new KickMenuCommand(this));
        getCommand("warnmenu").setExecutor(new WarnMenuCommand(this));
        getCommand("notesmenu").setExecutor(new NotesMenuCommand(this));
        getCommand("unbanmenu").setExecutor(new UnbanMenuCommand(this));
        getCommand("unmutemenu").setExecutor(new UnmuteMenuCommand(this));
        getCommand("punish").setTabCompleter(new PunishCommand(this, punishmentGUI));
        getCommand("ban").setTabCompleter(new BanCommand(this, punishmentService));
        getCommand("tempban").setTabCompleter(new TempbanCommand(this, punishmentService));
        getCommand("mute").setTabCompleter(new MuteCommand(this, punishmentService));
        getCommand("tempmute").setTabCompleter(new TempmuteCommand(this, punishmentService));
        getCommand("kick").setTabCompleter(new KickCommand(this, punishmentService));
        getCommand("warn").setTabCompleter(new WarnCommand(this, punishmentService));
        getCommand("unban").setTabCompleter(new UnbanCommand(this, punishmentService));
        getCommand("unmute").setTabCompleter(new UnmuteCommand(this, punishmentService));
        getCommand("ipban").setTabCompleter(new IPBanCommand(this, punishmentService));
        getCommand("ipmute").setTabCompleter(new IPMuteCommand(punishmentService));
        getCommand("unbanip").setTabCompleter(new UnbanIPCommand(this, punishmentService));
        getCommand("history").setTabCompleter(new HistoryCommand(this));
        getCommand("checkban").setTabCompleter(new CheckbanCommand(this));
        getCommand("banlist").setTabCompleter(new BanlistCommand(this));
        getCommand("mutelist").setTabCompleter(new MutelistCommand(this));
        getCommand("blame").setTabCompleter(new BlameCommand(this));
        getCommand("alts").setTabCompleter(new AltsCommand(this));
        getCommand("warns").setTabCompleter(new WarnsCommand(this));
        getCommand("unwarn").setTabCompleter(new UnwarnCommand(this));
        getCommand("banmenu").setTabCompleter(new BanMenuCommand(this));
        getCommand("tempbanmenu").setTabCompleter(new TempbanMenuCommand(this));
        getCommand("mutemenu").setTabCompleter(new MuteMenuCommand(this));
        getCommand("tempmutemenu").setTabCompleter(new TempmuteMenuCommand(this));
        getCommand("kickmenu").setTabCompleter(new KickMenuCommand(this));
        getCommand("warnmenu").setTabCompleter(new WarnMenuCommand(this));
        getCommand("notesmenu").setTabCompleter(new NotesMenuCommand(this));
        getCommand("unbanmenu").setTabCompleter(new UnbanMenuCommand(this));
        getCommand("unmutemenu").setTabCompleter(new UnmuteMenuCommand(this));

        if (getCommand("weguardian") != null) {
            WeGuardianCommand wg = new WeGuardianCommand(this);
            getCommand("weguardian").setExecutor(wg);
            getCommand("weguardian").setTabCompleter(wg);
        }
    }

    private void registerPlaceholders() {
        if (getConfig().getBoolean("features.placeholderapi_integration", true)) {
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new WeGuardianPlaceholderExpansion(this, databaseManager).register();
                getLogger().info("PlaceholderAPI integration enabled");
            }
        }
    }

    private void initializeMetrics() {
        if (getConfig().getBoolean("features.metrics", true)) {
            Metrics metrics = new Metrics(this, 27046);
            metrics.addCustomChart(new SimplePie("database_type", () ->
                    getConfig().getString("database.type", "yaml").toLowerCase()));
            metrics.addCustomChart(new SimplePie("server_platform", () ->
                    foliaLib.isFolia() ? "Folia" : "Paper/Spigot"));
            metrics.addCustomChart(new SingleLineChart("total_punishments", () -> {
                try {
                    return databaseManager.getTotalPunishments().join();
                } catch (Exception e) {
                    return 0;
                }
            }));
            getLogger().info("bStats metrics initialized (ID: 27046)");
        } else {
            getLogger().info("Metrics disabled in config");
        }
    }

    private void sendStartupMessage() {
        String[] bannerLines = {
                "&6╔══════════════════════════════════════╗",
                "&6║          &c&lWeGuardian v" + getPluginMeta().getVersion() + "&6           ║",
                "&6║    &e⚔ Advanced Punishment System ⚔&6    ║",
                "&6╚══════════════════════════════════════╝",
                "",
                "&a✓ &7Database: &f" + getConfig().getString("database.type", "yaml").toUpperCase(),
                "&a✓ &7Platform: &f" + (foliaLib.isFolia() ? "Folia" : "Paper/Spigot/Bukkit"),
                "&a✓ &7Version: &f" + getPluginMeta().getVersion(),
                "&a✓ &7Messages: &f" + messagesConfig.getKeys(true).size() + " message keys loaded",
                "&a✓ &7GUIs: &f" + guiConfig.getKeys(false).size() + " sections, Reasons loaded with " + reasonsConfig.getKeys(false).size() + " categories",
                "&a✓ &7Services: &fInitialized",
                "",
                "&6Plugin successfully enabled!"
        };
        LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();
        for (String line : bannerLines) {
            Bukkit.getConsoleSender().sendMessage(serializer.deserialize(line));
        }
    }

    private void sendShutdownMessage() {
        LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();
        Bukkit.getConsoleSender().sendMessage(serializer.deserialize("&c⚠ WeGuardian shutting down..."));
        String[] shutdownMessage = {
                "&c╔══════════════════════════════════════╗",
                "&c║          &4&lWeGuardian v" + getPluginMeta().getVersion() + "&c           ║",
                "&c║         &6Shutting down...         &c║",
                "&c╚══════════════════════════════════════╝"
        };
        for (String line : shutdownMessage) {
            Bukkit.getConsoleSender().sendMessage(serializer.deserialize(line));
        }
    }

    private void loadConfigurations() {
        loadMessagesConfig();
        loadGuiConfigs();
    }

    private void loadMessagesConfig() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        debug("Loaded messages configuration with " + messagesConfig.getKeys(true).size() + " message keys");
    }

    private void loadGuiConfigs() {
        File guiDir = new File(getDataFolder(), "gui");
        if (!guiDir.exists()) {
            guiDir.mkdirs();
        }
        File mainGuiFile = new File(guiDir, "main.yml");
        if (!mainGuiFile.exists()) {
            saveResource("gui/main.yml", false);
        }
        guiConfig = YamlConfiguration.loadConfiguration(mainGuiFile);
        File reasonsFile = new File(guiDir, "reasons.yml");
        if (!reasonsFile.exists()) {
            YamlConfiguration reasonsConfig = new YamlConfiguration();
            reasonsConfig.set("ban.hacking", "Hacking/Cheating");
            reasonsConfig.set("ban.griefing", "Griefing");
            reasonsConfig.set("ban.spam", "Spamming");
            reasonsConfig.set("mute.toxic", "Toxic Behavior");
            reasonsConfig.set("mute.spam", "Chat Spam");
            reasonsConfig.set("kick.afk", "AFK in important area");
            reasonsConfig.set("warn.minor", "Minor Rule Violation");
            try {
                reasonsConfig.save(reasonsFile);
            } catch (Exception e) {
                getLogger().warning("Could not save reasons.yml: " + e.getMessage());
            }
        }
        reasonsConfig = YamlConfiguration.loadConfiguration(reasonsFile);
        debug("Loaded GUI configurations - Menu: " + guiConfig.getKeys(false).size() +
                " sections, Reasons loaded with " + reasonsConfig.getKeys(false).size() + " categories");
    }

    public void reloadConfigurations() {
        reloadConfig();
        loadConfigurations();
        clearMessageCache();
        debug("§aConfigurations reloaded successfully!");
    }

    public String getMessage(String path) {
        return getMessage(path, "");
    }

    public String getMessage(String path, String defaultValue) {
        if (messagesConfig == null) {
            return defaultValue;
        }
        String cacheKey = "msg:" + path;
        String cached = messageCache.get(cacheKey);
        Long timestamp = cacheTimestamps.get(cacheKey);
        if (cached != null && timestamp != null &&
                (System.currentTimeMillis() - timestamp) < CACHE_DURATION) {
            return cached;
        }
        String message = messagesConfig.getString(path, defaultValue);
        messageCache.put(cacheKey, message);
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
        return message;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public FileConfiguration getGuiConfig() {
        return guiConfig;
    }

    public FileConfiguration getReasonsConfig() {
        return reasonsConfig;
    }

    public FoliaLib getFoliaLib() {
        return foliaLib;
    }

    public TemplateService getTemplateService() {
        return templateService;
    }

    public CrossServerSyncService getCrossServerSyncService() {
        return crossServerSyncService;
    }

    public MenuValidationService getMenuValidationService() {
        return menuValidationService;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PunishmentService getPunishmentService() {
        return punishmentService;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }

    public PunishmentGUI getPunishmentGUI() {
        return punishmentGUI;
    }

    public ConsoleCommandService getConsoleCommandService() {
        return consoleCommandService;
    }

    public GUIConfigLoader getGUIConfigLoader() {
        return guiConfigLoader;
    }

    public WebDashboardService getWebDashboardService() {
        return webDashboardService;
    }

    public boolean isFolia() {
        return foliaLib.isFolia();
    }
    
    public String getPlatformName() {
        return foliaLib.isFolia() ? "Folia" : "Paper/Spigot/Bukkit";
    }

    public String getAppealUrl() {
        return getConfig().getString("appeal_url", "your-server.com/appeals");
    }

    public void debug(String message) {
        if (getConfig().getBoolean("debug.enabled", false)) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    public void debug(String message, Object... args) {
        if (getConfig().getBoolean("debug.enabled", false)) {
            getLogger().info("[DEBUG] " + String.format(message, args));
        }
    }

    private void clearMessageCache() {
        messageCache.clear();
        cacheTimestamps.clear();
    }

    private void cleanupCache() {
        long currentTime = System.currentTimeMillis();
        cacheTimestamps.entrySet().removeIf(entry -> {
            if ((currentTime - entry.getValue()) > CACHE_DURATION) {
                messageCache.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
}
