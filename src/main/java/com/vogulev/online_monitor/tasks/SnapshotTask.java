package com.vogulev.online_monitor.tasks;

import com.vogulev.online_monitor.DatabaseManager;
import org.bukkit.Server;

/**
 * Periodic task for recording online snapshots
 */
public class SnapshotTask implements Runnable {
    private final DatabaseManager database;
    private final Server server;

    public SnapshotTask(DatabaseManager database, Server server) {
        this.database = database;
        this.server = server;
    }

    @Override
    public void run() {
        int currentOnline = server.getOnlinePlayers().size();
        database.recordOnlineSnapshot(currentOnline);
    }
}
