package com.vogulev.online_monitor.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vogulev.online_monitor.DatabaseManager;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.math.NumberUtils.isParsable;


/**
 * API endpoint для получения информации об игроках
 * GET /api/players - топ игроков
 * GET /api/players?name=PlayerName - статистика конкретного игрока
 */
public class PlayersApiServlet extends HttpServlet {
    private final DatabaseManager database;
    private final Gson gson;

    public PlayersApiServlet(DatabaseManager database) {
        this.database = database;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");

        try {
            String playerName = req.getParameter("name");

            if (playerName != null && !playerName.isEmpty()) {
                Map<String, Object> playerStats = new HashMap<>();
                playerStats.put("name", playerName);
                playerStats.put("joinCount", database.getPlayerJoinCount(playerName));
                playerStats.put("totalPlaytime", database.getPlayerTotalPlaytime(playerName));
                resp.getWriter().write(gson.toJson(playerStats));
            } else {
                String limitParam = req.getParameter("limit");
                int limit = isParsable(limitParam) ? Integer.parseInt(limitParam) : 10;
                Map<String, Integer> topPlayers = database.getTopPlayersByJoins(limit);
                resp.getWriter().write(gson.toJson(topPlayers));
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            resp.getWriter().write(gson.toJson(error));
        }
    }
}