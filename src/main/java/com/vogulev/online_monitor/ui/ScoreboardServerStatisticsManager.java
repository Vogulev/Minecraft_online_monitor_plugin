package com.vogulev.online_monitor.ui;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.vogulev.online_monitor.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import static com.vogulev.online_monitor.i18n.LocalizationManager.getMessage;


/**
 * Manages scoreboard for displaying online statistics
 */
public class ScoreboardServerStatisticsManager
{
    private final DatabaseManager database;

    private final Map<UUID, Boolean> playerScoreboardEnabled = new HashMap<>();

    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();

    private final boolean globallyEnabled;


    public ScoreboardServerStatisticsManager(final DatabaseManager database, final boolean enabled)
    {
        this.database = database;
        this.globallyEnabled = enabled;
    }


    public void showScoreboard(final Player player)
    {
        if (!globallyEnabled)
        {
            return;
        }

        final UUID playerId = player.getUniqueId();

        if (playerScoreboardEnabled.containsKey(playerId) && !playerScoreboardEnabled.get(playerId))
        {
            return;
        }

        final ScoreboardManager manager = Bukkit.getScoreboardManager();

        final Scoreboard scoreboard = manager.getNewScoreboard();
        final Objective objective = scoreboard.registerNewObjective("online_stats", "dummy",
            ChatColor.translateAlternateColorCodes('&', getMessage("scoreboard.title")));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(ChatColor.translateAlternateColorCodes('&', getMessage("scoreboard.title")));

        playerScoreboards.put(playerId, scoreboard);
        player.setScoreboard(scoreboard);

        playerScoreboardEnabled.put(playerId, true);
    }


    public void updateAllScoreboards()
    {
        for (final Player player : Bukkit.getOnlinePlayers())
        {
            updateScoreboard(player);
        }
    }


    public void updateScoreboard(final Player player)
    {
        if (!globallyEnabled)
        {
            return;
        }

        final UUID playerId = player.getUniqueId();

        if (playerScoreboardEnabled.containsKey(playerId) && !playerScoreboardEnabled.get(playerId))
        {
            return;
        }

        Scoreboard scoreboard = playerScoreboards.get(playerId);
        if (scoreboard == null)
        {
            showScoreboard(player);
            scoreboard = playerScoreboards.get(playerId);
        }

        if (scoreboard == null)
        {
            return;
        }

        final Objective objective = scoreboard.getObjective("online_stats");
        if (objective == null)
        {
            return;
        }

        for (final String entry : scoreboard.getEntries())
        {
            scoreboard.resetScores(entry);
        }

        final int currentOnline = Bukkit.getOnlinePlayers().size();
        final int maxOnline = database.getMaxOnline();
        final int uniquePlayers = database.getUniquePlayersCount();
        final long totalPlaytime = database.getTotalPlaytime();
        final int averageMinutes = uniquePlayers > 0 ? (int) ((totalPlaytime / uniquePlayers) / (1000 * 60)) : 0;

        objective.getScore(colorize(getMessage("scoreboard.avg"))).setScore(averageMinutes);
        objective.getScore(colorize(getMessage("scoreboard.online"))).setScore(currentOnline);
        objective.getScore(colorize(getMessage("scoreboard.record"))).setScore(maxOnline);
        objective.getScore(colorize(getMessage("scoreboard.unique"))).setScore(uniquePlayers);
    }


    private void enableScoreboard(final Player player)
    {
        final UUID playerId = player.getUniqueId();
        playerScoreboardEnabled.put(playerId, true);
        showScoreboard(player);
        updateScoreboard(player);
    }


    private void disableScoreboard(final Player player)
    {
        final UUID playerId = player.getUniqueId();
        playerScoreboardEnabled.put(playerId, false);

        playerScoreboards.remove(playerId);
    }


    public boolean toggleScoreboard(final Player player)
    {
        final UUID playerId = player.getUniqueId();
        final boolean currentState = playerScoreboardEnabled.getOrDefault(playerId, true);

        if (currentState)
        {
            disableScoreboard(player);
            return false;
        }
        else
        {
            enableScoreboard(player);
            return true;
        }
    }


    public void removePlayer(final Player player)
    {
        final UUID playerId = player.getUniqueId();
        playerScoreboards.remove(playerId);
    }


    private String colorize(final String text)
    {
        return ChatColor.translateAlternateColorCodes('&', text);
    }


    public boolean isGloballyEnabled()
    {
        return globallyEnabled;
    }
}
