package org.up.cd.queues;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.up.cd.protocol.BinaryProtocol;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Singleton. Maintains one outbound queue per service code (foliado protocol).
 * Each queue holds messages waiting to be sent by SenderThread.
 */
public class OutputQueueManager {

    private static final Logger logger = LogManager.getLogger(OutputQueueManager.class);

    private final Map<Integer, LinkedBlockingQueue<BinaryProtocol>> queues = new ConcurrentHashMap<>();

    private OutputQueueManager() {}

    public static OutputQueueManager getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final OutputQueueManager INSTANCE = new OutputQueueManager();
    }

    public void enqueue(int serviceId, BinaryProtocol msg) {
        queues.computeIfAbsent(serviceId, k -> new LinkedBlockingQueue<>()).add(msg);
        logger.debug("[OutQueue] Enqueued serviceId={} queueSize={}", serviceId,
                queues.get(serviceId).size());
    }

    /** Returns next message from any non-empty queue, or null if all empty. */
    public BinaryProtocol pollNext() {
        for (Map.Entry<Integer, LinkedBlockingQueue<BinaryProtocol>> e : queues.entrySet()) {
            BinaryProtocol msg = e.getValue().poll();
            if (msg != null) {
                logger.debug("[OutQueue] Dequeued serviceId={}", e.getKey());
                return msg;
            }
        }
        return null;
    }

    public Set<Integer> getServiceIds() {
        return queues.keySet();
    }

    public int queueSize(int serviceId) {
        LinkedBlockingQueue<BinaryProtocol> q = queues.get(serviceId);
        return q == null ? 0 : q.size();
    }
}
