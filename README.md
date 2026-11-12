# CHANGE // SPACE-LAB OS — plataforma académica

Plataforma académica para gestionar semilleros, Tripulaciones, Misiones, entregas, evaluaciones y progreso. Identidad visual Space-Lab: fondo espacial, cian orbital neón, naranja de trayectoria, cristal esmerilado, tipografía monoespaciada + sans limpia y barra de estado tipo terminal.

## Qué incluye esta versión

- Panel principal responsive con navegación lateral y resumen de la cuenta autenticada.
- Semilleros de Robótica y Astronomía/Física Espacial.
- Resumen visual de XP, misiones, entregas y rango.
- Paneles de progreso y Tripulación.
- Acceso por solicitud: `/registro` (crear cuenta validando ESTUDIANTE/PROFESOR) y `/administracion/solicitudes` (aprobación con contraseña temporal de un único uso).
- Rate-limit de envíos al formulario de login (20 por IP / 10 min; 429 + `Retry-After` en exceso).
- Diseño SPACE-LAB en las 33 plantillas (portada pública, login, registro, panel autenticado, laboratorios) con barra de estado tipo terminal.
- Jerarquía de dominio sellada `domain/Usuario → Estudiante · Profesor · Administrador` y capa de contrato `repository/UsuarioRepository` con implementación JDBC parametrizada.
- API REST en `/api/**` documentada con OpenAPI 3 (Swagger UI); perfil, catálogo de cursos, misiones, notificaciones, progreso por competencias y libro de XP.
- Notas con escala 0–5, criterios ponderados (`peso`) y restricciones `CHECK` en el esquema (migración V12).
- HTMX empaquetado mediante WebJars; no depende de un CDN. Estilos y scripts propios servidos localmente.

> Los nombres, XP, fechas, miembros y misiones visibles son datos demostrativos. No representan datos reales. Esta versión no está lista para producción.

## Inicio rápido (probar en un equipo limpio)

```bash
git clone https://github.com/XtremeStyle/laboratorio-academia.git
cd laboratorio-academia
./mvnw clean package   # compila y ejecuta la batería de pruebas (69)
./run-laboratorio.sh   # arranca en http://localhost:8080 (H2 persistente en ./data)
```

El primer arranque con H2 vacío pide crear un administrador (lo crea el perfil `dev` solo si se definen `LAB_ADMIN_EMAIL` y `LAB_ADMIN_PASSWORD`, o usando el asistente):

```bash
./INICIAR-CON-ADMIN.sh   # solicita correo y contraseña inicial, y arranca
```

El inicio de sesión queda en `http://localhost:8080/login`.

## Requisitos

- Java 21 o superior.
- Conexión a internet en la primera ejecución para descargar Apache Maven y las dependencias Java.
- `curl` y `tar` para el lanzador Linux (el lanzador intenta instalarlos cuando faltan).
- PostgreSQL 18 (opcional; ver “PostgreSQL”).

## Ejecutar

```bash
./run-laboratorio.sh              # H2 (por defecto) en http://localhost:8080
PORT=8090 ./run-laboratorio.sh    # otro puerto
./mvnw -DskipTests package        # solo empaquetar
java -jar target/laboratorio-0.1.0-SNAPSHOT.jar   # ejecución manual (perfil dev + H2)
./docs/verificar.sh               # compilación, pruebas y conteo en un paso
```

Para detener el servidor iniciado desde una terminal, presiona `Ctrl+C`. Para detener una instancia registrada desde otra terminal, ejecuta `./DETENER-LABORATORIO.sh`.

### PostgreSQL (opcional)

Las migraciones Flyway V1–V12 se aplican tanto sobre H2 como sobre PostgreSQL. Para correr contra un servidor local ya preparado (base creada, usuario con permisos y `pg_isready` respondiendo):

```bash
java -jar target/laboratorio-0.1.0-SNAPSHOT.jar \
  --spring.profiles.active=dev \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/<base> \
  --spring.datasource.username=<usuario> \
  --spring.datasource.password=<contraseña> \
  --spring.datasource.driver-class-name=org.postgresql.Driver
```

## Administración local de demostración

La primera cuenta administradora es **opcional**. En el perfil `dev` se crea al arrancar solo si se proporcionan ambas variables `LAB_ADMIN_EMAIL` y `LAB_ADMIN_PASSWORD` (12+ caracteres, máximo 72 bytes UTF-8). Define los valores en tu entorno; no los escribas en archivos del proyecto. Al arrancar se crea la cuenta en la academia de demostración si aún no existe ningún administrador; el proceso no modifica contraseñas existentes. Para el alta interactiva usa `./INICIAR-CON-ADMIN.sh`.

### Capacidades de administración

- Crear usuarios, editar nombres y roles, y activar/desactivar cuentas dentro de la academia (con protección del último administrador).
- Gestionar semilleros, asignaciones y Tripulaciones.
- Misiones con etapas (añadidas antes de asignar), entregas de texto y evaluaciones versionadas. La nota usa escala de 0 a 5 y cada Misión muestra el promedio de sus etapas evaluadas. Un promedio inferior a 3,0 se señala como insuficiente.
- Corrección con ajustes individuales justificados para Misiones grupales; se conservan al corregir.
- Competencias vinculadas a criterios: progreso personal con base en las notas más recientes de esos criterios.

### Flujo académico

1. `/registro` solicita una cuenta (ESTUDIANTE o PROFESOR).
2. Un administrador la aprueba en `/administracion/solicitudes`; el sistema genera una contraseña temporal de un único uso.
3. La persona cambia su contraseña en su perfil; la sesión se invalida si cambia la contraseña o los roles.

### Adjuntos y archivos

Las entregas de texto admiten un adjunto PDF, imagen o ZIP de hasta 10 MB. Se almacenan en `data/adjuntos` junto a la base local; protegidos por permisos del sistema de archivos porque no viven en la base. Se comprueba su firma básica (magic bytes), sin análisis antivirus. Respalda `data/` junto con la base.

### Notificaciones, auditoría y XP

Las notificaciones internas informan asignaciones y evaluaciones. La administración puede consultar una auditoría académica inicial. Las Misiones con final evaluado otorgan hasta 100 XP proporcionales a la nota; el rango y la clasificación del semillero se recalculan al corregir.

## Cuentas de prueba

El repositorio **no incluye credenciales**. En un equipo limpio, al instalar con `./install-space-lab.sh` se genera una contraseña temporal única y se muestra una sola vez (o define `LAB_ADMIN_EMAIL`/`LAB_ADMIN_PASSWORD` en el entorno). Fuera del perfil `dev` no se crean cuentas por defecto. Para recorrer el flujo completo, crea la cuenta de administrador y, desde ella, registra y aprueba estudiantes y docentes desde `/registro` y `/administracion/solicitudes`.

## REST API y Swagger

API pública autenticada en `/api/**`: perfil, catálogo de cursos, detalle de matrícula, misiones, notificaciones, progreso por competencias y libro de XP. Documentada con OpenAPI 3 y servida por Swagger UI.

- `GET /swagger-ui.html` → redirige a `/swagger-ui/index.html`.
- `GET /v3/api-docs` → definición JSON de la API (`openapi`).
- `GET /v3/api-docs.yaml` → definición YAML.
- Dependencia: `org.springdoc:springdoc-openapi-starter-webmvc-ui` (ver `pom.xml`).
- Autenticación: cookie `JSESSIONID` tras `POST /login`; la UI comparte la sesión del navegador.

## Pruebas

```bash
./mvnw test                # batería JUnit (69 pruebas): integración, migración, seguridad, ponderación de notas
node scripts/test-circuitos.cjs && node scripts/test-astronomia.cjs && node scripts/test-red-circuitos.cjs \
  && node scripts/test-estaciones.cjs && node scripts/test-luna.cjs && node scripts/test-pulsos.cjs \
  && node scripts/test-evidencia-diseno.cjs && node scripts/test-lanzamiento.cjs
```

## Rutas útiles

- Aplicación: `http://localhost:8080/`
- Consulta de semilleros de demostración (solo desarrollo): `http://localhost:8080/demostracion/semilleros`
- Salud: `http://localhost:8080/actuator/health`

## Estructura

```text
src/main/java/com/laboratorio
├── api/            API REST /api/** (perfil, cursos, misiones, notificaciones, progreso, XP)
├── attachment/     adjuntos de entregas (magic bytes, almacenamiento con clave UUID)
├── audit/          bitácora de auditoría (quién hizo qué y cuándo)
├── chat/           canales de Tripulación y conversaciones privadas (WebSocket)
├── config/         seguridad, cabeceras CSP/HSTS, filtros y manejo global de errores
├── course/         catálogo de cursos y lecciones
├── crew/           Tripulaciones y membresías
├── domain/         jerarquía sellada Usuario → Estudiante · Profesor · Administrador
├── evaluation/     evaluaciones versionadas, notas y ponderación por criterio
├── gamification/   XP, rangos, insignias y clasificación
├── identity/       usuarios, sesiones, solicitudes de cuenta, rate-limit de login
├── lab/            laboratorios educativos (circuitos, astronomía) y lab/design (diseños guardados)
├── mission/        Misiones, etapas, asignaciones y orden de bloqueo
├── notification/   notificaciones internas
├── presentation/   controladores MVC, formularios y vistas de estado; presentation/web (páginas web)
├── progress/       progreso por competencias, semilleros y alertas
├── report/         boletín PDF por usuario
├── repository/     contratos JDBC parametrizados (p. ej. UsuarioRepository)
├── research/       asignaciones y alcance por semillero
└── submission/     entregas de texto y versiones
src/main/resources
├── db/migration/   Flyway V1..V12
├── static/         CSS, JS, fuentes e imágenes servidos localmente
└── templates/      33 plantillas Thymeleaf
```

## Laboratorios y rutas formativas

- `/laboratorio/circuitos`: laboratorio de Robótica y Electrónica. Modelo DC ideal de un circuito en serie con batería, resistencia, interruptor y LED; calcula corriente y potencia y advierte al exceder límites declarados.
- `/laboratorio/astronomia`: laboratorio de Astronomía. Problema de dos cuerpos con órbita kepleriana; calcula periodo (tercera ley de Kepler en forma normalizada), perihelio, afelio y velocidades (vis-viva), con presets y controles accesibles.
- `/cursos`: CHANGE Academy. Rutas formativas persistidas (Robótica y Astronomía, ocho lecciones cada una) con inscripción por semillero activo y registro de evidencia por lección.

Los laboratorios son simulaciones educativas y no instrumentos de medición reales. Resultados de los modelos cubiertos por las pruebas `node scripts/test-*.cjs`.

Esta primera versión de cursos no sustituye el sistema de Misiones: la conversión de actividades en Misiones evaluables, rúbricas docentes, XP/insignias por curso y certificaciones sigue en desarrollo. Las migraciones Flyway V1–V12 se aplican tanto sobre H2 como sobre PostgreSQL.

## Seguridad de la aplicación web

Ver [`SECURITY.md`](SECURITY.md) y [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md) para el análisis técnico completo (OWASP Top 10 2021, limitaciones conocidas).

La configuración de Spring Security añade cabeceras: CSP estricta (`script-src 'self'`, sin scripts inline), `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy: strict-origin-when-cross-origin` y `Permissions-Policy` que desactiva cámara, micrófono y geolocalización. La página de inicio de sesión es propia (`/login`) y todas las plantillas sirven JS y CSS locales. En el perfil de producción la cookie de sesión es http-only, secure y same-site lax, con expiración de 30 minutos. La cabecera HSTS solo se emite sobre HTTPS (en desarrollo localhost no aparece). No constituye una revisión de seguridad completa.

## Repositorio

Código fuente público en [`github.com/XtremeStyle/laboratorio-academia`](https://github.com/XtremeStyle/laboratorio-academia) (licencia Apache-2.0). El historial refleja el desarrollo por entregas: inicialización, entrega completa, migraciones y mejoras de esquema. Para trabajar nuevos cambios, crea una rama y abre un pull request (ver [`CONTRIBUTING.md`](CONTRIBUTING.md)). La base de datos local (`data/`) y el entorno compilado (`target/`) no forman parte del repositorio.

## Diseños guardados

En las mesas de circuito y cohete se puede guardar y recuperar un diseño de cada tipo por cuenta. Guardar de nuevo reemplaza el anterior. En una lección, el estudiante puede añadir un resumen del diseño guardado a su evidencia y revisarlo antes de guardar o completar. Esto no otorga calificación ni XP automáticamente. La migración V8 almacena los diseños en la base; las prácticas usan modelos educativos simplificados.
