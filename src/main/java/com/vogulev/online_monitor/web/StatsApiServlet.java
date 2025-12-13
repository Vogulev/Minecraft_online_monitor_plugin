package com.vogulev.online_monitor.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vogulev.online_monitor.DatabaseManager;
import com.vogulev.online_monitor.OnlineMonitorPlugin;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * API endpoint for retrieving general server statistics
 * GET /api/stats
 */
public class StatsApiServlet extends HttpServlet {
    private final DatabaseManager database;
    private final OnlineMonitorPlugin plugin;
    private final Gson gson;

    public StatsApiServlet(final DatabaseManager database, final OnlineMonitorPlugin plugin) {
        this.database = database;
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");

        try {
            final Map<String, Object> stats = new HashMap<>();
            stats.put("maxOnline", database.getMaxOnline());
            stats.put("uniquePlayers", database.getUniquePlayersCount());
            stats.put("totalSessions", database.getTotalSessions());
            stats.put("activeSessions", database.getActiveSessions());
            stats.put("totalPlaytime", database.getTotalPlaytime());

            final Map<String, Integer> topPlayers = database.getTopPlayersByJoins(10);
            stats.put("topPlayers", topPlayers);

            resp.getWriter().write(gson.toJson(stats));
        } catch (final Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            final Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            resp.getWriter().write(gson.toJson(error));
        }
    }
}