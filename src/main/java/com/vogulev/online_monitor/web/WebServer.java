package com.vogulev.online_monitor.web;


import java.util.logging.Logger;

import com.vogulev.online_monitor.DatabaseManager;
import com.vogulev.online_monitor.OnlineMonitorPlugin;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;


/**
 * Веб-сервер на базе Embedded Jetty
 * Предоставляет веб-панель для просмотра статистики онлайна
 */
public class WebServer {
    private static final Logger logger = Logger.getLogger("OnlineMonitor");

    private final Server server;

    private final int port;

    private final OnlineMonitorPlugin plugin;

    private final DatabaseManager database;


    public WebServer(OnlineMonitorPlugin plugin, DatabaseManager database, int port) {
        this.plugin = plugin;
        this.database = database;
        this.port = port;
        this.server = new Server();
    }


    /**
     * Запустить веб-сервер
     */
    public void start() {
        try {
            ServerConnector connector = new ServerConnector(server);
            connector.setPort(port);
            server.addConnector(connector);

            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            server.setHandler(context);

            context.addServlet(new ServletHolder(new StatsApiServlet(database, plugin)), "/api/stats");
            context.addServlet(new ServletHolder(new OnlineApiServlet(database, plugin)), "/api/online");
            context.addServlet(new ServletHolder(new PlayersApiServlet(database)), "/api/players");
            context.addServlet(new ServletHolder(new SnapshotsApiServlet(database)), "/api/snapshots");
            context.addServlet(new ServletHolder(new DashboardServlet()), "/*");

            server.start();
            logger.info("Web panel started at server port: " + port);
        }
        catch (Exception e)
        {
            logger.severe("Failed to start web server: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Остановить веб-сервер
     */
    public void stop()
    {
        try
        {
            if (server != null && server.isRunning())
            {
                server.stop();
                logger.info("Web server stopped");
            }
        }
        catch (Exception e)
        {
            logger.severe("Failed to stop web server: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Проверить, запущен ли сервер
     */
    public boolean isRunning()
    {
        return server != null && server.isRunning();
    }
}