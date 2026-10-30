package com.growth360.model;

import java.time.LocalDateTime;

/**
 * Represents an operational business expense.
 */
public class Expense {
    private int id;
    private String category;
    private String description;
    private double amount;
    private LocalDateTime expenseDate;

    public Expense() {
        this.expenseDate = LocalDateTime.now();
    }

    public Expense(int id, String category, String description, double amount, LocalDateTime expenseDate) {
        this.id = id;
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.expenseDate = expenseDate != null ? expenseDate : LocalDateTime.now();
    }

    public Expense(String category, String description, double amount, LocalDateTime expenseDate) {
        this(0, category, description, amount, expenseDate);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDateTime getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDateTime expenseDate) { this.expenseDate = expenseDate; }
}
