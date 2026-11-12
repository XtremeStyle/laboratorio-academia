#!/usr/bin/env bash
# CHANGE // SPACE-LAB OS — instalador para Linux (Arch/Omarchy y Debian/Ubuntu).
# 1) instala dependencias del sistema (Java 21, curl, tar), 2) compila, 3) crea
#    lanzador de escritorio + comando `change-space-lab`, 4) prepara el arranque
#    con administrador local si no indicaste uno.
set -Eeuo pipefail

APP_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PORT_PREFERIDO="${PORT:-8090}"
LOCAL_BIN="$HOME/.local/bin"
DESKTOP_DIR="$HOME/.local/share/applications"

say() { printf '\n\033[1;36m[SPACE-LAB]\033[0m %s\n' "$*"; }
fail() { printf '\n\033[1;31m[SPACE-LAB] Error:\033[0m %s\n' "$*" >&2; exit 1; }

say "Iniciando instalación de CHANGE // SPACE-LAB OS en: $APP_DIR"

# --- 1) Dependencias del sistema ---
if ! command -v java >/dev/null 2>&1 || ! java -version 2>&1 | grep -qE '"(21|22|23|24|25)\.'; then
  say "Instalando Java 21…"
  if command -v pacman >/dev/null 2>&1; then
    if command -v sudo >/dev/null 2>&1; then sudo pacman -S --needed --noconfirm jdk21-openjdk curl tar || fail "pacman no pudo instalar Java 21"
    else pacman -S --needed --noconfirm jdk21-openjdk curl tar || fail "pacman no pudo instalar Java 21"; fi
  elif command -v apt-get >/dev/null 2>&1; then
    if command -v sudo >/dev/null 2>&1; then sudo apt-get update && sudo apt-get install -y openjdk-21-jdk-headless curl tar || fail "apt-get no pudo instalar Java 21"
    else apt-get update && apt-get install -y openjdk-21-jdk-headless curl tar || fail "apt-get no pudo instalar Java 21"; fi
  else fail "No reconozco el gestor de paquetes (pacman o apt-get). Instala manualmente Java 21, curl y tar."; fi
else
  say "Java ya instalado: $(java -version 2>&1 | head -1)"
fi
command -v curl >/dev/null 2>&1 || fail "Falta curl."
command -v tar  >/dev/null 2>&1 || fail "Falta tar."

# --- 2) Compilar (incluye pruebas) ---
say "Compilando y ejecutando la suite de pruebas (puede tardar unos minutos)…"
cd "$APP_DIR"
./mvnw package

JAR="$APP_DIR/target/laboratorio-0.1.0-SNAPSHOT.jar"
[[ -f "$JAR" ]] || fail "No se generó el ejecutable esperado: $JAR"

# --- 3) Credenciales de administrador local (primer arranque) ---
ENV_FILE="$APP_DIR/.env.local"
if [[ -z "${LAB_ADMIN_EMAIL:-}" || -z "${LAB_ADMIN_PASSWORD:-}" ]]; then
  say "Sin credenciales de admin en el entorno; genero una cuenta de un solo uso."
  GENERATED_PASS="$(LC_ALL=C tr -dc 'A-HJ-NP-Za-km-np-z2-9' < /dev/urandom | head -c 16; echo)"
  GENERATED_EMAIL="admin@espacio-lab.local"
  printf 'LAB_ADMIN_EMAIL=%s\nLAB_ADMIN_PASSWORD=%s\n' "$GENERATED_EMAIL" "$GENERATED_PASS" > "$ENV_FILE"
  chmod 600 "$ENV_FILE"
  BOOT_INFO=1
else
  printf 'LAB_ADMIN_EMAIL=%s\nLAB_ADMIN_PASSWORD=%s\n' "$LAB_ADMIN_EMAIL" "$LAB_ADMIN_PASSWORD" > "$ENV_FILE"
  chmod 600 "$ENV_FILE"
  BOOT_INFO=0
fi

# --- 4) Lanzador CLI ---
mkdir -p "$LOCAL_BIN"
cat > "$LOCAL_BIN/change-space-lab" <<LAUNCHER
#!/usr/bin/env bash
set -Eeuo pipefail
cd "$APP_DIR"
PORT="\${PORT:-$PORT_PREFERIDO}"
mkdir -p "$APP_DIR/data"
if [[ -f "$APP_DIR/.env.local" ]]; then set -a; . "$APP_DIR/.env.local"; set +a; fi
if curl -fsS --max-time 2 "http://localhost:\$PORT/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
  echo "CHANGE // SPACE-LAB OS ya responde en http://localhost:\$PORT"
else
  mkdir -p "$APP_DIR/data"
  (setsid nohup java -jar "$APP_DIR/target/laboratorio-0.1.0-SNAPSHOT.jar" --spring.profiles.active=dev --server.port="\$PORT" >> "$APP_DIR/data/lanzador.log" 2>&1 < /dev/null &)
  echo "Iniciando CHANGE // SPACE-LAB OS en http://localhost:\$PORT …"
  for i in \$(seq 1 45); do
    code=\$(curl -s -o /dev/null -w '%{http_code}' --max-time 2 "http://localhost:\$PORT/actuator/health" 2>/dev/null || true)
    if [[ "\$code" == "200" ]]; then echo "Listo: http://localhost:\$PORT"; break; fi
    sleep 2
  done
fi
if command -v xdg-open >/dev/null 2>&1; then xdg-open "http://localhost:\$PORT" >/dev/null 2>&1 || true; fi
LAUNCHER
chmod 755 "$LOCAL_BIN/change-space-lab"
say "Lanzador CLI creado: $LOCAL_BIN/change-space-lab"

# --- 5) Entrada de escritorio ---
mkdir -p "$DESKTOP_DIR"
cat > "$DESKTOP_DIR/change-space-lab.desktop" <<DESKTOP
[Desktop Entry]
Type=Application
Name=CHANGE Space-Lab
GenericName=Plataforma académica espacial
Comment=CHANGE // SPACE-LAB OS — Robótica y Astronomía en ocho etapas
Exec=$LOCAL_BIN/change-space-lab
Icon=$APP_DIR/src/main/resources/static/assets/brand/change-orbit.svg
Terminal=false
Categories=Education;Science;Development;
Keywords=change;espacial;robótica;astronomía;aprendizaje;
DESKTOP
command -v update-desktop-database >/dev/null 2>&1 && update-desktop-database "$DESKTOP_DIR" 2>/dev/null || true
say "Entrada de escritorio creada: $DESKTOP_DIR/change-space-lab.desktop"

say "Instalación completada."
printf 'Puerto por defecto: %s\n\n' "$PORT_PREFERIDO"
if [[ "$BOOT_INFO" == "1" ]]; then
  printf 'PRIMER ARRANQUE — cuenta de administrador local (de un solo uso):\n  correo:      admin@espacio-lab.local\n  contraseña:  %s\nCrea otro administrador y cambia/borra la contraseña temporal tras tu primer uso (Mi perfil > Cambiar contraseña).\n' "$GENERATED_PASS"
fi
printf '\nPara iniciar:      change-space-lab\nPara detener:      ./DETENER-LABORATORIO.sh\nPara desinstalar:  ./uninstall-space-lab.sh\n'
