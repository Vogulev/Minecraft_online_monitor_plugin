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
 * Управление scoreboard для отображения статистики онлайна
 */
public class ScoreboardServerStatisticsManager
{
    private final DatabaseManager database;

    private final Map<UUID, Boolean> playerScoreboardEnabled = new HashMap<>();

    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();

    private final boolean globallyEnabled;


    public ScoreboardServerStatisticsManager(DatabaseManager database, boolean enabled)
    {
        this.database = database;
        this.globallyEnabled = enabled;
    }


    public void showScoreboard(Player player)
    {
        if (!globallyEnabled)
        {
            return;
        }

        UUID playerId = player.getUniqueId();

        if (playerScoreboardEnabled.containsKey(playerId) && !playerScoreboardEnabled.get(playerId))
        {
            return;
        }

        ScoreboardManager manager = Bukkit.getScoreboardManager();

        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("online_stats", "dummy",
                ChatColor.translateAlternateColorCodes('&', getMessage("scoreboard.title")));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(ChatColor.translateAlternateColorCodes('&', getMessage("scoreboard.title")));

        playerScoreboards.put(playerId, scoreboard);
        player.setScoreboard(scoreboard);

        playerScoreboardEnabled.put(playerId, true);
    }


    public void updateAllScoreboards()
    {
        for (Player player : Bukkit.getOnlinePlayers())
        {
            updateScoreboard(player);
        }
    }


    public void updateScoreboard(Player player)
    {
        if (!globallyEnabled)
        {
            return;
        }

        UUID playerId = player.getUniqueId();

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

        Objective objective = scoreboard.getObjective("online_stats");
        if (objective == null)
        {
            return;
        }

        for (String entry : scoreboard.getEntries())
        {
            scoreboard.resetScores(entry);
        }

        int currentOnline = Bukkit.getOnlinePlayers().size();
        int maxOnline = database.getMaxOnline();
        int uniquePlayers = database.getUniquePlayersCount();
        long totalPlaytime = database.getTotalPlaytime();
        int averageMinutes = uniquePlayers > 0 ? (int) ((totalPlaytime / uniquePlayers) / (1000 * 60)) : 0;

        objective.getScore(colorize(getMessage("scoreboard.avg"))).setScore(averageMinutes);
        objective.getScore(colorize(getMessage("scoreboard.online"))).setScore(currentOnline);
        objective.getScore(colorize(getMessage("scoreboard.record"))).setScore(maxOnline);
        objective.getScore(colorize(getMessage("scoreboard.unique"))).setScore(uniquePlayers);
    }


    private void enableScoreboard(Player player)
    {
        UUID playerId = player.getUniqueId();
        playerScoreboardEnabled.put(playerId, true);
        showScoreboard(player);
        updateScoreboard(player);
    }


    private void disableScoreboard(Player player)
    {
        UUID playerId = player.getUniqueId();
        playerScoreboardEnabled.put(playerId, false);

        playerScoreboards.remove(playerId);
    }


    public boolean toggleScoreboard(Player player)
    {
        UUID playerId = player.getUniqueId();
        boolean currentState = playerScoreboardEnabled.getOrDefault(playerId, true);

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


    public void removePlayer(Player player)
    {
        UUID playerId = player.getUniqueId();
        playerScoreboards.remove(playerId);
    }


    private String colorize(String text)
    {
        return ChatColor.translateAlternateColorCodes('&', text);
    }


    public boolean isGloballyEnabled()
    {
        return globallyEnabled;
    }
}
