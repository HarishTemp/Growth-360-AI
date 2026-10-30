package com.growth360.view;

import com.growth360.controller.CustomerController;
import com.growth360.model.Customer;
import com.growth360.model.RfmResult;
import com.growth360.model.RfmResult.Segment;
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
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pure Java JavaFX Customer Intelligence view displaying RFM quintile segmentation,
 * customer lifetime value, churn risk, and visual cohort breakdown via PieChart.
 */
public class CustomerView extends VBox {

    private final CustomerController controller;
    private final ObservableList<RfmResult> rfmMasterList = FXCollections.observableArrayList();
    private FilteredList<RfmResult> filteredRfm;

    private Label lblTotalCustomers;
    private Label lblChampions;
    private Label lblLoyal;
    private Label lblAtRisk;
    private Label notificationBanner;
    private TableView<RfmResult> tableView;
    private TextField searchField;
    private ComboBox<String> segmentFilter;
    private PieChart segmentPieChart;

    private List<Customer> cachedCustomers = List.of();

    public CustomerView(CustomerController controller) {
        this.controller = controller;
        this.controller.setView(this);

        buildUI();
        this.controller.loadCustomers();
    }

    private void buildUI() {
        setSpacing(20);
        setPadding(new Insets(24, 28, 32, 28));
        setStyle("-fx-background-color: " + Theme.COLOR_BG_APP + ";");

        // Header
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Titan Encircle Loyalty & RFM Customer Intelligence");
        title.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label subtitle = new Label("Encircle tier segmentation (Horology collectors vs casual buyers) and churn surveillance");
        subtitle.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 13px;");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNewCustomer = new Button("+ Add New Customer");
        btnNewCustomer.setStyle(Theme.BTN_PRIMARY);
        btnNewCustomer.setOnAction(e -> openCustomerDialog(null));

        header.getChildren().addAll(titleBox, spacer, btnNewCustomer);

        // Notification Banner
        notificationBanner = new Label();
        notificationBanner.setVisible(false);
        notificationBanner.setManaged(false);

        // Stats Cards Row
        HBox statsRow = new HBox(16);
        lblTotalCustomers = new Label("0");
        VBox card1 = createStatCard("TOTAL CUSTOMERS", lblTotalCustomers, "#38BDF8");

        lblChampions = new Label("0");
        VBox card2 = createStatCard("CHAMPIONS COHORT", lblChampions, "#10B981");

        lblLoyal = new Label("0");
        VBox card3 = createStatCard("LOYAL CUSTOMERS", lblLoyal, "#818CF8");

        lblAtRisk = new Label("0");
        VBox card4 = createStatCard("AT-RISK / CHURNING", lblAtRisk, "#F59E0B");

        statsRow.getChildren().addAll(card1, card2, card3, card4);
        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);
        HBox.setHgrow(card4, Priority.ALWAYS);

        // Top Section: Segment Pie Chart + Legend
        HBox chartRow = new HBox(20);
        chartRow.setAlignment(Pos.CENTER_LEFT);
        chartRow.setStyle(Theme.CARD_STYLE);

        VBox chartTextBox = new VBox(8);
        chartTextBox.setPrefWidth(350);
        Label chartTitle = new Label("Customer Cohort Distribution (RFM)");
        chartTitle.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label chartDesc = new Label("Segments are computed dynamically by sorting customer accounts into empirical quintiles based on recency of purchase, transaction count, and lifetime monetary value.");
        chartDesc.setWrapText(true);
        chartDesc.setStyle("-fx-text-fill: " + Theme.TEXT_SECONDARY + "; -fx-font-size: 12px;");
        chartTextBox.getChildren().addAll(chartTitle, chartDesc);

        segmentPieChart = new PieChart();
        segmentPieChart.setAnimated(false);
        segmentPieChart.setLegendVisible(true);
        segmentPieChart.setPrefHeight(220);
        segmentPieChart.setPrefWidth(550);
        HBox.setHgrow(segmentPieChart, Priority.ALWAYS);

        chartRow.getChildren().addAll(chartTextBox, segmentPieChart);

        // Toolbar: Search + Filter
        HBox toolbar = new HBox(14);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("🔍 Search customer name...");
        searchField.setStyle(Theme.TEXT_INPUT);
        searchField.setPrefWidth(260);
        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilter());

        segmentFilter = new ComboBox<>(FXCollections.observableArrayList(
                "All Segments",
                "Champions",
                "Loyal Customers",
                "Potential Loyalists",
                "At-Risk",
                "Lost / Inactive"
        ));
        segmentFilter.setValue("All Segments");
        segmentFilter.setStyle(Theme.TEXT_INPUT);
        segmentFilter.setOnAction(e -> applyFilter());

        toolbar.getChildren().addAll(searchField, segmentFilter);

        // Table
        tableView = new TableView<>();
        tableView.setStyle(Theme.TABLE_VIEW);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        TableColumn<RfmResult, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(d -> new SimpleStringProperty("#" + d.getValue().customerId()));
        colId.setMaxWidth(70);

        TableColumn<RfmResult, String> colName = new TableColumn<>("Customer Name");
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().customerName()));

        TableColumn<RfmResult, String> colRecency = new TableColumn<>("Recency");
        colRecency.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().recencyDays() + " days ago"));
        colRecency.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<RfmResult, String> colFreq = new TableColumn<>("Orders (F)");
        colFreq.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().frequency() + " orders"));
        colFreq.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<RfmResult, String> colMonetary = new TableColumn<>("Monetary Spend (M)");
        colMonetary.setCellValueFactory(d -> new SimpleStringProperty(String.format("%s %,.2f", Theme.CURRENCY_SYMBOL, d.getValue().monetary())));
        colMonetary.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");

        TableColumn<RfmResult, String> colScore = new TableColumn<>("R-F-M Score");
        colScore.setCellValueFactory(d -> new SimpleStringProperty(
                String.format("%d-%d-%d", d.getValue().rScore(), d.getValue().fScore(), d.getValue().mScore())));
        colScore.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");

        TableColumn<RfmResult, Segment> colSegment = new TableColumn<>("Cohort Segment");
        colSegment.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().segment()));
        colSegment.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Segment segment, boolean empty) {
                super.updateItem(segment, empty);
                if (empty || segment == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(segment.getDisplayName());
                    badge.setStyle(Theme.getBadgeStyle("rgba(255,255,255,0.08)", segment.getColorHex()) +
                            " -fx-border-color: " + segment.getColorHex() + "; -fx-border-radius: 20px;");
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        TableColumn<RfmResult, Void> colActions = new TableColumn<>("Actions");
        colActions.setMaxWidth(140);
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox box = new HBox(6, btnEdit, btnDelete);

            {
                box.setAlignment(Pos.CENTER);
                btnEdit.setStyle(Theme.BTN_SECONDARY + " -fx-font-size: 10px; -fx-padding: 3 8 3 8;");
                btnDelete.setStyle(Theme.BTN_DANGER + " -fx-font-size: 10px; -fx-padding: 3 8 3 8;");

                btnEdit.setOnAction(e -> {
                    RfmResult r = getTableView().getItems().get(getIndex());
                    if (r != null) {
                        cachedCustomers.stream()
                                .filter(c -> c.getId() == r.customerId())
                                .findFirst()
                                .ifPresent(c -> openCustomerDialog(c));
                    }
                });

                btnDelete.setOnAction(e -> {
                    RfmResult r = getTableView().getItems().get(getIndex());
                    if (r != null) controller.deleteCustomer(r.customerId());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
                setAlignment(Pos.CENTER);
            }
        });

        tableView.getColumns().addAll(colId, colName, colRecency, colFreq, colMonetary, colScore, colSegment, colActions);

        filteredRfm = new FilteredList<>(rfmMasterList, p -> true);
        tableView.setItems(filteredRfm);

        getChildren().addAll(header, notificationBanner, statsRow, chartRow, toolbar, tableView);
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
        String seg = segmentFilter.getValue();

        filteredRfm.setPredicate(rfm -> {
            boolean matchesSeg = seg == null || "All Segments".equals(seg) ||
                    rfm.segment().getDisplayName().equalsIgnoreCase(seg);
            boolean matchesQuery = query == null || query.isBlank() ||
                    rfm.customerName().toLowerCase().contains(query.toLowerCase());
            return matchesSeg && matchesQuery;
        });
    }

    public void setCustomerData(List<Customer> customers, List<RfmResult> rfmResults) {
        this.cachedCustomers = customers;
        rfmMasterList.setAll(rfmResults);

        int total = rfmResults.size();
        long champions = rfmResults.stream().filter(r -> r.segment() == Segment.CHAMPIONS).count();
        long loyal = rfmResults.stream().filter(r -> r.segment() == Segment.LOYAL).count();
        long atRisk = rfmResults.stream().filter(r -> r.segment() == Segment.AT_RISK).count();

        lblTotalCustomers.setText(String.format("%,d Customers", total));
        lblChampions.setText(String.format("%d Champions", champions));
        lblLoyal.setText(String.format("%d Loyal", loyal));
        lblAtRisk.setText(String.format("%d At-Risk", atRisk));

        // Update Pie Chart
        segmentPieChart.getData().clear();
        Map<Segment, Long> countsBySeg = rfmResults.stream()
                .collect(Collectors.groupingBy(RfmResult::segment, Collectors.counting()));

        for (Segment seg : Segment.values()) {
            long count = countsBySeg.getOrDefault(seg, 0L);
            if (count > 0) {
                segmentPieChart.getData().add(new PieChart.Data(seg.getDisplayName() + " (" + count + ")", count));
            }
        }
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

    private void openCustomerDialog(Customer existing) {
        boolean isEdit = existing != null;
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(isEdit ? "Edit Customer Record" : "Register New Customer");

        VBox layout = new VBox(16);
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: " + Theme.COLOR_BG_CARD + ";");

        Label dTitle = new Label(isEdit ? "Edit Customer Record" : "Register New Customer");
        dTitle.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 18px; -fx-font-weight: bold;");

        TextField tfName = new TextField(isEdit ? existing.getName() : "");
        tfName.setPromptText("Full Name");
        tfName.setStyle(Theme.TEXT_INPUT);

        TextField tfEmail = new TextField(isEdit ? existing.getEmail() : "");
        tfEmail.setPromptText("Email Address");
        tfEmail.setStyle(Theme.TEXT_INPUT);

        TextField tfPhone = new TextField(isEdit ? existing.getPhone() : "");
        tfPhone.setPromptText("Phone Number");
        tfPhone.setStyle(Theme.TEXT_INPUT);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancel = new Button("Cancel");
        btnCancel.setStyle(Theme.BTN_SECONDARY);
        btnCancel.setOnAction(e -> dialog.close());

        Button btnSave = new Button(isEdit ? "Update Customer" : "Create Customer");
        btnSave.setStyle(Theme.BTN_PRIMARY);
        btnSave.setOnAction(e -> {
            String name = tfName.getText().trim();
            String email = tfEmail.getText().trim();
            String phone = tfPhone.getText().trim();

            if (!Validator.isNotEmpty(name)) {
                showError("Customer name cannot be empty.");
                return;
            }
            if (!Validator.isValidEmail(email)) {
                showError("Please provide a valid email address.");
                return;
            }

            if (isEdit) {
                existing.setName(name);
                existing.setEmail(email);
                existing.setPhone(phone);
                controller.updateCustomer(existing, dialog::close);
            } else {
                controller.createCustomer(name, email, phone, dialog::close);
            }
        });

        actions.getChildren().addAll(btnCancel, btnSave);

        layout.getChildren().addAll(
                dTitle,
                new Label("Customer Name:"), tfName,
                new Label("Email:"), tfEmail,
                new Label("Phone:"), tfPhone,
                actions
        );

        Scene scene = new Scene(layout, 420, 360);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
