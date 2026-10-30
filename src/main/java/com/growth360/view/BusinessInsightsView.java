package com.growth360.view;

import com.growth360.model.Recommendation;
import com.growth360.model.Recommendation.Priority;
import com.growth360.service.AnalyticsService;
import com.growth360.util.Theme;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure Java JavaFX 7th Screen: Business Insights & Prescriptive Decision Matrix.
 * Displays automated prescriptive actions derived from real-time analytics,
 * and acts as the official launchpad for the upcoming AI Copilot.
 */
public class BusinessInsightsView extends ScrollPane {

    private final AnalyticsService analyticsService;
    private final List<Recommendation> masterRecommendations = new ArrayList<>();

    private VBox mainContainer;
    private VBox recommendationsCardContainer;
    private StackPane loadingOverlay;
    private Label notificationBanner;

    private Label lblTotalRecs;
    private Label lblUrgentCount;
    private Label lblHighCount;
    private ComboBox<String> categoryFilter;

    public BusinessInsightsView(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;

        buildUI();
        loadInsights();
    }

    private void buildUI() {
        setFitToWidth(true);
        setStyle("-fx-background: " + Theme.COLOR_BG_APP + "; -fx-background-color: transparent;");

        mainContainer = new VBox(24);
        mainContainer.setPadding(new Insets(24, 28, 40, 28));
        mainContainer.setStyle("-fx-background-color: " + Theme.COLOR_BG_APP + ";");

        // 1. Top Header
        VBox headerBox = new VBox(4);
        Label title = new Label("Titan Watches — Strategic Business Insights & Prescriptions");
        title.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label subtitle = new Label("Prescriptive decision-support rules, operational interventions, and AI Copilot surveillance");
        subtitle.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 13px;");
        headerBox.getChildren().addAll(title, subtitle);

        notificationBanner = new Label();
        notificationBanner.setVisible(false);
        notificationBanner.setManaged(false);
        notificationBanner.setStyle("-fx-background-color: rgba(16, 185, 129, 0.15); -fx-text-fill: #34D399; -fx-padding: 10 16 10 16; -fx-background-radius: 8px; -fx-font-size: 13px;");

        // 2. AI Copilot Launchpad Hero Banner
        VBox aiHeroCard = buildAiCopilotHeroBanner();

        // 3. Stat KPI Cards
        HBox statsRow = buildStatsRow();

        // 4. Filter Toolbar
        HBox filterBar = buildFilterBar();

        // 5. Prescriptive Cards Container
        recommendationsCardContainer = new VBox(16);

        mainContainer.getChildren().addAll(headerBox, notificationBanner, aiHeroCard, statsRow, filterBar, recommendationsCardContainer);

        // Loading Overlay
        loadingOverlay = new StackPane();
        loadingOverlay.setStyle("-fx-background-color: rgba(10, 14, 23, 0.7);");
        VBox loadBox = new VBox(12, new ProgressIndicator(), new Label("Synthesizing strategic business insights..."));
        loadBox.setAlignment(Pos.CENTER);
        loadBox.getChildren().get(1).setStyle("-fx-text-fill: " + Theme.COLOR_ACCENT_PRIMARY + "; -fx-font-size: 14px;");
        loadingOverlay.getChildren().add(loadBox);
        loadingOverlay.setVisible(false);

        StackPane rootWrapper = new StackPane(mainContainer, loadingOverlay);
        setContent(rootWrapper);
    }

    private VBox buildAiCopilotHeroBanner() {
        VBox card = new VBox(12);
        card.setStyle(
                "-fx-background-color: linear-gradient(to right, #162032, #0F2A3D); " +
                "-fx-background-radius: 12px; " +
                "-fx-border-color: #38BDF8; " +
                "-fx-border-radius: 12px; " +
                "-fx-border-width: 1px; " +
                "-fx-padding: 20px;"
        );

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label aiBadge = new Label("AI COPILOT LAUNCHPAD");
        aiBadge.setStyle("-fx-background-color: rgba(56, 189, 248, 0.2); -fx-text-fill: #38BDF8; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 10 4 10; -fx-background-radius: 6px;");

        Label readyStatus = new Label("● MetricContext Telemetry Engine Active");
        readyStatus.setStyle("-fx-text-fill: #10B981; -fx-font-size: 12px; -fx-font-weight: bold;");

        titleRow.getChildren().addAll(aiBadge, readyStatus);

        Label heading = new Label("Deterministic Prescriptive Rules & Multi-Dimensional Diagnostic Engine");
        heading.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label body = new Label("Growth 360 AI evaluates inventory safety stocks, marketing spend variance, and Encircle buyer recency " +
                "to synthesize instant, deterministic management actions. Ready for seamless integration with upcoming ML Copilot models.");
        body.setWrapText(true);
        body.setStyle("-fx-text-fill: " + Theme.TEXT_SECONDARY + "; -fx-font-size: 13px;");

        card.getChildren().addAll(titleRow, heading, body);
        return card;
    }

    private HBox buildStatsRow() {
        HBox row = new HBox(16);
        lblTotalRecs = new Label("0");
        lblUrgentCount = new Label("0");
        lblHighCount = new Label("0");

        row.getChildren().addAll(
                createStatCard("Total Prescriptions", lblTotalRecs, Theme.COLOR_ACCENT_PRIMARY),
                createStatCard("Urgent Interventions", lblUrgentCount, Theme.COLOR_DANGER),
                createStatCard("High Priority Actions", lblHighCount, Theme.COLOR_WARNING),
                createStatCard("Analytical Reliability", new Label("100% Deterministic"), Theme.COLOR_SUCCESS)
        );
        return row;
    }

    private VBox createStatCard(String title, Label valLbl, String accentColor) {
        VBox card = new VBox(6);
        card.setStyle(Theme.CARD_STYLE);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label t = new Label(title);
        t.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 12px; -fx-font-weight: 500;");

        valLbl.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 22px; -fx-font-weight: bold;");
        card.getChildren().addAll(t, valLbl);
        return card;
    }

    private HBox buildFilterBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);

        Label filterLbl = new Label("Filter Domain:");
        filterLbl.setStyle("-fx-text-fill: " + Theme.TEXT_SECONDARY + "; -fx-font-size: 13px;");

        categoryFilter = new ComboBox<>();
        categoryFilter.getItems().addAll("All Domains", "Horology Inventory", "Financial Control", "Boutique Revenue", "Encircle Loyalty");
        categoryFilter.setValue("All Domains");
        categoryFilter.setStyle(
                "-fx-background-color: " + Theme.COLOR_BG_CARD + "; " +
                "-fx-text-fill: " + Theme.TEXT_PRIMARY + "; " +
                "-fx-border-color: " + Theme.COLOR_BORDER_LIGHT + "; " +
                "-fx-border-radius: 6px; -fx-background-radius: 6px;"
        );
        categoryFilter.setOnAction(e -> applyFilter());

        Button btnRefresh = new Button("↻ Refresh Insights");
        btnRefresh.setStyle(
                "-fx-background-color: " + Theme.COLOR_BG_CARD + "; " +
                "-fx-text-fill: " + Theme.COLOR_ACCENT_PRIMARY + "; " +
                "-fx-border-color: " + Theme.COLOR_ACCENT_PRIMARY + "; " +
                "-fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 6 14 6 14; -fx-cursor: hand;"
        );
        btnRefresh.setOnAction(e -> loadInsights());

        bar.getChildren().addAll(filterLbl, categoryFilter, new Region(), btnRefresh);
        HBox.setHgrow(bar.getChildren().get(2), Priority.ALWAYS);
        return bar;
    }

    public void loadInsights() {
        loadingOverlay.setVisible(true);
        analyticsService.computeSnapshotAsync(
                snapshot -> {
                    loadingOverlay.setVisible(false);
                    masterRecommendations.clear();
                    masterRecommendations.addAll(snapshot.recommendations());

                    lblTotalRecs.setText(String.valueOf(masterRecommendations.size()));
                    long urgent = masterRecommendations.stream().filter(r -> r.priority() == Priority.URGENT).count();
                    long high = masterRecommendations.stream().filter(r -> r.priority() == Priority.HIGH).count();
                    lblUrgentCount.setText(String.valueOf(urgent));
                    lblHighCount.setText(String.valueOf(high));

                    applyFilter();
                },
                error -> {
                    loadingOverlay.setVisible(false);
                    showNotification("Failed to load insights: " + error.getMessage(), true);
                }
        );
    }

    private void applyFilter() {
        recommendationsCardContainer.getChildren().clear();
        String selected = categoryFilter.getValue();

        List<Recommendation> filtered = masterRecommendations.stream()
                .filter(r -> {
                    if ("All Domains".equals(selected)) return true;
                    if ("Horology Inventory".equals(selected)) return r.category().contains("Inventory");
                    if ("Financial Control".equals(selected)) return r.category().contains("Financial");
                    if ("Boutique Revenue".equals(selected)) return r.category().contains("Revenue");
                    if ("Encircle Loyalty".equals(selected)) return r.category().contains("Loyalty") || r.category().contains("Customer");
                    return true;
                })
                .toList();

        if (filtered.isEmpty()) {
            Label emptyLbl = new Label("No active prescriptive actions matching the selected filter.");
            emptyLbl.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 14px; -fx-padding: 30;");
            recommendationsCardContainer.getChildren().add(emptyLbl);
            return;
        }

        for (Recommendation r : filtered) {
            recommendationsCardContainer.getChildren().add(createRecommendationCard(r));
        }
    }

    private VBox createRecommendationCard(Recommendation r) {
        VBox card = new VBox(12);
        card.setStyle(
                "-fx-background-color: " + Theme.COLOR_BG_CARD + "; " +
                "-fx-background-radius: 12px; " +
                "-fx-border-color: " + getBorderColorForPriority(r.priority()) + "; " +
                "-fx-border-radius: 12px; " +
                "-fx-border-width: 1px; " +
                "-fx-padding: 20px;"
        );

        // Header: Badge + Category + Action Title
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label priorityBadge = new Label(r.priority().name());
        priorityBadge.setStyle(
                "-fx-background-color: " + getBgColorForPriority(r.priority()) + "; " +
                "-fx-text-fill: " + getTextColorForPriority(r.priority()) + "; " +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10 4 10; -fx-background-radius: 6px;"
        );

        Label categoryLbl = new Label(r.category().toUpperCase());
        categoryLbl.setStyle("-fx-text-fill: " + Theme.COLOR_ACCENT_PRIMARY + "; -fx-font-size: 12px; -fx-font-weight: bold;");

        topRow.getChildren().addAll(priorityBadge, categoryLbl);

        Label actionTitle = new Label(r.title());
        actionTitle.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 17px; -fx-font-weight: bold;");

        Label actionText = new Label(r.actionText());
        actionText.setWrapText(true);
        actionText.setStyle("-fx-text-fill: " + Theme.TEXT_SECONDARY + "; -fx-font-size: 13px;");

        // Impact Box
        HBox impactBox = new HBox(8);
        impactBox.setAlignment(Pos.CENTER_LEFT);
        impactBox.setStyle(
                "-fx-background-color: rgba(16, 185, 129, 0.08); " +
                "-fx-padding: 10 14 10 14; -fx-background-radius: 8px; -fx-border-color: rgba(16, 185, 129, 0.25); -fx-border-radius: 8px;"
        );
        Label impactIcon = new Label("★ EXPECTED STRATEGIC IMPACT:");
        impactIcon.setStyle("-fx-text-fill: #10B981; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label impactText = new Label(r.expectedImpact());
        impactText.setStyle("-fx-text-fill: #A7F3D0; -fx-font-size: 12px;");
        impactBox.getChildren().addAll(impactIcon, impactText);

        // Bottom Action Button
        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER_RIGHT);
        Button btnAcknowledge = new Button("✓ Acknowledge & Deploy Strategy");
        btnAcknowledge.setStyle(
                "-fx-background-color: rgba(56, 189, 248, 0.12); " +
                "-fx-text-fill: " + Theme.COLOR_ACCENT_PRIMARY + "; " +
                "-fx-border-color: " + Theme.COLOR_ACCENT_PRIMARY + "; " +
                "-fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 7 16 7 16; -fx-font-size: 12px; -fx-cursor: hand;"
        );
        btnAcknowledge.setOnAction(e -> {
            showNotification("Strategy acknowledged: " + r.title() + " recorded into operational log.", false);
        });
        bottomRow.getChildren().add(btnAcknowledge);

        card.getChildren().addAll(topRow, actionTitle, actionText, impactBox, bottomRow);
        return card;
    }

    private String getBorderColorForPriority(Priority p) {
        return switch (p) {
            case URGENT -> "rgba(239, 68, 68, 0.4)";
            case HIGH -> "rgba(245, 158, 11, 0.4)";
            case MEDIUM -> "rgba(56, 189, 248, 0.3)";
            case LOW -> Theme.COLOR_BORDER;
        };
    }

    private String getBgColorForPriority(Priority p) {
        return switch (p) {
            case URGENT -> "rgba(239, 68, 68, 0.18)";
            case HIGH -> "rgba(245, 158, 11, 0.18)";
            case MEDIUM -> "rgba(56, 189, 248, 0.15)";
            case LOW -> "rgba(100, 116, 139, 0.15)";
        };
    }

    private String getTextColorForPriority(Priority p) {
        return switch (p) {
            case URGENT -> "#EF4444";
            case HIGH -> "#F59E0B";
            case MEDIUM -> "#38BDF8";
            case LOW -> "#94A3B8";
        };
    }

    private void showNotification(String message, boolean isError) {
        notificationBanner.setText(message);
        if (isError) {
            notificationBanner.setStyle("-fx-background-color: rgba(239, 68, 68, 0.2); -fx-text-fill: #FCA5A5; -fx-padding: 10 16 10 16; -fx-background-radius: 8px; -fx-font-size: 13px;");
        } else {
            notificationBanner.setStyle("-fx-background-color: rgba(16, 185, 129, 0.15); -fx-text-fill: #34D399; -fx-padding: 10 16 10 16; -fx-background-radius: 8px; -fx-font-size: 13px;");
        }
        notificationBanner.setVisible(true);
        notificationBanner.setManaged(true);
    }
}
