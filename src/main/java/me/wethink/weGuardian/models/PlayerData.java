package me.wethink.weGuardian.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class PlayerData {
    private UUID uuid;
    private String name;
    private String ipAddress;
    private LocalDateTime firstJoin;
    private LocalDateTime lastSeen;
    private long firstJoinTimestamp;
    private long lastJoinTimestamp;
    private String lastIP;
    private boolean banned;
    private boolean muted;
    private boolean warned;
    private boolean noted;
    private boolean kicked;
    private Punishment activeBan;
    private Punishment activeMute;

    public PlayerData() {
    }

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.firstJoin = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
        this.firstJoinTimestamp = System.currentTimeMillis();
        this.lastJoinTimestamp = System.currentTimeMillis();
        this.banned = false;
        this.muted = false;
        this.warned = false;
        this.noted = false;
        this.kicked = false;
    }

    public PlayerData(UUID uuid, String name, String ipAddress) {
        this.uuid = uuid;
        this.name = name;
        this.ipAddress = ipAddress;
        this.firstJoin = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
        this.firstJoinTimestamp = System.currentTimeMillis();
        this.lastJoinTimestamp = System.currentTimeMillis();
        this.lastIP = ipAddress;
        this.banned = false;
        this.muted = false;
        this.warned = false;
        this.noted = false;
        this.kicked = false;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlayerName() {
        return name;
    }

    public void setPlayerName(String playerName) {
        this.name = playerName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getFirstJoin() {
        return firstJoin;
    }

    public void setFirstJoin(LocalDateTime firstJoin) {
        this.firstJoin = firstJoin;
    }

    public void setFirstJoin(long firstJoinTimestamp) {
        this.firstJoinTimestamp = firstJoinTimestamp;
        this.firstJoin = new java.sql.Timestamp(firstJoinTimestamp).toLocalDateTime();
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public long getFirstJoinTimestamp() {
        return firstJoinTimestamp;
    }

    public long getLastJoinTimestamp() {
        return lastJoinTimestamp;
    }

    public LocalDateTime getLastJoin() {
        return new java.sql.Timestamp(lastJoinTimestamp).toLocalDateTime();
    }

    public void setLastJoin(long lastJoinTimestamp) {
        this.lastJoinTimestamp = lastJoinTimestamp;
    }

    public String getLastIP() {
        return lastIP;
    }

    public void setLastIP(String lastIP) {
        this.lastIP = lastIP;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isWarned() {
        return warned;
    }

    public void setWarned(boolean warned) {
        this.warned = warned;
    }

    public boolean isNoted() {
        return noted;
    }

    public void setNoted(boolean noted) {
        this.noted = noted;
    }

    public boolean isKicked() {
        return kicked;
    }

    public void setKicked(boolean kicked) {
        this.kicked = kicked;
    }

    public Punishment getActiveBan() {
        return activeBan;
    }

    public void setActiveBan(Punishment activeBan) {
        this.activeBan = activeBan;
        this.banned = activeBan != null && activeBan.isActive() && !activeBan.isExpired();
    }

    public Punishment getActiveMute() {
        return activeMute;
    }

    public void setActiveMute(Punishment activeMute) {
        this.activeMute = activeMute;
        this.muted = activeMute != null && activeMute.isActive() && !activeMute.isExpired();
    }

    public void updateLastSeen() {
        this.lastSeen = LocalDateTime.now();
        this.lastJoinTimestamp = System.currentTimeMillis();
    }

    public boolean hasActivePunishment(PunishmentType type) {
        switch (type) {
            case BAN:
            case TEMPBAN:
                return banned && activeBan != null && activeBan.isActive() && !activeBan.isExpired();
            case MUTE:
            case TEMPMUTE:
                return muted && activeMute != null && activeMute.isActive() && !activeMute.isExpired();
            case WARN:
                return warned;
            case NOTE:
                return noted;
            case KICK:
                return kicked;
            default:
                return false;
        }
    }

    public void setIpBanned(boolean ipBanned) {
        this.banned = ipBanned;
    }

    public void setIpMuted(boolean ipMuted) {
        this.muted = ipMuted;
    }

    public void setIpWarned(boolean ipWarned) {
        this.warned = ipWarned;
    }

    public void setIpKicked(boolean ipKicked) {
        this.kicked = ipKicked;
    }
}
