package com.growth360.analytics;

import com.growth360.model.Alert;
import com.growth360.model.Alert.AlertType;
import com.growth360.model.Alert.Severity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pure Java statistical anomaly detector using Z-Score.
 * Z = (X - μ) / σ
 * Flags anomalies:
 *   |Z| > 3.0 -> CRITICAL
 *   |Z| > 2.0 -> WARNING
 */
public class ZScoreDetector {

    public record AnomalyPoint(String label, double value, double zScore, Severity severity) {}

    /**
     * Computes Z-Score anomalies for a series of values with associated labels.
     */
    public List<AnomalyPoint> detectAnomalies(Map<String, Double> labeledValues) {
        List<AnomalyPoint> anomalies = new ArrayList<>();
        if (labeledValues == null || labeledValues.size() < 3) {
            return anomalies; // Need at least 3 points for meaningful deviation
        }

        List<Double> values = new ArrayList<>(labeledValues.values());
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .sum() / (values.size() - 1);
        double stdDev = Math.sqrt(variance);

        if (stdDev == 0.0) {
            return anomalies; // No variance, no anomalies
        }

        labeledValues.forEach((label, val) -> {
            double z = (val - mean) / stdDev;
            double absZ = Math.abs(z);
            if (absZ >= 3.0) {
                anomalies.add(new AnomalyPoint(label, val, z, Severity.CRITICAL));
            } else if (absZ >= 2.0) {
                anomalies.add(new AnomalyPoint(label, val, z, Severity.WARNING));
            }
        });

        return anomalies;
    }

    /**
     * Converts detected anomalies into business Alert records.
     */
    public List<Alert> createExpenseAlerts(Map<String, Double> expensesByCategory) {
        List<Alert> alerts = new ArrayList<>();
        List<AnomalyPoint> anomalies = detectAnomalies(expensesByCategory);

        for (AnomalyPoint pt : anomalies) {
            if (pt.zScore() > 0) { // Expense spike
                String title = "Expense Spike: " + pt.label();
                String desc = String.format("Category '%s' spent $%.2f which is %.1f standard deviations above the mean (Z=%.2f).",
                        pt.label(), pt.value(), pt.zScore(), pt.zScore());
                alerts.add(new Alert(
                        "EXP-Z-" + System.identityHashCode(pt),
                        AlertType.EXPENSE,
                        pt.severity(),
                        title,
                        desc,
                        pt.value(),
                        LocalDateTime.now()
                ));
            }
        }
        return alerts;
    }

    public List<Alert> createRevenueAlerts(Map<String, Double> dailyRevenue) {
        List<Alert> alerts = new ArrayList<>();
        List<AnomalyPoint> anomalies = detectAnomalies(dailyRevenue);

        for (AnomalyPoint pt : anomalies) {
            if (pt.zScore() < -2.0) { // Severe revenue drop
                String title = "Severe Revenue Dip: " + pt.label();
                String desc = String.format("Period '%s' revenue of $%.2f dropped %.1f standard deviations below normal (Z=%.2f).",
                        pt.label(), pt.value(), Math.abs(pt.zScore()), pt.zScore());
                alerts.add(new Alert(
                        "REV-Z-" + System.identityHashCode(pt),
                        AlertType.REVENUE,
                        pt.severity(),
                        title,
                        desc,
                        pt.value(),
                        LocalDateTime.now()
                ));
            }
        }
        return alerts;
    }
}
