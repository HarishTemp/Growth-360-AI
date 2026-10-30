package com.growth360.server;

import com.growth360.copilot.AiCopilotEngine;
import com.growth360.copilot.CopilotResponse;
import com.growth360.model.Alert;
import com.growth360.model.MetricContext;
import com.growth360.service.AnalyticsService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Pure Java Embedded HTTP Server (com.sun.net.httpserver.HttpServer).
 * Zero external web framework dependencies.
 * Serves an executive browser dashboard and JSON REST endpoints for the AI Copilot
 * on http://localhost:8080.
 */
public class LocalhostCopilotServer {

    private final AnalyticsService analyticsService;
    private final AiCopilotEngine copilotEngine;
    private HttpServer server;
    private int port = 8080;

    public LocalhostCopilotServer(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
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
                        // Extract query parameter simply
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

            String html = generateDashboardHtml();
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // =========================================================================
    // JSON Serialization Helpers (Pure Java, No external Jackson/Gson needed)
    // =========================================================================

    private String formatMetricContextJson(MetricContext ctx) {
        return String.format(java.util.Locale.ROOT,
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
            if (i < resp.suggestedActions().size() - 1) actionsJson.append(",");
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
    // Rich Modern Single-Page Dashboard HTML / Web Client
    // =========================================================================

    private String generateDashboardHtml() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Growth 360 AI — Titan Watches Decision Intelligence & Copilot</title>
    <style>
        :root {
            --bg-base: #0B0F19;
            --bg-card: #131B2E;
            --bg-input: #1A253D;
            --text-primary: #F8FAFC;
            --text-secondary: #94A3B8;
            --accent-cyan: #38BDF8;
            --accent-emerald: #10B981;
            --accent-amber: #F59E0B;
            --accent-rose: #EF4444;
            --border-card: rgba(56, 189, 248, 0.15);
        }
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        body { background: var(--bg-base); color: var(--text-primary); padding: 24px; min-height: 100vh; }
        .container { max-width: 1200px; margin: 0 auto; }
        header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border-card); padding-bottom: 18px; margin-bottom: 24px; }
        .logo { display: flex; align-items: center; gap: 12px; }
        .logo-icon { font-size: 32px; }
        .logo h1 { font-size: 22px; font-weight: 800; letter-spacing: 0.5px; }
        .logo span { color: var(--accent-cyan); font-size: 13px; font-weight: 600; display: block; }
        .badge-live { background: rgba(16, 185, 129, 0.15); color: var(--accent-emerald); border: 1px solid rgba(16, 185, 129, 0.3); padding: 6px 14px; border-radius: 20px; font-size: 12px; font-weight: bold; }
        .kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-bottom: 28px; }
        .kpi-card { background: var(--bg-card); border: 1px solid var(--border-card); border-radius: 12px; padding: 18px; }
        .kpi-card .title { font-size: 12px; color: var(--text-secondary); text-transform: uppercase; font-weight: bold; margin-bottom: 6px; }
        .kpi-card .value { font-size: 24px; font-weight: 800; color: var(--text-primary); }
        .kpi-card .sub { font-size: 12px; margin-top: 4px; color: var(--accent-cyan); }
        .copilot-section { background: var(--bg-card); border: 1px solid var(--border-card); border-radius: 16px; padding: 24px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
        .copilot-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
        .copilot-title { font-size: 18px; font-weight: 700; color: var(--accent-cyan); display: flex; align-items: center; gap: 8px; }
        .chips-container { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 18px; }
        .chip { background: var(--bg-input); border: 1px solid var(--border-card); color: var(--text-secondary); padding: 8px 14px; border-radius: 20px; font-size: 13px; cursor: pointer; transition: all 0.2s; }
        .chip:hover { border-color: var(--accent-cyan); color: var(--accent-cyan); transform: translateY(-2px); }
        .input-bar { display: flex; gap: 12px; margin-bottom: 24px; }
        .input-bar input { flex: 1; background: var(--bg-input); border: 1px solid var(--border-card); color: #fff; padding: 12px 18px; border-radius: 10px; font-size: 14px; outline: none; }
        .input-bar input:focus { border-color: var(--accent-cyan); box-shadow: 0 0 0 2px rgba(56, 189, 248, 0.2); }
        .btn-ask { background: linear-gradient(135deg, #0284C7, #0369A1); color: #fff; border: none; padding: 12px 24px; border-radius: 10px; font-weight: bold; cursor: pointer; font-size: 14px; transition: 0.2s; }
        .btn-ask:hover { background: #0284C7; transform: translateY(-1px); }
        .response-box { background: #0B111E; border: 1px solid rgba(56, 189, 248, 0.25); border-radius: 12px; padding: 20px; }
        .response-headline { font-size: 16px; font-weight: bold; color: #fff; margin-bottom: 12px; display: flex; align-items: center; justify-content: space-between; }
        .urgency-badge { font-size: 11px; padding: 4px 10px; border-radius: 6px; font-weight: bold; text-transform: uppercase; }
        .urgency-NORMAL { background: rgba(56, 189, 248, 0.2); color: var(--accent-cyan); }
        .urgency-WATCH { background: rgba(245, 158, 11, 0.2); color: var(--accent-amber); }
        .urgency-CRITICAL { background: rgba(239, 68, 68, 0.2); color: var(--accent-rose); }
        .metrics-row { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 14px; }
        .metric-tag { background: rgba(255, 255, 255, 0.06); border: 1px solid rgba(255, 255, 255, 0.1); padding: 4px 10px; border-radius: 6px; font-size: 12px; font-weight: 600; color: #38BDF8; }
        .narrative-content { font-size: 14px; line-height: 1.6; color: #CBD5E1; white-space: pre-line; margin-bottom: 16px; }
        .actions-list { background: rgba(16, 185, 129, 0.08); border: 1px solid rgba(16, 185, 129, 0.25); border-radius: 8px; padding: 14px 18px; margin-bottom: 16px; }
        .actions-list h4 { color: var(--accent-emerald); font-size: 12px; text-transform: uppercase; margin-bottom: 8px; }
        .actions-list li { margin-left: 20px; font-size: 13px; color: #A7F3D0; margin-bottom: 4px; }
    </style>
</head>
<body>
<div class="container">
    <header>
        <div class="logo">
            <div class="logo-icon">⌚</div>
            <div>
                <h1>GROWTH 360 AI</h1>
                <span>Titan Watches • Horology Decision Intelligence</span>
            </div>
        </div>
        <div class="badge-live">● ENGINE LIVE (Pure Java SE)</div>
    </header>

    <div class="kpi-grid">
        <div class="kpi-card">
            <div class="title">Boutique Revenue</div>
            <div class="value" id="valRevenue">Loading...</div>
            <div class="sub" id="valVelocity">+0% MoM</div>
        </div>
        <div class="kpi-card">
            <div class="title">Operating Margin</div>
            <div class="value" id="valMargin">Loading...</div>
            <div class="sub">Luxury Benchmark: 22-28%</div>
        </div>
        <div class="kpi-card">
            <div class="title">Growth Score</div>
            <div class="value" id="valScore">Loading...</div>
            <div class="sub" id="valBand">5-Factor Evaluator</div>
        </div>
        <div class="kpi-card">
            <div class="title">Supply Chain Surveillance</div>
            <div class="value" id="valRisks">Loading...</div>
            <div class="sub">Reorder Point (ROP) Alerts</div>
        </div>
    </div>

    <div class="copilot-section">
        <div class="copilot-header">
            <div class="copilot-title">🤖 AI Copilot Diagnostic Terminal</div>
            <span style="font-size: 12px; color: var(--text-secondary);">Ingesting live <code>MetricContext</code> telemetry</span>
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

        <div class="input-bar">
            <input type="text" id="copilotInput" placeholder="Ask Growth 360 AI Copilot anything about sales, inventory, expenses, or forecasts..." onkeydown="if(event.key==='Enter') doAsk()">
            <button class="btn-ask" onclick="doAsk()">⚡ Ask Copilot</button>
        </div>

        <div class="response-box" id="responseBox">
            <div class="response-headline">
                <span id="respTitle">Executive Surveillance Briefing</span>
                <span id="respUrgency" class="urgency-badge urgency-NORMAL">NORMAL</span>
            </div>
            <div class="metrics-row" id="respMetrics"></div>
            <div class="narrative-content" id="respNarrative">Synthesizing live operational parameters...</div>
            <div class="actions-list" id="respActions">
                <h4>★ Prescriptive Recommendations</h4>
                <ul id="respActionsList"></ul>
            </div>
        </div>
    </div>
</div>

<script>
    const fmtInr = (n) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n);

    async function loadMetrics() {
        try {
            const res = await fetch('/api/metrics');
            const data = await res.json();
            document.getElementById('valRevenue').innerText = fmtInr(data.totalRevenue);
            document.getElementById('valVelocity').innerText = '+' + data.momRevenueGrowth.toFixed(1) + '% MoM';
            document.getElementById('valMargin').innerText = data.profitMargin.toFixed(1) + '%';
            document.getElementById('valScore').innerText = data.growthScore.overall + ' / 100';
            document.getElementById('valBand').innerText = data.growthScore.band;
            document.getElementById('valRisks').innerText = data.inventoryRiskCount + ' SKUs Below ROP';
        } catch(e) {
            console.error(e);
        }
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
            renderResponse(data);
        } catch(e) {
            document.getElementById('respNarrative').innerText = "Error querying Copilot: " + e.message;
        }
    }

    function renderResponse(r) {
        document.getElementById('respTitle').innerText = r.headline;
        const urgencyEl = document.getElementById('respUrgency');
        urgencyEl.innerText = r.urgency;
        urgencyEl.className = 'urgency-badge urgency-' + r.urgency;

        const metricsEl = document.getElementById('respMetrics');
        metricsEl.innerHTML = (r.keyMetrics || []).map(m => `<span class="metric-tag">${m}</span>`).join('');

        document.getElementById('respNarrative').innerText = r.narrative.replace(/### /g, '').replace(/\\*\\*/g, '');

        const actionsEl = document.getElementById('respActionsList');
        actionsEl.innerHTML = (r.suggestedActions || []).map(a => `<li>${a}</li>`).join('');
    }

    loadMetrics();
    askCopilot("overview");
</script>
</body>
</html>
""";
    }
}
