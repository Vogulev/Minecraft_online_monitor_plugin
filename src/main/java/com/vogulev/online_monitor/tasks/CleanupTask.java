package com.vogulev.online_monitor.tasks;

import com.vogulev.online_monitor.DatabaseManager;

/**
 * Периодическая задача для очистки старых снимков онлайна
 */
public class CleanupTask implements Runnable {
    private final DatabaseManager database;
    private final int daysToKeep;

    public CleanupTask(DatabaseManager database, int daysToKeep) {
        this.database = database;
        this.daysToKeep = daysToKeep;
    }

    @Override
    public void run() {
        database.cleanOldSnapshots(daysToKeep);
    }
}
