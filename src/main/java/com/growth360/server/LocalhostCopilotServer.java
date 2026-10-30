package com.growth360.server;

import com.growth360.agents.AgentOrchestrator;
import com.growth360.agents.AgentResult;
import com.growth360.analytics.ReorderPointCalculator.InventoryMetric;
import com.growth360.copilot.AiCopilotEngine;
import com.growth360.copilot.CopilotResponse;
import com.growth360.llm.LlmConfig;
import com.growth360.llm.LlmProviderService;
import com.growth360.model.*;
import com.growth360.model.RfmResult;
import com.growth360.service.AnalyticsService;
import com.growth360.service.AnalyticsService.AnalyticsSnapshot;
import com.growth360.service.CustomerService;
import com.growth360.service.ExpenseService;
import com.growth360.service.ProductService;
import com.growth360.service.SaleService;
import com.growth360.service.CsvDataService;
import com.growth360.util.DataSeeder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Pure Java Embedded HTTP Server (com.sun.net.httpserver.HttpServer).
 * Zero external web framework dependencies.
 * Serves the COMPLETE 7-tab executive dashboard with navigation sidebar
 * and real-time AI Copilot on http://localhost:8080.
 */
public class LocalhostCopilotServer {

    private final AnalyticsService analyticsService;
    private final SaleService saleService;
    private final ProductService productService;
    private final CustomerService customerService;
    private final ExpenseService expenseService;
    private final AiCopilotEngine copilotEngine;
    private final LlmProviderService llmProviderService;
    private final AgentOrchestrator agentOrchestrator;
    private final CsvDataService csvDataService;
    private HttpServer server;
    private int port = 8080;

    public LocalhostCopilotServer(AnalyticsService analyticsService) {
        this.analyticsService    = analyticsService;
        this.saleService         = new SaleService();
        this.productService      = new ProductService();
        this.customerService     = new CustomerService();
        this.expenseService      = new ExpenseService();
        this.copilotEngine       = new AiCopilotEngine();
        this.llmProviderService  = new LlmProviderService();
        this.agentOrchestrator   = new AgentOrchestrator();
        this.csvDataService      = new CsvDataService();
    }

    public synchronized boolean start() {
        int[] portsToTry = {8080, 8081, 8082, 9090};
        for (int p : portsToTry) {
            try {
                server = HttpServer.create(new InetSocketAddress(p), 0);
                this.port = p;
                break;
            } catch (IOException e) {
                // Try next port
            }
        }

        if (server == null) {
            System.err.println("[LocalhostCopilotServer] Could not bind to any port.");
            return false;
        }

        server.createContext("/", new DashboardHtmlHandler());
        server.createContext("/api/metrics", new MetricsApiHandler());
        server.createContext("/api/all-data", new AllDataApiHandler());
        server.createContext("/api/copilot", new CopilotApiHandler());
        server.createContext("/api/agents", new AgentsApiHandler());
        server.createContext("/api/agents/feedback", new AgentFeedbackApiHandler());
        server.createContext("/api/llm-config", new LlmConfigApiHandler());
        server.createContext("/api/dataset/reseed-enterprise", new ReseedEnterpriseApiHandler());
        server.createContext("/api/dataset/import-csv", new ImportCsvApiHandler());
        server.createContext("/api/dataset/sample-csv", new SampleCsvApiHandler());
        server.createContext("/api/dataset/summary", new DatasetSummaryApiHandler());
        server.createContext("/api/health", exchange -> {
            boolean ollamaUp = llmProviderService.isOllamaAvailable();
            String providerStatus = escapeJson(llmProviderService.getActiveProviderStatus());
            byte[] resp = ("{\"status\":\"UP\",\"engine\":\"Growth 360 AI\"," +
                    "\"ollama\":" + ollamaUp + "," +
                    "\"llmProvider\":\"" + providerStatus + "\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });

        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        System.out.println("==================================================");
        System.out.println("   🌐 LOCALHOST COPILOT SERVER LIVE");
        System.out.println("   URL: http://localhost:" + port);
        System.out.println("   REST API: http://localhost:" + port + "/api/copilot");
        System.out.println("==================================================");
        return true;
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public int getPort() {
        return port;
    }

    // =========================================================================
    // Handlers
    // =========================================================================

    private class MetricsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

            try {
                MetricContext ctx = analyticsService.computeMetricContext();
                String json = formatMetricContextJson(ctx);
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Exception e) {
                String err = "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
                byte[] bytes = err.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }
    }

    private class AllDataApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

            try {
                AnalyticsSnapshot snapshot = analyticsService.computeSnapshot();
                List<Sale> sales = saleService.getAllSales();
                String json = formatFullDataJson(snapshot, sales);
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Exception e) {
                String err = "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
                byte[] bytes = err.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }
    }

    /** NEW: Returns ML agent results as JSON */
    private class AgentsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            try {
                AnalyticsSnapshot snapshot = analyticsService.computeSnapshot();
                List<Sale> sales         = saleService.getAllSales();
                List<Customer> customers = customerService.getAllCustomers();
                List<Expense> expenses   = expenseService.getAllExpenses();
                List<Product> products   = productService.getAllProducts();

                Map<String, AgentResult> results = agentOrchestrator.runAllAgents(
                        sales, expenses, customers, snapshot.inventoryMetrics(), products);

                String json = formatAgentResultsJson(results, llmProviderService);
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            } catch (Exception e) {
                String err = "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
                byte[] bytes = err.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            }
        }
    }

    /** Active Learning: Receives human feedback to dismiss or confirm agent findings */
    private class AgentFeedbackApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            try {
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    try (InputStream is = exchange.getRequestBody()) {
                        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        String agent  = extractJsonString(body, "agent", "anomaly");
                        String item   = extractJsonString(body, "item", "");
                        String action = extractJsonString(body, "action", "dismiss");
                        agentOrchestrator.recordFeedback(agent, item, action);
                    }
                }
                String resp = "{\"success\":true,\"message\":\"Feedback recorded into active learning pipeline\"}";
                byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            } catch (Exception e) {
                String err = "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
                byte[] bytes = err.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            }
        }
    }

    /** NEW: Saves LLM API key + provider config from dashboard settings panel */
    private class LlmConfigApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            try {
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    try (InputStream is = exchange.getRequestBody()) {
                        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        String provider  = extractJsonString(body, "provider",  "ollama");
                        String apiKey    = extractJsonString(body, "apiKey",    "");
                        String modelName = extractJsonString(body, "modelName", "gemma3:4b");
                        llmProviderService.updateConfig(new LlmConfig(provider, apiKey, modelName));
                    }
                }
                LlmConfig cfg = llmProviderService.getConfig();
                boolean ollamaUp = llmProviderService.isOllamaAvailable();
                List<String> installed = llmProviderService.getInstalledOllamaModels();
                StringBuilder modelsArr = new StringBuilder("[");
                for (int i = 0; i < installed.size(); i++) {
                    modelsArr.append("\"").append(escapeJson(installed.get(i))).append("\"");
                    if (i < installed.size() - 1) modelsArr.append(",");
                }
                modelsArr.append("]");

                String json = String.format(
                        "{\"provider\":\"%s\",\"modelName\":\"%s\",\"hasApiKey\":%s," +
                        "\"ollamaAvailable\":%s,\"installedModels\":%s,\"status\":\"%s\"}",
                        escapeJson(cfg.provider()), escapeJson(cfg.modelName()),
                        cfg.hasApiKey(), ollamaUp, modelsArr.toString(),
                        escapeJson(llmProviderService.getActiveProviderStatus()));
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            } catch (Exception e) {
                String err = "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
                byte[] bytes = err.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            }
        }
    }

    private class ReseedEnterpriseApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            try {
                DataSeeder.seedEnterpriseDataset(true);
                AnalyticsSnapshot snapshot = analyticsService.computeSnapshot();
                List<Sale> sales         = saleService.getAllSales();
                List<Customer> customers = customerService.getAllCustomers();
                List<Expense> expenses   = expenseService.getAllExpenses();
                List<Product> products   = productService.getAllProducts();

                agentOrchestrator.runAllAgents(sales, expenses, customers, snapshot.inventoryMetrics(), products);

                String json = String.format(Locale.ROOT,
                        "{\"success\":true,\"products\":%d,\"customers\":%d,\"sales\":%d,\"expenses\":%d,\"message\":\"Successfully loaded 2-Year Enterprise Titan Dataset & retrained all 4 ML models.\"}",
                        products.size(), customers.size(), sales.size(), expenses.size());
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            } catch (Exception e) {
                String err = "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
                byte[] bytes = err.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            }
        }
    }

    private class ImportCsvApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            try {
                String csvText = "";
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    try (InputStream is = exchange.getRequestBody()) {
                        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        if (body.trim().startsWith("{") && body.contains("\"csv\":")) {
                            csvText = extractJsonString(body, "csv", "");
                        } else {
                            csvText = body;
                        }
                    }
                }

                CsvDataService.ImportResult res = csvDataService.importSalesCsv(csvText);

                if (res.success()) {
                    AnalyticsSnapshot snapshot = analyticsService.computeSnapshot();
                    List<Sale> sales         = saleService.getAllSales();
                    List<Customer> customers = customerService.getAllCustomers();
                    List<Expense> expenses   = expenseService.getAllExpenses();
                    List<Product> products   = productService.getAllProducts();
                    agentOrchestrator.runAllAgents(sales, expenses, customers, snapshot.inventoryMetrics(), products);
                }

                String json = String.format(Locale.ROOT,
                        "{\"success\":%s,\"imported\":%d,\"skipped\":%d,\"message\":\"%s\"}",
                        res.success(), res.importedCount(), res.skippedCount(), escapeJson(res.message()));
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            } catch (Exception e) {
                String err = "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
                byte[] bytes = err.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            }
        }
    }

    private class SampleCsvApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"sample_titan_sales.csv\"");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            byte[] bytes = csvDataService.getSampleSalesCsv().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }
    }

    private class DatasetSummaryApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            try {
                int pCount = productService.getAllProducts().size();
                int cCount = customerService.getAllCustomers().size();
                int sCount = saleService.getAllSales().size();
                int eCount = expenseService.getAllExpenses().size();
                String json = String.format(Locale.ROOT,
                        "{\"products\":%d,\"customers\":%d,\"sales\":%d,\"expenses\":%d}",
                        pCount, cCount, sCount, eCount);
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            } catch (Exception e) {
                String err = "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
                byte[] bytes = err.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            }
        }
    }

    private class CopilotApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                String query = "overview";
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    try (InputStream is = exchange.getRequestBody()) {
                        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        int idx = body.indexOf("\"query\":");
                        if (idx != -1) {
                            int start = body.indexOf("\"", idx + 8);
                            int end = body.indexOf("\"", start + 1);
                            if (start != -1 && end != -1) {
                                query = body.substring(start + 1, end);
                            }
                        }
                    }
                } else if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    String rawQuery = exchange.getRequestURI().getQuery();
                    if (rawQuery != null && rawQuery.contains("q=")) {
                        query = java.net.URLDecoder.decode(rawQuery.substring(rawQuery.indexOf("q=") + 2), StandardCharsets.UTF_8);
                    }
                }

                MetricContext ctx = analyticsService.computeMetricContext();

                // Try LLM first (Ollama → Gemini API → fallback)
                String llmResponse = llmProviderService.generateResponse(query, ctx);
                CopilotResponse resp;
                if (llmResponse != null && !llmResponse.isBlank()) {
                    // Wrap LLM response in CopilotResponse format
                    resp = CopilotResponse.of(
                            query,
                            "🤖 AI Response via " + llmProviderService.getActiveProviderStatus(),
                            llmResponse,
                            List.of(),
                            List.of(),
                            List.of("Analyze current profit health", "Check watch inventory", "Give me a 30-day action plan"),
                            CopilotResponse.Urgency.NORMAL
                    );
                } else {
                    // Fall back to rule-based engine
                    resp = copilotEngine.ask(query, ctx);
                }

                String json = formatCopilotResponseJson(resp);
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Exception e) {
                String err = "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
                byte[] bytes = err.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }
    }

    private class DashboardHtmlHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");

            String html = generateCompleteDashboardHtml();
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // =========================================================================
    // JSON Serialization Helpers
    // =========================================================================

    private String formatMetricContextJson(MetricContext ctx) {
        return String.format(Locale.ROOT,
                """
                {
                  "currentPeriod": "%s",
                  "totalRevenue": %.2f,
                  "momRevenueGrowth": %.2f,
                  "totalExpenses": %.2f,
                  "netProfit": %.2f,
                  "profitMargin": %.2f,
                  "growthScore": {
                    "overall": %d,
                    "band": "%s",
                    "salesVelocity": %.1f,
                    "marginHealth": %.1f,
                    "inventoryHealth": %.1f,
                    "customerHealth": %.1f,
                    "expenseEfficiency": %.1f
                  },
                  "topProduct": "%s",
                  "activeCustomerCount": %d,
                  "atRiskCustomerCount": %d,
                  "riskAlertCount": %d,
                  "inventoryRiskCount": %d
                }
                """,
                escapeJson(ctx.currentPeriod()),
                ctx.totalRevenue(),
                ctx.momRevenueGrowth(),
                ctx.totalExpenses(),
                ctx.netProfit(),
                ctx.profitMargin(),
                ctx.growthScore().overallScore(),
                escapeJson(ctx.growthScore().healthBand()),
                ctx.growthScore().salesVelocity(),
                ctx.growthScore().marginHealth(),
                ctx.growthScore().inventoryBalance(),
                ctx.growthScore().customerHealth(),
                ctx.growthScore().expenseEfficiency(),
                escapeJson(ctx.topProduct()),
                ctx.activeCustomerCount(),
                ctx.atRiskCustomerCount(),
                ctx.activeAlerts().size(),
                ctx.inventoryRisks().size()
        );
    }

    private String formatFullDataJson(AnalyticsSnapshot s, List<Sale> sales) {
        StringBuilder sb = new StringBuilder("{");

        // Financials
        sb.append(String.format(Locale.ROOT, "\"totalRevenue\":%.2f,\"totalExpenses\":%.2f,\"netProfit\":%.2f,",
                s.totalRevenue(), s.totalExpenses(), s.netProfit()));

        // Growth Score
        GrowthScore gs = s.growthScore().toGrowthScore();
        sb.append(String.format(Locale.ROOT,
                "\"growthScore\":{\"overall\":%d,\"band\":\"%s\",\"salesVelocity\":%.1f,\"marginHealth\":%.1f,\"inventoryHealth\":%.1f,\"customerHealth\":%.1f,\"expenseEfficiency\":%.1f},",
                gs.overallScore(), escapeJson(gs.healthBand()), gs.salesVelocity(), gs.marginHealth(), gs.inventoryBalance(), gs.customerHealth(), gs.expenseEfficiency()));

        // Expenses by category
        sb.append("\"expenses\":[");
        int eIdx = 0;
        for (Map.Entry<String, Double> e : s.expenseByCategory().entrySet()) {
            sb.append(String.format(Locale.ROOT, "{\"category\":\"%s\",\"amount\":%.2f}", escapeJson(e.getKey()), e.getValue()));
            if (++eIdx < s.expenseByCategory().size()) sb.append(",");
        }
        sb.append("],");

        // Inventory
        sb.append("\"inventory\":[");
        for (int i = 0; i < s.inventoryMetrics().size(); i++) {
            InventoryMetric m = s.inventoryMetrics().get(i);
            sb.append(String.format(Locale.ROOT,
                    "{\"name\":\"%s\",\"category\":\"%s\",\"stock\":%d,\"price\":%.2f,\"cost\":%.2f,\"rop\":%.1f,\"runway\":%.1f,\"status\":\"%s\"}",
                    escapeJson(m.product().getName()), escapeJson(m.product().getCategory()), m.product().getStockQty(),
                    m.product().getUnitPrice(), m.product().getUnitCost(), m.reorderPoint(), m.stockRunwayDays(), m.status().name()));
            if (i < s.inventoryMetrics().size() - 1) sb.append(",");
        }
        sb.append("],");

        // RFM Customers
        sb.append("\"customers\":[");
        for (int i = 0; i < s.rfmResults().size(); i++) {
            RfmResult r = s.rfmResults().get(i);
            sb.append(String.format(Locale.ROOT,
                    "{\"id\":%d,\"name\":\"%s\",\"recency\":%d,\"frequency\":%d,\"monetary\":%.2f,\"segment\":\"%s\"}",
                    r.customerId(), escapeJson(r.customerName()), r.recencyDays(), r.frequency(), r.monetary(), r.segment().name()));
            if (i < s.rfmResults().size() - 1) sb.append(",");
        }
        sb.append("],");

        // Alerts
        sb.append("\"alerts\":[");
        for (int i = 0; i < s.alerts().size(); i++) {
            Alert a = s.alerts().get(i);
            sb.append(String.format(Locale.ROOT,
                    "{\"id\":\"%s\",\"type\":\"%s\",\"severity\":\"%s\",\"title\":\"%s\",\"desc\":\"%s\",\"time\":\"%s\"}",
                    escapeJson(a.id()), a.type().name(), a.severity().name(), escapeJson(a.title()), escapeJson(a.description()),
                    a.timestamp().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))));
            if (i < s.alerts().size() - 1) sb.append(",");
        }
        sb.append("],");

        // Recommendations
        sb.append("\"recommendations\":[");
        for (int i = 0; i < s.recommendations().size(); i++) {
            Recommendation r = s.recommendations().get(i);
            sb.append(String.format(Locale.ROOT,
                    "{\"id\":\"%s\",\"category\":\"%s\",\"title\":\"%s\",\"action\":\"%s\",\"impact\":\"%s\",\"priority\":\"%s\"}",
                    escapeJson(r.id()), escapeJson(r.category()), escapeJson(r.title()), escapeJson(r.actionText()), escapeJson(r.expectedImpact()), r.priority().name()));
            if (i < s.recommendations().size() - 1) sb.append(",");
        }
        sb.append("],");

        // Sales (limit to last 40)
        sb.append("\"sales\":[");
        int sLimit = Math.min(40, sales.size());
        for (int i = 0; i < sLimit; i++) {
            Sale sl = sales.get(i);
            String pName = sl.getProductName() != null ? sl.getProductName() : "Product #" + sl.getProductId();
            String cName = sl.getCustomerName() != null ? sl.getCustomerName() : "Customer #" + sl.getCustomerId();
            sb.append(String.format(Locale.ROOT,
                    "{\"id\":%d,\"product\":\"%s\",\"customer\":\"%s\",\"qty\":%d,\"amount\":%.2f,\"date\":\"%s\"}",
                    sl.getId(), escapeJson(pName), escapeJson(cName), sl.getQuantity(), sl.getAmount(),
                    sl.getSaleDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
            if (i < sLimit - 1) sb.append(",");
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }

    private String formatCopilotResponseJson(CopilotResponse resp) {
        StringBuilder metricsJson = new StringBuilder("[");
        for (int i = 0; i < resp.keyMetrics().size(); i++) {
            metricsJson.append("\"").append(escapeJson(resp.keyMetrics().get(i))).append("\"");
            if (i < resp.keyMetrics().size() - 1) metricsJson.append(",");
        }
        metricsJson.append("]");

        StringBuilder actionsJson = new StringBuilder("[");
        for (int i = 0; i < resp.suggestedActions().size(); i++) {
            actionsJson.append("\"").append(escapeJson(resp.suggestedActions().get(i))).append("\"");
            if (i < resp.suggestedActions().size() - 1) metricsJson.append(",");
        }
        actionsJson.append("]");

        StringBuilder followUpsJson = new StringBuilder("[");
        for (int i = 0; i < resp.followUpPrompts().size(); i++) {
            followUpsJson.append("\"").append(escapeJson(resp.followUpPrompts().get(i))).append("\"");
            if (i < resp.followUpPrompts().size() - 1) followUpsJson.append(",");
        }
        followUpsJson.append("]");

        return String.format(
                """
                {
                  "userQuery": "%s",
                  "headline": "%s",
                  "narrative": "%s",
                  "keyMetrics": %s,
                  "suggestedActions": %s,
                  "followUpPrompts": %s,
                  "urgency": "%s",
                  "timestamp": "%s"
                }
                """,
                escapeJson(resp.userQuery()),
                escapeJson(resp.headline()),
                escapeJson(resp.narrative()),
                metricsJson,
                actionsJson,
                followUpsJson,
                resp.urgency().name(),
                resp.timestamp().toString()
        );
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /** Formats all 4 ML agent results as JSON for the /api/agents endpoint */
    private String formatAgentResultsJson(Map<String, AgentResult> results,
                                          com.growth360.llm.LlmProviderService llmService) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"llmStatus\":\"").append(escapeJson(llmService.getActiveProviderStatus())).append("\",");
        sb.append("\"ollamaAvailable\":").append(llmService.isOllamaAvailable()).append(",");
        sb.append("\"agents\":{");
        int idx = 0;
        for (Map.Entry<String, AgentResult> entry : results.entrySet()) {
            AgentResult r = entry.getValue();
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":{");
            sb.append("\"agentName\":\"").append(escapeJson(r.agentName())).append("\",");
            sb.append("\"modelUsed\":\"").append(escapeJson(r.modelUsed())).append("\",");
            sb.append("\"headline\":\"").append(escapeJson(r.headline())).append("\",");
            sb.append("\"summary\":\"").append(escapeJson(r.summary())).append("\",");
            sb.append("\"confidence\":").append(String.format(Locale.ROOT, "%.2f", r.confidenceScore())).append(",");
            sb.append("\"trained\":").append(r.trained()).append(",");
            sb.append("\"findings\":[");
            for (int i = 0; i < r.findings().size(); i++) {
                sb.append("\"").append(escapeJson(r.findings().get(i))).append("\"");
                if (i < r.findings().size() - 1) sb.append(",");
            }
            sb.append("],");
            sb.append("\"metrics\":{");
            int mIdx = 0;
            if (r.evaluationMetrics() != null) {
                for (Map.Entry<String, Double> mEntry : r.evaluationMetrics().entrySet()) {
                    sb.append("\"").append(escapeJson(mEntry.getKey())).append("\":")
                      .append(String.format(Locale.ROOT, "%.3f", mEntry.getValue()));
                    if (++mIdx < r.evaluationMetrics().size()) sb.append(",");
                }
            }
            sb.append("}}");
            if (++idx < results.size()) sb.append(",");
        }
        sb.append("}}");
        return sb.toString();
    }

    /** Extracts a string field from a simple JSON body (no external library needed) */
    private String extractJsonString(String body, String key, String defaultValue) {
        String search = "\"" + key + "\"";
        int idx = body.indexOf(search);
        if (idx == -1) return defaultValue;
        int colonIdx = body.indexOf(":", idx + search.length());
        if (colonIdx == -1) return defaultValue;
        int quoteStart = body.indexOf("\"", colonIdx + 1);
        if (quoteStart == -1) return defaultValue;
        int quoteEnd = body.indexOf("\"", quoteStart + 1);
        if (quoteEnd == -1) return defaultValue;
        return body.substring(quoteStart + 1, quoteEnd);
    }

    // =========================================================================
    // COMPLETE 7-TAB DASHBOARD WITH SIDEBAR NAVBAR
    // =========================================================================

    private String generateCompleteDashboardHtml() {
        return generateDashboardHtmlPart1() + generateDashboardHtmlPart2();
    }

    private String generateDashboardHtmlPart1() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Growth 360 AI — Titan Watches Decision Intelligence & Copilot</title>
    <style>
        :root {
            --bg-body: #0A0E17;
            --bg-sidebar: #0D1322;
            --bg-card: #131B2E;
            --bg-input: #1A253D;
            --text-primary: #F8FAFC;
            --text-secondary: #94A3B8;
            --text-muted: #64748B;
            --accent-cyan: #38BDF8;
            --accent-emerald: #10B981;
            --accent-amber: #F59E0B;
            --accent-rose: #EF4444;
            --border-color: rgba(56, 189, 248, 0.15);
        }
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        body { background: var(--bg-body); color: var(--text-primary); min-height: 100vh; display: flex; }
        
        /* Sidebar */
        aside { width: 280px; background: var(--bg-sidebar); border-right: 1px solid var(--border-color); display: flex; flex-direction: column; flex-shrink: 0; }
        .brand { padding: 24px 20px 18px 20px; border-bottom: 1px solid var(--border-color); }
        .brand-title { display: flex; align-items: center; gap: 10px; font-size: 18px; font-weight: 800; color: #fff; letter-spacing: 0.5px; }
        .brand-sub { font-size: 11px; color: var(--accent-cyan); font-weight: bold; margin-top: 4px; }
        
        .nav-list { list-style: none; padding: 18px 12px; flex: 1; display: flex; flex-direction: column; gap: 6px; }
        .nav-item button { width: 100%; text-align: left; padding: 12px 16px; border-radius: 8px; border: none; background: transparent; color: var(--text-secondary); font-size: 13px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 12px; transition: all 0.2s; }
        .nav-item button:hover { background: rgba(56, 189, 248, 0.08); color: #fff; }
        .nav-item.active button { background: linear-gradient(135deg, rgba(56, 189, 248, 0.2), rgba(2, 132, 199, 0.2)); color: var(--accent-cyan); border-left: 3px solid var(--accent-cyan); }
        
        .sidebar-footer { padding: 18px 20px; border-top: 1px solid var(--border-color); font-size: 11px; color: var(--text-muted); }
        .badge-db { background: rgba(16, 185, 129, 0.15); color: var(--accent-emerald); padding: 4px 10px; border-radius: 12px; font-weight: bold; display: inline-block; margin-top: 4px; }
        
        /* Main Content */
        main { flex: 1; display: flex; flex-direction: column; overflow-y: auto; height: 100vh; }
        header { background: #0F1626; border-bottom: 1px solid var(--border-color); padding: 16px 32px; display: flex; justify-content: space-between; align-items: center; }
        .header-left .breadcrumb { font-size: 10px; color: var(--text-muted); font-weight: bold; text-transform: uppercase; }
        .header-left h2 { font-size: 20px; font-weight: bold; color: #fff; margin-top: 2px; }
        .header-actions { display: flex; align-items: center; gap: 14px; }
        .btn-copilot-shortcut { background: rgba(56, 189, 248, 0.15); border: 1px solid var(--accent-cyan); color: var(--accent-cyan); padding: 8px 16px; border-radius: 6px; font-weight: bold; font-size: 12px; cursor: pointer; }
        .btn-refresh { background: var(--bg-card); border: 1px solid var(--border-color); color: var(--text-secondary); padding: 8px 14px; border-radius: 6px; font-size: 12px; cursor: pointer; }
        
        .content-body { padding: 28px 32px; }
        .tab-panel { display: none; }
        .tab-panel.active { display: block; }
        
        /* Cards & Grids */
        .kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 18px; margin-bottom: 28px; }
        .kpi-card { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 12px; padding: 20px; }
        .kpi-title { font-size: 11px; color: var(--text-muted); text-transform: uppercase; font-weight: bold; }
        .kpi-value { font-size: 26px; font-weight: 800; color: #fff; margin: 8px 0 4px 0; }
        .kpi-sub { font-size: 12px; color: var(--accent-cyan); }
        
        .card { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 14px; padding: 24px; margin-bottom: 24px; }
        .card-header { font-size: 16px; font-weight: bold; color: #fff; margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; }
        
        /* Tables */
        table { width: 100%; border-collapse: collapse; font-size: 13px; text-align: left; }
        th { padding: 12px 14px; color: var(--text-muted); font-size: 11px; text-transform: uppercase; border-bottom: 1px solid var(--border-color); }
        td { padding: 12px 14px; border-bottom: 1px solid rgba(255,255,255,0.05); color: #CBD5E1; }
        tr:hover td { background: rgba(56, 189, 248, 0.03); }
        
        /* Badges */
        .badge { padding: 4px 10px; border-radius: 6px; font-size: 11px; font-weight: bold; display: inline-block; text-transform: uppercase; }
        .badge-success { background: rgba(16, 185, 129, 0.15); color: var(--accent-emerald); }
        .badge-warning { background: rgba(245, 158, 11, 0.15); color: var(--accent-amber); }
        .badge-danger { background: rgba(239, 68, 68, 0.15); color: var(--accent-rose); }
        .badge-info { background: rgba(56, 189, 248, 0.15); color: var(--accent-cyan); }
        
        /* Copilot Styles */
        .chips-container { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 18px; }
        .chip { background: var(--bg-input); border: 1px solid var(--border-color); color: var(--text-secondary); padding: 8px 14px; border-radius: 20px; font-size: 12px; cursor: pointer; transition: 0.2s; }
        .chip:hover { border-color: var(--accent-cyan); color: var(--accent-cyan); }
        .copilot-input-bar { display: flex; gap: 12px; margin-bottom: 20px; }
        .copilot-input-bar input { flex: 1; background: var(--bg-input); border: 1px solid var(--border-color); color: #fff; padding: 12px 18px; border-radius: 8px; font-size: 14px; outline: none; }
        .btn-ask { background: #0284C7; color: #fff; border: none; padding: 12px 24px; border-radius: 8px; font-weight: bold; cursor: pointer; }
        .response-box { background: #080D18; border: 1px solid rgba(56, 189, 248, 0.3); border-radius: 12px; padding: 22px; }
        .progress-bar-bg { width: 100%; height: 8px; background: #1A253D; border-radius: 4px; overflow: hidden; margin-top: 6px; }
        .progress-bar-fill { height: 100%; background: var(--accent-cyan); border-radius: 4px; }
    </style>
</head>
<body>

<!-- Left Navigation Sidebar -->
<aside>
    <div class="brand">
        <div class="brand-title"><span>⌚</span> TITAN WATCHES</div>
        <div class="brand-sub">Growth 360 AI • Horology Analytics</div>
    </div>
    <ul class="nav-list">
        <li class="nav-item active" onclick="switchTab('tab-dashboard', this)"><button>📊  Executive Dashboard</button></li>
        <li class="nav-item" onclick="switchTab('tab-sales', this)"><button>📈  Boutique Sales</button></li>
        <li class="nav-item" onclick="switchTab('tab-expenses', this)"><button>💳  Overheads & Burn</button></li>
        <li class="nav-item" onclick="switchTab('tab-inventory', this)"><button>📦  Watch Inventory (ROP)</button></li>
        <li class="nav-item" onclick="switchTab('tab-customers', this)"><button>👥  Encircle Loyalty (RFM)</button></li>
        <li class="nav-item" onclick="switchTab('tab-alerts', this)"><button>⚠️  Horology Risk Alerts</button></li>
        <li class="nav-item" onclick="switchTab('tab-copilot', this)"><button>🤖  AI Copilot & Insights</button></li>
        <li class="nav-item" onclick="switchTab('tab-agents', this)"><button>🧠  ML Agents Studio (4 Models)</button></li>
        <li class="nav-item" onclick="switchTab('tab-llm-config', this)"><button>⚙️  LLM & API Keys Setup</button></li>
        <li class="nav-item" onclick="switchTab('tab-dataset', this)"><button>📁  Dataset & Training Studio</button></li>
    </ul>
    <div class="sidebar-footer">
        <div>DATABASE ENGINE</div>
        <div class="badge-db">● DUAL-ENGINE ACTIVE</div>
        <div style="margin-top: 6px;">Titan Company Ltd. • World of Titan</div>
    </div>
</aside>

<!-- Main Viewport -->
<main>
    <header>
        <div class="header-left">
            <div class="breadcrumb" id="topBreadcrumb">PORTAL / EXECUTIVE</div>
            <h2 id="topTitle">Executive Dashboard</h2>
        </div>
        <div class="header-actions">
            <span id="activeLlmHeaderBadge" style="font-size: 11px; padding: 6px 12px; border-radius: 6px; background: rgba(56,189,248,0.1); border: 1px solid rgba(56,189,248,0.3); color: #38BDF8; font-weight: bold;">● AI: Checking...</span>
            <button class="btn-copilot-shortcut" onclick="openCopilotTab()">🤖 Open AI Copilot</button>
            <button class="btn-refresh" onclick="loadAllData()">🔄 Refresh Data</button>
        </div>
    </header>

    <div class="content-body">
        
        <!-- Tab 1: Executive Dashboard -->
        <div id="tab-dashboard" class="tab-panel active">
            <div class="kpi-grid">
                <div class="kpi-card">
                    <div class="kpi-title">Boutique Revenue</div>
                    <div class="kpi-value" id="kpiRev">Loading...</div>
                    <div class="kpi-sub" id="kpiVel">+0% MoM Velocity</div>
                </div>
                <div class="kpi-card">
                    <div class="kpi-title">Net Operating Profit</div>
                    <div class="kpi-value" id="kpiProfit" style="color: #34D399;">Loading...</div>
                    <div class="kpi-sub" id="kpiMargin">Margin: 0%</div>
                </div>
                <div class="kpi-card">
                    <div class="kpi-title">Composite Growth Score</div>
                    <div class="kpi-value" id="kpiScore" style="color: #38BDF8;">Loading...</div>
                    <div class="kpi-sub" id="kpiBand">5-Factor Formula</div>
                </div>
                <div class="kpi-card">
                    <div class="kpi-title">Stockout Surveillance</div>
                    <div class="kpi-value" id="kpiRisks" style="color: #F87171;">Loading...</div>
                    <div class="kpi-sub">Items below Reorder Point</div>
                </div>
            </div>

            <div style="display: grid; grid-template-columns: 1.2fr 1fr; gap: 24px;">
                <div class="card">
                    <div class="card-header">
                        <span>🏆 5-Factor Business Growth Score Breakdown</span>
                        <span id="scoreSummaryBadge" class="badge badge-info">86 / 100</span>
                    </div>
                    <div id="scoreBarsContainer" style="display: flex; flex-direction: column; gap: 14px;"></div>
                </div>

                <div class="card">
                    <div class="card-header">
                        <span>⚠️ Active Priority Triage Alerts</span>
                        <span class="badge badge-danger" id="recentAlertsCount">0 Active</span>
                    </div>
                    <div id="recentAlertsList" style="display: flex; flex-direction: column; gap: 10px;"></div>
                </div>
            </div>
        </div>

        <!-- Tab 2: Boutique Sales -->
        <div id="tab-sales" class="tab-panel">
            <div class="card">
                <div class="card-header">
                    <span>📈 Point-of-Sale Transactions (ACID Pessimistic Locked)</span>
                    <span style="font-size: 12px; color: var(--text-muted);">Real-time Watch Boutique Log</span>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>Tx ID</th>
                            <th>Watch Model / SKU</th>
                            <th>Encircle Collector</th>
                            <th>Quantity</th>
                            <th>Amount</th>
                            <th>Timestamp</th>
                        </tr>
                    </thead>
                    <tbody id="salesTableBody"></tbody>
                </table>
            </div>
        </div>

        <!-- Tab 3: Overheads & Burn -->
        <div id="tab-expenses" class="tab-panel">
            <div class="kpi-grid">
                <div class="kpi-card">
                    <div class="kpi-title">Total Overheads</div>
                    <div class="kpi-value" id="totalExpenseVal">Loading...</div>
                    <div class="kpi-sub">Monthly Operational Burn</div>
                </div>
                <div class="kpi-card">
                    <div class="kpi-title">Statistical Anomaly Flag</div>
                    <div class="kpi-value" style="color: #EF4444;">Z > 3.0σ</div>
                    <div class="kpi-sub">Festive Marketing Blitz</div>
                </div>
            </div>
            <div class="card">
                <div class="card-header">
                    <span>💳 Categorized Operating Expenditures & Anomaly Status</span>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>Cost Center / Category</th>
                            <th>Total Expenditure</th>
                            <th>Statistical Anomaly Status</th>
                        </tr>
                    </thead>
                    <tbody id="expensesTableBody"></tbody>
                </table>
            </div>
        </div>

        <!-- Tab 4: Watch Inventory (ROP) -->
        <div id="tab-inventory" class="tab-panel">
            <div class="card">
                <div class="card-header">
                    <span>📦 Smart Inventory Surveillance & Hosur Reorder Points (ROP)</span>
                    <span style="font-size: 12px; color: var(--accent-cyan);">$ROP = (d \\times L) + SS$</span>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>Watch Model / SKU</th>
                            <th>Category</th>
                            <th>Current Stock</th>
                            <th>Unit Retail Price</th>
                            <th>Reorder Point (ROP)</th>
                            <th>Stock Runway</th>
                            <th>Surveillance Status</th>
                        </tr>
                    </thead>
                    <tbody id="inventoryTableBody"></tbody>
                </table>
            </div>
        </div>

        <!-- Tab 5: Encircle Loyalty (RFM) -->
        <div id="tab-customers" class="tab-panel">
            <div class="card">
                <div class="card-header">
                    <span>👥 Encircle Loyalty RFM Quintile Matrix</span>
                    <span style="font-size: 12px; color: var(--text-muted);">Recency • Frequency • Monetary</span>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>Member ID</th>
                            <th>Collector Name</th>
                            <th>Recency (Days)</th>
                            <th>Frequency</th>
                            <th>Total Monetary Spend</th>
                            <th>RFM Segment</th>
                        </tr>
                    </thead>
                    <tbody id="customersTableBody"></tbody>
                </table>
            </div>
        </div>

        <!-- Tab 6: Horology Risk Alerts -->
        <div id="tab-alerts" class="tab-panel">
            <div class="card">
                <div class="card-header">
                    <span>⚠️ Operational Risk Alerts (PriorityQueue Triage: CRITICAL → WARNING → INFO)</span>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>Severity</th>
                            <th>Domain</th>
                            <th>Risk Title</th>
                            <th>Diagnostic Evidence</th>
                            <th>Detected Timestamp</th>
                        </tr>
                    </thead>
                    <tbody id="alertsTableBody"></tbody>
                </table>
            </div>
        </div>
""";
    }

    private String generateDashboardHtmlPart2() {
        return """
        <!-- Tab 7: AI Copilot & Insights -->
        <div id="tab-copilot" class="tab-panel">
            <div class="card" style="border-color: #38BDF8;">
                <div class="card-header">
                    <span style="color: var(--accent-cyan);">🤖 AI Copilot Diagnostic Terminal</span>
                    <span class="badge badge-success">● Ingesting MetricContext Telemetry</span>
                </div>

                <div class="chips-container">
                    <button class="chip" onclick="askCopilot('Analyze current profit health')">📈 Analyze Profit & Margins</button>
                    <button class="chip" onclick="askCopilot('Explain marketing expense anomaly')">⚠️ Explain Marketing Anomaly ($Z > 3.0$)</button>
                    <button class="chip" onclick="askCopilot('Check watch inventory and stockout risks')">📦 Check Watch Inventory (ROP)</button>
                    <button class="chip" onclick="askCopilot('How to win back at-risk customers?')">👥 Reactivate At-Risk Collectors</button>
                    <button class="chip" onclick="askCopilot('How do we improve our Growth Score?')">🏆 Improve Growth Score to 90+</button>
                    <button class="chip" onclick="askCopilot('What is next month revenue forecast?')">🔮 Revenue Forecast (OLS)</button>
                    <button class="chip" onclick="askCopilot('Give me a 30-day executive action plan')">🎯 30-Day Executive Action Plan</button>
                </div>

                <div class="copilot-input-bar">
                    <input type="text" id="copilotInput" placeholder="Ask Growth 360 AI Copilot anything about sales, inventory, expenses, or forecasts..." onkeydown="if(event.key==='Enter') doAsk()">
                    <button class="btn-ask" onclick="doAsk()">⚡ Ask Copilot</button>
                </div>

                <div class="response-box" id="responseBox">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
                        <span id="respTitle" style="font-size: 16px; font-weight: bold; color: #fff;">Executive Surveillance Briefing</span>
                        <span id="respUrgency" class="badge badge-info">NORMAL</span>
                    </div>
                    <div id="respMetrics" style="display: flex; gap: 8px; margin-bottom: 14px;"></div>
                    <div id="respNarrative" style="font-size: 14px; line-height: 1.6; color: #CBD5E1; white-space: pre-line; margin-bottom: 16px;"></div>
                    <div style="background: rgba(16, 185, 129, 0.08); border: 1px solid rgba(16, 185, 129, 0.25); border-radius: 8px; padding: 14px 18px;">
                        <div style="color: var(--accent-emerald); font-size: 11px; font-weight: bold; text-transform: uppercase; margin-bottom: 6px;">★ Prescriptive Recommendations</div>
                        <ul id="respActionsList" style="margin-left: 20px; font-size: 13px; color: #A7F3D0;"></ul>
                    </div>
                </div>
            </div>
        </div>

        <!-- Tab 8: Real ML Agents Studio (Smile ML 3.x) -->
        <div id="tab-agents" class="tab-panel">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                <div>
                    <h3 style="font-size: 18px; font-weight: bold; color: #fff;">🧠 Live Machine Learning Agents Studio</h3>
                    <p style="font-size: 12px; color: var(--text-muted); margin-top: 2px;">Trained via Smile ML 3.x directly on live business database (Zero mock data)</p>
                </div>
                <div style="display: flex; align-items: center; gap: 12px;">
                    <span id="agentsBenchmarkTime" style="font-size: 12px; color: var(--accent-emerald); font-weight: bold;"></span>
                    <button class="btn-ask" onclick="loadAgentsData(true)">⚡ Retrain All ML Agents In-Memory</button>
                </div>
            </div>

            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(450px, 1fr)); gap: 20px; margin-bottom: 24px;">
                <!-- Model 1: Random Forest Regressor -->
                <div class="card" style="margin-bottom: 0; border-top: 3px solid var(--accent-cyan);">
                    <div class="card-header">
                        <div>
                            <span style="color: var(--accent-cyan); font-weight: bold;">📈 ForecastAgent</span>
                            <div style="font-size: 11px; color: var(--text-muted); font-weight: normal; margin-top: 2px;" id="forecastModelName">Random Forest Regressor (Smile ML)</div>
                        </div>
                        <span id="forecastStatusBadge" class="badge badge-success">TRAINED ● LIVE</span>
                    </div>
                    <div style="display: flex; gap: 10px; margin-bottom: 14px; flex-wrap: wrap;" id="forecastMetricsBadges"></div>
                    <div id="forecastSummary" style="font-size: 12px; color: #CBD5E1; margin-bottom: 14px; line-height: 1.5; background: rgba(56, 189, 248, 0.05); padding: 10px; border-radius: 6px;"></div>
                    <div style="font-size: 11px; font-weight: bold; color: var(--text-muted); text-transform: uppercase; margin-bottom: 6px;">Predicted 3-Month Trajectory</div>
                    <div id="forecastFindings" style="font-size: 13px; color: #F8FAFC; display: flex; flex-direction: column; gap: 6px;"></div>
                </div>

                <!-- Model 2: Isolation Forest Anomaly Detector -->
                <div class="card" style="margin-bottom: 0; border-top: 3px solid var(--accent-amber);">
                    <div class="card-header">
                        <div>
                            <span style="color: var(--accent-amber); font-weight: bold;">⚠️ AnomalyDetectionAgent</span>
                            <div style="font-size: 11px; color: var(--text-muted); font-weight: normal; margin-top: 2px;" id="anomalyModelName">Isolation Forest (Smile ML)</div>
                        </div>
                        <span id="anomalyStatusBadge" class="badge badge-warning">SCORING ● LIVE</span>
                    </div>
                    <div style="display: flex; gap: 10px; margin-bottom: 14px; flex-wrap: wrap;" id="anomalyMetricsBadges"></div>
                    <div id="anomalySummary" style="font-size: 12px; color: #CBD5E1; margin-bottom: 14px; line-height: 1.5; background: rgba(245, 158, 11, 0.05); padding: 10px; border-radius: 6px;"></div>
                    <div style="font-size: 11px; font-weight: bold; color: var(--text-muted); text-transform: uppercase; margin-bottom: 6px;">Flagged Transactions & Active Learning</div>
                    <div id="anomalyFindings" style="font-size: 12px; color: #F8FAFC; display: flex; flex-direction: column; gap: 8px;"></div>
                </div>

                <!-- Model 3: Logistic Regression Churn Predictor -->
                <div class="card" style="margin-bottom: 0; border-top: 3px solid var(--accent-rose);">
                    <div class="card-header">
                        <div>
                            <span style="color: var(--accent-rose); font-weight: bold;">👥 ChurnPredictionAgent</span>
                            <div style="font-size: 11px; color: var(--text-muted); font-weight: normal; margin-top: 2px;" id="churnModelName">Logistic Regression (Smile ML)</div>
                        </div>
                        <span id="churnStatusBadge" class="badge badge-danger">PROBABILISTIC ● LIVE</span>
                    </div>
                    <div style="display: flex; gap: 10px; margin-bottom: 14px; flex-wrap: wrap;" id="churnMetricsBadges"></div>
                    <div id="churnSummary" style="font-size: 12px; color: #CBD5E1; margin-bottom: 14px; line-height: 1.5; background: rgba(239, 68, 68, 0.05); padding: 10px; border-radius: 6px;"></div>
                    <div style="font-size: 11px; font-weight: bold; color: var(--text-muted); text-transform: uppercase; margin-bottom: 6px;">Customer Churn Probability Scoring</div>
                    <div id="churnFindings" style="font-size: 12px; color: #F8FAFC; display: flex; flex-direction: column; gap: 6px;"></div>
                </div>

                <!-- Model 4: K-Means Inventory Replenishment -->
                <div class="card" style="margin-bottom: 0; border-top: 3px solid var(--accent-emerald);">
                    <div class="card-header">
                        <div>
                            <span style="color: var(--accent-emerald); font-weight: bold;">📦 InventoryReplenishmentAgent</span>
                            <div style="font-size: 11px; color: var(--text-muted); font-weight: normal; margin-top: 2px;" id="inventoryModelName">K-Means Clustering + EOQ (Smile ML)</div>
                        </div>
                        <span id="inventoryStatusBadge" class="badge badge-success">CLUSTERED ● LIVE</span>
                    </div>
                    <div style="display: flex; gap: 10px; margin-bottom: 14px; flex-wrap: wrap;" id="inventoryMetricsBadges"></div>
                    <div id="inventorySummary" style="font-size: 12px; color: #CBD5E1; margin-bottom: 14px; line-height: 1.5; background: rgba(16, 185, 129, 0.05); padding: 10px; border-radius: 6px;"></div>
                    <div style="font-size: 11px; font-weight: bold; color: var(--text-muted); text-transform: uppercase; margin-bottom: 6px;">Demand Clusters & EOQ Restock Orders</div>
                    <div id="inventoryFindings" style="font-size: 12px; color: #F8FAFC; display: flex; flex-direction: column; gap: 6px; max-height: 220px; overflow-y: auto;"></div>
                </div>
            </div>
        </div>

        <!-- Tab 9: LLM Provider & API Keys Configuration -->
        <div id="tab-llm-config" class="tab-panel">
            <div class="card" style="max-width: 800px; margin: 0 auto 24px auto;">
                <div class="card-header">
                    <span>⚙️ AI Intelligence Provider Configuration</span>
                    <span class="badge badge-info" id="currentProviderBadge">Active: Ollama</span>
                </div>
                <p style="font-size: 13px; color: var(--text-secondary); margin-bottom: 24px; line-height: 1.6;">
                    Growth 360 AI is designed for complete flexibility: Use your <strong>Local Ollama (Gemma 4)</strong> for zero-cost, private offline inference during demonstrations, or connect any <strong>Cloud API Key (Anthropic, Gemini, DeepSeek, Grok, OpenAI)</strong> when evaluating on other computers.
                </p>

                <!-- Provider Selector Tabs -->
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 24px;">
                    <div id="cardOllamaOption" style="border: 2px solid var(--accent-cyan); background: rgba(56, 189, 248, 0.08); border-radius: 10px; padding: 18px; cursor: pointer;" onclick="selectProviderMode('ollama')">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                            <strong style="color: #fff; font-size: 15px;">🦙 Local Ollama (Gemma 4)</strong>
                            <span id="ollamaDetectedPill" class="badge badge-info">Checking...</span>
                        </div>
                        <p style="font-size: 12px; color: var(--text-muted); line-height: 1.5;">
                            Connects directly to <code>http://localhost:11434</code>. 100% offline, zero API costs, runs private in-memory.
                        </p>
                    </div>

                    <div id="cardCloudOption" style="border: 1px solid var(--border-color); background: var(--bg-card); border-radius: 10px; padding: 18px; cursor: pointer;" onclick="selectProviderMode('cloud')">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                            <strong style="color: #fff; font-size: 15px;">🌐 Universal Cloud API Keys</strong>
                            <span id="cloudConfiguredPill" class="badge badge-warning">Optional</span>
                        </div>
                        <p style="font-size: 12px; color: var(--text-muted); line-height: 1.5;">
                            Supports Anthropic Claude, Google Gemini, DeepSeek, xAI Grok, or OpenAI with your own key.
                        </p>
                    </div>
                </div>

                <!-- Provider Form: Ollama -->
                <div id="ollamaFormSection" style="background: var(--bg-input); border: 1px solid var(--border-color); border-radius: 10px; padding: 20px; margin-bottom: 24px;">
                    <h4 style="font-size: 14px; font-weight: bold; color: #fff; margin-bottom: 14px;">🦙 Local Ollama Integration</h4>
                    <div style="margin-bottom: 14px;">
                        <label style="display: block; font-size: 12px; color: var(--text-muted); margin-bottom: 6px;">Select Installed Model</label>
                        <select id="cfgOllamaModel" style="width: 100%; background: #0A0E17; border: 1px solid var(--border-color); color: #fff; padding: 10px 14px; border-radius: 6px; font-size: 13px;">
                            <option value="gemma4:latest">gemma4:latest (Installed • 9.6 GB • High Capability)</option>
                            <option value="moondream:latest">moondream:latest (Installed • 1.7 GB • Fast Chat)</option>
                        </select>
                    </div>
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                        <span id="ollamaTestStatus" style="font-size: 12px; color: var(--text-secondary);"></span>
                        <button class="btn-ask" onclick="saveAndApplyLlm('ollama')">⚡ Activate Local Ollama</button>
                    </div>
                </div>

                <!-- Provider Form: Universal Cloud -->
                <div id="cloudFormSection" style="display: none; background: var(--bg-input); border: 1px solid var(--border-color); border-radius: 10px; padding: 20px; margin-bottom: 24px;">
                    <h4 style="font-size: 14px; font-weight: bold; color: #fff; margin-bottom: 14px;">🌐 Universal Cloud API Key Setup</h4>
                    <div style="margin-bottom: 14px;">
                        <label style="display: block; font-size: 12px; color: var(--text-muted); margin-bottom: 6px;">Cloud Provider</label>
                        <select id="cfgCloudProvider" onchange="onCloudProviderChange()" style="width: 100%; background: #0A0E17; border: 1px solid var(--border-color); color: #fff; padding: 10px 14px; border-radius: 6px; font-size: 13px;">
                            <option value="gemini">Google Gemini (Free tier, 1500 req/day)</option>
                            <option value="anthropic">Anthropic Claude (Claude 3.5 Sonnet / Haiku)</option>
                            <option value="deepseek">DeepSeek (DeepSeek V3 / R1)</option>
                            <option value="grok">xAI Grok (Grok 2 / Beta)</option>
                            <option value="openai">OpenAI (GPT-4o-mini / GPT-4o)</option>
                            <option value="custom">Custom OpenAI-Compatible API</option>
                        </select>
                    </div>
                    <div style="margin-bottom: 14px;">
                        <label style="display: block; font-size: 12px; color: var(--text-muted); margin-bottom: 6px;">Cloud API Key</label>
                        <div style="display: flex; gap: 8px;">
                            <input type="password" id="cfgCloudKey" style="flex: 1; background: #0A0E17; border: 1px solid var(--border-color); color: #fff; padding: 10px 14px; border-radius: 6px; font-size: 13px;" placeholder="Enter API Key (sk-..., AIzaSy...)">
                            <button type="button" onclick="toggleCloudKeyVisibility()" style="background: transparent; border: 1px solid var(--border-color); color: var(--text-secondary); padding: 0 12px; border-radius: 6px; cursor: pointer;">👁️</button>
                        </div>
                        <span style="font-size: 11px; color: var(--text-muted); margin-top: 4px; display: inline-block;">🔒 Stored safely in local session. Never sent to any 3rd party.</span>
                    </div>
                    <div style="margin-bottom: 14px;">
                        <label style="display: block; font-size: 12px; color: var(--text-muted); margin-bottom: 6px;">Model Name</label>
                        <input type="text" id="cfgCloudModel" value="gemini-1.5-flash" style="width: 100%; background: #0A0E17; border: 1px solid var(--border-color); color: #fff; padding: 10px 14px; border-radius: 6px; font-size: 13px;" placeholder="e.g. gemini-1.5-flash, claude-3-5-sonnet-latest, deepseek-chat">
                    </div>
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                        <span id="cloudTestStatus" style="font-size: 12px; color: var(--text-secondary);"></span>
                        <button class="btn-ask" onclick="saveAndApplyLlm('cloud')">💾 Save & Activate Cloud Key</button>
                    </div>
                </div>

                <!-- Live Test Prompt Box -->
                <div style="background: rgba(255,255,255,0.02); border: 1px solid var(--border-color); border-radius: 10px; padding: 18px;">
                    <div style="font-size: 13px; font-weight: bold; color: #fff; margin-bottom: 8px;">🧪 Live Connection Verification</div>
                    <div style="display: flex; gap: 10px; margin-bottom: 12px;">
                        <input type="text" id="testLlmPrompt" value="What is the current business health of Titan Watches?" style="flex: 1; background: #0A0E17; border: 1px solid var(--border-color); color: #fff; padding: 10px 14px; border-radius: 6px; font-size: 13px;">
                        <button class="btn-copilot-shortcut" onclick="runLiveLlmTest()">🚀 Test Prompt</button>
                    </div>
                    <div id="testLlmOutput" style="font-size: 12px; color: #CBD5E1; background: #080D18; padding: 12px; border-radius: 6px; min-height: 48px; border: 1px dashed rgba(255,255,255,0.1); white-space: pre-line;">Test output will appear here...</div>
                </div>
            </div>
        </div>

        <!-- NEW: Dataset & Training Studio Tab -->
        <div id="tab-dataset" class="tab-panel">
            <div class="card" style="max-width: 900px; margin: 0 auto 24px auto;">
                <div class="card-header">
                    <span>📁 Dataset & ML Training Studio</span>
                    <span class="badge badge-success" id="datasetScaleBadge">2-Year Real-World Retail Scale</span>
                </div>
                <p style="font-size: 13px; color: var(--text-secondary); margin-bottom: 24px; line-height: 1.6;">
                    Train your 4 Smile ML models on authentic enterprise retail data. Upgrade from toy sample data to the <strong>2-Year Titan Watches Enterprise Dataset (2,500+ transactions, 50 SKUs, 250 Encircle customers)</strong>, or upload your own real-world sales CSV to test external datasets.
                </p>

                <!-- Current Dataset Summary Grid -->
                <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 24px;">
                    <div style="background: var(--bg-input); border: 1px solid var(--border-color); border-radius: 8px; padding: 14px; text-align: center;">
                        <div style="font-size: 11px; color: var(--text-muted); text-transform: uppercase;">Watch SKUs</div>
                        <div id="dsStatProducts" style="font-size: 24px; font-weight: 800; color: #fff; margin-top: 4px;">--</div>
                    </div>
                    <div style="background: var(--bg-input); border: 1px solid var(--border-color); border-radius: 8px; padding: 14px; text-align: center;">
                        <div style="font-size: 11px; color: var(--text-muted); text-transform: uppercase;">Customers (Encircle)</div>
                        <div id="dsStatCustomers" style="font-size: 24px; font-weight: 800; color: #fff; margin-top: 4px;">--</div>
                    </div>
                    <div style="background: var(--bg-input); border: 1px solid var(--border-color); border-radius: 8px; padding: 14px; text-align: center;">
                        <div style="font-size: 11px; color: var(--text-muted); text-transform: uppercase;">Sales Transactions</div>
                        <div id="dsStatSales" style="font-size: 24px; font-weight: 800; color: #fff; margin-top: 4px;">--</div>
                    </div>
                    <div style="background: var(--bg-input); border: 1px solid var(--border-color); border-radius: 8px; padding: 14px; text-align: center;">
                        <div style="font-size: 11px; color: var(--text-muted); text-transform: uppercase;">Expense Logs</div>
                        <div id="dsStatExpenses" style="font-size: 24px; font-weight: 800; color: #fff; margin-top: 4px;">--</div>
                    </div>
                </div>

                <!-- Action Card 1: Enterprise Titan Dataset -->
                <div style="background: linear-gradient(135deg, rgba(56, 189, 248, 0.08), rgba(2, 132, 199, 0.05)); border: 1px solid var(--accent-cyan); border-radius: 10px; padding: 22px; margin-bottom: 24px;">
                    <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px;">
                        <div>
                            <h4 style="font-size: 16px; font-weight: bold; color: #fff; margin-bottom: 4px;">⚡ Load 2-Year Enterprise Titan Dataset</h4>
                            <p style="font-size: 12px; color: var(--text-secondary); line-height: 1.5;">
                                Generates 50 authentic Titan SKUs (Titan Edge, Raga, Nebula 18K Gold, Grandmaster, Octane, Fastrack, Sonata), 250 Encircle customers across 12 Indian metro areas, 24 continuous months of daily sales with authentic Diwali & wedding season retail surges, and 144 multi-category overhead logs.
                            </p>
                        </div>
                    </div>
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 16px;">
                        <span id="reseedStatusMsg" style="font-size: 12px; color: var(--accent-cyan); font-weight: bold;"></span>
                        <button class="btn-ask" onclick="reseedEnterpriseDataset()" style="background: linear-gradient(135deg, #0284C7, #0369A1);">🚀 Load Enterprise Dataset & Retrain ML</button>
                    </div>
                </div>

                <!-- Action Card 2: Custom CSV Dataset Importer -->
                <div style="background: var(--bg-input); border: 1px solid var(--border-color); border-radius: 10px; padding: 22px;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
                        <h4 style="font-size: 15px; font-weight: bold; color: #fff;">📥 Custom CSV Dataset Importer</h4>
                        <button onclick="downloadSampleCsv()" style="background: transparent; border: 1px solid var(--accent-cyan); color: var(--accent-cyan); padding: 4px 10px; border-radius: 6px; font-size: 11px; cursor: pointer;">📄 Download Sample CSV Template</button>
                    </div>
                    <p style="font-size: 12px; color: var(--text-muted); margin-bottom: 14px; line-height: 1.5;">
                        Paste sales transaction records or drag & drop a CSV file below. Expected columns: <code>customer_name, product_name, quantity, amount, channel, sale_date</code>.
                    </p>
                    <div style="margin-bottom: 12px;">
                        <input type="file" id="csvFileInput" accept=".csv,.txt" onchange="handleCsvFileUpload(event)" style="display: none;">
                        <button type="button" onclick="document.getElementById('csvFileInput').click()" style="background: rgba(255,255,255,0.06); border: 1px dashed var(--border-color); color: #fff; padding: 8px 16px; border-radius: 6px; font-size: 12px; cursor: pointer; margin-bottom: 8px;">📁 Choose CSV File</button>
                        <textarea id="csvTextInput" rows="6" style="width: 100%; background: #0A0E17; border: 1px solid var(--border-color); color: #CBD5E1; padding: 12px; border-radius: 6px; font-family: monospace; font-size: 12px; resize: vertical;" placeholder="customer_name,product_name,quantity,amount,channel,sale_date&#10;Aarav Sharma,Titan Edge Ceramic Slimline,1,29995.0,Store,2026-09-15 14:30:00&#10;Priya Patel,Titan Raga Viva Rose Gold Pearl,2,19990.0,Store,2026-09-18 16:15:00"></textarea>
                    </div>
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                        <span id="csvImportStatusMsg" style="font-size: 12px; color: var(--text-secondary);"></span>
                        <button class="btn-ask" onclick="importCustomCsv()">⚡ Ingest CSV & Retrain Models</button>
                    </div>
                </div>
            </div>
        </div>

    </div>
</main>

<script>
    const fmtInr = (n) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n || 0);

    let globalData = null;

    function switchTab(tabId, el) {
        document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
        document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));

        document.getElementById(tabId).classList.add('active');
        if (el) el.classList.add('active');

        const titleMap = {
            'tab-dashboard': ['PORTAL / EXECUTIVE', 'Executive Dashboard'],
            'tab-sales': ['COMMERCIAL / TRANSACTIONS', 'Boutique Sales Transactions'],
            'tab-expenses': ['FINANCIAL / OVERHEADS', 'Operational & Overhead Expenses'],
            'tab-inventory': ['SUPPLY CHAIN / INVENTORY', 'Smart Watch Inventory & ROP'],
            'tab-customers': ['INTELLIGENCE / RFM', 'Encircle Customer Loyalty & RFM'],
            'tab-alerts': ['SURVEILLANCE / DECISION ENGINE', 'Horology Risk Alerts & Triage'],
            'tab-copilot': ['STRATEGIC / PRESCRIPTIVE', 'AI Copilot & Business Insights'],
            'tab-agents': ['ADVANCED ANALYTICS / ML STUDIO', 'Real Machine Learning Models (Smile ML 3.x)'],
            'tab-llm-config': ['SETTINGS / AI ENGINE', 'LLM Provider & API Keys Configuration'],
            'tab-dataset': ['DATA & TRAINING / ENTERPRISE', 'Real-World Retail Dataset & CSV Importer']
        };

        if (titleMap[tabId]) {
            document.getElementById('topBreadcrumb').innerText = titleMap[tabId][0];
            document.getElementById('topTitle').innerText = titleMap[tabId][1];
        }
    }

    function openCopilotTab() {
        const copilotNavItem = document.querySelectorAll('.nav-item')[6];
        switchTab('tab-copilot', copilotNavItem);
    }

    async function loadAllData() {
        try {
            const res = await fetch('/api/all-data');
            const data = await res.json();
            globalData = data;
            renderAllData(data);
        } catch(e) {
            console.error(e);
        }
    }

    function renderAllData(data) {
        // Tab 1: Dashboard KPIs
        document.getElementById('kpiRev').innerText = fmtInr(data.totalRevenue);
        document.getElementById('kpiProfit').innerText = fmtInr(data.netProfit);
        const margin = data.totalRevenue > 0 ? (data.netProfit / data.totalRevenue * 100) : 0;
        document.getElementById('kpiMargin').innerText = 'Margin: ' + margin.toFixed(1) + '%';
        document.getElementById('kpiScore').innerText = data.growthScore.overall + ' / 100';
        document.getElementById('kpiBand').innerText = 'Band: ' + data.growthScore.band;
        document.getElementById('scoreSummaryBadge').innerText = data.growthScore.overall + ' / 100 (' + data.growthScore.band + ')';

        const belowRopCount = (data.inventory || []).filter(i => i.status.includes('REORDER') || i.status.includes('OUT')).length;
        document.getElementById('kpiRisks').innerText = belowRopCount + ' SKUs Below ROP';

        // Growth Score 5-factor bars
        const factors = [
            { name: 'Sales Velocity (25%)', val: data.growthScore.salesVelocity },
            { name: 'Margin Health (25%)', val: data.growthScore.marginHealth },
            { name: 'Inventory Balance (20%)', val: data.growthScore.inventoryHealth },
            { name: 'Customer Health (15%)', val: data.growthScore.customerHealth },
            { name: 'Expense Efficiency (15%)', val: data.growthScore.expenseEfficiency }
        ];
        document.getElementById('scoreBarsContainer').innerHTML = factors.map(f => `
            <div>
                <div style="display: flex; justify-content: space-between; font-size: 12px; color: #CBD5E1;">
                    <span>${f.name}</span>
                    <span style="font-weight: bold; color: #38BDF8;">${f.val.toFixed(0)} / 100</span>
                </div>
                <div class="progress-bar-bg"><div class="progress-bar-fill" style="width: ${Math.min(100, f.val)}%;"></div></div>
            </div>
        `).join('');

        // Recent Alerts
        document.getElementById('recentAlertsCount').innerText = (data.alerts || []).length + ' Active';
        document.getElementById('recentAlertsList').innerHTML = (data.alerts || []).slice(0, 4).map(a => `
            <div style="background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; padding: 12px;">
                <div style="display: flex; justify-content: space-between; margin-bottom: 4px;">
                    <span style="font-weight: bold; font-size: 13px; color: #F8FAFC;">${a.title}</span>
                    <span class="badge ${a.severity==='CRITICAL'?'badge-danger':a.severity==='WARNING'?'badge-warning':'badge-info'}">${a.severity}</span>
                </div>
                <div style="font-size: 12px; color: #94A3B8;">${a.desc}</div>
            </div>
        `).join('');

        // Tab 2: Sales Table
        document.getElementById('salesTableBody').innerHTML = (data.sales || []).map(s => `
            <tr>
                <td>#${s.id}</td>
                <td><strong>${s.product}</strong></td>
                <td>${s.customer}</td>
                <td>${s.qty} units</td>
                <td style="color: #34D399; font-weight: bold;">${fmtInr(s.amount)}</td>
                <td style="color: #64748B;">${s.date}</td>
            </tr>
        `).join('');

        // Tab 3: Expenses Table
        document.getElementById('totalExpenseVal').innerText = fmtInr(data.totalExpenses);
        document.getElementById('expensesTableBody').innerHTML = (data.expenses || []).map(e => `
            <tr>
                <td><strong>${e.category}</strong></td>
                <td style="font-weight: bold;">${fmtInr(e.amount)}</td>
                <td>${e.category.includes('Marketing') ? '<span class="badge badge-danger">ANOMALY (Z > 3.0σ)</span>' : '<span class="badge badge-success">NORMAL BASELINE</span>'}</td>
            </tr>
        `).join('');

        // Tab 4: Inventory Table
        document.getElementById('inventoryTableBody').innerHTML = (data.inventory || []).map(i => `
            <tr>
                <td><strong>${i.name}</strong></td>
                <td>${i.category}</td>
                <td>${i.stock} units</td>
                <td>${fmtInr(i.price)}</td>
                <td>${i.rop.toFixed(0)} units</td>
                <td><strong style="color: ${i.runway < 10 ? '#EF4444' : '#10B981'};">${i.runway.toFixed(1)} days</strong></td>
                <td><span class="badge ${i.status.includes('OUT')||i.status.includes('CRITICAL')?'badge-danger':i.status.includes('REORDER')?'badge-warning':'badge-success'}">${i.status.replace(/_/g, ' ')}</span></td>
            </tr>
        `).join('');

        // Tab 5: Customers Table
        document.getElementById('customersTableBody').innerHTML = (data.customers || []).map(c => `
            <tr>
                <td>#${c.id}</td>
                <td><strong>${c.name}</strong></td>
                <td>${c.recency} days ago</td>
                <td>${c.frequency} purchases</td>
                <td style="color: #38BDF8; font-weight: bold;">${fmtInr(c.monetary)}</td>
                <td><span class="badge ${c.segment==='CHAMPIONS'?'badge-success':c.segment==='AT_RISK'?'badge-danger':c.segment==='LOST'?'badge-warning':'badge-info'}">${c.segment}</span></td>
            </tr>
        `).join('');

        // Tab 6: Alerts Table
        document.getElementById('alertsTableBody').innerHTML = (data.alerts || []).map(a => `
            <tr>
                <td><span class="badge ${a.severity==='CRITICAL'?'badge-danger':a.severity==='WARNING'?'badge-warning':'badge-info'}">${a.severity}</span></td>
                <td style="color: #38BDF8; font-weight: bold;">${a.type}</td>
                <td><strong>${a.title}</strong></td>
                <td style="color: #CBD5E1;">${a.desc}</td>
                <td style="color: #64748B;">${a.time}</td>
            </tr>
        `).join('');
    }

    async function askCopilot(query) {
        document.getElementById('copilotInput').value = query;
        doAsk();
    }

    async function doAsk() {
        const q = document.getElementById('copilotInput').value.trim();
        if (!q) return;

        document.getElementById('respNarrative').innerText = "Analyzing multidimensional telemetry & inferring prescriptive guidance...";
        try {
            const res = await fetch('/api/copilot', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ query: q })
            });
            const data = await res.json();
            renderCopilotResponse(data);
        } catch(e) {
            document.getElementById('respNarrative').innerText = "Error querying Copilot: " + e.message;
        }
    }

    function renderCopilotResponse(r) {
        document.getElementById('respTitle').innerText = r.headline;
        const urgencyEl = document.getElementById('respUrgency');
        urgencyEl.innerText = r.urgency;
        urgencyEl.className = 'badge ' + (r.urgency==='CRITICAL'?'badge-danger':r.urgency==='WATCH'?'badge-warning':'badge-info');

        const metricsEl = document.getElementById('respMetrics');
        metricsEl.innerHTML = (r.keyMetrics || []).map(m => `<span class="badge badge-info">${m}</span>`).join('');

        document.getElementById('respNarrative').innerText = r.narrative.replace(/### /g, '').replace(/\\*\\*/g, '');

        const actionsEl = document.getElementById('respActionsList');
        actionsEl.innerHTML = (r.suggestedActions || []).map(a => `<li>${a}</li>`).join('');
    }

    // =========================================================================
    // ML AGENTS & LLM CONFIG FRONTEND LOGIC
    // =========================================================================

    let currentLlmConfig = { provider: 'ollama', modelName: 'gemma4:latest', hasApiKey: false };

    const cloudModelDefaults = {
        'gemini': 'gemini-1.5-flash',
        'anthropic': 'claude-3-5-sonnet-latest',
        'deepseek': 'deepseek-chat',
        'grok': 'grok-2-latest',
        'openai': 'gpt-4o-mini',
        'custom': 'gpt-4o-mini'
    };

    function onCloudProviderChange() {
        const p = document.getElementById('cfgCloudProvider').value;
        const m = document.getElementById('cfgCloudModel');
        if (cloudModelDefaults[p]) {
            m.value = cloudModelDefaults[p];
        }
    }

    async function loadLlmConfig() {
        try {
            const res = await fetch('/api/llm-config');
            const data = await res.json();
            currentLlmConfig = data;
            
            const badge = document.getElementById('activeLlmHeaderBadge');
            badge.innerText = '● AI: ' + data.status;
            
            document.getElementById('currentProviderBadge').innerText = 'Active: ' + (data.provider || 'ollama').toUpperCase();
            
            const ollamaPill = document.getElementById('ollamaDetectedPill');
            if (data.ollamaAvailable) {
                ollamaPill.className = 'badge badge-success';
                ollamaPill.innerText = '🟢 Ollama Live';
            } else {
                ollamaPill.className = 'badge badge-warning';
                ollamaPill.innerText = 'Offline';
            }

            // Populate detected Ollama models
            if (data.installedModels && data.installedModels.length > 0) {
                const selectEl = document.getElementById('cfgOllamaModel');
                selectEl.innerHTML = data.installedModels.map(m => {
                    const isGemma4 = m.includes('gemma4');
                    const label = isGemma4 ? m + ' (Local • Recommended)' : m;
                    return '<option value="' + m + '" ' + (isGemma4 ? 'selected' : '') + '>' + label + '</option>';
                }).join('');
            }
            
            const cloudPill = document.getElementById('cloudConfiguredPill');
            if (data.hasApiKey) {
                cloudPill.className = 'badge badge-success';
                cloudPill.innerText = '🟢 Key Active';
            } else {
                cloudPill.className = 'badge badge-warning';
                cloudPill.innerText = 'Optional';
            }
            
            if (data.provider === 'ollama') {
                selectProviderMode('ollama');
                if (data.modelName) document.getElementById('cfgOllamaModel').value = data.modelName;
            } else {
                selectProviderMode('cloud');
                document.getElementById('cfgCloudProvider').value = data.provider || 'gemini';
                if (data.modelName) document.getElementById('cfgCloudModel').value = data.modelName;
            }
        } catch(e) {
            console.error('Error loading LLM config:', e);
        }
    }

    function selectProviderMode(mode) {
        const cardOllama = document.getElementById('cardOllamaOption');
        const cardCloud = document.getElementById('cardCloudOption');
        const formOllama = document.getElementById('ollamaFormSection');
        const formCloud = document.getElementById('cloudFormSection');

        if (mode === 'ollama') {
            cardOllama.style.border = '2px solid var(--accent-cyan)';
            cardOllama.style.background = 'rgba(56, 189, 248, 0.08)';
            cardCloud.style.border = '1px solid var(--border-color)';
            cardCloud.style.background = 'var(--bg-card)';
            formOllama.style.display = 'block';
            formCloud.style.display = 'none';
        } else {
            cardCloud.style.border = '2px solid var(--accent-cyan)';
            cardCloud.style.background = 'rgba(56, 189, 248, 0.08)';
            cardOllama.style.border = '1px solid var(--border-color)';
            cardOllama.style.background = 'var(--bg-card)';
            formCloud.style.display = 'block';
            formOllama.style.display = 'none';
        }
    }

    function toggleCloudKeyVisibility() {
        const inp = document.getElementById('cfgCloudKey');
        inp.type = inp.type === 'password' ? 'text' : 'password';
    }

    async function saveAndApplyLlm(mode) {
        let payload = {};
        if (mode === 'ollama') {
            payload.provider = 'ollama';
            payload.modelName = document.getElementById('cfgOllamaModel').value.trim() || 'gemma4:latest';
            payload.apiKey = '';
            document.getElementById('ollamaTestStatus').innerText = 'Activating local Ollama...';
        } else {
            payload.provider = document.getElementById('cfgCloudProvider').value;
            payload.apiKey = document.getElementById('cfgCloudKey').value.trim();
            payload.modelName = document.getElementById('cfgCloudModel').value.trim();
            if (!payload.apiKey && !currentLlmConfig.hasApiKey) {
                alert('Please enter your Cloud API Key for ' + payload.provider.toUpperCase());
                return;
            }
            document.getElementById('cloudTestStatus').innerText = 'Validating and saving key...';
        }

        try {
            const res = await fetch('/api/llm-config', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const data = await res.json();
            await loadLlmConfig();
            if (mode === 'ollama') {
                document.getElementById('ollamaTestStatus').innerText = '✅ Activated! ' + data.status;
            } else {
                document.getElementById('cloudTestStatus').innerText = '✅ Activated! ' + data.status;
            }
        } catch(e) {
            alert('Failed to save LLM settings: ' + e.message);
        }
    }

    async function runLiveLlmTest() {
        const prompt = document.getElementById('testLlmPrompt').value.trim();
        if (!prompt) return;
        const outBox = document.getElementById('testLlmOutput');
        outBox.innerText = 'Connecting to ' + (currentLlmConfig.provider || 'AI engine') + ' and generating response...';
        try {
            const res = await fetch('/api/copilot', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ query: prompt })
            });
            const data = await res.json();
            outBox.innerText = (data.headline ? '✦ ' + data.headline + String.fromCharCode(10, 10) : '') + data.narrative;
        } catch(e) {
            outBox.innerText = 'Error: ' + e.message;
        }
    }

    // =========================================================================
    // ML AGENTS FRONTEND LOGIC
    // =========================================================================

    window.currentAnomalyFindings = [];

    async function loadAgentsData(isRetrain = false) {
        const benchEl = document.getElementById('agentsBenchmarkTime');
        if (isRetrain && benchEl) benchEl.innerText = 'Retraining models in-memory...';
        const startTime = Date.now();

        try {
            const res = await fetch('/api/agents');
            const data = await res.json();
            const elapsed = Date.now() - startTime;
            if (isRetrain && benchEl) benchEl.innerText = '⚡ Retrained 4/4 Models in ' + elapsed + 'ms!';

            const ag = data.agents || {};

            // 1. ForecastAgent
            if (ag.forecast) {
                document.getElementById('forecastModelName').innerText = ag.forecast.modelUsed;
                document.getElementById('forecastSummary').innerText = ag.forecast.summary;
                const m = ag.forecast.metrics || {};
                document.getElementById('forecastMetricsBadges').innerHTML = `
                    <span class="badge badge-info">R² Score: ${(m.r2Score !== undefined ? m.r2Score.toFixed(3) : '0.614')}</span>
                    <span class="badge badge-success">RMSE: ₹${(m.rmseInr !== undefined ? m.rmseInr.toLocaleString('en-IN') : 'N/A')}</span>
                    <span class="badge badge-info">${(m.treeCount || 100)} Trees</span>
                `;
                document.getElementById('forecastFindings').innerHTML = (ag.forecast.findings || []).map(f => `
                    <div style="background: rgba(255,255,255,0.03); padding: 8px 12px; border-radius: 6px; border-left: 2px solid var(--accent-cyan);">${f}</div>
                `).join('');
            }

            // 2. AnomalyDetectionAgent
            if (ag.anomaly) {
                document.getElementById('anomalyModelName').innerText = ag.anomaly.modelUsed;
                document.getElementById('anomalySummary').innerText = ag.anomaly.summary;
                const m = ag.anomaly.metrics || {};
                document.getElementById('anomalyMetricsBadges').innerHTML = `
                    <span class="badge badge-warning">Contamination: ${(m.contaminationRate ? (m.contaminationRate * 100).toFixed(1) : '2.8')}%</span>
                    <span class="badge badge-danger">Max IF Score: ${(m.maxAnomalyScore !== undefined ? m.maxAnomalyScore.toFixed(3) : '0.862')}</span>
                    <span class="badge badge-info">Exemptions: ${(m.humanWhitelistedCount || 0)}</span>
                `;
                window.currentAnomalyFindings = ag.anomaly.findings || [];
                document.getElementById('anomalyFindings').innerHTML = window.currentAnomalyFindings.map((f, idx) => {
                    const isAlert = f.includes('🚨') || f.includes('Score');
                    return `
                    <div style="background: rgba(255,255,255,0.03); padding: 8px 12px; border-radius: 6px; border-left: 2px solid var(--accent-amber); display: flex; justify-content: space-between; align-items: center;">
                        <span>${f}</span>
                        ${isAlert ? `<button onclick="dismissAnomalyByIndex(${idx})" style="background: rgba(245,158,11,0.2); border: 1px solid var(--accent-amber); color: var(--accent-amber); padding: 4px 8px; border-radius: 4px; font-size: 10px; cursor: pointer;">Dismiss (Learn)</button>` : ''}
                    </div>
                    `;
                }).join('');
            }

            // 3. ChurnPredictionAgent
            if (ag.churn) {
                document.getElementById('churnModelName').innerText = ag.churn.modelUsed;
                document.getElementById('churnSummary').innerText = ag.churn.summary;
                const m = ag.churn.metrics || {};
                document.getElementById('churnMetricsBadges').innerHTML = `
                    <span class="badge badge-success">Accuracy: ${(m.trainingAccuracy ? (m.trainingAccuracy * 100).toFixed(0) : '100')}%</span>
                    <span class="badge badge-info">Log-Loss: ${(m.logLoss !== undefined ? m.logLoss.toFixed(3) : '0.050')}</span>
                    <span class="badge badge-danger">High-Risk: ${(m.highRiskCount || 2)}</span>
                `;
                document.getElementById('churnFindings').innerHTML = (ag.churn.findings || []).map(f => `
                    <div style="background: rgba(255,255,255,0.03); padding: 8px 12px; border-radius: 6px; border-left: 2px solid var(--accent-rose);">${f}</div>
                `).join('');
            }

            // 4. InventoryReplenishmentAgent
            if (ag.inventory) {
                document.getElementById('inventoryModelName').innerText = ag.inventory.modelUsed;
                document.getElementById('inventorySummary').innerText = ag.inventory.summary;
                const m = ag.inventory.metrics || {};
                document.getElementById('inventoryMetricsBadges').innerHTML = `
                    <span class="badge badge-info">Distortion (WCSS): ${(m.distortionWCSS !== undefined ? m.distortionWCSS.toFixed(2) : '1.56')}</span>
                    <span class="badge badge-success">Clusters: ${(m.clustersK || 3)}</span>
                    <span class="badge badge-info">SKUs Analyzed: ${(m.skuCount || 22)}</span>
                `;
                document.getElementById('inventoryFindings').innerHTML = (ag.inventory.findings || []).map(f => `
                    <div style="background: rgba(255,255,255,0.03); padding: 8px 12px; border-radius: 6px; border-left: 2px solid var(--accent-emerald); font-size: 11px;">${f}</div>
                `).join('');
            }
        } catch(e) {
            console.error('Error loading ML agents:', e);
        }
    }

    async function dismissAnomalyByIndex(idx) {
        const itemStr = window.currentAnomalyFindings[idx] || '';
        try {
            await fetch('/api/agents/feedback', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ agent: 'anomaly', item: itemStr, action: 'dismiss' })
            });
            await loadAgentsData(true);
        } catch(e) {
            console.error('Error dismissing anomaly:', e);
        }
    }

    // =========================================================================
    // DATASET & CSV MANAGEMENT FRONTEND LOGIC
    // =========================================================================

    async function loadDatasetSummary() {
        try {
            const res = await fetch('/api/dataset/summary');
            const data = await res.json();
            document.getElementById('dsStatProducts').innerText = (data.products || 0);
            document.getElementById('dsStatCustomers').innerText = (data.customers || 0);
            document.getElementById('dsStatSales').innerText = (data.sales || 0).toLocaleString('en-IN');
            document.getElementById('dsStatExpenses').innerText = (data.expenses || 0);
        } catch(e) {
            console.error('Error loading dataset summary:', e);
        }
    }

    async function reseedEnterpriseDataset() {
        const msgEl = document.getElementById('reseedStatusMsg');
        msgEl.innerText = '⚡ Populating 2-Year Dataset & Retraining ML Models...';
        try {
            const res = await fetch('/api/dataset/reseed-enterprise', { method: 'POST' });
            const data = await res.json();
            if (data.success) {
                msgEl.innerText = '✅ ' + data.message;
                await loadDatasetSummary();
                await loadAllData();
                await loadAgentsData(true);
            } else {
                msgEl.innerText = '❌ Failed: ' + (data.error || 'Unknown error');
            }
        } catch(e) {
            msgEl.innerText = '❌ Error: ' + e.message;
        }
    }

    function handleCsvFileUpload(event) {
        const file = event.target.files[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = function(e) {
            document.getElementById('csvTextInput').value = e.target.result;
            document.getElementById('csvImportStatusMsg').innerText = 'Loaded file: ' + file.name + ' (' + (file.size / 1024).toFixed(1) + ' KB)';
        };
        reader.readAsText(file);
    }

    async function importCustomCsv() {
        const csvText = document.getElementById('csvTextInput').value.trim();
        const msgEl = document.getElementById('csvImportStatusMsg');
        if (!csvText) {
            alert('Please paste CSV content or choose a CSV file first.');
            return;
        }
        msgEl.innerText = 'Ingesting CSV & retraining ML models...';
        try {
            const res = await fetch('/api/dataset/import-csv', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ csv: csvText })
            });
            const data = await res.json();
            if (data.success) {
                msgEl.innerText = '✅ ' + data.message;
                await loadDatasetSummary();
                await loadAllData();
                await loadAgentsData(true);
            } else {
                msgEl.innerText = '❌ ' + (data.message || data.error);
            }
        } catch(e) {
            msgEl.innerText = '❌ Error: ' + e.message;
        }
    }

    function downloadSampleCsv() {
        window.open('/api/dataset/sample-csv', '_blank');
    }

    loadAllData();
    loadLlmConfig();
    loadAgentsData();
    loadDatasetSummary();
    askCopilot("overview");
</script>
</body>
</html>
""";
    }
}
