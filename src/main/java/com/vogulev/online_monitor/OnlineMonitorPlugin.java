package com.vogulev.online_monitor;


import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;


public class OnlineMonitor extends JavaPlugin implements Listener
{
    private static final Logger logger = Logger.getLogger("OnlineMonitor");
    private int uniqueJoins = 0;

    @Override
    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(this, this);

        saveDefaultConfig();

        logger.info("OnlineMonitor plugin enabled!");
        logger.info("Unique joins counter reset to 0");
    }

    @Override
    public void onDisable()
    {
        logger.info("OnlineMonitor plugin disabled!");
        logger.info("Total unique joins this session: " + uniqueJoins);
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();

        if (!player.hasPlayedBefore()) {
            uniqueJoins++;
            logger.info("New player joined: " + player.getName());
        }

        String welcomeMessage = getConfig().getString("welcome-message",
                "Добро пожаловать на сервер, %player%!");
        player.sendMessage(welcomeMessage.replace("%player%", player.getName()));

        logger.info(player.getName() + " joined. Online: " + getServer().getOnlinePlayers().size());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        logger.info(player.getName() + " left. Online: " + (getServer().getOnlinePlayers().size() - 1));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("online")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                int onlineCount = getServer().getOnlinePlayers().size();

                player.sendMessage("§aСейчас онлайн: §e" + onlineCount + " §aигроков");
                player.sendMessage("§6Новых игроков за сессию: §e" + uniqueJoins);

            } else {
                sender.sendMessage("Online: " + getServer().getOnlinePlayers().size());
            }
            return true;
        }

        return false;
    }
}
