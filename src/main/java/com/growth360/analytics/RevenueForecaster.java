package com.growth360.analytics;

import com.growth360.model.ForecastPoint;
import com.growth360.model.Sale;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Historical trend estimation and forecasting engine using Ordinary Least Squares (OLS) Linear Regression
 * combined with Moving Average smoothing.
 */
public class RevenueForecaster {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");

    /**
     * Generates a combined series of historical monthly actuals and the next 3 projected forecast points.
     */
    public List<ForecastPoint> forecastMonthlyRevenue(List<Sale> sales) {
        List<ForecastPoint> result = new ArrayList<>();

        if (sales == null || sales.isEmpty()) {
            return result;
        }

        // Aggregate historical revenue by YearMonth
        Map<YearMonth, Double> monthlyTotals = sales.stream()
                .collect(Collectors.groupingBy(
                        s -> YearMonth.from(s.getSaleDate()),
                        TreeMap::new,
                        Collectors.summingDouble(Sale::getAmount)
                ));

        List<YearMonth> months = new ArrayList<>(monthlyTotals.keySet());
        List<Double> actuals = new ArrayList<>();

        for (YearMonth ym : months) {
            double amount = monthlyTotals.get(ym);
            actuals.add(amount);
            result.add(new ForecastPoint(ym.format(MONTH_FORMATTER), amount));
        }

        int n = actuals.size();
        if (n == 0) return result;

        // If very few points, extrapolate reasonably
        if (n < 2) {
            double lastVal = actuals.get(0);
            YearMonth nextMonth = months.get(0);
            for (int i = 1; i <= 3; i++) {
                nextMonth = nextMonth.plusMonths(1);
                result.add(new ForecastPoint(
                        nextMonth.format(MONTH_FORMATTER),
                        lastVal,
                        lastVal * 0.9,
                        lastVal * 1.1
                ));
            }
            return result;
        }

        // 1. Ordinary Least Squares Linear Regression: y = m*x + b
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i + 1;
            double y = actuals.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double denominator = (n * sumX2 - sumX * sumX);
        double slope = denominator != 0 ? (n * sumXY - sumX * sumY) / denominator : 0.0;
        double intercept = (sumY - slope * sumX) / n;

        // 2. 3-Period Moving Average for smoothing
        int window = Math.min(3, n);
        double movingAvg = 0;
        for (int i = n - window; i < n; i++) {
            movingAvg += actuals.get(i);
        }
        movingAvg /= window;

        // 3. Project next 3 months (blend 60% regression trend + 40% moving average)
        YearMonth lastMonth = months.get(n - 1);
        for (int step = 1; step <= 3; step++) {
            YearMonth futureMonth = lastMonth.plusMonths(step);
            double futureX = n + step;
            double trendProj = intercept + (slope * futureX);

            // Guard against negative forecast
            double projectedRevenue = Math.max(0.0, (0.65 * trendProj) + (0.35 * movingAvg));

            // Confidence interval ±12%
            double lowerBound = Math.max(0.0, projectedRevenue * 0.88);
            double upperBound = projectedRevenue * 1.12;

            result.add(new ForecastPoint(
                    futureMonth.format(MONTH_FORMATTER) + " (Est)",
                    Math.round(projectedRevenue * 100.0) / 100.0,
                    Math.round(lowerBound * 100.0) / 100.0,
                    Math.round(upperBound * 100.0) / 100.0
            ));
        }

        return result;
    }
}
