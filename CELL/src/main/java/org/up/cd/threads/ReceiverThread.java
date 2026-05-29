package org.up.cd.threads;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.up.cd.config.Config;
import org.up.cd.network.MeshConnection;
import org.up.cd.protocol.BinaryProtocol;
import org.up.cd.queues.InputQueueManager;

import java.io.IOException;
import java.io.InputStream;

/**
 * Receiver thread. Reads raw bytes from the node connection.
 * - serviceNumber == 0  → ACK  → notify SenderThread
 * - serviceNumber  < 0  → response from server cell → enqueue for DispatchThread
 * - serviceNumber  > 0  → should not happen for client cells, ignore
 */
public class ReceiverThread implements Runnable {

    private static final Logger logger = LogManager.getLogger(ReceiverThread.class);
    private static final int BUFFER_SIZE = 4096;

    private final SenderThread senderThread;
    private volatile boolean running = true;

    public ReceiverThread(SenderThread senderThread) {
        this.senderThread = senderThread;
    }

    @Override
    public void run() {
        logger.info("[ReceiverThread] Started");

        while (running) {
            if (!MeshConnection.getInstance().isConnected()) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) { break; }
                continue;
            }

            InputStream is = MeshConnection.getInstance().getInputStream();
            if (is == null) { continue; }

            try {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead = is.read(buffer);
                if (bytesRead == -1) {
                    logger.warn("[ReceiverThread] Connection closed by node");
                    MeshConnection.getInstance().markDisconnected();
                    continue;
                }

                BinaryProtocol msg = BinaryProtocol.deserialize(buffer);
                int svc = msg.getServiceNumber();

                if (svc == BinaryProtocol.SERVICE_ACK) {
                    // ACK from a server cell
                    senderThread.receiveAck(msg.getEventId(), msg.getOriginEntity());

                } else if (svc < 0) {
                    // Response from server cell — only accept if addressed to this cell (huella filter)
                    String myId = Config.getInstance().getCellId();
                    String dest = msg.getDestEntity().trim();
                    if (!myId.equals(dest)) {
                        logger.debug("[ReceiverThread] Response destEntity={} not mine={} — ignored", dest, myId);
                        continue;
                    }
                    logger.info("[ReceiverThread] Response eventId={} serviceId={} from={}",
                            msg.getEventId(), svc, msg.getOriginEntity());
                    InputQueueManager.getInstance().enqueue(msg);

                } else {
                    logger.debug("[ReceiverThread] Ignored serviceId={} (client cell, not a server)", svc);
                }

            } catch (IOException e) {
                logger.error("[ReceiverThread] Read error: {}", e.getMessage());
                MeshConnection.getInstance().markDisconnected();
            } catch (Exception e) {
                logger.debug("[ReceiverThread] Deserialize error: {}", e.getMessage());
            }
        }
        logger.info("[ReceiverThread] Stopped");
    }

    public void stop() { running = false; }
}
