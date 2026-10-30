package com.growth360.view;

import com.growth360.controller.*;
import com.growth360.service.*;
import com.growth360.util.DBConnection;
import com.growth360.util.Theme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Main application window layout containing the left navigation sidebar,
 * top global header, and dynamic center content viewport.
 */
public class MainView extends BorderPane {

    private final ProductService productService;
    private final CustomerService customerService;
    private final SaleService saleService;
    private final ExpenseService expenseService;
    private final AnalyticsService analyticsService;

    // Controllers
    private final DashboardController dashboardController;
    private final SalesController salesController;
    private final ExpenseController expenseController;
    private final InventoryController inventoryController;
    private final CustomerController customerController;
    private final AlertController alertController;

    // Views
    private DashboardView dashboardView;
    private SalesView salesView;
    private ExpenseView expenseView;
    private InventoryView inventoryView;
    private CustomerView customerView;
    private AlertsView alertsView;
    private BusinessInsightsView businessInsightsView;

    // UI elements
    private StackPane contentArea;
    private Label topPageTitle;
    private Label topBreadcrumb;
    private final List<Button> navButtons = new ArrayList<>();
    private Button activeNavBtn = null;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("EEE, MMM dd • HH:mm");

    public MainView() {
        // Instantiate Services
        this.productService = new ProductService();
        this.customerService = new CustomerService();
        this.saleService = new SaleService();
        this.expenseService = new ExpenseService();
        this.analyticsService = new AnalyticsService(productService, customerService, saleService, expenseService);

        // Instantiate Controllers
        this.dashboardController = new DashboardController(analyticsService);
        this.salesController = new SalesController(saleService, customerService, productService);
        this.expenseController = new ExpenseController(expenseService);
        this.inventoryController = new InventoryController(productService, saleService);
        this.customerController = new CustomerController(customerService, saleService);
        this.alertController = new AlertController(analyticsService);

        buildUI();
    }

    private void buildUI() {
        setStyle(Theme.ROOT_STYLE);

        // 1. Sidebar (Left)
        VBox sidebar = buildSidebar();
        setLeft(sidebar);

        // 2. Center Content Wrapper
        BorderPane centerLayout = new BorderPane();

        // 3. Header (Top of Center)
        HBox header = buildHeader();
        centerLayout.setTop(header);

        // 4. Dynamic Content Area
        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: " + Theme.COLOR_BG_APP + ";");
        centerLayout.setCenter(contentArea);

        setCenter(centerLayout);

        // Default to Dashboard
        if (!navButtons.isEmpty()) {
            navButtons.get(0).fire();
        }
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(18);
        sidebar.setPrefWidth(260);
        sidebar.setPadding(new Insets(24, 16, 24, 16));
        sidebar.setStyle(Theme.SIDEBAR_STYLE);

        // Logo & Brand
        VBox brandBox = new VBox(4);
        brandBox.setPadding(new Insets(0, 8, 12, 8));

        HBox logoRow = new HBox(10);
        logoRow.setAlignment(Pos.CENTER_LEFT);

        Label logoIcon = new Label("⌚");
        logoIcon.setStyle("-fx-font-size: 24px; -fx-text-fill: " + Theme.COLOR_ACCENT_PRIMARY + ";");

        Label brandTitle = new Label("TITAN WATCHES");
        brandTitle.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 17px; -fx-font-weight: 800; -fx-letter-spacing: 0.5px;");

        logoRow.getChildren().addAll(logoIcon, brandTitle);

        Label tagline = new Label("Be More • Precision Horology");
        tagline.setStyle("-fx-text-fill: " + Theme.COLOR_ACCENT_PRIMARY + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        brandBox.getChildren().addAll(logoRow, tagline);

        // Navigation Items
        VBox navList = new VBox(6);

        Button btnDashboard = createNavButton("📊  Executive Dashboard", () -> showDashboard());
        Button btnSales = createNavButton("📈  Boutique Sales", () -> showSales());
        Button btnExpenses = createNavButton("💳  Overheads & Burn", () -> showExpenses());
        Button btnInventory = createNavButton("📦  Watch Inventory (ROP)", () -> showInventory());
        Button btnCustomers = createNavButton("👥  Encircle Loyalty (RFM)", () -> showCustomers());
        Button btnAlerts = createNavButton("⚠️  Horology Risk Alerts", () -> showAlerts());
        Button btnInsights = createNavButton("💡  Business Insights", () -> showBusinessInsights());

        navList.getChildren().addAll(btnDashboard, btnSales, btnExpenses, btnInventory, btnCustomers, btnAlerts, btnInsights);
        VBox.setVgrow(navList, Priority.ALWAYS);

        // Footer / Database Engine Status
        VBox footer = new VBox(6);
        footer.setPadding(new Insets(12, 8, 4, 8));
        footer.setStyle("-fx-border-color: " + Theme.COLOR_BORDER + "; -fx-border-width: 1px 0 0 0;");

        Label dbLabel = new Label("DATABASE ENGINE");
        dbLabel.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 10px; -fx-font-weight: bold;");

        Label dbEngine = new Label(DBConnection.getActiveEngineName());
        dbEngine.setWrapText(true);
        dbEngine.setStyle(Theme.getBadgeStyle("rgba(16, 185, 129, 0.15)", Theme.COLOR_SUCCESS));

        Label teamBadge = new Label("Titan Company Ltd. • World of Titan");
        teamBadge.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 11px;");

        footer.getChildren().addAll(dbLabel, dbEngine, teamBadge);

        sidebar.getChildren().addAll(brandBox, navList, footer);
        return sidebar;
    }

    private Button createNavButton(String label, Runnable onAction) {
        Button btn = new Button(label);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(Theme.NAV_BUTTON_DEFAULT);

        btn.setOnAction(e -> {
            setActiveNavButton(btn);
            onAction.run();
        });

        navButtons.add(btn);
        return btn;
    }

    private void setActiveNavButton(Button btn) {
        if (activeNavBtn != null) {
            activeNavBtn.setStyle(Theme.NAV_BUTTON_DEFAULT);
        }
        activeNavBtn = btn;
        activeNavBtn.setStyle(Theme.NAV_BUTTON_ACTIVE);
    }

    private HBox buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(Theme.HEADER_STYLE);

        VBox titleBox = new VBox(2);
        topBreadcrumb = new Label("PORTAL / EXECUTIVE");
        topBreadcrumb.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 10px; -fx-font-weight: bold;");

        topPageTitle = new Label("Executive Dashboard");
        topPageTitle.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 18px; -fx-font-weight: bold;");

        titleBox.getChildren().addAll(topBreadcrumb, topPageTitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label clockLabel = new Label(LocalDateTime.now().format(TIME_FMT));
        clockLabel.setStyle("-fx-text-fill: " + Theme.TEXT_SECONDARY + "; -fx-font-size: 12px;");

        Button btnRefresh = new Button("🔄 Refresh Analytics");
        btnRefresh.setStyle(Theme.BTN_SECONDARY);
        btnRefresh.setOnAction(e -> refreshCurrentView());

        header.getChildren().addAll(titleBox, spacer, clockLabel, btnRefresh);
        return header;
    }

    private void setView(Node view, String breadcrumb, String title) {
        topBreadcrumb.setText(breadcrumb);
        topPageTitle.setText(title);
        contentArea.getChildren().setAll(view);
    }

    private void showDashboard() {
        if (dashboardView == null) {
            dashboardView = new DashboardView(dashboardController);
        } else {
            dashboardController.loadDashboardData();
        }
        setView(dashboardView, "PORTAL / EXECUTIVE", "Executive Dashboard & Telemetry");
    }

    private void showSales() {
        if (salesView == null) {
            salesView = new SalesView(salesController);
        } else {
            salesController.loadSales();
        }
        setView(salesView, "COMMERCIAL / TRANSACTIONS", "Sales & Commercial Transactions");
    }

    private void showExpenses() {
        if (expenseView == null) {
            expenseView = new ExpenseView(expenseController);
        } else {
            expenseController.loadExpenses();
        }
        setView(expenseView, "FINANCIAL / OVERHEADS", "Operational & Overhead Expenses");
    }

    private void showInventory() {
        if (inventoryView == null) {
            inventoryView = new InventoryView(inventoryController);
        } else {
            inventoryController.loadInventory();
        }
        setView(inventoryView, "SUPPLY CHAIN / INVENTORY", "Smart Inventory & Reorder Intelligence (ROP)");
    }

    private void showCustomers() {
        if (customerView == null) {
            customerView = new CustomerView(customerController);
        } else {
            customerController.loadCustomers();
        }
        setView(customerView, "INTELLIGENCE / RFM", "Customer Intelligence & RFM Segmentation");
    }

    private void showAlerts() {
        if (alertsView == null) {
            alertsView = new AlertsView(alertController);
        } else {
            alertController.loadAlerts();
        }
        setView(alertsView, "SURVEILLANCE / DECISION ENGINE", "Risk Alerts & Prescriptive Recommendations");
    }

    private void showBusinessInsights() {
        if (businessInsightsView == null) {
            businessInsightsView = new BusinessInsightsView(analyticsService);
        } else {
            businessInsightsView.loadInsights();
        }
        setView(businessInsightsView, "STRATEGIC / PRESCRIPTIVE", "Business Insights & Decision Support");
    }

    private void refreshCurrentView() {
        if (contentArea.getChildren().isEmpty()) return;
        Node current = contentArea.getChildren().get(0);
        if (current instanceof DashboardView dv) {
            dv.refresh();
        } else if (current instanceof SalesView) {
            salesController.loadSales();
        } else if (current instanceof ExpenseView) {
            expenseController.loadExpenses();
        } else if (current instanceof InventoryView) {
            inventoryController.loadInventory();
        } else if (current instanceof CustomerView) {
            customerController.loadCustomers();
        } else if (current instanceof AlertsView) {
            alertController.loadAlerts();
        } else if (current instanceof BusinessInsightsView biv) {
            biv.loadInsights();
        }
    }
}
