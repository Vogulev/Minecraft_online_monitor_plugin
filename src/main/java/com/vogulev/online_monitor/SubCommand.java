package com.vogulev.online_monitor;

import java.util.Optional;

/**
 * Enumeration of all /online subcommands
 * Provides type-safe access to command names
 */
public enum SubCommand {
    /**
     * /online stats - detailed server statistics
     * Requires: Permission.STATS
     */
    STATS("stats"),

    /**
     * /online top - top players by join count
     * Requires: Permission.STATS
     */
    TOP("top"),

    /**
     * /online player <name> - detailed player statistics
     * Requires: Permission.STATS
     */
    PLAYER("player"),

    /**
     * /online hourly [days] - average online by hours
     * Requires: Permission.STATS
     */
    HOURLY("hourly"),

    /**
     * /online daily [days] - average online by days
     * Requires: Permission.STATS
     */
    DAILY("daily"),

    /**
     * /online weekday [weeks] - average online by weekdays
     * Requires: Permission.STATS
     */
    WEEKDAY("weekday"),

    /**
     * /online peak [days] - peak activity hours
     * Requires: Permission.STATS
     */
    PEAK("peak"),

    /**
     * /online ui - toggle UI scoreboard panel
     * Requires: Permission.BASIC
     */
    UI("ui"),

    /**
     * /online help - show help message (alias for unknown command)
     * Requires: Permission.BASIC
     */
    HELP("help");

    private final String name;

    SubCommand(String name) {
        this.name = name;
    }

    /**
     * Get the command name as a string
     * @return command name (e.g., "stats")
     */
    public String getName() {
        return name;
    }

    /**
     * Get the command name as a string (convenience method)
     * @return command name
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Parse a string into a SubCommand enum value
     * Case-insensitive matching
     * @param name The command name to parse
     * @return Optional containing the SubCommand if found, empty otherwise
     */
    public static Optional<SubCommand> fromString(String name) {
        if (name == null) {
            return Optional.empty();
        }

        String lowerName = name.toLowerCase();
        for (SubCommand cmd : values()) {
            if (cmd.name.equals(lowerName)) {
                return Optional.of(cmd);
            }
        }
        return Optional.empty();
    }

    /**
     * Get the permission required for this subcommand
     * @return required permission
     */
    public Permission getRequiredPermission() {
        return switch (this) {
            case UI, HELP -> Permission.BASIC;
            case STATS, TOP, PLAYER, HOURLY, DAILY, WEEKDAY, PEAK -> Permission.STATS;
        };
    }
}
