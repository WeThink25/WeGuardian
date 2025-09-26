package me.wethink.weGuardian.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.models.Punishment;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StatsApiServlet extends HttpServlet {
    
    private final WeGuardian plugin;
    private final SessionManager sessionManager;
    private final Gson gson = new GsonBuilder()
    .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> 
        context.serialize(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
    .create();    
    public StatsApiServlet(WeGuardian plugin, SessionManager sessionManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String sessionId = getSessionIdFromCookies(request);
        
        if (sessionId == null || !sessionManager.isValidSession(sessionId)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        response.setContentType("application/json");
        
        try {
            JsonObject stats = new JsonObject();
            
            CompletableFuture<List<Punishment>> future = plugin.getDatabaseManager().getAllActivePunishments();
            List<Punishment> activePunishments = future.join();
            if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                plugin.getLogger().info("Found " + activePunishments.size() + " active punishments");
                for (Punishment p : activePunishments) {
                    plugin.getLogger().info("Punishment: " + p.getTargetName() + " - " + p.getType() + " - Active: " + p.isActive());
                }
            }
            int activeBans = (int) activePunishments.stream()
                .filter(p -> p.getType().toString().contains("BAN"))
                .count();
            
            int activeMutes = (int) activePunishments.stream()
                .filter(p -> p.getType().toString().contains("MUTE"))
                .count();
            
            stats.addProperty("active_bans", activeBans);
            stats.addProperty("active_mutes", activeMutes);
            stats.addProperty("total_punishments", activePunishments.size());
            stats.addProperty("online_players", plugin.getServer().getOnlinePlayers().size());
            stats.addProperty("server_name", plugin.getConfig().getString("server_name", "Unknown"));
            
            PrintWriter out = response.getWriter();
            out.print(gson.toJson(stats));
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error getting stats: " + e.getMessage());
            
            JsonObject error = new JsonObject();
            error.addProperty("error", "Failed to retrieve stats");
            
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            PrintWriter out = response.getWriter();
            out.print(gson.toJson(error));
        }
    }
    
    private String getSessionIdFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("sessionId".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
