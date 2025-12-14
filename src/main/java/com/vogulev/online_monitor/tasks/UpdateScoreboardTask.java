package com.vogulev.online_monitor.tasks;

import com.vogulev.online_monitor.ui.ScoreboardServerStatisticsManager;

/**
 * Task for automatic scoreboard updates
 */
public class UpdateScoreboardTask implements Runnable {
    private final ScoreboardServerStatisticsManager scoreboardServerStatisticsManager;

    public UpdateScoreboardTask(final ScoreboardServerStatisticsManager scoreboardServerStatisticsManager) {
        this.scoreboardServerStatisticsManager = scoreboardServerStatisticsManager;
    }

    @Override
    public void run() {
        if (scoreboardServerStatisticsManager != null && scoreboardServerStatisticsManager.isGloballyEnabled()) {
            scoreboardServerStatisticsManager.updateAllScoreboards();
        }
    }
}
