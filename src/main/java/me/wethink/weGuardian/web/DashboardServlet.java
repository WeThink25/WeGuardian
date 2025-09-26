package me.wethink.weGuardian.web;

import me.wethink.weGuardian.WeGuardian;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class DashboardServlet extends HttpServlet {

    private final WeGuardian plugin;
    private final SessionManager sessionManager;

    public DashboardServlet(WeGuardian plugin, SessionManager sessionManager) {
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
    if (session == null) {
        response.sendRedirect("/login");
        return;
    }

    response.setContentType("text/html");
    PrintWriter out = response.getWriter();

    out.println("<!DOCTYPE html>");
    out.println("<html lang=\"en\">");
    out.println("<head>");
    out.println("    <meta charset=\"UTF-8\">");
    out.println("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
    out.println("    <title>WeGuardian - Dashboard</title>");
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
    out.println("        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 1.5rem; margin-bottom: 2rem; }");
    out.println("        .stat-card { background: var(--bg-card); border: 1px solid var(--border-color); padding: 2rem;");
    out.println("                     border-radius: 1rem; box-shadow: var(--shadow-lg); text-align: center;");
    out.println("                     transition: all 0.3s ease; position: relative; overflow: hidden; }");
    out.println("        .stat-card::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 4px;");
    out.println("                             background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary)); }");
    out.println("        .stat-card:hover { transform: translateY(-4px); }");
    out.println("        .stat-card .icon { font-size: 2.5rem; margin-bottom: 1rem;");
    out.println("                           background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));");
    out.println("                           -webkit-background-clip: text; -webkit-text-fill-color: transparent; }");
    out.println("        .stat-card h3 { color: var(--text-secondary); margin-bottom: 0.5rem; font-weight: 600;");
    out.println("                        font-size: 0.9rem; text-transform: uppercase; letter-spacing: 0.05em; }");
    out.println("        .stat-card .number { font-size: 2.5rem; font-weight: 700; color: var(--text-primary); margin-bottom: 0.5rem; }");
    out.println("        .stat-card .subtitle { color: var(--text-muted); font-size: 0.8rem; }");
    
    out.println("        .recent-activity { background: var(--bg-card); border: 1px solid var(--border-color);");
    out.println("                           padding: 2rem; border-radius: 1rem; box-shadow: var(--shadow-lg); margin-bottom: 2rem; }");
    out.println("        .recent-activity h3 { margin-bottom: 1.5rem; color: var(--text-primary); font-weight: 700;");
    out.println("                              font-size: 1.25rem; display: flex; align-items: center; gap: 0.5rem; }");
    out.println("        .activity-item { padding: 1.5rem; border-bottom: 1px solid var(--border-color);");
    out.println("                         display: flex; justify-content: space-between; align-items: flex-start;");
    out.println("                         transition: all 0.3s ease; border-radius: 0.5rem; margin-bottom: 0.5rem; }");
    out.println("        .activity-item:last-child { border-bottom: none; margin-bottom: 0; }");
    out.println("        .activity-item:hover { background: var(--bg-tertiary); }");
    out.println("        .activity-info { flex: 1; } .activity-info strong { color: var(--text-primary); }");
    out.println("        .activity-info small { color: var(--text-muted); display: block; margin-top: 0.25rem; }");
    out.println("        .activity-time { color: var(--text-muted); font-size: 0.85rem; white-space: nowrap; margin-left: 1rem; }");
    
    out.println("        .footer { background: var(--bg-card); border-top: 1px solid var(--border-color);");
    out.println("                  padding: 2rem; text-align: center; margin-top: auto; }");
    out.println("        .footer-content { max-width: 1400px; margin: 0 auto; display: flex;");
    out.println("                          flex-direction: column; align-items: center; gap: 1rem; }");
    out.println("        .footer-brand { display: flex; align-items: center; gap: 0.5rem; font-weight: 600; color: var(--text-primary); }");
    out.println("        .footer-brand .heart { color: var(--accent-danger); animation: heartbeat 2s ease-in-out infinite; }");
    out.println("        .footer-copyright { color: var(--text-muted); font-size: 0.9rem; }");
    out.println("        @keyframes heartbeat { 0%, 50%, 100% { transform: scale(1); } 25%, 75% { transform: scale(1.1); } }");
    
    out.println("        .loading { display: inline-flex; align-items: center; gap: 0.5rem; color: var(--text-muted); }");
    out.println("        .loading i { animation: spin 1s linear infinite; }");
    out.println("        @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }");
    out.println("        @media (max-width: 768px) {");
    out.println("            .header { padding: 1rem; flex-direction: column; gap: 1rem; }");
    out.println("            .main-content { padding: 1rem; } .stats-grid { grid-template-columns: 1fr; }");
    out.println("            .activity-item { flex-direction: column; align-items: flex-start; gap: 0.5rem; }");
    out.println("            .activity-time { margin-left: 0; } }");
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
    out.println("            <li><a href=\"/dashboard\" class=\"active\"><i class=\"fas fa-tachometer-alt\"></i> Dashboard</a></li>");
    out.println("            <li><a href=\"/punishments\"><i class=\"fas fa-gavel\"></i> Active Punishments</a></li>");
    out.println("            <li><a href=\"/players\"><i class=\"fas fa-users\"></i> Players</a></li>");
    out.println("            <li><a href=\"/settings\"><i class=\"fas fa-cog\"></i> Settings</a></li>");
    out.println("        </ul>");
    out.println("    </nav>");
    
    out.println("    <div class=\"main-content\">");
    out.println("        <div class=\"stats-grid\">");
    out.println("            <div class=\"stat-card\">");
    out.println("                <div class=\"icon\"><i class=\"fas fa-ban\"></i></div>");
    out.println("                <h3>Active Bans</h3>");
    out.println("                <div class=\"number\" id=\"active-bans\"><span class=\"loading\"><i class=\"fas fa-spinner\"></i> Loading...</span></div>");
    out.println("                <div class=\"subtitle\">Currently banned players</div>");
    out.println("            </div>");
    out.println("            <div class=\"stat-card\">");
    out.println("                <div class=\"icon\"><i class=\"fas fa-volume-mute\"></i></div>");
    out.println("                <h3>Active Mutes</h3>");
    out.println("                <div class=\"number\" id=\"active-mutes\"><span class=\"loading\"><i class=\"fas fa-spinner\"></i> Loading...</span></div>");
    out.println("                <div class=\"subtitle\">Currently muted players</div>");
    out.println("            </div>");
    out.println("            <div class=\"stat-card\">");
    out.println("                <div class=\"icon\"><i class=\"fas fa-chart-line\"></i></div>");
    out.println("                <h3>Total Punishments</h3>");
    out.println("                <div class=\"number\" id=\"total-punishments\"><span class=\"loading\"><i class=\"fas fa-spinner\"></i> Loading...</span></div>");
    out.println("                <div class=\"subtitle\">All time punishments</div>");
    out.println("            </div>");
    out.println("            <div class=\"stat-card\">");
    out.println("                <div class=\"icon\"><i class=\"fas fa-users\"></i></div>");
    out.println("                <h3>Online Players</h3>");
    out.println("                <div class=\"number\" id=\"online-players\"><span class=\"loading\"><i class=\"fas fa-spinner\"></i> Loading...</span></div>");
    out.println("                <div class=\"subtitle\">Currently online</div>");
    out.println("            </div>");
    out.println("        </div>");
    
    out.println("        <div class=\"recent-activity\">");
    out.println("            <h3><i class=\"fas fa-history\"></i> Recent Activity</h3>");
    out.println("            <div id=\"recent-activity-list\">");
    out.println("                <div class=\"activity-item\">");
    out.println("                    <div class=\"activity-info\">");
    out.println("                        <span class=\"loading\"><i class=\"fas fa-spinner\"></i> Loading recent activity...</span>");
    out.println("                    </div>");
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
    out.println("        function loadStats() {");
    out.println("            fetch('/api/stats').then(r=>r.json()).then(data=>{");
    out.println("                document.getElementById('active-bans').textContent = data.active_bans || 0;");
    out.println("                document.getElementById('active-mutes').textContent = data.active_mutes || 0;");
    out.println("                document.getElementById('total-punishments').textContent = data.total_punishments || 0;");
    out.println("                document.getElementById('online-players').textContent = data.online_players || 0;");
    out.println("            }).catch(e=>{");
    out.println("                console.error('Error loading stats:',e);");
    out.println("                ['active-bans','active-mutes','total-punishments','online-players'].forEach(id=>{");
    out.println("                    document.getElementById(id).innerHTML = '<span style=\"color: var(--accent-danger);\">Error</span>';");
    out.println("                });");
    out.println("            });");
    out.println("        }");
    out.println("        function loadRecentActivity() {");
    out.println("            fetch('/api/recent-activity').then(r=>r.json()).then(data=>{");
    out.println("                if(data.success){ displayRecentActivity(data.activities); }");
    out.println("                else { document.getElementById('recent-activity-list').innerHTML='<div class=\"activity-item\"><div class=\"activity-info\" style=\"color: var(--accent-danger);\"><i class=\"fas fa-exclamation-triangle\"></i> Error loading recent activity</div></div>'; }");
    out.println("            }).catch(e=>{ console.error('Error loading recent activity:',e); document.getElementById('recent-activity-list').innerHTML='<div class=\"activity-item\"><div class=\"activity-info\" style=\"color: var(--accent-danger);\"><i class=\"fas fa-exclamation-triangle\"></i> Error loading recent activity</div></div>'; });");
    out.println("        }");
    out.println("        function displayRecentActivity(activities){");
    out.println("            if(activities.length===0){ document.getElementById('recent-activity-list').innerHTML='<div class=\"activity-item\"><div class=\"activity-info\" style=\"color: var(--text-muted);\"><i class=\"fas fa-info-circle\"></i> No recent activity</div></div>'; return; }");
    out.println("            let html=''; activities.forEach(a=>{ const date=new Date(a.createdAt).toLocaleString(); html+=`<div class=\"activity-item\"><div class=\"activity-info\"><strong>${a.type}:</strong> ${a.targetName} by ${a.staffName}<small>${a.reason}</small></div><div class=\"activity-time\">${date}</div></div>`; });");
    out.println("            document.getElementById('recent-activity-list').innerHTML=html;");
    out.println("        }");
    out.println("        function connectWebSocket(){");
    out.println("            const protocol=window.location.protocol==='https:'?'wss:':'ws:'; const ws=new WebSocket(protocol+'//'+window.location.host+'/ws');");
    out.println("            ws.onmessage=function(event){ const data=JSON.parse(event.data); if(data.type==='stats_update'){ document.getElementById('active-bans').textContent=data.active_bans||0; document.getElementById('active-mutes').textContent=data.active_mutes||0; document.getElementById('total-punishments').textContent=data.total_punishments||0; loadRecentActivity(); } };");
    out.println("            ws.onclose=function(){ setTimeout(connectWebSocket,5000); };");
    out.println("        }");
    out.println("        loadStats(); loadRecentActivity(); connectWebSocket(); setInterval(loadStats, 10000); setInterval(loadRecentActivity, 30000);");
    out.println("    </script>");
    out.println("</body>");
    out.println("</html>");
}
private String getSessionIdFromCookies(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if(cookies!=null){
        for(Cookie c : cookies){
            if("sessionId".equals(c.getName())) return c.getValue();
        }
    }
    return null;
}

private String escapeHtml(String input){
    return input.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#x27;");
}
}