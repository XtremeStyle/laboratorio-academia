# Estado de sesión — 11 de noviembre de 2026

## Integración del trabajo CHANGE

- Se integró por grupos el trabajo de `/home/jose/Projects/Pruebas/CHANGE-backend-progreso`, verificando cada grupo con `./mvnw -q verify` y prueba de humo con H2 en memoria (la base real `data/` no se tocó).
- Grupo 1 (cursos): Flyway V6 (`curso`, `curso_semillero`, `leccion_curso`, `matricula_curso`, `progreso_leccion`), paquete `course` con matrícula idempotente, autorización re-verificada, progreso monotónico y auditoría; vistas `/cursos` y detalle; pruebas `LearningCoursesMigrationTest` y `CourseLearningFlowTest`.
- Grupo 2 (laboratorios y estética): `AstronomyController` con simulación orbital kepleriana (`/laboratorio/astronomia`), rediseño del laboratorio de circuitos con ruta formativa y kit, página de inicio de sesión propia (`/login`) con controlador, cabeceras de seguridad (CSP estricta, frame-deny, nosniff, referrer, permissions-policy), marca CHANGE en plantillas y estilos locales (`change-brand.css`, `labs-avanzados.css`, `cursos.css`, `assets/brand/change-orbit.svg`).
- Grupo 3 (configuración): endurecimiento de sesión en perfil prod (cookie http-only/secure/same-site lax, 30 min) y branding de visualización en `application.yml`. Se conserva `artifactId=laboratorio` porque los scripts locales referencian el JAR.
- Defectos del árbol CHANGE corregidos al integrar: paréntesis de cierre sobrante en `SecurityConfiguration` y encadenado inválido de `permissionsPolicy`/`contentSecurityPolicy` (la API de Spring Security 6.5.11 hace que `permissionsPolicy(Customizer)` devuelva `PermissionsPolicyConfig`). El árbol CHANGE no fue modificado.
- Prueba existente actualizada sin debilitarla: `AuthenticationIntegrationTest` esperaba el título antiguo del laboratorio de circuitos; ahora comprueba el `aria-label` del esquema SVG, que persiste en el rediseño.
- Verificación: 39 pruebas JUnit aprobadas (37 previas + 2 nuevas de cursos), Flyway V1–V6 sobre H2 en memoria; `node scripts/test-astronomia.cjs` y `node scripts/test-circuitos.cjs` aprobados; humo HTTP: `/`, `/login`, CSS y SVG 200; `/cursos` y laboratorios 302 sin sesión; cabeceras de seguridad presentes en la respuesta. HSTS solo se emite sobre HTTPS, por lo que no aparece en localhost.
- Pendiente: PostgreSQL real; conectar cursos con Misiones/XP/insignias; revisión de flujos en Chromium con datos ficticios; prueba manual integral por roles; rebranding completo (pom/scripts) diferido.

El resto del estado previo (10 de noviembre) se conserva abajo.

- Proyecto activo en `CONTEXTO-LABORATORIO/PROYECTO`; el directorio padre conserva traspaso, bitácora y copias.
- Misiones individuales, grupales e interdisciplinarias admiten etapas obligatorias u opcionales añadidas antes de asignar, con entregas de texto, evaluaciones versionadas y autorizaciones tardías por etapa. La entrega final requiere al menos una versión de cada etapa obligatoria. Las nuevas etapas incorporan un criterio; las etapas antiguas sin criterio no se modifican.
- Decisión del propietario: escala de 0 a 5 y promedio simple de las etapas evaluadas de cada Misión. Se usa la última evaluación por etapa; las notas antiguas fuera de escala se excluyen y se marcan para revisión, sin alterar el historial. Se señala rendimiento insuficiente por Misión si el promedio es menor de 3,0. En Misiones grupales el docente puede aplicar ajustes individuales justificados por etapa; se copian al corregir, son visibles solo al integrante correspondiente y afectan su promedio y XP.
- Gamificación inicial: hasta 100 XP por Misión proporcional al promedio vigente, con final evaluado y etapas obligatorias entregadas. La clasificación es visible a personas autorizadas del semillero; se recalcula al corregir notas. Se muestra el rango personal según XP acumulados: Explorador 0, Investigador 500, Especialista 1500 y Mentor 3000. Existe clasificación global restringida a administradores de la academia. Hay insignias de primera y cinco Misiones, conservadas, y nota perfecta, revocable si ya no se cumple; se conserva historial de eventos. Falta libro de movimientos XP. Las autorizaciones tardías vigentes se pueden revocar sin borrar el historial.
- Pruebas: `./mvnw -q verify`, 37 aprobadas en H2.
- Perfil personal permite consultar roles, editar nombre y cambiar contraseña verificando la actual; el cambio cierra la sesión presente e invalida otras sesiones locales al usarse.
- Administración permite editar nombre y apellido de usuarios de la academia; creación y edición de nombres se auditan. Se permite activar o desactivar cuentas dentro de la academia, con protección del último administrador. Las sesiones creadas mediante el inicio local se invalidan en la siguiente petición si cambia el estado, roles o hash de contraseña; Los administradores pueden cambiar roles, con protección del último administrador y restricciones si el estudiante tiene Misiones o membresías activas. Los escenarios concurrentes aún requieren pruebas.
- Notificaciones internas por asignación, evaluación y concesión o revocación de autorización tardía, marcadas como leídas solo por su destinatario. Auditoría inicial para usuarios, asignaciones de semillero, Tripulaciones y membresías, Misiones, etapas, entregas, evaluaciones y autorizaciones tardías; visible a administradores de la academia. No cubre todavía todas las operaciones ni fallos.
- El panel con sesión muestra conteos reales de Misiones, etapas entregadas y notificaciones, y semilleros asignados; la portada sin sesión muestra datos ficticios identificados. Hay gestión inicial de competencias por semillero, vínculo a criterios antes de asignar, notas por criterio y vista personal con promedio y señal de alerta inferior a 3,0. Los puntajes de competencias grupales son comunes al equipo; el ajuste individual afecta la nota de etapa, no las notas por criterio.
- Las alertas por competencia inferior a 3,0 se guardan, se notifican y se resuelven al mejorar; se reconcilian al evaluar o consultar el progreso. Existe una vista docente de progreso por competencias, acotada a sus semilleros autorizados y a 500 resultados; indica resultados inferiores a 3,0.
- Adjuntos opcionales de hasta 10 MB por versión: PDF, PNG, JPG, GIF, WebP o ZIP; firma básica y extensión comprobadas. Descarga autenticada restringida a participantes y docentes autorizados. Se guardan en `data/adjuntos`, fuera de las copias de código; no hay antivirus ni análisis completo de contenidos.
- Faltan gestión adicional de usuarios, estadísticas agregadas y paginación docente, otras alertas y métricas agregadas, notificaciones y auditoría completas, y mejoras de chat. No está listo para datos reales ni producción.
- Ver `../ESTADO-Y-TRASPASO.md` y `DATA-MODEL-STATUS.md`. PostgreSQL y concurrencia real no están comprobados. La base H2 privada se conserva en `data/`.

- Las competencias se calculan con los puntajes de criterio de la última versión de evaluación; los criterios vinculados requieren nota. No se han migrado datos anteriores ni cubierto todas las reglas de progreso.

- Boletín PDF local personal y para docentes con semillero activo compartido o administradores de la academia, con todas las Misiones activas o completadas, notas, competencias, XP e insignias; las descargas por terceros se auditan. se comprobó extracción de texto y una página visual con datos ficticios. Incluye fuente Liberation Sans y su licencia; PDFBox se empaqueta en Maven.
- Chat con canales de Tripulación y privados estudiante-profesor con semillero activo en común; historial paginado de 100 mensajes y WebSocket de mismo origen. Se comprueban permisos y sesión activa al recibir y difundir. Faltan paginación de listados de conversaciones, límites de abuso y más pruebas de concurrencia.
- El docente puede cerrar manualmente cada asignación tras entregar etapas obligatorias y evaluar la versión final más reciente. El participante conserva acceso al historial sin poder enviar nuevas versiones.
- Simulador educativo local de circuito en serie con LED, resistencia, batería e interruptor; calcula corriente y potencia en un modelo ideal. `node scripts/test-circuitos.cjs` verifica las fórmulas.
- Flyway V5 añade el semillero de destino de cada asignación para aislar entregas interdisciplinarias; se probó migración de datos V4 y se aplicó a la base local H2 tras copia privada con la instancia detenida.
- Flyway V3 agrega historial de insignias y V4 las relaciones de chat. Ambas se aplicaron correctamente en la base local H2 tras guardar una copia privada con la aplicación detenida.

## Corrección de hallazgos de revisión — 11 de noviembre de 2026

- Backend de cursos corregido: revocación de acceso/escritura al perder asignación activa (`detail`, `updateLesson`), asociación dinámica semillero→cursos (`CourseCatalogSeeder.link` + `SemilleroAdministration.create`) e integración del enlace a cursos con vista de solo lectura para docentes y administradores (`catalogFor`, `staffCatalog`, plantilla, navegación).
- Pruebas: `./mvnw verify` BUILD SUCCESS, 48 JUnit en H2 (37 base + nuevas de revocación, asociación dinámica, roles y migración V5→V6 con datos).
- Chromium (Playwright + Chromium headless, app local V6): F1 anónimo → /login; F2 admin catálogo solo lectura (2 cursos, sin acciones); F2 profesor semillero (1 curso, solo lectura); F3 estudiante matrícula/detalle/cierre/revocación (catálogo vacío + detalle 404).
- Base local: copia privada previa y V6 aplicada (`now at version v6`), estado UP; app detenida tras la verificación.
- Pendiente: PostgreSQL real, concurrencia, accesibilidad, rendimiento, XP, revisiones.

## Avances — 11 de noviembre de 2026

- Base H2 persistente conectada y migrada a V6; Maven Wrapper 3.9.11 y Java 21 operativos. PostgreSQL sigue sin verificarse.
- Catálogo con vista separada para roles docente/administrador y estudiante simultáneos; la inscripción sigue exigiendo pertenencia activa. Los cursos cerrados muestran evidencias sin botones de edición.
- `./mvnw -q verify`: 49 pruebas aprobadas en H2, incluida la vista del curso completado y el catálogo para roles simultáneos. Quedan pruebas de navegación y accesibilidad, conexión de cursos con Misiones/XP y revisión integral de seguridad.

## Portada y actividades — 11 de noviembre de 2026

- La portada pública presenta CHANGE y los dos laboratorios sin cifras ficticias. El acceso identifica el correo y ofrece orientación para solicitar una cuenta.
- Cada una de las seis clases de Robótica y de las seis de Astronomía incluye una pregunta interactiva con retroalimentación. Las respuestas se comprueban localmente en el navegador y no se guardan ni califican. Se ajustó la animación orbital para variar la velocidad con la posición y el periodo.
- Verificación: `./mvnw -q test` 49 pruebas aprobadas; pruebas Node de ambos modelos aprobadas; navegador: portada, acceso y una respuesta correcta en cada laboratorio. No se han probado todas las opciones, dispositivos ni la accesibilidad completa. Las clases son introductorias; no todos los temas tienen simulador propio ni integración de sus respuestas con cursos o Misiones.
- Vista previa temporal aislada en puerto 8092 con base H2 en memoria: se pierde al detenerse. Las instancias y bases anteriores se conservaron.

## Estaciones interactivas — 11 de noviembre de 2026

- Robótica: estación que compara distancia de obstáculo y umbral, muestra si el robot avanza o se detiene y permite probar un caso y reiniciar. Modelo lógico sencillo, sin física de frenado ni sensor real.
- Astronomía: estación para variar distancia y calcular retardo luminoso de ida y vuelta en el vacío. Las prácticas no guardan resultados.
- `node scripts/test-estaciones.cjs`, `node scripts/test-circuitos.cjs` y `node scripts/test-astronomia.cjs` aprobados. Vista aislada en puerto 8093: se comprobó cambio de estado del robot y resultado inicial de señal; no se comprobó el botón de distancia mediante automatización del navegador. No se ejecutó la suite Maven en este bloque, pues solo cambian recursos web.

## Ocho clases por ruta — 11 de noviembre de 2026

- V7 agrega dos lecciones por curso y desplaza la lección final al número 8 conservando su identificador y evidencias; las nuevas lecciones son obligatorias para matrículas activas. Los cursos ya completados conservan su estado histórico. Las matrículas anteriores incorporan registros faltantes al consultar o actualizar el curso, con bloqueo por matrícula.
- Cada lección muestra un enlace a su laboratorio; ambos laboratorios muestran ocho clases con preguntas locales. Se añadieron transiciones de entrada y énfasis visual con respeto a movimiento reducido. La simulación de circuitos sigue siendo un circuito en serie, no un editor de componentes y conexiones libres. Cohetería se limita a modelos no propulsados; no existe constructor ni simulador completo de vuelo.
- V7 se probó sobre H2 con datos de V6, preservando matrícula y progreso; pruebas dirigidas de migración, roles y flujo de cursos aprobadas, y suite completa anterior al ajuste de orden con 49 pruebas aprobadas. En navegador se observó el catálogo con ocho lecciones por curso. Vista temporal aislada en puerto 8094. V7 NO se aplicó a la base persistente local; antes debe detenerse la app y hacerse copia privada consistente. PostgreSQL no probado.

## Talleres de circuitos y cohetes — 11 de noviembre de 2026

- Electrónica: editor de hasta ocho resistencias entre cuatro nodos y fuente ideal de 0–24 V. Calcula voltajes, corrientes y potencias para redes resistivas de corriente continua; incluye ejemplos serie y paralelo, detección de circuito abierto y conexiones flotantes. No simula componentes reactivos, diodos en la red, cables ni cortocircuitos, ni guarda diseños.
- Astronomía: taller de cinco pasos para visualizar un modelo de papel no propulsado con cuerpo, punta y número de aletas configurables. Es un esquema de diseño, no un cálculo de estabilidad o vuelo; no se guardan cambios.
- `./mvnw -q test` 49 pruebas aprobadas. `node scripts/test-red-circuitos.cjs` y pruebas Node de simuladores existentes aprobadas. Navegador aislado: ejemplo paralelo produce 37,88 mA a 5 V con 220 Ω y 330 Ω; el taller de cohete avanza entre pasos. Vista aislada temporal en puerto 8095. V7 sigue sin aplicarse a la base persistente local.
- Pendiente: editor de componentes más amplio, simulaciones adicionales, guardar diseños y conexión con progreso y Misiones, accesibilidad y pruebas completas.

## Organización y contenido de las clases — 11 de noviembre de 2026

- Cada laboratorio muestra primero su ruta de ocho clases con idea clave, práctica guiada, bitácora, acceso a la práctica y pregunta interactiva. Se añadió navegación de secciones; simuladores y estaciones se mantienen más abajo. Son contenidos introductorios, no un currículo profesional completo.
- Las explicaciones y ejercicios breves de la página son independientes de las notas y del progreso guardado en el módulo de cursos. Se actualizó `RUTAS-FORMATIVAS-LABORATORIOS.md`.
- Pruebas Node de simuladores aprobadas. En navegador aislado se revisaron ambas páginas, navegación, orden y apertura de una clase de electrónica. No se ejecutó Maven en este bloque de contenidos. Vista previa temporal en puerto 8096. V7 no se aplicó a la base persistente.

## Flujo de clases y base local — 11 de noviembre de 2026

- Cada lección del curso enlaza a su clase concreta en el laboratorio. Al registrar evidencia inválida, el formulario muestra el motivo y conserva el texto; al guardar informa el resultado. El servidor limita la evidencia a 5000 caracteres en todos los estados.
- `./mvnw -q test`: 49 pruebas aprobadas, incluida evidencia corta, conservación del texto y límite de longitud. Scripts Node de los cuatro modelos aprobados.
- Se detuvo la instancia principal, se guardó y verificó `../respaldos/datos-antes-v7-20261111-162554.tar.gz` (datos privados), y se probó V7 sobre una copia: versión 7, 16 lecciones y mismas cantidades comprobadas de usuarios (14), matrículas (3) y avances (18). Se arrancó el paquete actual en 8080 contra la base persistente, con salud UP; contiene V7. No se verificó PostgreSQL ni la integridad de todos los registros.
- Incidente: el ejecutable se reconstruyó por error mientras la instancia anterior lo usaba; su cierre produjo un error de carga de clases. Se respaldó la base después del cierre y antes de migrar. La copia arrancó y migró, pero no se ha realizado auditoría integral de datos. No reconstruir un JAR en uso.
- El producto no está completo: persisten integración real de resultados de simuladores con cursos/Misiones, más componentes eléctricos, desarrollo de clases y evaluación, accesibilidad, seguridad, concurrencia, PostgreSQL y pruebas integrales por roles.

## Diseños y evidencias — 11 de noviembre de 2026

- V8 crea `diseno_laboratorio`: hasta un diseño de circuito y uno de cohete por usuario. Los datos se validan y limitan, las consultas usan la identidad autenticada, los cambios requieren CSRF y se auditan sin el contenido. Guardar sustituye el diseño anterior del mismo tipo.
- Cada taller permite guardar/recuperar; una lección permite añadir voluntariamente un resumen a la evidencia, sin completar ni evaluar automáticamente. Prueba Node comprueba la importación y límite. En navegador aislado se guardó y recuperó un circuito y se guardó un cohete; pruebas MVC cubren aislamiento entre usuarios, CSRF, entradas inválidas y cohete.
- `./mvnw -q test`: 50 pruebas aprobadas. V8 se probó en una copia privada de la base V7 con éxito, conservando las cantidades comprobadas (14 usuarios, 3 matrículas, 18 avances). Copia previa verificada `../respaldos/datos-antes-v8-20261111-163504.tar.gz`. La instancia principal arrancó en 8080 con V8; PostgreSQL sigue sin probar. Las instancias de prueba usan bases temporales independientes.
- Aún faltan revisión integral de seguridad y accesibilidad, pruebas por roles, concurrencia y PostgreSQL, profundidad de contenidos y componentes adicionales de simulación. No se declara el producto terminado.

## Consulta docente de cursos — 11 de noviembre de 2026

- Docentes con asignación activa al semillero y administradores de la academia pueden consultar lecciones y evidencias de estudiantes matriculados en un curso de ese semillero. Usuarios con varios roles mantienen acceso al ámbito correspondiente a cada rol. Se valida de nuevo la academia y el semillero en el servidor; no se modifica ni califica la evidencia.
- La consulta muestra hasta 4000 filas y advierte al alcanzar ese límite; no hay paginación. Pruebas MVC y de servicio verifican acceso docente, contenido y denegación a docentes/administradores ajenos. `./mvnw -q test`: 51 pruebas aprobadas; prueba Node de importación de evidencia aprobada. Paquete actualizado en 8080 con salud UP.
- Persisten pendientes de seguridad, accesibilidad, PostgreSQL, carga y funcionalidad; no se declara el producto completo.

- Actualización: la consulta docente se pagina de 100 filas en 100 y ya no usa el límite fijo de 4000. Prueba dirigida con 104 lecciones verifica ambas páginas y permisos. La última suite completa anterior al cambio de paginación tuvo 51 pruebas aprobadas; después pasó la prueba dirigida.

## Revisión de seguridad y puesta en marcha — 11 de noviembre de 2026

- La prueba docente comprueba que una evidencia con HTML se escape y no se ejecute, además del aislamiento por academia/semillero y la paginación. Auditoría estática simple de plantillas: no se detectaron ID literales duplicados ni campos sin etiquetas mediante la comprobación aplicada; no sustituye una revisión de accesibilidad completa.
- La mesa de resistencias distingue circuito abierto de circuito conectado con fuente de 0 V y rechaza voltaje vacío. Se comprobó el modelo con pruebas Node. El iniciador evita recompilar si la aplicación ya responde y advierte si el JAR de compilación está en uso; se comprobó sintaxis del script.
- `./mvnw -q test`: 51 pruebas aprobadas; cinco scripts Node aprobados. Se construyó el paquete fuera de la carpeta activa y la instancia local en 8080 arrancó. PostgreSQL no disponible en localhost:5432 y no se probó. Persisten revisión integral de seguridad y accesibilidad, recorridos por roles, carga, fidelidad de simuladores y profundidad de cursos; no se declara completo.

## Recorrido integral por roles — 11 de noviembre de 2026

Vista aislada con base H2 temporal en puerto 8097 (JAR del código actual; la instancia principal 8080 y `data/` no se tocaron). Recorridos automatizados en Chromium/Playwright por bloques (admin, profesor, estudiante, multirol), con datos totalmente ficticios. **51 pruebas Maven siguen aprobando (0 fallos) y 5 scripts Node; no hubo cambios de código.**

Resultados con evidencia: alta/rol multirol; asignaciones y Tripulación; Misiones individual/grupal/interdisciplinaria con sus autorizaciones; etapa obligatoria antes de asignar; entregas y adjunto PNG; evaluación por etapas; XP acumulado 270 (riego 5,0→100; medición eléctrica 4,5→90; cohetes grupal 4,0→80); ajuste individual de −1,5 justificado en un miembro de Nova, conservado al corregir; boletín PDF personal y docente (auditado como DESCARGAR_BOLETIN); chat WebSocket con exclusión estudiante↔estudiante y escape HTML; reactivación/revocación de cursos al cerrar la asignación activa (detalle 404; evidencia conservada); cierre de Misión con historial accesible; regla que impide cerrar asignaciones con membresías activas. El catálogo dual del multirol muestra su ámbito de estudiante más la consulta de solo lectura para roles docente/administrador.

No verificado aquí: PostgreSQL real, concurrencia bajo carga real, accesibilidad completa, revisión integral de seguridad. Quedan abiertas las tareas de la pauta (libro de XP, límites de abuso del chat, cobertura completa de auditoría/notificaciones, accesibilidad, estadísticas, laboratorios). La vista temporal se cerró; la carpeta con base/scripts ficticios permanece fuera del repositorio.

## CHANGE // SPACE-LAB OS — 11 de noviembre de 2026

Identidad visual SPACE-LAB aplicada a las 32 plantillas; registro público **acotado** (`/registro` + `/administracion/solicitudes` con aprobación y contraseña temporal de un único uso); rate-limit de login; jerarquía sellada de dominio y `repository/UsuarioRepository`; `SECURITY.md` (OWASP Top 10) y `docs/ARQUITECTURA.md`. Nuevos simuladores: lanzamiento (TWR, quemado, Δv) y multímetro DC en tiempo real. Verificación: 56 pruebas Maven, 0 fallos; 6 scripts Node; UI en Chromium (revisión en preview exacta de producción). V9 aplicada a la base local tras copia privada verificada; la instancia principal 8080 corre el paquete actual (health UP) y la vista aislada 8097 está activa para revisión con datos ficticios.
