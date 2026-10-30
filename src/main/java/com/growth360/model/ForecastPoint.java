package com.growth360.model;

/**
 * Record representing historical or forecasted revenue at a given time period.
 */
public record ForecastPoint(
        String periodLabel,
        Double actualRevenue,
        Double forecastedRevenue,
        Double lowerBound,
        Double upperBound,
        boolean isProjected
) {
    public ForecastPoint(String periodLabel, Double actualRevenue) {
        this(periodLabel, actualRevenue, null, null, null, false);
    }

    public ForecastPoint(String periodLabel, Double forecastedRevenue, Double lowerBound, Double upperBound) {
        this(periodLabel, null, forecastedRevenue, lowerBound, upperBound, true);
    }
}
