package me.wethink.weGuardian.web;

import me.wethink.weGuardian.WeGuardian;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.websocket.server.WsSci;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletException;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

public class WebDashboardService {
    
    private final WeGuardian plugin;
    private Tomcat tomcat;
    private SessionManager sessionManager;
    private WebSocketHandler webSocketHandler;
    private ScheduledExecutorService scheduler;
    
    public WebDashboardService(WeGuardian plugin) {
        this.plugin = plugin;
        this.sessionManager = new SessionManager(plugin);
        this.webSocketHandler = new WebSocketHandler(plugin, sessionManager);
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        WebSocketEndpoint.setPlugin(plugin);
        WebSocketEndpoint.setWebDashboardService(this);
    }
    
    public void start() {
        if (!plugin.getConfig().getBoolean("web-dashboard.enabled", false)) {
            plugin.getLogger().info("Web dashboard is disabled in config");
            return;
        }
        
        try {
            String host = plugin.getConfig().getString("web-dashboard.host", "0.0.0.0");
            int preferredPort = plugin.getConfig().getInt("web-dashboard.port", 8080);
            boolean autoDetectPort = plugin.getConfig().getBoolean("web-dashboard.auto-detect-port", true);
            
            String bindHost = sanitizeBindHost(host);
            if (!bindHost.equals(host)) {
                plugin.getLogger().warning("Configured host '" + host + "' is not bindable. Falling back to '" + bindHost + "'.");
            }
            int port = preferredPort;
            if (autoDetectPort) {
                int detected = findAvailablePort(bindHost, preferredPort);
                if (detected != preferredPort) {
                    plugin.getLogger().info("Port " + preferredPort + " is unavailable. Selected free port " + detected + "");
                }
                port = detected;
            }
             
             tomcat = new Tomcat();
             tomcat.setBaseDir(createTempDir().getAbsolutePath());
             Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
             connector.setPort(port);
             connector.setProperty("address", bindHost);
             tomcat.setConnector(connector);
             
             File docBase = new File("src/main/resources/web");
             if (!docBase.exists()) {
                 docBase.mkdirs();
             }
             Context context = tomcat.addContext("", docBase.getAbsolutePath());
             
             context.addServletContainerInitializer(new WsSci(), null);
             
             Tomcat.addServlet(context, "RootRedirectServlet", new RootRedirectServlet());
             context.addServletMappingDecoded("/", "RootRedirectServlet");
             Tomcat.addServlet(context, "LoginServlet", new LoginServlet(plugin, sessionManager));
             context.addServletMappingDecoded("/login", "LoginServlet");
             
             Tomcat.addServlet(context, "DashboardServlet", new DashboardServlet(plugin, sessionManager));
             context.addServletMappingDecoded("/dashboard", "DashboardServlet");
             
             Tomcat.addServlet(context, "PunishmentsServlet", new PunishmentsServlet(plugin, sessionManager));
             context.addServletMappingDecoded("/punishments", "PunishmentsServlet");
             
             Tomcat.addServlet(context, "PlayersServlet", new PlayersServlet(plugin, sessionManager));
             context.addServletMappingDecoded("/players", "PlayersServlet");
             
             Tomcat.addServlet(context, "SettingsServlet", new SettingsServlet(plugin, sessionManager));
             context.addServletMappingDecoded("/settings", "SettingsServlet");
             
             Tomcat.addServlet(context, "LoginApiServlet", new LoginApiServlet(plugin, sessionManager));
             context.addServletMappingDecoded("/api/login", "LoginApiServlet");
             
             Tomcat.addServlet(context, "LogoutApiServlet", new LogoutApiServlet(plugin, sessionManager));
             context.addServletMappingDecoded("/api/logout", "LogoutApiServlet");
             
             Tomcat.addServlet(context, "PunishmentsApiServlet", new PunishmentsApiServlet(plugin, sessionManager));
             context.addServletMappingDecoded("/api/punishments/*", "PunishmentsApiServlet");
             
             Tomcat.addServlet(context, "PlayersApiServlet", new PlayersApiServlet(plugin, sessionManager));
             context.addServletMappingDecoded("/api/players/*", "PlayersApiServlet");
             
             Tomcat.addServlet(context, "StatsApiServlet", new StatsApiServlet(plugin, sessionManager));
             context.addServletMappingDecoded("/api/stats", "StatsApiServlet");
             Tomcat.addServlet(context, "RecentActivityApiServlet", new RecentActivityApiServlet(plugin, sessionManager));
             context.addServletMappingDecoded("/api/recent-activity", "RecentActivityApiServlet");
             try {
                 tomcat.start();
                } catch (Exception e) {
                    Throwable cause = e;
                    boolean isBindException = false;
                    while (cause != null) {
                        if (cause instanceof BindException) {
                            isBindException = true;
                            break;
                        }
                        cause = cause.getCause();
                    }
                    
                    if (isBindException) {
                        if (!autoDetectPort) {
                            throw e;
                        }
                        int fallbackPort = findAvailablePort(bindHost, 0);
                        plugin.getLogger().warning("Failed to bind to port " + port + ": " + cause.getMessage() + ". Retrying on free port " + fallbackPort + "");
                        connector.setPort(fallbackPort);
                        tomcat.start();
                        port = fallbackPort;
                    } else {
                        throw e;
                    }
                }
             
             int actualPort = connector.getLocalPort() > 0 ? connector.getLocalPort() : port;
             plugin.getLogger().info("Web dashboard started on http://" + bindHost + ":" + actualPort);
             
             startScheduledTasks();
             
         } catch (Exception e) {
             plugin.getLogger().severe("Failed to start web dashboard: " + e.getMessage());
             e.printStackTrace();
         }
     }
     
     public void stop() {
         if (tomcat != null) {
             try {
                 tomcat.stop();
                 plugin.getLogger().info("Web dashboard stopped");
             } catch (Exception e) {
                 plugin.getLogger().severe("Error stopping web dashboard: " + e.getMessage());
             }
         }
         
         if (scheduler != null) {
             scheduler.shutdown();
         }
     }
     
     private void startScheduledTasks() {
         scheduler.scheduleAtFixedRate(() -> {
             sessionManager.cleanupExpiredSessions();
         }, 5, 5, TimeUnit.MINUTES);
         
         scheduler.scheduleAtFixedRate(() -> {
             webSocketHandler.broadcastStats();
         }, 30, 30, TimeUnit.SECONDS);
     }
     
     public SessionManager getSessionManager() {
         return sessionManager;
     }
     
     public WebSocketHandler getWebSocketHandler() {
         return webSocketHandler;
     }
     
     private static String sanitizeBindHost(String configuredHost) {
         String host = configuredHost == null || configuredHost.isBlank() ? "0.0.0.0" : configuredHost.trim();
         try {
             try (ServerSocket ss = new ServerSocket(0, 1, InetAddress.getByName(host))) {
                 return host;
             }
         } catch (Exception ignored) {
             return "0.0.0.0";
         }
     }
     
     private static boolean isPortFree(String host, int port) {
         try (ServerSocket ss = new ServerSocket(port, 1, InetAddress.getByName(host))) {
             ss.setReuseAddress(true);
             return true;
         } catch (Exception e) {
             return false;
         }
     }
     
     private static int findAvailablePort(String host, int preferredPort) {
         if (preferredPort > 0 && isPortFree(host, preferredPort)) {
             return preferredPort;
         }
         try (ServerSocket ss = new ServerSocket(0, 50, InetAddress.getByName(host))) {
             ss.setReuseAddress(true);
             return ss.getLocalPort();
         } catch (Exception e) {
             try (ServerSocket ss = new ServerSocket(0, 50, InetAddress.getByName("0.0.0.0"))) {
                 ss.setReuseAddress(true);
                 return ss.getLocalPort();
             } catch (Exception ex) {
                 return preferredPort > 0 ? preferredPort : 8080;
             }
         }
     }
     
     private static File createTempDir() {
         try {
             File temp = File.createTempFile("tomcat.", ".dir");
             if (!temp.delete()) throw new RuntimeException();
             if (!temp.mkdir()) throw new RuntimeException();
             temp.deleteOnExit();
             return temp;
         } catch (Exception e) {
             throw new RuntimeException("Unable to create temp dir for Tomcat", e);
         }
     }
}
