#!/usr/bin/env bash
# app-control.sh — start / stop / wait for the portal during development and
# evidence capture.
#
# Uses an explicit PID file rather than `pkill -f`, because a pattern like
# "spring-boot:run" also matches the shell that invokes it, which kills the
# caller instead of the application.
#
# Usage:
#   scripts/app-control.sh start [--war|--mvn] [--port 8080] [--profile dev]
#   scripts/app-control.sh wait  [--port 8080] [--timeout 90]
#   scripts/app-control.sh stop
#   scripts/app-control.sh status
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="$ROOT/.run"; mkdir -p "$RUN_DIR"
PID_FILE="$RUN_DIR/app.pid"
LOG_FILE="$RUN_DIR/app.log"

CMD="${1:-status}"; shift || true
MODE="mvn"; PORT="${SERVER_PORT:-8080}"; PROFILE=""; TIMEOUT=90
while [ $# -gt 0 ]; do
  case "$1" in
    --war) MODE="war";; --mvn) MODE="mvn";;
    --port) PORT="$2"; shift;; --profile) PROFILE="$2"; shift;;
    --timeout) TIMEOUT="$2"; shift;;
  esac; shift
done

health_url="http://localhost:${PORT}/actuator/health"

wait_up() {
  local t="$1"
  for i in $(seq 1 "$t"); do
    if [ "$(curl -s -o /dev/null -w '%{http_code}' --noproxy localhost --max-time 3 "$health_url" 2>/dev/null)" = "200" ]; then
      echo "[app-control] UP after ${i}s -> $health_url"; return 0
    fi
    if [ -f "$PID_FILE" ] && ! kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
      echo "[app-control] process died during startup; last log lines:"; tail -25 "$LOG_FILE"; return 1
    fi
    sleep 1
  done
  echo "[app-control] TIMEOUT after ${t}s; last log lines:"; tail -25 "$LOG_FILE"; return 1
}

case "$CMD" in
  start)
    if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
      echo "[app-control] already running (pid $(cat "$PID_FILE"))"; exit 0
    fi
    cd "$ROOT"
    # setsid, not just nohup. nohup only ignores SIGHUP for the process it starts;
    # Maven forks a child JVM that is still in the caller's session, so closing the
    # terminal takes the application down with it. setsid detaches it into its own
    # session so it genuinely keeps running.
    detach() { if command -v setsid >/dev/null 2>&1; then setsid "$@"; else nohup "$@"; fi; }

    if [ "$MODE" = "war" ]; then
      [ -f target/attendance-portal.war ] || { echo "[app-control] target/attendance-portal.war missing; run mvn package"; exit 1; }
      SERVER_PORT="$PORT" detach java -jar target/attendance-portal.war > "$LOG_FILE" 2>&1 &
    else
      # Run the packaged WAR rather than spring-boot:run when one exists: it starts
      # faster and, more importantly, is a single process rather than Maven plus a
      # forked JVM, so stopping and detaching it are both reliable.
      if [ -f target/attendance-portal.war ]; then
        SERVER_PORT="$PORT" detach java -jar target/attendance-portal.war > "$LOG_FILE" 2>&1 &
      else
        args=(-B spring-boot:run "-Dspring-boot.run.jvmArguments=-DSERVER_PORT=$PORT")
        [ -n "$PROFILE" ] && args+=("-Dspring-boot.run.profiles=$PROFILE")
        SERVER_PORT="$PORT" detach mvn "${args[@]}" > "$LOG_FILE" 2>&1 &
      fi
    fi
    echo $! > "$PID_FILE"
    echo "[app-control] started pid $(cat "$PID_FILE") mode=$MODE port=$PORT log=$LOG_FILE"
    wait_up "$TIMEOUT"
    ;;
  wait)   wait_up "$TIMEOUT" ;;
  status)
    if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
      echo "[app-control] running (pid $(cat "$PID_FILE"))"
    else echo "[app-control] not running"; fi
    curl -s --noproxy localhost --max-time 3 "$health_url" 2>/dev/null || true; echo
    ;;
  stop)
    if [ -f "$PID_FILE" ]; then
      pid="$(cat "$PID_FILE")"
      # Maven forks a child JVM; take the whole process group down.
      pkill -TERM -P "$pid" 2>/dev/null
      kill -TERM "$pid" 2>/dev/null
      for _ in $(seq 1 15); do kill -0 "$pid" 2>/dev/null || break; sleep 1; done
      pkill -KILL -P "$pid" 2>/dev/null; kill -KILL "$pid" 2>/dev/null
      rm -f "$PID_FILE"; echo "[app-control] stopped"
    else echo "[app-control] no pid file"; fi
    # Any JVM still holding the port would break the next start.
    if command -v fuser >/dev/null 2>&1; then fuser -k "${PORT}/tcp" 2>/dev/null; fi
    ;;
  *) echo "usage: app-control.sh {start|stop|wait|status} [--war|--mvn] [--port N] [--profile P] [--timeout S]"; exit 2;;
esac
