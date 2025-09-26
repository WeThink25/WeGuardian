package me.wethink.weGuardian.web;

import me.wethink.weGuardian.WeGuardian;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class PlayersServlet extends HttpServlet {
    
    private final WeGuardian plugin;
    private final SessionManager sessionManager;
    
    public PlayersServlet(WeGuardian plugin, SessionManager sessionManager) {
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
        out.println("    <title>WeGuardian - Players</title>");
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
        out.println("        .players-container { background: var(--bg-card); border: 1px solid var(--border-color);");
        out.println("                             padding: 2rem; border-radius: 1rem; box-shadow: var(--shadow-lg); margin-bottom: 2rem; }");
        out.println("        .players-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; }");
        out.println("        .players-header h2 { color: var(--text-primary); font-size: 1.5rem; font-weight: 700;");
        out.println("                             display: flex; align-items: center; gap: 0.5rem; }");
        
        out.println("        .search-section { display: flex; gap: 1rem; margin-bottom: 2rem; flex-wrap: wrap; }");
        out.println("        .search-input { flex: 1; min-width: 300px; padding: 1rem; background: var(--bg-tertiary);");
        out.println("                        border: 1px solid var(--border-color); border-radius: 0.75rem;");
        out.println("                        color: var(--text-primary); font-size: 1rem; transition: all 0.3s ease; }");
        out.println("        .search-input:focus { outline: none; border-color: var(--accent-primary); box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1); }");
        out.println("        .search-btn { background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));");
        out.println("                      color: white; border: none; padding: 1rem 2rem; border-radius: 0.75rem;");
        out.println("                      cursor: pointer; font-weight: 600; transition: all 0.3s ease;");
        out.println("                      display: flex; align-items: center; gap: 0.5rem; }");
        out.println("        .search-btn:hover { transform: translateY(-2px); }");
        
        out.println("        .player-info-card { background: var(--bg-tertiary); border: 1px solid var(--border-color);");
        out.println("                            padding: 2rem; border-radius: 1rem; margin-bottom: 2rem; }");
        out.println("        .player-header { display: flex; align-items: center; gap: 1rem; margin-bottom: 1.5rem; }");
        out.println("        .player-avatar { width: 64px; height: 64px; border-radius: 0.75rem; border: 2px solid var(--border-color); }");
        out.println("        .player-details h3 { color: var(--text-primary); font-size: 1.25rem; margin-bottom: 0.5rem; }");
        out.println("        .player-details .status { padding: 0.5rem 1rem; border-radius: 0.5rem; font-size: 0.8rem; font-weight: 600; }");
        out.println("        .status.online { background: rgba(16, 185, 129, 0.2); color: #6ee7b7; border: 1px solid rgba(16, 185, 129, 0.3); }");
        out.println("        .status.offline { background: rgba(107, 114, 128, 0.2); color: #9ca3af; border: 1px solid rgba(107, 114, 128, 0.3); }");
        
        out.println("        .punishment-form { background: var(--bg-secondary); border: 1px solid var(--border-color);");
        out.println("                           padding: 2rem; border-radius: 1rem; margin-top: 2rem; }");
        out.println("        .punishment-form h3 { color: var(--text-primary); margin-bottom: 1.5rem; font-size: 1.25rem;");
        out.println("                              display: flex; align-items: center; gap: 0.5rem; }");
        out.println("        .form-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 1rem; margin-bottom: 1.5rem; }");
        out.println("        .form-group { display: flex; flex-direction: column; }");
        out.println("        .form-group label { color: var(--text-primary); margin-bottom: 0.5rem; font-weight: 600; }");
        out.println("        .form-group input, .form-group select, .form-group textarea { padding: 0.75rem; background: var(--bg-tertiary);");
        out.println("                                                                      border: 1px solid var(--border-color); border-radius: 0.5rem;");
        out.println("                                                                      color: var(--text-primary); transition: all 0.3s ease; }");
        out.println("        .form-group input:focus, .form-group select:focus, .form-group textarea:focus {");
        out.println("            outline: none; border-color: var(--accent-primary); box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1); }");
        out.println("        .form-group textarea { resize: vertical; min-height: 80px; }");
        
        out.println("        .btn-group { display: flex; gap: 1rem; flex-wrap: wrap; }");
        out.println("        .apply-btn { background: linear-gradient(135deg, var(--accent-danger), #dc2626);");
        out.println("                     color: white; border: none; padding: 0.75rem 1.5rem; border-radius: 0.75rem;");
        out.println("                     cursor: pointer; font-weight: 600; transition: all 0.3s ease;");
        out.println("                     display: flex; align-items: center; gap: 0.5rem; }");
        out.println("        .apply-btn:hover { transform: translateY(-2px); }");
        out.println("        .cancel-btn { background: var(--bg-tertiary); color: var(--text-secondary);");
        out.println("                      border: 1px solid var(--border-color); padding: 0.75rem 1.5rem; border-radius: 0.75rem;");
        out.println("                      cursor: pointer; font-weight: 600; transition: all 0.3s ease; }");
        out.println("        .cancel-btn:hover { background: var(--bg-card); color: var(--text-primary); }");
        
        out.println("        .info { padding: 1.5rem; border-radius: 0.75rem; margin-top: 1.5rem;");
        out.println("                display: flex; align-items: center; gap: 0.5rem; }");
        out.println("        .info.success { background: rgba(16, 185, 129, 0.1); color: #6ee7b7; border: 1px solid rgba(16, 185, 129, 0.3); }");
        out.println("        .info.error { background: rgba(239, 68, 68, 0.1); color: #fca5a5; border: 1px solid rgba(239, 68, 68, 0.3); }");
        out.println("        .info.warning { background: rgba(245, 158, 11, 0.1); color: #fbbf24; border: 1px solid rgba(245, 158, 11, 0.3); }");
        
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
        out.println("            .search-section { flex-direction: column; }");
        out.println("            .search-input { min-width: unset; }");
        out.println("            .player-header { flex-direction: column; text-align: center; }");
        out.println("            .form-row { grid-template-columns: 1fr; }");
        out.println("            .btn-group { flex-direction: column; }");
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
        out.println("            <li><a href=\"/punishments\"><i class=\"fas fa-gavel\"></i> Active Punishments</a></li>");
        out.println("            <li><a href=\"/players\" class=\"active\"><i class=\"fas fa-users\"></i> Players</a></li>");
        out.println("            <li><a href=\"/settings\"><i class=\"fas fa-cog\"></i> Settings</a></li>");
        out.println("        </ul>");
        out.println("    </nav>");
        
        out.println("    <div class=\"main-content\">");
        out.println("        <div class=\"players-container\">");
        out.println("            <div class=\"players-header\">");
        out.println("                <h2><i class=\"fas fa-users\"></i> Player Management</h2>");
        out.println("            </div>");
        out.println("            <div class=\"search-section\">");
        out.println("                <input type=\"text\" id=\"player-search\" class=\"search-input\" placeholder=\"Enter player name...\">");
        out.println("                <button onclick=\"searchPlayer()\" class=\"search-btn\">");
        out.println("                    <i class=\"fas fa-search\"></i>");
        out.println("                    <span>Search</span>");
        out.println("                </button>");
        out.println("            </div>");
        out.println("            <div id=\"player-info\"></div>");
        out.println("            <div id=\"punishment-form\" class=\"punishment-form\" style=\"display: none;\">");
        out.println("                <h3><i class=\"fas fa-gavel\"></i> Apply Punishment</h3>");
        out.println("                <div class=\"form-row\">");
        out.println("                    <div class=\"form-group\">");
        out.println("                        <label for=\"punishment-type\"><i class=\"fas fa-list\"></i> Punishment Type</label>");
        out.println("                        <select id=\"punishment-type\">");
        out.println("                            <option value=\"BAN\">Ban</option>");
        out.println("                            <option value=\"TEMPBAN\">Temporary Ban</option>");
        out.println("                            <option value=\"MUTE\">Mute</option>");
        out.println("                            <option value=\"TEMPMUTE\">Temporary Mute</option>");
        out.println("                            <option value=\"KICK\">Kick</option>");
        out.println("                            <option value=\"WARN\">Warning</option>");
        out.println("                        </select>");
        out.println("                    </div>");
        out.println("                    <div class=\"form-group\">");
        out.println("                        <label for=\"duration\"><i class=\"fas fa-clock\"></i> Duration (for temp punishments)</label>");
        out.println("                        <input type=\"text\" id=\"duration\" placeholder=\"e.g., 1d, 2h, 30m\">");
        out.println("                    </div>");
        out.println("                </div>");
        out.println("                <div class=\"form-group\">");
        out.println("                    <label for=\"reason\"><i class=\"fas fa-comment\"></i> Reason</label>");
        out.println("                    <textarea id=\"reason\" placeholder=\"Enter punishment reason...\"></textarea>");
        out.println("                </div>");
        out.println("                <div class=\"btn-group\">");
        out.println("                    <button onclick=\"applyPunishment()\" class=\"apply-btn\">");
        out.println("                        <i class=\"fas fa-gavel\"></i>");
        out.println("                        <span>Apply Punishment</span>");
        out.println("                    </button>");
        out.println("                    <button onclick=\"hidePunishmentForm()\" class=\"cancel-btn\">");
        out.println("                        <i class=\"fas fa-times\"></i>");
        out.println("                        <span>Cancel</span>");
        out.println("                    </button>");
        out.println("                </div>");
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
        out.println("        let currentPlayer = null;");
        out.println("        function searchPlayer() {");
        out.println("            const playerName = document.getElementById('player-search').value.trim();");
        out.println("            if (!playerName) { showMessage('Please enter a player name', 'warning'); return; }");
        out.println("            document.getElementById('player-info').innerHTML = '<div class=\"info\"><i class=\"fas fa-spinner fa-spin\"></i> Searching for player...</div>';");
        out.println("            fetch(`/api/players/${encodeURIComponent(playerName)}`).then(r=>r.json()).then(data=>{");
        out.println("                if(data.success && data.player) { displayPlayerInfo(data.player); currentPlayer = playerName; }");
        out.println("                else { document.getElementById('player-info').innerHTML = '<div class=\"info error\"><i class=\"fas fa-exclamation-triangle\"></i> Player not found or has never joined the server</div>'; }");
        out.println("            }).catch(e=>{ console.error('Error:',e); document.getElementById('player-info').innerHTML = '<div class=\"info error\"><i class=\"fas fa-exclamation-triangle\"></i> Error searching for player</div>'; });");
        out.println("        }");
        out.println("        function displayPlayerInfo(player) {");
        out.println("            const isOnline = player.online || false;");
        out.println("            const avatarUrl = `https://crafatar.com/avatars/${player.uuid}?size=64&overlay`;");
        out.println("            const html = `<div class=\"player-info-card\"><div class=\"player-header\"><img src=\"${avatarUrl}\" alt=\"${player.name}\" class=\"player-avatar\"><div class=\"player-details\"><h3>${player.name}</h3><span class=\"status ${isOnline ? 'online' : 'offline'}\"><i class=\"fas fa-circle\"></i> ${isOnline ? 'Online' : 'Offline'}</span></div></div><button onclick=\"showPunishmentForm()\" class=\"apply-btn\"><i class=\"fas fa-gavel\"></i> Apply Punishment</button></div>`;");
        out.println("            document.getElementById('player-info').innerHTML = html;");
        out.println("        }");
        out.println("        function showPunishmentForm() { document.getElementById('punishment-form').style.display = 'block'; }");
        out.println("        function hidePunishmentForm() { document.getElementById('punishment-form').style.display = 'none'; }");
        out.println("        function applyPunishment() {");
        out.println("            if (!currentPlayer) { showMessage('No player selected', 'error'); return; }");
        out.println("            const type = document.getElementById('punishment-type').value;");
        out.println("            const duration = document.getElementById('duration').value.trim();");
        out.println("            const reason = document.getElementById('reason').value.trim();");
        out.println("            if (!reason) { showMessage('Please enter a reason', 'warning'); return; }");
        out.println("            const data = { targetName: currentPlayer, type: type, reason: reason };");
        out.println("            if (duration && (type === 'TEMPBAN' || type === 'TEMPMUTE')) { data.duration = duration; }");
        out.println("            fetch('/api/punishments', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data) })");
        out.println("            .then(r=>r.json()).then(data=>{ if(data.success) { showMessage('Punishment applied successfully', 'success'); hidePunishmentForm(); document.getElementById('reason').value = ''; document.getElementById('duration').value = ''; } else { showMessage('Error: ' + (data.error || 'Failed to apply punishment'), 'error'); } })");
        out.println("            .catch(e=>{ console.error('Error:',e); showMessage('Error applying punishment', 'error'); });");
        out.println("        }");
        out.println("        function showMessage(message, type) {");
        out.println("            const icons = { success: 'check-circle', error: 'exclamation-triangle', warning: 'exclamation-circle' };");
        out.println("            document.getElementById('message').innerHTML = `<div class=\"info ${type}\"><i class=\"fas fa-${icons[type]}\"></i> ${message}</div>`;");
        out.println("            setTimeout(() => { document.getElementById('message').innerHTML = ''; }, 5000);");
        out.println("        }");
        out.println("        document.getElementById('player-search').addEventListener('keypress', function(e) { if (e.key === 'Enter') { searchPlayer(); } });");
        out.println("    </script>");
        out.println("</body>");
        out.println("</html>");
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
