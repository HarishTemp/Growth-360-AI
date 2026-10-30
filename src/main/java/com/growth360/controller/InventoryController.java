package com.growth360.controller;

import com.growth360.analytics.ReorderPointCalculator;
import com.growth360.analytics.ReorderPointCalculator.InventoryMetric;
import com.growth360.model.Product;
import com.growth360.model.Sale;
import com.growth360.service.ProductService;
import com.growth360.service.SaleService;
import com.growth360.view.InventoryView;
import javafx.application.Platform;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InventoryController {

    private final ProductService productService;
    private final SaleService saleService;
    private final ReorderPointCalculator ropCalculator;
    private final ExecutorService executor;
    private InventoryView view;

    public InventoryController(ProductService productService, SaleService saleService) {
        this.productService = productService;
        this.saleService = saleService;
        this.ropCalculator = new ReorderPointCalculator();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "InventoryWorker");
            t.setDaemon(true);
            return t;
        });
    }

    public void setView(InventoryView view) {
        this.view = view;
    }

    public void loadInventory() {
        executor.submit(() -> {
            try {
                List<Product> products = productService.getAllProducts();
                List<Sale> sales = saleService.getAllSales();
                List<InventoryMetric> metrics = ropCalculator.calculateMetrics(products, sales);
                Platform.runLater(() -> {
                    if (view != null) view.setInventoryData(metrics);
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> {
                    if (view != null) view.showError("Failed to load inventory: " + ex.getMessage());
                });
            }
        });
    }

    public void createProduct(Product product, Runnable onComplete) {
        executor.submit(() -> {
            try {
                productService.createProduct(product);
                Platform.runLater(() -> {
                    if (view != null) {
                        view.showSuccess("Product created successfully.");
                        loadInventory();
                    }
                    if (onComplete != null) onComplete.run();
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> {
                    if (view != null) view.showError("Failed to create product: " + ex.getMessage());
                });
            }
        });
    }

    public void updateProduct(Product product, Runnable onComplete) {
        executor.submit(() -> {
            try {
                productService.updateProduct(product);
                Platform.runLater(() -> {
                    if (view != null) {
                        view.showSuccess("Product updated successfully.");
                        loadInventory();
                    }
                    if (onComplete != null) onComplete.run();
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> {
                    if (view != null) view.showError("Failed to update product: " + ex.getMessage());
                });
            }
        });
    }

    public void restockProduct(int productId, int quantity, Runnable onComplete) {
        executor.submit(() -> {
            try {
                productService.restockProduct(productId, quantity);
                Platform.runLater(() -> {
                    if (view != null) {
                        view.showSuccess("Restocked " + quantity + " units successfully.");
                        loadInventory();
                    }
                    if (onComplete != null) onComplete.run();
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> {
                    if (view != null) view.showError("Failed to restock product: " + ex.getMessage());
                });
            }
        });
    }

    public void deleteProduct(int productId) {
        executor.submit(() -> {
            try {
                productService.deleteProduct(productId);
                Platform.runLater(() -> {
                    if (view != null) {
                        view.showSuccess("Product deleted.");
                        loadInventory();
                    }
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> {
                    if (view != null) view.showError("Failed to delete product: " + ex.getMessage());
                });
            }
        });
    }
}
