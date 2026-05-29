package org.up.cd.threads;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.up.cd.protocol.BinaryProtocol;
import org.up.cd.queues.InputQueueManager;

import java.nio.charset.StandardCharsets;

/**
 * Dispatch thread. Drains the input queue and logs/displays results
 * received from server cells.
 */
public class DispatchThread implements Runnable {

    private static final Logger logger = LogManager.getLogger(DispatchThread.class);
    private static final long POLL_TIMEOUT_MS = 200;

    private volatile boolean running = true;

    @Override
    public void run() {
        logger.info("[DispatchThread] Started");

        while (running) {
            try {
                BinaryProtocol msg = InputQueueManager.getInstance().poll(POLL_TIMEOUT_MS);
                if (msg == null) continue;

                String result = new String(msg.getData(), StandardCharsets.UTF_8).trim();
                int svc = Math.abs(msg.getServiceNumber());

                logger.info("[DispatchThread] Response: eventId={} service={} result={} from={}",
                        msg.getEventId(), svc, result, msg.getOriginEntity());

                System.out.printf("%n[RESULT] event=%d service=%d result=%s (from: %s)%n> ",
                        msg.getEventId(), svc, result, msg.getOriginEntity());

            } catch (InterruptedException e) {
                break;
            }
        }
        logger.info("[DispatchThread] Stopped");
    }

    public void stop() { running = false; }
}
