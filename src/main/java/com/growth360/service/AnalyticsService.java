package com.growth360.service;

import com.growth360.analytics.*;
import com.growth360.analytics.GrowthScoreCalculator.GrowthScoreResult;
import com.growth360.analytics.ReorderPointCalculator.InventoryMetric;
import com.growth360.model.*;
import javafx.application.Platform;

import java.sql.SQLException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Orchestrator service that fetches raw business data and coordinates
 * all analytical engines on background threads, ensuring the JavaFX UI remains buttery smooth.
 */
public class AnalyticsService {

    private final ProductService productService;
    private final CustomerService customerService;
    private final SaleService saleService;
    private final ExpenseService expenseService;

    private final ZScoreDetector zScoreDetector;
    private final ReorderPointCalculator ropCalculator;
    private final RfmSegmenter rfmSegmenter;
    private final RevenueForecaster revenueForecaster;
    private final GrowthScoreCalculator growthScoreCalculator;
    private final RecommendationEngine recommendationEngine;

    private final ExecutorService executorService;

    public record AnalyticsSnapshot(
            double totalRevenue,
            double totalExpenses,
            double netProfit,
            GrowthScoreResult growthScore,
            List<InventoryMetric> inventoryMetrics,
            List<RfmResult> rfmResults,
            List<ForecastPoint> forecastPoints,
            List<Alert> alerts,
            List<Recommendation> recommendations,
            Map<String, Double> monthlyRevenue,
            Map<String, Double> expenseByCategory
    ) {}

    public AnalyticsService() {
        this(new ProductService(), new CustomerService(), new SaleService(), new ExpenseService());
    }

    public AnalyticsService(ProductService productService, CustomerService customerService,
                            SaleService saleService, ExpenseService expenseService) {
        this.productService = productService;
        this.customerService = customerService;
        this.saleService = saleService;
        this.expenseService = expenseService;

        this.zScoreDetector = new ZScoreDetector();
        this.ropCalculator = new ReorderPointCalculator();
        this.rfmSegmenter = new RfmSegmenter();
        this.revenueForecaster = new RevenueForecaster();
        this.growthScoreCalculator = new GrowthScoreCalculator();
        this.recommendationEngine = new RecommendationEngine();

        this.executorService = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "AnalyticsWorkerThread");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Executes the end-to-end analytical pipeline synchronously.
     */
    public AnalyticsSnapshot computeSnapshot() throws SQLException {
        // 1. Fetch raw data
        List<Product> products = productService.getAllProducts();
        List<Customer> customers = customerService.getAllCustomers();
        List<Sale> sales = saleService.getAllSales();
        List<Expense> expenses = expenseService.getAllExpenses();

        double totalRevenue = sales.stream().mapToDouble(Sale::getAmount).sum();
        double totalExpenses = expenses.stream().mapToDouble(Expense::getAmount).sum();
        double netProfit = totalRevenue - totalExpenses;

        // 2. Inventory analytics
        List<InventoryMetric> inventoryMetrics = ropCalculator.calculateMetrics(products, sales);
        List<Alert> invAlerts = ropCalculator.generateInventoryAlerts(inventoryMetrics);

        // 3. Customer RFM analytics
        List<RfmResult> rfmResults = rfmSegmenter.segmentCustomers(customers, sales);
        List<Alert> custAlerts = rfmSegmenter.generateCustomerAlerts(rfmResults);

        // 4. Time-series aggregation for Z-Score Anomaly Detection
        DateTimeFormatter ymFmt = DateTimeFormatter.ofPattern("MMM yyyy");
        Map<String, Double> monthlyRevenue = sales.stream()
                .collect(Collectors.groupingBy(
                        s -> YearMonth.from(s.getSaleDate()).format(ymFmt),
                        TreeMap::new,
                        Collectors.summingDouble(Sale::getAmount)
                ));

        Map<String, Double> expenseByCategory = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        TreeMap::new,
                        Collectors.summingDouble(Expense::getAmount)
                ));

        List<Alert> revAlerts = zScoreDetector.createRevenueAlerts(monthlyRevenue);
        List<Alert> expAlerts = zScoreDetector.createExpenseAlerts(expenseByCategory);

        // 5. Section 21: PriorityQueue<Alert> for prioritized alert triage (CRITICAL -> WARNING -> INFO)
        PriorityQueue<Alert> alertPriorityQueue = new PriorityQueue<>((a, b) -> {
            int sevCompare = Integer.compare(severityRank(b.severity()), severityRank(a.severity()));
            return sevCompare != 0 ? sevCompare : b.timestamp().compareTo(a.timestamp());
        });
        alertPriorityQueue.addAll(invAlerts);
        alertPriorityQueue.addAll(expAlerts);
        alertPriorityQueue.addAll(revAlerts);
        alertPriorityQueue.addAll(custAlerts);

        List<Alert> allAlerts = new ArrayList<>();
        while (!alertPriorityQueue.isEmpty()) {
            allAlerts.add(alertPriorityQueue.poll());
        }

        // 6. Revenue Forecasting
        List<ForecastPoint> forecastPoints = revenueForecaster.forecastMonthlyRevenue(sales);

        // 7. Growth Score Calculation (5-factor formula)
        GrowthScoreResult growthScore = growthScoreCalculator.calculateScore(sales, expenses, inventoryMetrics, rfmResults);

        // 8. Prescriptive Recommendations
        List<Recommendation> recommendations = recommendationEngine.generateRecommendations(allAlerts);

        return new AnalyticsSnapshot(
                totalRevenue,
                totalExpenses,
                netProfit,
                growthScore,
                inventoryMetrics,
                rfmResults,
                forecastPoints,
                allAlerts,
                recommendations,
                monthlyRevenue,
                expenseByCategory
        );
    }

    /**
     * Section 41: Assembles the MetricContext record payload for executive reporting
     * and the AI Copilot engine.
     */
    public MetricContext computeMetricContext() throws SQLException {
        AnalyticsSnapshot snapshot = computeSnapshot();

        String topProduct = snapshot.inventoryMetrics().isEmpty() ? "N/A" :
                snapshot.inventoryMetrics().stream()
                        .max(Comparator.comparingDouble(m -> m.dailyDemand() * m.product().getUnitPrice()))
                        .map(m -> m.product().getName())
                        .orElse("N/A");

        int atRiskCount = (int) snapshot.rfmResults().stream()
                .filter(r -> r.segment() == RfmResult.Segment.AT_RISK || r.segment() == RfmResult.Segment.LOST)
                .count();

        int activeCount = (int) snapshot.rfmResults().stream()
                .filter(r -> r.recencyDays() <= 60)
                .count();

        List<InventoryMetric> inventoryRisks = snapshot.inventoryMetrics().stream()
                .filter(m -> m.status() == ReorderPointCalculator.InventoryStatus.OUT_OF_STOCK ||
                             m.status() == ReorderPointCalculator.InventoryStatus.CRITICAL_REORDER ||
                             m.status() == ReorderPointCalculator.InventoryStatus.REORDER_SOON)
                .toList();

        double profitMargin = snapshot.totalRevenue() > 0 ? (snapshot.netProfit() / snapshot.totalRevenue()) * 100.0 : 0.0;
        double momGrowth = snapshot.growthScore().salesVelocity();

        return new MetricContext(
                YearMonth.now().format(DateTimeFormatter.ofPattern("MMM yyyy")),
                snapshot.totalRevenue(),
                momGrowth,
                snapshot.totalExpenses(),
                snapshot.netProfit(),
                Math.round(profitMargin * 10.0) / 10.0,
                snapshot.growthScore().toGrowthScore(),
                inventoryRisks,
                topProduct,
                activeCount,
                atRiskCount,
                snapshot.alerts(),
                snapshot.recommendations()
        );
    }

    private int severityRank(Alert.Severity severity) {
        return switch (severity) {
            case CRITICAL -> 3;
            case WARNING -> 2;
            case INFO -> 1;
        };
    }

    /**
     * Executes the analytics pipeline on a background thread and delivers the result
     * safely onto the JavaFX Application Thread.
     */
    public void computeSnapshotAsync(Consumer<AnalyticsSnapshot> onSuccess, Consumer<Throwable> onError) {
        executorService.submit(() -> {
            try {
                AnalyticsSnapshot snapshot = computeSnapshot();
                Platform.runLater(() -> onSuccess.accept(snapshot));
            } catch (Throwable ex) {
                Platform.runLater(() -> onError.accept(ex));
            }
        });
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
