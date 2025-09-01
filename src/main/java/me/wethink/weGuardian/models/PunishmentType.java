package me.wethink.weGuardian.models;

public enum PunishmentType {
    BAN("Ban", true),
    TEMPBAN("Tempban", false),
    MUTE("Mute", true),
    TEMPMUTE("Tempmute", false),
    KICK("Kick", false),
    WARN("Warn", false),
    NOTE("Note", false),
    UNBAN("Unban", false),
    UNMUTE("Unmute", false),
    IPBAN("IP Ban", true),
    IPTEMPBAN("IP Tempban", false),
    IPMUTE("IP Mute", true),
    IPTEMPMUTE("IP Tempmute", false),
    IPKICK("IP Kick", false),
    IPWARN("IP Warn", false),
    UNBANIP("Unban IP", false),
    UNMUTEIP("Unmute IP", false);

    private final String displayName;
    private final boolean permanent;

    PunishmentType(String displayName, boolean permanent) {
        this.displayName = displayName;
        this.permanent = permanent;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isPermanent() {
        return permanent;
    }

    public boolean isTemporary() {
        return !permanent;
    }
}
