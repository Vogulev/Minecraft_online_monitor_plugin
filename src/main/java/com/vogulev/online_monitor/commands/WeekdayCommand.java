package com.vogulev.online_monitor.commands;


import java.util.Map;

import com.vogulev.online_monitor.DatabaseManager;
import org.bukkit.command.CommandSender;

import static com.vogulev.online_monitor.utils.MessageUtils.sendColoredMessage;
import static com.vogulev.online_monitor.utils.NumericUtils.getMaxValue;
import static com.vogulev.online_monitor.utils.NumericUtils.parseIntOrDefault;
import static com.vogulev.online_monitor.utils.StatisticsBarCreater.createBar;
import static com.vogulev.online_monitor.i18n.LocalizationManager.getMessage;


public class WeekdayCommand implements OnlineMonitorCommand
{
    private final DatabaseManager database;


    protected WeekdayCommand(final DatabaseManager database)
    {
        this.database = database;
    }


    public void execute(final CommandSender sender, final String[] args)
    {
        final int weeks = args.length > 1 ? parseIntOrDefault(args[1], 4) : 4;
        sendWeekdayStats(sender, database.getWeekdayAverages(weeks), weeks);
    }


    private static void sendWeekdayStats(final CommandSender sender, final Map<Integer, Double> weekdayAvg, final int weeks)
    {
        if (weekdayAvg.isEmpty())
        {
            sendColoredMessage(sender, getMessage("analytics.insufficient_data"));
            return;
        }

        sendColoredMessage(sender, getMessage("analytics.weekday.header", weeks));
        for (final Map.Entry<Integer, Double> entry : weekdayAvg.entrySet())
        {
            final String weekdayName = getMessage("weekday." + entry.getKey());
            final String bar = createBar(entry.getValue(), getMaxValue(weekdayAvg.values()));
            sendColoredMessage(sender, "§b" + String.format("%-12s", weekdayName) + " §7" + bar + " §a" +
                String.format("%.1f", entry.getValue()));
        }
    }
}
