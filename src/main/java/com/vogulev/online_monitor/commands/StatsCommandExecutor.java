package com.vogulev.online_monitor.commands;


import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.vogulev.online_monitor.DatabaseManager;
import com.vogulev.online_monitor.LocalizationKey;
import com.vogulev.online_monitor.Permission;
import com.vogulev.online_monitor.SubCommand;
import com.vogulev.online_monitor.ui.ScoreboardServerStatisticsManager;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import static com.vogulev.online_monitor.LocalizationKey.COMMAND_STATS_CURRENT;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_STATS_HEADER;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_STATS_HINT;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_STATS_MAX;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_STATS_UNIQUE;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_UNKNOWN;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_USAGE;
import static com.vogulev.online_monitor.LocalizationKey.PERMISSION_DENIED_STATS;
import static com.vogulev.online_monitor.LocalizationKey.PERMISSION_DENIED_UI;
import static com.vogulev.online_monitor.Permission.BASIC;
import static com.vogulev.online_monitor.SubCommand.DAILY;
import static com.vogulev.online_monitor.SubCommand.HELP;
import static com.vogulev.online_monitor.SubCommand.HOURLY;
import static com.vogulev.online_monitor.SubCommand.PEAK;
import static com.vogulev.online_monitor.SubCommand.PLAYER;
import static com.vogulev.online_monitor.SubCommand.STATS;
import static com.vogulev.online_monitor.SubCommand.TOP;
import static com.vogulev.online_monitor.SubCommand.UI;
import static com.vogulev.online_monitor.SubCommand.WEEKDAY;
import static com.vogulev.online_monitor.i18n.LocalizationManager.getMessage;
import static com.vogulev.online_monitor.utils.MessageUtils.sendColoredMessage;


/**
 * Handler for /online commands
 */
public class StatsCommandExecutor implements CommandExecutor, TabCompleter
{
    private final DatabaseManager database;

    private final Server server;

    private final Map<String, Long> playerJoinTimes;

    private final ScoreboardServerStatisticsManager scoreboardServerStatisticsManager;

    private final Map<SubCommand, OnlineMonitorCommand> commandHandlers;


    public StatsCommandExecutor(final DatabaseManager database, final Server server, final Map<String, Long> playerJoinTimes,
        final ScoreboardServerStatisticsManager scoreboardServerStatisticsManager)
    {
        this.database = database;
        this.server = server;
        this.playerJoinTimes = playerJoinTimes;
        this.scoreboardServerStatisticsManager = scoreboardServerStatisticsManager;
        this.commandHandlers = initializeCommandHandlers();
    }


    /**
     * Initialize command handlers using Command pattern
     * Each subcommand is mapped to its handler implementation
     */
    private Map<SubCommand, OnlineMonitorCommand> initializeCommandHandlers()
    {
        final Map<SubCommand, OnlineMonitorCommand> handlers = new EnumMap<>(SubCommand.class);

        handlers.put(STATS, new SendDetailedStatsCommand(database, server));
        handlers.put(TOP, new TopStatsCommand(database));
        handlers.put(PLAYER, new PlayerStatsCommand(database, server, playerJoinTimes));
        handlers.put(HOURLY, new HourlyStatsCommand(database));
        handlers.put(DAILY, new DailyStatsCommand(database));
        handlers.put(WEEKDAY, new WeekdayCommand(database));
        handlers.put(PEAK, new PeakStatsCommand(database));
        handlers.put(UI, new ToggleUICommand(scoreboardServerStatisticsManager));
        handlers.put(HELP, new HelpCommand());

        return Collections.unmodifiableMap(handlers);
    }


    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args)
    {
        if (args.length == 0)
        {
            return handleBasicCommand(sender);
        }

        final SubCommand subCommand = SubCommand.fromString(args[0]).orElse(null);
        if (subCommand == null)
        {
            return handleUnknownCommand(sender);
        }

        if (!hasPermission(sender, subCommand.getRequiredPermission()))
        {
            return handlePermissionDenied(sender, subCommand.getRequiredPermission());
        }

        return executeSubCommand(subCommand, sender, args);
    }


    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args)
    {
        if (args.length == 1)
        {
            return Stream.of(SubCommand.values())
                .filter(subCmd -> hasPermission(sender, subCmd.getRequiredPermission()))
                .map(SubCommand::getName)
                .filter(name -> name.startsWith(args[0].toLowerCase()))
                .toList();
        }

        if (args.length == 2)
        {
            final SubCommand subCommand = SubCommand.fromString(args[0]).orElse(null);
            if (subCommand == PLAYER)
            {
                if (!hasPermission(sender, Permission.STATS))
                {
                    return Collections.emptyList();
                }
                return server.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
            }
        }

        return Collections.emptyList();
    }


    private boolean handleBasicCommand(final CommandSender sender)
    {
        if (!hasPermission(sender, BASIC))
        {
            sendColoredMessage(sender, getMessage(PERMISSION_DENIED_STATS));
            return true;
        }
        final int currentOnline = server.getOnlinePlayers().size();
        final int maxOnline = database.getMaxOnline();
        final int uniquePlayers = database.getUniquePlayersCount();

        sendColoredMessage(sender, getMessage(COMMAND_STATS_HEADER));
        sendColoredMessage(sender, getMessage(COMMAND_STATS_CURRENT, currentOnline));
        sendColoredMessage(sender, getMessage(COMMAND_STATS_MAX, maxOnline));
        sendColoredMessage(sender, getMessage(COMMAND_STATS_UNIQUE, uniquePlayers));
        sendColoredMessage(sender, getMessage(COMMAND_STATS_HINT));
        return true;
    }


    private boolean handleUnknownCommand(final CommandSender sender)
    {
        sendColoredMessage(sender, getMessage(COMMAND_UNKNOWN));
        sendColoredMessage(sender, getMessage(COMMAND_USAGE));
        return true;
    }


    private boolean handlePermissionDenied(final CommandSender sender, final Permission requiredPermission)
    {
        final LocalizationKey errorKey = requiredPermission == BASIC ? PERMISSION_DENIED_UI : PERMISSION_DENIED_STATS;
        sendColoredMessage(sender, getMessage(errorKey));
        return true;
    }


    private boolean executeSubCommand(final SubCommand subCommand, final CommandSender sender, final String[] args)
    {
        final OnlineMonitorCommand handler = commandHandlers.get(subCommand);
        if (handler != null)
        {
            handler.execute(sender, args);
        }
        return true;
    }


    /**
     * Checks if sender has the specified permission
     *
     * @param sender     The command sender
     * @param permission Permission to check
     * @return true if sender has permission, false otherwise
     */
    private boolean hasPermission(final CommandSender sender, final Permission permission)
    {
        return sender.hasPermission(permission.getNode());
    }
}
