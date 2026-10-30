package com.growth360.model;

/**
 * Represents a business product with inventory and cost metadata.
 */
public class Product {
    private int id;
    private String name;
    private String category;
    private int stockQty;
    private double unitCost;
    private double unitPrice;
    private int leadTimeDays;
    private int safetyStock;

    public Product() {
        this.leadTimeDays = 7;
        this.safetyStock = 10;
    }

    public Product(int id, String name, String category, int stockQty, double unitCost, double unitPrice, int leadTimeDays, int safetyStock) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.stockQty = stockQty;
        this.unitCost = unitCost;
        this.unitPrice = unitPrice;
        this.leadTimeDays = leadTimeDays;
        this.safetyStock = safetyStock;
    }

    public Product(String name, String category, int stockQty, double unitCost, double unitPrice, int leadTimeDays, int safetyStock) {
        this(0, name, category, stockQty, unitCost, unitPrice, leadTimeDays, safetyStock);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getStockQty() { return stockQty; }
    public void setStockQty(int stockQty) { this.stockQty = stockQty; }

    public double getUnitCost() { return unitCost; }
    public void setUnitCost(double unitCost) { this.unitCost = unitCost; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public int getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(int leadTimeDays) { this.leadTimeDays = leadTimeDays; }

    public int getSafetyStock() { return safetyStock; }
    public void setSafetyStock(int safetyStock) { this.safetyStock = safetyStock; }

    public double getProfitMargin() {
        if (unitPrice <= 0) return 0.0;
        return ((unitPrice - unitCost) / unitPrice) * 100.0;
    }

    @Override
    public String toString() {
        return name + " (" + category + ")";
    }
}
