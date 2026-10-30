package com.growth360.analytics;

import com.growth360.model.Alert;
import com.growth360.model.Alert.AlertType;
import com.growth360.model.Alert.Severity;
import com.growth360.model.Product;
import com.growth360.model.Sale;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Intelligent inventory calculations: Daily Demand, ROP, Runway, and Reorder Quantity.
 */
public class ReorderPointCalculator {

    private static final int DEFAULT_ANALYSIS_DAYS = 60;
    private static final int DEFAULT_REVIEW_PERIOD_DAYS = 14;

    public record InventoryMetric(
            Product product,
            double dailyDemand,
            double reorderPoint,
            double stockRunwayDays,
            int suggestedReorderQty,
            InventoryStatus status
    ) {}

    public enum InventoryStatus {
        OUT_OF_STOCK("Out of Stock", "#EF4444"),
        CRITICAL_REORDER("Reorder Urgently", "#DC2626"),
        REORDER_SOON("Reorder Needed", "#F59E0B"),
        HEALTHY("Optimal Stock", "#10B981"),
        OVERSTOCK("Overstocked", "#38BDF8");

        private final String label;
        private final String colorHex;

        InventoryStatus(String label, String colorHex) {
            this.label = label;
            this.colorHex = colorHex;
        }

        public String getLabel() { return label; }
        public String getColorHex() { return colorHex; }
    }

    /**
     * Calculates comprehensive inventory metrics for each product based on past sales.
     */
    public List<InventoryMetric> calculateMetrics(List<Product> products, List<Sale> sales) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(DEFAULT_ANALYSIS_DAYS);

        // Group quantity sold per product over the recent analysis window
        Map<Integer, Integer> unitsSoldMap = sales.stream()
                .filter(s -> s.getSaleDate().isAfter(cutoff))
                .collect(Collectors.groupingBy(
                        Sale::getProductId,
                        Collectors.summingInt(Sale::getQuantity)
                ));

        List<InventoryMetric> metrics = new ArrayList<>();

        for (Product p : products) {
            int unitsSold = unitsSoldMap.getOrDefault(p.getId(), 0);
            double dailyDemand = (double) unitsSold / DEFAULT_ANALYSIS_DAYS;

            // If sales history is very low, estimate a baseline demand
            if (dailyDemand <= 0.05) {
                dailyDemand = 0.2;
            }

            double rop = (dailyDemand * p.getLeadTimeDays()) + p.getSafetyStock();
            double runwayDays = dailyDemand > 0 ? ((double) p.getStockQty() / dailyDemand) : 999.0;

            // Suggested reorder qty covers lead time + review cycle
            double targetStock = dailyDemand * (p.getLeadTimeDays() + DEFAULT_REVIEW_PERIOD_DAYS) + p.getSafetyStock();
            int suggestedReorderQty = (int) Math.max(0, Math.ceil(targetStock - p.getStockQty()));

            InventoryStatus status;
            if (p.getStockQty() <= 0) {
                status = InventoryStatus.OUT_OF_STOCK;
            } else if (p.getStockQty() <= p.getSafetyStock()) {
                status = InventoryStatus.CRITICAL_REORDER;
            } else if (p.getStockQty() <= rop) {
                status = InventoryStatus.REORDER_SOON;
            } else if (runwayDays > 120 && p.getStockQty() > 100) {
                status = InventoryStatus.OVERSTOCK;
            } else {
                status = InventoryStatus.HEALTHY;
            }

            metrics.add(new InventoryMetric(p, dailyDemand, rop, runwayDays, suggestedReorderQty, status));
        }

        return metrics;
    }

    /**
     * Generates business Risk Alerts for critical inventory conditions.
     */
    public List<Alert> generateInventoryAlerts(List<InventoryMetric> metrics) {
        List<Alert> alerts = new ArrayList<>();

        for (InventoryMetric m : metrics) {
            Product p = m.product();
            if (m.status() == InventoryStatus.OUT_OF_STOCK) {
                alerts.add(new Alert(
                        "INV-STOCKOUT-" + p.getId(),
                        AlertType.INVENTORY,
                        Severity.CRITICAL,
                        "Stockout Alert: " + p.getName(),
                        String.format("Product '%s' is completely out of stock. Suggested reorder: %d units immediately.",
                                p.getName(), m.suggestedReorderQty()),
                        0.0,
                        LocalDateTime.now()
                ));
            } else if (m.status() == InventoryStatus.CRITICAL_REORDER) {
                alerts.add(new Alert(
                        "INV-CRIT-" + p.getId(),
                        AlertType.INVENTORY,
                        Severity.CRITICAL,
                        "Critical Stock Level: " + p.getName(),
                        String.format("Stock (%d) has breached safety stock (%d). Stock runway: %.1f days. Reorder %d units.",
                                p.getStockQty(), p.getSafetyStock(), m.stockRunwayDays(), m.suggestedReorderQty()),
                        p.getStockQty(),
                        LocalDateTime.now()
                ));
            } else if (m.status() == InventoryStatus.REORDER_SOON) {
                alerts.add(new Alert(
                        "INV-ROP-" + p.getId(),
                        AlertType.INVENTORY,
                        Severity.WARNING,
                        "Reorder Point Breached: " + p.getName(),
                        String.format("Stock (%d) is below ROP (%.1f). Lead time is %d days. Reorder %d units.",
                                p.getStockQty(), m.reorderPoint(), p.getLeadTimeDays(), m.suggestedReorderQty()),
                        p.getStockQty(),
                        LocalDateTime.now()
                ));
            } else if (m.status() == InventoryStatus.OVERSTOCK) {
                alerts.add(new Alert(
                        "INV-OVER-" + p.getId(),
                        AlertType.INVENTORY,
                        Severity.INFO,
                        "Overstock Alert: " + p.getName(),
                        String.format("High inventory holding (%d units, runway: %.0f days). Capital tied up: $%.2f.",
                                p.getStockQty(), m.stockRunwayDays(), p.getStockQty() * p.getUnitCost()),
                        p.getStockQty(),
                        LocalDateTime.now()
                ));
            }
        }

        return alerts;
    }
}
