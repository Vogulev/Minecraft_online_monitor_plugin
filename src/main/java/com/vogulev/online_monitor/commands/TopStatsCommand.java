package com.vogulev.online_monitor.commands;


import java.util.Map;

import com.vogulev.online_monitor.DatabaseManager;
import org.bukkit.command.CommandSender;

import static com.vogulev.online_monitor.LocalizationKey.COMMAND_TOP_EMPTY;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_TOP_HEADER;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_TOP_POSITION;
import static com.vogulev.online_monitor.utils.MessageUtils.sendColoredMessage;
import static com.vogulev.online_monitor.i18n.LocalizationManager.getMessage;


public class TopStatsCommand implements OnlineMonitorCommand
{
    private final DatabaseManager database;


    protected TopStatsCommand(final DatabaseManager database)
    {
        this.database = database;
    }


    @Override
    public void execute(final CommandSender sender, final String[] args)
    {
        sendColoredMessage(sender, getMessage(COMMAND_TOP_HEADER));

        final Map<String, Integer> topPlayers = database.getTopPlayersByJoins(10);
        if (topPlayers.isEmpty())
        {
            sendColoredMessage(sender, getMessage(COMMAND_TOP_EMPTY));
            return;
        }

        int position = 1;
        for (final Map.Entry<String, Integer> entry : topPlayers.entrySet())
        {
            final String top3position = position == 3 ? "§c§l" : "§e";
            final String top2Position = position == 2 ? "§7§l" : top3position;
            final String medal = position == 1 ? "§6§l" : top2Position;
            sendColoredMessage(sender,
                getMessage(COMMAND_TOP_POSITION, medal, position, entry.getKey(), entry.getValue()));
            position++;
        }
    }
}
