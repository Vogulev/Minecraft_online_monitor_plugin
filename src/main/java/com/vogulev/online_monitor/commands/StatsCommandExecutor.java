package com.vogulev.online_monitor.commands;

import com.vogulev.online_monitor.DatabaseManager;
import com.vogulev.online_monitor.formatters.StatsFormatter;
import com.vogulev.online_monitor.ui.ScoreboardServerStatisticsManager;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.vogulev.online_monitor.i18n.LocalizationManager.getMessage;


/**
 * Обработчик команд /online
 */
public class StatsCommandExecutor implements CommandExecutor, TabCompleter {
    private final DatabaseManager database;
    private final Server server;
    private final Map<String, Long> playerJoinTimes;
    private final ScoreboardServerStatisticsManager scoreboardServerStatisticsManager;

    public StatsCommandExecutor(DatabaseManager database, Server server, Map<String, Long> playerJoinTimes,
                                ScoreboardServerStatisticsManager scoreboardServerStatisticsManager) {
        this.database = database;
        this.server = server;
        this.playerJoinTimes = playerJoinTimes;
        this.scoreboardServerStatisticsManager = scoreboardServerStatisticsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendBasicStats(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "stats":
                sendDetailedStats(sender);
                break;
            case "top":
                sendTopStats(sender);
                break;
            case "player":
                if (args.length > 1) {
                    sendPlayerStats(sender, args[1]);
                } else {
                    StatsFormatter.sendColoredMessage(sender, getMessage("command.usage.player"));
                }
                break;
            case "hourly":
                int days = args.length > 1 ? parseIntOrDefault(args[1], 7) : 7;
                StatsFormatter.sendHourlyStats(sender, database.getHourlyAverages(days), days);
                break;
            case "daily":
                int daysDaily = args.length > 1 ? parseIntOrDefault(args[1], 7) : 7;
                StatsFormatter.sendDailyStats(sender, database.getDailyAverages(daysDaily), daysDaily);
                break;
            case "weekday":
                int weeks = args.length > 1 ? parseIntOrDefault(args[1], 4) : 4;
                StatsFormatter.sendWeekdayStats(sender, database.getWeekdayAverages(weeks), weeks);
                break;
            case "peak":
                int daysPeak = args.length > 1 ? parseIntOrDefault(args[1], 7) : 7;
                StatsFormatter.sendPeakHours(sender, database.getPeakHours(daysPeak), daysPeak);
                break;
            case "ui":
                toggleUI(sender);
                break;
            default:
                StatsFormatter.sendColoredMessage(sender, getMessage("command.unknown"));
                StatsFormatter.sendColoredMessage(sender, getMessage("command.usage"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Stream.of("stats", "top", "player", "hourly", "daily", "weekday", "peak", "ui", "help")
                    .filter(option -> option.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("player")) {
            return server.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return Collections.emptyList();
    }

    private void sendBasicStats(CommandSender sender) {
        int currentOnline = server.getOnlinePlayers().size();
        int maxOnline = database.getMaxOnline();
        int uniquePlayers = database.getUniquePlayersCount();

        StatsFormatter.sendColoredMessage(sender, getMessage("command.stats.header"));
        StatsFormatter.sendColoredMessage(sender, getMessage("command.stats.current", currentOnline));
        StatsFormatter.sendColoredMessage(sender, getMessage("command.stats.max", maxOnline));
        StatsFormatter.sendColoredMessage(sender, getMessage("command.stats.unique", uniquePlayers));
        StatsFormatter.sendColoredMessage(sender, getMessage("command.stats.hint"));
    }

    private void sendDetailedStats(CommandSender sender) {
        int currentOnline = server.getOnlinePlayers().size();
        int maxOnline = database.getMaxOnline();
        int uniquePlayers = database.getUniquePlayersCount();
        int totalSessions = database.getTotalSessions();
        int activeSessions = database.getActiveSessions();
        long totalPlaytime = database.getTotalPlaytime();
        long averageMinutes = uniquePlayers > 0 ? (totalPlaytime / uniquePlayers) / (1000 * 60) : 0;

        StatsFormatter.sendColoredMessage(sender, getMessage("command.detailed.header"));
        StatsFormatter.sendColoredMessage(sender, getMessage("command.detailed.current", currentOnline));
        StatsFormatter.sendColoredMessage(sender, getMessage("command.detailed.record", maxOnline));
        StatsFormatter.sendColoredMessage(sender, getMessage("command.detailed.unique", uniquePlayers));
        StatsFormatter.sendColoredMessage(sender, getMessage("command.detailed.sessions", totalSessions));
        StatsFormatter.sendColoredMessage(sender, getMessage("command.detailed.avg_time", averageMinutes));
        StatsFormatter.sendColoredMessage(sender, getMessage("command.detailed.active", activeSessions));

        if (currentOnline > 0) {
            String onlinePlayers = server.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .reduce((a, b) -> a + "§7, §f" + b)
                    .orElse("");
            StatsFormatter.sendColoredMessage(sender, getMessage("command.detailed.online", onlinePlayers));
        }
    }

    private void sendTopStats(CommandSender sender) {
        StatsFormatter.sendColoredMessage(sender, getMessage("command.top.header"));

        Map<String, Integer> topPlayers = database.getTopPlayersByJoins(10);
        if (topPlayers.isEmpty()) {
            StatsFormatter.sendColoredMessage(sender, getMessage("command.top.empty"));
            return;
        }

        int position = 1;
        for (Map.Entry<String, Integer> entry : topPlayers.entrySet()) {
            String medal = position == 1 ? "§6§l" : position == 2 ? "§7§l" : position == 3 ? "§c§l" : "§e";
            StatsFormatter.sendColoredMessage(sender,
                    getMessage("command.top.position", medal, position, entry.getKey(), entry.getValue()));
            position++;
        }
    }

    private void sendPlayerStats(CommandSender sender, String playerName) {
        Player player = server.getPlayer(playerName);
        int totalJoins = database.getPlayerJoinCount(playerName);
        long totalPlaytime = database.getPlayerTotalPlaytime(playerName);
        long totalHours = totalPlaytime / (1000 * 60 * 60);
        long totalMinutes = (totalPlaytime / (1000 * 60)) % 60;

        StatsFormatter.sendColoredMessage(sender, getMessage("command.player.header", playerName));

        if (player != null && player.isOnline()) {
            Long joinTime = playerJoinTimes.get(playerName);
            long sessionTime = joinTime != null ? System.currentTimeMillis() - joinTime : 0;
            long sessionMinutes = sessionTime / (1000 * 60);

            StatsFormatter.sendColoredMessage(sender, getMessage("command.player.status.online"));
            StatsFormatter.sendColoredMessage(sender, getMessage("command.player.session", sessionMinutes));
            StatsFormatter.sendColoredMessage(sender, getMessage("command.player.ping", player.getPing()));
            StatsFormatter.sendColoredMessage(sender, getMessage("command.player.location", formatLocation(player.getLocation())));
        } else {
            StatsFormatter.sendColoredMessage(sender, getMessage("command.player.status.offline"));
        }

        if (totalJoins > 0) {
            StatsFormatter.sendColoredMessage(sender, getMessage("command.player.joins", totalJoins));
            StatsFormatter.sendColoredMessage(sender, getMessage("command.player.total_time", totalHours, totalMinutes));
        } else {
            StatsFormatter.sendColoredMessage(sender, getMessage("command.player.not_found"));
        }
    }

    private String formatLocation(Location loc) {
        return String.format("X:%.0f Y:%.0f Z:%.0f", loc.getX(), loc.getY(), loc.getZ());
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void toggleUI(CommandSender sender) {
        if (!(sender instanceof Player)) {
            StatsFormatter.sendColoredMessage(sender, getMessage("command.ui.player_only"));
            return;
        }

        if (scoreboardServerStatisticsManager == null) {
            StatsFormatter.sendColoredMessage(sender, getMessage("command.ui.disabled"));
            return;
        }

        Player player = (Player) sender;
        boolean enabled = scoreboardServerStatisticsManager.toggleScoreboard(player);

        if (enabled) {
            StatsFormatter.sendColoredMessage(sender, getMessage("command.ui.enabled"));
        } else {
            StatsFormatter.sendColoredMessage(sender, getMessage("command.ui.disabled.player"));
        }
    }
}
