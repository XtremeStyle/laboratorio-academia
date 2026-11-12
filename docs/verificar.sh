#!/usr/bin/env bash
# Verificación rápida para revisores: compila, ejecuta la batería de pruebas
# e informa el resultado. No modifica el árbol ni requiere infraestructura externa.
set -uo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."

echo "── Java ─────────────────────────────────────────────"
if ! command -v java >/dev/null 2>&1; then
  echo "  No se encontró Java. Instala Java 21 y repite."; exit 1
fi
java -version 2>&1 | head -1 | sed 's/^/  /'

echo
echo "── Compilación y pruebas ────────────────────────────"
if ./mvnw -q test; then
  echo "  Suite JUnit: OK"
else
  echo "  Suite JUnit: FALLA — revisa la salida anterior."; exit 1
fi

echo
echo "── Conteo de pruebas ────────────────────────────────"
python3 docs/contar-tests.py 2>/dev/null | tail -1 | sed 's/^/  /' || true

echo
echo "── Salud de una instancia en ejecución (opcional) ───"
PORT="${PORT:-8080}"
code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 "http://localhost:${PORT}/actuator/health" 2>/dev/null || echo 000)"
if [ "$code" = "200" ]; then
  echo "  http://localhost:${PORT}/actuator/health → 200 (UP)"
else
  echo "  Sin instancia en :${PORT} (arráncala con ./run-laboratorio.sh y repite con PORT=<puerto>)."
fi
