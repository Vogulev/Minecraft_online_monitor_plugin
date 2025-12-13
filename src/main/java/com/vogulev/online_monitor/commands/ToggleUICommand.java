package com.vogulev.online_monitor.commands;


import com.vogulev.online_monitor.ui.ScoreboardServerStatisticsManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static com.vogulev.online_monitor.LocalizationKey.COMMAND_UI_DISABLED;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_UI_DISABLED_PLAYER;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_UI_ENABLED;
import static com.vogulev.online_monitor.LocalizationKey.COMMAND_UI_PLAYER_ONLY;
import static com.vogulev.online_monitor.i18n.LocalizationManager.getMessage;
import static com.vogulev.online_monitor.utils.MessageUtils.sendColoredMessage;


public class ToggleUICommand implements OnlineMonitorCommand
{
    private final ScoreboardServerStatisticsManager scoreboardServerStatisticsManager;


    protected ToggleUICommand(final ScoreboardServerStatisticsManager scoreboardServerStatisticsManager)
    {
        this.scoreboardServerStatisticsManager = scoreboardServerStatisticsManager;
    }


    @Override
    public void execute(final CommandSender sender, final String[] args)
    {
        if (!(sender instanceof final Player player))
        {
            sendColoredMessage(sender, getMessage(COMMAND_UI_PLAYER_ONLY));
            return;
        }

        if (scoreboardServerStatisticsManager == null)
        {
            sendColoredMessage(sender, getMessage(COMMAND_UI_DISABLED));
            return;
        }

        final boolean enabled = scoreboardServerStatisticsManager.toggleScoreboard(player);

        if (enabled)
        {
            sendColoredMessage(sender, getMessage(COMMAND_UI_ENABLED));
        }
        else
        {
            sendColoredMessage(sender, getMessage(COMMAND_UI_DISABLED_PLAYER));
        }
    }
}
