package me.wethink.weGuardian.models;

import java.util.UUID;

public class PlayerConnection {
    public final UUID uuid;
    public final String playerName;
    public final String ip;
    public final long timestamp;

    public PlayerConnection(UUID uuid, String playerName, String ip, long timestamp) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.ip = ip;
        this.timestamp = timestamp;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getIp() {
        return ip;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
