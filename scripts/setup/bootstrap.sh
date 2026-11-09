#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_DIR="${HOME}/Projects/ACADEMY/Laboratorio"
REPO_NAME="laboratorio-academia"

say() { printf "\n[Laboratorio] %s\n" "$*"; }
fail() { say "ERROR: $*"; exit 1; }
has() { command -v "$1" >/dev/null 2>&1; }

say "Preparación inicial para Laboratorio"
say "Carpeta definitiva: ${PROJECT_DIR}"
read -r -p "¿Continuar? [s/N]: " answer
[[ "${answer,,}" == "s" || "${answer,,}" == "si" ]] || exit 0

OS="$(uname -s)"
if [[ "$OS" == "Linux" ]]; then
  if has pacman; then
    missing=()
    has git || missing+=(git)
    has java || missing+=(jdk21-openjdk)
    has gh || missing+=(github-cli)
    if ((${#missing[@]})); then
      say "Se instalarán estos paquetes de Arch Linux: ${missing[*]}"
      read -r -p "¿Autorizar instalación con sudo/pacman? [s/N]: " install_answer
      if [[ "${install_answer,,}" == "s" || "${install_answer,,}" == "si" ]]; then
        sudo pacman -S --needed "${missing[@]}"
      else
        fail "Instala manualmente: ${missing[*]} y vuelve a ejecutar."
      fi
    fi
  elif has apt-get; then
    missing=()
    has git || missing+=(git)
    has java || missing+=(openjdk-21-jdk)
    has gh || missing+=(gh)
    if ((${#missing[@]})); then
      say "Instala los paquetes faltantes con el gestor de tu distribución: ${missing[*]}"
      say "Este instalador no agrega repositorios externos automáticamente."
      exit 1
    fi
  else
    fail "Distribución Linux no reconocida. Instala Git, Java 21 y GitHub CLI manualmente."
  fi
elif [[ "$OS" == "Darwin" ]]; then
  has brew || fail "No se detectó Homebrew. Instálalo desde https://brew.sh y vuelve a ejecutar."
  missing=()
  has git || missing+=(git)
  has java || missing+=(openjdk@21)
  has gh || missing+=(gh)
  if ((${#missing[@]})); then
    say "Se instalarán con Homebrew: ${missing[*]}"
    read -r -p "¿Continuar? [s/N]: " install_answer
    [[ "${install_answer,,}" == "s" || "${install_answer,,}" == "si" ]] || exit 0
    brew install "${missing[@]}"
  fi
else
  fail "Sistema no compatible con este script. En Windows usa bootstrap.ps1."
fi

has git || fail "Git no está disponible."
has java || fail "Java no está disponible."
has gh || fail "GitHub CLI no está disponible."

JAVA_VERSION="$(java -version 2>&1 | head -n 1)"
say "Java detectado: ${JAVA_VERSION}"
java -version 2>&1 | grep -Eq '"21([.+-]|")' || fail "Se requiere Java 21. Detectado: ${JAVA_VERSION}"

mkdir -p "$(dirname "$PROJECT_DIR")"
if [[ -e "$PROJECT_DIR" ]]; then
  [[ -d "$PROJECT_DIR" ]] || fail "La ruta existe y no es una carpeta: $PROJECT_DIR"
  if [[ -n "$(find "$PROJECT_DIR" -mindepth 1 -maxdepth 1 -print -quit)" ]]; then
    fail "La carpeta ya existe y no está vacía: $PROJECT_DIR. No se sobrescribirá."
  fi
else
  mkdir -p "$PROJECT_DIR"
fi

SOURCE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cp "$SOURCE_DIR/README.md" "$PROJECT_DIR/README.md"
cp "$SOURCE_DIR/.gitignore" "$PROJECT_DIR/.gitignore"
mkdir -p "$PROJECT_DIR/docs" "$PROJECT_DIR/src" "$PROJECT_DIR/scripts/setup"
cp "$SOURCE_DIR/scripts/bootstrap.sh" "$PROJECT_DIR/scripts/setup/"
cp "$SOURCE_DIR/scripts/bootstrap.ps1" "$PROJECT_DIR/scripts/setup/"

cd "$PROJECT_DIR"
git init -b main
git add README.md .gitignore docs src scripts

if ! git config user.name >/dev/null; then
  read -r -p "Nombre para los commits de este repositorio: " git_name
  [[ -n "$git_name" ]] || fail "El nombre no puede estar vacío."
  git config user.name "$git_name"
fi
if ! git config user.email >/dev/null; then
  read -r -p "Correo para los commits (puedes usar el correo privado de GitHub): " git_email
  [[ -n "$git_email" ]] || fail "El correo no puede estar vacío."
  git config user.email "$git_email"
fi

git commit -m "chore: initialize Laboratorio repository"

if gh auth status >/dev/null 2>&1; then
  read -r -p "Nombre del repositorio público de GitHub [${REPO_NAME}]: " input_repo
  [[ -n "$input_repo" ]] && REPO_NAME="$input_repo"
  say "Se creará y publicará ${REPO_NAME} como repositorio PÚBLICO."
  read -r -p "¿Confirmas que el contenido inicial será público? [s/N]: " public_answer
  if [[ "${public_answer,,}" == "s" || "${public_answer,,}" == "si" ]]; then
    gh repo create "$REPO_NAME" --public --source . --remote origin --push
  else
    say "Repositorio local creado. No se publicó en GitHub."
  fi
else
  say "GitHub CLI aún no está autenticado."
  say "Ejecuta: gh auth login"
  say "Después, entra en ${PROJECT_DIR} y ejecuta:"
  say "gh repo create ${REPO_NAME} --public --source . --remote origin --push"
fi

say "Preparación inicial terminada. Carpeta: ${PROJECT_DIR}"
