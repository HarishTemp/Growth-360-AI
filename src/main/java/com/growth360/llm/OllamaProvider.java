package com.growth360.llm;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Calls your LOCAL Ollama instance running on http://localhost:11434
 *
 * No API key needed. No internet. 100% free and private.
 * Detects locally installed models (e.g. gemma4:latest, moondream:latest).
 */
public class OllamaProvider {

    private static final String OLLAMA_GENERATE_URL = "http://localhost:11434/api/generate";
    private static final String OLLAMA_TAGS_URL     = "http://localhost:11434/api/tags";
    private static final Duration TIMEOUT           = Duration.ofSeconds(90);

    private final HttpClient httpClient;

    public OllamaProvider() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
    }

    /**
     * Checks if Ollama is currently running on this machine.
     */
    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_TAGS_URL))
                    .GET()
                    .timeout(Duration.ofSeconds(3))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Queries Ollama for all models downloaded on this machine.
     * E.g. ["gemma4:latest", "moondream:latest"]
     */
    public List<String> getInstalledModels() {
        List<String> models = new ArrayList<>();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_TAGS_URL))
                    .GET()
                    .timeout(Duration.ofSeconds(3))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                JSONArray arr = json.optJSONArray("models");
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        String name = arr.getJSONObject(i).optString("name", "");
                        if (!name.isBlank()) models.add(name);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return models;
    }

    /**
     * Determines the best local model to use:
     * Prefers gemma4:latest, then gemma, then first available model.
     */
    public String resolveBestModel(String requestedModel) {
        if (requestedModel != null && !requestedModel.isBlank() && !requestedModel.equalsIgnoreCase("default")) {
            return requestedModel;
        }
        List<String> installed = getInstalledModels();
        for (String m : installed) {
            if (m.toLowerCase().contains("gemma4")) return m;
        }
        for (String m : installed) {
            if (m.toLowerCase().contains("gemma")) return m;
        }
        return installed.isEmpty() ? "gemma4:latest" : installed.get(0);
    }

    /**
     * Sends a prompt to the local Ollama model and returns the response text.
     */
    public String generate(String modelName, String prompt) throws Exception {
        String effectiveModel = resolveBestModel(modelName);

        JSONObject requestJson = new JSONObject();
        requestJson.put("model", effectiveModel);
        requestJson.put("prompt", prompt);
        requestJson.put("stream", false);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_GENERATE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson.toString()))
                .timeout(TIMEOUT)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama returned HTTP " + response.statusCode() + ": " + response.body());
        }

        JSONObject json = new JSONObject(response.body());
        return json.getString("response").trim();
    }
}
