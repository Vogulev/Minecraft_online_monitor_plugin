package com.vogulev.online_monitor.web;


import java.util.logging.Logger;

import com.vogulev.online_monitor.DatabaseManager;
import com.vogulev.online_monitor.OnlineMonitorPlugin;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;


/**
 * Web server based on Embedded Jetty
 * Provides web panel for viewing online statistics
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
     * Start the web server
     */
    public void start() {
        try {
            ServerConnector connector = new ServerConnector(server);
            connector.setHost("0.0.0.0"); // Listen on all interfaces
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
            logger.info("============================================");
            logger.info("Web panel started successfully!");
            logger.info("Access it at: http://localhost:" + port);
            logger.info("Or from network: http://<server-ip>:" + port);
            logger.info("============================================");
        }
        catch (Exception e)
        {
            logger.severe("============================================");
            logger.severe("FAILED TO START WEB SERVER!");
            logger.severe("Error: " + e.getMessage());
            logger.severe("Port " + port + " might be already in use");
            logger.severe("============================================");
            e.printStackTrace();
        }
    }


    /**
     * Stop the web server
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
     * Check if the server is running
     */
    public boolean isRunning()
    {
        return server != null && server.isRunning();
    }
}