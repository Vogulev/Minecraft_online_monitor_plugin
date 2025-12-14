package com.vogulev.online_monitor.commands;


import org.bukkit.command.CommandSender;

import static com.vogulev.online_monitor.LocalizationKey.COMMAND_USAGE;
import static com.vogulev.online_monitor.i18n.LocalizationManager.getMessage;
import static com.vogulev.online_monitor.utils.MessageUtils.sendColoredMessage;


public class HelpCommand implements OnlineMonitorCommand
{

    protected HelpCommand()
    {
    }


    @Override
    public void execute(final CommandSender sender, final String[] args)
    {
        sendColoredMessage(sender, getMessage(COMMAND_USAGE));
    }
}
