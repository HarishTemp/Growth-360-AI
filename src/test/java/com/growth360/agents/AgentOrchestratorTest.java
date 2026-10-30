package com.growth360.agents;

import com.growth360.analytics.ReorderPointCalculator;
import com.growth360.dao.CustomerDAO;
import com.growth360.dao.ExpenseDAO;
import com.growth360.dao.ProductDAO;
import com.growth360.dao.SaleDAO;
import com.growth360.model.Customer;
import com.growth360.model.Expense;
import com.growth360.model.Product;
import com.growth360.model.Sale;
import com.growth360.util.DataSeeder;
import com.growth360.util.Schema;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AgentOrchestratorTest {

    @BeforeAll
    static void setUp() throws Exception {
        Schema.initSchema();
        DataSeeder.seedIfEmpty();
    }

    @Test
    @DisplayName("Test Parallel ML Agent Training & Inference (Random Forest, Isolation Forest, Logistic Regression, K-Means)")
    void testAllAgentsExecution() throws Exception {
        SaleDAO saleDAO = new SaleDAO();
        ExpenseDAO expenseDAO = new ExpenseDAO();
        CustomerDAO customerDAO = new CustomerDAO();
        ProductDAO productDAO = new ProductDAO();

        List<Sale> sales = saleDAO.findAll();
        List<Expense> expenses = expenseDAO.findAll();
        List<Customer> customers = customerDAO.findAll();
        List<Product> products = productDAO.findAll();

        ReorderPointCalculator reorderCalc = new ReorderPointCalculator();
        var metrics = reorderCalc.calculateMetrics(products, sales);

        assertFalse(sales.isEmpty(), "Sales must not be empty");
        assertFalse(expenses.isEmpty(), "Expenses must not be empty");
        assertFalse(customers.isEmpty(), "Customers must not be empty");
        assertFalse(products.isEmpty(), "Products must not be empty");

        AgentOrchestrator orchestrator = new AgentOrchestrator();
        Map<String, AgentResult> results = orchestrator.runAllAgents(sales, expenses, customers, metrics, products);

        assertNotNull(results, "Agent results should not be null");
        assertEquals(4, results.size(), "Should have results for all 4 agents");

        System.out.println("=== ML AGENT TEST RESULTS ===");
        results.forEach((k, v) -> {
            System.out.println("[" + k + "] Algo: " + v.modelUsed() + " | Trained: " + v.trained() + " | Confidence: " + v.confidenceScore());
            System.out.println("     Headline: " + v.headline());
            System.out.println("     Summary:  " + v.summary());
        });

        // 1. ForecastAgent check
        AgentResult forecast = results.get("forecast");
        assertNotNull(forecast);
        assertTrue(forecast.modelUsed().contains("Random Forest"));
        assertTrue(forecast.trained());
        assertTrue(forecast.summary().length() > 0);
        assertNotNull(forecast.evaluationMetrics());
        assertTrue(forecast.evaluationMetrics().containsKey("r2Score"));
        assertTrue(forecast.evaluationMetrics().containsKey("rmseInr"));

        // 2. AnomalyDetectionAgent check
        AgentResult anomaly = results.get("anomaly");
        assertNotNull(anomaly);
        assertEquals("Isolation Forest (Smile ML)", anomaly.modelUsed());
        assertNotNull(anomaly.summary());
        assertNotNull(anomaly.evaluationMetrics());
        assertTrue(anomaly.evaluationMetrics().containsKey("contaminationRate"));
        assertTrue(anomaly.evaluationMetrics().containsKey("maxAnomalyScore"));

        // 3. ChurnPredictionAgent check
        AgentResult churn = results.get("churn");
        assertNotNull(churn);
        assertTrue(churn.modelUsed().contains("Logistic Regression"));
        assertNotNull(churn.summary());
        assertNotNull(churn.evaluationMetrics());
        assertTrue(churn.evaluationMetrics().containsKey("trainingAccuracy"));
        assertTrue(churn.evaluationMetrics().containsKey("logLoss"));

        // 4. InventoryReplenishmentAgent check
        AgentResult inventory = results.get("inventory");
        assertNotNull(inventory);
        assertTrue(inventory.modelUsed().contains("K-Means"));
        assertNotNull(inventory.summary());
        assertNotNull(inventory.evaluationMetrics());
        assertTrue(inventory.evaluationMetrics().containsKey("distortionWCSS"));

        // 5. Test Active Learning / Human-in-the-Loop Feedback Loop
        orchestrator.recordFeedback("anomaly", "Renovation", "dismiss");
        Map<String, AgentResult> postFeedback = orchestrator.runAllAgents(sales, expenses, customers, metrics, products);
        assertEquals(1.0, postFeedback.get("anomaly").evaluationMetrics().get("humanWhitelistedCount"));
        System.out.println("✅ Active Learning Feedback successfully applied and persisted in agent pipeline!");
    }
}
