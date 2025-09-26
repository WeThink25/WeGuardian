package me.wethink.weGuardian.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.wethink.weGuardian.WeGuardian;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class LoginApiServlet extends HttpServlet {
    
    private final WeGuardian plugin;
    private final SessionManager sessionManager;
    private final Gson gson = new Gson();
    
    public LoginApiServlet(WeGuardian plugin, SessionManager sessionManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
    }
    
    @Override
protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setContentType("application/json");
    
    try {
        String username = null;
        String password = null;
        
        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            BufferedReader reader = request.getReader();
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            
            JsonObject requestData = gson.fromJson(json.toString(), JsonObject.class);
            username = requestData.get("username").getAsString();
            password = requestData.get("password").getAsString();
        } else {
            username = request.getParameter("username");
            password = request.getParameter("password");
        }
        
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("error", "Username and password are required");
            
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            PrintWriter out = response.getWriter();
            out.print(gson.toJson(error));
            return;
        }
        
        if (sessionManager.isAccountLocked(username)) {
            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("error", "Account is temporarily locked due to too many failed attempts");
            
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            PrintWriter out = response.getWriter();
            out.print(gson.toJson(error));
            return;
        }
        
        if (sessionManager.authenticate(username, password)) {
            String sessionId = sessionManager.createSession(username);
            
            Cookie sessionCookie = new Cookie("sessionId", sessionId);
            sessionCookie.setHttpOnly(true);
            sessionCookie.setMaxAge(plugin.getConfig().getInt("web-dashboard.session-timeout", 3600));
            sessionCookie.setPath("/");
            response.addCookie(sessionCookie);
            
            JsonObject success = new JsonObject();
            success.addProperty("success", true);
            success.addProperty("message", "Login successful");
            success.addProperty("sessionId", sessionId);
            
            PrintWriter out = response.getWriter();
            out.print(gson.toJson(success));
        } else {
            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("error", "Invalid username or password");
            
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            PrintWriter out = response.getWriter();
            out.print(gson.toJson(error));
        }
        
    } catch (Exception e) {
        plugin.getLogger().severe("Error processing login: " + e.getMessage());
        
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", "Internal server error");
        
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        PrintWriter out = response.getWriter();
        out.print(gson.toJson(error));
    }
  }
}
