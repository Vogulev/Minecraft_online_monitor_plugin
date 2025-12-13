package com.vogulev.online_monitor.commands;


import org.bukkit.command.CommandSender;


/**
 * Command pattern interface for handling subcommands
 * Each subcommand has its own implementation of this interface
 */
public interface OnlineMonitorCommand
{
    /**
     * Execute the command
     *
     * @param sender The command sender
     * @param args   Command arguments (including the subcommand name at index 0)
     */
    void execute(CommandSender sender, String[] args);
}
