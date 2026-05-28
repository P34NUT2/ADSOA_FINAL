package org.up.cd.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * Thread runner to handle regular and handshake communication on a single socket.
 * Implements the handshake logic and 'Regla de Oro' message forwarding.
 */
public class SocketHandler implements Runnable {
    private static final Logger logger = LogManager.getLogger(SocketHandler.class);

    private final Socket socket;
    private String connectionId;

    public SocketHandler(Socket socket, String connectionId) {
        this.socket = socket;
        this.connectionId = connectionId;
    }

    @Override
    public void run() {
        String realId = connectionId;
        try {
            InputStream is = socket.getInputStream();
            byte[] buffer = new byte[4096];

            while (socket.isConnected() && !socket.isClosed()) {
                int bytesRead = is.read(buffer);
                if (bytesRead == -1) {
                    break;
                }

                // 1. Handshake Identification (Only if identity is temporary/unknown)
                if (realId.startsWith("INC_")) {
                    String msg = new String(buffer, 0, bytesRead).trim();
                    if (msg.startsWith("HELLO:")) {
                        String[] parts = msg.split(":");
                        if (parts.length >= 3) {
                            String type = parts[1]; // NODE or CELL
                            String senderId = parts[2].trim();

                            // Redundancy check
                            if (Connections.getInstance().isNodeConnected(senderId) || Connections.getInstance().isCellConnected(senderId)) {
                                logger.warn("Duplicate connection detected from: {}. Closing connection.", senderId);
                                break;
                            }

                            logger.info("Identity confirmed: {} (was {})", senderId, realId);

                            // Clean up temporary maps record
                            Connections.getInstance().removeConnection(realId);

                            // Register in the correct map in the Connections Singleton
                            if ("NODE".equalsIgnoreCase(type)) {
                                Connections.getInstance().putNode(
                                        senderId,
                                        socket.getInetAddress().getHostAddress(),
                                        socket.getPort(),
                                        "CONNECTED",
                                        Thread.currentThread().getName(),
                                        socket
                                );
                            } else if ("CELL".equalsIgnoreCase(type)) {
                                Connections.getInstance().putCell(
                                        senderId,
                                        socket.getInetAddress().getHostAddress(),
                                        socket.getPort(),
                                        "CONNECTED",
                                        Thread.currentThread().getName(),
                                        socket
                                );
                            }

                            realId = senderId;
                            continue;
                        }
                    }
                }

                // 2. 'Regla de Oro' Forwarding Layer
                boolean isRegisteredNode = Connections.getInstance().getNodes().containsKey(realId);
                boolean isRegisteredCell = Connections.getInstance().getCells().containsKey(realId);

                if (isRegisteredNode || isRegisteredCell) {
                    boolean onlyCells = isRegisteredNode; // NODE -> CELLs only | CELL -> ALL (Total Flooding)

                    if (onlyCells) {
                        logger.info("[FWD] {} bytes from NODE {} -> forwarding to LOCAL CELLs only.", bytesRead, realId);
                    } else {
                        logger.info("[FWD] {} bytes from CELL {} -> performing TOTAL FLOODING (nodes and cells).", bytesRead, realId);
                    }

                    broadcast(buffer, bytesRead, realId, onlyCells);
                }
            }
        } catch (IOException e) {
            logger.warn("Connection lost with peer {}: {}", realId, e.getMessage());
        } finally {
            cleanupConnection(realId);
            if (!realId.equals(connectionId)) {
                cleanupConnection(connectionId);
            }
        }
    }

    /**
     * Broadcasts the received data to appropriate sockets based on the 'Regla de Oro' rules.
     */
    private void broadcast(byte[] data, int len, String senderId, boolean onlyCells) {
        // Forward to other active Nodes (only if not restricted to cells)
        if (!onlyCells) {
            Connections.getInstance().getNodes().forEach((id, info) -> {
                if (!id.equals(senderId) && "CONNECTED".equals(info.status) && info.socket != null && !info.socket.isClosed()) {
                    try {
                        info.socket.getOutputStream().write(data, 0, len);
                        info.socket.getOutputStream().flush();
                        logger.info("  [REP] Forwarded to NODE {}", id);
                    } catch (IOException e) {
                        logger.warn("Error sending data to NODE {}: {}", id, e.getMessage());
                    }
                }
            });
        }

        // Forward to all active Cell connections
        Connections.getInstance().getCells().forEach((id, info) -> {
            if (!id.equals(senderId) && "CONNECTED".equals(info.status) && info.socket != null && !info.socket.isClosed()) {
                try {
                    info.socket.getOutputStream().write(data, 0, len);
                    info.socket.getOutputStream().flush();
                    logger.info("  [REP] Forwarded to CELL {}", id);
                } catch (IOException e) {
                    logger.warn("Error sending data to CELL {}: {}", id, e.getMessage());
                }
            }
        });
    }

    /**
     * Frees resources, updates metadata mappings on disconnect.
     */
    private void cleanupConnection(String id) {
        Connections.getInstance().removeConnection(id);
        logger.info("Connection {} closed and cleaned up from registers.", id);
    }
}
