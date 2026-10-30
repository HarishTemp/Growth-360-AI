package com.growth360;

import com.growth360.service.AnalyticsService;
import com.growth360.service.AnalyticsService.AnalyticsSnapshot;
import com.growth360.util.DataSeeder;
import com.growth360.util.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HeadlessBootstrapTest {

    @Test
    @DisplayName("End-to-End System Bootstrap: Schema creation, 6-month data seeding, and analytics computation")
    void testFullBootstrapPipeline() throws Exception {
        // 1. DDL Schema initialization
        Schema.initSchema();

        // 2. Realistic 6-month seed data generation
        DataSeeder.seedIfEmpty();

        // 3. Complete analytical pipeline execution
        AnalyticsService analyticsService = new AnalyticsService();
        AnalyticsSnapshot snapshot = analyticsService.computeSnapshot();

        assertNotNull(snapshot, "Analytical snapshot must not be null");
        assertTrue(snapshot.totalRevenue() > 0, "Total revenue should be greater than 0");
        assertTrue(snapshot.totalExpenses() > 0, "Total expenses should be greater than 0");
        assertTrue(snapshot.growthScore().overallScore() > 0, "Growth score should be computed");
        assertFalse(snapshot.inventoryMetrics().isEmpty(), "Inventory metrics should not be empty");
        assertFalse(snapshot.rfmResults().isEmpty(), "RFM results should not be empty");
        assertFalse(snapshot.forecastPoints().isEmpty(), "Forecast points should be generated");
        assertFalse(snapshot.alerts().isEmpty(), "Risk alerts should be detected");
        // 4. Section 41: MetricContext verification (Copilot Engine payload)
        var metricContext = analyticsService.computeMetricContext();
        assertNotNull(metricContext, "MetricContext must be non-null");
        assertTrue(metricContext.totalRevenue() > 0, "MetricContext revenue must be positive");
        assertNotNull(metricContext.growthScore(), "MetricContext GrowthScore must exist");
        assertNotNull(metricContext.topProduct(), "Top product must be determined");

        System.out.println("[Bootstrap Test] Total Revenue: ₹" + snapshot.totalRevenue());
        System.out.println("[Bootstrap Test] Total Expenses: ₹" + snapshot.totalExpenses());
        System.out.println("[Bootstrap Test] Net Profit: ₹" + snapshot.netProfit());
        System.out.println("[Bootstrap Test] Growth Score: " + snapshot.growthScore().overallScore() + " (" + snapshot.growthScore().healthBand() + ")");
        System.out.println("[Bootstrap Test] MetricContext Top SKU: " + metricContext.topProduct());
        System.out.println("[Bootstrap Test] Active Alerts (PriorityQueue): " + snapshot.alerts().size());
        System.out.println("[Bootstrap Test] Prescriptive Recommendations: " + snapshot.recommendations().size());
    }
}
