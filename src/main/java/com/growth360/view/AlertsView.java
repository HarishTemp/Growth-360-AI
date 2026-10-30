package com.growth360.view;

import com.growth360.controller.AlertController;
import com.growth360.model.Alert;
import com.growth360.model.Alert.AlertType;
import com.growth360.model.Alert.Severity;
import com.growth360.model.Recommendation;
import com.growth360.util.Theme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pure Java JavaFX Risk Alerts view presenting severity-ranked anomalies and prescriptive recommendations.
 */
public class AlertsView extends ScrollPane {

    private final AlertController controller;
    private final List<Alert> masterAlerts = new ArrayList<>();
    private final List<Recommendation> masterRecommendations = new ArrayList<>();

    private VBox mainContainer;
    private VBox cardsContainer;
    private StackPane loadingOverlay;
    private Label notificationBanner;

    private Label lblTotalAlerts;
    private Label lblCriticalCount;
    private Label lblWarningCount;
    private Label lblRecCount;

    private ComboBox<String> severityFilter;
    private ComboBox<String> typeFilter;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    public AlertsView(AlertController controller) {
        this.controller = controller;
        this.controller.setView(this);

        buildUI();
        this.controller.loadAlerts();
    }

    private void buildUI() {
        setFitToWidth(true);
        setStyle("-fx-background: " + Theme.COLOR_BG_APP + "; -fx-background-color: transparent;");

        mainContainer = new VBox(24);
        mainContainer.setPadding(new Insets(24, 28, 40, 28));
        mainContainer.setStyle("-fx-background-color: " + Theme.COLOR_BG_APP + ";");

        // Header
        VBox headerBox = new VBox(4);
        Label title = new Label("Titan Horology Risk Alerts & Prescriptive Decisions");
        title.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label subtitle = new Label("Festive ad campaign Z-Score spikes, Nebula stockouts, and prescriptive horology supply actions");
        subtitle.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 13px;");
        headerBox.getChildren().addAll(title, subtitle);

        // Notification Banner
        notificationBanner = new Label();
        notificationBanner.setVisible(false);
        notificationBanner.setManaged(false);

        // Stats Row
        HBox statsRow = new HBox(16);
        lblTotalAlerts = new Label("0");
        VBox card1 = createStatCard("TOTAL ALERTS DETECTED", lblTotalAlerts, "#38BDF8");

        lblCriticalCount = new Label("0");
        VBox card2 = createStatCard("CRITICAL ANOMALIES", lblCriticalCount, "#EF4444");

        lblWarningCount = new Label("0");
        VBox card3 = createStatCard("OPERATIONAL WARNINGS", lblWarningCount, "#F59E0B");

        lblRecCount = new Label("0");
        VBox card4 = createStatCard("ATTACHED ACTION PLANS", lblRecCount, "#10B981");

        statsRow.getChildren().addAll(card1, card2, card3, card4);
        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);
        HBox.setHgrow(card4, Priority.ALWAYS);

        // Filter Toolbar
        HBox toolbar = new HBox(14);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        severityFilter = new ComboBox<>();
        severityFilter.getItems().addAll("All Severities", "CRITICAL", "WARNING", "INFO");
        severityFilter.setValue("All Severities");
        severityFilter.setStyle(Theme.TEXT_INPUT);
        severityFilter.setOnAction(e -> renderCards());

        typeFilter = new ComboBox<>();
        typeFilter.getItems().addAll("All Categories", "INVENTORY", "EXPENSE", "REVENUE", "CUSTOMER");
        typeFilter.setValue("All Categories");
        typeFilter.setStyle(Theme.TEXT_INPUT);
        typeFilter.setOnAction(e -> renderCards());

        toolbar.getChildren().addAll(new Label("Filter by:"), severityFilter, typeFilter);

        // Cards list
        cardsContainer = new VBox(16);

        mainContainer.getChildren().addAll(headerBox, notificationBanner, statsRow, toolbar, cardsContainer);

        // Loading overlay
        loadingOverlay = new StackPane();
        loadingOverlay.setStyle("-fx-background-color: rgba(10, 14, 23, 0.65);");
        ProgressIndicator pi = new ProgressIndicator();
        pi.setMaxSize(48, 48);
        loadingOverlay.getChildren().add(pi);
        loadingOverlay.setVisible(false);

        StackPane rootWrapper = new StackPane(mainContainer, loadingOverlay);
        setContent(rootWrapper);
    }

    private VBox createStatCard(String title, Label valLabel, String colorHex) {
        VBox card = new VBox(6);
        card.setStyle(Theme.STAT_CARD_STYLE);

        Label t = new Label(title);
        t.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        valLabel.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 22px; -fx-font-weight: bold;");

        card.getChildren().addAll(t, valLabel);
        return card;
    }

    public void setAlertsData(List<Alert> alerts, List<Recommendation> recommendations) {
        masterAlerts.clear();
        masterAlerts.addAll(alerts);

        masterRecommendations.clear();
        masterRecommendations.addAll(recommendations);

        long crit = masterAlerts.stream().filter(a -> a.severity() == Severity.CRITICAL).count();
        long warn = masterAlerts.stream().filter(a -> a.severity() == Severity.WARNING).count();

        lblTotalAlerts.setText(masterAlerts.size() + " Active");
        lblCriticalCount.setText(crit + " Critical");
        lblWarningCount.setText(warn + " Warnings");
        lblRecCount.setText(masterRecommendations.size() + " Available");

        renderCards();
    }

    private void renderCards() {
        cardsContainer.getChildren().clear();

        String sevSelected = severityFilter.getValue();
        String typeSelected = typeFilter.getValue();

        List<Alert> filtered = masterAlerts.stream()
                .filter(a -> "All Severities".equals(sevSelected) || a.severity().name().equalsIgnoreCase(sevSelected))
                .filter(a -> "All Categories".equals(typeSelected) || a.type().name().equalsIgnoreCase(typeSelected))
                .toList();

        if (filtered.isEmpty()) {
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40));
            emptyBox.setStyle(Theme.CARD_STYLE);

            Label emptyLabel = new Label("✓ No Active Risk Alerts in Selected Filter");
            emptyLabel.setStyle("-fx-text-fill: " + Theme.COLOR_SUCCESS + "; -fx-font-size: 16px; -fx-font-weight: bold;");

            Label emptySub = new Label("All operational metrics and statistical variances are within normal thresholds.");
            emptySub.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 13px;");

            emptyBox.getChildren().addAll(emptyLabel, emptySub);
            cardsContainer.getChildren().add(emptyBox);
            return;
        }

        Map<String, Recommendation> recMap = masterRecommendations.stream()
                .collect(Collectors.toMap(Recommendation::alertId, r -> r, (r1, r2) -> r1));

        for (Alert alert : filtered) {
            cardsContainer.getChildren().add(createAlertCard(alert, recMap.get(alert.id())));
        }
    }

    private VBox createAlertCard(Alert alert, Recommendation rec) {
        VBox card = new VBox(12);
        String borderAccent = alert.severity() == Severity.CRITICAL ? "#EF4444" :
                              alert.severity() == Severity.WARNING ? "#F59E0B" : "#0284C7";

        card.setStyle(Theme.CARD_STYLE + " -fx-border-color: " + borderAccent + "; -fx-border-width: 1px 1px 1px 4px;");

        // Top Row: Badges and timestamp
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        String badgeBg = alert.severity() == Severity.CRITICAL ? "#DC2626" :
                         alert.severity() == Severity.WARNING ? "#D97706" : "#0284C7";
        Label sevBadge = new Label(alert.severity().name());
        sevBadge.setStyle(Theme.getBadgeStyle(badgeBg, "#FFFFFF"));

        Label typeBadge = new Label(alert.type().name() + " ANOMALY");
        typeBadge.setStyle(Theme.getBadgeStyle("rgba(56, 189, 248, 0.15)", Theme.COLOR_ACCENT_PRIMARY));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label timeLabel = new Label(alert.timestamp().format(TIME_FMT));
        timeLabel.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 11px;");

        Button btnResolve = new Button("Mark Resolved ✓");
        btnResolve.setStyle(Theme.BTN_SECONDARY + " -fx-font-size: 11px; -fx-padding: 4 10 4 10;");
        btnResolve.setOnAction(e -> controller.resolveAlert(alert.id()));

        topRow.getChildren().addAll(sevBadge, typeBadge, timeLabel, spacer, btnResolve);

        // Alert Title & Description
        Label titleLabel = new Label(alert.title());
        titleLabel.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label descLabel = new Label(alert.description());
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: " + Theme.TEXT_SECONDARY + "; -fx-font-size: 13px;");

        card.getChildren().addAll(topRow, titleLabel, descLabel);

        // Attached Recommendation box if available
        if (rec != null) {
            VBox recBox = new VBox(6);
            recBox.setStyle("-fx-background-color: rgba(14, 165, 233, 0.08); " +
                    "-fx-background-radius: 8px; " +
                    "-fx-border-color: rgba(14, 165, 233, 0.3); " +
                    "-fx-border-radius: 8px; " +
                    "-fx-padding: 12px;");

            HBox recHeader = new HBox(8);
            recHeader.setAlignment(Pos.CENTER_LEFT);

            Label recTag = new Label("PRESCRIPTIVE ACTION PLAN:");
            recTag.setStyle("-fx-text-fill: " + Theme.COLOR_ACCENT_PRIMARY + "; -fx-font-size: 11px; -fx-font-weight: bold;");

            Label prioPill = new Label(rec.priority().name() + " PRIORITY");
            prioPill.setStyle(Theme.getBadgeStyle("#3B82F6", "#FFFFFF"));

            recHeader.getChildren().addAll(recTag, prioPill);

            Label recTitle = new Label(rec.title());
            recTitle.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: bold;");

            Label recAction = new Label(rec.actionText());
            recAction.setWrapText(true);
            recAction.setStyle("-fx-text-fill: " + Theme.TEXT_SECONDARY + "; -fx-font-size: 12px;");

            Label impactLabel = new Label("Expected ROI / Impact: " + rec.expectedImpact());
            impactLabel.setStyle("-fx-text-fill: " + Theme.COLOR_SUCCESS + "; -fx-font-size: 11px; -fx-font-weight: bold;");

            recBox.getChildren().addAll(recHeader, recTitle, recAction, impactLabel);
            card.getChildren().add(recBox);
        }

        return card;
    }

    public void showLoading(boolean show) {
        loadingOverlay.setVisible(show);
    }

    public void showSuccess(String msg) {
        notificationBanner.setText("✓ " + msg);
        notificationBanner.setStyle("-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #6EE7B7; -fx-padding: 10 16 10 16; -fx-background-radius: 8px; -fx-font-size: 13px;");
        notificationBanner.setVisible(true);
        notificationBanner.setManaged(true);
    }

    public void showError(String msg) {
        notificationBanner.setText("✕ " + msg);
        notificationBanner.setStyle("-fx-background-color: rgba(239, 68, 68, 0.2); -fx-text-fill: #FCA5A5; -fx-padding: 10 16 10 16; -fx-background-radius: 8px; -fx-font-size: 13px;");
        notificationBanner.setVisible(true);
        notificationBanner.setManaged(true);
    }
}
