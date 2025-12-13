package com.vogulev.online_monitor.utils;


public class StatisticsBarCreater
{

    private StatisticsBarCreater()
    {
    }


    public static String createBar(final double value, final double maxValue)
    {
        final int barLength = 20;
        final int filledLength = (int) ((value / maxValue) * barLength);
        final double percentage = (value / maxValue) * 100;

        final String barColor;
        if (percentage >= 75)
        {
            barColor = "&a"; // Green - high activity
        }
        else if (percentage >= 50)
        {
            barColor = "&e"; // Yellow - medium activity
        }
        else if (percentage >= 25)
        {
            barColor = "&6"; // Orange - low activity
        }
        else
        {
            barColor = "&c"; // Red - very low activity
        }

        final StringBuilder bar = new StringBuilder("§8[");
        bar.append(barColor);
        for (int i = 0; i < barLength; i++)
        {
            if (i < filledLength)
            {
                bar.append("█");
            }
            else
            {
                bar.append("§8░");
            }
        }
        bar.append("§8]");

        return bar.toString();
    }
}
