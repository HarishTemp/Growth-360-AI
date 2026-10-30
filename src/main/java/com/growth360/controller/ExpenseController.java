package com.growth360.controller;

import com.growth360.model.Expense;
import com.growth360.service.ExpenseService;
import com.growth360.view.ExpenseView;
import javafx.application.Platform;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExpenseController {

    private final ExpenseService expenseService;
    private final ExecutorService executor;
    private ExpenseView view;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ExpenseWorker");
            t.setDaemon(true);
            return t;
        });
    }

    public void setView(ExpenseView view) {
        this.view = view;
    }

    public void loadExpenses() {
        executor.submit(() -> {
            try {
                List<Expense> expenses = expenseService.getAllExpenses();
                Platform.runLater(() -> {
                    if (view != null) view.setExpensesData(expenses);
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> {
                    if (view != null) view.showError("Failed to load expenses: " + ex.getMessage());
                });
            }
        });
    }

    public void addExpense(String category, String description, double amount, LocalDateTime date, Runnable onComplete) {
        executor.submit(() -> {
            try {
                Expense expense = new Expense(category, description, amount, date);
                expenseService.createExpense(expense);
                Platform.runLater(() -> {
                    if (view != null) {
                        view.showSuccess("Expense added successfully.");
                        loadExpenses();
                    }
                    if (onComplete != null) onComplete.run();
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> {
                    if (view != null) view.showError("Failed to add expense: " + ex.getMessage());
                });
            }
        });
    }

    public void deleteExpense(int expenseId) {
        executor.submit(() -> {
            try {
                expenseService.deleteExpense(expenseId);
                Platform.runLater(() -> {
                    if (view != null) {
                        view.showSuccess("Expense deleted.");
                        loadExpenses();
                    }
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> {
                    if (view != null) view.showError("Failed to delete expense: " + ex.getMessage());
                });
            }
        });
    }
}
