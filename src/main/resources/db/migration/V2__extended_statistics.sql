-- OnlineMonitor Plugin - Extended Statistics
-- Version: 2.0 (0.5.0)
-- Description: Adds extended player statistics tracking (deaths, kills, blocks, messages, AFK)

-- Add new columns to player_stats table for extended statistics
ALTER TABLE player_stats ADD COLUMN deaths INTEGER DEFAULT 0;
ALTER TABLE player_stats ADD COLUMN mob_kills INTEGER DEFAULT 0;
ALTER TABLE player_stats ADD COLUMN player_kills INTEGER DEFAULT 0;
ALTER TABLE player_stats ADD COLUMN blocks_broken INTEGER DEFAULT 0;
ALTER TABLE player_stats ADD COLUMN blocks_placed INTEGER DEFAULT 0;
ALTER TABLE player_stats ADD COLUMN messages_sent INTEGER DEFAULT 0;
ALTER TABLE player_stats ADD COLUMN last_activity TIMESTAMP;