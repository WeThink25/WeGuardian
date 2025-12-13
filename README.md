# 🛡️ WeGuardian

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21+-brightgreen" alt="Minecraft Version">
  <img src="https://img.shields.io/badge/Java-21+-orange" alt="Java Version">
  <img src="https://img.shields.io/badge/Paper-Supported-blue" alt="Paper Support">
  <img src="https://img.shields.io/badge/Folia-Supported-purple" alt="Folia Support">
  <img src="https://img.shields.io/badge/Version-2.0-red" alt="Plugin Version">
</p>

<p align="center">
  <b>Professional Punishment Management System for Minecraft Servers</b><br>
  <i>by WeThink</i>
</p>

---

## ✨ Features

### 🔨 Punishment System
- **Ban & TempBan** - Permanently or temporarily ban players
- **IP Ban & TempIP Ban** - Ban players by IP address with automatic IP resolution
- **Mute & TempMute** - Silence players permanently or temporarily
- **IP Mute & TempIP Mute** - IP-based muting system
- **Kick** - Instantly remove players from the server

### 🎮 Interactive GUI
- **Punishment GUI** - Beautiful and intuitive punishment selection interface
- **Duration Selector** - Easy-to-use duration selection with presets
- **History Viewer** - Browse punishment history with pagination

### 📊 Database Support
- **SQLite** - Zero-configuration local storage (default)
- **MySQL** - Scale to larger servers with MySQL support
- **HikariCP** - High-performance connection pooling
- **Caffeine Caching** - Lightning-fast data retrieval

### 🌐 Web Dashboard
- Built-in web panel for remote management
- REST API for custom integrations
- Configurable branding and appearance

### 🔗 Discord Integration
- Webhook logging for all punishments
- Batched messages to avoid rate limits
- Beautiful embed formatting

### ⚡ Performance
- **Folia Support** - Full compatibility with Folia servers
- **Async Operations** - Non-blocking database operations
- **Lazy GUI Initialization** - Optimized GUI performance

---

## 📥 Installation

1. Download the latest `WeGuardian.jar`
2. Place it in your server's `plugins` folder
3. Restart your server
4. Configure `plugins/WeGuardian/config.yml`

---

## 📝 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/ban <player> [reason]` | Permanently ban a player | `weguardian.ban` |
| `/tempban <player> <duration> [reason]` | Temporarily ban a player | `weguardian.tempban` |
| `/unban <player>` | Unban a player | `weguardian.unban` |
| `/banip <player> [reason]` | IP ban a player | `weguardian.banip` |
| `/tempbanip <player> <duration> [reason]` | Temp IP ban a player | `weguardian.tempbanip` |
| `/unbanip <player>` | Unban a player's IP | `weguardian.unbanip` |
| `/mute <player> [reason]` | Permanently mute a player | `weguardian.mute` |
| `/tempmute <player> <duration> [reason]` | Temporarily mute a player | `weguardian.tempmute` |
| `/unmute <player>` | Unmute a player | `weguardian.unmute` |
| `/muteip <player> [reason]` | IP mute a player | `weguardian.muteip` |
| `/tempmuteip <player> <duration> [reason]` | Temp IP mute a player | `weguardian.tempmuteip` |
| `/unmuteip <player>` | Unmute a player's IP | `weguardian.unmuteip` |
| `/kick <player> [reason]` | Kick a player | `weguardian.kick` |
| `/punish <player>` | Open punishment GUI | `weguardian.punish` |
| `/history <player>` | View punishment history | `weguardian.history` |
| `/weguardian reload` | Reload configuration | `weguardian.admin` |

---

## 🔑 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `weguardian.staff` | All staff permissions | OP |
| `weguardian.admin` | Admin commands (reload) | OP |
| `weguardian.bypass` | Bypass all punishments | false |

---

## ⏱️ Duration Format

Durations support the following formats:

| Unit | Example | Description |
|------|---------|-------------|
| `s` | `30s` | Seconds |
| `m` | `30m` | Minutes |
| `h` | `6h` | Hours |
| `d` | `7d` | Days |
| `w` | `2w` | Weeks |
| `M` | `1M` | Months |
| `y` | `1y` | Years |

**Examples:** `1h`, `6h`, `1d`, `7d`, `30d`, `90d`

---

## ⚙️ Configuration

### Database (SQLite - Default)
```yaml
database:
  type: "sqlite"
  sqlite:
    file: "punishments.db"
```

### Database (MySQL)
```yaml
database:
  type: "mysql"
  mysql:
    host: "localhost"
    port: 3306
    database: "weguardian"
    username: "root"
    password: "password"
```

### Discord Webhook
```yaml
discord:
  enabled: true
  webhook-url: "YOUR_WEBHOOK_URL"
  batch-interval-seconds: 5
```

### Web Dashboard
```yaml
web-dashboard:
  enabled: true
  host: "0.0.0.0"
  port: 8080
  admin-api-key: "YOUR_SECURE_KEY"
```

---

## 🛠️ Requirements

- **Minecraft Server:** Paper 1.21+ or Folia
- **Java:** 21 or higher

---

## 📚 Dependencies

- [Paper API](https://papermc.io/) - Server API
- [FoliaLib](https://github.com/TechnicallyCoded/FoliaLib) - Folia compatibility
- [Caffeine](https://github.com/ben-manes/caffeine) - Caching
- [HikariCP](https://github.com/brettwooldridge/HikariCP) - Connection pooling
- [ACF](https://github.com/aikar/commands) - Command framework
- [FastInv](https://github.com/MrMicky-FR/FastInv) - GUI framework
- [bStats](https://bstats.org/) - Plugin metrics
- [Javalin](https://javalin.io/) - Web framework

---

## 📞 Support

For support and questions, join our Discord or open an issue on GitHub.

---

## 📄 License

This project is licensed under the MIT License. See [LICENSE.md](LICENSE.md) for details.

---

<p align="center">
  Made with ❤️ by <b>WeThink</b>
</p>
