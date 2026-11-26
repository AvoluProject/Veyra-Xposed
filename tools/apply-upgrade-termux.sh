#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

SOURCE_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET_ROOT="${1:-/storage/emulated/0/Download/Veyra-Xposed-1.0.0-Premium}"

if [[ ! -d "$TARGET_ROOT/.git" ]]; then
  echo "Target is not an existing Git repository: $TARGET_ROOT"
  echo "Pass the folder containing your current Veyra .git directory."
  exit 2
fi

if [[ "$SOURCE_ROOT" == "$TARGET_ROOT" ]]; then
  echo "The update source and Git repository are the same folder."
  exit 2
fi

pkg install git -y >/dev/null 2>&1 || true
if ! git config --global --get-all safe.directory 2>/dev/null | grep -Fxq "$TARGET_ROOT"; then
  git config --global --add safe.directory "$TARGET_ROOT" || true
fi

# Avoid committing under a placeholder identity. Ask locally only when needed.
if ! git -C "$TARGET_ROOT" config user.name >/dev/null 2>&1; then
  read -r -p "Git commit name: " git_name
  git -C "$TARGET_ROOT" config user.name "$git_name"
fi
if ! git -C "$TARGET_ROOT" config user.email >/dev/null 2>&1; then
  read -r -p "Git commit email (can be GitHub noreply): " git_email
  git -C "$TARGET_ROOT" config user.email "$git_email"
fi

# Copy the clean source tree over the existing clone; the destination .git is preserved.
cp -a "$SOURCE_ROOT/." "$TARGET_ROOT/"

git -C "$TARGET_ROOT" add -A
if git -C "$TARGET_ROOT" diff --cached --quiet; then
  echo "No tracked changes were found; nothing to commit."
else
  git -C "$TARGET_ROOT" commit -m "Veyra 1.0.1: stabilize Android build and view restoration"
fi

git -C "$TARGET_ROOT" push origin main
