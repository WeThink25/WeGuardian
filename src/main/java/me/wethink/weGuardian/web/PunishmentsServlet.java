package me.wethink.weGuardian.web;

import me.wethink.weGuardian.WeGuardian;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.models.PunishmentType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class PunishmentsServlet extends HttpServlet {

    private final WeGuardian plugin;
    private final SessionManager sessionManager;
    private final Gson gson = new Gson();

    public PunishmentsServlet(WeGuardian plugin, SessionManager sessionManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
    }
    
    @Override
protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String sessionId = getSessionIdFromCookies(request);
    
    if (sessionId == null || !sessionManager.isValidSession(sessionId)) {
        response.sendRedirect("/login");
        return;
    }
    
    SessionManager.Session session = sessionManager.getSession(sessionId);
    
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    
    out.println("<!DOCTYPE html>");
    out.println("<html lang=\"en\">");
    out.println("<head>");
    out.println("    <meta charset=\"UTF-8\">");
    out.println("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
    out.println("    <title>WeGuardian - Active Punishments</title>");
    out.println("    <link href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css\" rel=\"stylesheet\">");
    out.println("    <style>");
    out.println("        :root {");
    out.println("            --bg-primary: #0f0f23; --bg-secondary: #1a1a2e; --bg-tertiary: #16213e; --bg-card: #1e1e2e;");
    out.println("            --text-primary: #ffffff; --text-secondary: #b4b4b4; --text-muted: #6b7280;");
    out.println("            --accent-primary: #3b82f6; --accent-secondary: #8b5cf6; --accent-danger: #ef4444;");
    out.println("            --border-color: #374151; --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1);");
    out.println("        }");
    out.println("        * { margin: 0; padding: 0; box-sizing: border-box; }");
    out.println("        body { font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
    out.println("               background: linear-gradient(135deg, var(--bg-primary) 0%, var(--bg-secondary) 100%);");
    out.println("               color: var(--text-primary); min-height: 100vh; display: flex; flex-direction: column; }");
    
    out.println("        .header { background: linear-gradient(135deg, var(--bg-tertiary) 0%, var(--bg-card) 100%);");
    out.println("                  border-bottom: 1px solid var(--border-color); padding: 1.5rem 2rem;");
    out.println("                  display: flex; justify-content: space-between; align-items: center;");
    out.println("                  box-shadow: var(--shadow-lg); position: sticky; top: 0; z-index: 100; }");
    out.println("        .header .logo { display: flex; align-items: center; gap: 1rem; }");
    out.println("        .header .logo i { font-size: 2rem; background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));");
    out.println("                          -webkit-background-clip: text; -webkit-text-fill-color: transparent; }");
    out.println("        .header h1 { font-size: 1.75rem; font-weight: 700;");
    out.println("                     background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));");
    out.println("                     -webkit-background-clip: text; -webkit-text-fill-color: transparent; }");
    out.println("        .header .user-info { display: flex; align-items: center; gap: 1.5rem; }");
    out.println("        .user-welcome { display: flex; align-items: center; gap: 0.5rem; color: var(--text-secondary); font-weight: 500; }");
    out.println("        .logout-btn { background: linear-gradient(135deg, var(--accent-danger), #dc2626); border: none;");
    out.println("                      color: white; padding: 0.75rem 1.5rem; border-radius: 0.75rem; cursor: pointer;");
    out.println("                      text-decoration: none; font-weight: 600; transition: all 0.3s ease;");
    out.println("                      display: flex; align-items: center; gap: 0.5rem; }");
    out.println("        .logout-btn:hover { transform: translateY(-2px); }");
    
    out.println("        .nav { background: var(--bg-card); border-bottom: 1px solid var(--border-color); padding: 1rem 2rem; }");
    out.println("        .nav ul { list-style: none; display: flex; gap: 0.5rem; flex-wrap: wrap; }");
    out.println("        .nav a { text-decoration: none; color: var(--text-secondary); padding: 0.75rem 1.5rem;");
    out.println("                 border-radius: 0.75rem; transition: all 0.3s ease; font-weight: 500;");
    out.println("                 display: flex; align-items: center; gap: 0.5rem; }");
    out.println("        .nav a:hover { color: var(--text-primary); background: var(--bg-tertiary); transform: translateY(-1px); }");
    out.println("        .nav a.active { background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary)); color: white; }");
    
    out.println("        .main-content { flex: 1; padding: 2rem; max-width: 1400px; margin: 0 auto; width: 100%; }");
    out.println("        .punishments-container { background: var(--bg-card); border: 1px solid var(--border-color);");
    out.println("                                 padding: 2rem; border-radius: 1rem; box-shadow: var(--shadow-lg); margin-bottom: 2rem; }");
    out.println("        .punishments-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; }");
    out.println("        .punishments-header h2 { color: var(--text-primary); font-size: 1.5rem; font-weight: 700;");
    out.println("                                  display: flex; align-items: center; gap: 0.5rem; }");
    out.println("        .refresh-btn { background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));");
    out.println("                       color: white; border: none; padding: 0.75rem 1.5rem; border-radius: 0.75rem;");
    out.println("                       cursor: pointer; font-weight: 600; transition: all 0.3s ease;");
    out.println("                       display: flex; align-items: center; gap: 0.5rem; }");
    out.println("        .refresh-btn:hover { transform: translateY(-2px); }");
    

    out.println("        .punishments-table { width: 100%; border-collapse: collapse; background: var(--bg-tertiary);");
    out.println("                             border-radius: 0.75rem; overflow: hidden; }");
    out.println("        .punishments-table th, .punishments-table td { padding: 1rem; text-align: left;");
    out.println("                                                        border-bottom: 1px solid var(--border-color); }");
    out.println("        .punishments-table th { background: var(--bg-secondary); font-weight: 600; color: var(--text-primary);");
    out.println("                                 text-transform: uppercase; font-size: 0.85rem; letter-spacing: 0.05em; }");
    out.println("        .punishments-table td { color: var(--text-secondary); }");
    out.println("        .punishments-table tr:hover { background: var(--bg-card); }");
    

    out.println("        .punishment-type { padding: 0.5rem 1rem; border-radius: 0.5rem; font-size: 0.8rem; font-weight: 600;");
    out.println("                           text-transform: uppercase; letter-spacing: 0.05em; }");
    out.println("        .type-ban { background: rgba(239, 68, 68, 0.2); color: #fca5a5; border: 1px solid rgba(239, 68, 68, 0.3); }");
    out.println("        .type-tempban { background: rgba(245, 158, 11, 0.2); color: #fbbf24; border: 1px solid rgba(245, 158, 11, 0.3); }");
    out.println("        .type-mute { background: rgba(16, 185, 129, 0.2); color: #6ee7b7; border: 1px solid rgba(16, 185, 129, 0.3); }");
    out.println("        .type-tempmute { background: rgba(245, 158, 11, 0.2); color: #fbbf24; border: 1px solid rgba(245, 158, 11, 0.3); }");
    out.println("        .type-kick { background: rgba(139, 92, 246, 0.2); color: #c4b5fd; border: 1px solid rgba(139, 92, 246, 0.3); }");
    out.println("        .type-warn { background: rgba(59, 130, 246, 0.2); color: #93c5fd; border: 1px solid rgba(59, 130, 246, 0.3); }");
    
    out.println("        .revoke-btn { background: linear-gradient(135deg, var(--accent-danger), #dc2626);");
    out.println("                      color: white; border: none; padding: 0.5rem 1rem; border-radius: 0.5rem;");
    out.println("                      cursor: pointer; font-size: 0.8rem; font-weight: 600; transition: all 0.3s ease;");
    out.println("                      display: flex; align-items: center; gap: 0.25rem; }");
    out.println("        .revoke-btn:hover { transform: translateY(-1px); }");
    out.println("        .loading { text-align: center; padding: 3rem; color: var(--text-muted);");
    out.println("                   display: flex; flex-direction: column; align-items: center; gap: 1rem; }");
    out.println("        .loading i { font-size: 2rem; animation: spin 1s linear infinite; }");
    out.println("        @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }");
    

    out.println("        .footer { background: var(--bg-card); border-top: 1px solid var(--border-color);");
    out.println("                  padding: 2rem; text-align: center; margin-top: auto; }");
    out.println("        .footer-content { max-width: 1400px; margin: 0 auto; display: flex;");
    out.println("                          flex-direction: column; align-items: center; gap: 1rem; }");
    out.println("        .footer-brand { display: flex; align-items: center; gap: 0.5rem; font-weight: 600; color: var(--text-primary); }");
    out.println("        .footer-brand .heart { color: var(--accent-danger); animation: heartbeat 2s ease-in-out infinite; }");
    out.println("        .footer-copyright { color: var(--text-muted); font-size: 0.9rem; }");
    out.println("        @keyframes heartbeat { 0%, 50%, 100% { transform: scale(1); } 25%, 75% { transform: scale(1.1); } }");
    

    out.println("        @media (max-width: 768px) {");
    out.println("            .header { padding: 1rem; flex-direction: column; gap: 1rem; }");
    out.println("            .main-content { padding: 1rem; }");
    out.println("            .punishments-header { flex-direction: column; gap: 1rem; align-items: flex-start; }");
    out.println("            .punishments-table { font-size: 0.9rem; }");
    out.println("            .punishments-table th, .punishments-table td { padding: 0.75rem 0.5rem; }");
    out.println("        }");
    out.println("    </style>");
    out.println("</head>");
    out.println("<body>");
    

    out.println("    <div class=\"header\">");
    out.println("        <div class=\"logo\">");
    out.println("            <i class=\"fas fa-shield-alt\"></i>");
    out.println("            <h1>WeGuardian</h1>");
    out.println("        </div>");
    out.println("        <div class=\"user-info\">");
    out.println("            <div class=\"user-welcome\">");
    out.println("                <i class=\"fas fa-user-circle\"></i>");
    out.println("                <span>Welcome, " + escapeHtml(session.getUsername()) + "</span>");
    out.println("            </div>");
    out.println("            <a href=\"/logout\" class=\"logout-btn\">");
    out.println("                <i class=\"fas fa-sign-out-alt\"></i>");
    out.println("                <span>Logout</span>");
    out.println("            </a>");
    out.println("        </div>");
    out.println("    </div>");
    

    out.println("    <nav class=\"nav\">");
    out.println("        <ul>");
    out.println("            <li><a href=\"/dashboard\"><i class=\"fas fa-tachometer-alt\"></i> Dashboard</a></li>");
    out.println("            <li><a href=\"/punishments\" class=\"active\"><i class=\"fas fa-gavel\"></i> Active Punishments</a></li>");
    out.println("            <li><a href=\"/players\"><i class=\"fas fa-users\"></i> Players</a></li>");
    out.println("            <li><a href=\"/settings\"><i class=\"fas fa-cog\"></i> Settings</a></li>");
    out.println("        </ul>");
    out.println("    </nav>");
    

    out.println("    <div class=\"main-content\">");
    out.println("        <div class=\"punishments-container\">");
    out.println("            <div class=\"punishments-header\">");
    out.println("                <h2><i class=\"fas fa-gavel\"></i> Active Punishments</h2>");
    out.println("                <button class=\"refresh-btn\" onclick=\"loadPunishments()\">");
    out.println("                    <i class=\"fas fa-sync-alt\"></i>");
    out.println("                    <span>Refresh</span>");
    out.println("                </button>");
    out.println("            </div>");
    out.println("            <div id=\"punishments-content\">");
    out.println("                <div class=\"loading\">");
    out.println("                    <i class=\"fas fa-spinner\"></i>");
    out.println("                    <span>Loading punishments...</span>");
    out.println("                </div>");
    out.println("            </div>");
    out.println("        </div>");
    out.println("    </div>");
    

    out.println("    <footer class=\"footer\">");
    out.println("        <div class=\"footer-content\">");
    out.println("            <div class=\"footer-brand\">");
    out.println("                <span>Made with</span>");
    out.println("                <i class=\"fas fa-heart heart\"></i>");
    out.println("                <span>by <strong>WeThink</strong></span>");
    out.println("            </div>");
    out.println("            <div class=\"footer-copyright\">");
    out.println("                <span>&copy; " + java.time.Year.now().getValue() + " WeThink. All rights reserved.</span>");
    out.println("            </div>");
    out.println("        </div>");
    out.println("    </footer>");
    

    out.println("    <script>");
    out.println("        function loadPunishments() {");
    out.println("            document.getElementById('punishments-content').innerHTML = '<div class=\"loading\"><i class=\"fas fa-spinner\"></i><span>Loading punishments...</span></div>';");
    out.println("            fetch('/api/punishments').then(r=>r.json()).then(data=>{");
    out.println("                if(data.success){ displayPunishments(data.punishments); }");
    out.println("                else { document.getElementById('punishments-content').innerHTML = '<div class=\"loading\" style=\"color: var(--accent-danger);\"><i class=\"fas fa-exclamation-triangle\"></i><span>Error loading punishments</span></div>'; }");
    out.println("            }).catch(e=>{ console.error('Error:',e); document.getElementById('punishments-content').innerHTML = '<div class=\"loading\" style=\"color: var(--accent-danger);\"><i class=\"fas fa-exclamation-triangle\"></i><span>Error loading punishments</span></div>'; });");
    out.println("        }");
    out.println("        function displayPunishments(punishments) {");
    out.println("            if(punishments.length === 0) { document.getElementById('punishments-content').innerHTML = '<div class=\"loading\" style=\"color: var(--text-muted);\"><i class=\"fas fa-info-circle\"></i><span>No active punishments</span></div>'; return; }");
    out.println("            let html = '<table class=\"punishments-table\"><thead><tr><th>Player</th><th>Type</th><th>Reason</th><th>Staff</th><th>Date</th><th>Expires</th><th>Actions</th></tr></thead><tbody>';");
    out.println("            punishments.forEach(p => { const date = new Date(p.createdAt).toLocaleString(); const expires = p.expiresAt ? new Date(p.expiresAt).toLocaleString() : 'Never'; html += `<tr><td>${p.targetName}</td><td><span class=\"punishment-type type-${p.type.toLowerCase()}\">${p.type}</span></td><td>${p.reason}</td><td>${p.staffName}</td><td>${date}</td><td>${expires}</td><td><button class=\"revoke-btn\" onclick=\"revokePunishment(${p.id})\"><i class=\"fas fa-times\"></i> Revoke</button></td></tr>`; });");
    out.println("            html += '</tbody></table>'; document.getElementById('punishments-content').innerHTML = html;");
    out.println("        }");
    out.println("        function revokePunishment(id) {");
    out.println("            if(confirm('Are you sure you want to revoke this punishment?')) {");
    out.println("                fetch(`/api/punishments/${id}`, {");
    out.println("                    method: 'DELETE',");
    out.println("                    credentials: 'same-origin'");
    out.println("                })");
    out.println("                .then(r => {");
    out.println("                    if (!r.ok) {");
    out.println("                        throw new Error('Network response was not ok');");
    out.println("                    }");
    out.println("                    return r.json();");
    out.println("                })");
    out.println("                .then(data => {");
    out.println("                    if(data.success) {");
    out.println("                        loadPunishments();");
    out.println("                    } else {");
    out.println("                        alert('Error revoking punishment: ' + (data.error || 'Unknown error'));");
    out.println("                    }");
    out.println("                })");
    out.println("                .catch(err => {");
    out.println("                    console.error('Revoke error:', err);");
    out.println("                    alert('Failed to revoke punishment. Please try again.');");
    out.println("                });");
    out.println("            }");
    out.println("        }");
    out.println("        loadPunishments(); setInterval(loadPunishments, 30000);");
    out.println("    </script>");
    out.println("</body>");
    out.println("</html>");
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
        
        CompletableFuture<Void> future = plugin.getDatabaseManager().removePunishment(
            punishmentId, UUID.randomUUID(), staffName, "Revoked via web dashboard"
        );
        
        future.join();
        
        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("message", "Punishment revoked successfully");
        
        plugin.getWebDashboardService().getWebSocketHandler().broadcastStats();
        
        PrintWriter out = response.getWriter();
        out.print(gson.toJson(result));
        
    } catch (Exception e) {
        plugin.getLogger().severe("Error revoking punishment: " + e.getMessage());
        
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", "Failed to revoke punishment: " + e.getMessage());
        
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

private String escapeHtml(String input){
    return input.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#x27;");
}
}