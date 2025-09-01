package com.example.aicommits;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Runs `git diff --cached` and returns the staged diff text.
 */
public class GitDiffExtractor {
    public static String getStagedDiff() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("git", "diff", "--cached", "--unified=0", "--no-color");
        pb.redirectErrorStream(true);
        Process p = pb.start();

        try (InputStream is = p.getInputStream()) {
            String out = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            if (!p.waitFor(10, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new Exception("git diff --cached timed out");
            }
            if (p.exitValue() != 0) {
                throw new Exception("git diff --cached failed with code " + p.exitValue());
            }
            return out;
        }
    }
}
