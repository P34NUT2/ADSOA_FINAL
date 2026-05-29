package org.up.cd.threads;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.up.cd.config.Config;
import org.up.cd.network.MeshConnection;
import org.up.cd.protocol.BinaryProtocol;
import org.up.cd.queues.InputQueueManager;

import java.io.DataInputStream;
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

            // Wrap once per connection — readFully guarantees exactly one message per call,
            // preventing TCP coalescing from merging two messages into one read.
            DataInputStream dis = new DataInputStream(is);
            try {
                while (running && MeshConnection.getInstance().isConnected()) {
                    BinaryProtocol msg = BinaryProtocol.read(dis);
                    int svc = msg.getServiceNumber();

                    if (svc == BinaryProtocol.SERVICE_ACK) {
                        senderThread.receiveAck(msg.getEventId(), msg.getOriginEntity());

                    } else if (svc < 0) {
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
                        logger.debug("[ReceiverThread] Ignored serviceId={}", svc);
                    }
                }
            } catch (IOException e) {
                logger.warn("[ReceiverThread] Connection lost: {}", e.getMessage());
                MeshConnection.getInstance().markDisconnected();
            }
        }
        logger.info("[ReceiverThread] Stopped");
    }

    public void stop() { running = false; }
}
