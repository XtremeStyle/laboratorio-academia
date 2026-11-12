# Estado del modelo de datos

La migración [V2](../src/main/resources/db/migration/V2__academic_schema.sql) crea el esquema inicial de identidad, semilleros, Tripulaciones, Misiones, entregas, evaluaciones, competencias, gamificación y estructuras reservadas para notificaciones, auditoría y chat. Incluye los tres roles iniciales. El diseño y sus decisiones pendientes están en [DATA-MODEL-PROPOSAL.md](DATA-MODEL-PROPOSAL.md).

## Comprobado

- Flyway aplica las migraciones V1 a V5 en H2 de pruebas; la suite de integración verifica unicidad de correo, aislamiento en asignaciones, destinatarios, coherencia de etapas y versiones.
- Algunas claves foráneas compuestas impiden cruces entre academias o entre Misiones, entregas y evaluaciones. Los registros de relaciones activas tienen una clave única.

## Limitaciones que deben resolverse antes de uso real

- El esquema no equivale a servicios de negocio ni a autorización. La autenticación usa usuarios y roles persistidos. Existen creación y listado de usuarios y semilleros, asignaciones y Tripulaciones, Misiones individuales/grupales, entrega final de texto, autorizaciones tardías y notas base con historial. Existen etapas configurables antes de asignar y promedio por Misión; faltan gestión completa de cuentas y permisos, estadísticas docentes por competencias y gamificación completa.
- Los servicios de asignación y membresía actualizan períodos y registros activos en una transacción con bloqueo de usuario; el esquema no impide por sí solo períodos históricos solapados, períodos abiertos sin marcador, ni marcadores asociados a un período cerrado. Deben implementarse servicios transaccionales y probarse con concurrencia.
- Las reglas que dependen de varias tablas (membresías vigentes, roles autorizados, autor de entrega, plazos y autorizaciones, modalidad de destinatario, una etapa final, criterios aplicables, ajustes grupales y coherencia entre academias de todas las tablas) aún requieren comprobaciones transaccionales y pruebas. La inmutabilidad de versiones, evaluaciones y XP tampoco está garantizada por la base.
- La escala aprobada es 0 a 5 y la nota de Misión es el promedio simple de las últimas notas de sus etapas evaluadas. Las notas históricas fuera de escala se excluyen y se indican para revisión; no se modifican. Los XP proporcionales a nota (hasta 100 por Misión) y la clasificación por semillero se calculan desde evaluaciones vigentes; no se registra aún un libro de movimientos XP. Las competencias usan el promedio simple de las notas de criterio de la última evaluación de cada etapa; el umbral inferior a 3,0 se señala en pantalla. Los ajustes individuales afectan notas y XP, no puntajes de criterios. Faltan alertas persistentes. Los adjuntos locales usan la tabla prevista; falta revisión de seguridad y copias de datos. El chat usa la migración V4 para canales y pares privados.
- La migración se probó en H2; PostgreSQL y concurrencia real no se han verificado. H2 de desarrollo se conserva en un archivo local; las pruebas utilizan H2 en memoria.

No se debe usar esta base para datos reales hasta implementar y verificar esas reglas y la seguridad correspondiente. La inicialización de administrador existe solo en desarrollo y no configura producción.

Las migraciones V3 y V4 añaden chat y revocación e historial de eventos para las insignias; su aplicación se verificó en H2 de pruebas y en la base local, no en PostgreSQL.

V5 registra el semillero de destino de cada asignación y exige que pertenezca a la Misión; migración de una asignación V4 verificada en H2. Aplicada a la base local H2 tras copia privada; no comprobada en PostgreSQL.

## V6 — cursos y seguimiento de aprendizaje (11 de noviembre de 2026)

La migración `V6__learning_courses_and_progress.sql` añade `curso`, `curso_semillero`, `leccion_curso`, `matricula_curso` y `progreso_leccion`. Cada matrícula se vincula a un usuario y a un semillero permitido para ese curso. Las claves foráneas compuestas impiden registrar progreso para lecciones de otro curso; las restricciones de estado y marcas de tiempo mantienen coherencia básica; la unicidad impide duplicar matrícula por curso/usuario/semillero y progreso por matrícula/lección.

El catálogo inicial contiene Robótica y Cosmos, seis lecciones cada uno, y se persiste en la propia migración. La asociación a semilleros se realiza de forma idempotente por código (`ROBOTICA`, `ASTRONOMIA`, `COSMOS`) en `CourseCatalogSeeder`, de modo que funciona con varias academias sin fijar UUID de demostración. Los servicios vuelven a validar rol ESTUDIANTE y asignación activa; el progreso exige evidencia de 12 a 5000 caracteres al completar una lección y es monotónico; la matrícula se cierra cuando no quedan lecciones obligatorias pendientes. La creación de matrículas y las actualizaciones se serializan con bloqueo de fila, y la matrícula repetida es idempotente.

Verificación en este proyecto: la suite Maven/JUnit (`./mvnw -q verify`) ejecuta V1–V6 sobre H2 y las pruebas `LearningCoursesMigrationTest` (2 cursos, 12 lecciones, integridad referencial) y `CourseLearningFlowTest` (catálogo, matrícula repetida, evidencia, intento de retroceso) aprueban. Falta aplicar V6 a la base local (con aplicación detenida y copia privada), PostgreSQL real, concurrencia multiinstancia y conectar cursos con Misiones, evaluación docente y gamificación.

## V8 — diseños personales de laboratorio

`diseno_laboratorio` conserva un diseño de circuito y uno de cohete por usuario, con clave única por usuario y tipo. El acceso se resuelve desde la cuenta autenticada; el contenido se valida y limita antes de guardarse. La operación registra auditoría sin incluir el diseño. Guardar de nuevo sustituye el diseño anterior del mismo tipo. El diseño solo se incorpora a una evidencia cuando el estudiante lo solicita y luego guarda la lección; no asigna nota ni XP automáticamente. V8 se verificó en H2 y no en PostgreSQL.
