package org.up.cd;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.up.cd.config.Config;
import org.up.cd.network.MeshConnection;
import org.up.cd.threads.DispatchThread;
import org.up.cd.threads.ReceiverThread;
import org.up.cd.threads.SenderThread;
import org.up.cd.ui.MenuThread;

public class CellApp {

    private static final Logger logger = LogManager.getLogger(CellApp.class);

    public static void main(String[] args) {
        logger.info("[CellApp] Starting...");

        // Load config: CLI arg first, then CWD config.json
        String configPath = args.length > 0 ? args[0] : null;
        if (!Config.getInstance().load(configPath)) {
            logger.error("[CellApp] Cannot load config. Exiting.");
            System.exit(1);
        }

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("[CellApp] Shutdown — closing connection");
            MeshConnection.getInstance().closeAll();
        }, "ShutdownHook"));

        // Initial connection to node (retry loop in background)
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

        // Wait for first connection before starting threads
        while (!MeshConnection.getInstance().isConnected()) {
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }

        // Start threads
        SenderThread senderThread = new SenderThread();

        Thread sender   = new Thread(senderThread,              "SenderThread");
        Thread receiver = new Thread(new ReceiverThread(senderThread), "ReceiverThread");
        Thread dispatch = new Thread(new DispatchThread(),       "DispatchThread");
        Thread menu     = new Thread(new MenuThread(),           "MenuThread");

        sender.setDaemon(true);
        receiver.setDaemon(true);
        dispatch.setDaemon(true);

        sender.start();
        receiver.start();
        dispatch.start();
        menu.start(); // non-daemon: keeps JVM alive for user input

        // Reconnect monitor — background thread
        Thread monitor = new Thread(() -> {
            while (true) {
                try { Thread.sleep(5000); } catch (InterruptedException ignored) { return; }
                if (!MeshConnection.getInstance().isConnected()) {
                    logger.warn("[CellApp] Disconnected from node — reconnecting...");
                    MeshConnection.getInstance().connect();
                }
            }
        }, "ReconnectMonitor");
        monitor.setDaemon(true);
        monitor.start();

        logger.info("[CellApp] All threads running. Cell={}", Config.getInstance().getCellId());

        try { menu.join(); } catch (InterruptedException ignored) {}
        logger.info("[CellApp] Exiting.");
    }
}
