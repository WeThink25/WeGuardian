package me.wethink.weguardian.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import me.wethink.weguardian.WeGuardian;
import me.wethink.weguardian.model.Punishment;
import me.wethink.weguardian.model.PunishmentType;
import me.wethink.weguardian.util.DurationParser;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;


public class WebAPIController {

    private final WeGuardian plugin;
    private final Gson gson;
    private final String adminApiKey;
    private final ExecutorService webExecutor;

    public WebAPIController(WeGuardian plugin, Gson gson, ExecutorService webExecutor) {
        this.plugin = plugin;
        this.gson = gson;
        this.adminApiKey = plugin.getConfig().getString("web-dashboard.admin-api-key", "");
        this.webExecutor = webExecutor;
    }


    private void runAsync(Context ctx, CompletableFuture<String> future) {
        ctx.future(() -> future.thenApply(result -> {
            ctx.result(result);
            return result;
        }));
    }


    public void getConfig(Context ctx) {
        Map<String, Object> config = new HashMap<>();
        config.put("serverName", plugin.getConfig().getString("web-dashboard.branding.server-name", "My Server"));
        config.put("accentColor", plugin.getConfig().getString("web-dashboard.branding.accent-color", "#ff5555"));
        config.put("footerText",
                plugin.getConfig().getString("web-dashboard.branding.footer-text", "Powered by WeGuardian"));

        ctx.contentType("application/json");
        ctx.result(gson.toJson(config));
    }


    public void lookupPlayer(Context ctx) {
        String username = ctx.pathParam("username");

        if (username == null || username.trim().isEmpty()) {
            sendError(ctx, HttpStatus.BAD_REQUEST, "Username is required");
            return;
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(username);

        if (player.getUniqueId() == null || (!player.hasPlayedBefore() && !player.isOnline())) {
            sendError(ctx, HttpStatus.NOT_FOUND, "Player not found");
            return;
        }

        UUID uuid = player.getUniqueId();
        String playerName = player.getName();

        ctx.contentType("application/json");

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                List<Punishment> punishments = plugin.getPunishmentManager().getHistory(uuid).join();

                Map<String, Object> response = new HashMap<>();
                response.put("username", playerName);
                response.put("uuid", uuid.toString());
                response.put("punishments", punishments.stream()
                        .map(this::punishmentToMap)
                        .collect(Collectors.toList()));

                List<Map<String, Object>> active = punishments.stream()
                        .filter(Punishment::isActive)
                        .filter(p -> !p.isExpired())
                        .map(this::punishmentToMap)
                        .collect(Collectors.toList());
                response.put("activePunishments", active);

                return gson.toJson(response);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to lookup player: " + e.getMessage());
                return gson.toJson(Map.of("error", true, "message", "Failed to fetch punishments"));
            }
        }, webExecutor);

        runAsync(ctx, future);
    }


    public void getStats(Context ctx) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("serverName", plugin.getConfig().getString("web-dashboard.branding.server-name", "My Server"));
        stats.put("online", Bukkit.getOnlinePlayers().size());
        stats.put("maxPlayers", Bukkit.getMaxPlayers());

        ctx.contentType("application/json");
        ctx.result(gson.toJson(stats));
    }


    public void authenticateAdmin(Context ctx) {
        if (adminApiKey.isEmpty() || adminApiKey.equals("CHANGE-ME-TO-A-SECURE-KEY")) {
            sendError(ctx, HttpStatus.SERVICE_UNAVAILABLE, "Admin API key not configured");
            return;
        }

        String authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(ctx, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
            return;
        }

        String providedKey = authHeader.substring(7);
        if (!providedKey.equals(adminApiKey)) {
            sendError(ctx, HttpStatus.FORBIDDEN, "Invalid API key");
            return;
        }
    }


    public void getRecentPunishments(Context ctx) {
        int limit = 10;
        try {
            String limitParam = ctx.queryParam("limit");
            if (limitParam != null) {
                limit = Math.min(50, Math.max(1, Integer.parseInt(limitParam)));
            }
        } catch (NumberFormatException ignored) {
        }

        int finalLimit = limit;
        ctx.contentType("application/json");

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                List<Punishment> punishments = plugin.getPunishmentDAO().getRecentPunishments(finalLimit).join();

                Map<String, Object> response = new HashMap<>();
                response.put("punishments", punishments.stream()
                        .map(this::punishmentToMap)
                        .collect(Collectors.toList()));
                response.put("count", punishments.size());
                return gson.toJson(response);
            } catch (Exception e) {
                plugin.getLogger().warning("[Web] Failed to get recent punishments: " + e.getMessage());
                return gson.toJson(
                        Map.of("error", true, "message", "Failed to fetch recent punishments: " + e.getMessage()));
            }
        }, webExecutor);

        runAsync(ctx, future);
    }


    public void getAllActivePunishments(Context ctx) {
        ctx.contentType("application/json");

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                List<Punishment> punishments = plugin.getPunishmentDAO().getRecentPunishments(50).join();
                Map<String, Object> response = new HashMap<>();
                response.put("punishments", punishments.stream()
                        .map(this::punishmentToMap)
                        .collect(Collectors.toList()));
                response.put("count", punishments.size());
                return gson.toJson(response);
            } catch (Exception e) {
                plugin.getLogger().warning("[Web] Failed to get punishments: " + e.getMessage());
                return gson.toJson(Map.of("error", true, "message", "Failed to fetch punishments"));
            }
        }, webExecutor);

        runAsync(ctx, future);
    }


    public void createPunishment(Context ctx) {
        JsonObject body;
        try {
            body = gson.fromJson(ctx.body(), JsonObject.class);
        } catch (Exception e) {
            sendError(ctx, HttpStatus.BAD_REQUEST, "Invalid request body");
            return;
        }

        String targetName = getJsonString(body, "target");
        String type = getJsonString(body, "type");
        String reason = getJsonString(body, "reason");
        String duration = getJsonString(body, "duration");
        String staffName = getJsonString(body, "staffName");

        if (targetName == null || type == null) {
            sendError(ctx, HttpStatus.BAD_REQUEST, "Missing required fields: target, type");
            return;
        }

        PunishmentType punishmentType;
        try {
            punishmentType = PunishmentType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            sendError(ctx, HttpStatus.BAD_REQUEST, "Invalid punishment type. Valid: BAN, MUTE, KICK");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sendError(ctx, HttpStatus.NOT_FOUND, "Player not found");
            return;
        }

        UUID targetUUID = target.getUniqueId();
        String finalTargetName = target.getName();
        String finalReason = reason != null ? reason : "Punished via Web Dashboard";
        String finalStaffName = staffName != null ? staffName : "WebAdmin";
        String finalDuration = duration;

        ctx.contentType("application/json");

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> response = new HashMap<>();

                if (punishmentType == PunishmentType.KICK) {
                    Boolean success = plugin.getPunishmentManager().kick(
                            targetUUID, finalTargetName, null, finalStaffName, finalReason).join();
                    plugin.getLogger().info("[Web] " + finalStaffName + " kicked " + finalTargetName
                            + " via web dashboard. Reason: " + finalReason);
                    response.put("success", success);
                    response.put("message", success ? "Player kicked successfully" : "Failed to kick player");
                } else if (finalDuration != null && !finalDuration.isEmpty()) {
                    long durationMs = DurationParser.parse(finalDuration);
                    if (durationMs <= 0) {
                        return gson.toJson(Map.of("error", true, "message", "Invalid duration format"));
                    }

                    Punishment punishment = switch (punishmentType) {
                        case BAN -> plugin.getPunishmentManager().tempban(
                                targetUUID, finalTargetName, null, finalStaffName, durationMs, finalReason).join();
                        case MUTE -> plugin.getPunishmentManager().tempmute(
                                targetUUID, finalTargetName, null, finalStaffName, durationMs, finalReason).join();
                        default -> null;
                    };

                    if (punishment != null) {
                        plugin.getLogger()
                                .info("[Web] " + finalStaffName + " temp-" + punishmentType.name().toLowerCase()
                                        + "ned " + finalTargetName + " via web dashboard. Duration: " + finalDuration
                                        + ", Reason: " + finalReason);
                        response.put("success", true);
                        response.put("message", "Punishment applied successfully");
                        response.put("punishment", punishmentToMap(punishment));
                    } else {
                        return gson.toJson(Map.of("error", true, "message", "Unsupported punishment type"));
                    }
                } else {
                    Punishment punishment = switch (punishmentType) {
                        case BAN -> plugin.getPunishmentManager().ban(
                                targetUUID, finalTargetName, null, finalStaffName, finalReason).join();
                        case MUTE -> plugin.getPunishmentManager().mute(
                                targetUUID, finalTargetName, null, finalStaffName, finalReason).join();
                        default -> null;
                    };

                    if (punishment != null) {
                        plugin.getLogger()
                                .info("[Web] " + finalStaffName + " permanently " + punishmentType.name().toLowerCase()
                                        + "ned " + finalTargetName + " via web dashboard. Reason: " + finalReason);
                        response.put("success", true);
                        response.put("message", "Punishment applied successfully");
                        response.put("punishment", punishmentToMap(punishment));
                    } else {
                        return gson.toJson(Map.of("error", true, "message", "Unsupported punishment type"));
                    }
                }

                return gson.toJson(response);
            } catch (Exception e) {
                plugin.getLogger().warning("[Web] Failed to apply punishment: " + e.getMessage());
                return gson.toJson(Map.of("error", true, "message", "Failed to apply punishment: " + e.getMessage()));
            }
        }, webExecutor);

        runAsync(ctx, future);
    }


    public void removePunishment(Context ctx) {
        JsonObject body;
        try {
            body = gson.fromJson(ctx.body(), JsonObject.class);
        } catch (Exception e) {
            sendError(ctx, HttpStatus.BAD_REQUEST, "Invalid request body");
            return;
        }

        String targetName = getJsonString(body, "target");
        String type = getJsonString(body, "type");
        String reason = getJsonString(body, "reason");
        String staffName = getJsonString(body, "staffName");

        if (targetName == null || type == null) {
            sendError(ctx, HttpStatus.BAD_REQUEST, "Missing required fields: target, type");
            return;
        }

        PunishmentType punishmentType;
        try {
            punishmentType = PunishmentType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            sendError(ctx, HttpStatus.BAD_REQUEST, "Invalid punishment type. Valid: BAN, MUTE");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sendError(ctx, HttpStatus.NOT_FOUND, "Player not found");
            return;
        }

        UUID targetUUID = target.getUniqueId();
        String finalTargetName = target.getName();
        String finalReason = reason != null ? reason : "Removed via Web Dashboard";
        String finalStaffName = staffName != null ? staffName : "WebAdmin";

        ctx.contentType("application/json");

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                Boolean success = switch (punishmentType) {
                    case BAN ->
                        plugin.getPunishmentManager().unban(targetUUID, null, finalStaffName, finalReason).join();
                    case MUTE ->
                        plugin.getPunishmentManager().unmute(targetUUID, null, finalStaffName, finalReason).join();
                    default -> false;
                };

                if (success) {
                    String action = punishmentType == PunishmentType.BAN ? "unbanned" : "unmuted";
                    plugin.getLogger().info("[Web] " + finalStaffName + " " + action + " " + finalTargetName
                            + " via web dashboard. Reason: " + finalReason);
                }

                Map<String, Object> response = new HashMap<>();
                response.put("success", success);
                response.put("message", success ? "Punishment removed successfully" : "No active punishment found");
                return gson.toJson(response);
            } catch (Exception e) {
                plugin.getLogger().warning("[Web] Failed to remove punishment: " + e.getMessage());
                return gson.toJson(Map.of("error", true, "message", "Failed to remove punishment: " + e.getMessage()));
            }
        }, webExecutor);

        runAsync(ctx, future);
    }


    public void testEndpoint(Context ctx) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "Web server is working!");

        ctx.contentType("application/json");
        ctx.result(gson.toJson(response));
    }


    private Map<String, Object> punishmentToMap(Punishment p) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.getId());
        map.put("targetUuid", p.getTargetUUID() != null ? p.getTargetUUID().toString() : null);
        map.put("targetName", p.getTargetName());
        map.put("staffName", p.getStaffName());
        map.put("type", p.getType().name());
        map.put("reason", p.getReason());
        map.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toEpochMilli() : null);
        map.put("expiresAt", p.getExpiresAt() != null ? p.getExpiresAt().toEpochMilli() : null);
        map.put("active", p.isActive());
        map.put("expired", p.isExpired());
        map.put("permanent", p.isPermanent());

        if (!p.isActive() && p.getRemovedByName() != null) {
            map.put("removedByName", p.getRemovedByName());
            map.put("removedAt", p.getRemovedAt() != null ? p.getRemovedAt().toEpochMilli() : null);
            map.put("removeReason", p.getRemoveReason());
        }

        return map;
    }

    private String getJsonString(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return null;
    }

    private void sendError(Context ctx, HttpStatus status, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);

        ctx.status(status);
        ctx.contentType("application/json");
        ctx.result(gson.toJson(error));
    }
}
