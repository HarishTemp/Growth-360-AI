package com.growth360.controller;

import com.growth360.analytics.RfmSegmenter;
import com.growth360.model.Customer;
import com.growth360.model.RfmResult;
import com.growth360.model.Sale;
import com.growth360.service.CustomerService;
import com.growth360.service.SaleService;
import com.growth360.view.CustomerView;
import javafx.application.Platform;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomerController {

    private final CustomerService customerService;
    private final SaleService saleService;
    private final RfmSegmenter rfmSegmenter;
    private final ExecutorService executor;
    private CustomerView view;

    public CustomerController(CustomerService customerService, SaleService saleService) {
        this.customerService = customerService;
        this.saleService = saleService;
        this.rfmSegmenter = new RfmSegmenter();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "CustomerWorker");
            t.setDaemon(true);
            return t;
        });
    }

    public void setView(CustomerView view) {
        this.view = view;
    }

    public void loadCustomers() {
        executor.submit(() -> {
            try {
                List<Customer> customers = customerService.getAllCustomers();
                List<Sale> sales = saleService.getAllSales();
                List<RfmResult> rfmResults = rfmSegmenter.segmentCustomers(customers, sales);
                Platform.runLater(() -> {
                    if (view != null) view.setCustomerData(customers, rfmResults);
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> {
                    if (view != null) view.showError("Failed to load customer data: " + ex.getMessage());
                });
            }
        });
    }

    public void createCustomer(String name, String email, String phone, Runnable onComplete) {
        executor.submit(() -> {
            try {
                Customer customer = new Customer(name, email, phone);
                customerService.createCustomer(customer);
                Platform.runLater(() -> {
                    if (view != null) {
                        view.showSuccess("Customer created successfully.");
                        loadCustomers();
                    }
                    if (onComplete != null) onComplete.run();
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> {
                    if (view != null) view.showError("Failed to create customer: " + ex.getMessage());
                });
            }
        });
    }

    public void updateCustomer(Customer customer, Runnable onComplete) {
        executor.submit(() -> {
            try {
                customerService.updateCustomer(customer);
                Platform.runLater(() -> {
                    if (view != null) {
                        view.showSuccess("Customer updated successfully.");
                        loadCustomers();
                    }
                    if (onComplete != null) onComplete.run();
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> {
                    if (view != null) view.showError("Failed to update customer: " + ex.getMessage());
                });
            }
        });
    }

    public void deleteCustomer(int customerId) {
        executor.submit(() -> {
            try {
                customerService.deleteCustomer(customerId);
                Platform.runLater(() -> {
                    if (view != null) {
                        view.showSuccess("Customer deleted.");
                        loadCustomers();
                    }
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> {
                    if (view != null) view.showError("Failed to delete customer: " + ex.getMessage());
                });
            }
        });
    }
}
