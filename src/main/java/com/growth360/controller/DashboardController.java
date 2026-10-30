package com.growth360.controller;

import com.growth360.service.AnalyticsService;
import com.growth360.service.AnalyticsService.AnalyticsSnapshot;
import com.growth360.view.DashboardView;

import java.util.function.Consumer;

public class DashboardController {

    private final AnalyticsService analyticsService;
    private DashboardView view;

    public DashboardController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    public void setView(DashboardView view) {
        this.view = view;
    }

    public void loadDashboardData() {
        if (view != null) {
            view.showLoading(true);
        }
        analyticsService.computeSnapshotAsync(
                snapshot -> {
                    if (view != null) {
                        view.updateData(snapshot);
                        view.showLoading(false);
                    }
                },
                error -> {
                    if (view != null) {
                        view.showError("Failed to compute dashboard analytics: " + error.getMessage());
                        view.showLoading(false);
                    }
                }
        );
    }
}
