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
 * Receiver thread for SERVER_CELL.
 * On incoming service request (serviceNumber > 0):
 *   1. Send ACK back to requester (foliado protocol).
 *   2. Enqueue message for DispatchThread.
 */
public class ReceiverThread implements Runnable {

    private static final Logger logger = LogManager.getLogger(ReceiverThread.class);
    private static final int BUFFER_SIZE = 4096;

    private volatile boolean running = true;

    @Override
    public void run() {
        logger.info("[ReceiverThread] Started");

        while (running) {
            if (!MeshConnection.getInstance().isConnected()) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) { break; }
                continue;
            }

            InputStream is = MeshConnection.getInstance().getInputStream();
            if (is == null) continue;

            // Wrap once per connection — readFully guarantees exactly one message per call,
            // preventing TCP coalescing from merging two messages into one read.
            DataInputStream dis = new DataInputStream(is);
            try {
                while (running && MeshConnection.getInstance().isConnected()) {
                    BinaryProtocol msg = BinaryProtocol.read(dis);
                    int svc = msg.getServiceNumber();

                    if (svc == BinaryProtocol.SERVICE_ACK) {
                        logger.debug("[ReceiverThread] Received ACK (ignored by server cell)");

                    } else if (svc > 0) {
                        if (!Config.getInstance().handlesService(svc)) {
                            logger.debug("[ReceiverThread] serviceId={} not mine — ignored", svc);
                            continue;
                        }
                        logger.info("[ReceiverThread] Request eventId={} serviceId={} from={}",
                                msg.getEventId(), svc, msg.getOriginEntity());
                        sendAck(msg);
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

    private void sendAck(BinaryProtocol original) {
        Config cfg = Config.getInstance();
        String cellId = cfg.getCellId();
        try {
            BinaryProtocol ack = new BinaryProtocol(
                    cellId, "SERVER_CELL", cellId,          // origin = me (huella)
                    original.getOriginBusiness(),            // dest = original sender
                    original.getOriginSubsystem(),
                    original.getOriginEntity(),
                    original.getEventId(),                   // same eventId (foliado)
                    BinaryProtocol.SERVICE_ACK,              // serviceNumber = 0
                    new byte[0]
            );
            MeshConnection.getInstance().send(ack.serialize());
            logger.info("[ReceiverThread] ACK sent eventId={} to={}", original.getEventId(), original.getOriginEntity());
        } catch (Exception e) {
            logger.error("[ReceiverThread] ACK send error: {}", e.getMessage());
        }
    }

    public void stop() { running = false; }
}
