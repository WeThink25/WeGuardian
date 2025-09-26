# WeGuardian Web Dashboard

## Overview
The WeGuardian Web Dashboard provides a modern web interface for managing punishments, viewing statistics, and monitoring server activity.

## Features
- **Authentication**: Secure login system with session management
- **Dashboard**: Real-time statistics and server overview
- **Punishment Management**: Apply and revoke punishments through web interface
- **Player Management**: Search players and view punishment history
- **Live Updates**: WebSocket-based real-time updates
- **Responsive Design**: Works on desktop and mobile devices

## Configuration

### Enable the Dashboard
Edit your `config.yml` file:

```yaml
web-dashboard:
  enabled: true
  host: "127.0.0.1"
  port: 8080
  credentials:
    username: "admin"
    password: "supersecret"
  session-timeout: 3600
  ssl:
    enabled: false
    keystore-path: ""
    keystore-password: ""
  security:
    require-https: false
    max-login-attempts: 5
    lockout-duration: 300
```

### Configuration Options
- `enabled`: Enable/disable the web dashboard
- `host`: IP address to bind the server to
- `port`: Port number for the web server
- `credentials`: Login credentials for the dashboard
- `session-timeout`: Session timeout in seconds (default: 3600)
- `ssl`: SSL/TLS configuration (optional)
- `security`: Security settings for login protection

## Usage

### Accessing the Dashboard
1. Start your WeGuardian server
2. Open your web browser
3. Navigate to `http://localhost:8080` (or your configured host:port)
4. Login with the credentials from your config

### Dashboard Pages

#### Main Dashboard
- View server statistics
- Monitor active punishments
- See online player count
- Real-time updates via WebSocket

#### Active Punishments
- View all active punishments
- Revoke punishments with one click
- Filter by punishment type
- Auto-refresh for real-time updates

#### Player Management
- Search for players by name
- Apply new punishments
- View punishment history
- Support for all punishment types (ban, tempban, mute, tempmute, kick, warn)

#### Settings
- Configure dashboard settings
- Change login credentials
- Enable/disable features

## API Endpoints

### Authentication
- `POST /api/login` - Authenticate user
- `POST /api/logout` - Logout user

### Punishments
- `GET /api/punishments/active` - Get active punishments
- `POST /api/punishments/apply` - Apply new punishment
- `POST /api/punishments/revoke/:id` - Revoke punishment

### Statistics
- `GET /api/stats` - Get server statistics

### WebSocket
- `ws://localhost:8080/ws` - Real-time updates

## Security Features

### Session Management
- HttpOnly cookies for session storage
- Configurable session timeouts
- Automatic session cleanup

### Login Protection
- Account lockout after failed attempts
- Configurable lockout duration
- Secure credential validation

### CSRF Protection
- Session-based validation
- Secure token handling

## Troubleshooting

### Common Issues

#### Dashboard Won't Start
- Check if the port is already in use
- Verify the host configuration
- Ensure the dashboard is enabled in config

#### Login Issues
- Verify credentials in config.yml
- Check for account lockout
- Clear browser cookies if needed

#### WebSocket Connection Issues
- Ensure firewall allows WebSocket connections
- Check browser console for errors
- Verify WebSocket endpoint is accessible

These are automatically included when building the plugin.

## Development

### Building
The web dashboard is included in the main WeGuardian plugin. No separate build process is required.

### Customization
The web interface can be customized by modifying the HTML/CSS/JavaScript in the servlet classes. The design uses modern CSS with a responsive layout.

### Adding New Features
To add new features:
1. Create new servlet classes extending `HttpServlet`
2. Add API endpoints as needed
3. Update the navigation and routing
4. Add WebSocket support for real-time updates

## Support
For issues or questions about the web dashboard, please check the main WeGuardian documentation or contact the development team.

