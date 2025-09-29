package me.wethink.weGuardian.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.CompletableFuture;

public class PlayersApiServlet extends HttpServlet {

    private final WeGuardian plugin;
    private final SessionManager sessionManager;
    private final Gson gson = new Gson();

    public PlayersApiServlet(WeGuardian plugin, SessionManager sessionManager) {
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

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("error", "Player name is required");

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            PrintWriter out = response.getWriter();
            out.print(gson.toJson(error));
            return;
        }

        String playerName = pathInfo.substring(1);
        if (playerName.isEmpty()) {
            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("error", "Player name is required");

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            PrintWriter out = response.getWriter();
            out.print(gson.toJson(error));
            return;
        }

        try {
            CompletableFuture<PlayerData> future = plugin.getDatabaseManager().getPlayerData(playerName);
            PlayerData playerData = future.join();

            if (playerData == null) {
                JsonObject error = new JsonObject();
                error.addProperty("success", false);
                error.addProperty("error", "Player not found or has never joined the server");

                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                PrintWriter out = response.getWriter();
                out.print(gson.toJson(error));
                return;
            }

            boolean isOnline = false;
            Player onlinePlayer = Bukkit.getPlayerExact(playerData.getName());
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                isOnline = true;
            }

            JsonObject playerJson = new JsonObject();
            playerJson.addProperty("name", playerData.getName());
            playerJson.addProperty("uuid", playerData.getUuid().toString());
            playerJson.addProperty("online", isOnline);
            playerJson.addProperty("banned", playerData.isBanned());
            playerJson.addProperty("muted", playerData.isMuted());

            if (playerData.getFirstJoin() != null) {
                playerJson.addProperty("firstJoin", playerData.getFirstJoin().toString());
            }
            if (playerData.getLastSeen() != null) {
                playerJson.addProperty("lastSeen", playerData.getLastSeen().toString());
            }
            if (playerData.getLastIP() != null) {
                playerJson.addProperty("lastIP", playerData.getLastIP());
            }

            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.add("player", playerJson);

            PrintWriter out = response.getWriter();
            out.print(gson.toJson(result));

        } catch (Exception e) {
            plugin.getLogger().severe("Error searching for player '" + playerName + "': " + e.getMessage());
            e.printStackTrace();

            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("error", "Internal server error while searching for player");

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