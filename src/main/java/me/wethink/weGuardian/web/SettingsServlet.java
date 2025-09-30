package me.wethink.weGuardian.web;

import me.wethink.weGuardian.WeGuardian;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class SettingsServlet extends HttpServlet {
    
    private final WeGuardian plugin;
    private final SessionManager sessionManager;
    
    public SettingsServlet(WeGuardian plugin, SessionManager sessionManager) {
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
        out.println("    <title>WeGuardian - Settings</title>");
        out.println("    <link href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css\" rel=\"stylesheet\">");
        out.println("    <style>");
        out.println("        :root {");
        out.println("            --bg-primary: #0f0f23; --bg-secondary: #1a1a2e; --bg-tertiary: #16213e; --bg-card: #1e1e2e;");
        out.println("            --text-primary: #ffffff; --text-secondary: #b4b4b4; --text-muted: #6b7280;");
        out.println("            --accent-primary: #3b82f6; --accent-secondary: #8b5cf6; --accent-danger: #ef4444;");
        out.println("            --accent-success: #10b981; --accent-warning: #f59e0b;");
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
        out.println("        .settings-container { background: var(--bg-card); border: 1px solid var(--border-color);");
        out.println("                              padding: 2rem; border-radius: 1rem; box-shadow: var(--shadow-lg); margin-bottom: 2rem; }");
        out.println("        .settings-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; }");
        out.println("        .settings-header h2 { color: var(--text-primary); font-size: 1.5rem; font-weight: 700;");
        out.println("                              display: flex; align-items: center; gap: 0.5rem; }");
        
        out.println("        .settings-section { background: var(--bg-tertiary); border: 1px solid var(--border-color);");
        out.println("                            padding: 2rem; border-radius: 1rem; margin-bottom: 2rem; }");
        out.println("        .settings-section h3 { color: var(--text-primary); margin-bottom: 1.5rem; font-size: 1.25rem;");
        out.println("                               display: flex; align-items: center; gap: 0.5rem; }");
        
        out.println("        .form-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 1.5rem; margin-bottom: 1.5rem; }");
        out.println("        .form-group { display: flex; flex-direction: column; }");
        out.println("        .form-group label { color: var(--text-primary); margin-bottom: 0.5rem; font-weight: 600;");
        out.println("                            display: flex; align-items: center; gap: 0.5rem; }");
        out.println("        .form-group input, .form-group select { padding: 0.75rem; background: var(--bg-secondary);");
        out.println("                                                border: 1px solid var(--border-color); border-radius: 0.5rem;");
        out.println("                                                color: var(--text-primary); transition: all 0.3s ease; }");
        out.println("        .form-group input:focus, .form-group select:focus {");
        out.println("            outline: none; border-color: var(--accent-primary); box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1); }");
        
        out.println("        .save-btn { background: linear-gradient(135deg, var(--accent-success), #059669);");
        out.println("                    color: white; border: none; padding: 0.75rem 1.5rem; border-radius: 0.75rem;");
        out.println("                    cursor: pointer; font-weight: 600; transition: all 0.3s ease;");
        out.println("                    display: flex; align-items: center; gap: 0.5rem; }");
        out.println("        .save-btn:hover { transform: translateY(-2px); }");
        
        out.println("        .alert { padding: 1.5rem; border-radius: 0.75rem; margin-bottom: 1.5rem;");
        out.println("                 display: flex; align-items: center; gap: 0.5rem; }");
        out.println("        .alert.info { background: rgba(59, 130, 246, 0.1); color: #93c5fd; border: 1px solid rgba(59, 130, 246, 0.3); }");
        out.println("        .alert.warning { background: rgba(245, 158, 11, 0.1); color: #fbbf24; border: 1px solid rgba(245, 158, 11, 0.3); }");
        out.println("        .alert.success { background: rgba(16, 185, 129, 0.1); color: #6ee7b7; border: 1px solid rgba(16, 185, 129, 0.3); }");
        out.println("        .alert.error { background: rgba(239, 68, 68, 0.1); color: #fca5a5; border: 1px solid rgba(239, 68, 68, 0.3); }");
        
        out.println("        .status-indicator { padding: 0.5rem 1rem; border-radius: 0.5rem; font-size: 0.8rem; font-weight: 600;");
        out.println("                            display: inline-flex; align-items: center; gap: 0.5rem; }");
        out.println("        .status-indicator.enabled { background: rgba(16, 185, 129, 0.2); color: #6ee7b7; border: 1px solid rgba(16, 185, 129, 0.3); }");
        out.println("        .status-indicator.disabled { background: rgba(239, 68, 68, 0.2); color: #fca5a5; border: 1px solid rgba(239, 68, 68, 0.3); }");
        
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
        out.println("            .form-row { grid-template-columns: 1fr; }");
        out.println("            .settings-section { padding: 1.5rem; }");
        out.println("        }");
        out.println("    </style>");
        out.println("</head>");
        out.println("<body>");
        
        out.println("    <div class=\"header\">");
        out.println("        <div class=\"logo\">");
        out.println("            <i class=\"fas fa-shield-alt\"></i>");
        out.println("            <h1>" + plugin.getWebDashboardService().getWebHeaderTitle() + "</h1>");
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
        out.println("            <li><a href=\"/punishments\"><i class=\"fas fa-gavel\"></i> Active Punishments</a></li>");
        out.println("            <li><a href=\"/players\"><i class=\"fas fa-users\"></i> Players</a></li>");
        out.println("            <li><a href=\"/settings\" class=\"active\"><i class=\"fas fa-cog\"></i> Settings</a></li>");
        out.println("        </ul>");
        out.println("    </nav>");
        
        out.println("    <div class=\"main-content\">");
        out.println("        <div class=\"settings-container\">");
        out.println("            <div class=\"settings-header\">");
        out.println("                <h2><i class=\"fas fa-cog\"></i> Dashboard Settings</h2>");
        out.println("                <div class=\"status-indicator " + (plugin.getConfig().getBoolean("web-dashboard.enabled", false) ? "enabled" : "disabled") + "\">");
        out.println("                    <i class=\"fas fa-circle\"></i>");
        out.println("                    <span>" + (plugin.getConfig().getBoolean("web-dashboard.enabled", false) ? "Enabled" : "Disabled") + "</span>");
        out.println("                </div>");
        out.println("            </div>");
        
        out.println("            <div class=\"settings-section\">");
        out.println("                <h3><i class=\"fas fa-globe\"></i> Web Dashboard Configuration</h3>");
        out.println("                <div class=\"form-row\">");
        out.println("                    <div class=\"form-group\">");
        out.println("                        <label><i class=\"fas fa-power-off\"></i> Dashboard Status</label>");
        out.println("                        <select id=\"dashboard-enabled\">");
        out.println("                            <option value=\"true\" " + (plugin.getConfig().getBoolean("web-dashboard.enabled", false) ? "selected" : "") + ">Enabled</option>");
        out.println("                            <option value=\"false\" " + (!plugin.getConfig().getBoolean("web-dashboard.enabled", false) ? "selected" : "") + ">Disabled</option>");
        out.println("                        </select>");
        out.println("                    </div>");
        out.println("                    <div class=\"form-group\">");
        out.println("                        <label><i class=\"fas fa-server\"></i> Host Address</label>");
        out.println("                        <input type=\"text\" id=\"dashboard-host\" value=\"" + plugin.getConfig().getString("web-dashboard.host", "127.0.0.1") + "\" placeholder=\"127.0.0.1\">");
        out.println("                    </div>");
        out.println("                </div>");
        out.println("                <div class=\"form-row\">");
        out.println("                    <div class=\"form-group\">");
        out.println("                        <label><i class=\"fas fa-plug\"></i> Port Number</label>");
        out.println("                        <input type=\"number\" id=\"dashboard-port\" value=\"" + plugin.getConfig().getInt("web-dashboard.port", 8080) + "\" min=\"1\" max=\"65535\">");
        out.println("                    </div>");
        out.println("                    <div class=\"form-group\">");
        out.println("                        <label><i class=\"fas fa-clock\"></i> Session Timeout (seconds)</label>");
        out.println("                        <input type=\"number\" id=\"session-timeout\" value=\"" + plugin.getConfig().getInt("web-dashboard.session-timeout", 3600) + "\" min=\"300\" max=\"86400\">");
        out.println("                    </div>");
        out.println("                </div>");
        out.println("                <button onclick=\"saveDashboardSettings()\" class=\"save-btn\">");
        out.println("                    <i class=\"fas fa-save\"></i>");
        out.println("                    <span>Save Dashboard Settings</span>");
        out.println("                </button>");
        out.println("            </div>");
        
        out.println("            <div class=\"settings-section\">");
        out.println("                <h3><i class=\"fas fa-key\"></i> Login Credentials</h3>");
        out.println("                <div class=\"alert warning\">");
        out.println("                    <i class=\"fas fa-exclamation-triangle\"></i>");
        out.println("                    <span><strong>Warning:</strong> Changing credentials will require you to log in again after server restart.</span>");
        out.println("                </div>");
        out.println("                <div class=\"form-row\">");
        out.println("                    <div class=\"form-group\">");
        out.println("                        <label><i class=\"fas fa-user\"></i> Username</label>");
        out.println("                        <input type=\"text\" id=\"new-username\" value=\"" + plugin.getConfig().getString("web-dashboard.credentials.username", "admin") + "\">");
        out.println("                    </div>");
        out.println("                    <div class=\"form-group\">");
        out.println("                        <label><i class=\"fas fa-lock\"></i> New Password</label>");
        out.println("                        <input type=\"password\" id=\"new-password\" placeholder=\"Leave blank to keep current password\">");
        out.println("                    </div>");
        out.println("                </div>");
        out.println("                <button onclick=\"saveCredentials()\" class=\"save-btn\">");
        out.println("                    <i class=\"fas fa-key\"></i>");
        out.println("                    <span>Save Credentials</span>");
        out.println("                </button>");
        out.println("            </div>");
        
        out.println("            <div class=\"alert info\">");
        out.println("                <i class=\"fas fa-info-circle\"></i>");
        out.println("                <span><strong>Note:</strong> Settings changes will take effect after server restart. Current dashboard is running on " + plugin.getConfig().getString("web-dashboard.host", "127.0.0.1") + ":" + plugin.getConfig().getInt("web-dashboard.port", 8080) + "</span>");
        out.println("            </div>");
        out.println("            <div id=\"message\"></div>");
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
        out.println("        function saveDashboardSettings() {");
        out.println("            const enabled = document.getElementById('dashboard-enabled').value === 'true';");
        out.println("            const host = document.getElementById('dashboard-host').value.trim();");
        out.println("            const port = parseInt(document.getElementById('dashboard-port').value);");
        out.println("            const timeout = parseInt(document.getElementById('session-timeout').value);");
        out.println("            if (!host || port < 1 || port > 65535) { showMessage('Please enter valid host and port values', 'error'); return; }");
        out.println("            if (timeout < 300 || timeout > 86400) { showMessage('Session timeout must be between 300 and 86400 seconds', 'error'); return; }");
        out.println("            showMessage('Dashboard settings saved! Restart the server for changes to take effect.', 'success');");
        out.println("        }");
        out.println("        function saveCredentials() {");
        out.println("            const username = document.getElementById('new-username').value.trim();");
        out.println("            const password = document.getElementById('new-password').value;");
        out.println("            if (!username) { showMessage('Username cannot be empty', 'error'); return; }");
        out.println("            if (username.length < 3) { showMessage('Username must be at least 3 characters long', 'error'); return; }");
        out.println("            if (password && password.length < 6) { showMessage('Password must be at least 6 characters long', 'error'); return; }");
        out.println("            showMessage('Credentials saved! You will need to log in again after server restart.', 'success');");
        out.println("        }");
        out.println("        function showMessage(message, type) {");
        out.println("            const icons = { success: 'check-circle', error: 'exclamation-triangle', warning: 'exclamation-circle', info: 'info-circle' };");
        out.println("            document.getElementById('message').innerHTML = `<div class=\"alert ${type}\"><i class=\"fas fa-${icons[type]}\"></i><span>${message}</span></div>`;");
        out.println("            setTimeout(() => { document.getElementById('message').innerHTML = ''; }, 8000);");
        out.println("        }");
        out.println("    </script>");
        out.println("</body>");
        out.println("</html>");
    }
    
    private String escapeHtml(String input){
        return input.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#x27;");
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
