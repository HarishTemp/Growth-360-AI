package com.growth360.dao;

import com.growth360.model.Customer;
import com.growth360.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Customer entities.
 */
public class CustomerDAO {

    private static final String SQL_INSERT =
            "INSERT INTO customers (name, email, phone, created_at) VALUES (?, ?, ?, ?)";
    private static final String SQL_UPDATE =
            "UPDATE customers SET name = ?, email = ?, phone = ? WHERE id = ?";
    private static final String SQL_DELETE =
            "DELETE FROM customers WHERE id = ?";
    private static final String SQL_SELECT_ALL =
            "SELECT id, name, email, phone, created_at FROM customers ORDER BY name ASC";
    private static final String SQL_SELECT_BY_ID =
            "SELECT id, name, email, phone, created_at FROM customers WHERE id = ?";

    public Customer save(Customer customer) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, customer.getName());
            ps.setString(2, customer.getEmail());
            ps.setString(3, customer.getPhone());
            ps.setTimestamp(4, Timestamp.valueOf(customer.getCreatedAt()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    customer.setId(rs.getInt(1));
                }
            }
        }
        return customer;
    }

    public void update(Customer customer) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, customer.getName());
            ps.setString(2, customer.getEmail());
            ps.setString(3, customer.getPhone());
            ps.setInt(4, customer.getId());
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

    public Optional<Customer> findById(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCustomer(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Customer> findAll() throws SQLException {
        List<Customer> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToCustomer(rs));
            }
        }
        return list;
    }

    private Customer mapResultSetToCustomer(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new Customer(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("phone"),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}
