#!/usr/bin/env bash
# Bootstrap Maven local para que el proyecto no dependa de Maven global.
set -Eeuo pipefail

MAVEN_VERSION="3.9.11"
MAVEN_DIR="${HOME}/.m2/wrapper/dists/apache-maven-${MAVEN_VERSION}"
MAVEN_BIN="${MAVEN_DIR}/bin/mvn"
ARCHIVE="apache-maven-${MAVEN_VERSION}-bin.tar.gz"
BASE_URL="https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries"

if [[ ! -x "$MAVEN_BIN" ]]; then
  command -v curl >/dev/null 2>&1 || { echo "Se requiere curl para descargar Maven." >&2; exit 1; }
  command -v tar >/dev/null 2>&1 || { echo "Se requiere tar para extraer Maven." >&2; exit 1; }
  if command -v sha512sum >/dev/null 2>&1; then
    checksum_tool="sha512sum"
  elif command -v shasum >/dev/null 2>&1; then
    checksum_tool="shasum"
  else
    echo "Se requiere sha512sum o shasum para verificar Maven." >&2
    exit 1
  fi

  mkdir -p "$(dirname "$MAVEN_DIR")"
  TEMP_DIR="$(mktemp -d)"
  trap 'rm -rf "$TEMP_DIR"' EXIT

  echo "Descargando Apache Maven ${MAVEN_VERSION}..."
  curl --fail --location --silent --show-error "${BASE_URL}/${ARCHIVE}" -o "${TEMP_DIR}/${ARCHIVE}"
  curl --fail --location --silent --show-error "${BASE_URL}/${ARCHIVE}.sha512" -o "${TEMP_DIR}/${ARCHIVE}.sha512"

  expected="$(awk '{print $1}' "${TEMP_DIR}/${ARCHIVE}.sha512")"
  if [[ "$checksum_tool" == "sha512sum" ]]; then
    actual="$(sha512sum "${TEMP_DIR}/${ARCHIVE}" | awk '{print $1}')"
  else
    actual="$(shasum -a 512 "${TEMP_DIR}/${ARCHIVE}" | awk '{print $1}')"
  fi
  [[ -n "$expected" && "$expected" == "$actual" ]] || { echo "La verificación SHA-512 de Maven falló." >&2; exit 1; }

  mkdir -p "$MAVEN_DIR"
  tar -xzf "${TEMP_DIR}/${ARCHIVE}" --strip-components=1 -C "$MAVEN_DIR"
fi

exec "$MAVEN_BIN" "$@"
