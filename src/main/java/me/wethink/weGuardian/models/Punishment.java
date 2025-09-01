package me.wethink.weGuardian.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class Punishment {
    private int id;
    private UUID targetUuid;
    private String targetName;
    private String targetIP;
    private UUID staffUuid;
    private String staffName;
    private PunishmentType type;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean active;
    private UUID removedByUuid;
    private String removedByName;
    private LocalDateTime removedAt;
    private String removalReason;
    private String serverName;

    public Punishment() {
    }

    public Punishment(UUID targetUuid, String targetName, UUID staffUuid, String staffName,
                      PunishmentType type, String reason, LocalDateTime expiresAt, String serverName) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.staffUuid = staffUuid;
        this.staffName = staffName;
        this.type = type;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.active = true;
        this.serverName = serverName;
    }

    public Punishment(String targetIP, UUID staffUuid, String staffName,
                      PunishmentType type, String reason, LocalDateTime expiresAt, String serverName) {
        this.targetIP = targetIP;
        this.targetName = targetIP;
        this.staffUuid = staffUuid;
        this.staffName = staffName;
        this.type = type;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.active = true;
        this.serverName = serverName;
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

    public String getTargetIP() {
        return targetIP;
    }

    public void setTargetIP(String targetIP) {
        this.targetIP = targetIP;
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

    public PunishmentType getType() {
        return type;
    }

    public void setType(PunishmentType type) {
        this.type = type;
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

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public UUID getRemovedByUuid() {
        return removedByUuid;
    }

    public void setRemovedByUuid(UUID removedByUuid) {
        this.removedByUuid = removedByUuid;
    }

    public String getRemovedByName() {
        return removedByName;
    }

    public void setRemovedByName(String removedByName) {
        this.removedByName = removedByName;
    }

    public String getRemovedBy() {
        return removedByName;
    }

    public void setRemovedBy(String removedBy) {
        this.removedByName = removedBy;
    }

    public LocalDateTime getRemovedAt() {
        return removedAt;
    }

    public void setRemovedAt(LocalDateTime removedAt) {
        this.removedAt = removedAt;
    }

    public String getRemovalReason() {
        return removalReason;
    }

    public void setRemovalReason(String removalReason) {
        this.removalReason = removalReason;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isPermanent() {
        return expiresAt == null;
    }

    public boolean isIPBased() {
        return targetIP != null && !targetIP.isEmpty();
    }

    public String getDisplayTarget() {
        return isIPBased() ? targetIP : targetName;
    }

    public void remove(UUID removedBy, String removedByName, String reason) {
        this.active = false;
        this.removedByUuid = removedBy;
        this.removedByName = removedByName;
        this.removedAt = LocalDateTime.now();
        this.removalReason = reason;
    }
}
