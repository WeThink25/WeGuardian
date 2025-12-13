package me.wethink.weguardian.model;

import java.time.Instant;
import java.util.UUID;


public class Punishment {

    private int id;
    private UUID targetUUID;
    private String targetName;
    private String targetIp;
    private UUID staffUUID;
    private String staffName;
    private PunishmentType type;
    private String reason;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean active;
    private UUID removedByUUID;
    private String removedByName;
    private Instant removedAt;
    private String removeReason;

    public Punishment() {
    }

    public Punishment(UUID targetUUID, String targetName, UUID staffUUID, String staffName,
            PunishmentType type, String reason, Instant createdAt, Instant expiresAt) {
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.staffUUID = staffUUID;
        this.staffName = staffName;
        this.type = type;
        this.reason = reason;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.active = true;
    }

    public Punishment(UUID targetUUID, String targetName, String targetIp, UUID staffUUID, String staffName,
            PunishmentType type, String reason, Instant createdAt, Instant expiresAt) {
        this(targetUUID, targetName, staffUUID, staffName, type, reason, createdAt, expiresAt);
        this.targetIp = targetIp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UUID getTargetUUID() {
        return targetUUID;
    }

    public void setTargetUUID(UUID targetUUID) {
        this.targetUUID = targetUUID;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    public UUID getStaffUUID() {
        return staffUUID;
    }

    public void setStaffUUID(UUID staffUUID) {
        this.staffUUID = staffUUID;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public UUID getRemovedByUUID() {
        return removedByUUID;
    }

    public void setRemovedByUUID(UUID removedByUUID) {
        this.removedByUUID = removedByUUID;
    }

    public String getRemovedByName() {
        return removedByName;
    }

    public void setRemovedByName(String removedByName) {
        this.removedByName = removedByName;
    }

    public Instant getRemovedAt() {
        return removedAt;
    }

    public void setRemovedAt(Instant removedAt) {
        this.removedAt = removedAt;
    }

    public String getRemoveReason() {
        return removeReason;
    }

    public void setRemoveReason(String removeReason) {
        this.removeReason = removeReason;
    }


    public boolean isPermanent() {
        return expiresAt == null;
    }


    public boolean isExpired() {
        if (isPermanent()) {
            return false;
        }
        return Instant.now().isAfter(expiresAt);
    }


    public long getRemainingMillis() {
        if (isPermanent()) {
            return -1;
        }
        long remaining = expiresAt.toEpochMilli() - Instant.now().toEpochMilli();
        return Math.max(0, remaining);
    }

    @Override
    public String toString() {
        return "Punishment{" +
                "id=" + id +
                ", targetName='" + targetName + '\'' +
                ", type=" + type +
                ", reason='" + reason + '\'' +
                ", active=" + active +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
