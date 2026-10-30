package com.growth360.util;

import com.growth360.dao.CustomerDAO;
import com.growth360.dao.ExpenseDAO;
import com.growth360.dao.ProductDAO;
import com.growth360.dao.SaleDAO;
import com.growth360.model.Customer;
import com.growth360.model.Expense;
import com.growth360.model.Product;
import com.growth360.model.Sale;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * Seeds realistic 6-month historical business data for Titan Company Limited (Watches & Wearables Division).
 * Generates iconic collections (Titan Edge, Raga, Nebula, Octane, Fastrack, Sonata),
 * Encircle loyalty members, and World of Titan boutique retail operations.
 */
public final class DataSeeder {

    private DataSeeder() {}

    public static void seedIfEmpty() {
        try {
            ProductDAO productDAO = new ProductDAO();
            CustomerDAO customerDAO = new CustomerDAO();
            SaleDAO saleDAO = new SaleDAO();
            ExpenseDAO expenseDAO = new ExpenseDAO();

            List<Product> existingProducts = productDAO.findAll();
            List<Expense> existingExpenses = expenseDAO.findAll();

            boolean hasTitanData = existingProducts.stream()
                    .anyMatch(p -> p.getName().toLowerCase().contains("titan") || p.getName().toLowerCase().contains("nebula"));

            boolean hasUncalibratedExpenses = existingExpenses.stream().anyMatch(e -> e.getAmount() > 1000000.0);
            if (hasTitanData && existingProducts.size() >= 10 && !existingExpenses.isEmpty() && !hasUncalibratedExpenses) {
                System.out.println("[DataSeeder] Titan Watches dataset already verified (" + existingProducts.size() + " timepieces, " + existingExpenses.size() + " expense logs).");
                return;
            }

            System.out.println("--------------------------------------------------");
            System.out.println("[DataSeeder] Initializing TITAN WATCHES (Titan Company Ltd.) horology dataset...");
            System.out.println("--------------------------------------------------");

            // If old non-Titan data or uncalibrated data exists, cleanly wipe tables for fresh Titan demo
            if (!existingProducts.isEmpty() && (!hasTitanData || hasUncalibratedExpenses)) {
                System.out.println("[DataSeeder] Migrating database to Titan Watches product catalog...");
                try (Connection conn = DBConnection.getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.execute("DELETE FROM sales");
                    stmt.execute("DELETE FROM expenses");
                    stmt.execute("DELETE FROM products");
                    stmt.execute("DELETE FROM customers");
                }
            }

            // 1. Seed Iconic Titan Timepiece Collections
            // Tailored to showcase Healthy, Reorder Point, Out of Stock, and Overstocked states
            Product[] titanWatches = {
                    new Product("Titan Edge Ceramic Slimline", "Titan Edge (Ultra-Slim)", 42, 14000.0, 29995.0, 14, 10),
                    new Product("Titan Raga Viva Rose Gold Pearl", "Titan Raga (Women)", 18, 4200.0, 9995.0, 10, 20),           // Low stock / ROP breach!
                    new Product("Nebula 18K Solid Gold Mechanical", "Nebula Haute Horlogerie", 2, 110000.0, 245000.0, 21, 8),    // Urgent reorder! (2 units)
                    new Product("Titan Grandmaster Skeleton Automatic", "Titan Automatic", 0, 18000.0, 38995.0, 15, 12),          // Out of stock! (0 units)
                    new Product("Fastrack Reflex Beat+ Smartwatch", "Fastrack Wearables", 95, 1200.0, 2995.0, 7, 25),             // Fast-moving healthy stock
                    new Product("Sonata Poze Minimalist Quartz", "Sonata Value Horology", 150, 650.0, 1499.0, 5, 30),             // High volume base
                    new Product("Titan Octane Chrono Tachymeter", "Titan Octane (Sports)", 35, 5200.0, 11995.0, 10, 15),          // Sports chronograph
                    new Product("Titan Maritime Deep Blue Automatic", "Titan Maritime", 12, 13500.0, 27995.0, 14, 10),            // Marine automatic
                    new Product("Titan Smart Touch AMOLED Fitness", "Titan Smart (Connected)", 60, 3800.0, 7995.0, 8, 15),
                    new Product("Titan Italian Leather Strap Collector Box", "Horology Accessories", 260, 800.0, 2495.0, 5, 25)  // Overstocked (260 units)
            };

            for (Product p : titanWatches) {
                productDAO.save(p);
            }
            List<Product> savedProducts = productDAO.findAll();
            System.out.println("[DataSeeder] Seeded " + savedProducts.size() + " iconic Titan timepieces & wearable SKUs.");

            // 2. Seed Titan Encircle Loyalty Customers (Bangalore, Mumbai, Delhi, Chennai, Hyderabad)
            Customer[] encircleCustomers = {
                    new Customer("Aarav Sharma", "aarav.sharma@gmail.com", "+91 98201 11223"),
                    new Customer("Priya Patel", "priya.patel@yahoo.co.in", "+91 98450 22334"),
                    new Customer("Vikramaditya Rao", "vikram.rao@titanencircle.in", "+91 98765 33445"),
                    new Customer("Sneha Kulkarni", "sneha.k@outlook.com", "+91 98112 44556"),
                    new Customer("Rajesh Malhotra", "rajesh.m@corporatemail.com", "+91 98990 55667"),
                    new Customer("Ananya Sen", "ananya.sen@gmail.com", "+91 97170 66778"),
                    new Customer("Rohan Singhania", "rohan.s@luxuryhorology.com", "+91 99200 77889"),
                    new Customer("Divya Nambiar", "divya.n@kerala.net", "+91 98330 88990"),
                    new Customer("Karthik Sundaram", "karthik.s@tcs.com", "+91 98840 99001"),
                    new Customer("Meera Merchant", "meera.m@merchantgroup.in", "+91 97690 12345"),
                    new Customer("Siddharth Banerjee", "sid.b@iitb.ac.in", "+91 99880 23456"),
                    new Customer("Neha Wadhwa", "neha.w@delhicapital.com", "+91 98100 34567")
            };

            for (Customer c : encircleCustomers) {
                c.setCreatedAt(LocalDateTime.now().minusDays(180));
                customerDAO.save(c);
            }
            List<Customer> savedCustomers = customerDAO.findAll();
            System.out.println("[DataSeeder] Seeded " + savedCustomers.size() + " Titan Encircle loyalty customer profiles.");

            // 3. Seed 6 Months of Boutique Sales History
            // Simulates Champions (Nebula & Edge repeat luxury buyers), Loyal, Potential, At-Risk, and Lost buyers
            LocalDateTime now = LocalDateTime.now();
            Random rng = new Random(42);

            for (Customer c : savedCustomers) {
                int cid = c.getId();
                int orderCount;
                int maxDaysAgo;
                int minDaysAgo;

                if (cid <= 3) {
                    // Champions: frequent luxury watch collectors, recent visits to World of Titan
                    orderCount = 12 + rng.nextInt(6);
                    minDaysAgo = 2;
                    maxDaysAgo = 160;
                } else if (cid <= 6) {
                    // Loyal: Regular buyers across festivals and birthdays
                    orderCount = 8 + rng.nextInt(4);
                    minDaysAgo = 8;
                    maxDaysAgo = 170;
                } else if (cid <= 8) {
                    // Potential: Bought Fastrack/Titan Smart recently
                    orderCount = 2 + rng.nextInt(2);
                    minDaysAgo = 4;
                    maxDaysAgo = 30;
                } else if (cid <= 10) {
                    // At-Risk: Bought expensive timepieces 3-6 months ago, but inactive recently
                    orderCount = 7 + rng.nextInt(3);
                    minDaysAgo = 65;
                    maxDaysAgo = 175;
                } else {
                    // Lost: Single purchase 5 months ago
                    orderCount = 1;
                    minDaysAgo = 140;
                    maxDaysAgo = 175;
                }

                for (int i = 0; i < orderCount; i++) {
                    Product prod = savedProducts.get(rng.nextInt(savedProducts.size()));
                    int maxQty = (prod.getUnitPrice() < 3000) ? 6 : (prod.getUnitPrice() < 15000) ? 3 : 1;
                    int qty = 1 + rng.nextInt(maxQty);
                    double amount = Math.round(prod.getUnitPrice() * qty * 100.0) / 100.0;
                    int daysAgo = minDaysAgo + rng.nextInt(Math.max(1, maxDaysAgo - minDaysAgo + 1));
                    LocalDateTime saleDate = now.minusDays(daysAgo).minusHours(rng.nextInt(24)).minusMinutes(rng.nextInt(60));

                    Sale sale = new Sale(cid, prod.getId(), qty, amount, saleDate);
                    insertSeedSale(sale);
                }
            }

            // Seed 4 Corporate & Festive Bulk Gifting Orders (Classic Titan B2B channel)
            Customer corpClient = savedCustomers.get(4); // Rajesh Malhotra (Corporate)
            insertSeedSale(new Sale(corpClient.getId(), savedProducts.get(4).getId(), 45, 45 * 2995.0, now.minusDays(35))); // Fastrack Reflex bulk order
            insertSeedSale(new Sale(corpClient.getId(), savedProducts.get(5).getId(), 60, 60 * 1499.0, now.minusDays(110))); // Sonata bulk order
            insertSeedSale(new Sale(corpClient.getId(), savedProducts.get(0).getId(), 8, 8 * 29995.0, now.minusDays(18))); // Executive Titan Edge gifting

            System.out.println("[DataSeeder] Seeded 6 months of World of Titan boutique transactions & corporate gifting orders.");

            // 4. Seed 6 Months of Operational Expenses for Titan Watches Division
            for (int monthOffset = 5; monthOffset >= 0; monthOffset--) {
                LocalDateTime monthDate = now.minusMonths(monthOffset);

                // Fixed Boutique Leases (e.g. World of Titan Flagship Boutique)
                expenseDAO.save(new Expense("Boutique Leases", "World of Titan Flagship Boutique commercial lease", 85000.0, monthDate.withDayOfMonth(1)));

                // Horology Specialists & Boutique Staff Salaries
                expenseDAO.save(new Expense("Horology Staff & Payroll", "Master watchmakers, boutique managers & retail staff compensation", 140000.0, monthDate.withDayOfMonth(28)));

                // Retail POS & SAP Enterprise ERP Subscriptions
                expenseDAO.save(new Expense("ERP & Boutique Tech", "SAP Horology Supply Chain ERP & retail billing software", 25000.0, monthDate.withDayOfMonth(5)));

                // Sapphire Crystal & Swiss Movement Import Freight
                double freight = 32000.0 + rng.nextDouble() * 12000.0;
                expenseDAO.save(new Expense("Component Logistics", "Precision quartz movement & sapphire crystal import freight", Math.round(freight * 100.0) / 100.0, monthDate.withDayOfMonth(15)));

                // Titan Care Warranty & After-Sales Repair Network
                double repair = 26000.0 + rng.nextDouble() * 8000.0;
                expenseDAO.save(new Expense("Titan Care Service", "After-sales watch restoration, battery replacements & waterproofing tests", Math.round(repair * 100.0) / 100.0, monthDate.withDayOfMonth(20)));

                // Festive Celebrity Ad Blitz (Diwali / Wedding Season) -> Z-Score Spike Anomaly in monthOffset = 1!
                double marketing = (monthOffset == 1) ? 385000.0 : (55000.0 + rng.nextDouble() * 20000.0);
                String mktgDesc = (monthOffset == 1) ?
                        "Titan Festive Season Ad Blitz (Diwali/Wedding Campaign with Brand Ambassadors - Anomaly Spike)" :
                        "Standard digital performance marketing & print catalogue distribution";
                expenseDAO.save(new Expense("Marketing & Celebrity Ads", mktgDesc, Math.round(marketing * 100.0) / 100.0, monthDate.withDayOfMonth(12)));
            }

            System.out.println("[DataSeeder] Seeded 6 months of boutique overheads & Titan Care service logs.");
            System.out.println("[DataSeeder] Successfully injected Festive Season Ad Blitz Z-Score anomaly spike.");
            System.out.println("[DataSeeder] TITAN WATCHES dataset active. Ready for analytics.");
            System.out.println("--------------------------------------------------");

        } catch (SQLException ex) {
            System.err.println("[DataSeeder] Error during Titan data seeding: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static void insertSeedSale(Sale sale) throws SQLException {
        String sql = "INSERT INTO sales (customer_id, product_id, quantity, amount, sale_date) VALUES (?, ?, ?, ?, ?)";
        try (var conn = DBConnection.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sale.getCustomerId());
            ps.setInt(2, sale.getProductId());
            ps.setInt(3, sale.getQuantity());
            ps.setDouble(4, sale.getAmount());
            ps.setTimestamp(5, java.sql.Timestamp.valueOf(sale.getSaleDate()));
            ps.executeUpdate();
        }
    }
}
