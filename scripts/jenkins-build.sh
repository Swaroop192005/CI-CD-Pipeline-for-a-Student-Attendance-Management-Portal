#!/usr/bin/env bash
# jenkins-build.sh — trigger a Jenkins job and wait for the result.
# Jenkins ties its CSRF crumb to the session, so the cookie jar is required:
# sending the crumb without it returns HTTP 403.
set -uo pipefail
J="${JENKINS_URL:-http://localhost:8090}"
AUTH="${JENKINS_AUTH:-admin:admin123}"
JOB="${1:?job name required}"
WAIT="${2:-100}"
CJ="$(mktemp)"

CRUMB=$(curl -s --noproxy localhost -u "$AUTH" -c "$CJ" "$J/crumbIssuer/api/json" \
  | python3 -c "import json,sys;d=json.load(sys.stdin);print(d['crumbRequestField']+':'+d['crumb'])")
code=$(curl -s --noproxy localhost -u "$AUTH" -b "$CJ" -c "$CJ" -H "$CRUMB" \
  -X POST "$J/job/$JOB/build" -o /dev/null -w "%{http_code}")
echo "triggered $JOB -> HTTP $code"

prev=""
for i in $(seq 1 "$WAIT"); do
  R=$(curl -s --noproxy localhost -u "$AUTH" "$J/job/$JOB/lastBuild/api/json" 2>/dev/null \
    | python3 -c "
import json,sys
try:
  d=json.load(sys.stdin); print('%s|%s|%s' % (d.get('number'), d.get('building'), d.get('result')))
except Exception: print('||')" 2>/dev/null)
  num="${R%%|*}"; rest="${R#*|}"; building="${rest%%|*}"; result="${rest##*|}"
  [ "$R" != "$prev" ] && { echo "  build #${num:-?} building=${building:-?} result=${result:-running}"; prev="$R"; }
  case "$result" in SUCCESS|FAILURE|UNSTABLE|ABORTED) echo "FINAL: $result"; rm -f "$CJ"; [ "$result" = "SUCCESS" ] && exit 0 || exit 1;; esac
  sleep 5
done
echo "TIMED OUT after $((WAIT*5))s"; rm -f "$CJ"; exit 2
