package com.growth360.dao;

import com.growth360.model.Product;
import com.growth360.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Product entities.
 */
public class ProductDAO {

    private static final String SQL_INSERT =
            "INSERT INTO products (name, category, stock_qty, unit_cost, unit_price, lead_time_days, safety_stock) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE =
            "UPDATE products SET name = ?, category = ?, stock_qty = ?, unit_cost = ?, unit_price = ?, lead_time_days = ?, safety_stock = ? WHERE id = ?";
    private static final String SQL_DELETE =
            "DELETE FROM products WHERE id = ?";
    private static final String SQL_SELECT_ALL =
            "SELECT id, name, category, stock_qty, unit_cost, unit_price, lead_time_days, safety_stock FROM products ORDER BY name ASC";
    private static final String SQL_SELECT_BY_ID =
            "SELECT id, name, category, stock_qty, unit_cost, unit_price, lead_time_days, safety_stock FROM products WHERE id = ?";
    private static final String SQL_DECREMENT_STOCK =
            "UPDATE products SET stock_qty = stock_qty - ? WHERE id = ? AND stock_qty >= ?";
    private static final String SQL_INCREMENT_STOCK =
            "UPDATE products SET stock_qty = stock_qty + ? WHERE id = ?";

    public Product save(Product product) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, product.getName());
            ps.setString(2, product.getCategory());
            ps.setInt(3, product.getStockQty());
            ps.setDouble(4, product.getUnitCost());
            ps.setDouble(5, product.getUnitPrice());
            ps.setInt(6, product.getLeadTimeDays());
            ps.setInt(7, product.getSafetyStock());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    product.setId(rs.getInt(1));
                }
            }
        }
        return product;
    }

    public void update(Product product) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, product.getName());
            ps.setString(2, product.getCategory());
            ps.setInt(3, product.getStockQty());
            ps.setDouble(4, product.getUnitCost());
            ps.setDouble(5, product.getUnitPrice());
            ps.setInt(6, product.getLeadTimeDays());
            ps.setInt(7, product.getSafetyStock());
            ps.setInt(8, product.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public Optional<Product> findById(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProduct(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Product> findAll() throws SQLException {
        List<Product> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToProduct(rs));
            }
        }
        return list;
    }

    /**
     * Decrements product inventory inside an active JDBC transaction.
     */
    public boolean decrementStock(Connection conn, int productId, int quantity) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_DECREMENT_STOCK)) {
            ps.setInt(1, quantity);
            ps.setInt(2, productId);
            ps.setInt(3, quantity);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }

    public void increaseStock(int productId, int quantity) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INCREMENT_STOCK)) {
            ps.setInt(1, quantity);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getInt("stock_qty"),
                rs.getDouble("unit_cost"),
                rs.getDouble("unit_price"),
                rs.getInt("lead_time_days"),
                rs.getInt("safety_stock")
        );
    }
}
