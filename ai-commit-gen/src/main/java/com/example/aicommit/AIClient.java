package com.example.aicommit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * AIClient provides multiple providers for generating commit messages from a diff:
 * - DUMMY: simple heuristics for beginners (default)
 * - OLLAMA: free, local LLM via Ollama (http://localhost:11434)
 * - OPENAI / OPENROUTER: cloud providers
 *
 * Configure via environment variables or .env file at repo root.
 */
public class AIClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final String provider;
    private final Dotenv dotenv;
    private final HttpClient httpClient;

    public AIClient(Dotenv dotenv) {
        this.dotenv = dotenv;
        this.provider = getEnv("PROVIDER", "DUMMY");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null && dotenv != null) {
            value = dotenv.get(key);
        }
        return value != null && !value.isBlank() ? value : defaultValue;
    }

    /**
     * Generate a concise commit message for the provided diff.
     */
    public String generateCommitMessage(String diff) {
        String trimmed = diff == null ? "" : diff.trim();
        if (trimmed.isEmpty()) {
            return "chore: update without code changes";
        }

        switch (provider.toUpperCase()) {
            case "OLLAMA":
                return generateWithOllama(trimmed);
            case "OPENAI":
                return generateWithOpenAI(trimmed);
            case "OPENROUTER":
                return generateWithOpenRouter(trimmed);
            default:
                return generateWithDummy(trimmed);
        }
    }

    /**
     * Very simple, beginner-friendly logic.
     */
    private String generateWithDummy(String diff) {
        String lower = diff.toLowerCase();
        if (lower.contains("user")) {
            return "feat: update User-related logic based on staged changes";
        }
        if (lower.contains("fix") || lower.contains("bug")) {
            return "fix: address bug in recent changes";
        }
        if (lower.contains("doc") || lower.contains("readme")) {
            return "docs: update documentation";
        }
        if (lower.contains("test")) {
            return "test: add or update tests";
        }
        return "chore: apply staged updates";
    }

    private String buildPrompt(String diff) {
        return "You are an assistant that writes concise, conventional commit messages.\n" +
                "- Use present tense\n" +
                "- Keep it to one line (<72 chars)\n" +
                "- Include a scope when obvious\n" +
                "Generate only the message without extra commentary.\n\n" +
                "Diff:\n" + diff;
    }

    private String generateWithOllama(String diff) {
        try {
            String baseUrl = getEnv("OLLAMA_BASE_URL", "http://localhost:11434");
            String model = getEnv("OLLAMA_MODEL", "llama3.2");
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("prompt", buildPrompt(diff));
            body.put("stream", false);

            String json = MAPPER.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/generate"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode node = MAPPER.readTree(response.body());
                String text = node.path("response").asText("").trim();
                return sanitize(text);
            } else {
                return fallbackWithStatus("OLLAMA", response.statusCode());
            }
        } catch (Exception e) {
            return fallbackWithError("OLLAMA", e);
        }
    }

    private String generateWithOpenAI(String diff) {
        try {
            String apiKey = getEnv("OPENAI_API_KEY", "");
            if (apiKey.isBlank()) {
                return "chore: configure OPENAI_API_KEY to enable AI generation";
            }
            String baseUrl = getEnv("OPENAI_BASE_URL", "https://api.openai.com/v1");
            String model = getEnv("OPENAI_MODEL", "gpt-4o-mini");

            // Chat Completions request
            String body = MAPPER.writeValueAsString(Map.of(
                    "model", model,
                    "temperature", 0.2,
                    "messages", new Object[]{
                            Map.of("role", "system", "content", "You write concise conventional commit messages."),
                            Map.of("role", "user", "content", buildPrompt(diff))
                    }
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode node = MAPPER.readTree(response.body());
                String text = node.path("choices").path(0).path("message").path("content").asText("").trim();
                return sanitize(text);
            } else {
                return fallbackWithStatus("OPENAI", response.statusCode());
            }
        } catch (Exception e) {
            return fallbackWithError("OPENAI", e);
        }
    }

    private String generateWithOpenRouter(String diff) {
        try {
            String apiKey = getEnv("OPENROUTER_API_KEY", "");
            if (apiKey.isBlank()) {
                return "chore: configure OPENROUTER_API_KEY to enable AI generation";
            }
            String baseUrl = getEnv("OPENROUTER_BASE_URL", "https://openrouter.ai/api/v1");
            String model = getEnv("OPENROUTER_MODEL", "openrouter/auto");

            String body = MAPPER.writeValueAsString(Map.of(
                    "model", model,
                    "temperature", 0.2,
                    "messages", new Object[]{
                            Map.of("role", "system", "content", "You write concise conventional commit messages."),
                            Map.of("role", "user", "content", buildPrompt(diff))
                    }
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("HTTP-Referer", "https://local.git.hooks")
                    .header("X-Title", "AI Commit Generator")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode node = MAPPER.readTree(response.body());
                String text = node.path("choices").path(0).path("message").path("content").asText("").trim();
                return sanitize(text);
            } else {
                return fallbackWithStatus("OPENROUTER", response.statusCode());
            }
        } catch (Exception e) {
            return fallbackWithError("OPENROUTER", e);
        }
    }

    private String sanitize(String text) {
        if (text == null) return "chore: apply staged updates";
        String firstLine = text.split("\r?\n")[0].trim();
        // Remove leading dashes/quotes often produced by LLMs
        firstLine = firstLine.replaceFirst("^[\\\\\"'`\\-\\*\\s]+", "");
        if (firstLine.length() > 80) {
            firstLine = firstLine.substring(0, 80);
        }
        return firstLine.isEmpty() ? "chore: apply staged updates" : firstLine;
    }

    private String fallbackWithStatus(String provider, int statusCode) {
        return String.format("chore: %s API error (status %d), used fallback message", provider, statusCode);
    }

    private String fallbackWithError(String provider, Exception e) {
        return String.format("chore: %s error '%s', used fallback message", provider, e.getClass().getSimpleName());
    }
}

