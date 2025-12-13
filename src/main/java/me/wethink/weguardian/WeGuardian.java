package me.wethink.weguardian;

import co.aikar.commands.PaperCommandManager;
import fr.mrmicky.fastinv.FastInvManager;
import me.wethink.weguardian.cache.CacheManager;
import me.wethink.weguardian.commands.PunishmentCommands;
import me.wethink.weguardian.database.DatabaseManager;
import me.wethink.weguardian.database.PunishmentDAO;
import me.wethink.weguardian.listeners.PlayerChatListener;
import me.wethink.weguardian.listeners.PlayerLoginListener;
import me.wethink.weguardian.manager.PunishmentManager;
import me.wethink.weguardian.scheduler.SchedulerManager;
import me.wethink.weguardian.web.WebServer;
import me.wethink.weguardian.webhook.DiscordWebhookManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;


public final class WeGuardian extends JavaPlugin {

    private static final int BSTATS_PLUGIN_ID = 27046;

    private static WeGuardian instance;

    private SchedulerManager schedulerManager;
    private DatabaseManager databaseManager;
    private CacheManager cacheManager;
    private PunishmentManager punishmentManager;
    private PunishmentDAO punishmentDAO;
    private DiscordWebhookManager webhookManager;
    private WebServer webServer;

    private PaperCommandManager commandManager;

    private Metrics metrics;

    @Override
    public void onEnable() {
        instance = this;
        long startTime = System.currentTimeMillis();

        printBanner();

        saveDefaultConfig();

        FastInvManager.register(this);

        me.wethink.weguardian.gui.PunishmentGUI.initializeIcons();
        me.wethink.weguardian.gui.DurationGUI.initializeIcons();
        me.wethink.weguardian.gui.HistoryGUI.initializeIcons();
        getLogger().info("GUI icons pre-cached");

        getLogger().info("Initializing managers...");

        this.schedulerManager = new SchedulerManager(this);
        getLogger().info("Scheduler initialized" + (schedulerManager.isFolia() ? " (Folia mode)" : " (Bukkit mode)"));

        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.initialize();
        getLogger().info("Database initialized");

        this.punishmentDAO = new PunishmentDAO(this, databaseManager);

        this.cacheManager = new CacheManager(this);
        getLogger().info("Cache initialized");

        this.punishmentManager = new PunishmentManager(this, punishmentDAO, cacheManager);
        getLogger().info("Punishment manager initialized");

        this.webhookManager = new DiscordWebhookManager(this);
        getLogger().info("Discord webhook manager initialized");

        registerCommands();
        getLogger().info("Commands registered");

        registerListeners();
        getLogger().info("Listeners registered");

        initMetrics();

        this.webServer = new WebServer(this);
        this.webServer.start();

        long loadTime = System.currentTimeMillis() - startTime;
        getLogger().info("");
        getLogger().info("WeGuardian v" + getPluginMeta().getVersion() + " enabled! (" + loadTime + "ms)");
        getLogger().info("");
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down WeGuardian...");

        if (webServer != null) {
            webServer.stop();
        }

        if (schedulerManager != null) {
            schedulerManager.shutdown();
        }

        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        if (cacheManager != null) {
            cacheManager.clear();
        }

        getLogger().info("WeGuardian disabled. Goodbye!");
    }


    private void initMetrics() {
        try {
            this.metrics = new Metrics(this, BSTATS_PLUGIN_ID);
            getLogger().info("bStats metrics initialized (ID: " + BSTATS_PLUGIN_ID + ")");
        } catch (Exception e) {
            getLogger().warning("Failed to initialize bStats: " + e.getMessage());
        }
    }

    private void printBanner() {
        String[] banner = {
                "",
                "&8╔════════════════════════════════════════════════════════════════════════════════╗",
                "&8║                                                                                ║",
                "&8║  &c█&4█&c█   &c█&4█&c█ &6███████ &e█████&6█ &e█&6█   &e█&6█  &c█████&4█  &c█████&4█  &6█████&e█  &e█  &c█████&4█ &6█&e█   &6█&e█ &8║",
                "&8║  &c█&4█&c█   &c█&4█&c█ &6█       &e█      &e█&6█   &e█&6█ &c█    &4█&c█ &c█    &4█&c█ &6█    &e█  &e█  &c█    &4█&c█ &6█&e██  &6█&e█ &8║",
                "&8║  &c█&4█&c█ &c█ &c█&4█&c█ &6█████   &e█  ███ &e█&6█   &e█&6█ &c█████&4█&c█ &c█████&4█&c█ &6█    &e█  &e█  &c█████&4█&c█ &6█&e█ &6█ &6█&e█ &8║",
                "&8║  &c█&4█&c█ &c█ &c█&4█&c█ &6█       &e█    █ &e█&6█   &e█&6█ &c█    &4█&c█ &c█   &4█&c█  &6█    &e█  &e█  &c█    &4█&c█ &6█&e█  &6██&e█ &8║",
                "&8║  &4 ███&c█&4███  &6███████ &e██████  &e█&6█████&e█ &4█    &c█&4█ &4█    &c█&4█ &6█████&e█  &e█  &4█    &c█&4█ &6█&e█   &6█&e█ &8║",
                "&8║                                                                                ║",
                "&8╠════════════════════════════════════════════════════════════════════════════════╣",
                "&8║                                                                                ║",
                "&8║                &c⚔ &6Professional Punishment System &4v" + getPluginMeta().getVersion()
                        + " &c⚔                    &8║",
                "&8║                             &7by &e✦ WeThink ✦                                  &8║",
                "&8║                                                                                ║",
                "&8╠════════════════════════════════════════════════════════════════════════════════╣",
                "&8║    &a✓ &7Database   &8│   &a✓ &7Cache   &8│   &a✓ &7Discord   &8│   &a✓ &7Web Dashboard      &8║",
                "&8╚════════════════════════════════════════════════════════════════════════════════╝",
                ""
        };

        for (String line : banner) {
            getServer().getConsoleSender().sendMessage(colorize(line));
        }
    }


    private String colorize(String message) {
        java.util.regex.Pattern hexPattern = java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})");
        java.util.regex.Matcher matcher = hexPattern.matcher(message);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append("§").append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);

        return buffer.toString().replace("&", "§");
    }

    private void registerCommands() {
        this.commandManager = new PaperCommandManager(this);


        try {
            commandManager.enableUnstableAPI("help");
        } catch (Exception ignored) {
        }

        commandManager.getCommandCompletions().registerAsyncCompletion("players", c -> {
            return getServer().getOnlinePlayers().stream()
                    .map(p -> p.getName())
                    .toList();
        });

        commandManager.registerCommand(new PunishmentCommands(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerLoginListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerChatListener(this), this);
    }



    public static WeGuardian getInstance() {
        return instance;
    }

    public SchedulerManager getSchedulerManager() {
        return schedulerManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public PunishmentDAO getPunishmentDAO() {
        return punishmentDAO;
    }

    public PaperCommandManager getCommandManager() {
        return commandManager;
    }

    public DiscordWebhookManager getWebhookManager() {
        return webhookManager;
    }

    public WebServer getWebServer() {
        return webServer;
    }
}
