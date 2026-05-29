package org.up.cd.queues;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.up.cd.protocol.BinaryProtocol;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Singleton. Holds incoming service requests waiting for DispatchThread.
 */
public class InputQueueManager {

    private static final Logger logger = LogManager.getLogger(InputQueueManager.class);

    private final LinkedBlockingQueue<BinaryProtocol> queue = new LinkedBlockingQueue<>();

    private InputQueueManager() {}

    public static InputQueueManager getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final InputQueueManager INSTANCE = new InputQueueManager();
    }

    public void enqueue(BinaryProtocol msg) {
        queue.add(msg);
        logger.debug("[InQueue] Enqueued serviceId={} eventId={} from={}",
                msg.getServiceNumber(), msg.getEventId(), msg.getOriginEntity());
    }

    public BinaryProtocol poll(long timeoutMs) throws InterruptedException {
        return queue.poll(timeoutMs, TimeUnit.MILLISECONDS);
    }
}
