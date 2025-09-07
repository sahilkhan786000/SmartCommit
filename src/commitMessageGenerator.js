// src/commitMessageGenerator.js
import { generateCommitMessage } from "./aiClient.js";
import { extractGitDiff } from "./gitDiffExtractor.js";

async function main() {
  try {
    const diff = await extractGitDiff();
    const message = await generateCommitMessage(diff);

    console.log(message); // Hook captures this
  } catch (err) {
    console.error("❌ Error:", err.message);
    process.exit(1);
  }
}

main();
