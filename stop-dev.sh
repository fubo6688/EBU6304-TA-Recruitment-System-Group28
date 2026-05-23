#!/usr/bin/env bash
set -euo pipefail

resolve_tomcat_home() {
  if [[ -n "${CATALINA_HOME:-}" && -x "$CATALINA_HOME/bin/shutdown.sh" ]]; then
    printf '%s\n' "$CATALINA_HOME"
    return 0
  fi

  local candidate
  for candidate in "$HOME/Downloads/apache-tomcat"* "$HOME/Desktop/apache-tomcat"* "$HOME/iCloud云盘（归档）/Desktop/apache-tomcat"* /opt/homebrew/Cellar/tomcat/*/libexec /usr/local/Cellar/tomcat/*/libexec; do
    if [[ -x "$candidate/bin/shutdown.sh" ]]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done

  printf 'Tomcat not found. Set CATALINA_HOME first.\n' >&2
  return 1
}

TOMCAT_HOME="$(resolve_tomcat_home)"
"$TOMCAT_HOME/bin/shutdown.sh" >/dev/null || true
echo "Tomcat shutdown requested: $TOMCAT_HOME"
