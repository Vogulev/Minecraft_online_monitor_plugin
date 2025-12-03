package com.vogulev.online_monitor.formatters;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.Map;

import static com.vogulev.online_monitor.i18n.LocalizationManager.getMessage;

/**
 * Форматирование и отправка статистических сообщений игрокам
 */
public class StatsFormatter {

    private StatsFormatter()
    {
    }

    public static void sendColoredMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    public static String createBar(double value, double maxValue) {
        int barLength = 20;
        int filledLength = (int) ((value / maxValue) * barLength);
        double percentage = (value / maxValue) * 100;

        String barColor;
        if (percentage >= 75) {
            barColor = "&a"; // Зеленый - высокая активность
        } else if (percentage >= 50) {
            barColor = "&e"; // Желтый - средняя активность
        } else if (percentage >= 25) {
            barColor = "&6"; // Оранжевый - низкая активность
        } else {
            barColor = "&c"; // Красный - очень низкая активность
        }

        StringBuilder bar = new StringBuilder("§8[");
        bar.append(barColor);
        for (int i = 0; i < barLength; i++) {
            if (i < filledLength) {
                bar.append("█");
            } else {
                bar.append("§8░");
            }
        }
        bar.append("§8]");

        return bar.toString();
    }

    public static double getMaxValue(Collection<Double> values) {
        return values.stream().max(Double::compare).orElse(1.0);
    }

    public static void sendHourlyStats(CommandSender sender, Map<Integer, Double> hourlyAvg, int days) {
        if (hourlyAvg.isEmpty()) {
            sendColoredMessage(sender, getMessage("analytics.insufficient_data"));
            return;
        }

        sendColoredMessage(sender, getMessage("analytics.hourly.header", days));
        for (Map.Entry<Integer, Double> entry : hourlyAvg.entrySet()) {
            String hour = String.format("%02d:00", entry.getKey());
            String bar = createBar(entry.getValue(), getMaxValue(hourlyAvg.values()));
            sendColoredMessage(sender, "§b" + hour + " §7" + bar + " §a" + String.format("%.1f", entry.getValue()));
        }
    }

    public static void sendDailyStats(CommandSender sender, Map<String, Double> dailyAvg, int days) {
        if (dailyAvg.isEmpty()) {
            sendColoredMessage(sender, getMessage("analytics.insufficient_data"));
            return;
        }

        sendColoredMessage(sender, getMessage("analytics.daily.header", days));
        for (Map.Entry<String, Double> entry : dailyAvg.entrySet()) {
            String bar = createBar(entry.getValue(), getMaxValue(dailyAvg.values()));
            sendColoredMessage(sender, "§d" + entry.getKey() + " §7" + bar + " §a" + String.format("%.1f", entry.getValue()));
        }
    }

    public static void sendWeekdayStats(CommandSender sender, Map<String, Double> weekdayAvg, int weeks) {
        if (weekdayAvg.isEmpty()) {
            sendColoredMessage(sender, getMessage("analytics.insufficient_data"));
            return;
        }

        sendColoredMessage(sender, getMessage("analytics.weekday.header", weeks));
        for (Map.Entry<String, Double> entry : weekdayAvg.entrySet()) {
            String bar = createBar(entry.getValue(), getMaxValue(weekdayAvg.values()));
            sendColoredMessage(sender, "§b" + String.format("%-12s", entry.getKey()) + " §7" + bar + " §a" + String.format("%.1f", entry.getValue()));
        }
    }

    public static void sendPeakHours(CommandSender sender, Map<String, Integer> peakHours, int days) {
        if (peakHours.isEmpty()) {
            sendColoredMessage(sender, getMessage("analytics.insufficient_data"));
            return;
        }

        sendColoredMessage(sender, getMessage("analytics.peak.header", days));
        int position = 1;
        for (Map.Entry<String, Integer> entry : peakHours.entrySet()) {
            String emoji = position == 1 ? "§c§l⚡" : position == 2 ? "§e⚡" : position == 3 ? "§a⚡" : "§7•";
            sendColoredMessage(sender, emoji + " §b" + entry.getKey() + " §7- " + getMessage("analytics.peak.players", entry.getValue()));
            position++;
        }
    }
}
