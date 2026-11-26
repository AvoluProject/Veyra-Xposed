#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

if [[ -z "${1:-}" ]]; then
  echo "Usage: ./tools/termux-push.sh https://github.com/USERNAME/Veyra-Xposed.git"
  exit 2
fi

REMOTE="$1"
if ! command -v git >/dev/null 2>&1; then
  echo "Install git first: pkg install git unzip -y"
  exit 1
fi

git init -b main
git add .
git commit -m "Veyra Settings 1.0.0"
git remote add origin "$REMOTE" 2>/dev/null || git remote set-url origin "$REMOTE"
git push -u origin main
