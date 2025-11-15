package com.vogulev.online_monitor.web;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Главная страница веб-панели с графиками и статистикой
 */
public class DashboardServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger("OnlineMonitor");
    private static final String HTML_RESOURCE_PATH = "/web/dashboard.html";

    private String cachedHtml = null;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");

        String html = getHtmlPage();
        if (html != null) {
            resp.getWriter().write(html);
        } else {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error loading dashboard page");
        }
    }

    /**
     * Загружает HTML страницу из resources
     * Кэширует результат для повышения производительности
     */
    private String getHtmlPage() {
        if (cachedHtml != null) {
            return cachedHtml;
        }

        try (InputStream inputStream = getClass().getResourceAsStream(HTML_RESOURCE_PATH)) {
            if (inputStream == null) {
                logger.severe("Dashboard HTML file not found: " + HTML_RESOURCE_PATH);
                return null;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                cachedHtml = reader.lines().collect(Collectors.joining("\n"));
                logger.info("Dashboard HTML loaded successfully from " + HTML_RESOURCE_PATH);
                return cachedHtml;
            }
        } catch (IOException e) {
            logger.severe("Error loading dashboard HTML: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Очистить кэш HTML (полезно для разработки)
     */
    public void clearCache() {
        cachedHtml = null;
    }
}