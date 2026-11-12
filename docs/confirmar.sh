#!/usr/bin/env bash
# Verificación rápida de CHANGE // SPACE-LAB OS (sin intervención manual).
set -uo pipefail
cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/.."
echo "── Servicio PostgreSQL ────────────────────────────────"
systemctl --user is-active change-space-lab-postgres.service && printf '  pg_isready: ' && /usr/bin/pg_isready -h 127.0.0.1 -p 5433 -U spaceLab
echo
echo "── Datos reales en PG ─────────────────────────────────"
/usr/bin/psql -h 127.0.0.1 -p 5433 -U spaceLab -d change_space_lab -Atc \
 "SELECT 'usuarios=' || count(*) FROM usuario;
  SELECT 'cursos=' || count(*) FROM curso;
  SELECT 'lecciones=' || count(*) FROM leccion_curso;
  SELECT 'matriculas=' || count(*) FROM matricula_curso;
  SELECT 'avances=' || count(*) FROM progreso_leccion;
  SELECT 'auditoria=' || count(*) FROM auditoria;
  SELECT 'migraciones=' || count(*) FROM flyway_schema_history;" | sed 's/^/  /'
echo
echo "── Salud de las instancias ───────────────────────────"
for p in 8080 8090; do
  printf "  :%s  " "$p"; curl -s --max-time 3 "http://localhost:$p/actuator/health" || echo "CAIDA"
done
echo
echo "── Rutas públicas ────────────────────────────────────"
for path in / /login /registro /demostracion/semilleros; do
  printf "  %-30s %s\n" "$path" "$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 http://localhost:8090$path)"
done
echo
echo "── Acceso restringido (redirige a login) ─────────────"
for path in /cursos /laboratorio/circuitos /laboratorio/astronomia /mis-misiones /mi-progreso /chat /academia/misiones /administracion/usuarios; do
  printf "  %-30s %s\n" "$path" "$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 http://localhost:8090$path)"
done
echo
echo "── Suite de pruebas ──────────────────────────────────"
./mvnw -q test 2>&1 | grep -E "Tests run|ERROR" | tail -3; python3 docs/contar-tests.py
