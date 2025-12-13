package com.vogulev.online_monitor.listeners;


import java.util.concurrent.CompletableFuture;

import com.vogulev.online_monitor.AFKManager;
import com.vogulev.online_monitor.DatabaseManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;


/**
 * Listener for tracking extended player statistics
 */
public class PlayerStatisticsListener implements Listener
{

    private final DatabaseManager databaseManager;

    private final AFKManager afkManager;


    public PlayerStatisticsListener(final DatabaseManager databaseManager, final AFKManager afkManager)
    {
        this.databaseManager = databaseManager;
        this.afkManager = afkManager;
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(final PlayerDeathEvent event)
    {
        final Player player = event.getEntity();
        final String playerName = player.getName();

        CompletableFuture.runAsync(() -> databaseManager.incrementDeaths(playerName));

        afkManager.updateActivity(playerName);
        updateLastActivity(playerName);
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(final EntityDeathEvent event)
    {
        final Player killer = event.getEntity().getKiller();
        if (killer == null)
        {
            return;
        }

        final String killerName = killer.getName();
        final Entity victim = event.getEntity();

        CompletableFuture.runAsync(() -> {
            if (victim instanceof Player)
            {
                databaseManager.incrementPlayerKills(killerName);
            }
            else
            {
                databaseManager.incrementMobKills(killerName);
            }
        });

        afkManager.updateActivity(killerName);
        updateLastActivity(killerName);
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event)
    {
        final Player player = event.getPlayer();
        final String playerName = player.getName();

        CompletableFuture.runAsync(() -> databaseManager.incrementBlocksBroken(playerName));

        afkManager.updateActivity(playerName);
        updateLastActivity(playerName);
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event)
    {
        final Player player = event.getPlayer();
        final String playerName = player.getName();

        CompletableFuture.runAsync(() -> databaseManager.incrementBlocksPlaced(playerName));

        afkManager.updateActivity(playerName);
        updateLastActivity(playerName);
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(final AsyncChatEvent event)
    {
        final Player player = event.getPlayer();
        final String playerName = player.getName();

        CompletableFuture.runAsync(() -> databaseManager.incrementMessagesSent(playerName));

        afkManager.updateActivity(playerName);
        updateLastActivity(playerName);
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(final PlayerMoveEvent event)
    {
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
            event.getFrom().getBlockY() != event.getTo().getBlockY() ||
            event.getFrom().getBlockZ() != event.getTo().getBlockZ())
        {

            final Player player = event.getPlayer();
            final String playerName = player.getName();

            afkManager.updateActivity(playerName);
        }
    }


    private void updateLastActivity(final String playerName)
    {
        CompletableFuture.runAsync(() -> databaseManager.updateLastActivity(playerName));
    }
}
