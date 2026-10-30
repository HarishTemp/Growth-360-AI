package com.growth360.dao;

import com.growth360.model.User;
import com.growth360.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for User (Business Profile) entities.
 */
public class UserDAO {

    private static final String SQL_INSERT =
            "INSERT INTO users (business_name, owner_name, email, currency, created_at) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_SELECT_FIRST =
            "SELECT id, business_name, owner_name, email, currency, created_at FROM users ORDER BY id ASC LIMIT 1";
    private static final String SQL_COUNT =
            "SELECT COUNT(*) FROM users";

    public User save(User user) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getBusinessName());
            ps.setString(2, user.getOwnerName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getCurrency());
            ps.setTimestamp(5, Timestamp.valueOf(user.getCreatedAt()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                }
            }
        }
        return user;
    }

    public Optional<User> findPrimaryUser() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_FIRST);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("created_at");
                return Optional.of(new User(
                        rs.getInt("id"),
                        rs.getString("business_name"),
                        rs.getString("owner_name"),
                        rs.getString("email"),
                        rs.getString("currency"),
                        ts != null ? ts.toLocalDateTime() : null
                ));
            }
        }
        return Optional.empty();
    }

    public int count() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_COUNT);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}
