package me.wethink.weguardian.model;

public enum PunishmentType {
    BAN("Ban", "§c", true),
    TEMPBAN("Temporary Ban", "§6", true),
    BANIP("IP Ban", "§4", true),
    TEMPBANIP("Temporary IP Ban", "§c", true),
    MUTE("Mute", "§e", false),
    TEMPMUTE("Temporary Mute", "§a", false),
    MUTEIP("IP Mute", "§6", false),
    TEMPMUTEIP("Temporary IP Mute", "§e", false),
    KICK("Kick", "§9", false);

    private final String displayName;
    private final String color;
    private final boolean preventLogin;

    PunishmentType(String displayName, String color, boolean preventLogin) {
        this.displayName = displayName;
        this.color = color;
        this.preventLogin = preventLogin;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    public boolean preventsLogin() {
        return preventLogin;
    }

    public boolean isTemporary() {
        return this == TEMPBAN || this == TEMPMUTE || this == TEMPBANIP || this == TEMPMUTEIP;
    }

    public boolean isBan() {
        return this == BAN || this == TEMPBAN;
    }

    public boolean isMute() {
        return this == MUTE || this == TEMPMUTE;
    }

    public boolean isIpBan() {
        return this == BANIP || this == TEMPBANIP;
    }

    public boolean isIpMute() {
        return this == MUTEIP || this == TEMPMUTEIP;
    }

    public boolean isIpBased() {
        return isIpBan() || isIpMute();
    }

    public String getPermission() {
        return switch (this) {
            case BAN -> "weguardian.ban";
            case TEMPBAN -> "weguardian.tempban";
            case BANIP -> "weguardian.banip";
            case TEMPBANIP -> "weguardian.tempbanip";
            case MUTE -> "weguardian.mute";
            case TEMPMUTE -> "weguardian.tempmute";
            case MUTEIP -> "weguardian.muteip";
            case TEMPMUTEIP -> "weguardian.tempmuteip";
            case KICK -> "weguardian.kick";
        };
    }
}
