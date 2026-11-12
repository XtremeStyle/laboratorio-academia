# SECURITY.md — CHANGE // SPACE-LAB OS

Análisis honesto de las medidas de seguridad y de las limitaciones conocidas. Este documento describe lo implementado y probado; no declara conformidad con ningún estándar externo. **No usar con datos reales de estudiantes hasta una revisión integral externa.**

## OWASP Top 10 (2021) — análisis aplicado al proyecto

### A01 Broken Access Control
- RBAC persistente: ESTUDIANTE, PROFESOR, ADMINISTRADOR (catálogo `rol`); rutas `/administracion/**` exigen ADMINISTRADOR; `/academia/**` exige PROFESOR o ADMINISTRADOR; el resto exige autenticación.
- Aislamiento por academia y semillero verificado por pruebas de integración (asignaciones, Tripulaciones, Misiones, chat, cursos, progreso).
- El docente/administrador solo consulta evidencias y progreso de los semilleros de su ámbito, con paginación; una corrección grupal registra ajustes individuales justificados que afectan únicamente a su destinatario.
- Solicitudes de acceso (`/registro`): el alta es una **solicitud** aprobada por el administrador; sin privilegios auto-otorgados. Aprobar genera el rol solicitado (nunca ADMINISTRADOR) como contraseña temporal de un único uso.
- Pruebas negativas: ESTUDIANTE→`/administracion/**` (403), acceso entre academias denegado, docente ajeno (403/404).
- Pendiente conocido: límites de abuso del chat (mensajes/minuto) y bloqueo de cuenta tras intentos fallidos repetidos de login (existe freno por IP en memoria, ver A07).

### A02 Cryptographic Failures
- Contraseñas con **BCrypt** (parámetros por defecto de Spring Security).
- Sin contraseñas por defecto; el primer administrador se crea vía variables de entorno (`LAB_ADMIN_EMAIL`/`LAB_ADMIN_PASSWORD`) o el script interactivo, nunca se almacena en código ni en el repositorio (.gitignore: `.env`, `data/`).
- Cookie de sesión http-only en prod, `Secure` + `SameSite=Lax` en prod; HSTS (solo se emite sobre HTTPS).
- Ningún secreto en el repositorio ni en el ZIP de entrega (excluidos `data/`, adjuntos, `target/`, Git).
- Contraseñas temporales de alta generadas con `SecureRandom` (14 caracteres del alfabeto inequívoco), BCrypt en reposo, mostradas una sola vez al administrador.

### A03 Injection
- Sentencias vía `JdbcTemplate` con parámetros `?` en todo el código base.
- Las dos construcciones dinámicas son seguras: placeholders generados a partir del número de parámetros (`?` × n) y un fragmento estático interno (`scope`) de elección binaria en código (ADMIN: `s.academia_id=?` / resto: subconsulta de asignación). Ninguna incorpora input de usuario.
- Thymeleaf `th:text` y `th:utext` se revisaron: la evidencia docente se renderiza con escape (`&lt;script&gt;`) verificado con prueba explícita.

### A04 Insecure Design
- Flujo público **incompleto por diseño**: se puede solicitar una cuenta, pero el alta requiere aprobación administrativa con validación de rol (ESTUDIANTE/PROFESOR).
- Revisión docente de evidencias sin modificarlas; progreso monotónico; cierre de asignaciones bloqueado por membresía activa (se verifica antes de cerrar).
- Auditoría de eventos académicos y `DESCARGAR_BOLETIN`; notificaciones internas por destinatario.
- Registro/escritura idempotente de matrículas y membresías; serialización por bloqueos de fila.

### A05 Security Misconfiguration
- Cabeceras: CSP estricta (`default-src 'self'`, `frame-ancestors 'none'`, `object-src 'none'`, `script-src 'self'`), `frame-deny`, `nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy: camera=(), microphone=(), geolocation=()`; HSTS en HTTPS.
- Sin CORS habilitado (misma-origin); WebSocket sin origen remoto admitido.
- Servidor solo 127.0.0.1 en dev; en prod detrás de TLS (HSTS 31.536.000 s, incluye subdominios).
- Perfiles: `dev` (desarrollo), `prod` (endurecimiento de cookie SameSite=Lax http-only), `test` (tests/ajuncts en `target/`).

### A06 Vulnerable and Outdated Components
- Spring Boot 3.5.15, Java 21, Thymeleaf (activo mantenido), PDFBox 3.0.8, Flyway, H2/PostgreSQL.
- HTMX servido como WebJar local (sin CDN); estilos/fuentes propios; sin servicios externos en tiempo de ejecución.
- No hay dependencias propuestas sin uso verificado; dependencias y versiones fijadas en `pom.xml`.

### A07 Identification and Authentication Failures
- Capas: BCrypt + validación de correo/contraseña, roles persistidos, sesiones con invalidación si cambia estado/roles/hash de contraseña (`SessionAccountFilter`), BCrypt para temporales de alta.
- **Brute force**: `/login` cuenta 20 envíos POST por IP por 10 minutos; en exceso responde 429 con `Retry-After`. Limitaciones: contador en memoria (una instancia, no persistente entre reinicios, no compartido entre instancias); el conteo cubre todos los envíos, no solo fallos (se prefiere falso positivo moderado para el formulario).
- No hay registro libre; el alta exige aprobación administrativa (no hay fuerza bruta de registro).

### A08 Software and Data Integrity Failures
- Sin CDN ni paquetes remotos en tiempo de ejecución; artefactos y fuentes locales.
- Sin firma de artefactos; despliegue manual. Los builds se ejecutan en el equipo local; los recursos compilados no se publican.
- El pipeline de migración Flyway preserva historia; cada V8/V9 se ensayó sobre copia en H2 antes de aplicar a la base local.

### A09 Security Logging and Monitoring Failures
- Auditoría transaccional en `auditoria` (acciones académicas, consultas administrativas, boletines — sin contenido entregado ni contraseñas).
- Notificaciones internas al destinatario; eventos de alta/aprobación/rechazo (`APROBAR_SOLICITUD` / `RECHAZAR_SOLICITUD`) auditados.
- Logs de aplicación en stdout (dev) sin datos sensibles; sin telemetría externa.

### A10 Server-Side Request Forgery
- No hay llamadas salientes desde el servidor (sin fetch/HTTP a destinos externos); adjuntos se validan por firma y extensión y se sirven locales con autorización.

## Limitaciones conocidas (vigentes a 2026-11-11)

- Sin control de abuso del chat; sin antivirus de adjuntos; solo verificación de firma básica y tipo de archivo.
- El freno de login no identifica al usuario afectado (solo IP) y se pierde al reiniciar.
- Rate limiting/DF en memoria (una instancia); para producción multi-instancia se requiere store compartido.
- PostgreSQL sin validación; H2 ≠ garantía de integridad relacional real en prod.
- No hay certificación, pentest externo ni WAF; la copia de seguridad del estado es local.
- Los boletines PDF docentes pueden incluir datos de varios estudiantes de su ámbito; el control de acceso a la descarga está verificado pero no auditado como sistema de gestión de datos (GDPR-like).
- El flujo de solicitudes no integra correo; la entrega de credenciales temporales depende de canales como el navegador/bandeja administrativa.

## Ajuste de accesibilidad (skip-link, objetivo WCAG 2.2 AA)

- Encabezado nativo del navegador `Header` mantienen `lab-switch`, `nav.lab-contents`, `.mobile-menu` y la navegación lateral como `<nav aria-label=...>` con estados Page `aria-current="page"` donde aplica.
- Ancla accesible `skip-link` visible al enfocar (`:focus-visible`) en las 32 plantillas (selector `<main id="contenido">`).
- Contraste verificado programáticamente (Chromium/about:blank): texto 17.28:1; muted 7.59:1; cyan 13.10:1; naranja 9.67:1; todos ≥4.5:1 (WCAG 2.2 AA; mayoría AAA).
- Animaciones respetadas con `prefers-reduced-motion: reduce` y transiciones deshabilitadas bajo esa preferencia.
- Prueba `AccessibilityTest` bloquea regresiones en `/login`, `/` y `/registro`.

## Verificación aplicada en esta entrega

- Suite completa de integración (H2) aprobada: **63 pruebas, 0 fallos/0 errores**, incluidas pruebas de aislamiento por academia/semillero, roles, CSRF (403 sin token), sesiones, migraciones V8→V10, flujo de solicitudes y nuevas pruebas de endurecimiento (`SecurityHardeningTest`: 429/Retry-After tras 20+1 envíos de login desde la misma IP; cabeceras CSP/X-Frame-Options/nosniff/Referrer-Policy/Permissions-Policy; `denyAll` sobre `/actuator/**` salvo health/info; consulta IDOR entre estudiantes por matrícula).
- Barrido ciberseguridad en caja negra contra la vista aislada (9/9 PASS): SQLi en login rechazada, XSS no se refleja, traversal (400), `/actuator/env` inaccesible (302/403 para todo rol), `.env` no se sirve, CSRF exigido, rutas administrativas ajenas → 403, búsqueda pública escapada.
- Recorrido integral guiado en Chromium con datos ficticios (roles, Misiones, cierres, chat, boletines, competencias, cursos); capturas de UI con Playwright.
- Variables de entorno y rutas privadas nunca se imprimen en logs de entrega ni en documentación versionada.
- `/actuator/**` aplica `denyAll()` salvo `/actuator/health` e `/actuator/info`.
- V10 (libro de XP) aplicada en la base privada de 8080 con backup verificado (SHA-256).
- PostgreSQL real: **verificado en este equipo** (PostgreSQL 18.6, clúster local autenticado en loopback:5433 con `trust`; servicio `systemd --user change-space-lab-postgres.service`). Flyway aplica V1–V10 exitosamente sobre PG y las 14 cuentas reales y el progreso real fueron migradas (testeado con login y ciclo registro→aprobación en `:8090`).
- Incidencia durante la integración: migración H2→PG sobre copia con backend sqlite manual y `pg_dump`-compatible; en la práctica se exportó con **H2 Script** desde una copia de respaldo y se importó con `psql -v ON_ERROR_STOP=1`, limpiando el `PUBLIC` schema y quitando el `flyway_schema_history` duplicado.
- Lección: al migrar en caliente, siempre verificar conteos tabla a tabla (14 usuarios, 18 progreso_leccion, 3 matrículas, 25 auditoría, 10 migraciones) antes de conmutar la instancia principal.
