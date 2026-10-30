package com.growth360.view;

import com.growth360.analytics.GrowthScoreCalculator.GrowthScoreResult;
import com.growth360.controller.DashboardController;
import com.growth360.model.Alert;
import com.growth360.model.ForecastPoint;
import com.growth360.model.Recommendation;
import com.growth360.service.AnalyticsService.AnalyticsSnapshot;
import com.growth360.util.Theme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Map;

/**
 * Pure Java JavaFX Executive Dashboard view featuring Growth Score,
 * financial KPIs, forecasting line charts, expense bar charts, and prioritized prescriptive alerts.
 */
public class DashboardView extends ScrollPane {

    private final DashboardController controller;

    private VBox mainContainer;
    private StackPane loadingOverlay;
    private Label errorBanner;

    // Growth Score Hero components
    private Label growthScoreValueLabel;
    private Label healthBandBadge;
    private Label insightLabel;
    private ProgressBar pbRevenue;
    private ProgressBar pbExpense;
    private ProgressBar pbInventory;
    private ProgressBar pbCustomer;
    private Label lblRevenueScore;
    private Label lblExpenseScore;
    private Label lblInventoryScore;
    private Label lblCustomerScore;

    // KPI Stat labels
    private Label lblTotalRevenue;
    private Label lblTotalExpenses;
    private Label lblNetProfit;
    private Label lblActiveAlerts;

    // Charts
    private LineChart<String, Number> revenueChart;
    private XYChart.Series<String, Number> actualSeries;
    private XYChart.Series<String, Number> forecastSeries;
    private BarChart<String, Number> expenseChart;
    private XYChart.Series<String, Number> expenseSeries;

    // Bottom sections
    private VBox alertsContainer;
    private VBox recommendationsContainer;

    public DashboardView(DashboardController controller) {
        this.controller = controller;
        this.controller.setView(this);

        buildUI();
        this.controller.loadDashboardData();
    }

    private void buildUI() {
        setFitToWidth(true);
        setStyle("-fx-background: " + Theme.COLOR_BG_APP + "; -fx-background-color: transparent;");

        mainContainer = new VBox(24);
        mainContainer.setPadding(new Insets(24, 28, 40, 28));
        mainContainer.setStyle("-fx-background-color: " + Theme.COLOR_BG_APP + ";");

        // Top Header
        VBox headerBox = new VBox(4);
        Label title = new Label("Titan Watches Executive Analytics & Supply Surveillance");
        title.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label subtitle = new Label("World of Titan boutique sales, horology inventory runway, and festive ad anomaly surveillance");
        subtitle.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 13px;");
        headerBox.getChildren().addAll(title, subtitle);

        errorBanner = new Label();
        errorBanner.setVisible(false);
        errorBanner.setManaged(false);
        errorBanner.setStyle("-fx-background-color: rgba(239, 68, 68, 0.2); -fx-text-fill: #FCA5A5; -fx-padding: 10 16 10 16; -fx-background-radius: 8px; -fx-font-size: 13px;");

        // Top Row: Growth Score Hero Card (left) + 4 Stat KPI Cards (right)
        HBox topRow = new HBox(20);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox growthScoreCard = buildGrowthScoreHeroCard();
        HBox.setHgrow(growthScoreCard, Priority.NEVER);
        growthScoreCard.setPrefWidth(380);

        GridPane kpiGrid = buildKpiCardsGrid();
        HBox.setHgrow(kpiGrid, Priority.ALWAYS);

        topRow.getChildren().addAll(growthScoreCard, kpiGrid);

        // Middle Row: Charts
        HBox chartsRow = new HBox(20);
        chartsRow.setAlignment(Pos.CENTER);

        VBox revChartCard = buildRevenueForecastChartCard();
        HBox.setHgrow(revChartCard, Priority.ALWAYS);

        VBox expChartCard = buildExpenseBreakdownChartCard();
        HBox.setHgrow(expChartCard, Priority.ALWAYS);

        chartsRow.getChildren().addAll(revChartCard, expChartCard);

        // Bottom Row: Alerts and Recommendations
        HBox bottomRow = new HBox(20);

        VBox alertsCard = buildAlertsSection();
        HBox.setHgrow(alertsCard, Priority.ALWAYS);

        VBox recsCard = buildRecommendationsSection();
        HBox.setHgrow(recsCard, Priority.ALWAYS);

        bottomRow.getChildren().addAll(alertsCard, recsCard);

        mainContainer.getChildren().addAll(headerBox, errorBanner, topRow, chartsRow, bottomRow);

        // Overlay for background loading
        loadingOverlay = new StackPane();
        loadingOverlay.setStyle("-fx-background-color: rgba(10, 14, 23, 0.65);");
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(48, 48);
        loadingOverlay.getChildren().add(spinner);
        loadingOverlay.setVisible(false);

        StackPane rootWrapper = new StackPane(mainContainer, loadingOverlay);
        setContent(rootWrapper);
    }

    private VBox buildGrowthScoreHeroCard() {
        VBox card = new VBox(14);
        card.setStyle(Theme.STAT_CARD_STYLE + " -fx-border-color: #38BDF8; -fx-border-width: 1px 1px 1px 4px;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("BUSINESS HEALTH SCORE");
        title.setStyle("-fx-text-fill: " + Theme.COLOR_ACCENT_PRIMARY + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        healthBandBadge = new Label("ANALYZING");
        healthBandBadge.setStyle(Theme.getBadgeStyle("#334155", "#94A3B8"));
        header.getChildren().addAll(title, healthBandBadge);

        HBox scoreBox = new HBox(12);
        scoreBox.setAlignment(Pos.BASELINE_LEFT);
        growthScoreValueLabel = new Label("--");
        growthScoreValueLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 46px; -fx-font-weight: 800;");

        Label maxScoreLabel = new Label("/ 100");
        maxScoreLabel.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 18px; -fx-font-weight: 600;");
        scoreBox.getChildren().addAll(growthScoreValueLabel, maxScoreLabel);

        insightLabel = new Label("Computing composite health index...");
        insightLabel.setWrapText(true);
        insightLabel.setStyle("-fx-text-fill: " + Theme.TEXT_SECONDARY + "; -fx-font-size: 12px;");

        // Breakdown Progress Bars
        VBox breakdown = new VBox(8);

        lblRevenueScore = new Label("30% Revenue Trend: --");
        lblRevenueScore.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 11px;");
        pbRevenue = new ProgressBar(0.5);
        pbRevenue.setMaxWidth(Double.MAX_VALUE);
        pbRevenue.setStyle("-fx-accent: #38BDF8;");

        lblExpenseScore = new Label("25% Expense Control: --");
        lblExpenseScore.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 11px;");
        pbExpense = new ProgressBar(0.5);
        pbExpense.setMaxWidth(Double.MAX_VALUE);
        pbExpense.setStyle("-fx-accent: #10B981;");

        lblInventoryScore = new Label("25% Inventory Health: --");
        lblInventoryScore.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 11px;");
        pbInventory = new ProgressBar(0.5);
        pbInventory.setMaxWidth(Double.MAX_VALUE);
        pbInventory.setStyle("-fx-accent: #F59E0B;");

        lblCustomerScore = new Label("20% Customer Retention: --");
        lblCustomerScore.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 11px;");
        pbCustomer = new ProgressBar(0.5);
        pbCustomer.setMaxWidth(Double.MAX_VALUE);
        pbCustomer.setStyle("-fx-accent: #818CF8;");

        breakdown.getChildren().addAll(
                lblRevenueScore, pbRevenue,
                lblExpenseScore, pbExpense,
                lblInventoryScore, pbInventory,
                lblCustomerScore, pbCustomer
        );

        card.getChildren().addAll(header, scoreBox, insightLabel, breakdown);
        return card;
    }

    private GridPane buildKpiCardsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        // KPI 1: Revenue
        lblTotalRevenue = new Label("$0.00");
        VBox card1 = createKpiCard("TOTAL REVENUE", lblTotalRevenue, "6-month aggregate gross income", "#38BDF8");

        // KPI 2: Expenses
        lblTotalExpenses = new Label("$0.00");
        VBox card2 = createKpiCard("TOTAL EXPENSES", lblTotalExpenses, "Operational & payroll expenditure", "#EF4444");

        // KPI 3: Net Profit
        lblNetProfit = new Label("$0.00");
        VBox card3 = createKpiCard("NET OPERATING PROFIT", lblNetProfit, "Retained business surplus", "#10B981");

        // KPI 4: Active Alerts
        lblActiveAlerts = new Label("0 Active");
        VBox card4 = createKpiCard("RISK SURVEILLANCE", lblActiveAlerts, "Z-Score spikes & stockout alerts", "#F59E0B");

        grid.add(card1, 0, 0);
        grid.add(card2, 1, 0);
        grid.add(card3, 0, 1);
        grid.add(card4, 1, 1);

        return grid;
    }

    private VBox createKpiCard(String title, Label valueLabel, String subtitle, String accentHex) {
        VBox card = new VBox(6);
        card.setStyle(Theme.STAT_CARD_STYLE + " -fx-border-color: " + Theme.COLOR_BORDER_LIGHT + ";");

        Label t = new Label(title);
        t.setStyle("-fx-text-fill: " + accentHex + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 0.5px;");

        valueLabel.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 26px; -fx-font-weight: bold;");

        Label s = new Label(subtitle);
        s.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 12px;");

        card.getChildren().addAll(t, valueLabel, s);
        return card;
    }

    private VBox buildRevenueForecastChartCard() {
        VBox card = new VBox(12);
        card.setStyle(Theme.CARD_STYLE);

        Label title = new Label("Monthly Revenue Trend & 3-Period Predictive Forecast");
        title.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 15px; -fx-font-weight: bold;");

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Timeline Period");
        xAxis.setTickLabelFill(javafx.scene.paint.Color.web(Theme.TEXT_SECONDARY));

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Revenue (" + Theme.CURRENCY_SYMBOL + ")");
        yAxis.setTickLabelFill(javafx.scene.paint.Color.web(Theme.TEXT_SECONDARY));

        revenueChart = new LineChart<>(xAxis, yAxis);
        revenueChart.setAnimated(false);
        revenueChart.setLegendVisible(true);
        revenueChart.setPrefHeight(300);
        revenueChart.setStyle("-fx-background-color: transparent;");

        actualSeries = new XYChart.Series<>();
        actualSeries.setName("Historical Revenue");

        forecastSeries = new XYChart.Series<>();
        forecastSeries.setName("Projected Trend (Moving Avg + Linear Regression)");

        revenueChart.getData().addAll(actualSeries, forecastSeries);

        card.getChildren().addAll(title, revenueChart);
        return card;
    }

    private VBox buildExpenseBreakdownChartCard() {
        VBox card = new VBox(12);
        card.setStyle(Theme.CARD_STYLE);

        Label title = new Label("Operational Expense Breakdown by Category");
        title.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 15px; -fx-font-weight: bold;");

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setTickLabelFill(javafx.scene.paint.Color.web(Theme.TEXT_SECONDARY));

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Amount (" + Theme.CURRENCY_SYMBOL + ")");
        yAxis.setTickLabelFill(javafx.scene.paint.Color.web(Theme.TEXT_SECONDARY));

        expenseChart = new BarChart<>(xAxis, yAxis);
        expenseChart.setAnimated(false);
        expenseChart.setLegendVisible(false);
        expenseChart.setPrefHeight(300);
        expenseChart.setStyle("-fx-background-color: transparent;");

        expenseSeries = new XYChart.Series<>();
        expenseChart.getData().add(expenseSeries);

        card.getChildren().addAll(title, expenseChart);
        return card;
    }

    private VBox buildAlertsSection() {
        VBox card = new VBox(12);
        card.setStyle(Theme.CARD_STYLE);

        Label title = new Label("High-Priority Risk Alerts (Anomaly Surveillance)");
        title.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 15px; -fx-font-weight: bold;");

        alertsContainer = new VBox(10);
        card.getChildren().addAll(title, alertsContainer);
        return card;
    }

    private VBox buildRecommendationsSection() {
        VBox card = new VBox(12);
        card.setStyle(Theme.CARD_STYLE);

        Label title = new Label("Prescriptive Decision Engine (AI Action Items)");
        title.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 15px; -fx-font-weight: bold;");

        recommendationsContainer = new VBox(10);
        card.getChildren().addAll(title, recommendationsContainer);
        return card;
    }

    public void updateData(AnalyticsSnapshot snapshot) {
        // 1. Update KPIs
        lblTotalRevenue.setText(String.format("%s %,.2f", Theme.CURRENCY_SYMBOL, snapshot.totalRevenue()));
        lblTotalExpenses.setText(String.format("%s %,.2f", Theme.CURRENCY_SYMBOL, snapshot.totalExpenses()));
        lblNetProfit.setText(String.format("%s %,.2f", Theme.CURRENCY_SYMBOL, snapshot.netProfit()));
        if (snapshot.netProfit() < 0) {
            lblNetProfit.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 26px; -fx-font-weight: bold;");
        } else {
            lblNetProfit.setStyle("-fx-text-fill: #10B981; -fx-font-size: 26px; -fx-font-weight: bold;");
        }

        long criticalCount = snapshot.alerts().stream().filter(a -> a.severity() == Alert.Severity.CRITICAL).count();
        lblActiveAlerts.setText(snapshot.alerts().size() + " (" + criticalCount + " Critical)");

        // 2. Update Growth Score Hero
        GrowthScoreResult gs = snapshot.growthScore();
        growthScoreValueLabel.setText(String.valueOf(gs.overallScore()));
        healthBandBadge.setText(gs.healthBand().toUpperCase());
        healthBandBadge.setStyle(Theme.getBadgeStyle("rgba(56, 189, 248, 0.15)", gs.healthColorHex()));
        insightLabel.setText(gs.summaryInsight());

        lblRevenueScore.setText(String.format("30%% Revenue Trend: %.0f/100", gs.revenueScore()));
        pbRevenue.setProgress(gs.revenueScore() / 100.0);

        lblExpenseScore.setText(String.format("25%% Expense Control: %.0f/100", gs.expenseScore()));
        pbExpense.setProgress(gs.expenseScore() / 100.0);

        lblInventoryScore.setText(String.format("25%% Inventory Health: %.0f/100", gs.inventoryScore()));
        pbInventory.setProgress(gs.inventoryScore() / 100.0);

        lblCustomerScore.setText(String.format("20%% Customer Retention: %.0f/100", gs.customerScore()));
        pbCustomer.setProgress(gs.customerScore() / 100.0);

        // 3. Update Line Chart
        actualSeries.getData().clear();
        forecastSeries.getData().clear();

        for (ForecastPoint pt : snapshot.forecastPoints()) {
            if (!pt.isProjected() && pt.actualRevenue() != null) {
                actualSeries.getData().add(new XYChart.Data<>(pt.periodLabel(), pt.actualRevenue()));
            } else if (pt.isProjected() && pt.forecastedRevenue() != null) {
                forecastSeries.getData().add(new XYChart.Data<>(pt.periodLabel(), pt.forecastedRevenue()));
            }
        }

        // 4. Update Expense Bar Chart
        expenseSeries.getData().clear();
        for (Map.Entry<String, Double> entry : snapshot.expenseByCategory().entrySet()) {
            expenseSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        // 5. Update Risk Alerts Preview
        alertsContainer.getChildren().clear();
        List<Alert> topAlerts = snapshot.alerts().stream().limit(4).toList();
        if (topAlerts.isEmpty()) {
            Label noAlerts = new Label("All surveillance parameters healthy. No anomalies detected.");
            noAlerts.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 13px;");
            alertsContainer.getChildren().add(noAlerts);
        } else {
            for (Alert a : topAlerts) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color: " + Theme.COLOR_BG_INPUT + "; -fx-padding: 10 14 10 14; -fx-background-radius: 8px;");

                String badgeBg = a.severity() == Alert.Severity.CRITICAL ? "#DC2626" :
                                 a.severity() == Alert.Severity.WARNING ? "#D97706" : "#0284C7";
                Label sevBadge = new Label(a.severity().name());
                sevBadge.setStyle(Theme.getBadgeStyle(badgeBg, "#FFFFFF"));

                VBox textBox = new VBox(2);
                Label t = new Label(a.title());
                t.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-weight: bold; -fx-font-size: 13px;");
                Label d = new Label(a.description());
                d.setWrapText(true);
                d.setStyle("-fx-text-fill: " + Theme.TEXT_SECONDARY + "; -fx-font-size: 11px;");
                textBox.getChildren().addAll(t, d);
                HBox.setHgrow(textBox, Priority.ALWAYS);

                row.getChildren().addAll(sevBadge, textBox);
                alertsContainer.getChildren().add(row);
            }
        }

        // 6. Update Recommendations
        recommendationsContainer.getChildren().clear();
        List<Recommendation> topRecs = snapshot.recommendations().stream().limit(4).toList();
        if (topRecs.isEmpty()) {
            Label noRec = new Label("Operations running within target efficiency bounds.");
            noRec.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 13px;");
            recommendationsContainer.getChildren().add(noRec);
        } else {
            for (Recommendation r : topRecs) {
                VBox recBox = new VBox(4);
                recBox.setStyle("-fx-background-color: " + Theme.COLOR_BG_INPUT + "; -fx-padding: 12 14 12 14; -fx-background-radius: 8px; -fx-border-color: rgba(56, 189, 248, 0.2); -fx-border-radius: 8px;");

                HBox recHeader = new HBox(8);
                recHeader.setAlignment(Pos.CENTER_LEFT);

                Label prioBadge = new Label(r.priority().name());
                prioBadge.setStyle(Theme.getBadgeStyle("#3B82F6", "#FFFFFF"));

                Label category = new Label(r.category().toUpperCase());
                category.setStyle("-fx-text-fill: " + Theme.COLOR_ACCENT_PRIMARY + "; -fx-font-size: 10px; -fx-font-weight: bold;");

                recHeader.getChildren().addAll(prioBadge, category);

                Label t = new Label(r.title());
                t.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-weight: bold; -fx-font-size: 13px;");

                Label d = new Label(r.actionText());
                d.setWrapText(true);
                d.setStyle("-fx-text-fill: " + Theme.TEXT_SECONDARY + "; -fx-font-size: 12px;");

                Label impact = new Label("Impact: " + r.expectedImpact());
                impact.setStyle("-fx-text-fill: " + Theme.COLOR_SUCCESS + "; -fx-font-size: 11px; -fx-font-weight: bold;");

                recBox.getChildren().addAll(recHeader, t, d, impact);
                recommendationsContainer.getChildren().add(recBox);
            }
        }
    }

    public void showLoading(boolean show) {
        loadingOverlay.setVisible(show);
    }

    public void showError(String msg) {
        errorBanner.setText(msg);
        errorBanner.setVisible(true);
        errorBanner.setManaged(true);
    }

    public void refresh() {
        errorBanner.setVisible(false);
        errorBanner.setManaged(false);
        controller.loadDashboardData();
    }
}
