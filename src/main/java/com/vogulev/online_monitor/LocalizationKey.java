package com.vogulev.online_monitor;

/**
 * Enumeration of all localization keys used in the plugin
 * Provides type-safe access to message keys defined in messages_*.properties files
 *
 * Usage: getMessage(LocalizationKey.COMMAND_STATS_HEADER)
 */
public enum LocalizationKey {
    // Permission errors
    PERMISSION_DENIED_STATS("permission.denied.stats"),
    PERMISSION_DENIED_ADMIN("permission.denied.admin"),
    PERMISSION_DENIED_UI("permission.denied.ui"),

    // Command - Basic stats
    COMMAND_STATS_HEADER("command.stats.header"),
    COMMAND_STATS_CURRENT("command.stats.current"),
    COMMAND_STATS_MAX("command.stats.max"),
    COMMAND_STATS_UNIQUE("command.stats.unique"),
    COMMAND_STATS_HINT("command.stats.hint"),

    // Command - Detailed stats
    COMMAND_DETAILED_HEADER("command.detailed.header"),
    COMMAND_DETAILED_CURRENT("command.detailed.current"),
    COMMAND_DETAILED_RECORD("command.detailed.record"),
    COMMAND_DETAILED_UNIQUE("command.detailed.unique"),
    COMMAND_DETAILED_SESSIONS("command.detailed.sessions"),
    COMMAND_DETAILED_AVG_TIME("command.detailed.avg_time"),
    COMMAND_DETAILED_ACTIVE("command.detailed.active"),
    COMMAND_DETAILED_ONLINE("command.detailed.online"),

    // Command - Top players
    COMMAND_TOP_HEADER("command.top.header"),
    COMMAND_TOP_EMPTY("command.top.empty"),
    COMMAND_TOP_POSITION("command.top.position"),

    // Command - Player stats
    COMMAND_PLAYER_HEADER("command.player.header"),
    COMMAND_PLAYER_STATUS_ONLINE("command.player.status.online"),
    COMMAND_PLAYER_STATUS_OFFLINE("command.player.status.offline"),
    COMMAND_PLAYER_SESSION("command.player.session"),
    COMMAND_PLAYER_PING("command.player.ping"),
    COMMAND_PLAYER_LOCATION("command.player.location"),
    COMMAND_PLAYER_JOINS("command.player.joins"),
    COMMAND_PLAYER_TOTAL_TIME("command.player.total_time"),
    COMMAND_PLAYER_STATS_HEADER("command.player.stats_header"),
    COMMAND_PLAYER_DEATHS("command.player.deaths"),
    COMMAND_PLAYER_MOB_KILLS("command.player.mob_kills"),
    COMMAND_PLAYER_PLAYER_KILLS("command.player.player_kills"),
    COMMAND_PLAYER_BLOCKS_BROKEN("command.player.blocks_broken"),
    COMMAND_PLAYER_BLOCKS_PLACED("command.player.blocks_placed"),
    COMMAND_PLAYER_MESSAGES_SENT("command.player.messages_sent"),
    COMMAND_PLAYER_NOT_FOUND("command.player.not_found"),

    // Command - Usage and errors
    COMMAND_USAGE_PLAYER("command.usage.player"),
    COMMAND_UNKNOWN("command.unknown"),
    COMMAND_USAGE("command.usage"),

    // Command - UI
    COMMAND_UI_DISABLED("command.ui.disabled"),
    COMMAND_UI_ENABLED("command.ui.enabled"),
    COMMAND_UI_DISABLED_PLAYER("command.ui.disabled.player"),
    COMMAND_UI_PLAYER_ONLY("command.ui.player_only"),

    // Analytics
    ANALYTICS_HOURLY_HEADER("analytics.hourly.header"),
    ANALYTICS_DAILY_HEADER("analytics.daily.header"),
    ANALYTICS_WEEKDAY_HEADER("analytics.weekday.header"),
    ANALYTICS_PEAK_HEADER("analytics.peak.header"),
    ANALYTICS_PEAK_PLAYERS("analytics.peak.players"),
    ANALYTICS_INSUFFICIENT_DATA("analytics.insufficient_data"),

    // Scoreboard
    SCOREBOARD_TITLE("scoreboard.title"),
    SCOREBOARD_ONLINE("scoreboard.online"),
    SCOREBOARD_RECORD("scoreboard.record"),
    SCOREBOARD_UNIQUE("scoreboard.unique"),
    SCOREBOARD_AVG("scoreboard.avg"),

    // Weekdays
    WEEKDAY_0("weekday.0"),
    WEEKDAY_1("weekday.1"),
    WEEKDAY_2("weekday.2"),
    WEEKDAY_3("weekday.3"),
    WEEKDAY_4("weekday.4"),
    WEEKDAY_5("weekday.5"),
    WEEKDAY_6("weekday.6"),

    // Welcome
    WELCOME_DEFAULT("welcome.default");

    private final String key;

    LocalizationKey(String key) {
        this.key = key;
    }

    /**
     * Get the localization key as a string
     * @return localization key (e.g., "command.stats.header")
     */
    public String getKey() {
        return key;
    }

    /**
     * Get the localization key as a string (convenience method)
     * @return localization key
     */
    @Override
    public String toString() {
        return key;
    }
}
