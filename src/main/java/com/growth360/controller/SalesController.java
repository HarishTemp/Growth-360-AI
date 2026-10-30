package com.growth360.controller;

import com.growth360.model.Customer;
import com.growth360.model.Product;
import com.growth360.model.Sale;
import com.growth360.service.CustomerService;
import com.growth360.service.ProductService;
import com.growth360.service.SaleService;
import com.growth360.view.SalesView;
import javafx.application.Platform;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SalesController {

    private final SaleService saleService;
    private final CustomerService customerService;
    private final ProductService productService;
    private final ExecutorService executor;
    private SalesView view;

    public SalesController(SaleService saleService, CustomerService customerService, ProductService productService) {
        this.saleService = saleService;
        this.customerService = customerService;
        this.productService = productService;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SalesWorker");
            t.setDaemon(true);
            return t;
        });
    }

    public void setView(SalesView view) {
        this.view = view;
    }

    public void loadSales() {
        executor.submit(() -> {
            try {
                List<Sale> sales = saleService.getAllSales();
                Platform.runLater(() -> {
                    if (view != null) view.setSalesData(sales);
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> {
                    if (view != null) view.showError("Failed to load sales: " + ex.getMessage());
                });
            }
        });
    }

    public void recordSale(int customerId, int productId, int quantity, double amount, Runnable onComplete) {
        executor.submit(() -> {
            try {
                Sale sale = new Sale(customerId, productId, quantity, amount, java.time.LocalDateTime.now());
                saleService.recordSale(sale);
                Platform.runLater(() -> {
                    if (view != null) {
                        view.showSuccess("Sale recorded successfully! Stock deducted in transaction.");
                        loadSales();
                    }
                    if (onComplete != null) onComplete.run();
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> {
                    if (view != null) view.showError("Transaction failed: " + ex.getMessage());
                });
            }
        });
    }

    public void deleteSale(int saleId) {
        executor.submit(() -> {
            try {
                saleService.deleteSale(saleId);
                Platform.runLater(() -> {
                    if (view != null) {
                        view.showSuccess("Sale deleted successfully.");
                        loadSales();
                    }
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> {
                    if (view != null) view.showError("Failed to delete sale: " + ex.getMessage());
                });
            }
        });
    }

    public List<Customer> getCustomers() throws SQLException {
        return customerService.getAllCustomers();
    }

    public List<Product> getProducts() throws SQLException {
        return productService.getAllProducts();
    }
}
