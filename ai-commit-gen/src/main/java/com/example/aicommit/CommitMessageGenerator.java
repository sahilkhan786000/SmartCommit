package com.example.aicommit;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Entry point invoked by the Git commit-msg hook. It reads the staged diff, generates
 * a commit message using the configured AI provider (or dummy logic), and writes it
 * to the commit message file if the user hasn't provided one yet.
 */
public class CommitMessageGenerator {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -jar ai-commit-gen.jar <path-to-commit-msg-file>");
            System.exit(0); // Do not block commit
        }

        Path commitMsgPath = Path.of(args[0]);

        try {
            // Load .env from the repository root if present
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();

            // If a meaningful message already exists, do nothing
            if (hasExistingMessage(commitMsgPath)) {
                System.out.println("commit-msg: existing message detected; skipping auto-generation.");
                return;
            }

            // Extract staged diff
            GitDiffExtractor extractor = new GitDiffExtractor();
            String diff = extractor.extractStagedDiff();

            // Generate message
            AIClient aiClient = new AIClient(dotenv);
            String message = aiClient.generateCommitMessage(diff);

            // Write to commit message file (prepend)
            writeMessage(commitMsgPath, message);
            System.out.println("commit-msg: generated message -> " + message);
        } catch (Exception e) {
            System.err.println("commit-msg: failed to generate message: " + e.getMessage());
        }
    }

    /**
     * Determines whether the user already entered a non-comment, non-empty line.
     */
    private static boolean hasExistingMessage(Path path) throws IOException {
        if (!Files.exists(path)) return false;
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.startsWith("#")) continue; // comments inserted by Git template
            return true; // real content exists
        }
        return false;
    }

    /**
     * Inserts the generated message at the top of the commit message file,
     * preserving existing comments (and empty lines) below.
     */
    private static void writeMessage(Path path, String message) throws IOException {
        List<String> existing = Files.exists(path)
                ? Files.readAllLines(path, StandardCharsets.UTF_8)
                : List.of();

        StringBuilder builder = new StringBuilder();
        builder.append(message).append("\n\n");
        for (String line : existing) {
            builder.append(line).append("\n");
        }
        Files.writeString(path, builder.toString(), StandardCharsets.UTF_8);
    }
}

