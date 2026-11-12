#!/usr/bin/env bash
# Desinstala el lanzador y la entrada de escritorio de CHANGE // SPACE-LAB OS.
# NUNCA borra la carpeta del proyecto ni tus datos (data/).
set -Eeuo pipefail
LOCAL_BIN="$HOME/.local/bin"
DESKTOP_DIR="$HOME/.local/share/applications"
APP_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

printf '\033[1;36m[SPACE-LAB]\033[0m Desinstalando lanzador y entrada de escritorio…\n'
rm -f "$LOCAL_BIN/change-space-lab" "$DESKTOP_DIR/change-space-lab.desktop"
command -v update-desktop-database >/dev/null 2>&1 && update-desktop-database "$DESKTOP_DIR" 2>/dev/null || true
printf 'Listo. La carpeta %s (y su base H2 privada en data/) se conserva.\n' "$APP_DIR"
printf 'Para eliminarlos tú mismo, revisa la carpeta y bórrala a tu discreción (por ejemplo con el gestor de archivos).\n'
