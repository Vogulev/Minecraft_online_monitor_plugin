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
 * API endpoint для получения общей статистики сервера
 * GET /api/stats
 */
public class StatsApiServlet extends HttpServlet {
    private final DatabaseManager database;
    private final OnlineMonitorPlugin plugin;
    private final Gson gson;

    public StatsApiServlet(DatabaseManager database, OnlineMonitorPlugin plugin) {
        this.database = database;
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");

        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("maxOnline", database.getMaxOnline());
            stats.put("uniquePlayers", database.getUniquePlayersCount());
            stats.put("totalSessions", database.getTotalSessions());
            stats.put("activeSessions", database.getActiveSessions());
            stats.put("totalPlaytime", database.getTotalPlaytime());

            Map<String, Integer> topPlayers = database.getTopPlayersByJoins(10);
            stats.put("topPlayers", topPlayers);

            resp.getWriter().write(gson.toJson(stats));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            resp.getWriter().write(gson.toJson(error));
        }
    }
}