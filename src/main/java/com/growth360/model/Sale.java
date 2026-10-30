package com.growth360.model;

import java.time.LocalDateTime;

/**
 * Represents a completed sales transaction.
 */
public class Sale {
    private int id;
    private int customerId;
    private int productId;
    private int quantity;
    private double amount;
    private LocalDateTime saleDate;

    // Transient helper fields for UI display
    private String customerName;
    private String productName;

    public Sale() {
        this.saleDate = LocalDateTime.now();
    }

    public Sale(int id, int customerId, int productId, int quantity, double amount, LocalDateTime saleDate) {
        this.id = id;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.amount = amount;
        this.saleDate = saleDate != null ? saleDate : LocalDateTime.now();
    }

    public Sale(int customerId, int productId, int quantity, double amount, LocalDateTime saleDate) {
        this(0, customerId, productId, quantity, amount, saleDate);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDateTime getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDateTime saleDate) { this.saleDate = saleDate; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
}
