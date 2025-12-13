package com.vogulev.online_monitor.commands;


import java.util.Map;

import com.vogulev.online_monitor.DatabaseManager;
import org.bukkit.command.CommandSender;

import static com.vogulev.online_monitor.utils.MessageUtils.sendColoredMessage;
import static com.vogulev.online_monitor.utils.NumericUtils.getMaxValue;
import static com.vogulev.online_monitor.utils.NumericUtils.parseIntOrDefault;
import static com.vogulev.online_monitor.utils.StatisticsBarCreater.createBar;
import static com.vogulev.online_monitor.i18n.LocalizationManager.getMessage;


public class HourlyStatsCommand implements OnlineMonitorCommand
{
    private final DatabaseManager database;


    protected HourlyStatsCommand(final DatabaseManager database)
    {
        this.database = database;
    }


    @Override
    public void execute(final CommandSender sender, final String[] args)
    {
        final int days = args.length > 1 ? parseIntOrDefault(args[1], 7) : 7;
        sendHourlyStats(sender, database.getHourlyAverages(days), days);
    }


    private void sendHourlyStats(final CommandSender sender, final Map<Integer, Double> hourlyAvg, final int days)
    {
        if (hourlyAvg.isEmpty())
        {
            sendColoredMessage(sender, getMessage("analytics.insufficient_data"));
            return;
        }

        sendColoredMessage(sender, getMessage("analytics.hourly.header", days));
        for (final Map.Entry<Integer, Double> entry : hourlyAvg.entrySet())
        {
            final String hour = String.format("%02d:00", entry.getKey());
            final String bar = createBar(entry.getValue(), getMaxValue(hourlyAvg.values()));
            sendColoredMessage(sender, "§b" + hour + " §7" + bar + " §a" + String.format("%.1f", entry.getValue()));
        }
    }
}
