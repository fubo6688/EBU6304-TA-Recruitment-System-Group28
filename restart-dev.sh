#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
"$ROOT_DIR/stop-dev.sh" || true
sleep 2
"$ROOT_DIR/start-dev.sh"
