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
 * API endpoint for retrieving player information
 * GET /api/players - top players
 * GET /api/players?name=PlayerName - specific player statistics
 */
public class PlayersApiServlet extends HttpServlet {
    private final DatabaseManager database;
    private final Gson gson;

    public PlayersApiServlet(final DatabaseManager database) {
        this.database = database;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");

        try {
            final String playerName = req.getParameter("name");

            if (playerName != null && !playerName.isEmpty()) {
                final Map<String, Object> playerStats = new HashMap<>();
                playerStats.put("name", playerName);
                playerStats.put("joinCount", database.getPlayerJoinCount(playerName));
                playerStats.put("totalPlaytime", database.getPlayerTotalPlaytime(playerName));
                playerStats.put("deaths", database.getPlayerDeaths(playerName));
                playerStats.put("mobKills", database.getPlayerMobKills(playerName));
                playerStats.put("playerKills", database.getPlayerPlayerKills(playerName));
                playerStats.put("blocksBroken", database.getPlayerBlocksBroken(playerName));
                playerStats.put("blocksPlaced", database.getPlayerBlocksPlaced(playerName));
                playerStats.put("messagesSent", database.getPlayerMessagesSent(playerName));
                resp.getWriter().write(gson.toJson(playerStats));
            } else {
                final String limitParam = req.getParameter("limit");
                final int limit = isParsable(limitParam) ? Integer.parseInt(limitParam) : 10;
                final Map<String, Integer> topPlayers = database.getTopPlayersByJoins(limit);
                resp.getWriter().write(gson.toJson(topPlayers));
            }
        } catch (final Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            final Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            resp.getWriter().write(gson.toJson(error));
        }
    }
}