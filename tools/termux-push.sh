#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

if [[ -z "${1:-}" ]]; then
  echo "Usage: bash tools/termux-push.sh https://github.com/USERNAME/Veyra-Xposed.git"
  exit 2
fi

REMOTE="$1"
if ! command -v git >/dev/null 2>&1; then
  echo "Install git first: pkg install git -y"
  exit 1
fi

if [[ ! -d .git ]]; then
  git init -b main
fi

git config --global --get-all safe.directory 2>/dev/null | grep -Fxq "$PWD" || \
  git config --global --add safe.directory "$PWD"

if ! git config user.name >/dev/null 2>&1; then
  read -r -p "Git commit name: " git_name
  git config user.name "$git_name"
fi
if ! git config user.email >/dev/null 2>&1; then
  read -r -p "Git commit email (can be GitHub noreply): " git_email
  git config user.email "$git_email"
fi

git add -A
if ! git diff --cached --quiet; then
  git commit -m "Veyra Settings 1.0.1"
else
  echo "No local changes to commit."
fi

if git remote get-url origin >/dev/null 2>&1; then
  git remote set-url origin "$REMOTE"
else
  git remote add origin "$REMOTE"
fi

git push -u origin main
