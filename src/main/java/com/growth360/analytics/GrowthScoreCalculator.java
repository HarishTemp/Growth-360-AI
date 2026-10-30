package com.growth360.analytics;

import com.growth360.analytics.ReorderPointCalculator.InventoryMetric;
import com.growth360.analytics.ReorderPointCalculator.InventoryStatus;
import com.growth360.model.Expense;
import com.growth360.model.RfmResult;
import com.growth360.model.RfmResult.Segment;
import com.growth360.model.Sale;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Calculates the holistic Business Growth Score (0 - 100) combining Revenue Trend,
 * Expense Control, Inventory Health, and Customer Health.
 */
public class GrowthScoreCalculator {

    public static final double WEIGHT_REVENUE = 0.30;
    public static final double WEIGHT_EXPENSE = 0.25;
    public static final double WEIGHT_INVENTORY = 0.25;
    public static final double WEIGHT_CUSTOMER = 0.20;

    public record GrowthScoreResult(
            int overallScore,
            String healthBand,
            String healthColorHex,
            double revenueScore,
            double expenseScore,
            double inventoryScore,
            double customerScore,
            String summaryInsight
    ) {}

    public GrowthScoreResult calculateScore(
            List<Sale> sales,
            List<Expense> expenses,
            List<InventoryMetric> inventoryMetrics,
            List<RfmResult> rfmResults) {

        double revScore = computeRevenueScore(sales);
        double expScore = computeExpenseScore(sales, expenses);
        double invScore = computeInventoryScore(inventoryMetrics);
        double custScore = computeCustomerScore(rfmResults);

        double totalWeighted = (revScore * WEIGHT_REVENUE) +
                               (expScore * WEIGHT_EXPENSE) +
                               (invScore * WEIGHT_INVENTORY) +
                               (custScore * WEIGHT_CUSTOMER);

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
                Math.round(revScore * 10.0) / 10.0,
                Math.round(expScore * 10.0) / 10.0,
                Math.round(invScore * 10.0) / 10.0,
                Math.round(custScore * 10.0) / 10.0,
                insight
        );
    }

    private double computeRevenueScore(List<Sale> sales) {
        if (sales == null || sales.isEmpty()) return 50.0;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midPoint = now.minusDays(30);
        LocalDateTime startPoint = now.minusDays(60);

        double recentRevenue = sales.stream()
                .filter(s -> s.getSaleDate().isAfter(midPoint))
                .mapToDouble(Sale::getAmount)
                .sum();

        double prevRevenue = sales.stream()
                .filter(s -> s.getSaleDate().isAfter(startPoint) && s.getSaleDate().isBefore(midPoint))
                .mapToDouble(Sale::getAmount)
                .sum();

        if (prevRevenue <= 0) return 70.0;

        double growthRatio = recentRevenue / prevRevenue;
        // growthRatio = 1.0 -> 70 points, 1.25 -> 88 points, 0.75 -> 50 points
        double score = 70.0 + ((growthRatio - 1.0) * 80.0);
        return Math.max(10.0, Math.min(100.0, score));
    }

    private double computeExpenseScore(List<Sale> sales, List<Expense> expenses) {
        double totalRev = sales != null ? sales.stream().mapToDouble(Sale::getAmount).sum() : 0.0;
        double totalExp = expenses != null ? expenses.stream().mapToDouble(Expense::getAmount).sum() : 0.0;

        if (totalRev <= 0) return 40.0;

        double expenseRatio = totalExp / totalRev;
        // Healthy expense ratio is 40%-70% of revenue
        if (expenseRatio <= 0.50) return 95.0;
        if (expenseRatio <= 0.65) return 85.0;
        if (expenseRatio <= 0.80) return 70.0;
        if (expenseRatio <= 0.95) return 50.0;
        return 30.0;
    }

    private double computeInventoryScore(List<InventoryMetric> metrics) {
        if (metrics == null || metrics.isEmpty()) return 80.0;

        long total = metrics.size();
        long stockouts = metrics.stream().filter(m -> m.status() == InventoryStatus.OUT_OF_STOCK).count();
        long critical = metrics.stream().filter(m -> m.status() == InventoryStatus.CRITICAL_REORDER).count();
        long healthy = metrics.stream().filter(m -> m.status() == InventoryStatus.HEALTHY).count();

        double healthyRatio = (double) healthy / total;
        double penalty = (stockouts * 25.0 + critical * 15.0) / total;

        double score = (healthyRatio * 100.0) - penalty;
        return Math.max(15.0, Math.min(100.0, score));
    }

    private double computeCustomerScore(List<RfmResult> rfmResults) {
        if (rfmResults == null || rfmResults.isEmpty()) return 70.0;

        long total = rfmResults.size();
        long champions = rfmResults.stream().filter(r -> r.segment() == Segment.CHAMPIONS).count();
        long loyal = rfmResults.stream().filter(r -> r.segment() == Segment.LOYAL).count();
        long atRisk = rfmResults.stream().filter(r -> r.segment() == Segment.AT_RISK).count();
        long lost = rfmResults.stream().filter(r -> r.segment() == Segment.LOST).count();

        double positiveShare = (double) (champions + loyal) / total;
        double negativeShare = (double) (atRisk + lost) / total;

        double score = 50.0 + (positiveShare * 50.0) - (negativeShare * 30.0);
        return Math.max(20.0, Math.min(100.0, score));
    }
}
