package me.wethink.weGuardian.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.models.PunishmentType;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class PunishmentsApiServlet extends HttpServlet {
    
    private final WeGuardian plugin;
    private final SessionManager sessionManager;
    private final Gson gson = new GsonBuilder()
    .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> 
        context.serialize(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
    .create();    
    public PunishmentsApiServlet(WeGuardian plugin, SessionManager sessionManager) {
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
        
        String pathInfo = request.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            handleGetActivePunishments(request, response);
        } else if (pathInfo.startsWith("/active")) {
            handleGetActivePunishments(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String sessionId = getSessionIdFromCookies(request);
        
        if (sessionId == null || !sessionManager.isValidSession(sessionId)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        String pathInfo = request.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            handleApplyPunishment(request, response);
        } else if (pathInfo.startsWith("/revoke/")) {
            String punishmentIdStr = pathInfo.substring("/revoke/".length());
            handleRevokePunishment(request, response, punishmentIdStr);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String sessionId = getSessionIdFromCookies(request);

        if (sessionId == null || !sessionManager.isValidSession(sessionId)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String pathInfo = request.getPathInfo();

        if (pathInfo != null && pathInfo.startsWith("/")) {
            String punishmentIdStr = pathInfo.substring(1);
            handleRevokePunishment(request, response, punishmentIdStr);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }
    
    private void handleGetActivePunishments(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        
        try {
            CompletableFuture<List<Punishment>> future = plugin.getDatabaseManager().getAllActivePunishments();
            List<Punishment> punishments = future.join();
            
            JsonObject result = new JsonObject();
            result.add("punishments", gson.toJsonTree(punishments));
            result.addProperty("success", true);
            
            PrintWriter out = response.getWriter();
            out.print(gson.toJson(result));
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error getting active punishments: " + e.getMessage());
            
            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("error", "Failed to retrieve punishments");
            
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            PrintWriter out = response.getWriter();
            out.print(gson.toJson(error));
        }
    }
    
    private void handleApplyPunishment(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        
        try {
            BufferedReader reader = request.getReader();
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            
            JsonObject requestData = gson.fromJson(json.toString(), JsonObject.class);
            
            String targetName = requestData.get("targetName").getAsString();
            String punishmentType = requestData.get("type").getAsString();
            String reason = requestData.get("reason").getAsString();
            String duration = requestData.has("duration") ? requestData.get("duration").getAsString() : null;
            
            SessionManager.Session session = sessionManager.getSession(getSessionIdFromCookies(request));
            String staffName = session.getUsername();
            
            PunishmentType type = PunishmentType.valueOf(punishmentType.toUpperCase());
            
            CompletableFuture<Boolean> future = plugin.getPunishmentService().executePunishment(
                type, targetName, staffName, reason, duration
            );
            
            boolean success = future.join();
            
            JsonObject result = new JsonObject();
            result.addProperty("success", success);
            
            if (success) {
                result.addProperty("message", "Punishment applied successfully");
                plugin.getWebDashboardService().getWebSocketHandler().broadcastStats();
            } else {
                result.addProperty("error", "Failed to apply punishment");
            }
            
            PrintWriter out = response.getWriter();
            out.print(gson.toJson(result));
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error applying punishment: " + e.getMessage());
            
            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("error", "Failed to apply punishment: " + e.getMessage());
            
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            PrintWriter out = response.getWriter();
            out.print(gson.toJson(error));
        }
    }
    
    private void handleRevokePunishment(HttpServletRequest request, HttpServletResponse response, String punishmentIdStr) throws IOException {
        response.setContentType("application/json");
        
        try {
            int punishmentId = Integer.parseInt(punishmentIdStr);
            
            SessionManager.Session session = sessionManager.getSession(getSessionIdFromCookies(request));
            String staffName = session.getUsername();
            
            CompletableFuture<Punishment> getPunishmentFuture = plugin.getDatabaseManager().getPunishmentById(punishmentId);
            Punishment punishment = getPunishmentFuture.join();
            
            if (punishment == null) {
                throw new IllegalArgumentException("Punishment not found");
            }
            
            CompletableFuture<Boolean> unpunishFuture = CompletableFuture.completedFuture(false);
            
            switch (punishment.getType()) {
                case BAN:
                case TEMPBAN:
                    unpunishFuture = plugin.getPunishmentService().unbanPlayer(
                        punishment.getTargetUuid(), 
                        punishment.getTargetName(), 
                        staffName
                    );
                    break;
                case MUTE:
                case TEMPMUTE:
                    unpunishFuture = plugin.getPunishmentService().unmutePlayer(
                        punishment.getTargetUuid(), 
                        punishment.getTargetName(), 
                        staffName
                    );
                    break;
                case IPBAN:
                case IPTEMPBAN:
                    String ip = punishment.getTargetIP();
                    if (ip != null && !ip.isEmpty()) {
                        unpunishFuture = plugin.getPunishmentService().unbanIP(ip, staffName);
                    }
                    break;
                case IPMUTE:
                case IPTEMPMUTE:
                    String muteIp = punishment.getTargetIP();
                    if (muteIp != null && !muteIp.isEmpty()) {
                        unpunishFuture = plugin.getPunishmentService().unmuteIP(muteIp, staffName).thenApply(v -> true);
                    }
                    break;
                default:
                    break;
            }
            
            unpunishFuture.join();
            
            CompletableFuture<Void> removeFuture = plugin.getDatabaseManager().removePunishment(
                punishmentId, UUID.randomUUID(), staffName, "Revoked via web dashboard"
            );
            
            removeFuture.join();
            
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "Punishment revoked successfully");
            
            plugin.getWebDashboardService().getWebSocketHandler().broadcastStats();
            
            PrintWriter out = response.getWriter();
            out.print(gson.toJson(result));
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error revoking punishment: " + e.getMessage() + "\n" + java.util.Arrays.toString(e.getStackTrace()));
            
            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("error", "Failed to revoke punishment: " + e.getMessage());
            error.addProperty("stackTrace", java.util.Arrays.toString(e.getStackTrace()));
            
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
