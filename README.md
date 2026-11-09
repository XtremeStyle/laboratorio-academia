# Laboratorio

Plataforma web académica para la gestión de semilleros, tripulaciones, misiones,
entregas, evaluación y gamificación.

## Ubicación de desarrollo

El instalador crea el proyecto en:

- Linux/macOS: `~/Projects/ACADEMY/Laboratorio`
- Windows: `%USERPROFILE%\Projects\ACADEMY\Laboratorio`

## Requisitos previstos

- Java 21
- Git
- GitHub CLI (`gh`) para crear/publicar el repositorio desde el asistente
- Maven Wrapper (se incorporará con el proyecto Spring Boot)

## Inicialización

### Linux (Omarchy / Arch Linux) y macOS

Desde la carpeta extraída `Laboratorio-inicio`:

```bash
chmod +x scripts/bootstrap.sh
./scripts/bootstrap.sh
```

### Windows (PowerShell)

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\scripts\bootstrap.ps1
```

Los scripts solicitan confirmación antes de instalar paquetes o publicar el
repositorio. Para crear el repositorio público se utiliza GitHub CLI y debes
autenticarte con tu propia cuenta mediante `gh auth login`.

El instalador no sobrescribe una carpeta `Laboratorio` que ya tenga contenido.
También copia los scripts de preparación dentro del repositorio, en
`scripts/setup/`, para conservarlos junto al proyecto.

## Estado

Este paquete prepara herramientas y GitHub. Todavía no contiene la aplicación
Spring Boot; esa será la siguiente etapa, después de verificar el entorno.
