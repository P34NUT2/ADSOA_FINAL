package org.up.cd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.up.cd.network.Connections;
import org.up.cd.network.SocketHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Main application class for the distributed computing Node.
 * Manages configuration loading (external/internal), ServerSocket listening, and peer monitoring.
 */
public class NodoApp {
    private static final Logger logger = LogManager.getLogger(NodoApp.class);

    // Node Configuration
    public static String nodeId;
    public static int listenerPort;
    private static JsonNode peers;
    private static String configFilePath = null;

    public static void main(String[] args) {
        logger.info("Starting Node App...");

        // Check if an external config file was supplied via command-line arguments
        if (args.length > 0) {
            configFilePath = args[0];
        }

        // Load configuration
        if (!loadConfiguration()) {
            logger.fatal("Critical error: Node cannot start without configuration.");
            logger.info("Starting in listening mode as backup...");
        } else {
            logger.info("Configuration loaded successfully.");
            // Hilo de monitoreo para asegurar que todos los nodos pares estén conectados
            new Thread(NodoApp::monitorPeerConnections, "PeerMonitor").start();
        }

        // Hilo de escucha para permitir que otros se conecten a nosotros
        new Thread(NodoApp::connectionListener, "ServerListener").start();

        // Shutdown hook to release socket descriptors
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down node... Releasing resources.");
            Connections.getInstance().getListNode().forEach(socket -> {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException ignored) {}
            });
            Connections.getInstance().getListCell().forEach(socket -> {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException ignored) {}
            });
            logger.info("Node stopped successfully.");
        }, "ShutdownThread"));
    }

    /**
     * Listens for incoming connections (passive server).
     */
    private static void connectionListener() {
        try (ServerSocket serverSocket = new ServerSocket(listenerPort)) {
            logger.info("ServerSocket ready and listening on port {}", listenerPort);

            while (true) {
                Socket incomingSocket = serverSocket.accept();
                String remoteHost = incomingSocket.getInetAddress().getHostAddress();
                String remoteAddress = incomingSocket.getRemoteSocketAddress().toString();

                logger.info("New incoming connection accepted from: {}", remoteAddress);

                // Register temporarily as a cell connection until it identifies itself
                String tempId = "INC_" + remoteAddress;
                Connections.getInstance().putCell(
                        tempId,
                        remoteHost,
                        incomingSocket.getPort(),
                        "CONNECTED",
                        "",
                        incomingSocket
                );

                // Dispatch to SocketHandler thread
                Thread handlerThread = new Thread(new SocketHandler(incomingSocket, tempId), "ConnHandler-" + tempId);
                Connections.getInstance().getCells().get(tempId).threadName = handlerThread.getName();
                handlerThread.start();
            }
        } catch (IOException e) {
            logger.error("Fatal error in ServerSocket: {}. Incoming connection processing stopped.", e.getMessage());
        }
    }

    /**
     * Periodically monitors peer node status and initiates reconnection if necessary.
     */
    private static void monitorPeerConnections() {
        while (true) {
            try {
                StringBuilder statusReport = new StringBuilder("\n--- NODES STATUS ---\n");
                Connections.getInstance().getNodes().forEach((id, info) -> {
                    statusReport.append(String.format("[%s] -> %s | Thread: %s\n",
                            id, info.status, info.threadName.isEmpty() ? "NONE" : info.threadName));
                });
                statusReport.append("--- CELLS STATUS ---\n");
                Connections.getInstance().getCells().forEach((id, info) -> {
                    statusReport.append(String.format("[%s] -> %s | Thread: %s\n",
                            id, info.status, info.threadName.isEmpty() ? "NONE" : info.threadName));
                });
                logger.info(statusReport.toString());

                // Proactive peer reconnection
                Connections.getInstance().getNodes().forEach((id, info) -> {
                    if ("DISCONNECTED".equals(info.status)) {
                        logger.info("Attempting connection to peer {} at {}:{}...", id, info.host, info.port);
                        createThreadConnection(id, info.host, info.port);
                    }
                });

                Thread.sleep(5000);
            } catch (InterruptedException e) {
                logger.error("Peer monitor thread interrupted: {}", e.getMessage());
            } catch (Exception e) {
                logger.error("Error in peer monitor: {}", e.getMessage());
            }
        }
    }

    /**
     * Proactively attempts to establish an outbound connection to a peer node.
     */
    private static void createThreadConnection(String targetId, String host, int port) {
        new Thread(() -> {
            // Avoid redundant connections
            if (Connections.getInstance().isNodeConnected(targetId)) {
                return;
            }

            try {
                Socket socket = new Socket(host, port);

                // Send Handshake
                String handshake = "HELLO:NODE:" + nodeId;
                OutputStream os = socket.getOutputStream();
                os.write(handshake.getBytes());
                os.flush();

                logger.info("Handshake sent to {}: {}", targetId, handshake);

                // Register active connection in the Singleton
                Connections.getInstance().putNode(targetId, host, port, "CONNECTED", Thread.currentThread().getName(), socket);

                // Handover to socket communication handler
                new SocketHandler(socket, targetId).run();

            } catch (IOException e) {
                // Connection failed; peer monitor will retry in next loop cycle
            }
        }, "ConnThread-" + targetId).start();
    }

    /**
     * Loads configuration (supporting custom path, current directory, or internal fallback)
     */
    private static boolean loadConfiguration() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = null;

            // 1. Try to load from custom path if provided in arguments
            if (configFilePath != null) {
                File file = new File(configFilePath);
                if (file.exists()) {
                    is = new FileInputStream(file);
                    logger.info("Loading configuration from custom command-line path: {}", configFilePath);
                } else {
                    logger.error("Config file not found at specified path: {}", configFilePath);
                }
            }

            // 2. Try to load from current working directory
            if (is == null) {
                File file = new File("conections.json");
                if (file.exists()) {
                    is = new FileInputStream(file);
                    logger.info("Loading configuration from current working directory: conections.json");
                }
            }

            // 3. Fallback to classpath resource
            if (is == null) {
                is = NodoApp.class.getClassLoader().getResourceAsStream("conections.json");
                if (is != null) {
                    logger.info("Loading default internal configuration from resources classpath.");
                }
            }

            if (is == null) {
                logger.error("Configuration file 'conections.json' not found externally or internally.");
                return false;
            }

            JsonNode config = mapper.readTree(is);
            nodeId = config.get("nodeId").asText();
            listenerPort = config.get("listener_port").asInt();
            peers = config.get("peers");

            if (peers != null && peers.isArray()) {
                for (JsonNode peer : peers) {
                    String pId = peer.get("id").asText();
                    String host = peer.get("host").asText();
                    int port = peer.get("port").asInt();
                    Connections.getInstance().putNode(pId, host, port, "DISCONNECTED", "", null);
                }
            }

            logger.info("Initial configuration loaded for Node ID: {}", nodeId);
            return true;
        } catch (Exception e) {
            logger.error("Error loading or parsing connections config: {}", e.getMessage());
            return false;
        }
    }
}
