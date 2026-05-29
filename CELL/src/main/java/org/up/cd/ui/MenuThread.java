package org.up.cd.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.up.cd.config.Config;
import org.up.cd.protocol.BinaryProtocol;
import org.up.cd.queues.OutputQueueManager;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Interactive menu thread. Lets the user choose an operation (suma/resta/mult/div),
 * enter operands, and enqueue the request in the corresponding output queue.
 *
 * Service codes:
 *   1 = suma        -1 = suma response
 *   2 = resta       -2 = resta response
 *   3 = multiplicacion
 *   4 = division
 *   0 = ACK (internal)
 */
public class MenuThread implements Runnable {

    private static final Logger logger = LogManager.getLogger(MenuThread.class);

    private volatile boolean running = true;

    private static final String[] SERVICE_NAMES = {"", "Suma", "Resta", "Multiplicacion", "Division"};

    @Override
    public void run() {
        logger.info("[MenuThread] Started — interactive menu active");
        Scanner sc = new Scanner(System.in);
        Config cfg = Config.getInstance();

        printMenu();

        while (running) {
            System.out.print("> ");
            if (!sc.hasNextLine()) break;
            String line = sc.nextLine().trim();

            if (line.equals("0")) {
                logger.info("[MenuThread] Exit requested");
                System.exit(0);
                break;
            }

            int serviceId;
            try {
                serviceId = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("  Invalid option.");
                printMenu();
                continue;
            }

            if (serviceId < 1 || serviceId > 4) {
                System.out.println("  Choose 1-4 or 0 to exit.");
                printMenu();
                continue;
            }

            System.out.printf("  [%s] Enter operand A: ", SERVICE_NAMES[serviceId]);
            double a;
            double b;
            try {
                a = Double.parseDouble(sc.nextLine().trim());
                System.out.printf("  [%s] Enter operand B: ", SERVICE_NAMES[serviceId]);
                b = Double.parseDouble(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("  Invalid number.");
                continue;
            }

            String payload = buildPayload(serviceId, a, b);
            String cellId  = cfg.getCellId();

            BinaryProtocol msg = new BinaryProtocol(
                    cellId, "CELL", cellId,
                    "", "SERVER_CELL", "",
                    0L, serviceId,
                    payload.getBytes(StandardCharsets.UTF_8)
            );

            OutputQueueManager.getInstance().enqueue(serviceId, msg);
            logger.info("[MenuThread] Queued serviceId={} payload={}", serviceId, payload);
            System.out.printf("  Queued: %s(%s) — waiting for ACKs and response...%n", SERVICE_NAMES[serviceId], payload);
        }

        logger.info("[MenuThread] Stopped");
    }

    private String buildPayload(int serviceId, double a, double b) {
        String op = switch (serviceId) {
            case 1 -> "+";
            case 2 -> "-";
            case 3 -> "*";
            case 4 -> "/";
            default -> "?";
        };
        return a + " " + op + " " + b;
    }

    private void printMenu() {
        System.out.println();
        System.out.println("=== Cell Menu ===");
        System.out.println("  1. Suma");
        System.out.println("  2. Resta");
        System.out.println("  3. Multiplicacion");
        System.out.println("  4. Division");
        System.out.println("  0. Exit");
        System.out.println("=================");
    }

    public void stop() { running = false; }
}
