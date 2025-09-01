package com.example.aicommits;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;


/**
* Runs `git diff --cached` and returns the textual diff of staged changes.
*/
public class GitDiffExtractor {
public static String getStagedDiff() throws IOException, InterruptedException {
ProcessBuilder pb = new ProcessBuilder("git", "diff", "--cached", "--unified=0", "--no-color");
// The hook runs with repo root as working directory, but this is safe:
pb.redirectErrorStream(true);
Process p = pb.start();


try (InputStream is = p.getInputStream()) {
String out = new String(is.readAllBytes(), StandardCharsets.UTF_8);
// Wait a bit for process to finish
if (!p.waitFor(10, TimeUnit.SECONDS)) {
p.destroyForcibly();
throw new IOException("git diff --cached timed out");
}
int code = p.exitValue();
if (code != 0) {
throw new IOException("git diff --cached exited with code " + code);
}
return out;
}
}
}