#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "[Financial Integrity Suite] Running..."
if mvn -q -Dtest='*IT' test; then
  echo "[Financial Integrity Suite] PASS"
else
  echo "[Financial Integrity Suite] FAIL"
  exit 1
fi
