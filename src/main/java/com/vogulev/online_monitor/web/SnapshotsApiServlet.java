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

/**
 * API endpoint для получения исторических данных (снапшотов)
 * GET /api/snapshots?type=hourly&days=7 - почасовые средние
 * GET /api/snapshots?type=daily&days=30 - дневные средние
 * GET /api/snapshots?type=weekday&weeks=4 - средние по дням недели
 * GET /api/snapshots?type=peak&days=7 - пиковые часы
 */
public class SnapshotsApiServlet extends HttpServlet {
    private final DatabaseManager database;
    private final Gson gson;

    public SnapshotsApiServlet(DatabaseManager database) {
        this.database = database;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");

        try {
            String type = req.getParameter("type");
            if (type == null) {
                type = "hourly";
            }

            Object data;
            switch (type.toLowerCase()) {
                case "hourly":
                    int hourlyDays = getIntParam(req, "days", 7);
                    data = database.getHourlyAverages(hourlyDays);
                    break;
                case "daily":
                    int dailyDays = getIntParam(req, "days", 30);
                    data = database.getDailyAverages(dailyDays);
                    break;
                case "weekday":
                    int weeks = getIntParam(req, "weeks", 4);
                    data = database.getWeekdayAverages(weeks);
                    break;
                case "peak":
                    int peakDays = getIntParam(req, "days", 7);
                    data = database.getPeakHours(peakDays);
                    break;
                default:
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Invalid type parameter. Use: hourly, daily, weekday, or peak");
                    resp.getWriter().write(gson.toJson(error));
                    return;
            }

            resp.getWriter().write(gson.toJson(data));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            resp.getWriter().write(gson.toJson(error));
        }
    }

    private int getIntParam(HttpServletRequest req, String paramName, int defaultValue) {
        String param = req.getParameter(paramName);
        if (param != null) {
            try {
                return Integer.parseInt(param);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}