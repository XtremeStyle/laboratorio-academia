#!/usr/bin/env bash
# CHANGE // SPACE-LAB OS — arranca la app contra el PostgreSQL local de desarrollo (127.0.0.1:5433).
# REQUIERE: haber ejecutado antes  sudo ~/bin/root-tasks.sh  (sección 4) una única vez.
# La base H2 privada no se migra aquí; ver docs/MIGRACION-POSTGRESQL.md para el ensayo sobre copia.
set -Eeuo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

PORT="${PORT:-8091}"
JDBC_URL="jdbc:postgresql://127.0.0.1:5433/change_space_lab"
PGBIN=""
for d in /usr/lib/postgresql/*/bin; do
  [ -x "$d/pg_isready" ] && PGBIN="$d" && break
done
if [[ -z "$PGBIN" && -x /usr/bin/pg_isready ]]; then
  PGBIN=/usr/bin
fi
if [[ -z "$PGBIN" ]]; then
  echo "PostgreSQL no está instalado en este equipo."
  echo "Ejecuta una vez:  sudo ~/bin/root-tasks.sh  (sección 4)"
  exit 1
fi
if ! "$PGBIN/pg_isready" -h 127.0.0.1 -p 5433 -U spaceLab -q; then
  echo "El clúster de SPACE-LAB no responde en 127.0.0.1:5433."
  echo "Inicia el servidor:  sudo -u jose $PGBIN/pg_ctl -D ~/.local/share/space-lab/postgres/data start"
  exit 1
fi

[[ -x ./mvnw ]] || chmod +x ./mvnw
JAR=target/laboratorio-0.1.0-SNAPSHOT.jar
[[ -f "$JAR" ]] || { echo "No encuentro el ejecutable. Ejecuta primero ./install-space-lab.sh"; exit 1; }

set -a; [[ -f .env.local ]] && . ./.env.local; set +a
echo "CHANGE // SPACE-LAB OS con PostgreSQL (Flyway V10) en http://localhost:$PORT"
mkdir -p data
exec java -jar "$JAR" --spring.profiles.active=dev --server.port="$PORT" \
  --spring.datasource.url="$JDBC_URL" \
  --spring.datasource.username=spaceLab \
  --spring.datasource.password= \
  --spring.datasource.driver-class-name=org.postgresql.Driver \
  --spring.flyway.locations=classpath:db/migration
