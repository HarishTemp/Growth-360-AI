package com.growth360.model;

import com.growth360.analytics.ReorderPointCalculator.InventoryMetric;

import java.util.List;

/**
 * Immutable Java Record providing a consolidated business snapshot
 * used for executive KPI feeds, prescriptive decision intelligence,
 * and the AI Copilot engine payload.
 */
public record MetricContext(
        String currentPeriod,
        double totalRevenue,
        double momRevenueGrowth,
        double totalExpenses,
        double netProfit,
        double profitMargin,
        GrowthScore growthScore,
        List<InventoryMetric> inventoryRisks,
        String topProduct,
        int activeCustomerCount,
        int atRiskCustomerCount,
        List<Alert> activeAlerts,
        List<Recommendation> recommendations
) {}
