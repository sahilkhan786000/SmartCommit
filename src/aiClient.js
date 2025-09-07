// src/aiClient.js
import { InferenceClient } from "@huggingface/inference";
import dotenv from "dotenv";

dotenv.config();

const HF_TOKEN = process.env.HF_API_KEY;

// Hugging Face client
const client = new InferenceClient(HF_TOKEN);

/**
 * Generate commit message (streaming)
 */
// src/aiClient.js
export async function generateCommitMessage(diff) {
  let commitMessage = "";

  try {
    for await (const chunk of client.chatCompletionStream({
      model: "meta-llama/Llama-3.1-8B-Instruct",
      messages: [
        {
          role: "system",
          content:
            "You are an AI that generates ONLY a concise Git commit message in Conventional Commits format. " +
            "Do NOT explain or add extra text. Output only the commit message line.",
        },
        {
          role: "user",
          content: `Git diff:\n${diff}\n\nGenerate a single-line commit message:`,
        },
      ],
      max_tokens: 64,
      temperature: 0.5,
    })) {
      const delta = chunk?.choices?.[0]?.delta?.content;
      if (delta) {
        process.stdout.write(delta); // live stream
        commitMessage += delta;
      }
    }

    console.log("\n✅ Commit message generated!");
    return commitMessage.trim() || "chore: update project files";
  } catch (err) {
    console.error("\n❌ Error generating commit message:", err.message);
    return "chore: update project files";
  }
}
