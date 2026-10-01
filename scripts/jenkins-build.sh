#!/usr/bin/env bash
# jenkins-build.sh — trigger a Jenkins job and wait for THAT build's result.
#
# Two things this gets right that a naive version does not:
#  * Jenkins ties its CSRF crumb to the session, so the cookie jar is required;
#    sending the crumb without it returns HTTP 403.
#  * The last build number is recorded before triggering, and the script waits
#    for a number greater than it. Polling lastBuild alone reports the PREVIOUS
#    build's result in the window before the new one is queued.
set -uo pipefail
J="${JENKINS_URL:-http://localhost:8090}"
AUTH="${JENKINS_AUTH:-admin:admin123}"
JOB="${1:?job name required}"
WAIT="${2:-120}"
CJ="$(mktemp)"

api() { curl -s --noproxy localhost -u "$AUTH" "$@"; }

last_num() {
  api "$J/job/$JOB/lastBuild/api/json" 2>/dev/null \
    | python3 -c "
import json,sys
try: print(json.load(sys.stdin).get('number') or 0)
except Exception: print(0)" 2>/dev/null
}

BEFORE="$(last_num)"; BEFORE="${BEFORE:-0}"
echo "last build before trigger: #${BEFORE}"

CRUMB=$(api -c "$CJ" "$J/crumbIssuer/api/json" \
  | python3 -c "import json,sys;d=json.load(sys.stdin);print(d['crumbRequestField']+':'+d['crumb'])")
# A pipeline that declares parameters must be triggered via buildWithParameters;
# POSTing to /build returns HTTP 400 once the job has a parameter definition.
# The fallback keeps this working for the unparameterised freestyle job.
code=$(api -b "$CJ" -c "$CJ" -H "$CRUMB" -X POST "$J/job/$JOB/buildWithParameters" -o /dev/null -w "%{http_code}")
case "$code" in 404|405|400)
  code=$(api -b "$CJ" -c "$CJ" -H "$CRUMB" -X POST "$J/job/$JOB/build" -o /dev/null -w "%{http_code}") ;;
esac
echo "triggered $JOB -> HTTP $code"

prev=""
for i in $(seq 1 "$WAIT"); do
  R=$(api "$J/job/$JOB/lastBuild/api/json" 2>/dev/null | python3 -c "
import json,sys
try:
  d=json.load(sys.stdin); print('%s|%s|%s' % (d.get('number') or 0, d.get('building'), d.get('result')))
except Exception: print('0||')" 2>/dev/null)
  num="${R%%|*}"; rest="${R#*|}"; building="${rest%%|*}"; result="${rest##*|}"

  if [ "${num:-0}" -le "${BEFORE}" ]; then sleep 5; continue; fi   # still the old build
  [ "$R" != "$prev" ] && { echo "  build #${num} building=${building} result=${result:-running}"; prev="$R"; }

  case "$result" in
    SUCCESS|FAILURE|UNSTABLE|ABORTED)
      echo "FINAL: build #${num} -> $result"; rm -f "$CJ"
      [ "$result" = "SUCCESS" ] && exit 0 || exit 1;;
  esac
  sleep 5
done
echo "TIMED OUT after $((WAIT*5))s"; rm -f "$CJ"; exit 2
