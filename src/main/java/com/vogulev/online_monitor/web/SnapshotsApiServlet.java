package com.vogulev.online_monitor.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vogulev.online_monitor.DatabaseManager;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API endpoint for retrieving historical data (snapshots)
 * GET /api/snapshots?type=hourly&days=7 - hourly averages
 * GET /api/snapshots?type=daily&days=30 - daily averages
 * GET /api/snapshots?type=weekday&weeks=4 - averages by weekdays
 * GET /api/snapshots?type=peak&days=7 - peak hours
 */
public class SnapshotsApiServlet extends HttpServlet {
    private final DatabaseManager database;
    private final Gson gson;

    public SnapshotsApiServlet(final DatabaseManager database) {
        this.database = database;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");

        try {
            String type = req.getParameter("type");
            if (type == null) {
                type = "hourly";
            }

            final Object data;
            switch (type.toLowerCase()) {
                case "hourly":
                    final int hourlyDays = getIntParam(req, "days", 7);
                    data = database.getHourlyAverages(hourlyDays);
                    break;
                case "daily":
                    final int dailyDays = getIntParam(req, "days", 30);
                    data = database.getDailyAverages(dailyDays);
                    break;
                case "weekday":
                    final int weeks = getIntParam(req, "weeks", 4);
                    data = convertWeekdayMap(database.getWeekdayAverages(weeks));
                    break;
                case "peak":
                    final int peakDays = getIntParam(req, "days", 7);
                    data = database.getPeakHours(peakDays);
                    break;
                default:
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    final Map<String, String> error = new HashMap<>();
                    error.put("error", "Invalid type parameter. Use: hourly, daily, weekday, or peak");
                    resp.getWriter().write(gson.toJson(error));
                    return;
            }

            resp.getWriter().write(gson.toJson(data));
        } catch (final Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            final Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            resp.getWriter().write(gson.toJson(error));
        }
    }

    private int getIntParam(final HttpServletRequest req, final String paramName, final int defaultValue) {
        final String param = req.getParameter(paramName);
        if (param != null) {
            try {
                return Integer.parseInt(param);
            } catch (final NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private Map<String, Double> convertWeekdayMap(final Map<Integer, Double> weekdayData) {
        final Map<String, Double> result = new LinkedHashMap<>();
        final String[] weekdays = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

        for (final Map.Entry<Integer, Double> entry : weekdayData.entrySet()) {
            final int dayNum = entry.getKey();
            if (dayNum >= 0 && dayNum < weekdays.length) {
                result.put(weekdays[dayNum], entry.getValue());
            }
        }

        return result;
    }
}