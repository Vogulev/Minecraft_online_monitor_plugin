# Changelog

## [0.4.5] - 2025-12-06

### Added
- **Localization System** - Full plugin localization support
  - New `LocalizationManager` component for managing translations
  - Support for multiple languages via resource files
  - Language selection in config.yml (`language: en` or `language: ru`)
  - Resource files: `messages_en.properties` and `messages_ru.properties`
  - All plugin messages now support localization:
    - In-game commands and messages
    - Discord bot commands and embeds
    - Scoreboard UI panel
    - Player join/leave notifications
    - Web dashboard elements

### Changed
- Refactored all message handling to use localization system
- Updated config.yml with language selection option
- Improved code structure for better maintainability
- Enhanced Discord bot message formatting with localization
- Updated plugin.yml with localization improvements

### Configuration
- Added `language` parameter - select plugin language (default: en)
  - Available options: `en` (English), `ru` (Russian)



## [0.4.0] - 2025-11-30

### Added
- **UI Scoreboard Panel** - Real-time statistics display on screen
  - Compact scoreboard displayed on the right side of the screen
  - Shows: Online players, Record, Unique players, Average playtime
  - Auto-updates every second (configurable)
  - Individual player control via `/online ui` command
  - Automatically shown on player join
  - Global enable/disable in config.yml
  - Compact design (6 lines) for minimal screen space usage

### Changed
- All UI messages translated to English for better accessibility
- Scoreboard title shortened to "STATS" for compact display
- Optimized scoreboard layout for better readability

### Configuration
- Added `scoreboard.enabled` - enable/disable UI panel globally
- Added `scoreboard.update-interval-seconds` - scoreboard refresh rate (default: 1 second)

---

## [0.3.0]

### Added
- Web dashboard based on Embedded Jetty for viewing statistics through browser
- REST API endpoints for third-party service integration:
  - `/api/stats` - general server statistics
  - `/api/online` - current online and player list
  - `/api/players` - top players and specific player statistics
  - `/api/snapshots` - historical data (hourly, daily, weekly)
- Interactive dashboard with charts:
  - 📈 Hourly online statistics for the last 7 days (Chart.js)
  - 📊 Average online by day for the last 30 days
  - Top 10 players by join count
  - Real-time statistics cards
- Automatic data refresh on web dashboard every 30 seconds
- Responsive design for mobile devices
- Web dashboard settings in config.yml (enable/disable, port)

### Changed
- Updated Paper API version to 1.20.4 for Java 17 compatibility
- Updated documentation with detailed web dashboard and API description

### Dependencies
- Added Eclipse Jetty 11.0.18 (web server)
- Added Gson 2.10.1 (JSON serialization)
- Added Chart.js 4.4.0 (chart visualization)

---

## [1.1.0]

### Added
- Discord integration with full slash command support
- Discord notifications for player join/leave events
- New player notifications
- New online record notifications
- Server start/stop notifications
- Discord slash commands:
  - `/online` - show current online
  - `/stats` - show detailed statistics
  - `/top` - show top players
  - `/player <nickname>` - show player statistics
- Beautiful embed messages with emojis

### Changed
- Code refactoring for improved readability
- Database query optimization

---

## [1.0.0]

### Added
- Initial release of OnlineMonitor plugin
- Real-time online tracking
- Player statistics tracking (logins, playtime)
- Historical online data recording (snapshots every 5 minutes)
- SQLite and MySQL database support
- Detailed analytics by hours, days, and weekdays
- Top players by activity
- Personal statistics for each player
- In-game commands:
  - `/online` - basic statistics
  - `/online stats` - detailed statistics
  - `/online top` - top players
  - `/online player <name>` - player statistics
  - `/online hourly [days]` - average online by hour
  - `/online daily [days]` - average online by day
  - `/online weekday [weeks]` - average online by weekday
  - `/online peak [days]` - peak activity hours
- Customizable player join messages
- Automatic old snapshot cleanup
- Timezone support
- HikariCP connection pool for performance

---

## Change Types

- **Added** - for new features
- **Changed** - for changes in existing functionality
- **Deprecated** - for features that will be removed soon
- **Removed** - for removed features
- **Fixed** - for bug fixes
- **Security** - for security updates

---

## Links

- [Unreleased]: https://github.com/username/online_monitor/compare/v1.1.0...HEAD
- [0.2.0]: https://github.com/username/online_monitor/compare/v1.0.0...v1.1.0
- [0.1.0]: https://github.com/username/online_monitor/releases/tag/v1.0.0
