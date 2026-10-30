package com.growth360.agents;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Immutable result record produced by every ML Agent.
 * Contains the agent name, model used, key findings, confidence score,
 * and scientific evaluation metrics (R2, RMSE, Silhouette, Outlier Rate, etc.).
 */
public record AgentResult(
        String agentName,
        String modelUsed,
        String headline,
        String summary,
        List<String> findings,
        double confidenceScore,           // 0.0 - 1.0
        boolean trained,                  // false if insufficient data for model training
        Map<String, Double> evaluationMetrics // Scientific ML metrics (e.g. R2, RMSE, WCSS)
) {
    /** Backward-compatible constructor defaulting evaluationMetrics to empty map */
    public AgentResult(
            String agentName,
            String modelUsed,
            String headline,
            String summary,
            List<String> findings,
            double confidenceScore,
            boolean trained
    ) {
        this(agentName, modelUsed, headline, summary, findings, confidenceScore, trained, Collections.emptyMap());
    }

    /** Convenience factory when model could not be trained (fallback mode). */
    public static AgentResult fallback(String agentName, String reason) {
        return new AgentResult(
                agentName,
                "Rule-Based Fallback",
                "Insufficient data to train ML model",
                reason,
                List.of(),
                0.0,
                false,
                Collections.emptyMap()
        );
    }
}
