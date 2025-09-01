package com.example.aicommits;

import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

/**
 * Provides two commit message generation modes:
 * 1. Simple rule-based (default)
 * 2. OpenAI API (optional if AI_MODE=openai and OPENAI_API_KEY is set)
 */
public class AIClient {

    public static String generateWithRules(String diff) {
        if (diff == null || diff.isBlank()) {
            return "chore: empty staged diff (no changes?)";
        }

        String dLower = diff.toLowerCase(Locale.ROOT);
        int filesChanged = countOccurrences(diff, "\ndiff --git ");
        int linesAdded = countPrefixedLines(diff, "+");
        int linesDeleted = countPrefixedLines(diff, "-");

        if (dLower.contains("user")) {
            return "feat(user): update user-related code (" + linesAdded + "+/" + linesDeleted + "- in " + filesChanged + " file(s))";
        }
        if (dLower.contains("readme")) {
            return "docs: update README (" + linesAdded + "+/" + linesDeleted + "-)";
        }
        if (dLower.contains("test")) {
            return "test: update tests (" + linesAdded + "+/" + linesDeleted + "-)";
        }
        return "chore: update code (" + linesAdded + "+/" + linesDeleted + "- across " + filesChanged + " file(s))";
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0, from = 0;
        while (true) {
            int idx = text.indexOf(needle, from);
            if (idx == -1) return count;
            count++;
            from = idx + needle.length();
        }
    }

    private static int countPrefixedLines(String text, String prefix) {
    int count = 0;
    for (String line : text.split("\n")) {
        if (prefix.equals("+") && line.startsWith("+") && !line.startsWith("+++")) {
            count++;
        }
        if (prefix.equals("-") && line.startsWith("-") && !line.startsWith("---")) {
            count++;
        }
    }
    return count;
}


    public static String generateWithOpenAI(String diff) {
        try {
            String apiKey = System.getenv("OPENAI_API_KEY");
            if (apiKey == null || apiKey.isBlank()) return null;
            String model = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4o-mini");

            String prompt = "Generate a SINGLE LINE Conventional Commit message for this diff:\n" + diff;

            JsonObject body = new JsonObject();
            body.addProperty("model", model);
            JsonArray messages = new JsonArray();
            JsonObject sys = new JsonObject();
            sys.addProperty("role", "system");
            sys.addProperty("content", "You generate concise Conventional Commit messages.");
            messages.add(sys);
            JsonObject user = new JsonObject();
            user.addProperty("role", "user");
            user.addProperty("content", prompt);
            messages.add(user);
            body.add("messages", messages);
            body.addProperty("max_tokens", 60);
            body.addProperty("temperature", 0.2);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(body), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return null;

            JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) return null;
            return choices.get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString().trim();
        } catch (Exception e) {
            System.err.println("[AI Commit Hook] OpenAI error: " + e.getMessage());
            return null;
        }
    }
}
