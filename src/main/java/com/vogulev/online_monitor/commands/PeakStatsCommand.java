package com.vogulev.online_monitor.commands;


import java.util.Map;

import com.vogulev.online_monitor.DatabaseManager;
import org.bukkit.command.CommandSender;

import static com.vogulev.online_monitor.utils.MessageUtils.sendColoredMessage;
import static com.vogulev.online_monitor.utils.NumericUtils.parseIntOrDefault;
import static com.vogulev.online_monitor.i18n.LocalizationManager.getMessage;


public class PeakStatsCommand implements OnlineMonitorCommand
{
    private final DatabaseManager database;


    protected PeakStatsCommand(final DatabaseManager database)
    {
        this.database = database;
    }


    @Override
    public void execute(final CommandSender sender, final String[] args)
    {
        final int days = args.length > 1 ? parseIntOrDefault(args[1], 7) : 7;
        sendPeakHours(sender, database.getPeakHours(days), days);
    }


    private void sendPeakHours(final CommandSender sender, final Map<String, Integer> peakHours, final int days)
    {
        if (peakHours.isEmpty())
        {
            sendColoredMessage(sender, getMessage("analytics.insufficient_data"));
            return;
        }

        sendColoredMessage(sender, getMessage("analytics.peak.header", days));
        int position = 1;
        for (final Map.Entry<String, Integer> entry : peakHours.entrySet())
        {
            final String position3 = position == 3 ? "§a⚡" : "§7•";
            final String position2 = position == 2 ? "§e⚡" : position3;
            final String emoji = position == 1 ? "§c§l⚡" : position2;
            sendColoredMessage(sender, emoji + " §b" + entry.getKey() + " §7- " +
                getMessage("analytics.peak.players", entry.getValue()));
            position++;
        }
    }
}
