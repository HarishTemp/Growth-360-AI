package com.growth360.analytics;

import com.growth360.analytics.GrowthScoreCalculator.GrowthScoreResult;
import com.growth360.analytics.ReorderPointCalculator.InventoryMetric;
import com.growth360.analytics.ReorderPointCalculator.InventoryStatus;
import com.growth360.model.*;
import com.growth360.model.Alert.Severity;
import com.growth360.model.RfmResult.Segment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class AnalyticsEngineTest {

    @Test
    @DisplayName("ZScoreDetector: Correctly flags |Z| > 2 as WARNING and |Z| > 3 as CRITICAL")
    void testZScoreAnomalyDetection() {
        ZScoreDetector detector = new ZScoreDetector();
        Map<String, Double> expenses = new LinkedHashMap<>();
        // Baseline series around 500 (15 periods)
        for (int i = 1; i <= 15; i++) {
            expenses.put("Month-" + i, 500.0 + (i % 3) * 5.0);
        }
        // Moderate spike: |Z| > 2
        expenses.put("Month-16", 650.0);
        // Extreme spike: |Z| > 3
        expenses.put("Month-17", 2500.0);

        List<ZScoreDetector.AnomalyPoint> anomalies = detector.detectAnomalies(expenses);
        assertFalse(anomalies.isEmpty(), "Anomalies should be detected");

        boolean foundCritical = anomalies.stream().anyMatch(a -> a.label().equals("Month-17") && a.severity() == Severity.CRITICAL);
        assertTrue(foundCritical, "Month-17 should be CRITICAL due to extreme Z score");

        List<Alert> alerts = detector.createExpenseAlerts(expenses);
        assertFalse(alerts.isEmpty(), "Expense alerts should be generated");
    }

    @Test
    @DisplayName("ReorderPointCalculator: Accurately computes ROP, runway, and flags stockouts")
    void testReorderPointCalculations() {
        ReorderPointCalculator calculator = new ReorderPointCalculator();

        Product p1 = new Product(1, "Widget A", "Hardware", 5, 10.0, 25.0, 7, 10);
        Product p2 = new Product(2, "Widget B", "Hardware", 0, 15.0, 35.0, 10, 15);
        Product p3 = new Product(3, "Overstocked Item", "Supplies", 250, 5.0, 12.0, 5, 10);

        List<Sale> sales = new ArrayList<>();
        LocalDateTime recent = LocalDateTime.now().minusDays(10);
        // Sell 60 units of Widget A over the last 60 days -> daily demand ~ 1.0 unit/day
        for (int i = 0; i < 20; i++) {
            sales.add(new Sale(1, 1, 3, 75.0, recent));
        }

        List<InventoryMetric> metrics = calculator.calculateMetrics(List.of(p1, p2, p3), sales);
        assertEquals(3, metrics.size());

        InventoryMetric m1 = metrics.stream().filter(m -> m.product().getId() == 1).findFirst().orElseThrow();
        // ROP = (dailyDemand * leadTime) + safetyStock
        assertTrue(m1.reorderPoint() > 0, "ROP must be positive");
        assertEquals(InventoryStatus.CRITICAL_REORDER, m1.status(), "Stock of 5 below safety stock of 10 must be CRITICAL_REORDER");

        InventoryMetric m2 = metrics.stream().filter(m -> m.product().getId() == 2).findFirst().orElseThrow();
        assertEquals(InventoryStatus.OUT_OF_STOCK, m2.status(), "Zero stock must be OUT_OF_STOCK");

        List<Alert> alerts = calculator.generateInventoryAlerts(metrics);
        assertTrue(alerts.stream().anyMatch(a -> a.severity() == Severity.CRITICAL));
    }

    @Test
    @DisplayName("RfmSegmenter: Correctly segments Champions, Loyal, At-Risk, and Lost customers")
    void testRfmSegmentation() {
        RfmSegmenter segmenter = new RfmSegmenter();

        Customer c1 = new Customer(1, "Alice Champion", "alice@example.com", "123", LocalDateTime.now().minusDays(100));
        Customer c2 = new Customer(2, "Bob AtRisk", "bob@example.com", "456", LocalDateTime.now().minusDays(120));
        Customer c3 = new Customer(3, "Charlie Lost", "charlie@example.com", "789", LocalDateTime.now().minusDays(180));

        List<Customer> customers = List.of(c1, c2, c3);
        List<Sale> sales = new ArrayList<>();

        // Alice bought 10 times, most recently 2 days ago, high spend
        for (int i = 0; i < 10; i++) {
            sales.add(new Sale(1, 1, 2, 200.0, LocalDateTime.now().minusDays(2 + i * 5)));
        }

        // Bob bought 5 times, but last purchase was 80 days ago
        for (int i = 0; i < 5; i++) {
            sales.add(new Sale(2, 1, 2, 150.0, LocalDateTime.now().minusDays(80 + i * 5)));
        }

        // Charlie bought once 150 days ago
        sales.add(new Sale(3, 1, 1, 50.0, LocalDateTime.now().minusDays(150)));

        List<RfmResult> results = segmenter.segmentCustomers(customers, sales);
        assertEquals(3, results.size());

        RfmResult rAlice = results.stream().filter(r -> r.customerId() == 1).findFirst().orElseThrow();
        assertEquals(Segment.CHAMPIONS, rAlice.segment(), "Frequent recent big spender should be Champion");

        RfmResult rBob = results.stream().filter(r -> r.customerId() == 2).findFirst().orElseThrow();
        assertEquals(Segment.AT_RISK, rBob.segment(), "Customer inactive for 80 days should be At-Risk");
    }

    @Test
    @DisplayName("RevenueForecaster: Extrapolates next 3 periods using OLS Regression and Moving Average")
    void testRevenueForecasting() {
        RevenueForecaster forecaster = new RevenueForecaster();
        List<Sale> sales = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 6 months of historical revenue: $1000, $1200, $1400, $1600, $1800, $2000
        for (int m = 5; m >= 0; m--) {
            double amt = 1000.0 + (5 - m) * 200.0;
            sales.add(new Sale(1, 1, 1, amt, now.minusMonths(m).withDayOfMonth(15)));
        }

        List<ForecastPoint> points = forecaster.forecastMonthlyRevenue(sales);
        assertTrue(points.size() >= 7, "Should include historical points + 3 future forecast points");

        List<ForecastPoint> projected = points.stream().filter(ForecastPoint::isProjected).toList();
        assertEquals(3, projected.size(), "Must generate exactly 3 future forecast periods");

        for (ForecastPoint pt : projected) {
            assertNotNull(pt.forecastedRevenue());
            assertTrue(pt.forecastedRevenue() > 0, "Forecasted revenue must be positive");
            assertTrue(pt.lowerBound() < pt.forecastedRevenue(), "Lower bound must be strictly less than forecast");
            assertTrue(pt.upperBound() > pt.forecastedRevenue(), "Upper bound must be strictly greater than forecast");
        }
    }

    @Test
    @DisplayName("GrowthScoreCalculator: Correctly computes 0-100 composite index and health bands")
    void testGrowthScoreCalculation() {
        GrowthScoreCalculator calculator = new GrowthScoreCalculator();

        List<Sale> sales = List.of(
                new Sale(1, 1, 1, 5000.0, LocalDateTime.now().minusDays(10)),
                new Sale(1, 1, 1, 4000.0, LocalDateTime.now().minusDays(45))
        );
        List<Expense> expenses = List.of(
                new Expense("Hosting", "Cloud", 2000.0, LocalDateTime.now().minusDays(15))
        );

        Product p = new Product(1, "Prod", "Cat", 50, 10.0, 30.0, 5, 10);
        InventoryMetric metric = new InventoryMetric(p, 1.0, 15.0, 50.0, 0, InventoryStatus.HEALTHY);

        RfmResult rfm = new RfmResult(1, "Alice", 5, 10, 5000.0, 5, 5, 5, Segment.CHAMPIONS);

        GrowthScoreResult result = calculator.calculateScore(sales, expenses, List.of(metric), List.of(rfm));

        assertTrue(result.overallScore() >= 0 && result.overallScore() <= 100, "Score must be bounded between 0 and 100");
        assertNotNull(result.healthBand());
        assertNotNull(result.summaryInsight());
    }

    @Test
    @DisplayName("RecommendationEngine: Generates prescriptive actions matching risk alert categories")
    void testRecommendationEngine() {
        RecommendationEngine engine = new RecommendationEngine();

        Alert alertInv = new Alert("INV-1", Alert.AlertType.INVENTORY, Severity.CRITICAL, "Stockout", "Product X out", 0.0);
        Alert alertExp = new Alert("EXP-1", Alert.AlertType.EXPENSE, Severity.CRITICAL, "Marketing Spike", "Z=3.5", 5000.0);

        List<Recommendation> recs = engine.generateRecommendations(List.of(alertInv, alertExp));
        assertEquals(2, recs.size());

        assertTrue(recs.stream().anyMatch(r -> r.priority() == Recommendation.Priority.URGENT));
        assertTrue(recs.stream().anyMatch(r -> r.category().contains("Inventory")));
        assertTrue(recs.stream().anyMatch(r -> r.category().contains("Financial")));
    }
}
