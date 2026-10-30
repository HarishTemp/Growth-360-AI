package com.growth360.llm;

import com.growth360.model.MetricContext;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * LLM Provider Service — The Brain of the AI Copilot
 *
 * Implements a dual-mode architecture:
 *   Mode 1: 🦙 Local Ollama (e.g. gemma4:latest, moondream:latest) — Free, private, offline
 *   Mode 2: 🌐 Universal Cloud APIs (Gemini, Anthropic, DeepSeek, Grok, OpenAI) — Multi-cloud key support
 *   Fallback: 📋 Analytical Decision Engine (Rule-based horology intelligence)
 */
public class LlmProviderService {

    private static final Logger log = Logger.getLogger(LlmProviderService.class.getName());

    private final OllamaProvider ollamaProvider;
    private final UniversalCloudLlmProvider cloudProvider;

    private final AtomicReference<LlmConfig> config;
    private volatile String activeProviderStatus = "Checking LLM...";

    public LlmProviderService() {
        this.ollamaProvider = new OllamaProvider();
        this.cloudProvider  = new UniversalCloudLlmProvider();
        this.config         = new AtomicReference<>(LlmConfig.defaultConfig());
        refreshStatus();
    }

    public void updateConfig(LlmConfig newConfig) {
        config.set(newConfig);
        refreshStatus();
        log.info("[LlmProviderService] Config updated: provider=" + newConfig.provider() +
                ", model=" + newConfig.modelName());
    }

    public LlmConfig getConfig() {
        return config.get();
    }

    public String getActiveProviderStatus() {
        return activeProviderStatus;
    }

    public boolean isOllamaAvailable() {
        return ollamaProvider.isAvailable();
    }

    public List<String> getInstalledOllamaModels() {
        return ollamaProvider.getInstalledModels();
    }

    private void refreshStatus() {
        LlmConfig cfg = config.get();
        if (LlmConfig.PROVIDER_OLLAMA.equalsIgnoreCase(cfg.provider())) {
            if (ollamaProvider.isAvailable()) {
                String model = ollamaProvider.resolveBestModel(cfg.modelName());
                activeProviderStatus = "🦙 Ollama (" + model + ") — Local Offline";
            } else {
                activeProviderStatus = "🦙 Ollama Offline (Start 'ollama serve')";
            }
        } else if (cfg.hasApiKey()) {
            activeProviderStatus = "🌐 " + cfg.provider().toUpperCase() + " (" + cfg.modelName() + ") — Cloud";
        } else {
            activeProviderStatus = "📋 Rule-Based Engine (Analytical)";
        }
    }

    /**
     * Core method: generates an AI response to a user query.
     */
    public String generateResponse(String query, MetricContext context) {
        String prompt = buildPrompt(query, context);
        LlmConfig cfg = config.get();

        // 1. If configured for Ollama, run locally
        if (LlmConfig.PROVIDER_OLLAMA.equalsIgnoreCase(cfg.provider())) {
            if (ollamaProvider.isAvailable()) {
                try {
                    String modelName = ollamaProvider.resolveBestModel(cfg.modelName());
                    String response = ollamaProvider.generate(modelName, prompt);
                    activeProviderStatus = "🦙 Ollama (" + modelName + ") — Local Offline";
                    log.info("[LlmProviderService] Response via Ollama (" + modelName + ")");
                    return response;
                } catch (Exception e) {
                    log.warning("[LlmProviderService] Local Ollama failed: " + e.getMessage());
                }
            }
        }

        // 2. If configured for Cloud API (Gemini, Anthropic, DeepSeek, Grok, OpenAI)
        if (cfg.hasApiKey() && !LlmConfig.PROVIDER_OLLAMA.equalsIgnoreCase(cfg.provider())) {
            try {
                String response = cloudProvider.generate(cfg.provider(), cfg.apiKey(), cfg.modelName(), prompt);
                activeProviderStatus = "🌐 " + cfg.provider().toUpperCase() + " (" + cfg.modelName() + ") — Cloud";
                log.info("[LlmProviderService] Response via " + cfg.provider() + " API");
                return response;
            } catch (Exception e) {
                log.warning("[LlmProviderService] Cloud API failed: " + e.getMessage());
            }
        }

        // Fallback: If cloud key failed or Ollama not running
        activeProviderStatus = "📋 Rule-Based Horology Engine";
        return null;
    }

    /**
     * Builds a prompt for the LLM.
     */
    private String buildPrompt(String query, MetricContext context) {
        NumberFormat inr = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        inr.setMaximumFractionDigits(0);

        return String.format("""
                You are Growth 360 AI, an executive decision intelligence copilot for Titan Watches \
                (Titan Company Limited, part of the Tata Group). You analyze sales, margins, inventory ROP, \
                customer churn probability, and operational expenses.

                CURRENT TITAN WATCHES BUSINESS TELEMETRY:
                =========================================
                Financials:
                  - Boutique Revenue: %s
                  - Overheads / Expenses: %s
                  - Net Profit: %s (Margin: %.1f%%)
                  - MoM Revenue Growth: %.1f%%

                Composite Growth Score: %d/100 (Health Band: %s)

                Supply Chain & Inventory:
                  - SKUs Requiring Urgent Reorder (Below ROP): %d
                  - Top Moving Horology Line: %s

                Customer & Loyalty Intelligence:
                  - Active Encircle Loyalty Members: %d
                  - At-Risk Collectors (High Churn Probability): %d

                Risk Alerts: %d priority surveillance flags

                =========================================
                USER EXECUTIVE QUESTION: %s

                Instructions:
                - Ground your analysis firmly in the live numbers shown above.
                - Keep your tone executive, strategic, and concise.
                - Structure with clear bullet points and bold financial metrics.
                - Provide 2-3 direct prescriptive actions for the store director.
                """,
                inr.format(context.totalRevenue()),
                inr.format(context.totalExpenses()),
                inr.format(context.netProfit()),
                context.profitMargin(),
                context.momRevenueGrowth(),
                context.growthScore().overallScore(),
                context.growthScore().healthBand(),
                context.inventoryRisks().size(),
                context.topProduct(),
                context.activeCustomerCount(),
                context.atRiskCustomerCount(),
                context.activeAlerts().size(),
                query
        );
    }
}
