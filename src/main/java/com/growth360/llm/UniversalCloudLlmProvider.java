package com.growth360.llm;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Universal Cloud LLM Provider
 *
 * Supports any major Cloud LLM API:
 *   1. Google Gemini (gemini-1.5-flash, gemini-1.5-pro, gemini-2.0-flash)
 *   2. Anthropic Claude (claude-3-5-sonnet, claude-3-haiku)
 *   3. DeepSeek (deepseek-chat, deepseek-reasoner)
 *   4. xAI Grok (grok-2-latest, grok-beta)
 *   5. OpenAI (gpt-4o-mini, gpt-4o)
 *   6. Custom / OpenAI-Compatible (Groq, Together AI, OpenRouter)
 */
public class UniversalCloudLlmProvider {

    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private final HttpClient httpClient;

    public UniversalCloudLlmProvider() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Dispatches inference to the requested cloud LLM provider.
     *
     * @param provider  "gemini", "anthropic", "deepseek", "grok", "openai", "custom"
     * @param apiKey    User's API key
     * @param modelName Model identifier
     * @param prompt    User prompt with MetricContext business telemetry
     * @return          Generated response text
     */
    public String generate(String provider, String apiKey, String modelName, String prompt) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API Key cannot be empty for cloud provider: " + provider);
        }

        String p = provider == null ? "gemini" : provider.trim().toLowerCase();

        return switch (p) {
            case "gemini", "google" -> generateGemini(apiKey, modelName, prompt);
            case "anthropic", "claude" -> generateAnthropic(apiKey, modelName, prompt);
            case "deepseek" -> generateOpenAiCompatible("https://api.deepseek.com/chat/completions",
                    apiKey, modelName.isBlank() ? "deepseek-chat" : modelName, prompt);
            case "grok", "xai" -> generateOpenAiCompatible("https://api.x.ai/v1/chat/completions",
                    apiKey, modelName.isBlank() ? "grok-2-latest" : modelName, prompt);
            case "openai" -> generateOpenAiCompatible("https://api.openai.com/v1/chat/completions",
                    apiKey, modelName.isBlank() ? "gpt-4o-mini" : modelName, prompt);
            default -> generateOpenAiCompatible("https://api.openai.com/v1/chat/completions",
                    apiKey, modelName.isBlank() ? "gpt-4o-mini" : modelName, prompt);
        };
    }

    private String generateGemini(String apiKey, String modelName, String prompt) throws Exception {
        String model = (modelName == null || modelName.isBlank()) ? "gemini-1.5-flash" : modelName;
        String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                model, apiKey.trim()
        );

        JSONObject requestBody = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();

        part.put("text", prompt);
        parts.put(part);
        content.put("parts", parts);
        contents.put(content);
        requestBody.put("contents", contents);

        JSONObject genConfig = new JSONObject();
        genConfig.put("maxOutputTokens", 1200);
        genConfig.put("temperature", 0.7);
        requestBody.put("generationConfig", genConfig);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .timeout(TIMEOUT)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini returned HTTP " + response.statusCode() + ": " + response.body());
        }

        JSONObject json = new JSONObject(response.body());
        return json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim();
    }

    private String generateAnthropic(String apiKey, String modelName, String prompt) throws Exception {
        String model = (modelName == null || modelName.isBlank()) ? "claude-3-5-sonnet-latest" : modelName;
        String url = "https://api.anthropic.com/v1/messages";

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        requestBody.put("max_tokens", 1200);

        JSONArray messages = new JSONArray();
        JSONObject msg = new JSONObject();
        msg.put("role", "user");
        msg.put("content", prompt);
        messages.put(msg);
        requestBody.put("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("x-api-key", apiKey.trim())
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .timeout(TIMEOUT)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Anthropic returned HTTP " + response.statusCode() + ": " + response.body());
        }

        JSONObject json = new JSONObject(response.body());
        return json.getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
                .trim();
    }

    private String generateOpenAiCompatible(String endpointUrl, String apiKey, String modelName, String prompt) throws Exception {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", modelName);
        requestBody.put("max_tokens", 1200);
        requestBody.put("temperature", 0.7);

        JSONArray messages = new JSONArray();
        JSONObject msg = new JSONObject();
        msg.put("role", "user");
        msg.put("content", prompt);
        messages.put(msg);
        requestBody.put("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl))
                .header("Authorization", "Bearer " + apiKey.trim())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .timeout(TIMEOUT)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Cloud API (" + endpointUrl + ") returned HTTP " + response.statusCode() + ": " + response.body());
        }

        JSONObject json = new JSONObject(response.body());
        return json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();
    }
}
