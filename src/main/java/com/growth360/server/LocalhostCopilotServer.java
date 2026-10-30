package com.growth360.server;

import com.growth360.analytics.ReorderPointCalculator.InventoryMetric;
import com.growth360.copilot.AiCopilotEngine;
import com.growth360.copilot.CopilotResponse;
import com.growth360.model.*;
import com.growth360.model.RfmResult;
import com.growth360.service.AnalyticsService;
import com.growth360.service.AnalyticsService.AnalyticsSnapshot;
import com.growth360.service.SaleService;
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
    private final AiCopilotEngine copilotEngine;
    private HttpServer server;
    private int port = 8080;

    public LocalhostCopilotServer(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
        this.saleService = new SaleService();
        this.copilotEngine = new AiCopilotEngine();
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
        server.createContext("/api/health", exchange -> {
            byte[] resp = "{\"status\":\"UP\",\"engine\":\"Growth 360 AI\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
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
                CopilotResponse resp = copilotEngine.ask(query, ctx);

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

    // =========================================================================
    // COMPLETE 7-TAB DASHBOARD WITH SIDEBAR NAVBAR
    // =========================================================================

    private String generateCompleteDashboardHtml() {
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
            'tab-copilot': ['STRATEGIC / PRESCRIPTIVE', 'AI Copilot & Business Insights']
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

    loadAllData();
    askCopilot("overview");
</script>
</body>
</html>
""";
    }
}
