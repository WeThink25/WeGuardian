package me.wethink.weGuardian.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class BanwaveEntry {
    private int id;
    private UUID targetUuid;
    private String targetName;
    private UUID staffUuid;
    private String staffName;
    private String reason;
    private LocalDateTime createdAt;
    private boolean executed;
    private LocalDateTime executedAt;

    public BanwaveEntry() {
    }

    public BanwaveEntry(UUID targetUuid, String targetName, UUID staffUuid, String staffName, String reason) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.staffUuid = staffUuid;
        this.staffName = staffName;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
        this.executed = false;
    }

    public BanwaveEntry(int id, UUID targetUuid, String targetName, UUID staffUuid, String staffName, String reason, LocalDateTime createdAt, boolean executed) {
        this.id = id;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.staffUuid = staffUuid;
        this.staffName = staffName;
        this.reason = reason;
        this.createdAt = createdAt;
        this.executed = executed;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public void setTargetUuid(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public UUID getStaffUuid() {
        return staffUuid;
    }

    public void setStaffUuid(UUID staffUuid) {
        this.staffUuid = staffUuid;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isExecuted() {
        return executed;
    }

    public void setExecuted(boolean executed) {
        this.executed = executed;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public void execute() {
        this.executed = true;
        this.executedAt = LocalDateTime.now();
    }

    public String getPlayerName() {
        return targetName;
    }

    public void setPlayerName(String playerName) {
        this.targetName = playerName;
    }
}
