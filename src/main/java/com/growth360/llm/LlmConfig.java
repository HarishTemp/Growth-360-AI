package com.growth360.llm;

/**
 * Configuration record for the LLM provider.
 * Stored in memory; can be updated at runtime via the dashboard settings panel.
 */
public record LlmConfig(
        String provider,    // "ollama", "gemini", "anthropic", "deepseek", "grok", "openai", "custom"
        String apiKey,      // Cloud API key (Gemini, Anthropic, DeepSeek, Grok, OpenAI)
        String modelName    // e.g. "gemma4:latest", "gemini-1.5-flash", "claude-3-5-sonnet-latest", "deepseek-chat", "grok-2-latest"
) {
    public static final String PROVIDER_OLLAMA    = "ollama";
    public static final String PROVIDER_GEMINI    = "gemini";
    public static final String PROVIDER_ANTHROPIC = "anthropic";
    public static final String PROVIDER_DEEPSEEK  = "deepseek";
    public static final String PROVIDER_GROK      = "grok";
    public static final String PROVIDER_OPENAI    = "openai";
    public static final String PROVIDER_NONE      = "none";

    /** Default config: Local Ollama with gemma4:latest. */
    public static LlmConfig defaultConfig() {
        return new LlmConfig(PROVIDER_OLLAMA, "", "gemma4:latest");
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
