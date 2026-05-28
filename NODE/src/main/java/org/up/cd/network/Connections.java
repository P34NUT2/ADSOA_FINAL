package org.up.cd.network;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton to manage configurations and active connections for nodes and cells.
 * Safe for multi-threaded access using ConcurrentHashMap.
 */
public class Connections {
    
    private Connections() {
    }
    
    public static Connections getInstance() {
        return ConnectionsHolder.INSTANCE;
    }
    
    private static class ConnectionsHolder {
        private static final Connections INSTANCE = new Connections();
    }

    /**
     * Nested class representing connection metadata, status, and physical socket.
     */
    public static class ConnectionInfo {
        public String type; // NODE or CELL
        public String host;
        public int port;
        public String status; // CONNECTED / DISCONNECTED
        public String threadName; // Name of the assigned handler thread
        public Socket socket; // The active physical socket descriptor

        public ConnectionInfo(String type, String host, int port, String status, String threadName, Socket socket) {
            this.type = type;
            this.host = host;
            this.port = port;
            this.status = status;
            this.threadName = threadName;
            this.socket = socket;
        }
    }
    
    // Unified maps of ConnectionInfo for nodes and cells
    private final Map<String, ConnectionInfo> nodes = new ConcurrentHashMap<>();
    private final Map<String, ConnectionInfo> cells = new ConcurrentHashMap<>();
    
    public Map<String, ConnectionInfo> getNodes() {
        return nodes;
    }
    
    public Map<String, ConnectionInfo> getCells() {
        return cells;
    }
    
    /**
     * Gets a list of all active physical Node sockets.
     */
    public List<Socket> getListNode() {
        List<Socket> activeSockets = new ArrayList<>();
        for (ConnectionInfo info : nodes.values()) {
            if ("CONNECTED".equals(info.status) && info.socket != null && !info.socket.isClosed()) {
                activeSockets.add(info.socket);
            }
        }
        return activeSockets;
    }
    
    /**
     * Gets a list of all active physical Cell sockets.
     */
    public List<Socket> getListCell() {
        List<Socket> activeSockets = new ArrayList<>();
        for (ConnectionInfo info : cells.values()) {
            if ("CONNECTED".equals(info.status) && info.socket != null && !info.socket.isClosed()) {
                activeSockets.add(info.socket);
            }
        }
        return activeSockets;
    }
    
    /**
     * Registers or updates a Node connection.
     */
    public void putNode(String id, String host, int port, String status, String threadName, Socket socket) {
        ConnectionInfo info = nodes.get(id);
        if (info == null) {
            info = new ConnectionInfo("NODE", host, port, status, threadName, socket);
            nodes.put(id, info);
        } else {
            info.host = host;
            info.port = port;
            info.status = status;
            info.threadName = threadName;
            info.socket = socket;
        }
    }
    
    /**
     * Registers or updates a Cell connection.
     */
    public void putCell(String id, String host, int port, String status, String threadName, Socket socket) {
        ConnectionInfo info = cells.get(id);
        if (info == null) {
            info = new ConnectionInfo("CELL", host, port, status, threadName, socket);
            cells.put(id, info);
        } else {
            info.host = host;
            info.port = port;
            info.status = status;
            info.threadName = threadName;
            info.socket = socket;
        }
    }
    
    /**
     * Verifies if a node ID belongs to an actively connected peer.
     */
    public Boolean isNodeConnected(String id) {
        ConnectionInfo info = nodes.get(id);
        return info != null && "CONNECTED".equals(info.status);
    }
    
    /**
     * Verifies if a cell ID belongs to an actively connected cell.
     */
    public Boolean isCellConnected(String id) {
        ConnectionInfo info = cells.get(id);
        return info != null && "CONNECTED".equals(info.status);
    }
    
    /**
     * Removes/cleans up a connection by socket.
     */
    public void removeSocket(Socket socket) {
        // Search and disconnect in nodes
        for (ConnectionInfo info : nodes.values()) {
            if (info.socket == socket) {
                info.status = "DISCONNECTED";
                info.threadName = "";
                info.socket = null;
            }
        }
        // Search and remove in cells (since cell connections are dynamic clients)
        cells.entrySet().removeIf(entry -> entry.getValue().socket == socket);
    }

    /**
     * Removes/cleans up a connection by ID.
     */
    public void removeConnection(String id) {
        if (nodes.containsKey(id)) {
            ConnectionInfo info = nodes.get(id);
            info.status = "DISCONNECTED";
            info.threadName = "";
            info.socket = null;
        }
        cells.remove(id);
    }
}
