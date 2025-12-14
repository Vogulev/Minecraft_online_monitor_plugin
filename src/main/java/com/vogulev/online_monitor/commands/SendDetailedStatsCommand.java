package com.vogulev.online_monitor.commands;


import com.vogulev.online_monitor.DatabaseManager;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static com.vogulev.online_monitor.LocalizationKey.COMMAND_DETAILED_ACTIVE;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_DETAILED_AVG_TIME;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_DETAILED_CURRENT;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_DETAILED_HEADER;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_DETAILED_ONLINE;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_DETAILED_RECORD;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_DETAILED_SESSIONS;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_DETAILED_UNIQUE;
import static com.vogulev.online_monitor.utils.MessageUtils.sendColoredMessage;
import static com.vogulev.online_monitor.i18n.LocalizationManager.getMessage;


public class SendDetailedStatsCommand implements OnlineMonitorCommand
{
    private final Server server;
    private final DatabaseManager database;


    protected SendDetailedStatsCommand(final DatabaseManager database, final Server server)
    {
        this.database = database;
        this.server = server;
    }


    @Override
    public void execute(final CommandSender sender, final String[] args)
    {
        sendDetailedStats(sender);
    }


    private void sendDetailedStats(final CommandSender sender)
    {
        final int currentOnline = server.getOnlinePlayers().size();
        final int maxOnline = database.getMaxOnline();
        final int uniquePlayers = database.getUniquePlayersCount();
        final int totalSessions = database.getTotalSessions();
        final int activeSessions = database.getActiveSessions();
        final long totalPlaytime = database.getTotalPlaytime();
        final long averageMinutes = uniquePlayers > 0 ? (totalPlaytime / uniquePlayers) / (1000 * 60) : 0;

        sendColoredMessage(sender, getMessage(COMMAND_DETAILED_HEADER));
        sendColoredMessage(sender, getMessage(COMMAND_DETAILED_CURRENT, currentOnline));
        sendColoredMessage(sender, getMessage(COMMAND_DETAILED_RECORD, maxOnline));
        sendColoredMessage(sender, getMessage(COMMAND_DETAILED_UNIQUE, uniquePlayers));
        sendColoredMessage(sender, getMessage(COMMAND_DETAILED_SESSIONS, totalSessions));
        sendColoredMessage(sender, getMessage(COMMAND_DETAILED_AVG_TIME, averageMinutes));
        sendColoredMessage(sender, getMessage(COMMAND_DETAILED_ACTIVE, activeSessions));

        if (currentOnline > 0)
        {
            final String onlinePlayers = server.getOnlinePlayers().stream()
                .map(Player::getName)
                .reduce((a, b) -> a + "§7, §f" + b)
                .orElse("");
            sendColoredMessage(sender, getMessage(COMMAND_DETAILED_ONLINE, onlinePlayers));
        }
    }
}
