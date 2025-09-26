package me.wethink.weGuardian.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.models.Punishment;
import jakarta.websocket.Session;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

public class WebSocketHandler {
    
    private final WeGuardian plugin;
    private final SessionManager sessionManager;
    private final Set<Session> connections = ConcurrentHashMap.newKeySet();
    private final Gson gson = new Gson();
    
    public WebSocketHandler(WeGuardian plugin, SessionManager sessionManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
    }
    
    public void addConnection(Session session) {
        connections.add(session);
        plugin.getLogger().info("WebSocket connection added. Total connections: " + connections.size());
    }
    
    public void removeConnection(Session session) {
        connections.remove(session);
        plugin.getLogger().info("WebSocket connection removed. Total connections: " + connections.size());
    }
    
    public void broadcastPunishmentUpdate(Punishment punishment, String action) {
        JsonObject message = new JsonObject();
        message.addProperty("type", "punishment_update");
        message.addProperty("action", action);
        message.add("punishment", gson.toJsonTree(punishment));
        
        broadcast(message.toString());
    }
    
    public void broadcastStats() {
        CompletableFuture.runAsync(() -> {
            try {
                JsonObject stats = new JsonObject();
                stats.addProperty("type", "stats_update");
                
                List<Punishment> activePunishments = plugin.getDatabaseManager()
                    .getActivePunishments(null).join();
                
                int activeBans = (int) activePunishments.stream()
                    .filter(p -> p.getType().toString().contains("BAN"))
                    .count();
                
                int activeMutes = (int) activePunishments.stream()
                    .filter(p -> p.getType().toString().contains("MUTE"))
                    .count();
                
                stats.addProperty("active_bans", activeBans);
                stats.addProperty("active_mutes", activeMutes);
                stats.addProperty("total_punishments", activePunishments.size());
                
                broadcast(stats.toString());
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error broadcasting stats: " + e.getMessage());
            }
        });
    }
    
    public void broadcast(String message) {
        connections.removeIf(session -> {
            try {
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(message);
                    return false;
                } else {
                    return true;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error sending WebSocket message: " + e.getMessage());
                return true;
            }
        });
    }
    
}
