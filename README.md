# GROWTH 360 AI
### “Manage. Predict. Grow.”

**Intelligent Decision Support, Prescriptive Surveillance & Horology Analytics for Enterprise SMBs**  
*Titan Company Limited • Watches & Wearables Division Case Study*  
*Developed for CSE-AIML Mini Project • Team GIT BLAME*

---

## 1. Executive Problem Statement & USP
Small and Medium Businesses (SMBs) and boutique luxury retail networks capture substantial transactional data across Point-of-Sale (POS) terminals, ERP systems, and CRM loyalty programs. However, raw data is often siloed, forcing managers to rely on backward-looking reporting.

> **USP**: *“Most business dashboards tell you what happened. Growth 360 AI tells you what is happening, what is likely to happen next, and what you should do about it.”*

Growth 360 AI is a 100% pure-Java desktop decision intelligence system converting raw transactions into:
1. **Real-time Business KPIs & Revenue Trends**
2. **Composite Business Growth Score (0–100)** across 5 strategic dimensions
3. **Inventory Reorder Point (ROP) & Stock Runway Surveillance**
4. **Statistical Rolling Z-Score Anomaly Detection** for cost overruns and revenue dips
5. **Customer RFM Quintile Segmentation** (Champions, Loyal, Potential, At-Risk, Lost)
6. **Time-Series Revenue Forecasting** using Ordinary Least Squares (OLS) Regression and Moving Average smoothing
7. **Priority-Queue Risk Alerts & Prescriptive Business Interventions**
8. **Integrated AI Copilot Telemetry Engine (`MetricContext`)**

---

## 2. Technology Stack & Design Architecture

* **Programming Language**: Java 21 LTS (Records, Text Blocks, Pattern Matching, Stream API)
* **GUI Framework**: JavaFX 21.0.2 — **100% Pure Java Desktop Design System** (Zero external CSS or FXML dependencies)
* **Data Persistence**: Direct JDBC with Dual-Engine Resilience:
  * **Primary**: MySQL 8.3 Connector/J (`jdbc:mysql://localhost:3306/growth360`)
  * **Failover**: Embedded H2 2.2 (`jdbc:h2:./data/growth360;MODE=MySQL`) for zero-configuration viva execution
* **Concurreny & Multithreading**: Background `ExecutorService` daemon worker pools with thread synchronization via `javafx.application.Platform.runLater()`
* **Build System**: Maven (`pom.xml`) & Gradle (`build.gradle`)
* **Unit Testing**: JUnit Jupiter 5.10.2

```
                           GROWTH 360 AI
                         JAVA MINI PROJECT
                                │
               ┌────────────────┴────────────────┐
               ▼                                 ▼
          FRONTEND                            BACKEND
               │                                 │
               ▼                                 │
         JavaFX 21 (Pure Java)                   │
               │                                 │
     ┌─────────┼─────────┐                       │
     ▼         ▼         ▼                       ▼
  Dashboard  Charts    KPI Cards             CORE JAVA
     │         │         │                       │
     └─────────┴─────────┘             ┌─────────┼──────────┐
                                       ▼         ▼          ▼
                                      OOP   Collections  Multithreading
                                       │         │          │
                                       ▼         ▼          ▼
                                    Models      Lists      Background
                                    DAOs        Maps       Analytics
                                    Services    Sets
                                                PriorityQueue
                                       │         │
                                       └────┬────┘
                                            ▼
                                   APPLICATION LOGIC
                                            │
                           ┌────────────────┼────────────────┐
                           ▼                ▼                ▼
                    Growth Score       Business Rules   Recommendations
                       0–100            & Prescriptions   Matrix (7th Tab)
                           │                │
                           └───────┬────────┘
                                   ▼
                           ANALYTICAL ENGINE
                                   │
                 ┌─────────────────┼─────────────────┐
                 ▼                 ▼                 ▼
          Rolling Z-Score         ROP          RFM Segmentation
          Anomaly Detection     Inventory         Customers
                 │                 │                 │
                 └─────────────────┼─────────────────┘
                                   ▼
                                  JDBC
                                   │
                                   ▼
                        MySQL 8.3 / Embedded H2
```

---

## 3. Core Analytical Engines & Mathematical Formulations

### A. Holistic Growth Score Engine (0–100 Index)
Evaluates five weighted operational dimensions strictly adhering to specification:
$$\text{Growth Score} = 0.25(\text{Sales Velocity}) + 0.25(\text{Margin Health}) + 0.20(\text{Inventory Balance}) + 0.15(\text{Customer Health}) + 0.15(\text{Expense Efficiency})$$

1. **Sales Velocity (25%)**:
   $$\text{MoM Growth} = \left(\frac{\text{Current Rev} - \text{Prev Rev}}{\text{Prev Rev}}\right) \times 100, \quad \text{Score} = \text{clamp}\left(\frac{\text{MoM Growth}}{10\%} \times 100, 0, 100\right)$$
2. **Net Margin Health (25%)**:
   $$\text{Net Margin} = \left(\frac{\text{Revenue} - \text{Expenses}}{\text{Revenue}}\right) \times 100, \quad \text{Score} = \text{clamp}\left(\frac{\text{Net Margin}}{40\%} \times 100, 0, 100\right)$$
3. **Inventory Balance (20%)**:
   Identifies percentage of SKUs maintaining optimal stock runway ($7 \le \text{Days} \le 45$):
   $$\text{Score} = \left(\frac{\text{Healthy SKUs}}{\text{Total SKUs}}\right) \times 100$$
4. **Customer Health (15%)**:
   Percentage of buyers active within the last 60 rolling days:
   $$\text{Score} = \left(\frac{\text{Active Customers}}{\text{Total Customers}}\right) \times 100$$
5. **Expense Efficiency (15%)**:
   Operating Expense Ratio (OER): $\text{OER} = \left(\frac{\text{Expenses}}{\text{Revenue}}\right) \times 100$.
   * $\text{OER} \le 30\% \implies 100$
   * $\text{OER} \ge 60\% \implies 0$
   * $30\% < \text{OER} < 60\% \implies 100 - \left(\frac{\text{OER} - 30}{30} \times 100\right)$

### B. Statistical Z-Score Anomaly Detection
Detects statistically significant spikes in expenditure or dips in sales:
$$Z = \frac{X - \mu}{\sigma}, \quad \text{where } \sigma = \sqrt{\frac{\sum (X_i - \mu)^2}{N - 1}}$$
* $|Z| \ge 2.0 \implies \text{WARNING}$
* $|Z| \ge 3.0 \implies \text{CRITICAL}$ (e.g. *Festive Season Celebrity Ad Blitz* $Z > 3.2$).

### C. Intelligent Inventory Reorder Point (ROP) & Stock Runway
* **Average Daily Demand ($D$)**: $\frac{\text{Units Sold in 60-Day Window}}{60}$
* **Reorder Point ($\text{ROP}$)**: $(D \times \text{Lead Time Days}) + \text{Safety Stock}$
* **Stock Runway (Days)**: $\frac{\text{Current Stock}}{D}$
* **Target Stock Quantity**: $D \times (\text{Lead Time} + 14) + \text{Safety Stock}$
* **Suggested Order**: $\max(0, \text{Target Stock} - \text{Current Stock})$

### D. Customer RFM Quintile Segmentation
Ranks customers across Recency, Frequency, and Monetary quintiles into 5 cohorts:
1. **Champions**: Recent luxury repeat buyers (Nebula 18K Gold, Titan Edge).
2. **Loyal Customers**: Regular seasonal and festive buyers.
3. **Potential Loyalists**: Recent smart wearable buyers with high retention potential.
4. **At-Risk**: High historical spenders inactive for over 70 days.
5. **Lost / Inactive**: Dormant accounts requiring promotional reactivation.

### E. Time-Series Revenue Forecasting
Combines **Ordinary Least Squares (OLS) Linear Regression** ($y = mx + b$) with a **3-period Moving Average** to project the next 3 months of revenue with $\pm 12\%$ confidence bounds.

---

## 4. Key Java Concepts Demonstrated for Viva

| Concept | Implementation in Code |
| :--- | :--- |
| **Java Records** | [`GrowthScore`](file:///c:/Users/ASUS/Documents/java_project/src/main/java/com/growth360/model/GrowthScore.java), [`MetricContext`](file:///c:/Users/ASUS/Documents/java_project/src/main/java/com/growth360/model/MetricContext.java), [`Alert`](file:///c:/Users/ASUS/Documents/java_project/src/main/java/com/growth360/model/Alert.java), [`Recommendation`](file:///c:/Users/ASUS/Documents/java_project/src/main/java/com/growth360/model/Recommendation.java), [`ForecastPoint`](file:///c:/Users/ASUS/Documents/java_project/src/main/java/com/growth360/model/ForecastPoint.java). |
| **Collections Framework** | `List<Sale>`, `Map<Integer, Double>`, `Set<Integer>`, and **`PriorityQueue<Alert>`** in [`AnalyticsService.java`](file:///c:/Users/ASUS/Documents/java_project/src/main/java/com/growth360/service/AnalyticsService.java). |
| **Multithreading** | Daemon thread pools via `Executors.newFixedThreadPool(2)` decoupling heavy statistics from the JavaFX UI loop. |
| **JDBC Transactions** | `conn.setAutoCommit(false)`, `SELECT FOR UPDATE` pessimistic locking, stock deduction, and `conn.rollback()` in [`SaleDAO.java`](file:///c:/Users/ASUS/Documents/java_project/src/main/java/com/growth360/dao/SaleDAO.java). |
| **Pure Java GUI** | Dark-themed JavaFX design system in [`Theme.java`](file:///c:/Users/ASUS/Documents/java_project/src/main/java/com/growth360/util/Theme.java) — Zero external XML or CSS file vulnerabilities. |

---

## 5. Database Schema & Tables

1. **`users`**: Business entity, store manager profile, and default currency (INR ₹).
2. **`products`**: Timepiece catalog, SKU codes, categories, unit cost, retail price, lead time, and safety stocks.
3. **`customers`**: Encircle loyalty customer profiles, contact metadata, and registration history.
4. **`sales`**: Completed transactions, retail channel, quantity, total amount, and timestamp.
5. **`expenses`**: Operational expenditures, categorized overheads, and anomaly audit logs.
6. **`risk_alerts`**: Persistent operational alerts, severity ratings, diagnosis, and resolution states.

Standalone DDL script available at: [`database/schema.sql`](file:///c:/Users/ASUS/Documents/java_project/database/schema.sql).

---

## 6. Execution & Installation Guide

### Prerequisites
* Java 21+ JDK installed
* MySQL 8.0+ (Optional: Embedded H2 automatically activates if MySQL is absent)

### Environment Variables (Optional)
```bash
set GROWTH360_DB_URL=jdbc:mysql://localhost:3306/growth360
set GROWTH360_DB_USER=root
set GROWTH360_DB_PASSWORD=your_password
```

### Run using Gradle Wrapper
```bash
.\gradlew.bat run
```

### Run Tests
```bash
.\gradlew.bat test
```

---

## 7. Viva Q&A Quick Reference

* **Q: Why is PriorityQueue used for Alerts?**  
  *A: Business risks have varying urgency. A `PriorityQueue<Alert>` ensures `CRITICAL` stockouts and cost surges are evaluated first ($O(1)$ head retrieval) before `WARNING` and `INFO` notices.*
* **Q: How does the application handle database disconnection during a demo?**  
  *A: [`DBConnection.java`](file:///c:/Users/ASUS/Documents/java_project/src/main/java/com/growth360/util/DBConnection.java) implements transparent failover to an embedded persistent H2 database in MySQL compatibility mode. The app never crashes if local MySQL is offline.*
* **Q: How does `MetricContext` prepare for an AI Copilot?**  
  *A: It aggregates all operational KPIs, growth indicators, active alerts, and top products into an immutable payload ready to be ingested by local or cloud ML/LLM copilot inference pipelines.*
