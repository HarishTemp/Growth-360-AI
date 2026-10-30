package com.growth360.copilot;

import com.growth360.model.MetricContext;
import com.growth360.server.LocalhostCopilotServer;
import com.growth360.service.AnalyticsService;
import com.growth360.util.DataSeeder;
import com.growth360.util.Schema;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

public class AiCopilotEngineTest {

    private static AnalyticsService analyticsService;
    private static MetricContext metricContext;
    private static AiCopilotEngine copilotEngine;

    @BeforeAll
    static void setup() throws Exception {
        Schema.initSchema();
        DataSeeder.seedIfEmpty();
        analyticsService = new AnalyticsService();
        metricContext = analyticsService.computeMetricContext();
        copilotEngine = new AiCopilotEngine();
    }

    @Test
    @DisplayName("Copilot: Financial health and margin diagnostic query")
    void testFinancialDiagnostic() {
        CopilotResponse resp = copilotEngine.ask("Analyze profit margins and cash flow", metricContext);
        assertNotNull(resp);
        assertNotNull(resp.headline());
        assertTrue(resp.headline().contains("Margin"));
        assertTrue(resp.narrative().contains("Boutique Revenue"));
        assertFalse(resp.keyMetrics().isEmpty());
        assertFalse(resp.suggestedActions().isEmpty());
    }

    @Test
    @DisplayName("Copilot: Statistical marketing expense anomaly query (Z-Score > 3.0)")
    void testAnomalyReasoning() {
        CopilotResponse resp = copilotEngine.ask("Explain marketing expense anomaly", metricContext);
        assertNotNull(resp);
        assertEquals(CopilotResponse.Urgency.CRITICAL, resp.urgency());
        assertTrue(resp.narrative().contains("Z-Score") || resp.narrative().contains("Gaussian"));
        assertTrue(resp.narrative().contains("3.0"));
    }

    @Test
    @DisplayName("Copilot: Supply chain and Reorder Point (ROP) query")
    void testInventorySupplyChain() {
        CopilotResponse resp = copilotEngine.ask("Check watch inventory and stockout risks", metricContext);
        assertNotNull(resp);
        assertTrue(resp.headline().contains("Inventory Alert"));
        assertTrue(resp.narrative().contains("Hosur"));
        assertFalse(resp.suggestedActions().isEmpty());
    }

    @Test
    @DisplayName("Copilot: Customer RFM quintiles and Encircle loyalty query")
    void testCustomerRetention() {
        CopilotResponse resp = copilotEngine.ask("How to win back at-risk customers?", metricContext);
        assertNotNull(resp);
        assertTrue(resp.headline().contains("Encircle Loyalty"));
        assertTrue(resp.narrative().contains("Active Boutique Clientele"));
    }

    @Test
    @DisplayName("Copilot: 5-Factor Growth Score breakdown query")
    void testGrowthScoreExplanation() {
        CopilotResponse resp = copilotEngine.ask("How do we improve our Growth Score?", metricContext);
        assertNotNull(resp);
        assertTrue(resp.headline().contains("Growth Score"));
        assertTrue(resp.narrative().contains("Sales Velocity"));
        assertTrue(resp.narrative().contains("Margin Health"));
        assertTrue(resp.narrative().contains("Inventory Balance"));
    }

    @Test
    @DisplayName("Copilot: Revenue forecast trajectory query")
    void testRevenueForecast() {
        CopilotResponse resp = copilotEngine.ask("What is next month revenue forecast?", metricContext);
        assertNotNull(resp);
        assertTrue(resp.headline().contains("Forecast"));
        assertTrue(resp.narrative().contains("OLS") || resp.narrative().contains("Ordinary Least Squares"));
    }

    @Test
    @DisplayName("Copilot: 30-Day strategic action roadmap query")
    void testActionPlan() {
        CopilotResponse resp = copilotEngine.ask("Give me a 30-day executive action plan", metricContext);
        assertNotNull(resp);
        assertTrue(resp.narrative().contains("Executive Strategic Action Plan"));
        assertFalse(resp.suggestedActions().isEmpty());
    }

    @Test
    @DisplayName("LocalhostCopilotServer: Embedded HTTP Server lifecycle & REST endpoints")
    void testLocalhostServerLifecycleAndEndpoints() throws Exception {
        LocalhostCopilotServer server = new LocalhostCopilotServer(analyticsService);
        boolean started = server.start();
        assertTrue(started, "Embedded HTTP server must start successfully");

        int port = server.getPort();
        HttpClient client = HttpClient.newHttpClient();

        // 1. Test /api/health
        HttpRequest healthReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/health"))
                .GET()
                .build();
        HttpResponse<String> healthResp = client.send(healthReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, healthResp.statusCode());
        assertTrue(healthResp.body().contains("UP"));

        // 2. Test /api/metrics
        HttpRequest metricsReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/metrics"))
                .GET()
                .build();
        HttpResponse<String> metricsResp = client.send(metricsReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, metricsResp.statusCode());
        assertTrue(metricsResp.body().contains("totalRevenue"));
        assertTrue(metricsResp.body().contains("growthScore"));

        // 3. Test /api/copilot
        HttpRequest copilotReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/copilot"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"overview\"}"))
                .build();
        HttpResponse<String> copilotResp = client.send(copilotReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, copilotResp.statusCode());
        assertTrue(copilotResp.body().contains("headline"));
        assertTrue(copilotResp.body().contains("narrative"));

        server.stop();
    }
}
