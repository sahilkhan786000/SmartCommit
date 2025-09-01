package com.example.aicommits;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;

/**
 * Entry point for Git commit-msg hook.
 * Git passes the path to the commit message file as the first argument.
 */
public class CommitMessageGenerator {
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                System.err.println("[AI Commit Hook] Missing commit message file argument.");
                System.exit(1);
            }

            File commitMsgFile = new File(args[0]);
            if (!commitMsgFile.exists()) {
                System.err.println("[AI Commit Hook] Commit message file not found: " + commitMsgFile);
                System.exit(1);
            }

            // Get staged diff
            String diff = GitDiffExtractor.getStagedDiff();
            if (diff == null) diff = "";

            // Select AI mode
            String mode = System.getenv().getOrDefault("AI_MODE", "rules").toLowerCase().trim();
            String message;
            if (Objects.equals(mode, "openai")) {
                message = AIClient.generateWithOpenAI(diff);
                if (message == null || message.isBlank()) {
                    System.err.println("[AI Commit Hook] OpenAI failed, falling back to rules.");
                    message = AIClient.generateWithRules(diff);
                }
            } else {
                message = AIClient.generateWithRules(diff);
            }

            // Update commit message file
            String existing = Files.readString(commitMsgFile.toPath());
            String finalMsg;
            if (existing == null || existing.isBlank()) {
                finalMsg = message + System.lineSeparator();
            } else {
                finalMsg = existing + System.lineSeparator() +
                        "\n# AI-SUGGESTED: (you can edit this)\n" +
                        message + System.lineSeparator();
            }
            Files.writeString(commitMsgFile.toPath(), finalMsg, StandardCharsets.UTF_8);

            System.out.println("[AI Commit Hook] Wrote commit message (mode=" + mode + ")");
        } catch (Exception e) {
            System.err.println("[AI Commit Hook] ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
