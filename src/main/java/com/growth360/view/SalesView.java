package com.growth360.view;

import com.growth360.controller.SalesController;
import com.growth360.model.Customer;
import com.growth360.model.Product;
import com.growth360.model.Sale;
import com.growth360.util.Theme;
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

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Pure Java JavaFX Sales view with transactional order processing and customer/product lookups.
 */
public class SalesView extends VBox {

    private final SalesController controller;
    private final ObservableList<Sale> salesMasterList = FXCollections.observableArrayList();
    private FilteredList<Sale> filteredSales;

    private Label lblTotalOrders;
    private Label lblGrossRevenue;
    private Label lblAvgOrderValue;
    private Label notificationBanner;
    private TableView<Sale> tableView;
    private TextField searchField;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public SalesView(SalesController controller) {
        this.controller = controller;
        this.controller.setView(this);

        buildUI();
        this.controller.loadSales();
    }

    private void buildUI() {
        setSpacing(20);
        setPadding(new Insets(24, 28, 32, 28));
        setStyle("-fx-background-color: " + Theme.COLOR_BG_APP + ";");

        // Header
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("World of Titan — Boutique Sales & Orders");
        title.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label subtitle = new Label("Point of sale order processing with real-time horology inventory deduction");
        subtitle.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 13px;");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNewSale = new Button("+ Record Transaction");
        btnNewSale.setStyle(Theme.BTN_PRIMARY);
        btnNewSale.setOnAction(e -> openNewSaleDialog());

        header.getChildren().addAll(titleBox, spacer, btnNewSale);

        // Notification Banner
        notificationBanner = new Label();
        notificationBanner.setVisible(false);
        notificationBanner.setManaged(false);

        // Stat Cards Row
        HBox statsRow = new HBox(16);
        lblTotalOrders = new Label("0");
        VBox card1 = createStatCard("TOTAL ORDERS", lblTotalOrders, "#38BDF8");

        lblGrossRevenue = new Label("$0.00");
        VBox card2 = createStatCard("GROSS REVENUE", lblGrossRevenue, "#10B981");

        lblAvgOrderValue = new Label("$0.00");
        VBox card3 = createStatCard("AVG ORDER VALUE (AOV)", lblAvgOrderValue, "#818CF8");

        statsRow.getChildren().addAll(card1, card2, card3);
        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);

        // Filter / Search Toolbar
        HBox toolbar = new HBox(14);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("🔍 Search customer, product, or order ID...");
        searchField.setStyle(Theme.TEXT_INPUT);
        searchField.setPrefWidth(320);
        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilter(newV));

        toolbar.getChildren().addAll(searchField);

        // Table
        tableView = new TableView<>();
        tableView.setStyle(Theme.TABLE_VIEW);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        TableColumn<Sale, String> colId = new TableColumn<>("Order #");
        colId.setCellValueFactory(data -> new SimpleStringProperty("#" + data.getValue().getId()));
        colId.setMaxWidth(90);

        TableColumn<Sale, String> colDate = new TableColumn<>("Date & Time");
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSaleDate().format(DATE_FMT)));

        TableColumn<Sale, String> colCustomer = new TableColumn<>("Customer Name");
        colCustomer.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCustomerName()));

        TableColumn<Sale, String> colProduct = new TableColumn<>("Product Sold");
        colProduct.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName()));

        TableColumn<Sale, String> colQty = new TableColumn<>("Quantity");
        colQty.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getQuantity())));
        colQty.setMaxWidth(100);

        TableColumn<Sale, String> colAmount = new TableColumn<>("Total Amount");
        colAmount.setCellValueFactory(data -> new SimpleStringProperty(String.format("%s %,.2f", Theme.CURRENCY_SYMBOL, data.getValue().getAmount())));
        colAmount.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");

        TableColumn<Sale, Void> colActions = new TableColumn<>("Action");
        colActions.setMaxWidth(100);
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button("Delete");
            {
                btnDelete.setStyle(Theme.BTN_DANGER + " -fx-font-size: 11px; -fx-padding: 4 10 4 10;");
                btnDelete.setOnAction(e -> {
                    Sale sale = getTableView().getItems().get(getIndex());
                    if (sale != null) {
                        controller.deleteSale(sale.getId());
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDelete);
                setAlignment(Pos.CENTER);
            }
        });

        tableView.getColumns().addAll(colId, colDate, colCustomer, colProduct, colQty, colAmount, colActions);

        filteredSales = new FilteredList<>(salesMasterList, p -> true);
        tableView.setItems(filteredSales);

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

    private void applyFilter(String query) {
        if (query == null || query.isBlank()) {
            filteredSales.setPredicate(s -> true);
            return;
        }
        String lower = query.toLowerCase();
        filteredSales.setPredicate(s ->
                String.valueOf(s.getId()).contains(lower) ||
                (s.getCustomerName() != null && s.getCustomerName().toLowerCase().contains(lower)) ||
                (s.getProductName() != null && s.getProductName().toLowerCase().contains(lower))
        );
    }

    public void setSalesData(List<Sale> sales) {
        salesMasterList.setAll(sales);

        int count = sales.size();
        double totalRev = sales.stream().mapToDouble(Sale::getAmount).sum();
        double aov = count > 0 ? (totalRev / count) : 0.0;

        lblTotalOrders.setText(String.format("%,d Orders", count));
        lblGrossRevenue.setText(String.format("%s %,.2f", Theme.CURRENCY_SYMBOL, totalRev));
        lblAvgOrderValue.setText(String.format("%s %,.2f", Theme.CURRENCY_SYMBOL, aov));
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

    private void openNewSaleDialog() {
        try {
            List<Customer> customers = controller.getCustomers();
            List<Product> products = controller.getProducts();

            if (customers.isEmpty() || products.isEmpty()) {
                showError("Please ensure at least one customer and product exist before recording a transaction.");
                return;
            }

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Record Commercial Transaction (ACID)");

            VBox layout = new VBox(16);
            layout.setPadding(new Insets(24));
            layout.setStyle("-fx-background-color: " + Theme.COLOR_BG_CARD + ";");

            Label dTitle = new Label("Record Commercial Transaction");
            dTitle.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 18px; -fx-font-weight: bold;");

            Label dSub = new Label("Executes an atomic JDBC transaction: deducts stock and commits sale.");
            dSub.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 12px;");

            ComboBox<Customer> cbCustomer = new ComboBox<>(FXCollections.observableArrayList(customers));
            cbCustomer.setPromptText("Select Customer");
            cbCustomer.setMaxWidth(Double.MAX_VALUE);
            cbCustomer.setStyle(Theme.TEXT_INPUT);

            ComboBox<Product> cbProduct = new ComboBox<>(FXCollections.observableArrayList(products));
            cbProduct.setPromptText("Select Product");
            cbProduct.setMaxWidth(Double.MAX_VALUE);
            cbProduct.setStyle(Theme.TEXT_INPUT);

            Label lblStockInfo = new Label("Stock: -- | Unit Price: --");
            lblStockInfo.setStyle("-fx-text-fill: " + Theme.COLOR_ACCENT_PRIMARY + "; -fx-font-size: 12px; -fx-font-weight: bold;");

            TextField tfQty = new TextField("1");
            tfQty.setPromptText("Quantity");
            tfQty.setStyle(Theme.TEXT_INPUT);

            Label lblTotalAmt = new Label("Calculated Total: $0.00");
            lblTotalAmt.setStyle("-fx-text-fill: " + Theme.COLOR_SUCCESS + "; -fx-font-size: 16px; -fx-font-weight: bold;");

            Runnable updateTotal = () -> {
                Product p = cbProduct.getValue();
                if (p != null) {
                    lblStockInfo.setText(String.format("Available Stock: %d units | Unit Price: $%.2f", p.getStockQty(), p.getUnitPrice()));
                    try {
                        int q = Integer.parseInt(tfQty.getText().trim());
                        if (q > 0) {
                            double total = q * p.getUnitPrice();
                            lblTotalAmt.setText(String.format("Calculated Total: $%,.2f", total));
                        }
                    } catch (NumberFormatException ignored) {}
                }
            };

            cbProduct.setOnAction(e -> updateTotal.run());
            tfQty.textProperty().addListener((obs, oldV, newV) -> updateTotal.run());

            HBox actions = new HBox(12);
            actions.setAlignment(Pos.CENTER_RIGHT);

            Button btnCancel = new Button("Cancel");
            btnCancel.setStyle(Theme.BTN_SECONDARY);
            btnCancel.setOnAction(e -> dialog.close());

            Button btnCommit = new Button("Confirm & Transact");
            btnCommit.setStyle(Theme.BTN_PRIMARY);
            btnCommit.setOnAction(e -> {
                Customer c = cbCustomer.getValue();
                Product p = cbProduct.getValue();
                if (c == null || p == null) {
                    showError("Please select both customer and product.");
                    return;
                }
                try {
                    int qty = Integer.parseInt(tfQty.getText().trim());
                    if (qty <= 0) {
                        showError("Quantity must be greater than zero.");
                        return;
                    }
                    if (qty > p.getStockQty()) {
                        showError("Insufficient inventory! Only " + p.getStockQty() + " units available.");
                        return;
                    }
                    double amt = qty * p.getUnitPrice();
                    controller.recordSale(c.getId(), p.getId(), qty, amt, dialog::close);
                } catch (NumberFormatException ex) {
                    showError("Please enter a valid numeric quantity.");
                }
            });

            actions.getChildren().addAll(btnCancel, btnCommit);

            layout.getChildren().addAll(
                    dTitle, dSub,
                    new Label("Customer:"), cbCustomer,
                    new Label("Product:"), cbProduct,
                    lblStockInfo,
                    new Label("Quantity:"), tfQty,
                    lblTotalAmt,
                    actions
            );

            Scene scene = new Scene(layout, 480, 480);
            dialog.setScene(scene);
            dialog.showAndWait();

        } catch (SQLException ex) {
            showError("Failed to open transaction modal: " + ex.getMessage());
        }
    }
}
