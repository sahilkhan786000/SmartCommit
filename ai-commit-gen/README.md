## AI Commit Message Generator (Java + Git Hooks)

Beginner-friendly, fully working Java project that generates commit messages using your staged `git diff`. It plugs into Git's `commit-msg` hook and supports:

- **DUMMY** provider: simple heuristics (no API, works out of the box)
- **Ollama** (free, local LLM): `ollama run llama3.2`
- **OpenAI** and **OpenRouter** (optional cloud providers)

### How it works

1. The `commit-msg` hook runs after you type `git commit`.
2. It calls the shaded JAR, which:
   - Runs `git diff --cached` to get staged changes
   - Sends the diff to an AI provider (or dummy logic)
   - Inserts the generated message into the commit message file if it was empty

### Project Structure

```
ai-commit-gen/
  ├─ src/main/java/com/example/aicommit/
  │   ├─ CommitMessageGenerator.java   // main entry point
  │   ├─ GitDiffExtractor.java         // runs `git diff --cached`
  │   └─ AIClient.java                 // DUMMY, OLLAMA, OPENAI, OPENROUTER
  ├─ scripts/
  │   └─ commit-msg                    // Git hook script
  ├─ install-hooks.sh                  // helper to install the hook + jar
  ├─ .env.example                      // config template
  └─ pom.xml
```

### Prerequisites

- Java 17+
- Maven 3.8+
- Git

Optional for AI providers:
- For free local AI: install Ollama (`https://ollama.com`) and pull a small model: `ollama pull llama3.2`

### Quick Start (DUMMY provider - no setup)

1. Build the project:
   ```bash
   mvn -q -DskipTests package
   ```
2. Install the Git hook into your current repository:
   ```bash
   ./install-hooks.sh
   ```
3. Make and stage a change, then commit:
   ```bash
   echo "hello" >> hello.txt
   git add hello.txt
   git commit
   ```
   If your commit message is empty, the hook will insert something like `chore: apply staged updates`.

### Free AI: Ollama (local)

1. Install Ollama and start it (it runs at `http://localhost:11434` by default).
2. Pull a small model:
   ```bash
   ollama pull llama3.2
   ```
3. Set provider in your repo root `.env` (create from `.env.example`):
   ```bash
   cp .env.example .env
   echo "PROVIDER=OLLAMA" >> .env
   echo "OLLAMA_MODEL=llama3.2" >> .env
   ```
4. Commit as usual. The hook will query Ollama and insert the generated message.

### OpenAI (optional)

1. In your repo root `.env`:
   ```bash
   echo "PROVIDER=OPENAI" >> .env
   echo "OPENAI_API_KEY=sk-your-key" >> .env
   echo "OPENAI_MODEL=gpt-4o-mini" >> .env
   ```
2. Commit changes. The hook will call OpenAI's Chat Completions API.

### OpenRouter (optional)

1. In your repo root `.env`:
   ```bash
   echo "PROVIDER=OPENROUTER" >> .env
   echo "OPENROUTER_API_KEY=your-key" >> .env
   echo "OPENROUTER_MODEL=openrouter/auto" >> .env
   ```

### Behavior and Safety

- If the commit message already contains non-comment, non-empty text, the hook does nothing.
- If there are no staged changes, it writes a fallback like `chore: update without code changes`.
- If an AI call fails, it writes a safe fallback and does not block your commit.

### Uninstall

Remove the hook and the copied jar:
```bash
rm -f .git/hooks/commit-msg .git/hooks/ai-commit-gen.jar
```

### Troubleshooting

- Ensure the hook is executable: `chmod +x .git/hooks/commit-msg`.
- For Ollama, verify the service: `curl http://localhost:11434/api/tags`.
- For OpenAI/OpenRouter, confirm your API key and that your model is available.

