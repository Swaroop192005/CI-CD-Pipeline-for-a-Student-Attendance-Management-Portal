#!/usr/bin/env bash
# run-and-shot.sh — execute a command for real, capture its output to a .log,
# and render that transcript to a .png evidence screenshot.
#
# The PNG is only ever a typeset view of the .log produced by this run, and both
# files are committed together so every screenshot stays verifiable.
#
# Usage: run-and-shot.sh <task-nn> <slug> <command...>
#   e.g. run-and-shot.sh task-07 maven-build mvn -B clean package
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TASK="${1:?task dir required, e.g. task-07}"; shift
SLUG="${1:?slug required}"; shift

EV_DIR="$REPO_ROOT/docs/evidence/$TASK"
mkdir -p "$EV_DIR"
LOG="$EV_DIR/${SLUG}.log"
PNG="$EV_DIR/${SLUG}.png"
CMD="$*"

{
  printf '$ %s\n' "$CMD"
  printf '# executed %s on %s\n\n' "$(date -u '+%Y-%m-%d %H:%M:%S UTC')" "$(uname -sr)"
} > "$LOG"

# Force colour where tools honour it, then record the real combined output.
STAMP="$(date -u '+%Y-%m-%d %H:%M:%S UTC')"
# Widen the pty before running: at the default 80 columns the kernel inserts
# wrap artefacts mid-token that would show up in the rendered screenshot.
script -qefc "stty cols ${SHOT_PTY_COLS:-200} rows 120 2>/dev/null; $CMD" /dev/null 2>&1 | tee -a "$LOG"
rc=${PIPESTATUS[0]}

printf '\n[exit code: %s]\n' "$rc" >> "$LOG"

node "$REPO_ROOT/scripts/capture/shot-term.js" \
  --log "$LOG" --out "$PNG" --title "$CMD" --exit "$rc" --stamp "$STAMP" \
  --max-lines "${SHOT_MAX_LINES:-46}" --cols "${SHOT_COLS:-112}" || true

echo "[run-and-shot] $TASK/$SLUG -> exit $rc | $LOG | $PNG"
exit "$rc"
