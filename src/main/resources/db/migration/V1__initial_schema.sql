-- OnlineMonitor Plugin - Initial Database Schema
-- Version: 1.0
-- Description: Creates initial tables for server statistics, player stats, sessions, and snapshots

-- Server-wide statistics table
CREATE TABLE IF NOT EXISTS server_stats (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    max_online INTEGER DEFAULT 0,
    total_unique_players INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Initialize server stats with default values
INSERT INTO server_stats (id, max_online, total_unique_players)
SELECT 1, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM server_stats WHERE id = 1);

-- Player aggregated statistics table
CREATE TABLE IF NOT EXISTS player_stats (
    player_name VARCHAR(16) PRIMARY KEY,
    total_joins INTEGER DEFAULT 0,
    total_playtime BIGINT DEFAULT 0,
    first_join TIMESTAMP,
    last_join TIMESTAMP
);

-- Player individual sessions table
CREATE TABLE IF NOT EXISTS player_sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_name VARCHAR(16) NOT NULL,
    join_time TIMESTAMP NOT NULL,
    quit_time TIMESTAMP,
    session_duration BIGINT DEFAULT 0,
    FOREIGN KEY (player_name) REFERENCES player_stats(player_name)
);

-- Online snapshots for historical analytics
CREATE TABLE IF NOT EXISTS online_snapshots (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    online_count INTEGER NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for efficient time-range queries
CREATE INDEX IF NOT EXISTS idx_snapshots_timestamp ON online_snapshots(timestamp);