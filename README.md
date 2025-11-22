# OnlineMonitor - Minecraft Server Online Monitoring Plugin

A powerful plugin for tracking player statistics on your Minecraft server with full Discord integration!

## Features

### Core Functions:
- Real-time online tracking
- Player statistics (logins, playtime)
- Historical online data recording (snapshots every 5 minutes)
- SQLite and MySQL database support
- Detailed analytics by hours, days, and weekdays
- Top players by activity
- Personal statistics for each player

### Discord Integration:
- Player join/leave notifications
- New player announcements
- New online record alerts
- Server start/stop notifications
- Slash commands to view statistics directly in Discord
- Beautiful embed messages with emojis

### Web Dashboard:
- 📊 Interactive web panel with charts
- 📈 Real-time statistics visualization
- 🎨 Modern responsive design
- 📉 Online charts by hours and days (Chart.js)
- 🔄 Automatic data refresh every 30 seconds
- 🌐 REST API for third-party service integration
- 📱 Responsive interface for mobile devices

---

## Requirements

- **Minecraft**: 1.21.8 (Paper/Spigot)
- **Java**: 17 or higher
- **Discord bot** (for Discord integration)

---

## Installation

### Step 1: Download the Plugin

1. Download `online_monitor-X.X.X.jar` from the Releases section
2. Place the file in your server's `plugins/` folder

### Step 2: First Launch

1. Start the server
2. The plugin will create a configuration file `plugins/OnlineMonitor/config.yml`
3. Stop the server for configuration

---

## Discord Bot Setup

To use Discord integration, follow these steps:

### 1. Create Discord Application

1. Go to [Discord Developer Portal](https://discord.com/developers/applications)
2. Click **"New Application"**
3. Enter a name (e.g., "Minecraft Monitor")
4. Click **"Create"**

### 2. Create Bot and Get Token

1. In the left menu, select **"Bot"**
2. Click **"Add Bot"** → **"Yes, do it!"**
3. Scroll to **"Privileged Gateway Intents"** section
4. Enable all three toggles:
   - ✅ **PRESENCE INTENT**
   - ✅ **SERVER MEMBERS INTENT**
   - ✅ **MESSAGE CONTENT INTENT** ← **REQUIRED!**
5. Click **"Save Changes"**
6. Scroll up and click **"Reset Token"** → **"Copy"**
7. Save the token (you'll need it for config.yml)

### 3. Invite Bot to Server

1. In the left menu, select **"OAuth2"** → **"URL Generator"**
2. In **SCOPES** section, select:
   - `bot`
   - `applications.commands`
3. In **BOT PERMISSIONS** section, select:
   - `Send Messages`
   - `Embed Links`
   - `Read Message History`
   - `Use Slash Commands`
4. Copy the generated URL at the bottom of the page
5. Open the URL in your browser and add the bot to your Discord server

### 4. Get Channel ID

1. In Discord, open **User Settings** → **Advanced** → enable **"Developer Mode"**
2. Return to your Discord server
3. **Right-click** on the channel where you want to receive notifications
4. Select **"Copy Channel ID"**
5. Save the channel ID

---

## Configuration Setup

Open the `plugins/OnlineMonitor/config.yml` file and configure the parameters:

### Basic Settings

```yaml
# Player join message
welcome-message: "Welcome to the server, %player%!"

# Logging settings
log-unique-joins: true
log-join-quit: true
```

### Database Settings

```yaml
database:
  # Database type: sqlite or mysql
  type: sqlite

  # MySQL settings (if using MySQL)
  mysql:
    host: localhost
    port: 3306
    database: minecraft_stats
    username: root
    password: password
```

### Analytics Settings

```yaml
# Online snapshot recording interval (in minutes)
snapshot-interval-minutes: 5

# How many days to keep snapshots
snapshot-days-to-keep: 30

# Timezone (offset from UTC)
timezone-offset: +3
```

### Discord Settings

```yaml
discord:
  # Enable Discord integration
  enabled: true

  # Bot token (from step 2)
  bot-token: "YOUR_TOKEN_HERE"

  # Channel ID for notifications (from step 4)
  channel-id: "YOUR_CHANNEL_ID_HERE"

  # Notification settings
  notifications:
    player-join: true        # Player join notifications
    player-quit: true        # Player quit notifications
    new-player: true         # New player notifications
    new-record: true         # New record notifications
    server-start: true       # Server start notification
    server-stop: true        # Server stop notification
```

### Web Dashboard Settings

```yaml
web-panel:
  # Enable web dashboard
  enabled: true

  # Port for web dashboard (make sure the port is available)
  port: 8080
```

After enabling the web dashboard, it will be accessible at:
- **Locally**: http://localhost:8080
- **Network**: http://YOUR_IP:8080

**Important**: If you want to open the web dashboard for external access, configure:
1. Port forwarding on your router
2. Firewall rules
3. **Recommended**: Use a reverse proxy (nginx/Apache) with SSL/TLS for security

---

## Web Dashboard

After starting the server with the web dashboard enabled, open your browser and go to `http://your_server_address:8080`

### Web Dashboard Features:

#### 📊 Main Dashboard
- **Real-time statistics**:
  - Online record
  - Unique players count
  - Total sessions
  - Active sessions
  - Total playtime

- **Interactive charts**:
  - 📈 Hourly online statistics for the last 7 days
  - 📊 Average online by day for the last 30 days

- **Top players**:
  - Display of top 10 players by join count
  - Auto-updates

#### 🌐 REST API Endpoints

The web dashboard provides a REST API for integration with third-party services:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/stats` | GET | General server statistics |
| `/api/online` | GET | Current online and player list |
| `/api/players` | GET | Top players (parameter `limit=N`) |
| `/api/players?name=PlayerName` | GET | Statistics for a specific player |
| `/api/snapshots?type=hourly&days=7` | GET | Hourly average values |
| `/api/snapshots?type=daily&days=30` | GET | Daily average values |
| `/api/snapshots?type=weekday&weeks=4` | GET | Weekday averages |
| `/api/snapshots?type=peak&days=7` | GET | Peak activity hours |

#### API Usage Examples:

```bash
# Get general statistics
curl http://localhost:8080/api/stats

# Get current online
curl http://localhost:8080/api/online

# Get top 5 players
curl http://localhost:8080/api/players?limit=5

# Get statistics for player Notch
curl http://localhost:8080/api/players?name=Notch

# Get hourly statistics for 14 days
curl http://localhost:8080/api/snapshots?type=hourly&days=14
```

#### API Response Format (JSON):

**GET /api/stats**:
```json
{
  "maxOnline": 42,
  "uniquePlayers": 156,
  "totalSessions": 1247,
  "activeSessions": 8,
  "totalPlaytime": 125678900,
  "topPlayers": {
    "Notch": 125,
    "Herobrine": 98,
    ...
  }
}
```

**GET /api/online**:
```json
{
  "current": 8,
  "max": 100,
  "record": 42,
  "players": ["Player1", "Player2", ...]
}
```

---

## In-Game Commands

### Main Command: `/online`

| Command | Description |
|---------|-------------|
| `/online` | Show basic online statistics |
| `/online stats` | Detailed server statistics |
| `/online top` | Top 10 players by join count |
| `/online player <name>` | Statistics for a specific player |
| `/online hourly [days]` | Average online by hour (for N days, default 7) |
| `/online daily [days]` | Average online by day (for N days, default 7) |
| `/online weekday [weeks]` | Average online by weekday (for N weeks, default 4) |
| `/online peak [days]` | Peak activity hours (for N days, default 7) |

### Usage Examples:

```
/online
/online stats
/online top
/online player Notch
/online hourly 14
/online daily 30
/online weekday 8
/online peak 7
```

---

## Discord Slash Commands

After successful bot setup, the following commands will be available in Discord:

| Command | Description |
|---------|-------------|
| `/online` | Show current online on the server |
| `/stats` | Show detailed server statistics |
| `/top` | Show top 10 players by activity |
| `/player <nickname>` | Show statistics for a specific player |

---

## Discord Notifications

The plugin sends beautiful embed messages to Discord:

### Notification Types:

1. **Player Join**
   - Shows player name
   - Current online count
   - Marks new players with 🎉 icon

2. **Player Quit**
   - Shows player name
   - Current online count
   - Time spent in game

3. **New Online Record**
   - Bright notification about achieving a new record
   - Number of players

4. **Server Start/Stop**
   - Server status notifications

---

## Database

### SQLite (default)
- Database file: `plugins/OnlineMonitor/statistics.db`
- Suitable for small and medium servers
- No additional configuration required

### MySQL (optional)
- For large servers with high load
- Requires configuration in `config.yml`
- Supports connection pooling (HikariCP)

### Data Structure:

The plugin stores:
- Unique players
- Login and logout history
- Time spent in game
- Online snapshots (every 5 minutes)
- Maximum online achieved

---

## Troubleshooting

### Bot doesn't start in Discord

**Cause 1**: MESSAGE CONTENT INTENT not enabled

**Solution**:
1. Open [Discord Developer Portal](https://discord.com/developers/applications)
2. Select your application → Bot
3. Enable **MESSAGE CONTENT INTENT**
4. Save changes and restart the server

**Cause 2**: Incorrect token

**Solution**:
1. Generate a new token in Discord Developer Portal
2. Update `bot-token` in config.yml
3. Restart the server

### No notifications in Discord

**Check**:
- Correct `channel-id` in config.yml
- Bot permissions to send messages in the channel
- Notification settings in config.yml (should be `true`)

### Slash commands don't work

**Solution**:
- Wait up to 1 hour (Discord caches commands)
- Re-invite the bot with `applications.commands` permissions
- Check server logs for errors

### Database is not created

**Solution**:
- Check write permissions in `plugins/OnlineMonitor/` folder
- For MySQL: verify credentials are correct
- Check server logs for connection errors

### Web dashboard won't open

**Cause 1**: Port is occupied by another application

**Solution**:
- Change port in `config.yml` (e.g., to 8081, 8082)
- Check if port is free: `netstat -an | grep 8080`

**Cause 2**: Firewall blocks the port

**Solution**:
- Add a firewall rule for the selected port
- Windows: Windows Defender Firewall → Inbound Rules
- Linux: `sudo ufw allow 8080`

**Cause 3**: Web dashboard is disabled

**Solution**:
- Check `config.yml`: `web-panel.enabled` should be `true`
- Restart server after configuration change

### API returns empty data

**Solution**:
- Wait a few minutes after first launch
- Make sure snapshots are being recorded (check database)
- Verify that `snapshot-interval-minutes` is configured in `config.yml`

---

## System Requirements

### Minimum:
- Paper/Spigot 1.21.8
- Java 17
- 50 MB free disk space (for SQLite)
- 256 MB RAM

### Recommended:
- Paper 1.21.8
- Java 21
- MySQL database (for large servers)
- 512 MB RAM

---

## Building from Source

If you want to build the plugin yourself:

```bash
# Clone the repository
git clone https://github.com/Vogulev/Minecraft_online_monitor_plugin.git
cd online_monitor

# Build with Maven
./mvnw clean package

# The ready JAR file will be in:
# target/online_monitor-0.0.1-SNAPSHOT.jar
```

---

## License

This project is distributed under the MIT License.

---

## Support

If you have problems or suggestions:
- Create an Issue on GitHub
- Describe the problem in detail
- Attach server logs

---

## Credits

Libraries and technologies used:
- [JDA (Java Discord API)](https://github.com/discord-jda/JDA) - for Discord integration
- [HikariCP](https://github.com/brettwooldridge/HikariCP) - database connection pooling
- [SQLite JDBC](https://github.com/xerial/sqlite-jdbc) - SQLite driver
- [Eclipse Jetty](https://www.eclipse.org/jetty/) - embedded web server
- [Gson](https://github.com/google/gson) - JSON serialization
- [Chart.js](https://www.chartjs.org/) - chart visualization

---

**Enjoy the game!**