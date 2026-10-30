package com.growth360.view;

import com.growth360.copilot.AiCopilotEngine;
import com.growth360.copilot.CopilotResponse;
import com.growth360.model.MetricContext;
import com.growth360.model.Recommendation;
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
 * Pure Java JavaFX 7th Screen: Business Insights & Interactive AI Copilot.
 * Integrates deterministic prescriptive actions with a real-time conversational
 * reasoning agent ingesting live MetricContext telemetry.
 */
public class BusinessInsightsView extends ScrollPane {

    private final AnalyticsService analyticsService;
    private final AiCopilotEngine copilotEngine = new AiCopilotEngine();
    private final List<Recommendation> masterRecommendations = new ArrayList<>();
    private MetricContext currentContext = null;

    private VBox mainContainer;
    private VBox recommendationsCardContainer;
    private StackPane loadingOverlay;
    private Label notificationBanner;

    // Copilot UI components
    private TextField copilotInputField;
    private VBox copilotResponseCard;
    private Label copilotHeadline;
    private Label copilotUrgencyBadge;
    private FlowPane copilotMetricsPane;
    private Label copilotNarrative;
    private VBox copilotActionsContainer;
    private FlowPane copilotFollowUpsPane;

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
        Label title = new Label("Titan Watches — Strategic Business Insights & AI Copilot");
        title.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label subtitle = new Label("Prescriptive decision-support rules, operational interventions, and real-time AI Copilot reasoning");
        subtitle.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 13px;");
        headerBox.getChildren().addAll(title, subtitle);

        notificationBanner = new Label();
        notificationBanner.setVisible(false);
        notificationBanner.setManaged(false);
        notificationBanner.setStyle("-fx-background-color: rgba(16, 185, 129, 0.15); -fx-text-fill: #34D399; -fx-padding: 10 16 10 16; -fx-background-radius: 8px; -fx-font-size: 13px;");

        // 2. Interactive AI Copilot Section
        VBox aiCopilotSection = buildAiCopilotSection();

        // 3. Stat KPI Cards
        HBox statsRow = buildStatsRow();

        // 4. Filter Toolbar
        HBox filterBar = buildFilterBar();

        // 5. Prescriptive Cards Container
        recommendationsCardContainer = new VBox(16);

        mainContainer.getChildren().addAll(headerBox, notificationBanner, aiCopilotSection, statsRow, filterBar, recommendationsCardContainer);

        // Loading Overlay
        loadingOverlay = new StackPane();
        loadingOverlay.setStyle("-fx-background-color: rgba(10, 14, 23, 0.7);");
        VBox loadBox = new VBox(12, new ProgressIndicator(), new Label("Synthesizing strategic business insights & telemetry..."));
        loadBox.setAlignment(Pos.CENTER);
        loadBox.getChildren().get(1).setStyle("-fx-text-fill: " + Theme.COLOR_ACCENT_PRIMARY + "; -fx-font-size: 14px;");
        loadingOverlay.getChildren().add(loadBox);
        loadingOverlay.setVisible(false);

        StackPane rootWrapper = new StackPane(mainContainer, loadingOverlay);
        setContent(rootWrapper);
    }

    private VBox buildAiCopilotSection() {
        VBox card = new VBox(16);
        card.setStyle(
                "-fx-background-color: linear-gradient(to right, #131D31, #0E2436); " +
                "-fx-background-radius: 14px; " +
                "-fx-border-color: #38BDF8; " +
                "-fx-border-radius: 14px; " +
                "-fx-border-width: 1px; " +
                "-fx-padding: 22px;"
        );

        // Header
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label aiBadge = new Label("🤖 AI COPILOT REASONING TERMINAL");
        aiBadge.setStyle("-fx-background-color: rgba(56, 189, 248, 0.2); -fx-text-fill: #38BDF8; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 5 12 5 12; -fx-background-radius: 6px;");

        Label readyStatus = new Label("● MetricContext Telemetry Engine Active");
        readyStatus.setStyle("-fx-text-fill: #10B981; -fx-font-size: 12px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label localServerLbl = new Label("🌐 Localhost Server: http://localhost:8080");
        localServerLbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px; -fx-background-color: rgba(255,255,255,0.06); -fx-padding: 4 10 4 10; -fx-background-radius: 6px;");

        titleRow.getChildren().addAll(aiBadge, readyStatus, spacer, localServerLbl);

        // Quick Prompt Chips
        FlowPane chipsPane = new FlowPane(8, 8);
        chipsPane.getChildren().addAll(
                createPromptChip("📈 Analyze Profit & Margins", "Analyze current profit health"),
                createPromptChip("⚠️ Explain Marketing Anomaly (Z > 3.0)", "Explain marketing expense anomaly"),
                createPromptChip("📦 Check Watch Inventory (ROP)", "Check watch inventory and stockout risks"),
                createPromptChip("👥 Reactivate At-Risk Collectors", "How to win back at-risk customers?"),
                createPromptChip("🏆 Improve Growth Score to 90+", "How do we improve our Growth Score?"),
                createPromptChip("🔮 Revenue Forecast (OLS)", "What is next month revenue forecast?"),
                createPromptChip("🎯 30-Day Executive Action Plan", "Give me a 30-day executive action plan")
        );

        // Input Bar
        HBox inputBar = new HBox(10);
        inputBar.setAlignment(Pos.CENTER_LEFT);

        copilotInputField = new TextField();
        copilotInputField.setPromptText("Ask Growth 360 AI Copilot anything about sales, inventory, expenses, or forecasts...");
        copilotInputField.setStyle(
                "-fx-background-color: #0B1220; " +
                "-fx-text-fill: #F8FAFC; " +
                "-fx-border-color: rgba(56, 189, 248, 0.4); " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px; " +
                "-fx-padding: 10 16 10 16; " +
                "-fx-font-size: 13px;"
        );
        HBox.setHgrow(copilotInputField, Priority.ALWAYS);
        copilotInputField.setOnAction(e -> executeCopilotQuery(copilotInputField.getText()));

        Button btnAsk = new Button("⚡ Ask Copilot");
        btnAsk.setStyle(
                "-fx-background-color: #0284C7; " +
                "-fx-text-fill: #FFFFFF; " +
                "-fx-font-weight: bold; " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px; " +
                "-fx-padding: 10 20 10 20; " +
                "-fx-cursor: hand;"
        );
        btnAsk.setOnAction(e -> executeCopilotQuery(copilotInputField.getText()));

        Button btnClear = new Button("🧹 Reset");
        btnClear.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06); " +
                "-fx-text-fill: #94A3B8; " +
                "-fx-border-color: rgba(255,255,255,0.1); " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px; " +
                "-fx-padding: 10 16 10 16; " +
                "-fx-cursor: hand;"
        );
        btnClear.setOnAction(e -> {
            copilotInputField.clear();
            executeCopilotQuery("overview");
        });

        inputBar.getChildren().addAll(copilotInputField, btnAsk, btnClear);

        // Response Card Container
        copilotResponseCard = buildCopilotResponseCard();

        card.getChildren().addAll(titleRow, chipsPane, inputBar, copilotResponseCard);
        return card;
    }

    private Button createPromptChip(String title, String query) {
        Button chip = new Button(title);
        chip.setStyle(
                "-fx-background-color: #1A263D; " +
                "-fx-text-fill: #CBD5E1; " +
                "-fx-border-color: rgba(56, 189, 248, 0.25); " +
                "-fx-border-radius: 16px; " +
                "-fx-background-radius: 16px; " +
                "-fx-padding: 6 14 6 14; " +
                "-fx-font-size: 12px; " +
                "-fx-cursor: hand;"
        );
        chip.setOnAction(e -> {
            copilotInputField.setText(query);
            executeCopilotQuery(query);
        });
        return chip;
    }

    private VBox buildCopilotResponseCard() {
        VBox box = new VBox(12);
        box.setStyle(
                "-fx-background-color: #090F1B; " +
                "-fx-background-radius: 10px; " +
                "-fx-border-color: rgba(56, 189, 248, 0.3); " +
                "-fx-border-radius: 10px; " +
                "-fx-border-width: 1px; " +
                "-fx-padding: 18px;"
        );

        // Headline & Urgency Badge Row
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        copilotHeadline = new Label("Initializing AI Copilot Telemetry Engine...");
        copilotHeadline.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 15px; -fx-font-weight: bold;");
        HBox.setHgrow(copilotHeadline, Priority.ALWAYS);

        copilotUrgencyBadge = new Label("NORMAL");
        copilotUrgencyBadge.setStyle("-fx-background-color: rgba(56, 189, 248, 0.2); -fx-text-fill: #38BDF8; -fx-padding: 3 8 3 8; -fx-background-radius: 4px; -fx-font-size: 10px; -fx-font-weight: bold;");

        topRow.getChildren().addAll(copilotHeadline, copilotUrgencyBadge);

        // Metrics FlowPane
        copilotMetricsPane = new FlowPane(8, 8);

        // Narrative Content Label
        copilotNarrative = new Label("Loading operational state...");
        copilotNarrative.setWrapText(true);
        copilotNarrative.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 13px; -fx-line-spacing: 3px;");

        // Prescriptive Actions Container
        copilotActionsContainer = new VBox(6);
        copilotActionsContainer.setStyle(
                "-fx-background-color: rgba(16, 185, 129, 0.08); " +
                "-fx-border-color: rgba(16, 185, 129, 0.25); " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px; " +
                "-fx-padding: 12px 16px;"
        );

        // Follow Up Prompts FlowPane
        VBox followUpBox = new VBox(6);
        Label followUpTitle = new Label("SUGGESTED EXPLORATION FOLLOW-UPS:");
        followUpTitle.setStyle("-fx-text-fill: #64748B; -fx-font-size: 10px; -fx-font-weight: bold;");
        copilotFollowUpsPane = new FlowPane(8, 8);
        followUpBox.getChildren().addAll(followUpTitle, copilotFollowUpsPane);

        box.getChildren().addAll(topRow, copilotMetricsPane, copilotNarrative, copilotActionsContainer, followUpBox);
        return box;
    }

    private void executeCopilotQuery(String query) {
        if (currentContext == null) {
            try {
                currentContext = analyticsService.computeMetricContext();
            } catch (Exception e) {
                showNotification("Failed to fetch MetricContext: " + e.getMessage(), true);
                return;
            }
        }

        CopilotResponse response = copilotEngine.ask(query, currentContext);
        renderCopilotResponse(response);
    }

    private void renderCopilotResponse(CopilotResponse resp) {
        copilotHeadline.setText(resp.headline());

        // Urgency styling
        copilotUrgencyBadge.setText(resp.urgency().name());
        switch (resp.urgency()) {
            case CRITICAL -> copilotUrgencyBadge.setStyle("-fx-background-color: rgba(239, 68, 68, 0.25); -fx-text-fill: #EF4444; -fx-padding: 3 8 3 8; -fx-background-radius: 4px; -fx-font-size: 10px; -fx-font-weight: bold;");
            case HIGH -> copilotUrgencyBadge.setStyle("-fx-background-color: rgba(245, 158, 11, 0.25); -fx-text-fill: #F59E0B; -fx-padding: 3 8 3 8; -fx-background-radius: 4px; -fx-font-size: 10px; -fx-font-weight: bold;");
            case WATCH -> copilotUrgencyBadge.setStyle("-fx-background-color: rgba(245, 158, 11, 0.2); -fx-text-fill: #FBBF24; -fx-padding: 3 8 3 8; -fx-background-radius: 4px; -fx-font-size: 10px; -fx-font-weight: bold;");
            case NORMAL -> copilotUrgencyBadge.setStyle("-fx-background-color: rgba(56, 189, 248, 0.2); -fx-text-fill: #38BDF8; -fx-padding: 3 8 3 8; -fx-background-radius: 4px; -fx-font-size: 10px; -fx-font-weight: bold;");
        }

        // Metrics tags
        copilotMetricsPane.getChildren().clear();
        for (String m : resp.keyMetrics()) {
            Label tag = new Label(m);
            tag.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #38BDF8; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 8 3 8; -fx-background-radius: 4px;");
            copilotMetricsPane.getChildren().add(tag);
        }

        // Clean narrative markdown tags for pure JavaFX label
        String cleanNarrative = resp.narrative()
                .replace("### ", "")
                .replace("**", "")
                .replace("* ", "• ");
        copilotNarrative.setText(cleanNarrative);

        // Actions
        copilotActionsContainer.getChildren().clear();
        Label actHeader = new Label("★ PRESCRIPTIVE NEXT ACTIONS:");
        actHeader.setStyle("-fx-text-fill: #10B981; -fx-font-size: 11px; -fx-font-weight: bold;");
        copilotActionsContainer.getChildren().add(actHeader);

        for (String act : resp.suggestedActions()) {
            Label actLbl = new Label("→ " + act);
            actLbl.setWrapText(true);
            actLbl.setStyle("-fx-text-fill: #A7F3D0; -fx-font-size: 12px;");
            copilotActionsContainer.getChildren().add(actLbl);
        }

        // Follow Up Chips
        copilotFollowUpsPane.getChildren().clear();
        for (String fu : resp.followUpPrompts()) {
            Button fuBtn = new Button(fu);
            fuBtn.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.05); " +
                    "-fx-text-fill: #94A3B8; " +
                    "-fx-border-color: rgba(255,255,255,0.1); " +
                    "-fx-border-radius: 12px; " +
                    "-fx-background-radius: 12px; " +
                    "-fx-padding: 4 10 4 10; " +
                    "-fx-font-size: 11px; " +
                    "-fx-cursor: hand;"
            );
            fuBtn.setOnAction(e -> {
                copilotInputField.setText(fu);
                executeCopilotQuery(fu);
            });
            copilotFollowUpsPane.getChildren().add(fuBtn);
        }
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
                    long urgent = masterRecommendations.stream().filter(r -> r.priority() == Recommendation.Priority.URGENT).count();
                    long high = masterRecommendations.stream().filter(r -> r.priority() == Recommendation.Priority.HIGH).count();
                    lblUrgentCount.setText(String.valueOf(urgent));
                    lblHighCount.setText(String.valueOf(high));

                    applyFilter();

                    // Pre-load Copilot with current context overview
                    try {
                        currentContext = analyticsService.computeMetricContext();
                        executeCopilotQuery("overview");
                    } catch (Exception ignored) {}
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

    private String getBorderColorForPriority(Recommendation.Priority p) {
        return switch (p) {
            case URGENT -> "rgba(239, 68, 68, 0.4)";
            case HIGH -> "rgba(245, 158, 11, 0.4)";
            case MEDIUM -> "rgba(56, 189, 248, 0.3)";
            case LOW -> Theme.COLOR_BORDER;
        };
    }

    private String getBgColorForPriority(Recommendation.Priority p) {
        return switch (p) {
            case URGENT -> "rgba(239, 68, 68, 0.18)";
            case HIGH -> "rgba(245, 158, 11, 0.18)";
            case MEDIUM -> "rgba(56, 189, 248, 0.15)";
            case LOW -> "rgba(100, 116, 139, 0.15)";
        };
    }

    private String getTextColorForPriority(Recommendation.Priority p) {
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
