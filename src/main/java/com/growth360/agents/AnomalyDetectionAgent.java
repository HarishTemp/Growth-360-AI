package com.growth360.agents;

import com.growth360.model.Expense;
import smile.anomaly.IsolationForest;

import java.util.*;

/**
 * Agent 2 — Anomaly Detection Agent
 *
 * Uses Smile's Isolation Forest — a proper tree-based ML anomaly detector.
 * Replaces the statistical Z-Score formula with a true unsupervised ML model.
 *
 * How Isolation Forest works:
 *   - Builds random trees that try to "isolate" each data point.
 *   - Anomalies are isolated in fewer splits (shorter path length).
 *   - Score closer to 1.0 = highly anomalous.
 *   - Score closer to 0.0 = normal.
 *
 * Training features per expense:
 *   - normalized_amount  : expense amount scaled 0–1
 *   - category_encoded   : numeric category ID (scaled 0–1)
 */
public class AnomalyDetectionAgent {

    private static final int    MIN_TRAINING_RECORDS = 8;
    private static final double ANOMALY_THRESHOLD    = 0.60;

    // Fixed category encoding for consistency across runs
    private static final Map<String, Integer> CATEGORY_MAP = Map.of(
            "Marketing",     1,
            "Rent",          2,
            "Salaries",      3,
            "Utilities",     4,
            "Logistics",     5,
            "Maintenance",   6,
            "IT & Software", 7,
            "Training",      8,
            "Miscellaneous", 9
    );

    // Active Learning / Human-in-the-Loop: Whitelisted transactions dismissed by store manager
    private final Set<String> whitelistedSignatures = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Allows managers to whitelist/dismiss false alarms (Active Learning feedback loop) */
    public void whitelistTransaction(String descriptionOrCategory) {
        if (descriptionOrCategory != null && !descriptionOrCategory.isBlank()) {
            whitelistedSignatures.add(descriptionOrCategory.trim().toLowerCase());
        }
    }

    public Set<String> getWhitelistedTransactions() {
        return Collections.unmodifiableSet(whitelistedSignatures);
    }

    public AgentResult train(List<Expense> expenses) {
        if (expenses == null || expenses.size() < MIN_TRAINING_RECORDS) {
            return AgentResult.fallback("AnomalyDetectionAgent",
                    "Need at least " + MIN_TRAINING_RECORDS + " expense records to train Isolation Forest.");
        }

        try {
            double maxAmount = expenses.stream().mapToDouble(Expense::getAmount).max().orElse(1.0);
            double minAmount = expenses.stream().mapToDouble(Expense::getAmount).min().orElse(0.0);
            double range     = (maxAmount - minAmount) == 0 ? 1.0 : (maxAmount - minAmount);

            // Build training matrix: [normalized_amount, category_encoded]
            double[][] trainingData = expenses.stream()
                    .map(e -> new double[]{
                            (e.getAmount() - minAmount) / range,
                            CATEGORY_MAP.getOrDefault(e.getCategory(), 9) / 9.0
                    })
                    .toArray(double[][]::new);

            // Smile 3.x IsolationForest.fit(data)
            IsolationForest model = IsolationForest.fit(trainingData);

            // Score each expense and flag anomalies
            List<String> anomalies = new ArrayList<>();
            int flaggedCount = 0;
            double maxObservedScore = 0.0;

            for (Expense e : expenses) {
                double normalizedAmt = (e.getAmount() - minAmount) / range;
                int catCode = CATEGORY_MAP.getOrDefault(e.getCategory(), 9);
                double[] sample = {normalizedAmt, catCode / 9.0};

                double score = model.score(sample);
                if (score > maxObservedScore) maxObservedScore = score;

                if (score >= ANOMALY_THRESHOLD) {
                    String desc = e.getDescription() != null && !e.getDescription().isBlank()
                            ? e.getDescription() : e.getCategory();

                    // Check active learning whitelist
                    if (whitelistedSignatures.contains(desc.toLowerCase()) ||
                        whitelistedSignatures.contains(e.getCategory().toLowerCase())) {
                        continue; // Suppressed by human manager feedback
                    }

                    flaggedCount++;
                    anomalies.add(String.format(
                            "🚨 [IF Score: %.2f] %s — ₹%,.0f (%s)",
                            score, desc, e.getAmount(), e.getCategory()
                    ));
                }
            }

            if (anomalies.isEmpty()) {
                anomalies.add("✅ No anomalies detected. All expense records within normal distribution.");
            }

            double confidence = Math.min(1.0, expenses.size() / 50.0);
            double contamination = Math.round((flaggedCount / (double) expenses.size()) * 1000.0) / 1000.0;

            Map<String, Double> metrics = new LinkedHashMap<>();
            metrics.put("contaminationRate", contamination);
            metrics.put("maxAnomalyScore", Math.round(maxObservedScore * 1000.0) / 1000.0);
            metrics.put("anomalyThreshold", ANOMALY_THRESHOLD);
            metrics.put("sampleSize", (double) expenses.size());
            metrics.put("humanWhitelistedCount", (double) whitelistedSignatures.size());

            return new AgentResult(
                    "AnomalyDetectionAgent",
                    "Isolation Forest (Smile ML)",
                    "⚠️ Expense Anomaly Detection — Isolation Forest",
                    "Trained on " + expenses.size() + " expense records. Flagged " + flaggedCount +
                            " anomalous transactions (threshold: score ≥ " + ANOMALY_THRESHOLD + ", contamination: " +
                            String.format("%.1f%%", contamination * 100) + ").",
                    anomalies,
                    confidence,
                    true,
                    metrics
            );

        } catch (Exception e) {
            return AgentResult.fallback("AnomalyDetectionAgent",
                    "Isolation Forest training failed: " + e.getMessage());
        }
    }
}
