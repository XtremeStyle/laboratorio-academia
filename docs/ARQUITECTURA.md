# Arquitectura — CHANGE // SPACE-LAB OS

Monolito Spring Boot en capas sobre Flyway, sin dependencias externas en tiempo de ejecución (toda la librería web se sirve localmente). Java 21, Spring Boot 3.5.x, Thymeleaf, H2 (desarrollo) y PostgreSQL (producción prevista).

## Capas

```
presentation  com.laboratorio.*.*Controller (46)  → HTTP: mapea parámetros, valida, redirige
  application/service (36)                        → reglas de negocio y transacciones (@Transactional)
  domain      com.laboratorio.domain              → POO puro: jerarquía sellada Usuario (Estudiante ·
                                                Profesor · Administrador), Rol, EstadoUsuario,
                                                SolicitudCuenta
  repository  com.laboratorio.repository           → contratos JDBC parametrizados (UsuarioRepository) + adaptadores
  persistence Flyway V1..V9                        → DDL versionado; H2 de desarrollo y PostgreSQL de producción
  security    config/SecurityConfiguration         → RBAC, CSP, formLogin, SessionAccountFilter (invalidación por cambio de cuenta)
```

## Política de persistencia

- Todas las consultas usan `JdbcTemplate` con parámetros `?`. No se concatena input de usuario en SQL. Los únicos SQL dinámicos son placeholders generados (`WHERE id IN (${n×?}))`) y fragmentos estáticos (`scope`) compilados en código.
- H2 persistente en desarrollo (`data/`) y H2 en memoria en pruebas. PostgreSQL instalado como driver; **sin verificar en este entorno**.
- El modelo de datos no garantiza por sí solo todas las reglas; los servicios transaccionales comprueban academia, asignaciones, membresías y secuencia de etapas con bloqueos de fila (`FOR UPDATE`) donde aplica concurrencia.

## Registro público acotado

Autoservicio solo como **solicitud de cuenta** (`/registro`): la academia aprueba el alta con una contraseña temporal de un único uso (mostrada una sola vez, BCrypt). No existe registro libre; el alta directa la conserva el administrador (`/administracion/usuarios`). Ver `docs/SECURITY.md`.

## Transacciones y concurrencia

Servicios `@Transactional`; unicidad de asignaciones/membresías serializada por bloqueos. Pruebas de concurrencia en H2 (altas simultáneas, dos evaluaciones simultáneas) en verde. Enumeración completa de la cobertura en `docs/DATA-MODEL-STATUS.md`.

## Inercia deliberada

El patrón de repositorios se adopta incrementalmente: `repository/` envuelve identidad y crecerá hacia evaluaciones, misiones y chat. Los servicios restantes usan `JdbcTemplate` directo hasta su migración.
