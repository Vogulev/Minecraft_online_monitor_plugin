package com.vogulev.online_monitor.listeners;

import com.vogulev.online_monitor.AFKManager;
import com.vogulev.online_monitor.DatabaseManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Listener for tracking extended player statistics
 */
public class PlayerStatisticsListener implements Listener {

    private final DatabaseManager databaseManager;
    private final AFKManager afkManager;

    public PlayerStatisticsListener(DatabaseManager databaseManager, AFKManager afkManager) {
        this.databaseManager = databaseManager;
        this.afkManager = afkManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String playerName = player.getName();

        CompletableFuture.runAsync(() -> databaseManager.incrementDeaths(playerName));

        afkManager.updateActivity(playerName);
        updateLastActivity(playerName);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        String killerName = killer.getName();
        Entity victim = event.getEntity();

        CompletableFuture.runAsync(() -> {
            if (victim instanceof Player) {
                // Player killed another player
                databaseManager.incrementPlayerKills(killerName);
            } else {
                // Player killed a mob
                databaseManager.incrementMobKills(killerName);
            }
        });

        afkManager.updateActivity(killerName);
        updateLastActivity(killerName);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        CompletableFuture.runAsync(() -> databaseManager.incrementBlocksBroken(playerName));

        afkManager.updateActivity(playerName);
        updateLastActivity(playerName);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        CompletableFuture.runAsync(() -> databaseManager.incrementBlocksPlaced(playerName));

        afkManager.updateActivity(playerName);
        updateLastActivity(playerName);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        CompletableFuture.runAsync(() -> databaseManager.incrementMessagesSent(playerName));

        afkManager.updateActivity(playerName);
        updateLastActivity(playerName);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only track if player actually moved (not just head movement)
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
            event.getFrom().getBlockY() != event.getTo().getBlockY() ||
            event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {

            Player player = event.getPlayer();
            String playerName = player.getName();

            afkManager.updateActivity(playerName);
            // Don't update DB on every move, only AFK manager
        }
    }

    private void updateLastActivity(String playerName) {
        CompletableFuture.runAsync(() -> databaseManager.updateLastActivity(playerName));
    }
}
