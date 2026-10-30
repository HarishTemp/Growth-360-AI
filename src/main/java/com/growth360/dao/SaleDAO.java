package com.growth360.dao;

import com.growth360.model.Sale;
import com.growth360.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Sale entities.
 * Enforces transactional execution when recording sales and deducting inventory.
 */
public class SaleDAO {

    private static final String SQL_INSERT_SALE =
            "INSERT INTO sales (customer_id, product_id, quantity, amount, sale_date) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_CHECK_STOCK =
            "SELECT stock_qty FROM products WHERE id = ? FOR UPDATE";
    private static final String SQL_DECREMENT_STOCK =
            "UPDATE products SET stock_qty = stock_qty - ? WHERE id = ?";
    private static final String SQL_SELECT_ALL =
            """
            SELECT s.id, s.customer_id, s.product_id, s.quantity, s.amount, s.sale_date,
                   c.name AS customer_name, p.name AS product_name
            FROM sales s
            JOIN customers c ON s.customer_id = c.id
            JOIN products p ON s.product_id = p.id
            ORDER BY s.sale_date DESC
            """;
    private static final String SQL_DELETE =
            "DELETE FROM sales WHERE id = ?";
    private static final String SQL_TOTAL_REVENUE =
            "SELECT COALESCE(SUM(amount), 0.0) FROM sales";

    /**
     * Records a sale in a strict JDBC transaction:
     * 1. Locks product row and verifies available stock
     * 2. Inserts sale record
     * 3. Decrements product stock quantity
     * 4. Commits on success, or rolls back on failure
     */
    public Sale recordSaleTransactional(Sale sale) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Check stock
            int availableStock = 0;
            try (PreparedStatement psCheck = conn.prepareStatement(SQL_CHECK_STOCK)) {
                psCheck.setInt(1, sale.getProductId());
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        availableStock = rs.getInt("stock_qty");
                    } else {
                        throw new SQLException("Product with ID " + sale.getProductId() + " does not exist.");
                    }
                }
            }

            if (availableStock < sale.getQuantity()) {
                throw new SQLException("Insufficient stock! Requested: " + sale.getQuantity() + ", Available: " + availableStock);
            }

            // 2. Insert sale record
            try (PreparedStatement psSale = conn.prepareStatement(SQL_INSERT_SALE, Statement.RETURN_GENERATED_KEYS)) {
                psSale.setInt(1, sale.getCustomerId());
                psSale.setInt(2, sale.getProductId());
                psSale.setInt(3, sale.getQuantity());
                psSale.setDouble(4, sale.getAmount());
                psSale.setTimestamp(5, Timestamp.valueOf(sale.getSaleDate() != null ? sale.getSaleDate() : LocalDateTime.now()));
                psSale.executeUpdate();

                try (ResultSet rs = psSale.getGeneratedKeys()) {
                    if (rs.next()) {
                        sale.setId(rs.getInt(1));
                    }
                }
            }

            // 3. Decrement product stock
            try (PreparedStatement psStock = conn.prepareStatement(SQL_DECREMENT_STOCK)) {
                psStock.setInt(1, sale.getQuantity());
                psStock.setInt(2, sale.getProductId());
                psStock.executeUpdate();
            }

            // 4. Commit transaction
            conn.commit();
            return sale;

        } catch (SQLException ex) {
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("[SaleDAO] Transaction rolled back due to error: " + ex.getMessage());
                } catch (SQLException rbEx) {
                    System.err.println("[SaleDAO] Rollback failed: " + rbEx.getMessage());
                }
            }
            throw ex;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    // Ignore close exceptions
                }
            }
        }
    }

    public List<Sale> findAll() throws SQLException {
        List<Sale> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Sale sale = new Sale(
                        rs.getInt("id"),
                        rs.getInt("customer_id"),
                        rs.getInt("product_id"),
                        rs.getInt("quantity"),
                        rs.getDouble("amount"),
                        rs.getTimestamp("sale_date").toLocalDateTime()
                );
                sale.setCustomerName(rs.getString("customer_name"));
                sale.setProductName(rs.getString("product_name"));
                list.add(sale);
            }
        }
        return list;
    }

    public void delete(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public double getTotalRevenue() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_TOTAL_REVENUE);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        }
        return 0.0;
    }
}
