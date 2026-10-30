package com.growth360.service;

import com.growth360.dao.CustomerDAO;
import com.growth360.dao.ProductDAO;
import com.growth360.model.Customer;
import com.growth360.model.Product;
import com.growth360.util.DBConnection;

import java.io.BufferedReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Universal CSV Importer for Real-World Retail Datasets.
 * Supports importing custom sales transactions and product inventories from CSV files
 * (e.g., Kaggle Superstore, Shopify exports, or enterprise POS extracts).
 */
public class CsvDataService {

    private final ProductDAO productDAO;
    private final CustomerDAO customerDAO;

    public record ImportResult(boolean success, int importedCount, int skippedCount, String message) {}

    public CsvDataService() {
        this.productDAO = new ProductDAO();
        this.customerDAO = new CustomerDAO();
    }

    /**
     * Imports sales transactions from CSV.
     * Expected columns (order flexible, case-insensitive headers):
     *   customer_name (or customer_id), product_name (or product_id), quantity, amount (or price), channel, sale_date
     */
    public ImportResult importSalesCsv(String csvContent) {
        if (csvContent == null || csvContent.isBlank()) {
            return new ImportResult(false, 0, 0, "CSV content is empty.");
        }

        try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return new ImportResult(false, 0, 0, "CSV header is missing.");
            }

            String delimiter = detectDelimiter(headerLine);
            String[] headers = splitLine(headerLine, delimiter);
            Map<String, Integer> colMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                colMap.put(headers[i].trim().toLowerCase().replaceAll("[^a-z0-9_]", ""), i);
            }

            int custCol = findColumn(colMap, "customer_name", "customer", "customer_id", "client");
            int prodCol = findColumn(colMap, "product_name", "product", "product_id", "item", "sku");
            int qtyCol  = findColumn(colMap, "quantity", "qty", "units", "count");
            int amtCol  = findColumn(colMap, "amount", "total", "revenue", "price", "sales");
            int chCol   = findColumn(colMap, "channel", "type", "store_type");
            int dateCol = findColumn(colMap, "sale_date", "date", "timestamp", "order_date");

            if (prodCol == -1 || amtCol == -1) {
                return new ImportResult(false, 0, 0,
                        "Required columns missing. CSV must include product and amount/price columns.");
            }

            // Preload existing products and customers for fast lookup
            Map<String, Product> productByName = new HashMap<>();
            for (Product p : productDAO.findAll()) {
                productByName.put(p.getName().trim().toLowerCase(), p);
            }

            Map<String, Customer> customerByName = new HashMap<>();
            for (Customer c : customerDAO.findAll()) {
                customerByName.put(c.getName().trim().toLowerCase(), c);
            }

            // Fallback default entities if names are missing
            Product defaultProduct = productByName.values().stream().findFirst().orElse(null);
            Customer defaultCustomer = customerByName.values().stream().findFirst().orElse(null);

            String insertSql = "INSERT INTO sales (customer_id, product_id, quantity, amount, channel, sale_date) VALUES (?, ?, ?, ?, ?, ?)";
            int imported = 0;
            int skipped = 0;

            try (Connection conn = DBConnection.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank()) continue;
                        String[] parts = splitLine(line, delimiter);
                        if (parts.length <= Math.max(prodCol, amtCol)) {
                            skipped++;
                            continue;
                        }

                        try {
                            // 1. Resolve product
                            String prodStr = getPart(parts, prodCol);
                            Product prod = productByName.get(prodStr.toLowerCase());
                            if (prod == null) {
                                // Create new product on the fly if not found
                                prod = new Product(prodStr, "Imported Collection", 50, 1000.0, 2500.0, 7, 10);
                                productDAO.save(prod);
                                productByName.put(prodStr.toLowerCase(), prod);
                            }

                            // 2. Resolve customer
                            String custStr = custCol != -1 ? getPart(parts, custCol) : "Walk-in Customer";
                            Customer cust = customerByName.get(custStr.toLowerCase());
                            if (cust == null) {
                                cust = new Customer(custStr, custStr.replaceAll("\\s+", ".").toLowerCase() + "@titan.customer", "+91 98000 00000");
                                customerDAO.save(cust);
                                customerByName.put(custStr.toLowerCase(), cust);
                            }

                            // 3. Resolve quantity & amount
                            int qty = qtyCol != -1 ? parsePositiveInt(getPart(parts, qtyCol), 1) : 1;
                            double amount = parsePositiveDouble(getPart(parts, amtCol), prod.getUnitPrice() * qty);

                            // 4. Resolve channel
                            String channel = chCol != -1 && !getPart(parts, chCol).isBlank() ? getPart(parts, chCol) : "Store";

                            // 5. Resolve date
                            LocalDateTime saleDate = dateCol != -1 ? parseDateTime(getPart(parts, dateCol)) : LocalDateTime.now();

                            ps.setInt(1, cust.getId());
                            ps.setInt(2, prod.getId());
                            ps.setInt(3, qty);
                            ps.setDouble(4, amount);
                            ps.setString(5, channel);
                            ps.setTimestamp(6, Timestamp.valueOf(saleDate));
                            ps.addBatch();
                            imported++;

                            if (imported % 500 == 0) {
                                ps.executeBatch();
                            }
                        } catch (Exception parseEx) {
                            skipped++;
                        }
                    }
                    ps.executeBatch();
                    conn.commit();
                } catch (Exception ex) {
                    conn.rollback();
                    throw ex;
                } finally {
                    conn.setAutoCommit(true);
                }
            }

            return new ImportResult(true, imported, skipped,
                    "Successfully imported " + imported + " sales transactions (" + skipped + " skipped).");

        } catch (Exception e) {
            return new ImportResult(false, 0, 0, "Error importing CSV: " + e.getMessage());
        }
    }

    /**
     * Generates a sample CSV template for sales transactions.
     */
    public String getSampleSalesCsv() {
        return """
                customer_name,product_name,quantity,amount,channel,sale_date
                Aarav Sharma,Titan Edge Ceramic Slimline,1,29995.0,Store,2026-09-15 14:30:00
                Priya Patel,Titan Raga Viva Rose Gold Pearl,2,19990.0,Store,2026-09-18 16:15:00
                Vikramaditya Rao,Nebula 18K Solid Gold Mechanical,1,245000.0,Titan E-Store,2026-09-22 11:00:00
                Sneha Kulkarni,Fastrack Reflex Beat+ Smartwatch,1,2995.0,Store,2026-09-25 18:45:00
                Rajesh Malhotra,Titan Octane Chrono Tachymeter,5,59975.0,Corporate Gifting,2026-10-02 10:20:00
                """;
    }

    private static String detectDelimiter(String header) {
        if (header.contains("\t")) return "\t";
        if (header.contains(";")) return ";";
        return ",";
    }

    private static String[] splitLine(String line, String delimiter) {
        // Basic CSV split supporting quoted tokens
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (String.valueOf(c).equals(delimiter) && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens.toArray(new String[0]);
    }

    private static int findColumn(Map<String, Integer> colMap, String... candidates) {
        for (String c : candidates) {
            String norm = c.toLowerCase().replaceAll("[^a-z0-9_]", "");
            if (colMap.containsKey(norm)) return colMap.get(norm);
        }
        return -1;
    }

    private static String getPart(String[] parts, int idx) {
        if (idx >= 0 && idx < parts.length) {
            return parts[idx].trim().replaceAll("^\"|\"$", "");
        }
        return "";
    }

    private static int parsePositiveInt(String s, int fallback) {
        try {
            int val = Integer.parseInt(s.replaceAll("[^0-9-]", ""));
            return val > 0 ? val : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static double parsePositiveDouble(String s, double fallback) {
        try {
            double val = Double.parseDouble(s.replaceAll("[^0-9.-]", ""));
            return val > 0 ? val : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) return LocalDateTime.now();
        s = s.trim();
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ISO_LOCAL_DATE
        );
        for (DateTimeFormatter fmt : formatters) {
            try {
                if (fmt == DateTimeFormatter.ISO_LOCAL_DATE || fmt.toString().contains("yyyy-MM-dd") && !s.contains(" ")) {
                    LocalDate ld = LocalDate.parse(s, fmt);
                    return ld.atTime(12, 0);
                }
                return LocalDateTime.parse(s, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        return LocalDateTime.now();
    }
}
