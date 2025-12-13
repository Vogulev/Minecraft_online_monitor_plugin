package com.vogulev.online_monitor.commands;


import java.util.Map;

import com.vogulev.online_monitor.DatabaseManager;
import org.bukkit.command.CommandSender;

import static com.vogulev.online_monitor.utils.MessageUtils.sendColoredMessage;
import static com.vogulev.online_monitor.utils.NumericUtils.getMaxValue;
import static com.vogulev.online_monitor.utils.NumericUtils.parseIntOrDefault;
import static com.vogulev.online_monitor.utils.StatisticsBarCreater.createBar;
import static com.vogulev.online_monitor.i18n.LocalizationManager.getMessage;


public class DailyStatsCommand implements OnlineMonitorCommand
{
    private final DatabaseManager database;


    protected DailyStatsCommand(final DatabaseManager database)
    {
        this.database = database;
    }


    @Override
    public void execute(final CommandSender sender, final String[] args)
    {
        final int days = args.length > 1 ? parseIntOrDefault(args[1], 7) : 7;
        sendDailyStats(sender, database.getDailyAverages(days), days);
    }


    private void sendDailyStats(final CommandSender sender, final Map<String, Double> dailyAvg, final int days)
    {
        if (dailyAvg.isEmpty())
        {
            sendColoredMessage(sender, getMessage("analytics.insufficient_data"));
            return;
        }
        sendColoredMessage(sender, getMessage("analytics.daily.header", days));
        for (final Map.Entry<String, Double> entry : dailyAvg.entrySet())
        {
            final String bar = createBar(entry.getValue(), getMaxValue(dailyAvg.values()));
            sendColoredMessage(sender,
                "§d" + entry.getKey() + " §7" + bar + " §a" + String.format("%.1f", entry.getValue()));
        }
    }
}
