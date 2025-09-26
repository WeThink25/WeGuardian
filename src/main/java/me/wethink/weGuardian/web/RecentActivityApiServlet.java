package me.wethink.weGuardian.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.models.Punishment;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RecentActivityApiServlet extends HttpServlet {
    
    private final WeGuardian plugin;
    private final SessionManager sessionManager;
    private final Gson gson = new GsonBuilder()
        .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> 
            context.serialize(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
        .create();
    
    public RecentActivityApiServlet(WeGuardian plugin, SessionManager sessionManager) {
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
            CompletableFuture<List<Punishment>> future = plugin.getDatabaseManager().getAllActivePunishments();
            List<Punishment> punishments = future.join();
            
            JsonArray activities = new JsonArray();
            
            punishments.stream()
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .limit(10)
                .forEach(punishment -> {
                    JsonObject activity = new JsonObject();
                    activity.addProperty("type", punishment.getType().toString());
                    activity.addProperty("targetName", punishment.getTargetName());
                    activity.addProperty("staffName", punishment.getStaffName());
                    activity.addProperty("reason", punishment.getReason());
                    activity.addProperty("createdAt", punishment.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    activities.add(activity);
                });
            
            JsonObject result = new JsonObject();
            result.add("activities", activities);
            result.addProperty("success", true);
            
            PrintWriter out = response.getWriter();
            out.print(gson.toJson(result));
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error getting recent activity: " + e.getMessage());
            
            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("error", "Failed to retrieve recent activity");
            
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