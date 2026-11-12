#!/usr/bin/env bash
set -Eeuo pipefail

APP_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PORT="${PORT:-8080}"
APP_URL="http://localhost:${PORT}"

say() { printf '\n\033[1;36m[Laboratorio]\033[0m %s\n' "$*"; }
fail() { printf '\n\033[1;31m[Laboratorio] Error:\033[0m %s\n' "$*" >&2; exit 1; }

install_packages() {
  local missing=("$@")
  ((${#missing[@]})) || return 0
  say "Faltan dependencias del sistema: ${missing[*]}"
  if command -v pacman >/dev/null 2>&1; then
    command -v sudo >/dev/null 2>&1 && sudo pacman -S --needed --noconfirm "${missing[@]}" || pacman -S --needed --noconfirm "${missing[@]}"
  elif command -v apt-get >/dev/null 2>&1; then
    command -v sudo >/dev/null 2>&1 && sudo apt-get update && sudo apt-get install -y "${missing[@]}" || { apt-get update && apt-get install -y "${missing[@]}"; }
  else
    fail "No reconozco tu gestor de paquetes. Instala manualmente Java 21, curl y tar."
  fi
}

# Java 21 es el único requisito de ejecución de la aplicación.
if ! command -v java >/dev/null 2>&1; then
  if command -v pacman >/dev/null 2>&1; then install_packages jdk21-openjdk
  elif command -v apt-get >/dev/null 2>&1; then install_packages openjdk-21-jdk
  else fail "Necesitas Java 21. Instálalo y vuelve a ejecutar este archivo."; fi
fi
JAVA_MAJOR="$(java -version 2>&1 | awk -F '"' '/version/ {split($2,v,"."); if(v[1]=="1") print v[2]; else print v[1]; exit}')"
[[ "${JAVA_MAJOR:-0}" -ge 21 ]] || fail "Se requiere Java 21 o superior. Versión detectada: ${JAVA_MAJOR:-desconocida}."

missing=()
command -v curl >/dev/null 2>&1 || missing+=(curl)
command -v tar >/dev/null 2>&1 || missing+=(tar)
((${#missing[@]} == 0)) || install_packages "${missing[@]}"

[[ -x "$APP_DIR/mvnw" ]] || chmod +x "$APP_DIR/mvnw"
cd "$APP_DIR"
# Recompilar un JAR que ya está cargado puede romper el cierre de otra instancia.
# Si la aplicación ya responde, abrirla sin reconstruir ni modificar su base.
if curl -fsS --max-time 2 "$APP_URL/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
  say "Ya hay una aplicación activa en $APP_URL. Abriendo la instancia existente."
  if command -v xdg-open >/dev/null 2>&1; then xdg-open "$APP_URL" >/dev/null 2>&1 || true; fi
  exit 0
fi
if command -v pgrep >/dev/null 2>&1 && pgrep -f 'java -jar (.*\/)?target/laboratorio-0[.]1[.]0-SNAPSHOT[.]jar([[:space:]]|$)' >/dev/null; then
  fail "Otra instancia usa el ejecutable de compilación. Detén esa instancia antes de reconstruirlo."
fi
say "Preparando Laboratorio. La primera ejecución descarga Maven y dependencias; requiere internet."
./mvnw -q -DskipTests package

JAR="$APP_DIR/target/laboratorio-0.1.0-SNAPSHOT.jar"
[[ -f "$JAR" ]] || fail "No se generó el ejecutable JAR esperado: $JAR"

if command -v xdg-open >/dev/null 2>&1; then
  (sleep 3; xdg-open "$APP_URL" >/dev/null 2>&1 || true) &
fi
say "Aplicación disponible en $APP_URL"
say "Para detenerla, presiona Ctrl+C en esta terminal."
mkdir -p "$APP_DIR/data"
printf '%s\n' "$$" > "$APP_DIR/data/laboratorio.pid"
exec java -jar target/laboratorio-0.1.0-SNAPSHOT.jar --spring.profiles.active=dev --server.port="$PORT"
