package me.wethink.weGuardian.web;

import me.wethink.weGuardian.WeGuardian;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class LoginServlet extends HttpServlet {
    
    private final WeGuardian plugin;
    private final SessionManager sessionManager;
    
    public LoginServlet(WeGuardian plugin, SessionManager sessionManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
    }
    
@Override
protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String sessionId = getSessionIdFromCookies(request);
    
    if (sessionId != null && sessionManager.isValidSession(sessionId)) {
        response.sendRedirect("/dashboard");
        return;
    }
    
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    
    out.println("<!DOCTYPE html>");
    out.println("<html lang=\"en\">");
    out.println("<head>");
    out.println("    <meta charset=\"UTF-8\">");
    out.println("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
    out.println("    <title>WeGuardian - Login</title>");
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
    out.println("               color: var(--text-primary); min-height: 100vh; display: flex; align-items: center; justify-content: center; }");
    out.println("        .login-container { background: var(--bg-card); border: 1px solid var(--border-color);");
    out.println("                           padding: 3rem; border-radius: 1rem; box-shadow: var(--shadow-lg);");
    out.println("                           width: 100%; max-width: 450px; backdrop-filter: blur(10px); }");
    out.println("        .login-header { text-align: center; margin-bottom: 2.5rem; }");
    out.println("        .login-header .logo { font-size: 3rem; margin-bottom: 1rem;");
    out.println("                              background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));");
    out.println("                              -webkit-background-clip: text; -webkit-text-fill-color: transparent; }");
    out.println("        .login-header h1 { color: var(--text-primary); margin: 0; font-size: 2rem; font-weight: 700;");
    out.println("                           background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));");
    out.println("                           -webkit-background-clip: text; -webkit-text-fill-color: transparent; }");
    out.println("        .login-header p { color: var(--text-secondary); margin: 0.5rem 0 0 0; }");
    out.println("        .form-group { margin-bottom: 1.5rem; }");
    out.println("        .form-group label { display: block; margin-bottom: 0.5rem; color: var(--text-primary); font-weight: 600; }");
    out.println("        .form-group input { width: 100%; padding: 1rem; background: var(--bg-tertiary);");
    out.println("                            border: 1px solid var(--border-color); border-radius: 0.75rem;");
    out.println("                            font-size: 1rem; color: var(--text-primary); transition: all 0.3s ease; }");
    out.println("        .form-group input:focus { outline: none; border-color: var(--accent-primary); box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1); }");
    out.println("        .login-btn { width: 100%; padding: 1rem; background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));");
    out.println("                     border: none; border-radius: 0.75rem; color: white; font-size: 1rem; font-weight: 600;");
    out.println("                     cursor: pointer; transition: all 0.3s ease; display: flex; align-items: center; justify-content: center; gap: 0.5rem; }");
    out.println("        .login-btn:hover { transform: translateY(-2px); box-shadow: var(--shadow-lg); }");
    out.println("        .error-message { background: rgba(239, 68, 68, 0.1); border: 1px solid var(--accent-danger);");
    out.println("                         color: var(--accent-danger); padding: 1rem; border-radius: 0.75rem; margin-bottom: 1.5rem;");
    out.println("                         display: flex; align-items: center; gap: 0.5rem; }");
    out.println("        .footer { position: absolute; bottom: 2rem; left: 50%; transform: translateX(-50%);");
    out.println("                  text-align: center; color: var(--text-muted); }");
    out.println("        .footer-brand { display: flex; align-items: center; gap: 0.5rem; font-weight: 600; justify-content: center; }");
    out.println("        .footer-brand .heart { color: var(--accent-danger); animation: heartbeat 2s ease-in-out infinite; }");
    out.println("        @keyframes heartbeat { 0%, 50%, 100% { transform: scale(1); } 25%, 75% { transform: scale(1.1); } }");
    out.println("        @media (max-width: 480px) { .login-container { padding: 2rem; margin: 1rem; } }");
    out.println("    </style>");
    out.println("</head>");
    out.println("<body>");
    out.println("    <div class=\"login-container\">");
    out.println("        <div class=\"login-header\">");
    out.println("            <div class=\"logo\"><i class=\"fas fa-shield-alt\"></i></div>");
    out.println("            <h1>WeGuardian</h1>");
    out.println("            <p>Secure Admin Access</p>");
    out.println("        </div>");
    
    String error = request.getParameter("error");
    if (error != null) {
        out.println("        <div class=\"error-message\">");
        out.println("            <i class=\"fas fa-exclamation-triangle\"></i>");
        out.println("            <span>Invalid username or password. Please try again.</span>");
        out.println("        </div>");
    }
    
    out.println("        <form method=\"post\" action=\"/login\">");
    out.println("            <div class=\"form-group\">");
    out.println("                <label for=\"username\"><i class=\"fas fa-user\"></i> Username</label>");
    out.println("                <input type=\"text\" id=\"username\" name=\"username\" required>");
    out.println("            </div>");
    out.println("            <div class=\"form-group\">");
    out.println("                <label for=\"password\"><i class=\"fas fa-lock\"></i> Password</label>");
    out.println("                <input type=\"password\" id=\"password\" name=\"password\" required>");
    out.println("            </div>");
    out.println("            <button type=\"submit\" class=\"login-btn\">");
    out.println("                <i class=\"fas fa-sign-in-alt\"></i>");
    out.println("                <span>Login</span>");
    out.println("            </button>");
    out.println("        </form>");
    out.println("    </div>");
    out.println("    <div class=\"footer\">");
    out.println("        <div class=\"footer-brand\">");
    out.println("            <span>Made with</span>");
    out.println("            <i class=\"fas fa-heart heart\"></i>");
    out.println("            <span>by <strong>WeThink</strong></span>");
    out.println("        </div>");
    out.println("    </div>");
    out.println("</body>");
    out.println("</html>");
}

@Override
protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String username = request.getParameter("username");
    String password = request.getParameter("password");
    
    if (username == null || password == null) {
        response.sendRedirect("/login?error=" + java.net.URLEncoder.encode("Username and password are required", "UTF-8"));
        return;
    }
    
    if (sessionManager.isAccountLocked(username)) {
        response.sendRedirect("/login?error=" + java.net.URLEncoder.encode("Account is temporarily locked due to too many failed attempts", "UTF-8"));
        return;
    }
    
    if (sessionManager.authenticate(username, password)) {
        String sessionId = sessionManager.createSession(username);
        
        Cookie sessionCookie = new Cookie("sessionId", sessionId);
        sessionCookie.setHttpOnly(true);
        sessionCookie.setMaxAge(plugin.getConfig().getInt("web-dashboard.session-timeout", 3600));
        sessionCookie.setPath("/");
        response.addCookie(sessionCookie);
        
        response.sendRedirect("/dashboard");
    } else {
        response.sendRedirect("/login?error=" + java.net.URLEncoder.encode("Invalid username or password", "UTF-8"));
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