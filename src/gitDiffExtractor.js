// src/gitDiffExtractor.js
import { execSync } from "child_process";

/**
 * Extracts staged git diff (truncated for safety).
 */
export function extractGitDiff() {
  try {
    const diff = execSync("git diff --cached", { encoding: "utf-8" });

    // Limit diff length (e.g., first 4000 chars) to avoid model token limits
    const MAX_CHARS = 4000;
    if (diff.length > MAX_CHARS) {
      return diff.slice(0, MAX_CHARS) + "\n...\n[Diff truncated]";
    }

    return diff;
  } catch (err) {
    console.error("❌ Error extracting git diff:", err.message);
    return "";
  }
}
