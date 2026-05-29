package org.up.cd.threads;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.up.cd.config.Config;
import org.up.cd.network.MeshConnection;
import org.up.cd.protocol.BinaryProtocol;
import org.up.cd.queues.OutputQueueManager;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sender thread. Extracts messages from output queues one at a time,
 * sends each through MeshConnection, then waits for minAcks unique ACKs
 * (foliado protocol) before sending the next message.
 */
public class SenderThread implements Runnable {

    private static final Logger logger = LogManager.getLogger(SenderThread.class);
    private static final int ACK_TIMEOUT_SECONDS = 30;

    private final AtomicLong eventCounter = new AtomicLong(0);
    private final Map<Long, PendingAck> pendingAcks = new ConcurrentHashMap<>();
    private volatile boolean running = true;

    static class PendingAck {
        private final Set<String> ackSenders = ConcurrentHashMap.newKeySet();
        private final CountDownLatch latch;
        private final int minAcks;

        PendingAck(int minAcks) {
            this.minAcks = minAcks;
            this.latch = new CountDownLatch(minAcks);
        }

        /** Returns true if this sender had not ACKed yet (dedup by huella). */
        boolean addAck(String senderHuella) {
            if (ackSenders.add(senderHuella)) {
                latch.countDown();
                return true;
            }
            return false;
        }

        boolean await() throws InterruptedException {
            return latch.await(ACK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        int getAckCount() { return ackSenders.size(); }
    }

    @Override
    public void run() {
        logger.info("[SenderThread] Started");
        Config cfg = Config.getInstance();

        while (running) {
            if (!MeshConnection.getInstance().isConnected()) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) { break; }
                continue;
            }

            BinaryProtocol msg = OutputQueueManager.getInstance().pollNext();
            if (msg == null) {
                try { Thread.sleep(100); } catch (InterruptedException ignored) { break; }
                continue;
            }

            long eventId = eventCounter.incrementAndGet();
            // Rebuild message with assigned eventId
            BinaryProtocol stamped = new BinaryProtocol(
                    msg.getOriginBusiness(), msg.getOriginSubsystem(), msg.getOriginEntity(),
                    msg.getDestBusiness(),   msg.getDestSubsystem(),   msg.getDestEntity(),
                    eventId, msg.getServiceNumber(), msg.getData()
            );

            PendingAck pending = new PendingAck(cfg.getMinAcks());
            pendingAcks.put(eventId, pending);

            try {
                MeshConnection.getInstance().send(stamped.serialize());
                logger.info("[SenderThread] Sent eventId={} serviceId={} waiting for {} ACKs",
                        eventId, msg.getServiceNumber(), cfg.getMinAcks());

                boolean acked = pending.await();
                if (acked) {
                    logger.info("[SenderThread] eventId={} got all {} ACKs — sending next",
                            eventId, pending.getAckCount());
                } else {
                    logger.warn("[SenderThread] eventId={} ACK timeout after {}s (got {}/{})",
                            eventId, ACK_TIMEOUT_SECONDS, pending.getAckCount(), cfg.getMinAcks());
                }

            } catch (Exception e) {
                logger.error("[SenderThread] Send error eventId={}: {}", eventId, e.getMessage());
                MeshConnection.getInstance().markDisconnected();
            } finally {
                pendingAcks.remove(eventId);
            }
        }
        logger.info("[SenderThread] Stopped");
    }

    /** Called by ReceiverThread when an ACK arrives. */
    public void receiveAck(long eventId, String senderHuella) {
        PendingAck pending = pendingAcks.get(eventId);
        if (pending != null) {
            boolean counted = pending.addAck(senderHuella);
            logger.info("[SenderThread] ACK eventId={} from={} counted={}", eventId, senderHuella, counted);
        } else {
            logger.debug("[SenderThread] ACK for unknown eventId={}", eventId);
        }
    }

    public void stop() { running = false; }
}
