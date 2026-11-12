#!/usr/bin/env bash
set -Eeuo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
./DETENER-LABORATORIO.sh
printf '%s\n' 'Inicialización local opcional: se creará un administrador solo si aún no existe uno.'
read -r -p 'Correo del administrador: ' LAB_ADMIN_EMAIL
read -r -s -p 'Contraseña inicial (12 caracteres mínimo, hasta 72 bytes): ' LAB_ADMIN_PASSWORD
printf '\n'
export LAB_ADMIN_EMAIL LAB_ADMIN_PASSWORD
exec ./run-laboratorio.sh
