package com.growth360.dao;

import com.growth360.model.Alert;
import com.growth360.model.Alert.AlertType;
import com.growth360.model.Alert.Severity;
import com.growth360.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for RiskAlert entities.
 * Allows persisting, querying, and resolving alerts in the database.
 */
public class AlertDAO {

    private static final String SQL_INSERT_OR_UPDATE = """
            MERGE INTO risk_alerts (alert_id, severity, category, headline, root_cause, recommended_action, is_resolved, created_at)
            KEY(alert_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SQL_INSERT_MYSQL = """
            INSERT INTO risk_alerts (alert_id, severity, category, headline, root_cause, recommended_action, is_resolved, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE severity = VALUES(severity), headline = VALUES(headline)
            """;

    private static final String SQL_RESOLVE =
            "UPDATE risk_alerts SET is_resolved = TRUE WHERE alert_id = ?";

    private static final String SQL_SELECT_ACTIVE =
            "SELECT alert_id, severity, category, headline, root_cause, recommended_action, is_resolved, created_at FROM risk_alerts WHERE is_resolved = FALSE ORDER BY created_at DESC";

    public void save(Alert alert) throws SQLException {
        String sql = DBConnection.isH2Fallback() ? SQL_INSERT_OR_UPDATE : SQL_INSERT_MYSQL;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, alert.id());
            ps.setString(2, alert.severity().name());
            ps.setString(3, alert.type().name());
            ps.setString(4, alert.title());
            ps.setString(5, alert.description());
            ps.setString(6, "Review prescriptive recommendations in Decision Matrix");
            ps.setBoolean(7, false);
            ps.setTimestamp(8, Timestamp.valueOf(alert.timestamp()));
            ps.executeUpdate();
        }
    }

    public void resolve(String alertId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_RESOLVE)) {
            ps.setString(1, alertId);
            ps.executeUpdate();
        }
    }

    public List<Alert> findActive() throws SQLException {
        List<Alert> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_ACTIVE);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("alert_id");
                Severity sev = Severity.valueOf(rs.getString("severity"));
                AlertType type = AlertType.valueOf(rs.getString("category"));
                String title = rs.getString("headline");
                String desc = rs.getString("root_cause");
                Timestamp ts = rs.getTimestamp("created_at");

                list.add(new Alert(id, type, sev, title, desc, 0.0, ts.toLocalDateTime()));
            }
        }
        return list;
    }
}
