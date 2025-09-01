# WeGuardian - Advanced Minecraft Punishment System

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Paper](https://img.shields.io/badge/Paper-1.21.1-blue.svg)](https://papermc.io/)
[![Folia](https://img.shields.io/badge/Folia-Support%20Soon-yellow.svg)](https://papermc.io/software/folia)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![bStats](https://img.shields.io/badge/bStats-27046-brightgreen.svg)](https://bstats.org/plugin/bukkit/WeGuardian/27046)
[![Discord](https://img.shields.io/badge/Discord-WeGuardian-blue.svg)](https://discord.gg/aSWKs3k4XA)
[![GitHub](https://img.shields.io/badge/GitHub-WeThink25/WeGuardian-blue.svg)](https://github.com/WeThink25/WeGuardian)

WeGuardian is a comprehensive, professional-grade Minecraft punishment system plugin designed for modern Paper/Bukkit servers. It provides a complete suite of moderation tools with GUI-driven interfaces, advanced punishment templates, and IP-based punishments.

## Key Features

### Core Punishment System

- **GUI-Driven Interface**: Intuitive `/punish <player>` command opens comprehensive punishment center
- **Complete Punishment Types**: Ban, Tempban, Mute, Tempmute, Kick, Warn, Unban, Unmute, Notes
- **IP-Based Punishments**: IP bans, IP mutes with automatic player correlation
- **Smart Templates**: Advanced punishment escalation system with auto-progression
- **Silent Punishments**: Execute punishments without broadcasting with `-s` flag
- **Advanced Validation**: Duplicate punishment prevention and permission hierarchy checks

### Advanced Features

- **Punishment Templates**: Automated escalation system with configurable offense levels
- **Advanced Commands**: `/banlist`, `/mutelist`, `/alts`, `/blame`, `/warns`, `/unwarn`
- **IP Correlation**: Advanced alt account detection with shared IP analysis
- **Punishment History**: Complete audit trail with clickable punishment IDs
- **Template Integration**: Smart punishment progression based on player history
- **Rich Formatting**: Adventure API with hover tooltips and clickable elements

### Database & Storage

- **YAML Storage**: Primary storage system with individual player files
- **MySQL Support**: Coming soon - Cross-server MySQL with HikariCP pooling
- **SQLite Support**: Coming soon - Lightweight single-server option
- **Async Operations**: All database operations are asynchronous for optimal performance
- **Data Integrity**: Comprehensive backup system and data validation

### Advanced GUI System

- **Comprehensive Main Menu**: All-in-one punishment interface with direct execution
- **Pre-configured Actions**: Common punishment scenarios with predefined reasons/durations
- **Template Integration**: Smart punishment templates accessible from GUI
- **Player Information**: Quick access to history, ban status, and notes
- **Professional Layout**: 54-slot organized interface with color-coded severity levels

### Performance & Compatibility

- **Folia Support**: Coming soon - Full Folia compatibility with region-aware scheduling
- **FoliaLib Integration**: Modern scheduler API for cross-platform compatibility
- **Thread Pool Optimization**: Configurable async operations with performance tuning
- **Platform Detection**: Automatic optimization for Paper/Spigot vs Folia servers
- **bStats Metrics**: Plugin usage analytics and performance monitoring

## Commands Overview

### Core Punishment Commands

| Command                              | Description               | Permission            |
|--------------------------------------|---------------------------|-----------------------|
| `/punish <player>`                   | Open punishment GUI       | `weguardian.punish`   |
| `/ban <player> <reason> [-s] [-t template]` | Permanently ban a player  | `weguardian.ban`      |
| `/tempban <player> <time> <reason> [-s] [-t template]` | Temporarily ban a player  | `weguardian.tempban`  |
| `/mute <player> <reason> [-s] [-t template] [--ip]` | Permanently mute a player | `weguardian.mute`     |
| `/tempmute <player> <time> <reason> [-s] [-t template] [--ip]` | Temporarily mute a player | `weguardian.tempmute` |
| `/kick <player> <reason> [-s] [-t template] [--ip]` | Kick a player             | `weguardian.kick`     |
| `/warn <player> <reason> [-s] [-t template]` | Warn a player             | `weguardian.warn`     |
| `/unban <player>`                    | Unban a player            | `weguardian.unban`    |
| `/unmute <player>`                   | Unmute a player           | `weguardian.unmute`   |

### IP-Based Punishment Commands

| Command                              | Description               | Permission            |
|--------------------------------------|---------------------------|-----------------------|
| `/ipban <ip> <reason>`               | Ban IP address            | `weguardian.ipban`    |
| `/ipmute <ip> <reason>`              | Mute IP address           | `weguardian.ipmute`   |
| `/unbanip <ip>`                      | Unban IP address          | `weguardian.unbanip`  |
| `/unmuteip <ip>`                     | Unmute IP address         | `weguardian.unmuteip` |

### Advanced Commands

| Command                              | Description               | Permission            |
|--------------------------------------|---------------------------|-----------------------|
| `/banlist [--ip] [--filter]`        | List all bans with pagination | `weguardian.banlist`  |
| `/mutelist [--ip] [--filter]`       | List all mutes with pagination | `weguardian.mutelist` |
| `/alts <player> [--strict] [--punished]` | Find alt accounts         | `weguardian.alts`     |
| `/blame <id>`                        | Show punishment details   | `weguardian.blame`    |
| `/warns <player>`                    | Show player warnings      | `weguardian.warns`    |
| `/unwarn <id> <reason>`              | Remove specific warning   | `weguardian.unwarn`   |

### Information & History Commands

| Command                | Description             | Permission               |
|------------------------|-------------------------|--------------------------|
| `/history <player>`    | View punishment history | `weguardian.history`     |
| `/checkban <player>`   | Check ban/mute status   | `weguardian.checkban`    |
| `/notes <player>`      | View staff notes        | `weguardian.notes`       |
| `/staffstatus <staff>` | View staff statistics   | `weguardian.staffstatus` |
| `/stats`               | View global statistics  | `weguardian.stats`       |

### Admin & Management Commands

| Command                                    | Description               | Permission            |
|--------------------------------------------|---------------------------|-----------------------|
| `/rollback <id> [reason]`                  | Rollback punishment by ID | `weguardian.rollback` |
| `/banwave <add/remove/list/execute/clear>` | Manage banwave queue      | `weguardian.banwave`  |
| `/weguardian <reload/about/version/help>`  | Plugin management         | `weguardian.admin`    |

### GUI Navigation Commands

| Command                 | Description                | Permission                |
|-------------------------|----------------------------|---------------------------|
| `/banmenu <player>`     | Open ban GUI directly      | `weguardian.gui.ban`      |
| `/tempbanmenu <player>` | Open tempban GUI directly  | `weguardian.gui.tempban`  |
| `/mutemenu <player>`    | Open mute GUI directly     | `weguardian.gui.mute`     |
| `/tempmutemenu <player>` | Open tempmute GUI directly | `weguardian.gui.tempmute` |
| `/warnmenu <player>`    | Open warn GUI directly     | `weguardian.gui.warn`     |
| `/kickmenu <player>`    | Open kick GUI directly     | `weguardian.gui.kick`     |
| `/notesmenu <player>`   | Open notes GUI directly    | `weguardian.gui.notes`    |
| `/unbanmenu <player>`   | Open unban GUI directly    | `weguardian.gui.unban`    |
| `/unmutemenu <player>`  | Open unmute GUI directly   | `weguardian.gui.unmute`   |

## Installation

### Requirements

- **Java 21** or higher
- **Paper/Bukkit 1.21.1** or compatible
- **Folia Support**: Coming soon with FoliaLib integration
- **PlaceholderAPI** (optional but recommended)

### Setup Steps

1. **Download & Install**
   ```bash
   wget https://github.com/WeThink25/WeGuardian/releases/latest/download/WeGuardian.jar
   ```

2. **First Run**
   ```bash
   # Start your server to generate default configuration
   # Plugin will create config.yml, messages.yml, templates.yml, and GUI files automatically
   ```

3. **Configure Storage**
   ```yaml
   storage:
     type: "yaml"
   ```

4. **Set Permissions**
   ```yaml
   groups:
     moderator:
       permissions:
         - weguardian.punish
         - weguardian.tempban
         - weguardian.tempmute
         - weguardian.kick
         - weguardian.warn
         - weguardian.history
         - weguardian.checkban
         - weguardian.gui.*
     admin:
       permissions:
         - weguardian.*
         - weguardian.bypass.override
   ```

## Permissions System

### Core Permissions

```yaml
weguardian.punish          # Access to /punish command
weguardian.ban             # Permanent ban permission
weguardian.tempban         # Temporary ban permission
weguardian.mute            # Permanent mute permission
weguardian.tempmute        # Temporary mute permission
weguardian.kick            # Kick permission
weguardian.warn            # Warning permission
weguardian.unban           # Unban permission
weguardian.unmute          # Unmute permission

weguardian.ipban           # IP ban permission
weguardian.ipmute          # IP mute permission
weguardian.unbanip         # IP unban permission
weguardian.unmuteip        # IP unmute permission

weguardian.banlist         # View ban lists
weguardian.mutelist        # View mute lists
weguardian.alts            # Alt account detection
weguardian.blame           # View punishment details
weguardian.warns           # View warnings
weguardian.unwarn          # Remove warnings

weguardian.history         # View punishment history
weguardian.checkban        # Check ban/mute status
weguardian.notes           # View staff notes
weguardian.staffstatus     # View staff statistics
weguardian.stats           # View global statistics

weguardian.rollback        # Rollback punishments
weguardian.banwave         # Manage banwave system
weguardian.admin           # Plugin administration

weguardian.gui.*           # All GUI access
weguardian.gui.ban         # Ban GUI access
weguardian.gui.tempban     # Tempban GUI access
weguardian.gui.mute        # Mute GUI access
weguardian.gui.tempmute    # Tempmute GUI access
weguardian.gui.warn        # Warn GUI access
weguardian.gui.kick        # Kick GUI access
weguardian.gui.notes       # Notes GUI access
weguardian.gui.unban       # Unban GUI access
weguardian.gui.unmute      # Unmute GUI access

weguardian.bypass.override # Bypass punishment protection
weguardian.silent          # Use silent punishment flags
weguardian.templates       # Use punishment templates
```

## Punishment Templates

### Template System

WeGuardian includes a comprehensive punishment template system:

```yaml
templates:
  spam:
    category: "chat"
    auto-escalate: true
    track-offenses: true
    levels:
      1:
        type: "TEMPMUTE"
        duration: "30m"
        reason: "Spam - First Offense"
      2:
        type: "TEMPMUTE"
        duration: "2h"
        reason: "Spam - Second Offense"
      3:
        type: "TEMPBAN"
        duration: "1d"
        reason: "Spam - Final Warning"
```

### Available Templates

- **Chat Violations**: spam, toxicity, harassment, advertising
- **Gameplay**: griefing, stealing, pvp_abuse, building_violations
- **Cheating**: hacking, xray, fly, speed, killaura
- **Conduct**: disrespect, inappropriate_language, trolling
- **Evasion**: ban_evasion, mute_evasion, alt_abuse

## Development

### Building from Source

```bash
git clone https://github.com/WeThink25/WeGuardian.git
cd WeGuardian
mvn clean package
```

### API Usage

```java
WeGuardian plugin = (WeGuardian) Bukkit.getPluginManager().getPlugin("WeGuardian");
PunishmentService service = plugin.getPunishmentService();

service.executeTemplate(
    staffUuid,
    staffName,
    targetName,
    "spam"
).thenAccept(success -> {
    if (success) {
        // Template punishment executed with auto-escalation
    }
});

service.ban(
    targetName,
    staffName,
    "Griefing",
    null
).thenAccept(success -> {
    if (success) {
        // Ban executed successfully
    }
});
```

## Upcoming Features

### Database Support
- **MySQL Support**: Cross-server punishment synchronization
- **SQLite Support**: Lightweight single-server option
- **Migration Tools**: Easy transition between storage types

### Folia Compatibility
- **Region-Aware Scheduling**: Full Folia support with FoliaLib
- **Performance Optimization**: Enhanced threading for Folia servers
- **Cross-Platform**: Seamless operation on both Paper and Folia

### Advanced Features
- **Discord Integration**: Rich webhook notifications and embeds
- **Web Interface**: Browser-based punishment management
- **API Expansion**: Comprehensive developer API
- **Import Tools**: Migration from other punishment plugins

## Support & Contributing

### Getting Help

- **Discord**: [Join our Discord](https://discord.gg/aSWKs3k4XA)
- **Issues**: [GitHub Issues](https://github.com/WeThink25/WeGuardian/issues)
- **Wiki**: [Documentation Wiki](https://github.com/WeThink25/WeGuardian/wiki)
- **bStats**: [Plugin Statistics](https://bstats.org/plugin/bukkit/WeGuardian/27046)

### Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- **Paper Team** - For the excellent server software
- **Folia Team** - For next-generation server architecture
- **PlaceholderAPI** - For placeholder integration
- **Adventure API** - For modern text components
- **FoliaLib** - For cross-platform compatibility

**WeGuardian** - Professional Minecraft punishment system for the modern server.
