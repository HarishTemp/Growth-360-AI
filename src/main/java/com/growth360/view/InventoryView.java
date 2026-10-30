package com.growth360.view;

import com.growth360.analytics.ReorderPointCalculator.InventoryMetric;
import com.growth360.analytics.ReorderPointCalculator.InventoryStatus;
import com.growth360.controller.InventoryController;
import com.growth360.model.Product;
import com.growth360.util.Theme;
import com.growth360.util.Validator;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

/**
 * Pure Java JavaFX Smart Inventory view displaying ROP, runway days,
 * suggested reorder quantities, and interactive SKU replenishment.
 */
public class InventoryView extends VBox {

    private final InventoryController controller;
    private final ObservableList<InventoryMetric> inventoryMasterList = FXCollections.observableArrayList();
    private FilteredList<InventoryMetric> filteredMetrics;

    private Label lblTotalSkus;
    private Label lblLowStockSkus;
    private Label lblCapitalTied;
    private Label lblHealthyShare;
    private Label notificationBanner;
    private TableView<InventoryMetric> tableView;
    private TextField searchField;
    private ComboBox<String> statusFilter;

    public InventoryView(InventoryController controller) {
        this.controller = controller;
        this.controller.setView(this);

        buildUI();
        this.controller.loadInventory();
    }

    private void buildUI() {
        setSpacing(20);
        setPadding(new Insets(24, 28, 32, 28));
        setStyle("-fx-background-color: " + Theme.COLOR_BG_APP + ";");

        // Header
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Titan Timepiece Inventory & Reorder Intelligence (ROP)");
        title.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label subtitle = new Label("Reorder Point (ROP) algorithm, boutique stock runway, and horology supply chain replenishment");
        subtitle.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 13px;");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNewProduct = new Button("+ Add New SKU");
        btnNewProduct.setStyle(Theme.BTN_PRIMARY);
        btnNewProduct.setOnAction(e -> openProductDialog(null));

        header.getChildren().addAll(titleBox, spacer, btnNewProduct);

        // Notification Banner
        notificationBanner = new Label();
        notificationBanner.setVisible(false);
        notificationBanner.setManaged(false);

        // Stats Row
        HBox statsRow = new HBox(16);
        lblTotalSkus = new Label("0");
        VBox card1 = createStatCard("TOTAL MANAGED SKUS", lblTotalSkus, "#38BDF8");

        lblLowStockSkus = new Label("0");
        VBox card2 = createStatCard("REORDER / CRITICAL ITEMS", lblLowStockSkus, "#EF4444");

        lblCapitalTied = new Label("$0.00");
        VBox card3 = createStatCard("HOLDING CAPITAL TIED UP", lblCapitalTied, "#F59E0B");

        lblHealthyShare = new Label("0%");
        VBox card4 = createStatCard("OPTIMAL STOCK SHARE", lblHealthyShare, "#10B981");

        statsRow.getChildren().addAll(card1, card2, card3, card4);
        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);
        HBox.setHgrow(card4, Priority.ALWAYS);

        // Filter / Search Toolbar
        HBox toolbar = new HBox(14);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("🔍 Search product or category...");
        searchField.setStyle(Theme.TEXT_INPUT);
        searchField.setPrefWidth(280);
        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilter());

        statusFilter = new ComboBox<>(FXCollections.observableArrayList(
                "All Statuses",
                "Out of Stock",
                "Reorder Urgently",
                "Reorder Needed",
                "Optimal Stock",
                "Overstocked"
        ));
        statusFilter.setValue("All Statuses");
        statusFilter.setStyle(Theme.TEXT_INPUT);
        statusFilter.setOnAction(e -> applyFilter());

        toolbar.getChildren().addAll(searchField, statusFilter);

        // Table
        tableView = new TableView<>();
        tableView.setStyle(Theme.TABLE_VIEW);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        TableColumn<InventoryMetric, String> colName = new TableColumn<>("Product SKU");
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().product().getName()));

        TableColumn<InventoryMetric, String> colCat = new TableColumn<>("Category");
        colCat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().product().getCategory()));

        TableColumn<InventoryMetric, String> colStock = new TableColumn<>("Current Stock");
        colStock.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().product().getStockQty() + " units"));
        colStock.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<InventoryMetric, String> colPrice = new TableColumn<>("Unit Price");
        colPrice.setCellValueFactory(d -> new SimpleStringProperty(String.format("%s %,.2f", Theme.CURRENCY_SYMBOL, d.getValue().product().getUnitPrice())));
        colPrice.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<InventoryMetric, String> colDemand = new TableColumn<>("Daily Demand");
        colDemand.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.2f /day", d.getValue().dailyDemand())));
        colDemand.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<InventoryMetric, String> colRop = new TableColumn<>("ROP (Reorder Point)");
        colRop.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.1f units", d.getValue().reorderPoint())));
        colRop.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");

        TableColumn<InventoryMetric, String> colRunway = new TableColumn<>("Stock Runway");
        colRunway.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.0f days", d.getValue().stockRunwayDays())));
        colRunway.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<InventoryMetric, String> colSuggested = new TableColumn<>("Suggested Reorder");
        colSuggested.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().suggestedReorderQty() > 0 ?
                "+" + d.getValue().suggestedReorderQty() + " units" : "0"));
        colSuggested.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");

        TableColumn<InventoryMetric, InventoryStatus> colStatus = new TableColumn<>("Health Status");
        colStatus.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().status()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(InventoryStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(status.getLabel());
                    badge.setStyle(Theme.getBadgeStyle("rgba(255,255,255,0.08)", status.getColorHex()) +
                            " -fx-border-color: " + status.getColorHex() + "; -fx-border-radius: 20px;");
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        TableColumn<InventoryMetric, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(190);
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnRestock = new Button("Restock");
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Del");
            private final HBox btnBox = new HBox(6, btnRestock, btnEdit, btnDelete);

            {
                btnBox.setAlignment(Pos.CENTER);
                btnRestock.setStyle(Theme.BTN_SUCCESS + " -fx-font-size: 10px; -fx-padding: 3 8 3 8;");
                btnEdit.setStyle(Theme.BTN_SECONDARY + " -fx-font-size: 10px; -fx-padding: 3 8 3 8;");
                btnDelete.setStyle(Theme.BTN_DANGER + " -fx-font-size: 10px; -fx-padding: 3 8 3 8;");

                btnRestock.setOnAction(e -> {
                    InventoryMetric m = getTableView().getItems().get(getIndex());
                    if (m != null) openRestockDialog(m);
                });

                btnEdit.setOnAction(e -> {
                    InventoryMetric m = getTableView().getItems().get(getIndex());
                    if (m != null) openProductDialog(m.product());
                });

                btnDelete.setOnAction(e -> {
                    InventoryMetric m = getTableView().getItems().get(getIndex());
                    if (m != null) controller.deleteProduct(m.product().getId());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnBox);
                setAlignment(Pos.CENTER);
            }
        });

        tableView.getColumns().addAll(colName, colCat, colStock, colPrice, colDemand, colRop, colRunway, colSuggested, colStatus, colActions);

        filteredMetrics = new FilteredList<>(inventoryMasterList, p -> true);
        tableView.setItems(filteredMetrics);

        getChildren().addAll(header, notificationBanner, statsRow, toolbar, tableView);
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

    private void applyFilter() {
        String query = searchField.getText();
        String status = statusFilter.getValue();

        filteredMetrics.setPredicate(metric -> {
            boolean matchesStatus = status == null || "All Statuses".equals(status) ||
                    metric.status().getLabel().equalsIgnoreCase(status);
            boolean matchesQuery = query == null || query.isBlank() ||
                    metric.product().getName().toLowerCase().contains(query.toLowerCase()) ||
                    metric.product().getCategory().toLowerCase().contains(query.toLowerCase());
            return matchesStatus && matchesQuery;
        });
    }

    public void setInventoryData(List<InventoryMetric> metrics) {
        inventoryMasterList.setAll(metrics);

        int total = metrics.size();
        long lowStock = metrics.stream().filter(m ->
                m.status() == InventoryStatus.OUT_OF_STOCK ||
                m.status() == InventoryStatus.CRITICAL_REORDER ||
                m.status() == InventoryStatus.REORDER_SOON
        ).count();

        double capital = metrics.stream().mapToDouble(m -> m.product().getStockQty() * m.product().getUnitCost()).sum();
        long healthy = metrics.stream().filter(m -> m.status() == InventoryStatus.HEALTHY).count();
        double healthyPct = total > 0 ? ((double) healthy / total * 100.0) : 0.0;

        lblTotalSkus.setText(String.format("%,d SKUs", total));
        lblLowStockSkus.setText(String.format("%d Items", lowStock));
        lblCapitalTied.setText(String.format("%s %,.2f", Theme.CURRENCY_SYMBOL, capital));
        lblHealthyShare.setText(String.format("%.0f%%", healthyPct));
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

    private void openRestockDialog(InventoryMetric metric) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Restock Inventory - " + metric.product().getName());

        VBox layout = new VBox(16);
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: " + Theme.COLOR_BG_CARD + ";");

        Label title = new Label("Replenish Inventory: " + metric.product().getName());
        title.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label info = new Label(String.format("Current Stock: %d units | Suggested PO: +%d units",
                metric.product().getStockQty(), Math.max(10, metric.suggestedReorderQty())));
        info.setStyle("-fx-text-fill: " + Theme.COLOR_ACCENT_PRIMARY + "; -fx-font-size: 12px;");

        TextField tfRestockQty = new TextField(String.valueOf(Math.max(10, metric.suggestedReorderQty())));
        tfRestockQty.setStyle(Theme.TEXT_INPUT);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancel = new Button("Cancel");
        btnCancel.setStyle(Theme.BTN_SECONDARY);
        btnCancel.setOnAction(e -> dialog.close());

        Button btnConfirm = new Button("Confirm Restock");
        btnConfirm.setStyle(Theme.BTN_SUCCESS);
        btnConfirm.setOnAction(e -> {
            try {
                int qty = Integer.parseInt(tfRestockQty.getText().trim());
                if (qty <= 0) {
                    showError("Restock quantity must be positive.");
                    return;
                }
                controller.restockProduct(metric.product().getId(), qty, dialog::close);
            } catch (NumberFormatException ex) {
                showError("Please enter a valid numeric integer.");
            }
        });

        actions.getChildren().addAll(btnCancel, btnConfirm);
        layout.getChildren().addAll(title, info, new Label("Units to Receive:"), tfRestockQty, actions);

        Scene scene = new Scene(layout, 420, 260);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void openProductDialog(Product existing) {
        boolean isEdit = existing != null;
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(isEdit ? "Edit Product SKU" : "Register New Product SKU");

        VBox layout = new VBox(14);
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: " + Theme.COLOR_BG_CARD + ";");

        Label dTitle = new Label(isEdit ? "Edit Product SKU" : "Register New Product SKU");
        dTitle.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 18px; -fx-font-weight: bold;");

        TextField tfName = new TextField(isEdit ? existing.getName() : "");
        tfName.setPromptText("Product Name");
        tfName.setStyle(Theme.TEXT_INPUT);

        TextField tfCategory = new TextField(isEdit ? existing.getCategory() : "");
        tfCategory.setPromptText("Category (e.g. Electronics)");
        tfCategory.setStyle(Theme.TEXT_INPUT);

        TextField tfStock = new TextField(isEdit ? String.valueOf(existing.getStockQty()) : "50");
        tfStock.setPromptText("Stock Quantity");
        tfStock.setStyle(Theme.TEXT_INPUT);

        TextField tfCost = new TextField(isEdit ? String.valueOf(existing.getUnitCost()) : "25.00");
        tfCost.setPromptText("Unit Cost ($)");
        tfCost.setStyle(Theme.TEXT_INPUT);

        TextField tfPrice = new TextField(isEdit ? String.valueOf(existing.getUnitPrice()) : "59.99");
        tfPrice.setPromptText("Unit Selling Price ($)");
        tfPrice.setStyle(Theme.TEXT_INPUT);

        TextField tfLeadTime = new TextField(isEdit ? String.valueOf(existing.getLeadTimeDays()) : "7");
        tfLeadTime.setPromptText("Supplier Lead Time (Days)");
        tfLeadTime.setStyle(Theme.TEXT_INPUT);

        TextField tfSafety = new TextField(isEdit ? String.valueOf(existing.getSafetyStock()) : "10");
        tfSafety.setPromptText("Safety Stock (Buffer Units)");
        tfSafety.setStyle(Theme.TEXT_INPUT);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancel = new Button("Cancel");
        btnCancel.setStyle(Theme.BTN_SECONDARY);
        btnCancel.setOnAction(e -> dialog.close());

        Button btnSave = new Button(isEdit ? "Update SKU" : "Create SKU");
        btnSave.setStyle(Theme.BTN_PRIMARY);
        btnSave.setOnAction(e -> {
            if (!Validator.isNotEmpty(tfName.getText()) || !Validator.isNotEmpty(tfCategory.getText())) {
                showError("Name and category cannot be empty.");
                return;
            }
            if (!Validator.isValidNonNegativeInt(tfStock.getText()) ||
                !Validator.isValidPositiveDouble(tfCost.getText()) ||
                !Validator.isValidPositiveDouble(tfPrice.getText()) ||
                !Validator.isValidPositiveInt(tfLeadTime.getText()) ||
                !Validator.isValidNonNegativeInt(tfSafety.getText())) {
                showError("Please enter valid numeric figures for inventory, pricing, and lead times.");
                return;
            }

            int stock = Integer.parseInt(tfStock.getText().trim());
            double cost = Double.parseDouble(tfCost.getText().trim());
            double price = Double.parseDouble(tfPrice.getText().trim());
            int lead = Integer.parseInt(tfLeadTime.getText().trim());
            int safety = Integer.parseInt(tfSafety.getText().trim());

            if (isEdit) {
                existing.setName(tfName.getText().trim());
                existing.setCategory(tfCategory.getText().trim());
                existing.setStockQty(stock);
                existing.setUnitCost(cost);
                existing.setUnitPrice(price);
                existing.setLeadTimeDays(lead);
                existing.setSafetyStock(safety);
                controller.updateProduct(existing, dialog::close);
            } else {
                Product p = new Product(tfName.getText().trim(), tfCategory.getText().trim(), stock, cost, price, lead, safety);
                controller.createProduct(p, dialog::close);
            }
        });

        actions.getChildren().addAll(btnCancel, btnSave);

        layout.getChildren().addAll(
                dTitle,
                new Label("Product Name:"), tfName,
                new Label("Category:"), tfCategory,
                new Label("Initial Stock Qty:"), tfStock,
                new Label("Unit Cost ($):"), tfCost,
                new Label("Unit Selling Price ($):"), tfPrice,
                new Label("Supplier Lead Time (Days):"), tfLeadTime,
                new Label("Safety Stock Buffer:"), tfSafety,
                actions
        );

        ScrollPane sp = new ScrollPane(layout);
        sp.setFitToWidth(true);
        Scene scene = new Scene(sp, 480, 560);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
