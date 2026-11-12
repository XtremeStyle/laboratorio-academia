# CHANGE // SPACE-LAB OS — plataforma académica en desarrollo

Plataforma académica en construcción para gestionar semilleros, Tripulaciones, Misiones, entregas, evaluaciones y progreso. Identidad visual Space-Lab: fondo espacial, cian orbital neón, naranja de trayectoria, cristal esmerilado, tipografía monoespaciada + sans limpia y barra de estado tipo terminal.

## Qué incluye esta versión

- Panel principal responsive con navegación lateral y resumen de la cuenta autenticada.
- Semilleros de Robótica y Astronomía/Física Espacial.
- Resumen visual de XP, misiones, entregas y rango.
- Paneles de progreso y Tripulación.
- Nuevo acceso por solicitud: `/registro` (crear cuenta/solicitar clearance, validado ESTUDIANTE/PROFESOR) y `/administracion/solicitudes` (aprobación con contraseña temporal de un único uso).
- Rate-limit de envíos al formulario de login (20 por IP / 10 min; 429 + Retry-After en exceso).
- Nuevo diseño SPACE-LAB en las 32 plantillas (portada pública, login, registro, panel autenticado, laboratorios) con barra de estado tipo terminal.
- Jerarquía de dominio sellada `domain/Usuario → Estudiante · Profesor · Administrador` y capa de contrato `repository/UsuarioRepository` con implementación JDBC parametrizada.

- Menú móvil y navegación por secciones.
- Spring Boot 3.5, Java 21, Maven y Thymeleaf.
- H2 local legado (deprecado). La línea principal ya opera contra **PostgreSQL 18 real** (servicio 2026, puerto 5433, unit `systemd --user` `change-space-lab-postgres.service`). Migración H2→PG completada; sin datos ficticios.

- HTMX empaquetado mediante WebJars; no depende de un CDN.
- Estilos y scripts propios servidos localmente. Las fuentes usan tipografías del sistema.

> Los nombres, XP, fechas, miembros y misiones visibles son datos demostrativos. No representan datos reales; la consulta de semilleros usa registros de demostración persistidos en H2. El panel tras iniciar sesión muestra los semilleros y conteos académicos reales disponibles. Esta versión no está lista para producción.

## Requisitos

- Java 21 o superior.
- Conexión a internet en la primera ejecución para descargar Apache Maven y las dependencias Java.
- `curl` y `tar` para el lanzador Linux (el lanzador intenta instalarlos si faltan y detecta Arch Linux o Debian/Ubuntu).

PostgreSQL 18 está instalado en este equipo y el servicio local (`systemd --user`) lo arranca automáticamente. La base `change_space_lab` contiene los datos reales migrados desde H2 (sin cuentas de ejemplo ni semilleros ficticios). Para arrancar la app contra PG: `./INICIAR-CON-POSTGRES.sh`. Para volver a H2: `./run-laboratorio.sh` (dato deprecated para desarrollo activo).

## Ejecutar en Linux (recomendado)

1. Extrae el ZIP en una carpeta de tu elección.
2. Abre una terminal dentro de la carpeta `Laboratorio`.
3. Ejecuta:

   ```bash
   ./run-laboratorio.sh
   ```

El lanzador verifica Java, instala dependencias del sistema faltantes cuando puede, prepara Maven, compila el proyecto, inicia Spring Boot y abre `http://localhost:8080` en el navegador si `xdg-open` está disponible.

Para usar otro puerto:

```bash
PORT=8090 ./run-laboratorio.sh
```

Para detener el servidor iniciado desde una terminal, presiona `Ctrl+C`. Para detener la instancia registrada desde otra terminal, ejecuta `./DETENER-LABORATORIO.sh`.

## Ejecutar manualmente

```bash
./mvnw -DskipTests package
java -jar target/laboratorio-0.1.0-SNAPSHOT.jar --spring.profiles.active=dev
```

## Administración local de demostración

La primera cuenta administradora local es opcional y solo se crea en el perfil `dev` cuando se proporcionan ambas variables de entorno `LAB_ADMIN_EMAIL` y `LAB_ADMIN_PASSWORD`. La contraseña debe tener al menos 12 caracteres y como máximo 72 bytes UTF-8. Define los valores en tu entorno privado antes de iniciar; no los escribas en archivos del proyecto. Al arrancar, se crea la cuenta en la academia de demostración si aún no existe ningún administrador. El proceso no modifica contraseñas de cuentas existentes. Después inicia sesión en `/login` y visita `/administracion/semilleros`.

La inicialización local no es un procedimiento de alta para producción. La administración permite crear usuarios, editar sus nombres y roles, activar y desactivar cuentas, gestionar semilleros, asignaciones y Tripulaciones; hay un flujo inicial de Misiones con etapas, entregas de texto y evaluaciones. La nota usa escala de 0 a 5 y cada Misión muestra el promedio de sus etapas evaluadas. Las etapas obligatorias deben entregarse antes de la entrega final; un promedio inferior a 3,0 se señala como insuficiente. Los docentes cierran cada Misión asignada tras revisar la entrega final; los participantes conservan el historial. Las Misiones interdisciplinarias se pueden asignar a estudiantes o Tripulaciones de los semilleros vinculados. Un simulador educativo explora un circuito en serie con LED. El chat ofrece canales de Tripulación y conversaciones privadas entre estudiantes y profesores con semillero activo compartido. Cada usuario puede descargar su boletín PDF; también pueden hacerlo sus docentes con semillero activo compartido y los administradores de su academia. El boletín incluye las Misiones activas o completadas, competencias, XP e insignias. El panel muestra insignias por primera Misión, cinco Misiones y nota perfecta. Las entregas de texto admiten un adjunto PDF, imagen o ZIP de hasta 10 MB. Los archivos se guardan en `data/adjuntos` junto a la base local y requieren protección y copias privadas; se comprueba su firma básica, sin análisis antivirus. Las Misiones grupales admiten ajustes individuales justificados que se conservan al corregir evaluaciones. Las competencias vinculadas a criterios muestran progreso personal con base en las notas más recientes de esos criterios. Las notificaciones internas informan asignaciones y evaluaciones; la administración puede consultar una auditoría académica inicial. Las Misiones con final evaluado otorgan hasta 100 XP proporcionales a la nota y cuentan en la clasificación del semillero cuando todas las etapas obligatorias tienen entrega. Faltan funcionalidades y controles para uso real; consulta `docs/SESSION-STATE.md`. Para inicializar de forma interactiva, ejecuta `./INICIAR-CON-ADMIN.sh` y sigue las indicaciones. El script detiene primero la instancia registrada de esta misma carpeta, si existe.

## REST API y Swagger

API pública autenticada en `/api/**`: perfil, catálogo de cursos, detalle de matrícula, misiones, notificaciones, progreso por competencias y libro de XP. Documentada con OpenAPI 3 y servida por `Swagger UI`.

- `GET /swagger-ui.html` → redirige a `/swagger-ui/index.html`.
- `GET /v3/api-docs` → definición JSON de la API (`openapi`).
- `GET /v3/api-docs.yaml` → definición YAML.
- Requisitos de la entrega: 1 dependencia nueva (`org.springdoc:springdoc-openapi-starter-webmvc-ui`), documentada en `pom.xml` y en `SECURITY.md`.
- Autenticación: cookie `JSESSIONID` tras `POST /login`; la UI comparte la sesión del navegador.

## Cuentas de desarrollo (solo este clúster)

| Rol | Correo | Contraseña |
|---|---|---|
| Admin | `dev-admin@espacio.local` | `DevAdmin123!` |
| Docente | `dev-prof@espacio.local` | `DevProf123!` |
| Estudiante | `dev-estu1@espacio.local` | `DevEstu123!` |
| Estudiante | `dev-estu2@espacio.local` | `DevEstu123!` |

> Las cuentas anteriores fueron creadas manualmente en la base PG local para facilitar la verificación. No se crean automáticamente al instalar. Si instalas este paquete en otro equipo, tales cuentas no existen (Amazónica → creación desde `/registro` + aprobación administrativa).

## Pruebas

```bash
./mvnw test
node scripts/test-circuitos.cjs && node scripts/test-astronomia.cjs && node scripts/test-red-circuitos.cjs && node scripts/test-estaciones.cjs && node scripts/test-astronomia.cjs && node scripts/test-evidencia-diseno.cjs && node scripts/test-lanzamiento.cjs
```

## Rutas útiles

- Aplicación: `http://localhost:8080/`
- Consulta de semilleros de demostración (solo desarrollo): `http://localhost:8080/demostracion/semilleros`
- Salud: `http://localhost:8080/actuator/health`

## Estructura

```text
src/main/java/com/laboratorio
├── config/
└── presentation/web/
src/main/resources
├── db/migration/
├── static/css/
├── static/js/
└── templates/
```

La estructura modular de dominio se incorporará a medida que se aprueben los modelos y reglas de negocio. No se crean paquetes vacíos por anticipado.

## Laboratorios y rutas formativas

- `/laboratorio/circuitos`: laboratorio de Robótica y Electrónica. Modelo DC ideal de un circuito en serie con batería, resistencia, interruptor y LED; calcula corriente y potencia y advierte al exceder límites declarados. No sustituye mediciones ni prácticas supervisadas con componentes reales.
- `/laboratorio/astronomia`: laboratorio de Astronomía. Problema de dos cuerpos con órbita kepleriana; calcula periodo (tercera ley de Kepler en forma normalizada), perihelio, afelio y velocidades (vis-viva en unidades normalizadas), con presets y controles accesibles. Aproximación educativa: no apta para navegación real.
- `/cursos`: CHANGE Academy. Rutas formativas persistidas (Robótica y Cosmos, seis lecciones cada una) con inscripción por semillero activo y registro de evidencia por lección. Las rutas tienen **ocho lecciones** y se vinculan automáticamente a semilleros activos con código `ROBOTICA`, `ASTRONOMIA` o `COSMOS`.

Los laboratorios son simulaciones educativas y no instrumentos de medición reales. Pruebas de modelos: `node scripts/test-circuitos.cjs`, `node scripts/test-astronomia.cjs`, `node scripts/test-red-circuitos.cjs`, `node scripts/test-estaciones.cjs`, `node scripts/test-evidencia-diseno.cjs` y `node scripts/test-lanzamiento.cjs` (incluye Δv de Tsiolkovsky y relación empuje/peso).

Esta primera versión de cursos no sustituye el sistema de Misiones: la conversión de actividades en Misiones evaluables, rúbricas docentes, XP/insignias por curso y certificaciones sigue en desarrollo. La migración V6 añade tablas sin modificar las anteriores. La validación disponible se realizó con H2; PostgreSQL real y la revisión integral de seguridad deben ejecutarse antes de considerar la plataforma apta para producción.

## Seguridad de la aplicación web

Ver [`SECURITY.md`](SECURITY.md) y [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md) para el análisis técnico completo (OWASP Top 10 2021, limitaciones conocidas).


La configuración de Spring Security añade cabeceras: CSP estricta (`script-src 'self'`, sin scripts inline), `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy: strict-origin-when-cross-origin` y `Permissions-Policy` que desactiva cámara, micrófono y geolocalización. La página de inicio de sesión es propia (`/login`) y todas las plantillas sirven JS y CSS locales. En el perfil de producción la cookie de sesión es http-only, secure y same-site lax, con expiración de 30 minutos. La cabecera HSTS solo se emite sobre HTTPS (en desarrollo localhost no aparece). No constituye una revisión de seguridad completa.

## GitHub

Este paquete no publica ni sincroniza nada con GitHub. La publicación queda reservada para cuando el proyecto esté terminado y exista autorización expresa.

### Diseños guardados

En las mesas de circuito y cohete se puede guardar y recuperar un diseño de cada tipo por cuenta. Guardar de nuevo reemplaza el anterior. En una lección, el estudiante puede añadir un resumen del diseño guardado a su evidencia y revisarlo antes de guardar o completar. Esto no otorga calificación ni XP automáticamente. La migración V8 almacena los diseños en la base; las prácticas usan modelos educativos simplificados.
