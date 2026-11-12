#!/usr/bin/env bash
set -Eeuo pipefail
raiz="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
archivo="$raiz/data/laboratorio.pid"
[[ -f "$archivo" ]] || { echo 'No hay una instancia registrada en esta carpeta.'; exit 0; }
read -r proceso < "$archivo"
[[ "$proceso" =~ ^[0-9]+$ ]] || { echo 'El identificador guardado no es válido.' >&2; exit 1; }
if ! kill -0 "$proceso" 2>/dev/null; then echo 'La instancia registrada ya está detenida.'; exit 0; fi
if [[ "$(readlink -f "/proc/$proceso/cwd")" != "$raiz" ]] || ! tr '\0' '\n' < "/proc/$proceso/cmdline" | grep -Fxq 'target/laboratorio-0.1.0-SNAPSHOT.jar'; then
  echo 'La instancia registrada no coincide con este proyecto; no se detuvo ningún proceso.' >&2; exit 1
fi
kill "$proceso"
for ((intento=0; intento<100; intento++)); do
  if ! kill -0 "$proceso" 2>/dev/null; then echo 'Laboratorio detenido.'; exit 0; fi
  sleep 0.1
done
echo 'La aplicación todavía se está cerrando; vuelve a intentarlo.' >&2
exit 1
