package me.wethink.weguardian.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import me.wethink.weguardian.WeGuardian;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;


public class WebServer {

    private final WeGuardian plugin;
    private final Gson gson;
    private final ExecutorService webExecutor;
    private Javalin app;
    private WebAPIController apiController;

    public WebServer(WeGuardian plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                .create();

        this.webExecutor = Executors.newFixedThreadPool(8, r -> {
            Thread t = new Thread(r, "WeGuardian-Web-Worker");
            t.setDaemon(true);
            return t;
        });
    }


    public ExecutorService getWebExecutor() {
        return webExecutor;
    }


    public void start() {
        if (!plugin.getConfig().getBoolean("web-dashboard.enabled", false)) {
            plugin.getLogger().info("Web dashboard is disabled in config.");
            return;
        }

        String host = plugin.getConfig().getString("web-dashboard.host", "0.0.0.0");
        int port = plugin.getConfig().getInt("web-dashboard.port", 8080);

        try {
            extractWebFiles();

            apiController = new WebAPIController(plugin, gson, webExecutor);

            app = Javalin.create(config -> {
                config.showJavalinBanner = false;

                File webFolder = new File(plugin.getDataFolder(), "web");
                config.staticFiles.add(staticFiles -> {
                    staticFiles.hostedPath = "/";
                    staticFiles.directory = webFolder.getAbsolutePath();
                    staticFiles.location = Location.EXTERNAL;
                });
            });

            app.before("/api/*", ctx -> {
                ctx.header("Access-Control-Allow-Origin", "*");
                ctx.header("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
                ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
            });

            app.options("/api/*", ctx -> ctx.status(200));

            registerAPIRoutes();

            app.start(host, port);

            plugin.getLogger().info("============================================");
            plugin.getLogger().info("Web Dashboard started successfully!");
            plugin.getLogger().info("Access at: http://" + (host.equals("0.0.0.0") ? "localhost" : host) + ":" + port);
            plugin.getLogger().info("============================================");

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to start web dashboard", e);
        }
    }


    private void registerAPIRoutes() {
        app.get("/api/test", apiController::testEndpoint);

        app.get("/api/config", apiController::getConfig);
        app.get("/api/lookup/{username}", apiController::lookupPlayer);
        app.get("/api/stats", apiController::getStats);
        app.get("/api/recent", apiController::getRecentPunishments);

        app.before("/api/admin/*", apiController::authenticateAdmin);
        app.get("/api/admin/punishments", apiController::getAllActivePunishments);
        app.post("/api/admin/punish", apiController::createPunishment);
        app.post("/api/admin/unpunish", apiController::removePunishment);
    }


    private void extractWebFiles() {
        File webFolder = new File(plugin.getDataFolder(), "web");
        if (!webFolder.exists()) {
            webFolder.mkdirs();
        }

        String[] files = { "index.html", "admin.html", "styles.css", "app.js" };

        for (String fileName : files) {
            File targetFile = new File(webFolder, fileName);
            try (InputStream in = plugin.getResource("web/" + fileName)) {
                if (in != null) {
                    try (OutputStream out = new FileOutputStream(targetFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = in.read(buffer)) > 0) {
                            out.write(buffer, 0, length);
                        }
                    }
                } else {
                    plugin.getLogger().warning("Missing web resource: " + fileName);
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to extract web file: " + fileName, e);
            }
        }
    }


    public void stop() {
        if (app != null) {
            try {
                app.stop();
                plugin.getLogger().info("Web dashboard stopped.");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error stopping web dashboard", e);
            }
        }

        if (webExecutor != null && !webExecutor.isShutdown()) {
            webExecutor.shutdown();
            try {
                if (!webExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    webExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                webExecutor.shutdownNow();
            }
        }
    }


    public WebAPIController getApiController() {
        return apiController;
    }
}
