-- ================================================================
-- GROWTH 360 AI — Database Schema Script
-- Decision Intelligence, Prescriptive Surveillance & Horology Analytics
-- Compatible with MySQL 8.0+ and Embedded H2 (MySQL Mode)
-- ================================================================

CREATE DATABASE IF NOT EXISTS growth360;
USE growth360;

-- 1. USERS TABLE (Business / Multi-tenant Boutique Profile)
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    business_name VARCHAR(120) NOT NULL,
    owner_name VARCHAR(120) NOT NULL,
    email VARCHAR(120) UNIQUE NOT NULL,
    currency VARCHAR(10) DEFAULT 'INR',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. PRODUCTS TABLE (Timepieces & Wearable SKUs)
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
);

-- 3. CUSTOMERS TABLE (Encircle Loyalty Profiles)
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
);

-- 4. SALES TABLE (Boutique Transactions & B2B Orders)
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
);

-- 5. EXPENSES TABLE (Boutique Overhead & Operational Burn)
CREATE TABLE IF NOT EXISTS expenses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(60) NOT NULL,
    description VARCHAR(255),
    amount DOUBLE NOT NULL,
    expense_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. RISK ALERTS TABLE (Persistent Operational Surveillance)
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
);

-- PERFORMANCE INDEXES
CREATE INDEX idx_sales_date ON sales(sale_date);
CREATE INDEX idx_sales_product ON sales(product_id);
CREATE INDEX idx_sales_customer ON sales(customer_id);
CREATE INDEX idx_expenses_date ON expenses(expense_date);
CREATE INDEX idx_expenses_category ON expenses(category);
CREATE INDEX idx_alert_status ON risk_alerts(is_resolved);
