package com.growth360.view;

import com.growth360.controller.ExpenseController;
import com.growth360.model.Expense;
import com.growth360.util.Theme;
import com.growth360.util.Validator;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Pure Java JavaFX Expense management view for tracking overheads and operational burn.
 */
public class ExpenseView extends VBox {

    private final ExpenseController controller;
    private final ObservableList<Expense> expensesMasterList = FXCollections.observableArrayList();
    private FilteredList<Expense> filteredExpenses;

    private Label lblTotalExpense;
    private Label lblEntryCount;
    private Label lblAvgExpense;
    private Label notificationBanner;
    private TableView<Expense> tableView;
    private TextField searchField;
    private ComboBox<String> categoryFilter;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String[] CATEGORIES = {
            "Marketing & Celebrity Ads",
            "Boutique Leases",
            "Horology Staff & Payroll",
            "ERP & Boutique Tech",
            "Component Logistics",
            "Titan Care Service",
            "Manufacturing & Craftsmanship",
            "Retail Packaging & Visual Merchandising",
            "Miscellaneous"
    };

    public ExpenseView(ExpenseController controller) {
        this.controller = controller;
        this.controller.setView(this);

        buildUI();
        this.controller.loadExpenses();
    }

    private void buildUI() {
        setSpacing(20);
        setPadding(new Insets(24, 28, 32, 28));
        setStyle("-fx-background-color: " + Theme.COLOR_BG_APP + ";");

        // Header
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Titan Boutique & Horology Operational Expenses");
        title.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label subtitle = new Label("Track boutique leases, master horologist payroll, Swiss component logistics, and festive campaigns");
        subtitle.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + "; -fx-font-size: 13px;");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAddExpense = new Button("+ Add Expense Entry");
        btnAddExpense.setStyle(Theme.BTN_PRIMARY);
        btnAddExpense.setOnAction(e -> openAddExpenseDialog());

        header.getChildren().addAll(titleBox, spacer, btnAddExpense);

        // Notification Banner
        notificationBanner = new Label();
        notificationBanner.setVisible(false);
        notificationBanner.setManaged(false);

        // Stats Row
        HBox statsRow = new HBox(16);
        lblTotalExpense = new Label("$0.00");
        VBox card1 = createStatCard("TOTAL EXPENDITURE", lblTotalExpense, "#EF4444");

        lblEntryCount = new Label("0");
        VBox card2 = createStatCard("LOGGED ENTRIES", lblEntryCount, "#38BDF8");

        lblAvgExpense = new Label("$0.00");
        VBox card3 = createStatCard("AVERAGE ENTRY", lblAvgExpense, "#F59E0B");

        statsRow.getChildren().addAll(card1, card2, card3);
        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);

        // Filter / Search Toolbar
        HBox toolbar = new HBox(14);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("🔍 Search description...");
        searchField.setStyle(Theme.TEXT_INPUT);
        searchField.setPrefWidth(260);
        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilter());

        ObservableList<String> catOptions = FXCollections.observableArrayList("All Categories");
        catOptions.addAll(CATEGORIES);
        categoryFilter = new ComboBox<>(catOptions);
        categoryFilter.setValue("All Categories");
        categoryFilter.setStyle(Theme.TEXT_INPUT);
        categoryFilter.setOnAction(e -> applyFilter());

        toolbar.getChildren().addAll(searchField, categoryFilter);

        // Table
        tableView = new TableView<>();
        tableView.setStyle(Theme.TABLE_VIEW);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        TableColumn<Expense, String> colId = new TableColumn<>("#");
        colId.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        colId.setMaxWidth(70);

        TableColumn<Expense, String> colDate = new TableColumn<>("Date & Time");
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getExpenseDate().format(DATE_FMT)));

        TableColumn<Expense, String> colCat = new TableColumn<>("Category");
        colCat.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));

        TableColumn<Expense, String> colDesc = new TableColumn<>("Description / Vendor");
        colDesc.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));

        TableColumn<Expense, String> colAmt = new TableColumn<>("Amount");
        colAmt.setCellValueFactory(data -> new SimpleStringProperty(String.format("%s %,.2f", Theme.CURRENCY_SYMBOL, data.getValue().getAmount())));
        colAmt.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold; -fx-text-fill: #EF4444;");

        TableColumn<Expense, Void> colActions = new TableColumn<>("Action");
        colActions.setMaxWidth(100);
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button("Delete");
            {
                btnDelete.setStyle(Theme.BTN_DANGER + " -fx-font-size: 11px; -fx-padding: 4 10 4 10;");
                btnDelete.setOnAction(e -> {
                    Expense exp = getTableView().getItems().get(getIndex());
                    if (exp != null) {
                        controller.deleteExpense(exp.getId());
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

        tableView.getColumns().addAll(colId, colDate, colCat, colDesc, colAmt, colActions);

        filteredExpenses = new FilteredList<>(expensesMasterList, p -> true);
        tableView.setItems(filteredExpenses);

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
        String cat = categoryFilter.getValue();

        filteredExpenses.setPredicate(exp -> {
            boolean matchesCat = cat == null || "All Categories".equals(cat) || exp.getCategory().equalsIgnoreCase(cat);
            boolean matchesQuery = query == null || query.isBlank() ||
                    (exp.getDescription() != null && exp.getDescription().toLowerCase().contains(query.toLowerCase()));
            return matchesCat && matchesQuery;
        });
    }

    public void setExpensesData(List<Expense> expenses) {
        expensesMasterList.setAll(expenses);

        int count = expenses.size();
        double total = expenses.stream().mapToDouble(Expense::getAmount).sum();
        double avg = count > 0 ? (total / count) : 0.0;

        lblTotalExpense.setText(String.format("%s %,.2f", Theme.CURRENCY_SYMBOL, total));
        lblEntryCount.setText(String.format("%,d Logged", count));
        lblAvgExpense.setText(String.format("%s %,.2f", Theme.CURRENCY_SYMBOL, avg));
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

    private void openAddExpenseDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Record Operational Expenditure");

        VBox layout = new VBox(16);
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: " + Theme.COLOR_BG_CARD + ";");

        Label dTitle = new Label("Add Operational Expense");
        dTitle.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 18px; -fx-font-weight: bold;");

        ComboBox<String> cbCat = new ComboBox<>(FXCollections.observableArrayList(CATEGORIES));
        cbCat.setValue(CATEGORIES[0]);
        cbCat.setMaxWidth(Double.MAX_VALUE);
        cbCat.setStyle(Theme.TEXT_INPUT);

        TextField tfDesc = new TextField();
        tfDesc.setPromptText("Description / Vendor");
        tfDesc.setStyle(Theme.TEXT_INPUT);

        TextField tfAmount = new TextField();
        tfAmount.setPromptText("Amount in USD (e.g. 450.00)");
        tfAmount.setStyle(Theme.TEXT_INPUT);

        DatePicker dpDate = new DatePicker(LocalDate.now());
        dpDate.setMaxWidth(Double.MAX_VALUE);
        dpDate.setStyle(Theme.TEXT_INPUT);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancel = new Button("Cancel");
        btnCancel.setStyle(Theme.BTN_SECONDARY);
        btnCancel.setOnAction(e -> dialog.close());

        Button btnSave = new Button("Save Expense");
        btnSave.setStyle(Theme.BTN_PRIMARY);
        btnSave.setOnAction(e -> {
            String cat = cbCat.getValue();
            String desc = tfDesc.getText();
            String amtStr = tfAmount.getText();
            LocalDate ld = dpDate.getValue();

            if (!Validator.isNotEmpty(desc)) {
                showError("Please enter a valid expense description.");
                return;
            }
            if (!Validator.isValidPositiveDouble(amtStr)) {
                showError("Please enter a valid positive numeric amount.");
                return;
            }

            double amt = Double.parseDouble(amtStr.trim());
            LocalDateTime dt = ld != null ? ld.atTime(12, 0) : LocalDateTime.now();
            controller.addExpense(cat, desc, amt, dt, dialog::close);
        });

        actions.getChildren().addAll(btnCancel, btnSave);

        layout.getChildren().addAll(
                dTitle,
                new Label("Expense Category:"), cbCat,
                new Label("Description / Vendor:"), tfDesc,
                new Label("Amount (USD):"), tfAmount,
                new Label("Expense Date:"), dpDate,
                actions
        );

        Scene scene = new Scene(layout, 450, 460);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
