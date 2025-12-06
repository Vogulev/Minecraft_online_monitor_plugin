package com.vogulev.online_monitor.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vogulev.online_monitor.DatabaseManager;
import com.vogulev.online_monitor.OnlineMonitorPlugin;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API endpoint for retrieving current online information
 * GET /api/online
 */
public class OnlineApiServlet extends HttpServlet {
    private final DatabaseManager database;
    private final OnlineMonitorPlugin plugin;
    private final Gson gson;

    public OnlineApiServlet(DatabaseManager database, OnlineMonitorPlugin plugin) {
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
            Map<String, Object> data = getServerData();
            resp.getWriter().write(gson.toJson(data));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            resp.getWriter().write(gson.toJson(error));
        }
    }


    private Map<String, Object> getServerData()
    {
        Map<String, Object> data = new HashMap<>();

        int currentOnline = plugin.getServer().getOnlinePlayers().size();
        data.put("current", currentOnline);
        data.put("max", plugin.getServer().getMaxPlayers());
        data.put("record", database.getMaxOnline());

        List<String> players = new ArrayList<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            players.add(player.getName());
        }
        data.put("players", players);
        return data;
    }
}