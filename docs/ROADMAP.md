# Hoja de ruta de Laboratorio

| Fase | Alcance | Estado |
|---|---|---|
| 0 | Definición funcional y stack | Aprobada |
| 1 | Modelo ER, restricciones, clases, rendimiento y paquetes | Diseño integral documentado; decisiones académicas pendientes |
| 2 | Proyecto base, configuración y migraciones | Migraciones V1 a V6 aplicadas en H2 local y probadas en H2 (JUnit); PostgreSQL sin probar |
| 3 | Autenticación, usuarios y roles | Inicio, alta, edición de nombres y activación/desactivación implementados; gestión de roles implementada; revisión de concurrencia y seguridad pendientes |
| 4 | Semilleros, Tripulaciones, Misiones, entregas y notas | Flujo inicial con etapas, adjuntos, ajustes, Misiones interdisciplinarias y cierre manual; reglas y revisión de integridad pendientes |
| 5 | Promedios, progreso, competencias débiles y alertas | Promedio por Misión y competencias por criterio implementados; vista docente por semillero inicial; alertas persistentes de competencias implementadas; estadísticas agregadas pendientes |
| 6 | XP, rangos, insignias y rankings | XP calculados por nota y clasificación de semillero implementados; rangos, insignias y clasificación global administrativa implementados; movimientos XP pendientes |
| 7 | Chat en tiempo real | Canales, conversaciones privadas e historial paginado implementados; listas, abuso y concurrencia pendientes |
| 8 | Auditoría, notificaciones y boletines PDF | Notificaciones y auditoría iniciales y boletines PDF personales y docentes implementados; cobertura de auditoría y notificaciones pendiente |
| 9 | Frontend completo y laboratorio interactivo | Panel personal; laboratorios de Robótica (circuitos) y Astronomía (órbitas) con rutas formativas y estética CHANGE; accesibilidad y revisión en Chromium pendientes |
| 10 | Pruebas finales, documentación y entrega | En curso; falta PostgreSQL y revisión integral de seguridad |

## Hito actual

Las Misiones admiten etapas previas a su asignación. La calificación aprobada va de 0 a 5 y el promedio se calcula con la última nota de cada etapa evaluada; las notas históricas fuera de escala se excluyen y se señalan para revisión. La entrega final requiere una entrega en cada etapa obligatoria; cada etapa nueva incluye un criterio. Los promedios inferiores a 3,0 se indican como insuficientes.

La base visual se integró en la raíz definitiva. La verificación Maven incluye el arranque
del contexto y pruebas de seguridad de la página inicial. El esquema inicial existe; el siguiente hito es implementar y probar las reglas
transaccionales y de autorización que aún no están cubiertas. Ver `DATA-MODEL-STATUS.md`.

### Actualización 11 de noviembre de 2026 — integración CHANGE

Trabajo desarrollado en `/home/jose/Projects/Pruebas/CHANGE-backend-progreso` e integrado por grupos al proyecto principal, con verificación tras cada grupo (`./mvnw -q verify` completo y prueba de humo con H2 en memoria). El árbol CHANGE contenía dos defectos de compilación que impedían construir con Maven y que se corrigieron al integrar: un paréntesis de cierre sobrante en `SecurityConfiguration` y el orden de encadenado de `permissionsPolicy`/`contentSecurityPolicy` (según la API de Spring Security 6.5.11, `permissionsPolicy(Customizer)` devuelve `PermissionsPolicyConfig` y no admite encadenar después).

| Grupo | Alcance | Estado |
|---|---|---|
| 1 | Cursos y progreso: migración V6, `CourseService`/`CourseController`/`CourseCatalogSeeder`, vistas `/cursos` y detalle, pruebas de matrícula, evidencia y migración | Integrado y verificado con JUnit: 39 pruebas aprobadas |
| 2 | Laboratorios: Astronomía (`/laboratorio/astronomia`) y rediseño de circuitos con rutas formativas; página de inicio de sesión propia; cabeceras de seguridad (CSP, frame-deny, referrer, permissions-policy); estética CHANGE en plantillas | Integrado y verificado: 39 pruebas, scripts `test-astronomia.cjs` y `test-circuitos.cjs` aprobados, humo HTTP correcto |
| 3 | Endurecimiento de sesión en perfil prod (cookie http-only/secure/same-site, 30 min) y branding de visualización en `application.yml` | Integrado y verificado |
| 4 | Documentación y copia | En curso |

Notas: se conserva `artifactId=laboratorio` porque los scripts locales (`run-laboratorio.sh`, `DETENER-LABORATORIO.sh`) referencian el nombre del JAR; el rebranding completo queda diferido. La migración V6 se aplicó a la base local `data/` después de una copia privada. Los cursos aún no están conectados con Misiones, rúbricas docentes, XP, insignias ni certificados.

### Ocho clases y laboratorios — 11 de noviembre de 2026

Las dos rutas se ampliaron a ocho lecciones mediante V7, probada en H2; falta aplicarla a la base local con copia privada y aplicación detenida. Los cursos enlazan al laboratorio correspondiente. El editor de circuitos de conexiones libres, el constructor didáctico de cohetes y su integración académica siguen pendientes; los simuladores actuales son modelos simplificados.

### Diseños de laboratorio — 11 de noviembre de 2026

Guardado y recuperación de un circuito y un cohete por usuario con V8; importación opcional del resumen como evidencia de una lección. La funcionalidad se probó en H2 y se aplicó a la base local tras una copia privada. Pendientes: revisión integral de seguridad y accesibilidad, PostgreSQL, variedad y fidelidad de simuladores, experiencia completa por roles y pruebas de carga.

Docentes y administradores autorizados pueden consultar el progreso y las evidencias de cursos por semillero; la vista se pagina de 100 en 100 filas. La importación de diseños a evidencia no asigna notas ni XP automáticamente.
