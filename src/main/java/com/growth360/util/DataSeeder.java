package com.growth360.util;

import com.growth360.dao.*;
import com.growth360.model.*;

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
 * Calibrated for positive net margins, healthy growth scores, and a verified Z-score marketing anomaly spike.
 */
public final class DataSeeder {

    private DataSeeder() {}

    public static void seedIfEmpty() {
        try {
            UserDAO userDAO = new UserDAO();
            ProductDAO productDAO = new ProductDAO();
            CustomerDAO customerDAO = new CustomerDAO();
            SaleDAO saleDAO = new SaleDAO();
            ExpenseDAO expenseDAO = new ExpenseDAO();

            List<Product> existingProducts = productDAO.findAll();
            List<Expense> existingExpenses = expenseDAO.findAll();

            boolean hasTitanData = existingProducts.stream()
                    .anyMatch(p -> p.getName().toLowerCase().contains("titan") || p.getName().toLowerCase().contains("nebula"));

            boolean isProperlyCalibrated = existingProducts.size() >= 10 && existingExpenses.size() >= 30;
            if (hasTitanData && isProperlyCalibrated && userDAO.count() > 0) {
                System.out.println("[DataSeeder] Titan Watches dataset already verified (" + existingProducts.size() + " SKUs, " + existingExpenses.size() + " expense logs).");
                return;
            }

            System.out.println("--------------------------------------------------");
            System.out.println("[DataSeeder] Initializing TITAN WATCHES (Titan Company Ltd.) horology dataset...");
            System.out.println("--------------------------------------------------");

            // Cleanly wipe tables for fresh calibrated demo
            try (Connection conn = DBConnection.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM sales");
                stmt.execute("DELETE FROM expenses");
                stmt.execute("DELETE FROM products");
                stmt.execute("DELETE FROM customers");
                stmt.execute("DELETE FROM risk_alerts");
                stmt.execute("DELETE FROM users");
            }

            // 0. Seed Primary Business User Profile
            userDAO.save(new User(
                    "World of Titan — Flagship Boutique",
                    "Rajesh V. (Boutique General Manager)",
                    "indiranagar.boutique@titan.co.in",
                    "INR"
            ));

            // 1. Seed Iconic Titan Timepiece Collections
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

            // 2. Seed 24 Titan Encircle Loyalty Customers (Bangalore, Mumbai, Delhi, Chennai, Hyderabad, Pune, Kolkata)
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
                    new Customer("Neha Wadhwa", "neha.w@delhicapital.com", "+91 98100 34567"),
                    new Customer("Aditya Verma", "aditya.v@bengaluru.tech", "+91 98455 12345"),
                    new Customer("Pooja Hegde", "pooja.h@mangalore.in", "+91 98452 67890"),
                    new Customer("Arjun Kapoor", "arjun.k@bollywoodstyle.com", "+91 99201 98765"),
                    new Customer("Deepa Krishnamurthy", "deepa.k@chennaihorology.com", "+91 98400 45678"),
                    new Customer("Manish Sisodia", "manish.s@businesscorp.in", "+91 98110 54321"),
                    new Customer("Sunita Reddy", "sunita.r@hyderabadhealth.com", "+91 98490 87654"),
                    new Customer("Kabir Mehta", "kabir.m@ahmedabadtextiles.com", "+91 98250 12389"),
                    new Customer("Isha Deshmukh", "isha.d@puneauto.in", "+91 98220 98712"),
                    new Customer("Gautam Gambhir", "gautam.g@sportsdelhi.com", "+91 98101 23456"),
                    new Customer("Tanya Choudhary", "tanya.c@kolkataheritage.in", "+91 98300 76543"),
                    new Customer("Rameshwar Birla", "ramesh.birla@industrialgroup.in", "+91 98290 87650"),
                    new Customer("Shweta Bachchan", "shweta.b@mumbaicouture.com", "+91 98202 34567")
            };

            for (Customer c : encircleCustomers) {
                c.setCreatedAt(LocalDateTime.now().minusDays(180));
                customerDAO.save(c);
            }
            List<Customer> savedCustomers = customerDAO.findAll();
            System.out.println("[DataSeeder] Seeded " + savedCustomers.size() + " Titan Encircle loyalty customer profiles.");

            // 3. Seed 6 Months of Boutique Sales History with Realistic High Volume
            LocalDateTime now = LocalDateTime.now();
            Random rng = new Random(42);

            for (Customer c : savedCustomers) {
                int cid = c.getId();
                int orderCount;
                int maxDaysAgo;
                int minDaysAgo;

                if (cid <= 5) {
                    // Champions: frequent luxury watch collectors, recent visits to World of Titan
                    orderCount = 10 + rng.nextInt(6);
                    minDaysAgo = 2;
                    maxDaysAgo = 160;
                } else if (cid <= 12) {
                    // Loyal: Regular buyers across festivals and birthdays
                    orderCount = 6 + rng.nextInt(4);
                    minDaysAgo = 8;
                    maxDaysAgo = 170;
                } else if (cid <= 17) {
                    // Potential Loyalists: Bought Fastrack/Titan Smart recently
                    orderCount = 3 + rng.nextInt(3);
                    minDaysAgo = 5;
                    maxDaysAgo = 45;
                } else if (cid <= 21) {
                    // At-Risk: Bought expensive timepieces 3-6 months ago, but inactive recently
                    orderCount = 5 + rng.nextInt(3);
                    minDaysAgo = 70;
                    maxDaysAgo = 175;
                } else {
                    // Lost / Inactive: Single purchase 5 months ago
                    orderCount = 1;
                    minDaysAgo = 140;
                    maxDaysAgo = 175;
                }

                for (int i = 0; i < orderCount; i++) {
                    Product prod = savedProducts.get(rng.nextInt(savedProducts.size()));
                    int maxQty = (prod.getUnitPrice() < 3000) ? 5 : (prod.getUnitPrice() < 15000) ? 2 : 1;
                    int qty = 1 + rng.nextInt(maxQty);
                    double amount = Math.round(prod.getUnitPrice() * qty * 100.0) / 100.0;
                    int daysAgo = minDaysAgo + rng.nextInt(Math.max(1, maxDaysAgo - minDaysAgo + 1));
                    LocalDateTime saleDate = now.minusDays(daysAgo).minusHours(rng.nextInt(24)).minusMinutes(rng.nextInt(60));

                    Sale sale = new Sale(cid, prod.getId(), qty, amount, saleDate);
                    insertSeedSale(sale);
                }
            }

            // Seed 6 Corporate & Festive Bulk Gifting Orders (Classic Titan B2B channel)
            Customer corpClient1 = savedCustomers.get(4);  // Rajesh Malhotra (Corporate)
            Customer corpClient2 = savedCustomers.get(22); // Rameshwar Birla (Industrial)

            insertSeedSale(new Sale(corpClient1.getId(), savedProducts.get(4).getId(), 50, 50 * 2995.0, now.minusDays(35)));   // Fastrack Reflex bulk order (₹1.49L)
            insertSeedSale(new Sale(corpClient1.getId(), savedProducts.get(5).getId(), 80, 80 * 1499.0, now.minusDays(110)));  // Sonata bulk order (₹1.19L)
            insertSeedSale(new Sale(corpClient1.getId(), savedProducts.get(0).getId(), 10, 10 * 29995.0, now.minusDays(18)));  // Executive Titan Edge gifting (₹2.99L)
            insertSeedSale(new Sale(corpClient2.getId(), savedProducts.get(2).getId(), 2, 2 * 245000.0, now.minusDays(60)));    // Nebula Haute Horlogerie Gold (₹4.90L)
            insertSeedSale(new Sale(corpClient2.getId(), savedProducts.get(6).getId(), 25, 25 * 11995.0, now.minusDays(85)));  // Titan Octane Sports Awards (₹2.99L)
            insertSeedSale(new Sale(corpClient1.getId(), savedProducts.get(7).getId(), 8, 8 * 27995.0, now.minusDays(12)));    // Titan Maritime Corporate (₹2.23L)

            System.out.println("[DataSeeder] Seeded 6 months of World of Titan boutique transactions & corporate gifting orders.");

            // 4. Seed 6 Months of Operational Expenses Calibrated for Healthy Net Margin (~40-50%)
            for (int monthOffset = 5; monthOffset >= 0; monthOffset--) {
                LocalDateTime monthDate = now.minusMonths(monthOffset);

                // Boutique Leases (World of Titan Flagship Boutique)
                expenseDAO.save(new Expense("Boutique Leases", "World of Titan Flagship Boutique commercial lease", 55000.0, monthDate.withDayOfMonth(1)));

                // Horology Specialists & Retail Staff Compensation
                expenseDAO.save(new Expense("Horology Staff & Payroll", "Master watchmakers, boutique managers & retail staff compensation", 85000.0, monthDate.withDayOfMonth(28)));

                // Retail POS & SAP Enterprise ERP Subscriptions
                expenseDAO.save(new Expense("ERP & Boutique Tech", "SAP Horology Supply Chain ERP & retail billing software", 15000.0, monthDate.withDayOfMonth(5)));

                // Precision Components & Import Logistics
                double freight = 22000.0 + rng.nextDouble() * 5000.0;
                expenseDAO.save(new Expense("Component Logistics", "Precision quartz movement & sapphire crystal import freight", Math.round(freight * 100.0) / 100.0, monthDate.withDayOfMonth(15)));

                // Titan Care Warranty & Watch Restoration
                double repair = 18000.0 + rng.nextDouble() * 4000.0;
                expenseDAO.save(new Expense("Titan Care Service", "After-sales watch restoration, battery replacements & waterproofing tests", Math.round(repair * 100.0) / 100.0, monthDate.withDayOfMonth(20)));

                // Festive Celebrity Ad Blitz (Diwali / Wedding Season) -> Z-Score Spike Anomaly in monthOffset = 1!
                // Baseline marketing: ~₹35,000. Spike: ₹320,000 (9x jump!)
                double marketing = (monthOffset == 1) ? 320000.0 : (35000.0 + rng.nextDouble() * 10000.0);
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
        String sql = "INSERT INTO sales (customer_id, product_id, quantity, amount, channel, sale_date) VALUES (?, ?, ?, ?, ?, ?)";
        try (var conn = DBConnection.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sale.getCustomerId());
            ps.setInt(2, sale.getProductId());
            ps.setInt(3, sale.getQuantity());
            ps.setDouble(4, sale.getAmount());
            ps.setString(5, "Store");
            ps.setTimestamp(6, java.sql.Timestamp.valueOf(sale.getSaleDate()));
            ps.executeUpdate();
        }
    }
}
