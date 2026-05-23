#!/usr/bin/env bash
set -euo pipefail

PORT="${PORT:-8080}"
NO_BROWSER="${NO_BROWSER:-0}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
BACKEND_WEB_INF="$BACKEND_DIR/WEB-INF"
ROOT_WEB_INF="$ROOT_DIR/WEB-INF"
DATA_DIR="$ROOT_DIR/data"

step() {
  printf '\n==> %s\n' "$1"
}

resolve_tomcat_home() {
  if [[ -n "${CATALINA_HOME:-}" && -x "$CATALINA_HOME/bin/startup.sh" ]]; then
    printf '%s\n' "$CATALINA_HOME"
    return 0
  fi

  local candidates=(
    "$HOME/Downloads/apache-tomcat"*
    "$HOME/Desktop/apache-tomcat"*
    "$HOME/iCloud云盘（归档）/Desktop/apache-tomcat"*
    /opt/homebrew/Cellar/tomcat/*/libexec
    /usr/local/Cellar/tomcat/*/libexec
  )

  local candidate
  for candidate in "${candidates[@]}"; do
    if [[ -x "$candidate/bin/startup.sh" ]]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done

  printf 'Tomcat not found. Set CATALINA_HOME to a Tomcat 10.1+ or 11 directory.\n' >&2
  return 1
}

wait_ready() {
  local url="$1"
  local deadline=$((SECONDS + 20))
  while (( SECONDS < deadline )); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  return 1
}

step "Checking Java and Tomcat"
command -v javac >/dev/null || { echo "javac not found. Install JDK 17+ first." >&2; exit 1; }
TOMCAT_HOME="$(resolve_tomcat_home)"
export CATALINA_HOME="$TOMCAT_HOME"
export CATALINA_BASE="$TOMCAT_HOME"
export TA_DATA_DIR="$DATA_DIR"

echo "Project: $ROOT_DIR"
echo "Tomcat : $TOMCAT_HOME"
echo "Data   : $DATA_DIR"

mkdir -p "$DATA_DIR"

step "Compiling backend Java sources"
CLASSES_DIR="$BACKEND_WEB_INF/classes"
rm -rf "$CLASSES_DIR"
mkdir -p "$CLASSES_DIR"

javac --release 17 -encoding UTF-8 \
  -cp "$BACKEND_WEB_INF/lib/*" \
  -d "$CLASSES_DIR" \
  "$BACKEND_DIR"/src/com/ta/model/*.java \
  "$BACKEND_DIR"/src/com/ta/util/*.java \
  "$BACKEND_DIR"/src/com/ta/servlet/*.java

step "Syncing WEB-INF to root"
rm -rf "$ROOT_WEB_INF/classes"
mkdir -p "$ROOT_WEB_INF/classes" "$ROOT_WEB_INF/lib"
cp "$BACKEND_WEB_INF/web.xml" "$ROOT_WEB_INF/web.xml"
cp -R "$CLASSES_DIR"/. "$ROOT_WEB_INF/classes/"

find "$BACKEND_WEB_INF/lib" -maxdepth 1 -name 'json*.jar' -exec cp {} "$ROOT_WEB_INF/lib/" \;
PDFBOX_JAR="$(find "$ROOT_WEB_INF/lib" -maxdepth 1 -name 'pdfbox-app-*.jar' 2>/dev/null | sort -V | tail -n 1)"
if [[ -z "$PDFBOX_JAR" ]]; then
  PDFBOX_JAR="$(find "$BACKEND_WEB_INF/lib" -maxdepth 1 -name 'pdfbox-app-*.jar' 2>/dev/null | sort -V | tail -n 1)"
fi
if [[ -n "$PDFBOX_JAR" ]]; then
  PDFBOX_NAME="$(basename "$PDFBOX_JAR")"
  find "$ROOT_WEB_INF/lib" -maxdepth 1 -name 'pdfbox-app-*.jar' ! -name "$PDFBOX_NAME" -delete
  if [[ "$PDFBOX_JAR" != "$ROOT_WEB_INF/lib/$PDFBOX_NAME" ]]; then
    cp "$PDFBOX_JAR" "$ROOT_WEB_INF/lib/"
  fi
else
  echo "pdfbox-app jar not found under $BACKEND_WEB_INF/lib" >&2
  exit 1
fi

step "Deploying to Tomcat webapps contexts"
for context in ta-system MyRecruitmentSystem; do
  APP_DIR="$TOMCAT_HOME/webapps/$context"
  WORK_DIR="$TOMCAT_HOME/work/Catalina/localhost/$context"
  mkdir -p "$APP_DIR"
  rm -rf "$WORK_DIR"
  rsync -a --delete \
    --exclude '.git' \
    --exclude '.vscode' \
    --exclude 'backend' \
    --exclude 'data' \
    --exclude 'target' \
    --exclude 'backend/test-bin' \
    --exclude '*.ps1' \
    --exclude '*.bat' \
    "$ROOT_DIR"/ "$APP_DIR"/
  echo "Deployed context: /$context"
done

step "Starting Tomcat"
if lsof -iTCP:"$PORT" -sTCP:LISTEN -n -P >/dev/null 2>&1; then
  echo "Port $PORT is already listening; skip Tomcat startup."
else
  "$TOMCAT_HOME/bin/startup.sh" >/dev/null
fi

URL="http://localhost:$PORT/ta-system/login.html"
step "Validating app availability"
if ! wait_ready "$URL"; then
  echo "App is not ready: $URL" >&2
  echo "Check Tomcat logs under: $TOMCAT_HOME/logs" >&2
  exit 1
fi

echo
echo "Done. Open: $URL"

if [[ "$NO_BROWSER" != "1" ]]; then
  open "$URL" >/dev/null 2>&1 || true
fi
