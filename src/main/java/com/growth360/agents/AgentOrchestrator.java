package com.growth360.agents;

import com.growth360.analytics.ReorderPointCalculator.InventoryMetric;
import com.growth360.model.Customer;
import com.growth360.model.Expense;
import com.growth360.model.Product;
import com.growth360.model.Sale;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * Agent Orchestrator — Manages all 4 ML Agents
 *
 * Runs all agents in parallel using a thread pool (ExecutorService),
 * collects results, and caches them for fast dashboard API responses.
 *
 * Agents managed:
 *   1. ForecastAgent              — Random Forest Regressor (revenue prediction)
 *   2. AnomalyDetectionAgent      — Isolation Forest (expense anomaly detection)
 *   3. ChurnPredictionAgent       — Logistic Regression (customer churn probability)
 *   4. InventoryReplenishmentAgent — K-Means + EOQ (inventory demand clustering)
 */
public class AgentOrchestrator {

    private static final Logger log = Logger.getLogger(AgentOrchestrator.class.getName());

    private final ForecastAgent               forecastAgent;
    private final AnomalyDetectionAgent       anomalyAgent;
    private final ChurnPredictionAgent        churnAgent;
    private final InventoryReplenishmentAgent inventoryAgent;

    private final ExecutorService executor;

    // Cached results — refreshed on each call to runAllAgents()
    private volatile Map<String, AgentResult> lastResults = new LinkedHashMap<>();

    public AgentOrchestrator() {
        this.forecastAgent   = new ForecastAgent();
        this.anomalyAgent    = new AnomalyDetectionAgent();
        this.churnAgent      = new ChurnPredictionAgent();
        this.inventoryAgent  = new InventoryReplenishmentAgent();

        // 4 threads — one per agent, all run in parallel
        this.executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "ML-Agent-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Runs all 4 ML agents IN PARALLEL and returns their results.
     * This is where the actual ML training happens — on every refresh of live data.
     *
     * @param sales     All sales records from DB
     * @param expenses  All expense records from DB
     * @param customers All customer records from DB
     * @param metrics   Pre-computed inventory metrics from ReorderPointCalculator
     * @param products  All product records from DB
     * @return          Map of agent name → AgentResult
     */
    public Map<String, AgentResult> runAllAgents(
            List<Sale> sales,
            List<Expense> expenses,
            List<Customer> customers,
            List<InventoryMetric> metrics,
            List<Product> products
    ) {
        log.info("[AgentOrchestrator] Starting parallel ML agent execution...");
        long startTime = System.currentTimeMillis();

        // Submit all 4 agents to thread pool simultaneously
        Future<AgentResult> forecastFuture   = executor.submit(() -> forecastAgent.train(sales));
        Future<AgentResult> anomalyFuture    = executor.submit(() -> anomalyAgent.train(expenses));
        Future<AgentResult> churnFuture      = executor.submit(() -> churnAgent.train(customers, sales));
        Future<AgentResult> inventoryFuture  = executor.submit(() -> inventoryAgent.train(metrics, products));

        Map<String, AgentResult> results = new LinkedHashMap<>();

        // Collect results (wait for each to complete)
        results.put("forecast",   safeGet(forecastFuture,   "ForecastAgent"));
        results.put("anomaly",    safeGet(anomalyFuture,    "AnomalyDetectionAgent"));
        results.put("churn",      safeGet(churnFuture,      "ChurnPredictionAgent"));
        results.put("inventory",  safeGet(inventoryFuture,  "InventoryReplenishmentAgent"));

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[AgentOrchestrator] All 4 agents completed in " + elapsed + "ms.");

        // Count how many actually trained (vs. fell back)
        long trained = results.values().stream().filter(AgentResult::trained).count();
        log.info("[AgentOrchestrator] " + trained + "/4 agents successfully trained ML models.");

        this.lastResults = results;
        return results;
    }

    /**
     * Returns the last computed agent results (cached — no recomputation).
     */
    public Map<String, AgentResult> getLastResults() {
        return lastResults;
    }

    /**
     * Active Learning feedback loop: Allows humans to provide feedback on agent outputs.
     */
    public void recordFeedback(String agentType, String itemId, String action) {
        log.info("[AgentOrchestrator] Human feedback received: " + agentType + " | item: " + itemId + " | action: " + action);
        if ("anomaly".equalsIgnoreCase(agentType) && ("dismiss".equalsIgnoreCase(action) || "whitelist".equalsIgnoreCase(action))) {
            anomalyAgent.whitelistTransaction(itemId);
        }
    }

    /**
     * Returns true if all 4 agents have been run at least once.
     */
    public boolean hasResults() {
        return !lastResults.isEmpty();
    }

    private AgentResult safeGet(Future<AgentResult> future, String agentName) {
        try {
            return future.get();
        } catch (Exception e) {
            log.severe("[AgentOrchestrator] " + agentName + " threw an exception: " + e.getMessage());
            return AgentResult.fallback(agentName, "Unexpected error: " + e.getMessage());
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}
