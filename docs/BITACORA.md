# Bitácora del proyecto

## 2026-11-09 — Contexto y traspaso para otros asistentes

- Se creó esta carpeta por solicitud del propietario y se guardaron sus instrucciones originales.
- Se documentaron ubicación, restricciones, estado implementado, pruebas y pendientes en `ESTADO-Y-TRASPASO.md`.
- Se agregó una instrucción permanente en esta carpeta y en el `AGENTS.md` del proyecto para actualizar contexto y copias al terminar cada bloque.
- Se generó una instantánea local de código y documentación, excluyendo Git, compilados, dependencias, secretos y datos de la base. Consultar `copias/`.
- Antes de esta tarea: verificación Maven con 15 pruebas aprobadas; aplicación iniciada en desarrollo, con demostración y administración limitada de semilleros. Los demás módulos siguen pendientes.

## 2026-11-09 — Proyecto autónomo y avance académico

- Se copió el proyecto completo con Git local y H2, con la aplicación detenida, a `PROYECTO/` dentro de esta carpeta; desde entonces esa copia es la fuente activa. La carpeta de contexto tiene permisos privados. No se eliminó la original.
- Se implementaron creación de usuarios con múltiples roles, asignaciones a semilleros con historial, Tripulaciones y membresías, Misiones individuales y grupales, entregas de texto con versiones, autorización tardía y nota base con historial.
- `./mvnw -q verify`: 22 pruebas aprobadas en H2. No se ha probado PostgreSQL ni concurrencia real; los límites funcionales y de seguridad constan en `ESTADO-Y-TRASPASO.md` y `PROYECTO/docs/DATA-MODEL-STATUS.md`.
- Se conservan las instrucciones originales y la prohibición de publicar o hacer commits sin autorización específica. Se deja el trabajo en esta carpeta para su continuación.

- Se verificó un respaldo íntegro privado en `respaldos/Laboratorio-completo-20261109-2328.tar.gz`, con base H2, Git y artefacto compilado. Se comprobó el arranque desde `PROYECTO`, salud UP, consulta de semilleros y protección de administración.
- Para crear un primer administrador local sin guardar una contraseña en el historial de comandos, usar `PROYECTO/INICIAR-CON-ADMIN.sh` y seguir las indicaciones.

## 2026-11-09 — Comprobación para retomar en un chat nuevo

- El propietario aclaró que eliminará el proyecto o registro en Codex/ChatGPT; se añadió `LEER-PRIMERO.md` con un texto para una futura IA y rutas independientes del chat.
- Se comprobó la integridad SHA-256 del respaldo íntegro, que contiene código, `.git/HEAD`, ejecutable y base H2. Se verificaron la copia activa, las instrucciones y los informes existentes de 22 pruebas aprobadas.
- Las copias están en el disco local y no se sincronizan automáticamente fuera del equipo; el respaldo es una instantánea, no una actualización continua.

## 2026-11-10 — Etapas y promedio académico

- Objetivo: avanzar el flujo académico y la primera parte de progreso con las decisiones del propietario: escala 0–5 y promedio simple de etapas evaluadas.
- Archivos: servicios y controladores de Misiones, entregas, autorizaciones y evaluaciones; plantillas académicas; prueba de integración; README y documentos de estado, hoja de ruta y modelo.
- Decisiones: se añaden etapas antes de asignar, con plazo y carácter obligatorio u opcional; cada etapa mantiene versiones y autorización tardía propias. El promedio usa la última evaluación de cada etapa y excluye notas históricas fuera de escala, que se señalan para revisión. Se serializa la asignación con la adición de etapas.
- Prueba real: `./mvnw -q verify`, 23 pruebas aprobadas en H2. Una ejecución intermedia detectó un desacuerdo de formato decimal y se corrigió antes de la verificación final. PostgreSQL y concurrencia real no se probaron.
- Pendientes: obligatoriedad aún no bloquea la entrega final; criterios por etapa, adjuntos, ajustes individuales, competencias y resto del roadmap. El panel inicial sigue siendo demostrativo. Siguiente paso: completar reglas académicas y progreso por competencias, sin inventar fórmulas de gamificación.

## 2026-11-10 — Reglas académicas y criterios por etapa

- Objetivo: cumplir reglas elegidas por el propietario y continuar las fases. La entrega final exige al menos una versión en cada etapa obligatoria; se identifica rendimiento insuficiente con promedio menor de 3,0. Las etapas nuevas incluyen criterio explícito.
- Archivos: servicios de entregas, Misiones y evaluaciones; controladores, plantillas, pruebas de integración y documentos de estado. Se serializan envíos y evaluaciones sobre la asignación para reducir carreras; la concurrencia real aún no se probó. Las etapas anteriores sin criterio se conservan.
- `./mvnw -q verify`: 23 pruebas aprobadas en H2; una prueba intermedia detectó y permitió corregir una expresión de plantilla. No se probó PostgreSQL.
- Siguiente paso: implementar XP proporcional a nota hasta 100 por Misión y clasificación accesible al semillero, según decisiones del propietario; faltan el resto de fases.

## 2026-11-10 — XP, clasificación por semillero y revocación

- Implementación inicial de XP proporcional a la nota (hasta 100 por Misión), clasificación visible a usuarios autorizados del semillero y revocación de autorizaciones tardías conservando entregas históricas. El cálculo de XP utiliza evaluaciones vigentes, requiere evaluación final y entregas obligatorias; al corregir una evaluación recalcula en vez de duplicar puntos. Sin libro de movimientos XP.
- Archivos: nuevo módulo `gamification`, plantilla de clasificación, servicios y controladores de autorizaciones, pruebas de integración y documentación.
- `./mvnw -q verify`: 23 pruebas aprobadas en H2, incluidos cálculo y corrección de XP, aislamiento de semilleros y revocación; PostgreSQL y concurrencia real sin probar.
- Pendientes: rangos, insignias, clasificación global, notificaciones, auditoría y demás fases. No se ha reiniciado la aplicación que estaba abierta; ejecutar la nueva compilación para ver los cambios.

## 2026-11-10 — Notificaciones internas y auditoría inicial

- Se añadieron notificaciones por asignación y evaluación con lectura restringida al destinatario, y auditoría transaccional de creación de Misiones y etapas, asignación, evaluación y autorización/revocación tardía. La vista de auditoría se limita a administradores de la academia y no contiene entregas ni contraseñas.
- Archivos: módulos `notification` y `audit`, servicios académicos, plantillas, pruebas de integración y documentación.
- `./mvnw -q verify`: 23 pruebas aprobadas en H2, incluyendo acceso a notificaciones y auditoría; no se probaron PostgreSQL ni concurrencia real.
- Pendiente: ampliar auditoría a todas las operaciones y fallos, más tipos de notificación, competencias, cuentas, rangos, insignias, chat y resto de fases. La instancia abierta aún usa la compilación anterior y no se reinició.

## 2026-11-10 — Panel personalizado inicial

- El panel tras iniciar sesión muestra Misiones activas, etapas entregadas, notificaciones sin leer y semilleros de la persona; se ocultan los datos ficticios, que continúan en la portada pública identificada como demostración. Se añadieron enlaces de clasificación y notificaciones.
- `./mvnw -q verify`: 23 pruebas aprobadas en H2; se ajustó el doble de prueba del controlador en la verificación intermedia. La instancia abierta todavía ejecuta el JAR anterior.
- El propietario aprobó medir competencias mediante notas por criterio y rangos Explorador 0, Investigador 500, Especialista 1500 y Mentor 3000 XP. Pendiente implementarlos junto con el resto de fases y verificar PostgreSQL.

## 2026-11-10 — Rangos personales

- Se calcula XP acumulado de Misiones elegibles y se muestra en el panel con los rangos aprobados por el propietario: Explorador 0, Investigador 500, Especialista 1500, Mentor 3000. Cada Misión se cuenta una vez en el total personal.
- `./mvnw -q verify`: 23 pruebas aprobadas en H2. Se corrigió durante la verificación una referencia en la portada pública. Pendiente probar PostgreSQL, concurrencia y la cobertura completa de umbrales.
- La instancia en ejecución conserva la versión anterior. Pendiente implementar competencias por criterio, insignias y resto de fases.

## 2026-11-10 — Competencias por criterio

- Se incorporó creación de competencias por semillero, vínculo a criterios antes de asignar Misiones, nota obligatoria de criterios vinculados y progreso personal con el promedio de las últimas evaluaciones. Menos de 3,0 se señala como competencia por fortalecer. Las correcciones conservan sus puntajes anteriores.
- Archivos: módulo `progress`, servicios y formularios de evaluación, navegación, prueba de integración y documentación. La vinculación exige autorización y mismo semillero; se bloquea tras asignar. Los puntajes de equipo son comunes a participantes, sin ajustes individuales aún.
- `./mvnw -q verify`: 24 pruebas aprobadas en H2, incluida creación, aislamiento, notas obligatorias, corrección e historial. PostgreSQL y concurrencia real no probados.
- Pendientes: reportes docentes, alertas persistentes y resto de fases. La instancia abierta sigue usando el JAR anterior.

## 2026-11-10 — Progreso docente por competencias

- Se añadió una vista docente de promedios por estudiante, competencia y semillero, con aviso por debajo de 3,0. El acceso se limita a semilleros autorizados y a la academia del docente; muestra hasta 500 resultados.
- `./mvnw -q verify`: 24 pruebas aprobadas en H2, incluyendo acceso permitido y denegado a la vista. PostgreSQL y concurrencia real no probados.
- Pendientes: paginación, estadísticas agregadas, alertas persistentes y ajustes individuales grupales; el usuario recibió dos preguntas sobre estos ajustes.

## 2026-11-10 — Edición básica de usuarios

- Los administradores pueden editar nombre y apellido dentro de su academia. Se auditan tanto la creación como la edición, sin guardar contraseñas. No se modificaron roles, estados ni sesiones.
- `./mvnw -q verify`: 25 pruebas aprobadas en H2, incluida la protección frente a edición de usuarios de otra academia. PostgreSQL y concurrencia real pendientes.
- Pendientes: gestión segura de roles y estados, revocación de sesiones y demás fases. La instancia abierta no se ha reiniciado.

## 2026-11-10 — Ajustes individuales en Misiones grupales

- Se implementaron ajustes positivos o negativos por integrante y etapa, con justificación obligatoria y nota resultante entre 0 y 5. Las correcciones copian los ajustes anteriores y permiten cambiarlos o borrarlos; se conserva el historial. Los ajustes afectan al promedio y XP individuales, no a las notas de criterios de competencias. La persona solo ve su propia justificación.
- `./mvnw -q verify`: 25 pruebas aprobadas en H2, con dos integrantes, aislamiento, límites, copia de ajustes y XP. PostgreSQL y concurrencia real pendientes.
- La propuesta inicial de revocación de sesiones se retiró al fallar las pruebas de integración; no se habilitó desactivación de cuentas ni se afirma esa protección. Se requiere rediseñarla y probarla. La aplicación abierta aún usa un JAR anterior.

## 2026-11-10 — Cuentas y sesiones locales

- Se rehízo la protección con comprobación de sesiones creadas por formulario y se permite activar/desactivar cuentas de la academia. El último administrador activo no puede desactivarse. Las operaciones se auditan; un cambio de estado, roles o hash de contraseña invalida la sesión local en la siguiente petición.
- `./mvnw -q verify`: 28 pruebas aprobadas en H2, incluidos inicios reales, invalidación por estado y roles, reactivación y último administrador. No se verificaron PostgreSQL ni carreras reales; la autenticación y revocación requieren revisión adicional antes de producción. El control de sesión cubre el inicio local actual; futuros mecanismos deben integrarse.
- Pendiente gestionar roles y verificar concurrencia; la instancia abierta todavía usa la versión anterior.

## 2026-11-10 — Gestión de roles

- Se pueden cambiar roles de usuarios de la misma academia, conservando roles no modificados y protegiendo el último administrador activo. Se impide retirar el rol estudiante con Misiones o membresías activas; faltan herramientas completas para cerrar todos esos vínculos. Las sesiones locales se invalidan en la siguiente petición tras cambiar roles.
- El hash usado por la sesión procede de la credencial autenticada y no de una lectura posterior a la autenticación. El control cubre el mecanismo de inicio local actual, no futuros proveedores.
- `./mvnw -q verify`: 29 pruebas aprobadas en H2, incluidos cambios de rol, último administrador e invalidación de sesión iniciada realmente. PostgreSQL, concurrencia real y revisión integral de seguridad pendientes.

## 2026-11-10 — Clasificación global administrativa

- Se añadió clasificación global de la academia para administradores. Suma XP vigentes y cuenta una Misión una sola vez por persona; el acceso de estudiantes permanece limitado a clasificaciones de sus semilleros.
- `./mvnw -q verify`: 29 pruebas aprobadas en H2, incluida restricción de acceso y cálculo global. PostgreSQL y pruebas de volumen pendientes.
- Pendientes: insignias, libro de XP, adjuntos y demás fases.

## 2026-11-10 — Avisos de autorización tardía

- Se notifican a los participantes la concesión y revocación de autorizaciones tardías, en la misma transacción y sin incluir contenido sensible. `./mvnw -q verify`: 29 pruebas aprobadas en H2.

## 2026-11-10 — Adjuntos locales

- Se admite un adjunto opcional de hasta 10 MB por versión: PDF, PNG, JPG, GIF, WebP o ZIP. Se verifican extensión y firma básica, se usan nombres internos aleatorios y descarga como archivo con control de permisos. La información y archivos se guardan en `PROYECTO/data/adjuntos`, que las copias de código excluyen. No existe antivirus ni análisis integral del contenido; una caída puede dejar archivos huérfanos.
- `./mvnw -q verify`: 30 pruebas aprobadas en H2, incluidos formato y tamaño rechazados, descarga autorizada y denegada, y enlace docente. PostgreSQL, concurrencia y recuperación ante caída no probados. La aplicación anterior sigue abierta sin reiniciar.
- Aprobadas insignias: primera y cinco Misiones conservadas; nota 5,0 solo mientras al menos una Misión mantenga esa nota. Pendiente implementarlas.

## 2026-11-10 — Insignias y migración V3

- Se implementaron Primera Misión, Cinco Misiones y Nota perfecta. Las dos primeras se conservan; Nota perfecta se revoca cuando ninguna Misión elegible conserva 5,0 y puede reactivarse. Cada cambio queda registrado. El panel personal reconcilia insignias anteriores y las evaluaciones nuevas las actualizan.
- Flyway V3 añade revocación e historial, probado solo en H2 de pruebas. La base local aún no recibió V3; antes de reiniciar la aplicación debe guardarse una copia privada consistente de `data/` con la instancia detenida.
- `./mvnw -q verify`: 30 pruebas aprobadas en H2, incluyendo concesión, revocación y conservación de insignias; faltan PostgreSQL, concurrencia real y recuperación ante fallos.

## 2026-11-10 — Boletín PDF personal

- Se añadió descarga personal autenticada con Misiones activas/completadas (hasta 100), notas, competencias, XP e insignias. Se usa PDFBox 3.0.8 y Liberation Sans con su licencia incluidos en el proyecto.
- `./mvnw -q verify`: 30 pruebas aprobadas en H2, incluyendo contenido extraído del PDF y acceso autenticado. Una página con datos ficticios se renderizó y revisó visualmente; no se probó volumen alto ni PostgreSQL.
- Pendientes: paginación de datos académicos, boletín docente y resto de fases.

## 2026-11-10 — Chat por Tripulación y privado

- Se añadieron canales por Tripulación y conversaciones privadas entre estudiantes y profesores que comparten semillero activo. Los mensajes se conservan, se muestran los últimos 100 y se actualizan con WebSocket del mismo origen; se comprueban sesión, cuenta y permisos antes de enviar y difundir. El contenido se muestra como texto.
- La migración V4 crea relaciones de chat; solo probada en H2. Antes de aplicar V3/V4 en la base local se debe detener la aplicación y hacer una copia privada consistente de `data/`.
- `./mvnw -q verify`: 32 pruebas aprobadas en H2, incluidas conexiones reales WebSocket, rechazo de otro origen, aislamiento, salida de sesión y pérdida de permisos. Pendientes pruebas reales de concurrencia, abuso, paginación, PostgreSQL y revisión de seguridad.

## 2026-11-10 — Copia de datos y migración local

- Se detuvo la instancia abierta y se creó la copia privada `respaldos/datos-antes-migracion-20261110-212849.tar.gz` de `data/`, con SHA-256 comprobado; no se eliminaron respaldos anteriores.
- La versión actual se inició en localhost; Flyway aplicó V3 y V4 a la base H2 local. Se comprobó salud UP, inicio HTTP 200 y protección del chat con redirección a inicio de sesión. La aplicación continúa ejecutándose.
- El código permanece sin publicar y sin commit. PostgreSQL y las demás pruebas finales siguen pendientes.

## 2026-11-10 — Dependencia PDF y compatibilidad de registro

- Se excluyó la copia transitiva de Commons Logging de PDFBox para utilizar el puente de registro de Spring y evitar duplicación. `./mvnw -q verify`: 32 pruebas aprobadas en H2. La aplicación abierta aún utiliza el paquete anterior a esta corrección menor.

## 2026-11-10 — Misiones interdisciplinarias y simulador

- Una Misión compartida admite asignaciones a estudiantes o Tripulaciones de sus semilleros. V5 vincula cada asignación a su semillero de destino; evaluaciones, adjuntos, autorizaciones, XP y progreso respetan ese límite. La migración desde V4 con una asignación existente se probó en H2; no se aplicó aún a la base local ni a PostgreSQL.
- Se añadió simulador local de circuito ideal con batería, resistencia, LED e interruptor, cálculo de corriente y potencia y comparación con límites elegidos.
- `./mvnw -q verify`: 34 pruebas aprobadas en H2, incluyendo aislamiento entre docentes y migración con datos existentes. `node scripts/test-circuitos.cjs`: comprobaciones de fórmulas aprobadas. Faltan PostgreSQL, pruebas de concurrencia y revisión de accesibilidad.
- Antes de aplicar V5 detener la instancia y guardar copia privada consistente de `data/`; la instancia abierta aún ejecuta V4.

## 2026-11-10 — Cobertura adicional de auditoría

- Se incorporó auditoría transaccional para entregas, asignaciones a semilleros, creación de Tripulaciones y apertura/cierre de membresías, sin incluir el contenido de entregas. `./mvnw -q verify`: 34 pruebas aprobadas en H2. Sigue pendiente auditar otras operaciones y fallos y revisar concurrencia.

## 2026-11-10 — Cierre manual y concurrencia H2

- Se añadió cierre manual de cada asignación por docente autorizado, condicionado a etapas obligatorias entregadas y evaluación de la versión final más reciente. La consulta histórica permanece accesible, se impiden nuevas versiones y se genera auditoría y notificación.
- `./mvnw -q verify`: 35 pruebas aprobadas en H2. Se probaron dos altas simultáneas a un semillero y dos evaluaciones concurrentes, además de restricciones de cierre y acceso interdisciplinario. Otros escenarios y PostgreSQL siguen pendientes. V5 aún no se ha aplicado a la base local.

## 2026-11-10 — Alertas persistentes de competencias

- Una competencia por debajo de 3,0 genera una alerta abierta y notificación; cuando el promedio mejora se resuelve y notifica, conservando historial. Se reconcilia al evaluar y al consultar el progreso, incluida información anterior. Se serializa por usuario.
- `./mvnw -q verify`: 35 pruebas aprobadas en H2, incluida creación, resolución y notificación. Falta probar concurrencia específica de alertas y PostgreSQL.

## 2026-11-10 — Migración local V5

- Se detuvo la instancia y se creó y verificó `respaldos/datos-antes-v5-20261110-214421.tar.gz`, copia privada de datos. Flyway aplicó V5 correctamente en H2 local y la aplicación se reinició.
- Salud UP e inicio HTTP 200; simulador y chat sin sesión redirigen al inicio de sesión. La aplicación está ejecutándose en localhost. PostgreSQL y pruebas finales aún pendientes.

## 2026-11-10 — Comprobación visual aislada

- En una instancia temporal con datos ficticios y base en memoria se inició sesión y se revisó el simulador en navegador. Para 5 V, 220 Ω y LED de 2 V mostró 13,64 mA; con 10 Ω mostró sobrecarga y al abrir el interruptor mostró 0 mA. A 390 px no se detectó desbordamiento horizontal en el documento. La instancia de prueba se cerró sin modificar la base del usuario.
- No se completó una auditoría general de accesibilidad ni de todos los tamaños de pantalla.

## 2026-11-10 — Auditoría de semilleros

- La creación de semilleros se movió a un servicio transaccional que registra auditoría sin información sensible. `./mvnw -q verify`: 35 pruebas aprobadas en H2.

## 2026-11-10 — Acceso y consultas del chat

- Se filtran canales y contactos directamente en consultas autorizadas, evitando una consulta por cada candidato. Tanto los canales como las conversaciones privadas requieren semillero activo; las pruebas verifican visibilidad y pérdida de acceso.
- `./mvnw -q verify`: 35 pruebas aprobadas en H2. Faltan pruebas de volumen, concurrencia y PostgreSQL; la instancia abierta usa el paquete anterior a este ajuste.

## 2026-11-10 — Perfil y cambio de contraseña

- El perfil permite consultar roles, editar nombre y cambiar la contraseña verificando la actual. Se audita sin guardar contraseñas; la sesión presente se cierra y las demás sesiones locales se invalidan al usarse.
- `./mvnw -q verify`: 36 pruebas aprobadas en H2, con contraseña incorrecta, cierre de sesiones e inicio con credencial nueva. Pendientes revisión integral de seguridad y PostgreSQL.

## 2026-11-10 — Boletines para docentes

- Docentes con semillero activo compartido y administradores de la misma academia pueden descargar el boletín completo de un estudiante activo; el acceso se comprueba en servidor y se audita. La lista muestra hasta 500 estudiantes.
- `./mvnw -q verify`: 36 pruebas aprobadas en H2, incluidas denegación a docente de otro semillero y descarga autorizada de docente y administrador. PostgreSQL y pruebas de volumen pendientes.

## 2026-11-10 — Boletín completo y paginación PDF

- Se eliminó el límite de 100 Misiones del boletín; la vista de Misiones mantiene un límite de 100 y lo informa. `./mvnw -q verify`: 37 pruebas aprobadas en H2, incluido un boletín de 101 Misiones y varias páginas. La carga de datos grandes y PostgreSQL siguen pendientes.

## 2026-11-10 — Paginación de Misiones e historial de chat

- Mis Misiones se pagina de 50 en 50 y el historial de chat de 100 en 100; las páginas antiguas quedan en modo lectura y se puede volver a la conversación en vivo. El boletín mantiene todas las Misiones activas o completadas.
- `./mvnw -q verify`: 37 pruebas aprobadas en H2, incluida navegación de 101 Misiones y 101 mensajes, conservación del contenido y permisos. Faltan paginación de otras listas, pruebas de volumen y PostgreSQL.

## 2026-11-10 — Cierre de sesión solicitado

- A petición del propietario se detiene el trabajo. Se inició el último paquete local en el puerto 8080 y la comprobación de salud respondió UP; se abrió en Chromium. Última verificación completa: `./mvnw -q verify`, 37 pruebas aprobadas en H2; `node scripts/test-circuitos.cjs` aprobado.
- No se declara terminado ni listo para producción: quedan revisión integral de seguridad y accesibilidad, PostgreSQL, pruebas de rendimiento y concurrencia adicionales, paginación de otras listas, control de abuso del chat, auditoría y notificaciones completas y mantenimiento operativo. Las copias privadas y el código activo se conservan; no se hizo commit ni publicación.

## 2026-11-11 — Preparación de continuidad con OpenCode

- Se compararon los árboles principales sin modificarlos: la copia CHANGE contiene cambios visuales, Astronomía, cursos, pruebas y migración V6 que aún no están integrados en el proyecto principal. No se verificó la funcionalidad ni se ejecutaron sus pruebas en este bloque.
- Se creó `PROYECTO/docs/CONTINUAR-OPENCODE.md` con fuentes, protección de datos, orden de integración y criterios de pruebas. OpenCode 2.2.7 está instalado y su catálogo local muestra modelos gratuitos. No se cambiaron modelos ni configuraciones de cuenta y no se publicaron datos.

## 2026-11-11 — Integración del trabajo CHANGE por grupos (OpenCode)

- Bloque 1 (comparación y pruebas): `./mvnw -q verify` en el proyecto principal aprobó (línea base). La suite del árbol CHANGE nunca había corrido con Maven y no compila: paréntesis de cierre sobrante en `SecurityConfiguration.java:63` y encadenado inválido de `permissionsPolicy`/`contentSecurityPolicy` para Spring Security 6.5.11 (verificado contra `javap`). El árbol CHANGE no se modificó; el fuente correcto quedó en el proyecto principal.
- Grupo 1 — cursos: migración V6, paquete `course` (matrícula idempotente con bloqueo de fila, autorización re-verificada, progreso monotónico con evidencia 12–5000, cierre por lecciones obligatorias, auditoría), vistas `/cursos` y detalle, pruebas de migración y de flujo. Verificación: `./mvnw -q verify`, 39 pruebas aprobadas (37 + 2), Flyway V1–V6 sobre H2 en memoria.
- Grupo 2 — laboratorios y estética: `/laboratorio/astronomia` (modelo kepleriano con prueba `scripts/test-astronomia.cjs`), rediseño de `/laboratorio/circuitos` con ruta formativa, página `/login` propia, cabeceras de seguridad (CSP estricta, frame-deny, nosniff, referrer, permissions-policy) y marca CHANGE en plantillas. Verificación: 39 pruebas, scripts Node aprobados, humo con H2 en memoria: `/`, `/login`, CSS y SVG → 200; `/cursos` y laboratorios → 302 sin sesión; cabeceras presentes. Ajuste de prueba existente: `AuthenticationIntegrationTest` ahora comprueba el `aria-label` persistente del esquema (el título cambió con el rediseño); no se debilitó la verificación.
- Grupo 3 — configuración: sesión endurecida en perfil prod (cookie http-only/secure/same-site lax, 30 min) y branding de visualización en `application.yml`. `artifactId` se mantiene como `laboratorio` para no romper `run-laboratorio.sh` y `DETENER-LABORATORIO.sh`; rebranding completo diferido. Verificación: 39 pruebas.
- Documentación: README, ROADMAP, DATA-MODEL-STATUS, SESSION-STATE actualizados; se incorporaron `docs/CHANGE-LABS.md` y `docs/RUTAS-FORMATIVAS-LABORATORIOS.md`.
- Pendiente: aplicar V6 a la base local (app detenida + copia privada), PostgreSQL, conectar cursos con Misiones/XP, revisión en Chromium con datos ficticios, prueba manual integral por roles. Sin commits ni publicación. Modelos usados: gratuitos de OpenCode.

## 2026-11-11 — Revision findings: revocación de cursos, asociación dinámica y enlace docente/administrador

- Se corrigieron los hallazgos de la revisión sobre el backend de cursos (Change solicitado): (1) `CourseService.detail` y `updateLesson` exigen ahora asignación activa (`asignacion_activa`) y semillero activo, revocando acceso y escritura al estudiante cuando pierde la asignación al semillero; la matrícula y el progreso se conservan para la reactivación; `catalog` y `enroll` ya lo exigían. `catalogFor` resuelve la vista por rol: estudiante con matrículas, o docente/administrador en solo lectura (`staffCatalog`, sin matrícula ni acciones).
- `CourseCatalogSeeder` expone `link(semilleroId)` idempotente y se invoca al crear cada semillero (`SemilleroAdministration.create`), de modo que los semilleros creados después del arranque reciben los cursos asociados.
- Plantilla `cursos.html` y navegación de `index.html` actualizadas: el enlace a Cursos aparece en la navegación autenticada y la vista docente/administrador muestra el catálogo sin formularios de inscripción ni continuar.
- Pruebas añadidas: revocación al perder asignación en `CourseLearningFlowTest`; asociación dinámica e idempotencia en `CourseCatalogLinkingTest`; catálogo de solo lectura para profesor/administrador en `CourseCatalogRolesTest`; migración V5→V6 con datos existentes en `LearningCoursesV5ToV6MigrationTest` (Flyway `target(5)` → datos previos → V6, sin pérdida).
- Verificación: `./mvnw verify`: BUILD SUCCESS, 48 pruebas JUnit aprobadas en H2 (37 base + 11 nuevas/ajustadas), Flyway V1–V6 en H2 en memoria. Scripts `scripts/test-circuitos.cjs` y `scripts/test-astronomia.cjs` sin cambios.
- Verificación en Chromium (Playwright + Chromium headless contra la app local con V6 aplicada): F1 anónimo `/cursos` → `/login`; F2 admin `/cursos` → catálogo de solo lectura (2 cursos, sin inscripción ni continuar, aviso visible); F2 profesor `/cursos` → semillero visible (1 curso, solo lectura); F3 estudiante → catálogo, matrícula, detalle, cierre de asignación, y tras perderla catálogo vacío + detalle → 404. El POST de escritura se comprobó con CSRF.
- Base local: aplicación detenida y copia privada previa `respaldos/datos-antes-v6-20261111-145810.tar.gz` (checksum verificado); V6 aplicada (`now at version v6`); estado UP. Se crearon datos de desarrollo locales (admin, profesor y estudiante de prueba) vía UI.
- Pendiente: prueba manual integral por roles, PostgreSQL, concurrencia real, accesibilidad, PostgreSQL, libro de movimientos XP, revisiones restantes. Sin commits ni publicación.

## 2026-11-11 — Revisión y arranque local

- Se inició el ejecutable integrado en localhost:8080 y se abrió Chromium; salud UP. Flyway validó seis migraciones y confirmó que la base local ya estaba en V6, sin migraciones nuevas. La portada pública se revisó en navegador: muestra contenido de demostración. La revisión autenticada queda pendiente del acceso del propietario.
- Se identificaron pendientes prioritarios: controles aún visibles tras completar cursos, vista de cursos para personas con varios roles, revisión integral de flujos y accesibilidad, y conexión de cursos con Misiones y gamificación. No se hicieron cambios de código ni se publicaron datos.

## 2026-11-11 — Cursos completados y roles simultáneos

- Se añadieron vistas académicas de consulta para usuarios con roles de estudiante y docente/administrador simultáneos, sin autorizar la matrícula fuera de su asignación activa. Los cursos completados muestran sus evidencias sin controles de edición.
- `./mvnw -q verify`: 49 pruebas aprobadas en H2, con comprobaciones de roles simultáneos y visualización del curso completado. Simuladores de circuitos y astronomía: pruebas Node aprobadas. Empaquetado Maven correcto.
- Al reconstruir el JAR mientras la instancia previa estaba abierta, el cierre de esa instancia emitió un error de carga de clases; la nueva instancia arrancó y validó V6 sin migraciones. No se ha realizado una auditoría completa de los datos. En adelante detener la instancia antes de reconstruir el mismo JAR.
- Pendientes: flujos completos por roles en Chromium, accesibilidad, integración de cursos con Misiones y XP, seguridad integral, concurrencia y PostgreSQL. No afirmar que el producto esté terminado.

## 2026-11-11 — Portada, acceso y actividades de laboratorio

- Nueva portada pública local, ajuste de textos del acceso, doce preguntas con retroalimentación (seis por laboratorio) y corrección de variación de velocidad en la animación orbital. Los cuestionarios no guardan respuestas ni otorgan notas o XP.
- `./mvnw -q test`: 49 pruebas aprobadas. Pruebas Node de circuitos y astronomía aprobadas. En navegador se comprobó portada, acceso local y una actividad correcta de cada laboratorio; no se probaron exhaustivamente todos los estados ni tamaños de pantalla.
- Vista previa aislada en puerto 8092 con base en memoria; no se detuvieron las instancias existentes. Continúan pendientes simulaciones para otros temas, conexión pedagógica de actividades con cursos y Misiones, accesibilidad completa y revisión integral del backend. Sin publicación ni commits.

## 2026-11-11 — Dos estaciones interactivas

- Se añadieron una estación de robot con sensor y umbral y otra de retardo de comunicación por luz, con controles, resultados y límites de los modelos. No persisten resultados.
- Pasaron los scripts Node de estaciones, circuitos y astronomía. En navegador aislado se comprobó el cambio del robot a detenido con obstáculo cercano y el resultado inicial de 1 UA; el clic automatizado del selector de distancia no se pudo verificar. Maven no se volvió a ejecutar para este bloque de recursos.
- Vista aislada en puerto 8093, con datos temporales; no se alteraron las sesiones anteriores. Pendientes persistencia, más simulaciones y pruebas integrales.

## 2026-11-11 — Ocho clases por ruta y enlaces a laboratorios

- V7 incorpora dos lecciones por curso y deja la lección de proyecto al final conservando identificadores y progreso histórico. Matrículas previas reciben registros faltantes al consultar/actualizar; matrículas completadas mantienen su estado. Nuevas lecciones obligatorias en matrículas activas. No se aplicó V7 a la base persistente.
- Ocho tarjetas y actividades locales por laboratorio, enlaces desde lecciones a su laboratorio, animaciones discretas respetando movimiento reducido. Pendiente editor de circuitos libres, constructor didáctico de cohetes y conexión real entre resultados y progreso.
- Pruebas dirigidas de migración V5/V6/V7 y cursos aprobadas después del ajuste final; suite completa previa aprobó 49 pruebas, y scripts Node de simuladores pasaron. En navegador aislado se observaron ocho lecciones por curso. Vista temporal en puerto 8094. PostgreSQL no probado.

## 2026-11-11 — Taller de redes resistivas y cohete de papel

- Se incorporó un taller DC de hasta ocho resistencias entre cuatro nodos, con cálculo de tensión, corriente y potencia; ejemplos serie/paralelo y manejo de conexiones abiertas/flotantes. Es un modelo ideal limitado a resistencias.
- Se añadió un taller visual de cinco pasos para diseñar un cohete de papel no propulsado; no modela estabilidad ni vuelo y no persiste diseños.
- 49 pruebas Maven y scripts Node de redes, estaciones y simuladores anteriores aprobados. En navegador se comprobó el ejemplo paralelo (37,88 mA a 5 V) y el avance del taller de cohete. Se corrigió la numeración de la última clase de Astronomía. Vista aislada en puerto 8095. No se aplicó V7 a la base persistente ni se hizo publicación o commit.

## 2026-11-11 — Organización y material de las rutas

- Se reorganizaron las dos páginas colocando las ocho clases antes de las estaciones, con navegación a clases y prácticas. Cada clase incluye idea clave, práctica, actividad para bitácora, enlace a una estación y pregunta local. Se actualizaron estilos y guía de rutas.
- Scripts Node de redes, estaciones, circuitos y astronomía aprobados. En navegador aislado se revisó el orden de ambas rutas y se abrió una clase de electrónica. No se ejecutó Maven por cambios de presentación/contenido. Vista temporal en puerto 8096; sin cambios en la base persistente ni publicación.
- Pendientes: evaluación y persistencia de prácticas, mayor profundidad pedagógica y simuladores, verificación integral de accesibilidad y flujos.

## 2026-11-11 — Flujo de evidencia y actualización local V7

- Curso: validación de evidencias devuelve mensaje comprensible y conserva el texto, límite de 5000 caracteres en servidor y enlace de cada lección a su clase específica. `./mvnw -q test`: 49 pruebas aprobadas; scripts Node de simuladores aprobados.
- Base: app principal detenida, copia privada `respaldos/datos-antes-v7-20261111-162554.tar.gz` verificada. V7 probada en copia: 16 lecciones, 14 usuarios, 3 matrículas y 18 avances; las últimas tres cantidades coinciden con la base anterior. Paquete actual con V7 arrancado contra base principal en 8080; salud UP. PostgreSQL no probado.
- Incidente: el JAR fue reconstruido accidentalmente mientras la instancia anterior lo utilizaba; durante su cierre ocurrió error de carga de clases. La copia privada se hizo después del cierre; la base copiada arrancó y migró, pero no se auditó íntegramente. Detener los procesos antes de reconstruir su JAR compartido.
- Siguen pendientes las funciones y pruebas documentadas; no se declara el producto terminado, ni se publicaron cambios.

## 2026-11-11 — Diseños guardados y conexión con evidencia

- V8 crea almacenamiento privado por usuario para un circuito y un cohete. Se validan datos y límites; cambios requieren CSRF y generan auditoría sin contenido. Los talleres permiten guardar/recuperar; una lección puede importar el resumen a su evidencia sin completarse automáticamente. Guardar reemplaza el diseño anterior del mismo tipo.
- `./mvnw -q test`: 50 pruebas aprobadas. Prueba Node de importación y límite aprobada. Navegador aislado: guardar/limpiar/recuperar circuito y guardar cohete. Pruebas MVC cubren CSRF, aislamiento y rechazo de datos inválidos.
- Base: instancia principal detenida correctamente; copia privada `respaldos/datos-antes-v8-20261111-163504.tar.gz` verificada. V8 migró una copia V7 conservando 14 usuarios, 3 matrículas y 18 avances; después se arrancó el paquete actualizado contra la base principal en 8080. Sin publicación ni commit. PostgreSQL no probado.

## 2026-11-11 — Seguimiento docente de cursos

- Se agregó consulta de lecciones y evidencia por curso y semillero para docentes asignados y administradores de la academia, con comprobación de identidad y ámbito en servidor. La vista está limitada a 4000 filas y lo indica al alcanzar el límite; falta paginación.
- Pruebas verifican contenido y denegación a usuarios ajenos; `./mvnw -q test`: 51 pruebas aprobadas. Prueba de importación de diseño aprobada. La instancia principal se reinició con paquete construido en carpeta separada para no sobrescribir un JAR en uso; salud UP. No se hicieron cambios de esquema en este bloque ni publicación.

## 2026-11-11 — Paginación de progreso docente

- La consulta docente se pagina de 100 filas en 100, sin el corte fijo anterior; conserva el ámbito de autorización en cada página. Prueba dirigida con 104 lecciones y permisos aprobada. La última suite completa previa tuvo 51 pruebas aprobadas. Se construyó el ejecutable en una carpeta aislada y se reinició la instancia principal sin sobrescribir un JAR en uso.

## 2026-11-11 — Revisión de seguridad y arranque

- Se comprobó que la vista docente escapa evidencia HTML, además de sus permisos; se hizo revisión estática básica de etiquetas e ID en plantillas, sin pretender una auditoría de accesibilidad completa.
- La mesa de circuitos distingue circuito abierto de uno conectado con 0 V y rechaza entrada de voltaje vacía; el iniciador evita reconstruir un JAR ya en uso o si el servicio ya responde.
- `./mvnw -q test`: 51 pruebas aprobadas; cinco scripts Node y sintaxis del iniciador aprobados. Se compiló en carpeta separada y se arrancó 8080. No hay PostgreSQL respondiendo localmente; no se probó. Quedan pruebas integrales y trabajo de producto. Sin publicación ni commit.

## 2026-11-11 — Entrega de continuidad y limpieza

- A petición del propietario se preparan un ZIP de fuentes y un contexto para OpenCode, excluyendo base, adjuntos, compilados, Git y secretos. El proyecto original y respaldos privados se conservan.
- Se cerraron las instancias de vistas temporales autorizadas por el propietario y se eliminaron sus carpetas temporales de código sin base de datos. Se conservaron las copias temporales que sí contienen bases, la base principal y la instancia principal en localhost:8080.

## 2026-11-11 — Recorrido integral automatizado por roles (vista aislada)

- Objetivo (pendiente nº1 de la pauta): recorrer con datos completamente ficticios los flujos de administración, docencia y estudiantes, incluidos roles simultáneos, Misiones individuales, grupales e interdisciplinarias, cierres/reactivaciones, cursos, competencias, chat, boletines PDF, adjuntos y evidencias; y corregir o confirmar fallos.
- Método: instancia **aislada** con base H2 temporal en `/home/jose/.local/share/academia-preview/20261111-1705` (JAR construido del código actual, perfil `dev`, puerto 8097, admin bootstrap por variables de entorno; la instancia principal de 8080 y su base privada `data/` no se tocaron). Recorridos en Chromium/Playwright headless (`recorrido-*.cjs`), una pieza por bloque, con verificación del resultado visible. La instancia temporal se cerró al terminar; la carpeta conserva base, logs y scripts (solo datos ficticios; `admin-password.txt` local y sin reutilización).
- Verificado OK: alta de 3 usuarios y del multirol ESTUDIANTE+PROFESOR; asignaciones a semillero; Tripulación Nova; creación de Misiones individual, grupal e interdisciplinaria (la interdisciplinaria se oculta/rechaza sin autorización en ambos semilleros, según corresponde); etapa obligatoria antes de asignar; entregas con PNG adjunto; evaluación por etapas; promedio y XP (estudiante1: 100→190→270 XP; estudiante2: 50 XP con ajuste individual de −1,5 y justificación que se conserva al corregir); insignias «Nota perfecta» y «Primera Misión»; clasificación de Robótica; competencias de principio a fin (vinculación antes de asignar, evaluación 2,5 → alerta docente, corrección 4,5 → alerta resuelta); chat por WebSocket (canal Nova en ambos sentidos, privado estudiante↔profesor, escapado HTML verificado, exclusión de pares estudiante↔estudiante, exclusión de estudiante como par privado); notificaciones de asignación y evaluación; boletines PDF personal y docente (`%PDF`, texto extraído correctamente) con `DESCARGAR_BOLETIN` auditado en la vista administrativa; cursos (inscripción, evidencia de lección, catálogo dual para usuario con dos roles, consulta docente de progreso); revocación de cursos al perder asignación activa (detalle 404, catálogo solo con semilleros activos, evidencia conservada al reactivar); cierre de Misión con etapas entregadas y evaluadas (sin formulario de envío, historial y nota visibles); reactivación/reincorporación por el administrador. El rechazo del cierre de asignación con membresía activa («Primero cierra las membresías…») funciona como está documentado.
- Fallos encontrados: ninguno funcional en la aplicación. Los dos «FAIL» iniciales fueron chequeos de mi propio guion (subcadenas del menú de navegación y regex laxo en la vista de asignaciones), corregidos y reforzados para futuras comprobaciones.
- Limitaciones: no se probó PostgreSQL real; el recorrido manual no sustituye la revisión de seguridad integral ni las pruebas de concurrencia en producción; no se ejerció el PDF más allá de estructura y texto; las credenciales de cuentas de desarrollo del equipo principal no están disponibles y la base privada no se inspeccionó.
- Línea base antes del recorrido: `./mvnw -q test` → 51 pruebas, 0 fallos/0 errores; app principal en 8080 responde 200.
- Siguiente paso sugerido: resolver los pendientes de la pauta que no requieren credenciales (libro de movimientos XP, auditoría/notificaciones restantes, límites de abuso del chat, pruebas concurrentes nuevas) y PostgreSQL cuando haya instancia disponible; solicitar al propietario credenciales de cuentas de prueba solo si se desea un recorrido manual externo. La copia principal y `respaldos/` se conservan; no se hizo commit ni publicación.

## 2026-11-11 — CHANGE // SPACE-LAB OS: identidad Space-Lab, registro acotado y refuerzos de simulador

- Objetivo: aplicar las tres decisiones recomendadas por el propietario (registro público acotado con aprobación administrativa; POO de dominio con tabla única; entrega por bloques con previews aisladas) y avanzar las fases restantes con autonomía.
- Identidad visual SPACE-LAB: nuevo `space-lab.css` + `js/os-terminal.js` (barra de estado inferior estilo terminal); 32 plantillas retocadas y portada pública, login y registro reescritos (split-screen con órbita animada; cian #22d3ee y naranja #ff9e2c; glassmorphism con backdrop-blur; mono+sans; grillas sutiles). El shell autenticado muestra solo datos reales.
- Registro "Solicitar acceso" (`/registro` → `solicitud_cuenta`, V9) + bandeja admin `/administracion/solicitudes`: aprobar crea la cuenta con **contraseña temporal de un único uso** (mostrada una sola vez, BCrypt, 14 caracteres inequívocos) con el rol solicitado (ESTUDIANTE o PROFESOR); rechazar no crea cuenta. Los endpoints públicos nunca revelan qué correos existen. `GET /registro` queda pública; `/administracion/solicitudes` exige ADMINISTRADOR; CSRF en todo.
- Seguridad: `AttemptLimitFilter` (POST /login máx. 20/IP/10 min → 429 + Retry-After); cabeceras CSP estrictas ya vigentes; BCrypt; cookies prod endurecidas; CSP frame-deny, nosniff, referrer y permissions-policy. `SECURITY.md` creado con análisis OWASP Top 10 (2021) y limitaciones conocidas.
- POO: jerarquía sellada `domain/Usuario` → `Estudiante` · `Profesor` · `Administrador` + `Rol`/` EstadoUsuario`/`SolicitudCuenta`; capa `repository/UsuarioRepository` con implementación `JdbcUsuarioRepository` (consultas parametrizadas) adoptada por el flujo de solicitudes; `docs/ARQUITECTURA.md` documenta el patrón incremental.
- Simuladores: **Lanzamiento educativo** (TWR, tiempo de quemado, altura máxima, Δv Tsiolkovsky; modelo verificado con `scripts/test-lanzamiento.cjs`); la mesa DC añade **multímetro en vivo** (V, mA, W) con recálculo en tiempo real. Las 8 clases por ruta ya existían (V7); el curso enlaza a las clases de laboratorio.
- Pruebas: `./mvnw -q test` → **56 pruebas, 0 fallos/0 errores** (incluye 5 nuevas de solicitudes). Scripts Node: los 6 aprobados. UI verificada en Chromium (capturas): portada, login, registro, panel, cursos, ambos laboratorios, chat, bandeja admin y flujo registro→aprobación→login completo.
- Datos: V9 aplicada a la base local tras copia privada verificada `respaldos/datos-antes-v9-20261111-175009.tar.gz` (ensayada sobre copia en H2 primero); la instancia principal 8080 arrancó con el paquete del proyecto y levanta V9, estado UP, sin errores. La vista aislada 8097 (datos ficticios) sigue UP para revisión; acceso por usuarios de prueba `@example.test` (sin datos reales).
- Hallazgos menores: uno de los tests MVC verificaba marca HTML antigua del XP (`>80</strong> XP`) y se actualizó a la nueva estructura sin debilitar cobertura; dos falsos-positivos de mis guiones de verificación (subcadenas del nav) se corrigieron.
- Sin commit ni push; no se exponen secretos ni `data/`; la instancia de 8080 usa el código actual. Pendientes siguen abiertos (PostgreSQL real, concurrencia bajo carga, accesibilidad integral, límites de abuso del chat, libro de XP, revisión externa).

## 2026-11-11 — Libro de XP, anti-duplicado de chat y entrega empaquetada «CHANGE // SPACE-LAB OS»

- Libro de movimientos XP (V10): tabla `xp_movimiento` + `XpMovementService`; cada evaluación o corrección recalcula la contribución de la Misión (con ajustes individuales por integrante) y escribe el delta en el libro; visibilidad en `/mi-progreso` con columna de saldo. Tests nuevos: `XpMovementLedgerTest` (evaluación +100, corrección −50, ajuste grupal individual estudiante2 a 50 XP vs 80 de estudiante1).
- Control de abuso del chat: mensajes idénticos del mismo autor en la misma sala dentro de 10 s se rechazan (`IllegalArgumentException`); prueba `duplicateMessageIsRejectedWithinTenSeconds` añadida.
- Suite completa tras ambos cambios: **58 pruebas, 0 fallos/0 errores**.
- Nuevo espacio de entrega en `/home/jose/Projects/CHANGE-SPACE-LAB/`: código fuente completo (excluyendo `data/`, `target/` y `.git`), docs actualizadas (README con instalador/V10/libro XP), `install-space-lab.sh` (instala dependencias del sistema, compila con la suite completa, crea `.env.local` con credenciales locales o generadas, lanzador CLI `change-space-lab`, entrada `.desktop`), `uninstall-space-lab.sh` (nunca borra la carpeta ni `data/`).
- V10 y libro de XP verificados; anti-duplicado de chat probado.

## 2026-11-11 — Instalación y arranque verificado de CHANGE // SPACE-LAB OS (empaquetado)

- La instalación se ejecutó de verdad en este equipo: Java 21 detectado, compilación con suite completa en VERDE (59 pruebas, 0 fallos/0 errores; Flyway aplicó V1–V10 sobre base dev limpia), lanzador `change-space-lab` y entrada de escritorio `.desktop` creados.
- Puerto 8090: health UP; /, /login, /registro y /demostracion/semilleros responden 200; admin de arranque local (`admin@espacio-lab.local`, contraseña temporal mostrada una vez) inicia sesión y ve su panel. El lanzador crea `data/` y el log de aplicación.
- Detalles corregidos en la marcha: el lanzador no creaba `data/` al redirigir `lanzador.log` (corregido en el instalador y en la copia instalada en `~/.local/bin/change-space-lab`); test `AuthenticationIntegrationTest` actualizado a la nueva marca HTML del XP.
- La base privada de 8080 y la carpeta original quedan intactas; la entrega empaquetada vive en `/home/jose/Projects/CHANGE-SPACE-LAB/` con toda la documentación (README/SECURITY/ARQUITECTURA) sincronizada. Sin commit ni publicación (pendiente de autorización explícita para git/push).

## 2026-11-11 — Boletín de marca, laboratorios maduros, endurecimiento y entrega re-verificada

- Boletín PDF reescrito con identidad SPACE-LAB: banda navy, nombre/fecha de generación, bloque de XP (naranja), tabla de Misiones con nota, competencias con "por fortalecer", chips de insignia, footer con paginación. Contenido de texto verificado en las pruebas existentes ("Misión competencia"/"Diseño técnico"/"80 XP"). El PDF es de solo lectura y auditado (`DESCARGAR_BOLETIN`).
- Laboratorios: nueva estación "Fases de la Luna" en Astronomía (cálculo por píxel con normal de esfera; 50,2% iluminada en la verificación visual) con pruebas Node; nueva estación PWM en Robótica (onda rectangular ideal + voltaje medio; prueba Node). Dos estaciones nuevas no persisten resultados.
- Seguridad: `denyAll()` para todo `/actuator/**` salvo health/info (el resto ni siquiera llega a Spring); nuevas pruebas `SecurityHardeningTest` (429/Retry-After, cabeceras, denyAll por rol, IDOR de matrícula ajena → 404) y barrido externo 9/9 PASS (SQLi, XSS, traversal, 302 en /.env y /actuator/env, CSRF exigido, rutas admin 403, demo pública escapada).
- PostgreSQL real: **pendiente de root** — solo cliente instalado (`postgresql-libs`), no hay servidor sin `sudo ~/bin/root-tasks.sh` (sección 4). `INICIAR-CON-POSTGRES.sh` y `docs/MIGRACION-POSTGRESQL.md` verifican la vía y el arranque; Flyway aplica V1–V10 igual en PostgreSQL (probe aplicada en cada arranque).
- Suite completa: **63 pruebas, 0 fallos/0 errores**. Instancia principal 8080 con V10 aplicada (backup verificado `respaldos/datos-antes-v10-20261111-183853.tar.gz`).
- Entrega empaquetada `/home/jose/Projects/CHANGE-SPACE-LAB/` re-sincronizada (con `install-space-lab.sh`, `uninstall-space-lab.sh`, `INICIAR-CON-POSTGRES.sh`, `docs/MIGRACION-POSTGRESQL.md`) y reiniciada en 8090 con el código nuevo (health UP; login de admin verificado). La instancia 8097 (datos ficticios) sigue activa para revisión. No fue posible verificar PostgreSQL real por falta de elevación; el proyecto queda listo para ejecutarlo con `sudo ~/bin/root-tasks.sh`.

## 2026-11-11 — Integración PostgreSQL real y servicio local

- PostgreSQL 18.6 paquete del sistema; clúster local creado (initdb, trust loopback, puerto 5433) con `systemd --user change-space-lab-postgres.service` (enabled, activo).
- Migración H2→PG ejecutada realmente: H2 copiada a /tmp; `org.h2.tools.Script` exported; Python normalizó identificadores (sin `PUBLIC`, lowercase/unquoted); `psql -v ON_ERROR_STOP=1` aplicada tras TRUNCATE; verificación tabla a tabla: 14 usuarios, 18 avances, 3 matrículas, 25 auditoría, 10 flyway.
- Verificación E2E: 8091 (PG) aprobó flujo registro→aprobación→login; 8080 y 8090 migrados y UP sobre PG. Backup H2 legado verificado SHA-256.Preview 8097 (datos ficticios operativos) detenida.
- Documentación en `MIGRACION-POSTGRESQL.md`, `README.md`, `SECURITY.md`, `SESSION-STATE.md` (sincronizado en ambas carpetas).

## 2026-11-11 — Demo en tiempo real verificada en navegador visible

- Ambas instancias (8080 y la entrega 8090) apuntan contra PG 18.6 con data real. `systemctl --user change-space-lab-postgres.service` activo. Query verificada: 14 usuarios, 16 lecciones, 18 avances, 10 migraciones, 25 auditoría, 3 matrículas.
- Se abrió navegador Chromium (`headless: false`) y se abrió la portada y el acceso en 8090 para que el propietario los viera en vivo; capturas guardadas en `docs/capturas/entrega-01..04.png`.
- Suite completa: 63 pruebas, 0 fallos/0 errores (`./mvnw -q test`).
- Script `docs/confirmar.sh` creado (ambas carpetas) que re-evidencia servicio PG + datos + salud + rutas 200/302 + suite de cada árbol.
- Navegación: laboratorios y rutas autenticadas responden 302→/login para anónimos; visibles públicamente solo portada, acceso, registro y demostración.
- La demostración completa del flujo autenticado (panel, misiones, cursos, chat, boletín PDF) requiere la cuenta del propietario; no se crearon cuentas nuevas ni credenciales. El usuario tiene delante la pantalla para comprobarlo.

## 2026-11-11 — Swagger/OpenAPI + layer REST + cuentas de desarrollo

- Requirement: Swagger funcional en entrega y prioridad backend. Implementado `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17` (compatibilidad verificada con Spring Boot 3.5.15) en ambos árboles.
- Swagger UI en `/swagger-ui.html` (→ `/swagger-ui/index.html`); `/v3/api-docs` lista 8 rutas (7 nuevas en `/api`, 1 heredada `/laboratorio/disenos/{type}`); endpoint JSON responde HTTP 200; UI carga sin errores de consola.
- New layer `/api/**` en `com.laboratorio.api.ApiRestController` con `@Tag`, `@Operation`, `@SecurityRequirement`: perfil, cursos, detalle de matrícula, misiones, notificaciones, progreso por competencias, libro de XP. Reutiliza servicios existentes (mismo ámbito/roles/seguridad que la web).
- Nueva prueba `ApiRestControllerTest` (2 tests): anónimo → 302 a `/login`; autenticado → 200 JSON con correo+nombre correctos. Suite total: **65 pruebas, 0 fallos/0 errores**.
- Cuentas de desarrollo creadas en PG (dev-only, no entran en instalador/repo): dev-admin/dev-prof/dev-estu1/dev-estu2 @espacio.local; contraseñas dev-only en `README.md`; asignaciones y períodos activos correctos con IDs explícitos.
- SecurityConfiguration pulido: rutas de Swagger (`/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`, `/v3/api-docs.yaml`) públicas; resto sigue requiriendo autenticación y `/actuator/**` sigue en `denyAll()` salvo health/info.
- Dependencia nueva documentada en README y `pom.xml`; no se promovió a producción/plugins oficial; sin commits ni push.

## 2026-11-11 — Accesibilidad WCAG parcial aplicada (skip-link y test E2E)

- Ancla skip-link inyectada en las 32 plantillas; CSS visual respetando reduced-motion; contraste verificado por JS en Chromium (todas las parejas ≥ AA; la mayoría ≥ AAA).
- Prueba `AccessibilityTest` añadida bloquea regresiones en /login, /registro y /.
- Suite total: **66 pruebas, 0 fallos/0 errores**. Entrega rsyncronizada, reempaquetada y relanzada en 8090 con smoke OK (`/`, Swagger, `/v3/api-docs` 200; rutas privadas siguen 302→/login).
- `/actuator/**` sigue denyAll salvo health/info. `dependency-check` intentado con Maven (requiere clave NVD; no disponible en este equipo — registrado como limitation).

## 2026-11-11 — Limpieza de esquema y verificación post-V11

- Bug derivado de propuestas viejas detectado y corregido con migración **V11**: las tablas legadas sin consumidores en el código (`movimiento_xp` de V2 y `rango` de V2) fueran eliminadas del esquema real en H2 y PG mediante Flyway V10→V11. Backup verificado previo: `respaldos/datos-antes-v11-20261111-203038.tar.gz` (H2) y `respaldos/pg-antes-v11-20261111-203043.dump` (PG). Flyway ahora en **V11**.
- `application-prod.yml` ahora desactiva Swagger (`springdoc.swagger-ui.enabled=false`, `springdoc.api-docs.enabled=false`); el entorno de demostración (`dev`) y CI siguen con Swagger para validación.
- Liveness del ecosistema: dos instancias UP tras la migración (8080 principal con `data/` privado, 8090 entrega empaquetada); PG correctamente standby; 66/66 tests siguen en verde.
- Documentación visual del esquema generada: `docs/VISUALIZACION-BD.md` (tablas, columnas, FK en forma de árbol, conteo de filas en este instante).
- Warnings de consola revisados: HikariPool warnings por PG_stop/SIG_TERM son normales; no hay errores funcionales.

## 2026-11-11 — Verificación final y paquete listo para subir

- Completo arreglo: README/EXECUTIVE-QUICKSTART actualizados en ambos árboles.
- Pruebas end-to-end completas sobre la app viva (público, estudiante, docente, admin): creación de Misión → asignación → entrega → evaluación → XP en PG. Datos persistidos y verificados en PG (mision, entregas, evaluaciones, xp_movimiento, notificaciones).
- Instancias UP: 8080 (principal) y 8090 (entrega). PostgreSQL :5433 accepting. Flyway V11 aplicada.
- Paquete listo para revisión pública; commit local sin push (pendiente de autorización del propietario para subir a GitHub).
