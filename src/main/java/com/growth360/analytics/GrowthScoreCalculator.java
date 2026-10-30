package com.growth360.analytics;

import com.growth360.analytics.ReorderPointCalculator.InventoryMetric;
import com.growth360.analytics.ReorderPointCalculator.InventoryStatus;
import com.growth360.model.Expense;
import com.growth360.model.GrowthScore;
import com.growth360.model.RfmResult;
import com.growth360.model.Sale;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mathematical Engine computing the 5-component composite Business Growth Score (0 - 100)
 * strictly adhering to Section 28 - 33 of the specification:
 * 1. Sales Velocity       (25%)
 * 2. Net Margin Health    (25%)
 * 3. Inventory Balance    (20%)
 * 4. Customer Health      (15%)
 * 5. Expense Efficiency   (15%)
 */
public class GrowthScoreCalculator {

    public static final double WEIGHT_SALES_VELOCITY = 0.25;
    public static final double WEIGHT_MARGIN_HEALTH = 0.25;
    public static final double WEIGHT_INVENTORY_BALANCE = 0.20;
    public static final double WEIGHT_CUSTOMER_HEALTH = 0.15;
    public static final double WEIGHT_EXPENSE_EFFICIENCY = 0.15;

    // Type alias / inner record maintaining full backward compatibility
    public record GrowthScoreResult(
            int overallScore,
            String healthBand,
            String healthColorHex,
            double salesVelocity,
            double marginHealth,
            double inventoryBalance,
            double customerHealth,
            double expenseEfficiency,
            String summaryInsight
    ) {
        // Compatibility methods for existing views
        public double revenueScore() { return salesVelocity; }
        public double expenseScore() { return expenseEfficiency; }
        public double inventoryScore() { return inventoryBalance; }
        public double customerScore() { return customerHealth; }

        public GrowthScore toGrowthScore() {
            return new GrowthScore(overallScore, healthBand, healthColorHex, salesVelocity, marginHealth, inventoryBalance, customerHealth, expenseEfficiency, summaryInsight);
        }
    }

    public GrowthScoreResult calculateScore(
            List<Sale> sales,
            List<Expense> expenses,
            List<InventoryMetric> inventoryMetrics,
            List<RfmResult> rfmResults) {

        double salesVelocity = computeSalesVelocity(sales);
        double marginHealth = computeMarginHealth(sales, expenses);
        double inventoryBalance = computeInventoryBalance(inventoryMetrics);
        double customerHealth = computeCustomerHealth(rfmResults);
        double expenseEfficiency = computeExpenseEfficiency(sales, expenses);

        double totalWeighted = (salesVelocity * WEIGHT_SALES_VELOCITY) +
                               (marginHealth * WEIGHT_MARGIN_HEALTH) +
                               (inventoryBalance * WEIGHT_INVENTORY_BALANCE) +
                               (customerHealth * WEIGHT_CUSTOMER_HEALTH) +
                               (expenseEfficiency * WEIGHT_EXPENSE_EFFICIENCY);

        int finalScore = (int) Math.round(Math.max(0.0, Math.min(100.0, totalWeighted)));

        String band;
        String colorHex;
        String insight;

        if (finalScore >= 80) {
            band = "Excellent";
            colorHex = "#10B981";
            insight = "Strong growth trajectory with robust margins, active customer engagement, and balanced inventory.";
        } else if (finalScore >= 60) {
            band = "Good";
            colorHex = "#38BDF8";
            insight = "Stable performance with healthy operations, though minor optimization in inventory or customer retention is recommended.";
        } else if (finalScore >= 40) {
            band = "Needs Attention";
            colorHex = "#F59E0B";
            insight = "Moderate business vulnerabilities detected. Immediate focus needed on stock replenishment and customer re-engagement.";
        } else {
            band = "Critical";
            colorHex = "#EF4444";
            insight = "High risk profile. Significant margin compression, elevated customer churn, or severe inventory stockouts require intervention.";
        }

        return new GrowthScoreResult(
                finalScore,
                band,
                colorHex,
                round1(salesVelocity),
                round1(marginHealth),
                round1(inventoryBalance),
                round1(customerHealth),
                round1(expenseEfficiency),
                insight
        );
    }

    /**
     * Section 29: Sales Velocity (25%)
     * MoM Growth = ((Current Revenue - Previous Revenue) / Previous Revenue) * 100
     * Target: 10%
     * SalesVelocity = clamp((MoM Growth / 10) * 100, 0, 100)
     */
    private double computeSalesVelocity(List<Sale> sales) {
        if (sales == null || sales.isEmpty()) return 50.0;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midPoint = now.minusDays(30);
        LocalDateTime startPoint = now.minusDays(60);

        double currentRevenue = sales.stream()
                .filter(s -> s.getSaleDate().isAfter(midPoint))
                .mapToDouble(Sale::getAmount)
                .sum();

        double prevRevenue = sales.stream()
                .filter(s -> s.getSaleDate().isAfter(startPoint) && s.getSaleDate().isBefore(midPoint))
                .mapToDouble(Sale::getAmount)
                .sum();

        if (prevRevenue <= 0) {
            return currentRevenue > 0 ? 80.0 : 50.0;
        }

        double momGrowth = ((currentRevenue - prevRevenue) / prevRevenue) * 100.0;
        double score = (momGrowth / 10.0) * 100.0;
        return Math.max(0.0, Math.min(100.0, score));
    }

    /**
     * Section 30: Net Margin Health (25%)
     * Profit = Revenue - Expenses
     * Net Margin = (Profit / Revenue) * 100
     * Mapping: 0% -> 0 score, 40% -> 100 score
     * MarginHealth = clamp((NetMargin / 40) * 100, 0, 100)
     */
    private double computeMarginHealth(List<Sale> sales, List<Expense> expenses) {
        double totalRev = sales != null ? sales.stream().mapToDouble(Sale::getAmount).sum() : 0.0;
        double totalExp = expenses != null ? expenses.stream().mapToDouble(Expense::getAmount).sum() : 0.0;

        if (totalRev <= 0) return 0.0;

        double profit = totalRev - totalExp;
        double netMargin = (profit / totalRev) * 100.0;

        double score = (netMargin / 40.0) * 100.0;
        return Math.max(0.0, Math.min(100.0, score));
    }

    /**
     * Section 31: Inventory Balance (20%)
     * Healthy SKUs have 7 - 45 days of inventory runway.
     * Score = (Healthy SKUs / Total SKUs) * 100
     */
    private double computeInventoryBalance(List<InventoryMetric> metrics) {
        if (metrics == null || metrics.isEmpty()) return 75.0;

        long total = metrics.size();
        long healthyCount = metrics.stream()
                .filter(m -> m.stockRunwayDays() >= 7.0 && m.stockRunwayDays() <= 45.0)
                .count();

        // Also credit items in optimal health status
        long optimalCount = metrics.stream()
                .filter(m -> m.status() == InventoryStatus.HEALTHY)
                .count();

        long effectiveHealthy = Math.max(healthyCount, optimalCount);
        double score = ((double) effectiveHealthy / total) * 100.0;
        return Math.max(0.0, Math.min(100.0, score));
    }

    /**
     * Section 32: Customer Health (15%)
     * Active customer = purchased within last 60 days (recency <= 60).
     * Score = (Active Customers / Total Customers) * 100
     */
    private double computeCustomerHealth(List<RfmResult> rfmResults) {
        if (rfmResults == null || rfmResults.isEmpty()) return 60.0;

        long total = rfmResults.size();
        long activeCount = rfmResults.stream()
                .filter(r -> r.recencyDays() <= 60)
                .count();

        double score = ((double) activeCount / total) * 100.0;
        return Math.max(0.0, Math.min(100.0, score));
    }

    /**
     * Section 33: Expense Efficiency (15%)
     * OER = (Operating Expenses / Revenue) * 100
     * OER <= 30% -> 100
     * OER >= 60% -> 0
     * Between: 100 - ((OER - 30) / 30 * 100)
     */
    private double computeExpenseEfficiency(List<Sale> sales, List<Expense> expenses) {
        double totalRev = sales != null ? sales.stream().mapToDouble(Sale::getAmount).sum() : 0.0;
        double totalExp = expenses != null ? expenses.stream().mapToDouble(Expense::getAmount).sum() : 0.0;

        if (totalRev <= 0) return 20.0;

        double oer = (totalExp / totalRev) * 100.0;

        if (oer <= 30.0) return 100.0;
        if (oer >= 60.0) return 0.0;

        double score = 100.0 - (((oer - 30.0) / 30.0) * 100.0);
        return Math.max(0.0, Math.min(100.0, score));
    }

    private static double round1(double val) {
        return Math.round(val * 10.0) / 10.0;
    }
}
