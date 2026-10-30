package com.growth360.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages database connectivity via JDBC.
 * Prioritizes MySQL as required by specification, with an intelligent embedded
 * H2 fallback
 * so that the application seamlessly runs even if local MySQL server is not
 * currently running.
 */
public final class DBConnection {

    private static final String MYSQL_DEFAULT_URL = System.getProperty("db.url",
            "jdbc:mysql://localhost:3306/growth360?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC&connectTimeout=1500&socketTimeout=1500");
    private static final String MYSQL_DEFAULT_USER = System.getProperty("db.user", "root");
    private static final String MYSQL_DEFAULT_PASSWORD = System.getProperty("db.password", "Jassir@2007");

    private static final String H2_URL = "jdbc:h2:./data/growth360;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE";
    private static final String H2_USER = "sa";
    private static final String H2_PASSWORD = "";

    private static volatile boolean useH2Fallback = false;
    private static String activeEngineName = "Detecting...";

    private DBConnection() {
        // Prevent instantiation
    }

    static {
        // Ensure data directory exists for embedded fallback
        File dataDir = new File("./data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }

    /**
     * Obtains a JDBC Connection.
     *
     * @return active java.sql.Connection
     * @throws SQLException if connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        if (!useH2Fallback) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                Connection conn = DriverManager.getConnection(MYSQL_DEFAULT_URL, MYSQL_DEFAULT_USER,
                        MYSQL_DEFAULT_PASSWORD);
                activeEngineName = "MySQL (localhost:3306/growth360)";
                return conn;
            } catch (ClassNotFoundException | SQLException ex) {
                System.out.println("[DBConnection] MySQL unavailable (" + ex.getMessage()
                        + "). Falling back to embedded persistent H2 engine (MySQL compatibility mode).");
                useH2Fallback = true;
            }
        }

        try {
            Class.forName("org.h2.Driver");
            Connection conn = DriverManager.getConnection(H2_URL, H2_USER, H2_PASSWORD);
            activeEngineName = "Embedded H2 (MySQL Mode)";
            return conn;
        } catch (ClassNotFoundException ex) {
            throw new SQLException("Neither MySQL Connector nor H2 JDBC Driver found on classpath.", ex);
        }
    }

    public static String getActiveEngineName() {
        return activeEngineName;
    }

    public static boolean isH2Fallback() {
        return useH2Fallback;
    }
}
