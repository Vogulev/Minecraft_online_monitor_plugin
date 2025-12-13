package com.vogulev.online_monitor.commands;


import java.util.Map;

import com.vogulev.online_monitor.DatabaseManager;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static com.vogulev.online_monitor.LocalizationKey.COMMAND_PLAYER_BLOCKS_BROKEN;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_PLAYER_BLOCKS_PLACED;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_PLAYER_DEATHS;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_PLAYER_HEADER;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_PLAYER_JOINS;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_PLAYER_LOCATION;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_PLAYER_MESSAGES_SENT;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_PLAYER_MOB_KILLS;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_PLAYER_NOT_FOUND;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_PLAYER_PING;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_PLAYER_PLAYER_KILLS;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_PLAYER_SESSION;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_PLAYER_STATS_HEADER;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_PLAYER_STATUS_OFFLINE;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_PLAYER_STATUS_ONLINE;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_PLAYER_TOTAL_TIME;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_USAGE_PLAYER;
import static com.vogulev.online_monitor.i18n.LocalizationManager.getMessage;
import static com.vogulev.online_monitor.utils.MessageUtils.formatLocation;
import static com.vogulev.online_monitor.utils.MessageUtils.sendColoredMessage;


public class PlayerStatsCommand implements OnlineMonitorCommand
{
    private final Server server;

    private final Map<String, Long> playerJoinTimes;

    private final DatabaseManager database;


    protected PlayerStatsCommand(final DatabaseManager database, final Server server,
        final Map<String, Long> playerJoinTimes)
    {
        this.database = database;
        this.server = server;
        this.playerJoinTimes = playerJoinTimes;
    }


    @Override
    public void execute(final CommandSender sender, final String[] args)
    {
        if (args.length > 1)
        {
            sendPlayerStats(sender, args[1]);
        }
        else
        {
            sendColoredMessage(sender, getMessage(COMMAND_USAGE_PLAYER));
        }
    }


    private void sendPlayerStats(final CommandSender sender, final String playerName)
    {
        final Player player = server.getPlayer(playerName);
        final int totalJoins = database.getPlayerJoinCount(playerName);
        final long totalPlaytime = database.getPlayerTotalPlaytime(playerName);
        final long totalHours = totalPlaytime / (1000 * 60 * 60);
        final long totalMinutes = (totalPlaytime / (1000 * 60)) % 60;

        final int deaths = database.getPlayerDeaths(playerName);
        final int mobKills = database.getPlayerMobKills(playerName);
        final int playerKills = database.getPlayerPlayerKills(playerName);
        final int blocksBroken = database.getPlayerBlocksBroken(playerName);
        final int blocksPlaced = database.getPlayerBlocksPlaced(playerName);
        final int messagesSent = database.getPlayerMessagesSent(playerName);

        sendColoredMessage(sender, getMessage(COMMAND_PLAYER_HEADER, playerName));

        if (player != null && player.isOnline())
        {
            final Long joinTime = playerJoinTimes.get(playerName);
            final long sessionTime = joinTime != null ? System.currentTimeMillis() - joinTime : 0;
            final long sessionMinutes = sessionTime / (1000 * 60);

            sendColoredMessage(sender, getMessage(COMMAND_PLAYER_STATUS_ONLINE));
            sendColoredMessage(sender, getMessage(COMMAND_PLAYER_SESSION, sessionMinutes));
            sendColoredMessage(sender, getMessage(COMMAND_PLAYER_PING, player.getPing()));
            sendColoredMessage(sender, getMessage(COMMAND_PLAYER_LOCATION, formatLocation(player.getLocation())));
        }
        else
        {
            sendColoredMessage(sender, getMessage(COMMAND_PLAYER_STATUS_OFFLINE));
        }

        if (totalJoins > 0)
        {
            sendColoredMessage(sender, getMessage(COMMAND_PLAYER_JOINS, totalJoins));
            sendColoredMessage(sender, getMessage(COMMAND_PLAYER_TOTAL_TIME, totalHours, totalMinutes));

            sendColoredMessage(sender, getMessage(COMMAND_PLAYER_STATS_HEADER));
            sendColoredMessage(sender, getMessage(COMMAND_PLAYER_DEATHS, deaths));
            sendColoredMessage(sender, getMessage(COMMAND_PLAYER_MOB_KILLS, mobKills));
            sendColoredMessage(sender, getMessage(COMMAND_PLAYER_PLAYER_KILLS, playerKills));
            sendColoredMessage(sender, getMessage(COMMAND_PLAYER_BLOCKS_BROKEN, blocksBroken));
            sendColoredMessage(sender, getMessage(COMMAND_PLAYER_BLOCKS_PLACED, blocksPlaced));
            sendColoredMessage(sender, getMessage(COMMAND_PLAYER_MESSAGES_SENT, messagesSent));
        }
        else
        {
            sendColoredMessage(sender, getMessage(COMMAND_PLAYER_NOT_FOUND));
        }
    }
}
