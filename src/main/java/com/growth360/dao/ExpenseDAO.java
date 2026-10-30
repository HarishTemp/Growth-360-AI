package com.growth360.dao;

import com.growth360.model.Expense;
import com.growth360.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Expense entities.
 */
public class ExpenseDAO {

    private static final String SQL_INSERT =
            "INSERT INTO expenses (category, description, amount, expense_date) VALUES (?, ?, ?, ?)";
    private static final String SQL_UPDATE =
            "UPDATE expenses SET category = ?, description = ?, amount = ?, expense_date = ? WHERE id = ?";
    private static final String SQL_DELETE =
            "DELETE FROM expenses WHERE id = ?";
    private static final String SQL_SELECT_ALL =
            "SELECT id, category, description, amount, expense_date FROM expenses ORDER BY expense_date DESC";
    private static final String SQL_TOTAL_EXPENSE =
            "SELECT COALESCE(SUM(amount), 0.0) FROM expenses";

    public Expense save(Expense expense) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, expense.getCategory());
            ps.setString(2, expense.getDescription());
            ps.setDouble(3, expense.getAmount());
            ps.setTimestamp(4, Timestamp.valueOf(expense.getExpenseDate()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    expense.setId(rs.getInt(1));
                }
            }
        }
        return expense;
    }

    public void update(Expense expense) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, expense.getCategory());
            ps.setString(2, expense.getDescription());
            ps.setDouble(3, expense.getAmount());
            ps.setTimestamp(4, Timestamp.valueOf(expense.getExpenseDate()));
            ps.setInt(5, expense.getId());
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

    public List<Expense> findAll() throws SQLException {
        List<Expense> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Expense(
                        rs.getInt("id"),
                        rs.getString("category"),
                        rs.getString("description"),
                        rs.getDouble("amount"),
                        rs.getTimestamp("expense_date").toLocalDateTime()
                ));
            }
        }
        return list;
    }

    public double getTotalExpense() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_TOTAL_EXPENSE);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        }
        return 0.0;
    }
}
