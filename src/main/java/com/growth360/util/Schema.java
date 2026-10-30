package com.growth360.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages database schema generation via JDBC statements.
 * No external .sql files are used (Pure Java hard rule).
 */
public final class Schema {

    private static final String CREATE_PRODUCTS_TABLE = """
            CREATE TABLE IF NOT EXISTS products (
                id INT AUTO_INCREMENT PRIMARY KEY,
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

    private Schema() {
        // Prevent instantiation
    }

    /**
     * Executes table creation queries on the active database connection.
     */
    public static void initSchema() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(CREATE_PRODUCTS_TABLE);
            stmt.execute(CREATE_CUSTOMERS_TABLE);
            stmt.execute(CREATE_SALES_TABLE);
            stmt.execute(CREATE_EXPENSES_TABLE);

            System.out.println("[Schema] All database tables verified and initialized successfully.");
        }
    }
}
