package com.vogulev.online_monitor.utils;


import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;


public class MessageUtils
{

    private MessageUtils()
    {
    }


    public static void sendColoredMessage(final CommandSender sender, final String message)
    {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }


    public static String formatLocation(final Location loc)
    {
        return String.format("X:%.0f Y:%.0f Z:%.0f", loc.getX(), loc.getY(), loc.getZ());
    }
}
