package com.vogulev.online_monitor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manager for tracking AFK (Away From Keyboard) players
 */
public class AFKManager {

    private final Map<String, Long> lastActivityTime = new HashMap<>();
    private final long afkThresholdMillis;

    /**
     * Create AFK manager with specified threshold
     * @param afkThresholdMinutes Minutes of inactivity before marking as AFK
     */
    public AFKManager(final int afkThresholdMinutes) {
        this.afkThresholdMillis = afkThresholdMinutes * 60 * 1000L;
    }

    /**
     * Update player's last activity time
     * @param playerName Player name
     */
    public void updateActivity(final String playerName) {
        lastActivityTime.put(playerName, System.currentTimeMillis());
    }

    /**
     * Remove player from tracking (e.g., on quit)
     * @param playerName Player name
     */
    public void removePlayer(final String playerName) {
        lastActivityTime.remove(playerName);
    }

    /**
     * Check if player is AFK
     * @param playerName Player name
     * @return true if player is AFK
     */
    public boolean isAFK(final String playerName) {
        final Long lastActivity = lastActivityTime.get(playerName);
        if (lastActivity == null) {
            return false;
        }
        final long timeSinceActivity = System.currentTimeMillis() - lastActivity;
        return timeSinceActivity >= afkThresholdMillis;
    }

    /**
     * Get time in milliseconds since last activity
     * @param playerName Player name
     * @return Time since last activity, or 0 if not tracked
     */
    public long getTimeSinceActivity(final String playerName) {
        final Long lastActivity = lastActivityTime.get(playerName);
        if (lastActivity == null) {
            return 0;
        }
        return System.currentTimeMillis() - lastActivity;
    }

    /**
     * Get all AFK players
     * @return Set of AFK player names
     */
    public Set<String> getAFKPlayers() {
        final long currentTime = System.currentTimeMillis();
        return lastActivityTime.entrySet().stream()
            .filter(entry -> currentTime - entry.getValue() >= afkThresholdMillis)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    /**
     * Get count of AFK players
     * @return Number of AFK players
     */
    public int getAFKCount() {
        return getAFKPlayers().size();
    }

    /**
     * Clear all tracking data
     */
    public void clear() {
        lastActivityTime.clear();
    }
}
