# GROWTH 360 AI — Precision Horology Decision Intelligence
> **Tagline**: *“Manage. Predict. Grow.”*  
> **Domain Case Study**: Titan Watches (Titan Company Limited / Tata Group)  
> **College Mini Project**: Advanced Object-Oriented Software Architecture & Decision Intelligence

---

## 1. Executive Summary & Problem Statement

Small and Medium Businesses (SMBs) and premium retail enterprises like **Titan Watches** deal with volatile footfall, festive demand surges, supply chain bottlenecks at assembly plants (e.g., Hosur, Tamil Nadu), and high customer acquisition costs.

Raw business data is usually siloed across disparate point-of-sale systems, leading to backward-looking reporting.

> **Core Value Proposition**: *“Most business dashboards tell you what happened. Growth 360 AI tells you what is happening, what is likely to happen next, and what prescriptive actions you should take right now.”*

Growth 360 AI is a **100% Pure Java 21** desktop application and embedded intelligence suite providing:
1. **Real-time Boutique Commercial Telemetry** (Revenue, Margins, Overheads)
2. **Composite Business Growth Score (0–100)** evaluated across 5 weighted dimensions
3. **Smart Inventory Reorder Point (ROP) & Safety Stock Runway Surveillance**
4. **Gaussian Rolling Z-Score Anomaly Detection** ($Z > 3.0\sigma$) for marketing spend overruns
5. **Customer RFM Quintile Segmentation** (Champions, Loyal, Potential, At-Risk, Lost)
6. **Time-Series Revenue Forecasting** via Ordinary Least Squares (OLS) Linear Regression
7. **Priority-Queue Risk Alert Triage & Prescriptive Recommendations Engine**
8. **Interactive AI Copilot Reasoning Engine (`AiCopilotEngine`)** ingesting live `MetricContext` telemetry
9. **Embedded Localhost Web Dashboard & REST API (`LocalhostCopilotServer`)** on `http://localhost:8080`

---

## 2. System Architecture & Package Directory

The codebase strictly adheres to clean layered MVC architecture:

```
src/main/java/com/growth360/
├── Main.java                          # Bootstrap entry point (DDL, seed data, UI launch)
├── model/                             # OOP Entities & Immutable Java Records
│   ├── Product.java                   # Watch catalog entity (SKU, stock, cost, price)
│   ├── Customer.java                  # Encircle loyalty member entity
│   ├── Sale.java                      # Retail transaction record
│   ├── Expense.java                   # Operating overhead log
│   ├── User.java                      # Store manager profile
│   ├── Alert.java                     # Record: System risk alert (INVENTORY, EXPENSE, etc.)
│   ├── Recommendation.java            # Record: Prescriptive business action
│   ├── GrowthScore.java               # Record: 5-factor composite business health score
│   ├── MetricContext.java             # Record: Telemetry payload ingested by AI Copilot
│   └── ForecastPoint.java             # Record: Predictive time-series coordinate
├── dao/                               # Data Access Objects (Direct JDBC + Transactions)
│   ├── ProductDAO.java                # Watch CRUD + inventory queries
│   ├── CustomerDAO.java               # Customer loyalty queries
│   ├── SaleDAO.java                   # Transactional sale recording with pessimistic lock
│   ├── ExpenseDAO.java                # Expense insertion & category breakdown
│   ├── AlertDAO.java                  # Risk alert resolution & persistence
│   └── UserDAO.java                   # User profile access
├── service/                           # Business Logic & Orchestration
│   ├── ProductService.java
│   ├── CustomerService.java
│   ├── SaleService.java
│   ├── ExpenseService.java
│   └── AnalyticsService.java          # Multithreaded analytics pipeline + PriorityQueue
├── analytics/                         # Statistical & Mathematical Engines
│   ├── GrowthScoreCalculator.java     # 5-factor mathematical score formula (25/25/20/15/15)
│   ├── ReorderPointCalculator.java    # ROP = (d * L) + SS calculation & runway
│   ├── ZScoreDetector.java            # Rolling Gaussian Z-Score anomaly detector
│   ├── RfmSegmenter.java              # Quintile Recency, Frequency, Monetary scoring
│   ├── RevenueForecaster.java         # OLS Linear Regression & Moving Average smoothing
│   └── RecommendationEngine.java      # Prescriptive rule-based decision matrix
├── copilot/                           # AI / ML Copilot Reasoning Engine
│   ├── AiCopilotEngine.java           # Contextual semantic parser & diagnostic reasoning
│   └── CopilotResponse.java           # Record: Rich narrative, metrics badges, actions
├── server/                            # Embedded Pure Java HTTP Server
│   └── LocalhostCopilotServer.java    # Web dashboard & REST API on http://localhost:8080
├── controller/                        # JavaFX UI Controllers
│   ├── DashboardController.java
│   ├── SalesController.java
│   ├── ExpenseController.java
│   ├── InventoryController.java
│   ├── CustomerController.java
│   └── AlertController.java
├── view/                              # 100% Pure Java JavaFX Desktop Views (Zero XML/CSS)
│   ├── MainView.java                  # Sidebar, header, navigation, and Copilot shortcut
│   ├── DashboardView.java             # Screen 1: Executive KPI cards, charts & forecasts
│   ├── SalesView.java                 # Screen 2: Boutique point-of-sale transactions
│   ├── ExpenseView.java               # Screen 3: Overhead expenditures & category breakdown
│   ├── InventoryView.java             # Screen 4: ROP stock runway & Hosur replenishment
│   ├── CustomerView.java              # Screen 5: Encircle RFM loyalty quintile matrix
│   ├── AlertsView.java                # Screen 6: PriorityQueue alerts & resolutions
│   └── BusinessInsightsView.java      # Screen 7: Interactive AI Copilot & Prescriptive Matrix
└── util/                              # Infrastructure & Utilities
    ├── DBConnection.java              # Dual-engine connection pool (MySQL + H2 failover)
    ├── Schema.java                    # Pure Java DDL table & index creator
    ├── DataSeeder.java                # Realistic 6-month Titan Watches dataset seeder
    └── Theme.java                     # Dark-mode design system & typography tokens
```

---

## 3. The 5 Core Mathematical & ML Algorithms

### A. Growth Score Formula (5-Factor Evaluator)
$$\text{Growth Score} = 0.25(S) + 0.25(M) + 0.20(I) + 0.15(C) + 0.15(E)$$
* $S$ (Sales Velocity, 25%): MoM revenue growth rate normalized between 0 and 100.
* $M$ (Margin Health, 25%): Operating profit margin against luxury retail benchmark ($\ge 25\% \implies 100$).
* $I$ (Inventory Balance, 20%): Percentage of catalog SKUs maintaining healthy stock above ROP.
* $C$ (Customer Health, 15%): Ratio of Encircle Champion/Loyal customers vs. churned patrons.
* $E$ (Expense Efficiency, 15%): Overhead-to-revenue ratio penalized by statistical Z-score anomalies.

### B. Inventory Reorder Point (ROP) & Safety Stock
$$ROP = (d \times L) + SS \quad \text{where} \quad SS = Z \times \sqrt{L} \times \sigma_d$$
* $d$: Mean daily demand rate over the last 60 days.
* $L$: Factory lead time in days (e.g., Hosur assembly facility = 7 days).
* $SS$: Safety stock buffer absorbing daily demand volatility $\sigma_d$ at 95% service level ($Z = 1.645$).
* $\text{Stock Runway} = \frac{\text{Current Stock}}{d}$ (in days).

### C. Gaussian Rolling Z-Score Anomaly Detection
$$Z = \frac{X - \mu}{\sigma}$$
* Detects sudden, non-random expenditure surges in overhead categories (e.g., Festive Ad Blitz).
* Any expense where $Z > 3.0$ triggers a **CRITICAL** risk alert with 99.7% statistical confidence.

### D. Customer RFM Quintile Segmentation
Ranks customers across 3 independent behavioral dimensions:
* **Recency ($R$)**: Days since last boutique timepiece purchase.
* **Frequency ($F$)**: Total number of purchases completed.
* **Monetary ($M$)**: Total revenue contributed.  
Maps customers into 5 strategic cohorts: **Champions**, **Loyal Customers**, **Potential Loyalists**, **At-Risk**, and **Lost**.

### E. Time-Series Revenue Forecasting (OLS Regression)
Fits historical monthly sales against timeline intervals:
$$y = mx + c \quad \text{where} \quad m = \frac{N\sum xy - \sum x \sum y}{N\sum x^2 - (\sum x)^2}$$
Blended with a 3-month moving average to project upcoming month boutique sales with confidence bands.

---

## 4. The AI Copilot Reasoning Engine (`AiCopilotEngine`)

The **AI Copilot** is a deterministic decision-intelligence engine designed for business executives. It ingests the consolidated immutable **`MetricContext`** record and performs semantic classification and multi-step reasoning.

### Pre-Engineered Prompt Accelerators:
1. 📈 **"Analyze current profit health"** $\to$ Analyzes gross revenue, overheads, profit margin %, and compares against luxury retail benchmarks.
2. ⚠️ **"Explain marketing expense anomaly"** $\to$ Explains Gaussian standard deviation ($Z > 3.0\sigma$), identifies the festive advertising blitz, and warns of margin erosion.
3. 📦 **"Check watch inventory and stockout risks"** $\to$ Flags items below ROP (e.g., Titan Edge Ceramic), calculates runway in days, and advises Hosur factory batch orders.
4. 👥 **"How to win back at-risk customers?"** $\to$ Identifies dormant luxury collectors and provides Encircle VIP preview & bonus points retention strategies.
5. 🏆 **"How do we improve our Growth Score?"** $\to$ Evaluates the 5 mathematical dimensions and provides a concrete roadmap to achieve an elite 90+ score.
6. 🔮 **"What is next month revenue forecast?"** $\to$ Analyzes OLS regression slope and projects sales volume.
7. 🎯 **"Give me a 30-day executive action plan"** $\to$ Synthesizes a prioritized roadmap across supply chain, fiscal control, and customer retention.

---

## 5. Dual-Interface Architecture: Desktop + Web

### 1. Native JavaFX Desktop Application (Primary)
* High-performance, dark-mode native desktop window (1360 × 850).
* 7 interactive navigation tabs + global header Copilot button.
* 100% Pure Java programmatic layout via `Theme.java` — zero CSS or FXML parsing overhead.

### 2. Embedded Localhost Web Server (Bonus / Web Access)
* Powered by `com.sun.net.httpserver.HttpServer` (Standard Java SE, zero Tomcat/Spring dependencies).
* Listens on **`http://localhost:8080`**.
* Serves a modern, dark-mode single-page executive web dashboard with live KPI cards and an interactive AI Copilot chat terminal.
* Exposes JSON REST APIs:
  * `GET /api/metrics` — Returns real-time `MetricContext` JSON.
  * `POST /api/copilot` — Accepts `{ "query": "..." }` and returns structured `CopilotResponse` JSON.
  * `GET /api/health` — System status check.

---

## 6. Database Persistence & JDBC Transactions

Growth 360 AI uses direct JDBC with resilient dual-engine failover:
* **Primary Engine**: MySQL 8.3 (`jdbc:mysql://localhost:3306/growth360`).
* **Automatic Failover**: Embedded persistent H2 2.2 (`jdbc:h2:./data/growth360;MODE=MySQL`). If MySQL is offline, the app switches to embedded H2 seamlessly with zero crashes.

### 6 Database Tables:
1. **`users`**: Store manager credentials, boutique identity, and base currency.
2. **`products`**: Watch catalog, SKUs, inventory counts, unit cost, retail price, lead time, and safety stocks.
3. **`customers`**: Encircle loyalty customer profiles, contact information, and registration dates.
4. **`sales`**: Retail transactions, sales channel (Flagship, Multi-Brand, Online, Corporate), quantities, and timestamps.
5. **`expenses`**: Operating expense logs, categories, and anomaly audit flags.
6. **`risk_alerts`**: Persistent system alerts, severity ratings, diagnosis, and resolution statuses.

### Transaction ACID Compliance (`SaleDAO.java`):
```java
conn.setAutoCommit(false); // Begin ACID transaction
// 1. SELECT stock_qty FROM products WHERE id = ? FOR UPDATE (Pessimistic Locking)
// 2. Validate stock availability
// 3. UPDATE products SET stock_qty = stock_qty - ? WHERE id = ?
// 4. INSERT INTO sales (...) VALUES (...)
conn.commit();             // Atomic commit
// On error: conn.rollback()
```

---

## 7. How to Run the Project

### Running the Desktop App:
```powershell
# Set Java 21 environment variable
$env:JAVA_HOME = "C:\Users\ASUS\.jdks\temurin-21\jdk-21.0.12.1+1"

# Launch desktop app & embedded localhost server
.\gradlew.bat run
```

### Accessing the Web Dashboard & Copilot:
While the application is running, open your web browser to:
👉 **`http://localhost:8080`**

### Running the Automated Unit Test Suite:
```powershell
.\gradlew.bat test
```
*(All 18 automated unit tests pass with a 100% success rate!)*

---

## 8. Complete External Examiner Viva Guide

### Q1: "Explain the architecture of this project."
> *"The project follows clean Layered MVC architecture. The `model` package holds our domain records and entities. The `dao` package handles JDBC persistence with atomic transactions. The `service` layer coordinates business logic. The `analytics` and `copilot` packages execute all statistical algorithms and AI reasoning. The `controller` mediates events, and the `view` package renders our 100% Pure Java JavaFX user interface. Additionally, `LocalhostCopilotServer` in `com.growth360.server` exposes a live REST API and web dashboard on port 8080."*

### Q2: "Show me where the AI / ML Copilot is implemented."
> *"The AI Copilot lives in `com.growth360.copilot.AiCopilotEngine`. It consumes our immutable `MetricContext` record produced by `AnalyticsService`. It parses user queries into intent categories—such as Financial Health, Statistical Anomalies, Inventory ROP, Customer Retention, and 30-Day Action Plans—and synthesizes structured `CopilotResponse` records containing executive headlines, narrative proofs, key metric badges, and prescriptive actions. In the GUI, it is accessible on Screen 7 ('AI Copilot & Insights') and via the top-header quick button."*

### Q3: "Where is the Collections Framework used?"
> *"We use multiple data structures from the Java Collections Framework:  
> 1. `List<Sale>` and `List<Product>` for ordered entity collections.  
> 2. `Map<String, Double>` (specifically `TreeMap` for chronological sorting) for time-series revenue and expense breakdowns.  
> 3. `Set<Integer>` for unique customer and SKU lookups.  
> 4. **`PriorityQueue<Alert>`** in `AnalyticsService` to prioritize alerts based on severity: `CRITICAL` alerts are dequeued first before `WARNING` and `INFO`."*

### Q4: "What happens if MySQL is not running on my machine?"
> *"Our `DBConnection` class has an automatic failover mechanism. It attempts to connect to MySQL first; if unavailable, it seamlessly falls back to an embedded persistent H2 database in MySQL compatibility mode. The application and all database transactions continue running without crashing."*

### Q5: "How does the Z-Score Anomaly Detector work?"
> *"In `ZScoreDetector.java`, we calculate the mean $\mu$ and standard deviation $\sigma$ across expense categories. For each category, we compute $Z = (X - \mu)/\sigma$. If $Z > 3.0$, it falls in the extreme 0.3% Gaussian tail, indicating a statistically significant anomaly rather than normal variation. In our Titan Watches case study, this catches our festive Diwali marketing campaign blitz."*

### Q6: "How do you ensure UI responsiveness during heavy statistical calculations?"
> *"We decouple the JavaFX Application Thread from computational logic using Java Multithreading. `AnalyticsService` submits analytical calculations to an `ExecutorService` thread pool. Once computation finishes, it dispatches the result back onto the UI thread via `Platform.runLater()`."*
