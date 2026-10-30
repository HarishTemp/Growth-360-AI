package com.growth360.dao;

import com.growth360.model.Customer;
import com.growth360.model.Product;
import com.growth360.model.Sale;
import com.growth360.util.DBConnection;
import com.growth360.util.Schema;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class SaleTransactionTest {

    @BeforeAll
    static void initDb() throws SQLException {
        Schema.initSchema();
    }

    @Test
    @DisplayName("SaleDAO: Successful sale decrements stock and commits transaction")
    void testSaleCommit() throws SQLException {
        ProductDAO productDAO = new ProductDAO();
        CustomerDAO customerDAO = new CustomerDAO();
        SaleDAO saleDAO = new SaleDAO();

        Product p = productDAO.save(new Product("Transaction Test Product", "Test", 50, 10.0, 20.0, 7, 5));
        Customer c = customerDAO.save(new Customer("Txn Customer", "txn@test.com", "999"));

        Sale sale = new Sale(c.getId(), p.getId(), 5, 100.0, LocalDateTime.now());
        Sale recorded = saleDAO.recordSaleTransactional(sale);

        assertTrue(recorded.getId() > 0, "Sale ID should be generated upon commit");

        Product refreshed = productDAO.findById(p.getId()).orElseThrow();
        assertEquals(45, refreshed.getStockQty(), "Product stock should be decremented by 5");
    }

    @Test
    @DisplayName("SaleDAO: Insufficient stock triggers rollback without altering stock or persisting sale")
    void testSaleRollbackOnInsufficientStock() throws SQLException {
        ProductDAO productDAO = new ProductDAO();
        CustomerDAO customerDAO = new CustomerDAO();
        SaleDAO saleDAO = new SaleDAO();

        Product p = productDAO.save(new Product("Scarce Item", "Test", 2, 10.0, 20.0, 7, 5));
        Customer c = customerDAO.save(new Customer("Txn Customer 2", "txn2@test.com", "888"));

        Sale sale = new Sale(c.getId(), p.getId(), 10, 200.0, LocalDateTime.now());

        assertThrows(SQLException.class, () -> saleDAO.recordSaleTransactional(sale),
                "Should throw SQLException due to insufficient stock");

        Product refreshed = productDAO.findById(p.getId()).orElseThrow();
        assertEquals(2, refreshed.getStockQty(), "Stock should remain unchanged after rollback");
    }
}
