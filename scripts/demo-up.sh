#!/usr/bin/env bash
# demo-up.sh — bring the whole project up on a clean machine, for a demo or viva.
#
#   scripts/demo-up.sh           # app + tests (no Docker needed)
#   scripts/demo-up.sh --full    # also Jenkins, registry, Selenium Grid, Tomcat
#   scripts/demo-up.sh --down    # stop everything this script started
#
# Requires: JDK 17+, Maven 3.9+.  --full additionally requires Docker.
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

GREEN=$'\033[32m'; RED=$'\033[31m'; YEL=$'\033[33m'; DIM=$'\033[2m'; OFF=$'\033[0m'
ok(){ echo "${GREEN}  ok${OFF}  $*"; }
bad(){ echo "${RED} FAIL${OFF} $*"; }
note(){ echo "${DIM}      $*${OFF}"; }
step(){ echo; echo "${YEL}==> $*${OFF}"; }

MODE="${1:-basic}"

if [ "$MODE" = "--down" ]; then
  step "Stopping everything"
  scripts/app-control.sh stop >/dev/null 2>&1 && ok "application stopped"
  if command -v docker >/dev/null 2>&1; then
    docker compose -f jenkins/docker-compose.yml down >/dev/null 2>&1 && ok "jenkins + selenium stopped"
    for c in samp-tomcat samp-registry samp-attendance-run samp-attendance-prod; do
      docker rm -f "$c" >/dev/null 2>&1 && ok "removed $c"
    done
  fi
  exit 0
fi

step "1. Checking prerequisites"
# Some JDK setups print a "Picked up JAVA_TOOL_OPTIONS:" banner on stderr before
# the real output, so the first line is not necessarily the version. Filter it
# out rather than assuming head -1 is the version string.
clean_ver() { grep -v "^Picked up " | grep -v "^OpenJDK.*warning" | head -1; }

for c in java mvn; do
  command -v "$c" >/dev/null 2>&1 || { bad "$c not found"; exit 1; }
done
ok "java   $(java -version 2>&1 | clean_ver | cut -c1-48)"
ok "maven  $(mvn --version 2>&1 | clean_ver | cut -c1-48)"

# Accept both the 1.8-style and the modern "17.0.1" version strings.
JV=$(java -version 2>&1 | clean_ver | sed -E 's/.*version "([0-9]+)(\.([0-9]+))?.*/\1 \3/')
MAJOR=$(echo "$JV" | awk '{print ($1==1)?$2:$1}')
if ! [ "${MAJOR:-0}" -ge 17 ] 2>/dev/null; then
  bad "JDK 17+ required, found: $(java -version 2>&1 | clean_ver)"; exit 1
fi
ok "JDK major version $MAJOR"

step "2. Building and running the unit tests"
BUILD_LOG="$(mktemp)"
if mvn -B clean package >"$BUILD_LOG" 2>&1; then
  # Report the real count rather than a hard-coded one, which goes stale the
  # moment a test is added.
  TESTS=$(grep -oE "Tests run: [0-9]+, Failures: 0, Errors: 0" "$BUILD_LOG" | tail -1 | grep -oE "[0-9]+" | head -1)
  ok "build succeeded, ${TESTS:-all} unit tests passed"
  note "artefact: target/attendance-portal.war"
else
  bad "build failed"
  tail -25 "$BUILD_LOG" | sed 's/^/      /'
  exit 1
fi
rm -f "$BUILD_LOG"

step "3. Starting the application"
# A previous run still holding the port is the most common cause of a failed
# start, and it also locks the H2 database file. Detect it and say so plainly
# rather than leaving the user to read a stack trace.
if command -v lsof >/dev/null 2>&1 && lsof -ti:8080 >/dev/null 2>&1; then
  bad "port 8080 is already in use by PID $(lsof -ti:8080 | tr '\n' ' ')"
  note "an earlier run of this app is probably still going. Free it with:"
  note "    scripts/demo-up.sh --down"
  note "    lsof -ti:8080 | xargs kill -9"
  exit 1
fi
rm -rf data
if scripts/app-control.sh start --port 8080 --timeout 120 | tail -1 | grep -q "UP after"; then
  ok "application healthy on http://localhost:8080"
  note "sign in: faculty1/faculty123 · admin/admin123 · 22cs001/student123"
else
  bad "application did not start. Last lines of .run/app.log:"
  tail -25 .run/app.log 2>/dev/null | sed 's/^/      /'
  exit 1
fi

if [ "$MODE" != "--full" ]; then
  step "Ready"
  echo "  Portal      http://localhost:8080"
  echo "  Health      http://localhost:8080/actuator/health"
  echo
  echo "  Selenium journeys :  mvn -Pselenium verify"
  echo "  Everything else   :  scripts/demo-up.sh --full"
  echo "  Stop              :  scripts/demo-up.sh --down"
  exit 0
fi

command -v docker >/dev/null 2>&1 || { bad "--full needs Docker"; exit 1; }
if ! docker info >/dev/null 2>&1; then
  bad "Docker daemon not reachable"
  note "start it (e.g. 'sudo dockerd &' or Docker Desktop) and re-run"
  exit 1
fi

step "4. Starting Jenkins and the Selenium Grid"
HOST_MAVEN="${HOST_MAVEN:-$(dirname "$(dirname "$(readlink -f "$(command -v mvn)")")")}" \
REPO_PATH="$ROOT" docker compose -f jenkins/docker-compose.yml up -d >/dev/null 2>&1
for i in $(seq 1 60); do
  [ "$(curl -s -o /dev/null -w '%{http_code}' --noproxy localhost --max-time 3 http://localhost:8090/login 2>/dev/null)" = "200" ] \
    && { ok "Jenkins on http://localhost:8090  (admin/admin123)"; break; }
  sleep 3
done
curl -s --noproxy localhost --max-time 3 http://localhost:4444/status 2>/dev/null | grep -q '"ready": *true' \
  && ok "Selenium Grid on http://localhost:4444"

step "5. Starting the registry and Tomcat"
docker run -d --name samp-registry --restart unless-stopped -p 5000:5000 registry:2 >/dev/null 2>&1
sleep 2 && curl -s -o /dev/null --noproxy localhost http://localhost:5000/v2/ && ok "registry on :5000"
mkdir -p deploy/webapps && chmod 777 deploy/webapps
docker run -d --name samp-tomcat -p 8082:8080 -v "$ROOT/deploy/webapps":/usr/local/tomcat/webapps \
  tomcat:10.1-jdk17 >/dev/null 2>&1 && ok "Tomcat on :8082 (pipeline deploys here)"

step "Ready"
cat <<EOT
  Portal (dev)    http://localhost:8080
  Jenkins         http://localhost:8090      admin / admin123
  Selenium Grid   http://localhost:4444
  Registry        http://localhost:5000/v2/_catalog
  Tomcat          http://localhost:8082/attendance   (after a pipeline run)

  Run the pipeline :  scripts/jenkins-build.sh samp-pipeline
  Provision a node :  cd ansible && ansible-playbook site.yml -e app_version=latest
  Stop everything  :  scripts/demo-up.sh --down
EOT
