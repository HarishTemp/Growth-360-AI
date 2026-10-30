package com.growth360.model;

/**
 * Immutable Java Record representing the holistic Business Growth Score (0 - 100)
 * evaluated across the 5 core strategic dimensions:
 * 1. Sales Velocity (25%)
 * 2. Net Margin Health (25%)
 * 3. Inventory Balance (20%)
 * 4. Customer Health (15%)
 * 5. Expense Efficiency (15%)
 */
public record GrowthScore(
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
    // Backward compatibility helpers for existing views
    public double revenueScore() { return salesVelocity; }
    public double expenseScore() { return expenseEfficiency; }
    public double inventoryScore() { return inventoryBalance; }
    public double customerScore() { return customerHealth; }
}
