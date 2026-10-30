package com.growth360.agents;

import com.growth360.model.Customer;
import com.growth360.model.RfmResult;
import com.growth360.model.Sale;
import smile.classification.LogisticRegression;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent 3 — Churn Prediction Agent
 *
 * Uses Smile's Logistic Regression — a proper supervised ML classification model.
 *
 * What is Logistic Regression?
 *   - A binary classifier: outputs P(churn) for each customer (0 to 1).
 *   - Learns optimal weights (w1, w2, w3) for RFM features from labeled data.
 *   - More accurate than a hardcoded sigmoid formula because the weights are LEARNED.
 *
 * Training:
 *   - Features: [recency_days_normalized, frequency_normalized, monetary_normalized]
 *   - Labels:   1 = will churn (AT_RISK or LOST), 0 = safe
 *
 * The model assigns each customer a precise churn probability, e.g., "78.3% churn risk"
 * instead of just a label like "AT_RISK".
 */
public class ChurnPredictionAgent {

    private static final int MIN_CUSTOMERS = 6;
    private static final int MIN_PER_CLASS = 2; // need at least 2 examples of each class

    public AgentResult train(List<Customer> customers, List<Sale> sales) {
        if (customers == null || customers.size() < MIN_CUSTOMERS) {
            return AgentResult.fallback("ChurnPredictionAgent",
                    "Need at least " + MIN_CUSTOMERS + " customers to train Logistic Regression.");
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            Map<Integer, List<Sale>> salesByCustomer = sales.stream()
                    .collect(Collectors.groupingBy(Sale::getCustomerId));

            // Build RFM raw stats for each customer
            List<double[]> rawFeatures = new ArrayList<>();
            List<Integer> rawLabels   = new ArrayList<>();

            double maxRecency   = 1.0;
            double maxFrequency = 1.0;
            double maxMonetary  = 1.0;

            // First pass: compute raw values
            List<double[]> rawRfm = new ArrayList<>();
            for (Customer c : customers) {
                List<Sale> cs = salesByCustomer.getOrDefault(c.getId(), Collections.emptyList());
                long recencyDays = cs.isEmpty()
                        ? Duration.between(c.getCreatedAt(), now).toDays()
                        : Duration.between(
                                cs.stream().map(Sale::getSaleDate).max(Comparator.naturalOrder()).orElse(c.getCreatedAt()),
                                now).toDays();
                int freq      = cs.size();
                double monetary = cs.stream().mapToDouble(Sale::getAmount).sum();
                rawRfm.add(new double[]{recencyDays, freq, monetary});

                maxRecency   = Math.max(maxRecency, recencyDays);
                maxFrequency = Math.max(maxFrequency, freq);
                maxMonetary  = Math.max(maxMonetary, monetary);
            }

            int churners    = 0;
            int nonChurners = 0;

            for (int i = 0; i < customers.size(); i++) {
                double[] raw = rawRfm.get(i);
                // Normalize to 0–1
                double normRecency   = raw[0] / maxRecency;
                double normFrequency = raw[1] / maxFrequency;
                double normMonetary  = raw[2] / maxMonetary;
                rawFeatures.add(new double[]{normRecency, normFrequency, normMonetary});

                // Label: churn if inactive > 60 days with low frequency, or inactive > 90 days
                boolean isChurn = (raw[0] > 60 && raw[1] <= 4) || (raw[0] > 90);
                int label = isChurn ? 1 : 0;
                rawLabels.add(label);
                if (label == 1) churners++; else nonChurners++;
            }

            if (churners < MIN_PER_CLASS || nonChurners < MIN_PER_CLASS) {
                return AgentResult.fallback("ChurnPredictionAgent",
                        "Need at least " + MIN_PER_CLASS + " churned AND " + MIN_PER_CLASS + " active customers. Found " +
                                churners + " churned, " + nonChurners + " active.");
            }

            double[][] X = rawFeatures.toArray(new double[0][]);
            int[] y = rawLabels.stream().mapToInt(Integer::intValue).toArray();

            // Train Logistic Regression
            LogisticRegression model = LogisticRegression.fit(X, y);

            // Score each customer
            List<String> highRisk = new ArrayList<>();
            List<String> safe     = new ArrayList<>();
            double[] posteriori   = new double[2];

            for (int i = 0; i < customers.size(); i++) {
                model.predict(rawFeatures.get(i), posteriori);
                double churnProbability = posteriori[1] * 100.0;
                String customerName = customers.get(i).getName();

                if (churnProbability >= 65.0) {
                    highRisk.add(String.format("🔴 %s — %.1f%% churn probability", customerName, churnProbability));
                } else if (churnProbability >= 40.0) {
                    highRisk.add(String.format("🟡 %s — %.1f%% churn probability", customerName, churnProbability));
                } else {
                    safe.add(String.format("🟢 %s — %.1f%% churn probability", customerName, churnProbability));
                }
            }

            // Compute model training accuracy & log loss
            int correct = 0;
            double logLoss = 0.0;
            for (int i = 0; i < customers.size(); i++) {
                model.predict(rawFeatures.get(i), posteriori);
                int predClass = posteriori[1] >= 0.5 ? 1 : 0;
                if (predClass == y[i]) correct++;
                double p = Math.max(1e-7, Math.min(1.0 - 1e-7, posteriori[y[i]]));
                logLoss += -Math.log(p);
            }
            double trainAccuracy = (double) correct / customers.size();
            double avgLogLoss = logLoss / customers.size();

            List<String> findings = new ArrayList<>();
            findings.add(String.format("📊 Training Metrics: Accuracy = %.1f%% | Log-Loss = %.3f | Cohort: %d Churned / %d Active",
                    trainAccuracy * 100.0, avgLogLoss, churners, nonChurners));
            findings.addAll(highRisk.stream().limit(8).toList());
            if (safe.size() > 0) findings.add("✅ " + safe.size() + " customers classified as low-churn-risk (stable)");

            double confidence = Math.min(0.95, 0.5 + (customers.size() / 100.0) + (trainAccuracy * 0.15));

            Map<String, Double> metrics = new LinkedHashMap<>();
            metrics.put("trainingAccuracy", Math.round(trainAccuracy * 1000.0) / 1000.0);
            metrics.put("logLoss", Math.round(avgLogLoss * 1000.0) / 1000.0);
            metrics.put("highRiskCount", (double) highRisk.size());
            metrics.put("safeCount", (double) safe.size());
            metrics.put("cohortSize", (double) customers.size());

            return new AgentResult(
                    "ChurnPredictionAgent",
                    "Logistic Regression (Smile ML)",
                    "👥 Customer Churn Prediction — Logistic Regression",
                    "Model trained on normalized RFM features (Accuracy: " + String.format("%.1f%%", trainAccuracy * 100) +
                            "). " + highRisk.size() + " customers flagged as medium-to-high churn risk.",
                    findings,
                    confidence,
                    true,
                    metrics
            );

        } catch (Exception e) {
            return AgentResult.fallback("ChurnPredictionAgent",
                    "Logistic Regression training failed: " + e.getMessage());
        }
    }
}
