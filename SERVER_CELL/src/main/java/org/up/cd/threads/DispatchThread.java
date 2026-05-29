package org.up.cd.threads;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.up.cd.config.Config;
import org.up.cd.network.MeshConnection;
import org.up.cd.protocol.BinaryProtocol;
import org.up.cd.queues.InputQueueManager;
import org.up.cd.service.DynamicServiceLoader;

import java.nio.charset.StandardCharsets;

/**
 * Dispatch thread for SERVER_CELL.
 * Drains the input queue, executes the requested service via DynamicServiceLoader,
 * then sends the response back (header-flip pattern, serviceNumber negated).
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
                BinaryProtocol request = InputQueueManager.getInstance().poll(POLL_TIMEOUT_MS);
                if (request == null) continue;

                int svc = request.getServiceNumber();
                logger.info("[DispatchThread] Processing eventId={} serviceId={} from={}",
                        request.getEventId(), svc, request.getOriginEntity());

                byte[] result = DynamicServiceLoader.getInstance().execute(svc, request.getData());
                if (result == null) {
                    // This cell doesn't handle this serviceId — silently discard.
                    // Another server cell with the right JAR will respond.
                    logger.debug("[DispatchThread] serviceId={} not handled by this cell — discarding", svc);
                    continue;
                }

                sendResponse(request, result);

            } catch (InterruptedException e) {
                break;
            }
        }
        logger.info("[DispatchThread] Stopped");
    }

    private void sendResponse(BinaryProtocol request, byte[] result) {
        Config cfg = Config.getInstance();
        String cellId = cfg.getCellId();
        try {
            // Header flip: we become the origin, original sender becomes dest
            BinaryProtocol response = new BinaryProtocol(
                    cellId, "SERVER_CELL", cellId,
                    request.getOriginBusiness(),
                    request.getOriginSubsystem(),
                    request.getOriginEntity(),
                    request.getEventId(),
                    -request.getServiceNumber(),   // negate service number = response
                    result
            );
            MeshConnection.getInstance().send(response.serialize());
            logger.info("[DispatchThread] Response sent eventId={} serviceId={} to={}",
                    request.getEventId(), -request.getServiceNumber(), request.getOriginEntity());
        } catch (Exception e) {
            logger.error("[DispatchThread] Response send error: {}", e.getMessage());
        }
    }

    public void stop() { running = false; }
}
