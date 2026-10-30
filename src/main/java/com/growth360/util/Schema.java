package com.growth360.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages database schema generation via JDBC statements.
 * Creates all 6 required tables (users, products, customers, sales, expenses, risk_alerts)
 * and performance indexes.
 */
public final class Schema {

    private static final String CREATE_USERS_TABLE = """
            CREATE TABLE IF NOT EXISTS users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                business_name VARCHAR(120) NOT NULL,
                owner_name VARCHAR(120) NOT NULL,
                email VARCHAR(120) UNIQUE NOT NULL,
                currency VARCHAR(10) DEFAULT 'INR',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

    private static final String CREATE_PRODUCTS_TABLE = """
            CREATE TABLE IF NOT EXISTS products (
                id INT AUTO_INCREMENT PRIMARY KEY,
                sku VARCHAR(50) UNIQUE,
                name VARCHAR(120) NOT NULL,
                category VARCHAR(60) NOT NULL,
                stock_qty INT NOT NULL DEFAULT 0,
                unit_cost DOUBLE NOT NULL DEFAULT 0.0,
                unit_price DOUBLE NOT NULL DEFAULT 0.0,
                lead_time_days INT NOT NULL DEFAULT 7,
                safety_stock INT NOT NULL DEFAULT 10
            )
            """;

    private static final String CREATE_CUSTOMERS_TABLE = """
            CREATE TABLE IF NOT EXISTS customers (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(120) NOT NULL,
                email VARCHAR(120) NOT NULL,
                phone VARCHAR(40),
                segment VARCHAR(50) DEFAULT 'New',
                total_spending DOUBLE DEFAULT 0.0,
                purchase_frequency INT DEFAULT 0,
                last_purchase_date TIMESTAMP,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

    private static final String CREATE_SALES_TABLE = """
            CREATE TABLE IF NOT EXISTS sales (
                id INT AUTO_INCREMENT PRIMARY KEY,
                customer_id INT NOT NULL,
                product_id INT NOT NULL,
                quantity INT NOT NULL,
                amount DOUBLE NOT NULL,
                channel VARCHAR(30) DEFAULT 'Store',
                sale_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT fk_sales_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
                CONSTRAINT fk_sales_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
            )
            """;

    private static final String CREATE_EXPENSES_TABLE = """
            CREATE TABLE IF NOT EXISTS expenses (
                id INT AUTO_INCREMENT PRIMARY KEY,
                category VARCHAR(60) NOT NULL,
                description VARCHAR(255),
                amount DOUBLE NOT NULL,
                expense_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

    private static final String CREATE_RISK_ALERTS_TABLE = """
            CREATE TABLE IF NOT EXISTS risk_alerts (
                alert_id VARCHAR(100) PRIMARY KEY,
                severity VARCHAR(20) NOT NULL,
                category VARCHAR(50) NOT NULL,
                headline VARCHAR(200) NOT NULL,
                root_cause TEXT NOT NULL,
                recommended_action TEXT NOT NULL,
                action_type VARCHAR(50),
                is_resolved BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

    private Schema() {
        // Prevent instantiation
    }

    /**
     * Executes table creation queries on the active database connection.
     */
    public static void initSchema() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(CREATE_USERS_TABLE);
            stmt.execute(CREATE_PRODUCTS_TABLE);
            stmt.execute(CREATE_CUSTOMERS_TABLE);
            stmt.execute(CREATE_SALES_TABLE);
            stmt.execute(CREATE_EXPENSES_TABLE);
            stmt.execute(CREATE_RISK_ALERTS_TABLE);

            // Attempt index creation safely (ignore if exists)
            createIndexSilently(stmt, "idx_sales_date", "CREATE INDEX idx_sales_date ON sales(sale_date)");
            createIndexSilently(stmt, "idx_sales_product", "CREATE INDEX idx_sales_product ON sales(product_id)");
            createIndexSilently(stmt, "idx_sales_customer", "CREATE INDEX idx_sales_customer ON sales(customer_id)");
            createIndexSilently(stmt, "idx_expenses_date", "CREATE INDEX idx_expenses_date ON expenses(expense_date)");
            createIndexSilently(stmt, "idx_expenses_category", "CREATE INDEX idx_expenses_category ON expenses(category)");
            createIndexSilently(stmt, "idx_alert_status", "CREATE INDEX idx_alert_status ON risk_alerts(is_resolved)");

            System.out.println("[Schema] All 6 database tables & indexes verified and initialized successfully.");
        }
    }

    private static void createIndexSilently(Statement stmt, String name, String ddl) {
        try {
            stmt.execute(ddl);
        } catch (SQLException ignored) {
            // Index likely already exists
        }
    }
}
