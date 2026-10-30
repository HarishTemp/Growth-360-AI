package com.growth360.controller;

import com.growth360.model.Alert;
import com.growth360.model.Recommendation;
import com.growth360.service.AnalyticsService;
import com.growth360.view.AlertsView;

import java.util.ArrayList;
import java.util.List;

public class AlertController {

    private final AnalyticsService analyticsService;
    private AlertsView view;
    private List<Alert> cachedAlerts = new ArrayList<>();
    private List<Recommendation> cachedRecommendations = new ArrayList<>();

    public AlertController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    public void setView(AlertsView view) {
        this.view = view;
    }

    public void loadAlerts() {
        if (view != null) view.showLoading(true);
        analyticsService.computeSnapshotAsync(
                snapshot -> {
                    this.cachedAlerts = new ArrayList<>(snapshot.alerts());
                    this.cachedRecommendations = new ArrayList<>(snapshot.recommendations());
                    if (view != null) {
                        view.setAlertsData(cachedAlerts, cachedRecommendations);
                        view.showLoading(false);
                    }
                },
                error -> {
                    if (view != null) {
                        view.showError("Failed to evaluate risk alerts: " + error.getMessage());
                        view.showLoading(false);
                    }
                }
        );
    }

    public void resolveAlert(String alertId) {
        cachedAlerts.removeIf(a -> a.id().equals(alertId));
        cachedRecommendations.removeIf(r -> r.alertId().equals(alertId));
        if (view != null) {
            view.setAlertsData(cachedAlerts, cachedRecommendations);
            view.showSuccess("Risk alert marked as resolved.");
        }
    }
}
