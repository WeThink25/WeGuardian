package me.wethink.weGuardian.web;

import me.wethink.weGuardian.WeGuardian;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    
    private final WeGuardian plugin;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, Integer> loginAttempts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lockouts = new ConcurrentHashMap<>();
    
    public SessionManager(WeGuardian plugin) {
        this.plugin = plugin;
    }
    
    public boolean authenticate(String username, String password) {
        String configUsername = plugin.getConfig().getString("web-dashboard.credentials.username", "admin");
        String configPassword = plugin.getConfig().getString("web-dashboard.credentials.password", "supersecret");
        
        if (username.equals(configUsername) && password.equals(configPassword)) {
            return true;
        }
        
        recordFailedLogin(username);
        return false;
    }
    
    public String createSession(String username) {
        String sessionId = UUID.randomUUID().toString();
        long timeoutSeconds = plugin.getConfig().getLong("web-dashboard.session-timeout", 3600);
        
        Session session = new Session(sessionId, username, LocalDateTime.now().plusSeconds(timeoutSeconds));
        sessions.put(sessionId, session);
        
        return sessionId;
    }
    
    public boolean isValidSession(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            return false;
        }
        
        if (session.isExpired()) {
            sessions.remove(sessionId);
            return false;
        }
        
        return true;
    }
    
    public void invalidateSession(String sessionId) {
        sessions.remove(sessionId);
    }
    
    public Session getSession(String sessionId) {
        return sessions.get(sessionId);
    }
    
    public void cleanupExpiredSessions() {
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
        lockouts.entrySet().removeIf(entry -> entry.getValue().isBefore(LocalDateTime.now().minusMinutes(5)));
    }
    
    private void recordFailedLogin(String username) {
        loginAttempts.put(username, loginAttempts.getOrDefault(username, 0) + 1);
        
        int maxAttempts = plugin.getConfig().getInt("web-dashboard.security.max-login-attempts", 5);
        if (loginAttempts.get(username) >= maxAttempts) {
            int lockoutDuration = plugin.getConfig().getInt("web-dashboard.security.lockout-duration", 300);
            lockouts.put(username, LocalDateTime.now().plusSeconds(lockoutDuration));
            plugin.getLogger().warning("Account locked for " + username + " due to too many failed login attempts");
        }
    }
    
    public boolean isAccountLocked(String username) {
        LocalDateTime lockoutTime = lockouts.get(username);
        if (lockoutTime == null) {
            return false;
        }
        
        if (lockoutTime.isBefore(LocalDateTime.now())) {
            lockouts.remove(username);
            loginAttempts.remove(username);
            return false;
        }
        
        return true;
    }
    
    public static class Session {
        private final String sessionId;
        private final String username;
        private final LocalDateTime expiresAt;
        
        public Session(String sessionId, String username, LocalDateTime expiresAt) {
            this.sessionId = sessionId;
            this.username = username;
            this.expiresAt = expiresAt;
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public String getUsername() {
            return username;
        }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }
}
