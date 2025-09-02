package com.example.aicommit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * GitDiffExtractor runs 'git diff --cached' to obtain the staged changes.
 * This is used by the commit-msg hook to generate a relevant commit message.
 */
public class GitDiffExtractor {

    /**
     * Extracts the staged diff using: git diff --cached --unified=0 --no-color
     *
     * @return The textual diff of staged changes.
     * @throws IOException if the git command fails or cannot be executed.
     * @throws InterruptedException if the process is interrupted.
     */
    public String extractStagedDiff() throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("git", "diff", "--cached", "--unified=0", "--no-color");
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Failed to run git diff --cached. Exit code: " + exitCode);
        }

        return output == null ? "" : output;
    }
}

