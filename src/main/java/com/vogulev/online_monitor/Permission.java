package com.vogulev.online_monitor;

/**
 * Enumeration of all plugin permissions
 * Provides type-safe access to permission nodes defined in plugin.yml
 */
public enum Permission {
    /**
     * All plugin permissions (wildcard)
     * Default: none
     */
    ALL("onlinemonitor.*"),

    /**
     * Basic commands: /online, /online ui
     * Default: true (all players)
     */
    BASIC("onlinemonitor.basic"),

    /**
     * Extended statistics commands: /online stats, top, player, hourly, daily, weekday, peak
     * Default: true (all players)
     */
    STATS("onlinemonitor.stats"),

    /**
     * Admin commands (reserved for future use)
     * Default: op (operators only)
     */
    ADMIN("onlinemonitor.admin");

    private final String node;

    Permission(String node) {
        this.node = node;
    }

    /**
     * Get the permission node as a string
     * @return permission node (e.g., "onlinemonitor.basic")
     */
    public String getNode() {
        return node;
    }

    /**
     * Get the permission node as a string (convenience method for direct usage)
     * @return permission node
     */
    @Override
    public String toString() {
        return node;
    }
}
