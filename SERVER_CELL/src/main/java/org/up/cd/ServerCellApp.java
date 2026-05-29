package org.up.cd;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.up.cd.config.Config;
import org.up.cd.network.MeshConnection;
import org.up.cd.threads.DispatchThread;
import org.up.cd.threads.ReceiverThread;

public class ServerCellApp {

    private static final Logger logger = LogManager.getLogger(ServerCellApp.class);

    public static void main(String[] args) {
        logger.info("[ServerCellApp] Starting...");

        String configPath = args.length > 0 ? args[0] : null;
        if (!Config.getInstance().load(configPath)) {
            logger.error("[ServerCellApp] Cannot load config. Exiting.");
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("[ServerCellApp] Shutdown — closing connection");
            MeshConnection.getInstance().closeAll();
        }, "ShutdownHook"));

        // Initial connection
        Thread connector = new Thread(() -> {
            while (!MeshConnection.getInstance().isConnected()) {
                MeshConnection.getInstance().connect();
                if (!MeshConnection.getInstance().isConnected()) {
                    try { Thread.sleep(3000); } catch (InterruptedException ignored) { return; }
                }
            }
        }, "Connector");
        connector.setDaemon(true);
        connector.start();

        while (!MeshConnection.getInstance().isConnected()) {
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }

        Thread receiver = new Thread(new ReceiverThread(), "ReceiverThread");
        Thread dispatch = new Thread(new DispatchThread(), "DispatchThread");

        receiver.setDaemon(true);
        dispatch.setDaemon(true);

        receiver.start();
        dispatch.start();

        // Reconnect monitor
        Thread monitor = new Thread(() -> {
            while (true) {
                try { Thread.sleep(5000); } catch (InterruptedException ignored) { return; }
                if (!MeshConnection.getInstance().isConnected()) {
                    logger.warn("[ServerCellApp] Disconnected — reconnecting...");
                    MeshConnection.getInstance().connect();
                }
            }
        }, "ReconnectMonitor");
        monitor.setDaemon(false); // keep JVM alive
        monitor.start();

        logger.info("[ServerCellApp] Running. Cell={}", Config.getInstance().getCellId());
        try { monitor.join(); } catch (InterruptedException ignored) {}
    }
}
