#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
OUT="${1:-$HOME/storage/shared/Download/VeyraDiagnostics.txt}"
mkdir -p "$(dirname "$OUT")"
su -c 'logcat -d -s VeyraModule:I VeyraHooks:I VeyraHooks:W VeyraUI:I VeyraUI:W VeyraTree:D' > "$OUT"
echo "Saved: $OUT"
