#!/usr/bin/env bash

set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
HOOKS_DIR="$REPO_ROOT/.git/hooks"

echo "Building project..."
mvn -q -DskipTests package

JAR_SRC="$REPO_ROOT/target/ai-commit-gen-jar-with-dependencies.jar"
JAR_DST="$HOOKS_DIR/ai-commit-gen.jar"

if [ ! -f "$JAR_SRC" ]; then
  echo "Error: built jar not found at $JAR_SRC" >&2
  exit 1
fi

echo "Copying commit-msg hook and jar..."
mkdir -p "$HOOKS_DIR"
cp "$REPO_ROOT/scripts/commit-msg" "$HOOKS_DIR/commit-msg"
chmod +x "$HOOKS_DIR/commit-msg"
cp "$JAR_SRC" "$JAR_DST"

echo "Done. Try making a commit with an empty message to let the tool generate one."

