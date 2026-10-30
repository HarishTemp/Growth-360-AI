package com.growth360.server;

import com.growth360.service.AnalyticsService;
import com.growth360.util.DataSeeder;
import com.growth360.util.Schema;

/**
 * Standalone entry point to start only the embedded Localhost HTTP Server
 * without launching the JavaFX desktop UI.
 */
public class ServerMain {

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("   GROWTH 360 AI — LOCALHOST WEB SERVER & COPILOT");
        System.out.println("   Titan Watches • Decision Intelligence");
        System.out.println("==================================================");

        // 1. Initialize schema and seed data
        Schema.initSchema();
        DataSeeder.seedIfEmpty();

        // 2. Start embedded HTTP server
        AnalyticsService analyticsService = new AnalyticsService();
        LocalhostCopilotServer server = new LocalhostCopilotServer(analyticsService);
        boolean started = server.start();

        if (started) {
            System.out.println("\n>>> SUCCESS! Open this URL in your browser: <<<");
            System.out.println("👉 http://localhost:" + server.getPort() + "\n");
            System.out.println("Press Ctrl+C in this terminal window to stop the server.");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down Localhost server...");
                server.stop();
                analyticsService.shutdown();
            }));

            // Keep main thread alive
            Thread.currentThread().join();
        } else {
            System.err.println("Failed to start localhost server.");
            System.exit(1);
        }
    }
}
