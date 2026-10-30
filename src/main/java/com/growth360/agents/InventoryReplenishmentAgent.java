package com.growth360.agents;

import com.growth360.analytics.ReorderPointCalculator.InventoryMetric;
import com.growth360.analytics.ReorderPointCalculator.InventoryStatus;
import com.growth360.model.Product;
import com.growth360.model.Sale;
import smile.clustering.KMeans;

import java.util.*;

/**
 * Agent 4 — Inventory Replenishment Agent
 *
 * Combines two techniques:
 *   1. K-Means Clustering (Smile ML): segments products into demand tiers
 *      (fast-movers, medium-movers, slow-movers).
 *   2. EOQ Formula (Operations Research): calculates the optimal order quantity.
 *
 * EOQ = sqrt(2 × D × S / H)
 *   D = Annual demand units
 *   S = Ordering cost per order (₹500 per Hosur batch dispatch)
 *   H = Holding cost per unit per year (20% of unit cost)
 */
public class InventoryReplenishmentAgent {

    private static final int    MIN_PRODUCTS        = 3;
    private static final double ORDERING_COST_INR   = 500.0;
    private static final double HOLDING_COST_RATE   = 0.20;

    public AgentResult train(List<InventoryMetric> metrics, List<Product> products) {
        if (metrics == null || metrics.size() < MIN_PRODUCTS) {
            return AgentResult.fallback("InventoryReplenishmentAgent",
                    "Need at least " + MIN_PRODUCTS + " products to run K-Means clustering.");
        }

        try {
            // Normalize features for K-Means
            double maxDemand = metrics.stream().mapToDouble(InventoryMetric::dailyDemand).max().orElse(1.0);
            double maxStock  = metrics.stream().mapToDouble(m -> m.product().getStockQty()).max().orElse(1.0);
            double maxLead   = metrics.stream().mapToDouble(m -> m.product().getLeadTimeDays()).max().orElse(1.0);
            if (maxDemand == 0) maxDemand = 1.0;
            if (maxStock  == 0) maxStock  = 1.0;
            if (maxLead   == 0) maxLead   = 1.0;

            final double fMaxDemand = maxDemand;
            final double fMaxStock  = maxStock;
            final double fMaxLead   = maxLead;

            double[][] data = metrics.stream()
                    .map(m -> new double[]{
                            m.dailyDemand()               / fMaxDemand,
                            m.product().getStockQty()     / fMaxStock,
                            m.product().getLeadTimeDays() / fMaxLead
                    })
                    .toArray(double[][]::new);

            // K-Means with 3 clusters (slow / medium / fast movers)
            int k = Math.min(3, metrics.size());
            KMeans kmeans = KMeans.fit(data, k);

            // Get centroids — Smile 3.x: kmeans.centroids is a public field (double[][])
            double[][] centroids = kmeans.centroids;

            // Rank clusters by centroid demand (first feature)
            // cluster with lowest demand = slow, highest = fast
            Integer[] clusterRanks = new Integer[k];
            for (int i = 0; i < k; i++) clusterRanks[i] = i;
            Arrays.sort(clusterRanks, Comparator.comparingDouble(i -> centroids[i][0]));
            int[] rankMap = new int[k];  // rankMap[clusterIdx] = tier (0=slow, 1=medium, 2=fast)
            for (int rank = 0; rank < k; rank++) rankMap[clusterRanks[rank]] = rank;

            String[] TIER_NAMES = {"🐢 Slow-Mover", "🚶 Medium-Mover", "🚀 Fast-Mover"};

            List<String> findings = new ArrayList<>();
            findings.add("🔬 K-Means clustered " + metrics.size() + " watch SKUs → " + k + " demand tiers");

            for (int i = 0; i < metrics.size(); i++) {
                InventoryMetric m = metrics.get(i);
                Product p = m.product();
                int cluster   = kmeans.predict(data[i]);
                int tier      = rankMap[cluster];
                String label  = tier < TIER_NAMES.length ? TIER_NAMES[tier] : TIER_NAMES[2];

                if (m.status() == InventoryStatus.OUT_OF_STOCK
                        || m.status() == InventoryStatus.CRITICAL_REORDER
                        || m.status() == InventoryStatus.REORDER_SOON) {

                    // EOQ = sqrt(2 × D × S / H)
                    double annualDemand = m.dailyDemand() * 365.0;
                    double holdingCost  = Math.max(1.0, p.getUnitCost() * HOLDING_COST_RATE);
                    int eoq = (int) Math.ceil(Math.sqrt((2.0 * annualDemand * ORDERING_COST_INR) / holdingCost));
                    eoq = Math.max(eoq, m.suggestedReorderQty());

                    findings.add(String.format(
                            "📦 %s [%s] — Stock: %d | Runway: %.0f days | EOQ: %d units | %s",
                            p.getName(), label, p.getStockQty(),
                            m.stockRunwayDays(), eoq, m.status().getLabel()
                    ));
                }
            }

            if (findings.size() == 1) {
                findings.add("✅ All watch SKUs are adequately stocked across all demand tiers.");
            }

            double confidence = Math.min(0.92, 0.6 + (metrics.size() / 40.0));

            Map<String, Double> evalMetrics = new LinkedHashMap<>();
            evalMetrics.put("distortionWCSS", Math.round(kmeans.distortion * 1000.0) / 1000.0);
            evalMetrics.put("clustersK", (double) k);
            evalMetrics.put("skuCount", (double) metrics.size());

            findings.add(0, String.format("📊 Clustering Metrics: K = %d | Within-Cluster Distortion (WCSS) = %.3f | SKUs = %d",
                    k, kmeans.distortion, metrics.size()));

            return new AgentResult(
                    "InventoryReplenishmentAgent",
                    "K-Means Clustering (Smile ML) + EOQ Formula",
                    "📦 Inventory Replenishment — K-Means + EOQ",
                    "K-Means segmented " + metrics.size() + " SKUs into " + k +
                            " demand tiers (Distortion: " + String.format("%.2f", kmeans.distortion) +
                            "). EOQ formula applied for optimal order quantities.",
                    findings,
                    confidence,
                    true,
                    evalMetrics
            );

        } catch (Exception e) {
            return AgentResult.fallback("InventoryReplenishmentAgent",
                    "K-Means training failed: " + e.getMessage());
        }
    }
}
