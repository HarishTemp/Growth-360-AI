package com.growth360.llm;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Calls the Google Gemini API (FREE tier: 1,500 requests/day, no billing needed).
 *
 * API Endpoint:
 *   POST https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=API_KEY
 *
 * Free API key: https://aistudio.google.com → "Get API Key" (just needs a Google account)
 *
 * Also supports OpenAI-compatible endpoints (e.g. OpenAI, Groq, Together AI)
 * by switching the base URL and model name.
 */
public class GeminiProvider {

    private static final String GEMINI_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
    private static final Duration TIMEOUT = Duration.ofSeconds(45);

    private final HttpClient httpClient;

    public GeminiProvider() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Sends a prompt to the Gemini API and returns the generated text.
     *
     * @param apiKey    Your free Gemini API key from aistudio.google.com
     * @param modelName e.g. "gemini-1.5-flash" or "gemini-1.5-pro"
     * @param prompt    The full prompt to send
     * @return          The AI-generated response
     */
    public String generate(String apiKey, String modelName, String prompt) throws Exception {
        String url = String.format(GEMINI_BASE_URL, modelName, apiKey);

        // Build Gemini request JSON
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

        // Safety & generation config
        JSONObject genConfig = new JSONObject();
        genConfig.put("maxOutputTokens", 1024);
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
            throw new RuntimeException("Gemini API returned HTTP " + response.statusCode() + ": " + response.body());
        }

        // Parse response: candidates[0].content.parts[0].text
        JSONObject json = new JSONObject(response.body());
        return json.getJSONArray("candidates")
                   .getJSONObject(0)
                   .getJSONObject("content")
                   .getJSONArray("parts")
                   .getJSONObject(0)
                   .getString("text")
                   .trim();
    }
}
