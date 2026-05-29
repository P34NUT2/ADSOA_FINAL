package org.up.cd.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.up.cd.config.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MeshConnection {

    private static final Logger logger = LogManager.getLogger(MeshConnection.class);

    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private volatile boolean connected = false;

    private MeshConnection() {}

    public static MeshConnection getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final MeshConnection INSTANCE = new MeshConnection();
    }

    public synchronized boolean connect() {
        Config cfg = Config.getInstance();
        try {
            if (socket != null && !socket.isClosed()) socket.close();

            socket = new Socket(cfg.getTargetNodeHost(), cfg.getTargetNodePort());
            out = socket.getOutputStream();
            in  = socket.getInputStream();

            String handshake = "HELLO:CELL:" + cfg.getCellId();
            out.write(handshake.getBytes(StandardCharsets.UTF_8));
            out.flush();

            connected = true;
            logger.info("[MeshConnection] Connected to node {}:{}", cfg.getTargetNodeHost(), cfg.getTargetNodePort());
            return true;

        } catch (IOException e) {
            connected = false;
            logger.warn("[MeshConnection] Connect failed: {}", e.getMessage());
            return false;
        }
    }

    public synchronized void send(byte[] data) throws IOException {
        if (!connected || out == null) throw new IOException("Not connected");
        out.write(data);
        out.flush();
    }

    public InputStream getInputStream() { return in; }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public void markDisconnected() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    public void closeAll() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        connected = false;
    }
}
