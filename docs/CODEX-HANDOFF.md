# Traspaso de Laboratorio a Codex

## 1. Objetivo del proyecto
Laboratorio es una plataforma web para una academia con semilleros especializados, inicialmente Robótica y Astronomía/Física Espacial. Debe gestionar estudiantes, profesores, administradores, tripulaciones, misiones, entregas, evaluaciones y progreso académico con gamificación.

El sistema debe tener identidad visual propia, ser usable, responsive, seguro, modular y defendible como proyecto universitario. El objetivo no es solo producir una maqueta: evolucionar hacia una aplicación real con backend, persistencia, validaciones y pruebas.

## 2. Stack aprobado
- Java 21.
- Spring Boot 3.x.
- Maven.
- Spring MVC tradicional + Thymeleaf + HTMX.
- Spring Web, Spring Data JPA/Hibernate, Spring Security.
- PostgreSQL para producción; H2 para pruebas y desarrollo local.
- Flyway para migraciones.
- JUnit 5 y Mockito.
- SLF4J y Bean Validation.
- WebSocket más adelante, no es prioridad del MVP.

No asumir Docker. No exigir PostgreSQL local para ejecutar la demo. No usar servicios externos como requisito de tiempo de ejecución sin aprobación.

## 3. Arquitectura aprobada
Monolito modular con separación por módulos de negocio y capas internas. No microservicios.

Paquetes objetivo:
- `config/`
- `shared/`
- `identity/`
- `academy/`
- `research/` (semilleros)
- `crew/` (Tripulaciones)
- `mission/` (Misiones)
- `submission/` (entregas)
- `evaluation/`
- `progress/`
- `gamification/`
- `presentation/`

Dentro de cada módulo, separar responsabilidades cuando tenga sentido: `domain`, `application`/`service`, `infrastructure`/`persistence`, `presentation`/`web`. No crear carpetas vacías ni capas artificiales.

## 4. Dominio y reglas de negocio aprobadas
Jerarquía conceptual:
Academia → Semilleros → Tripulaciones → Misiones → Entregas → Evaluaciones/Notas.

- Un usuario puede tener varios roles simultáneamente.
- El correo es único globalmente.
- Roles iniciales: administrador, profesor y estudiante. Preparar el diseño para crecer sin hardcodear permisos dispersos.
- Una persona puede pertenecer simultáneamente a Robótica y Astronomía.
- Las asignaciones a semilleros deben conservar historial y poder reactivarse.
- Código de semillero único por academia.
- No permitir dos asignaciones activas duplicadas del mismo usuario al mismo semillero.
- Administración gestiona usuarios, asignaciones a semilleros, profesores y tripulaciones.
- Las misiones pueden ser individuales, grupales o interdisciplinarias.
- Las misiones pueden asignarse a estudiantes individuales o a Tripulaciones.
- Una misión grupal tiene una nota grupal base y ajustes individuales justificados según desempeño y contribución. Las notas conservan historial.
- Entregas: el profesor define etapas obligatorias u opcionales; en cada etapa se permiten varias versiones; existe una entrega final; cambios después de la fecha límite requieren autorización docente; se conserva historial.
- Evaluación por etapa o solo sobre la entrega final, según configuración del profesor.
- XP y clasificación híbridos: por semillero y global.
- Vocabulario de interfaz: grupos = **Tripulaciones**; tareas = **Misiones**.

## 5. MVP y alcance por fases
1. Definición y decisiones — aprobada.
2. Modelo de datos y diseño — en curso; falta cerrar ER completo, restricciones, clases y estructura de paquetes.
3. Base del proyecto: estructura, configuración, migraciones.
4. Autenticación y roles.
5. Semilleros, Tripulaciones, Misiones, entregas y notas.
6. Cálculos: promedios, progreso, competencias débiles y alertas.
7. Gamificación: XP, rangos, insignias.
8. Chat en tiempo real.
9. Auditoría, notificaciones y boletines PDF.
10. Frontend completo y laboratorio interactivo.
11. Pulido, documentación y entrega.

Prioridad: MVP académico + gamificación. Chat, simuladores y extras son posteriores. Backend funcional y probado antes de expandir UI.

## 6. Diseño visual aprobado
- Identidad Cosmic + Dual Lab.
- Diseño minimalista, sobrio, menos saturado y sin efectos decorativos innecesarios.
- Tema oscuro predeterminado y tema claro intercambiable.
- Panel principal personalizado después del inicio de sesión.
- Robótica: acentos cian.
- Astronomía/Física Espacial: acentos violeta/azul.
- Espacios amplios, tipografía legible, tarjetas discretas, navegación clara y responsive.
- Sin depender de Google Fonts ni de recursos remotos para que la interfaz se vea correctamente.
- El dashboard debe usar datos reales cuando el backend exista. Cualquier dato ficticio debe estar claramente identificado como demostración.

## 7. Estado del código entregado (punto de partida, no asumir que está terminado)
El ZIP `Laboratorio-vista-ejecutable-v2.zip` fue generado y contiene:
- `pom.xml`
- `mvnw`, `mvnw.cmd`
- `run-laboratorio.sh`
- `src/main/java/com/laboratorio/LaboratorioApplication.java`
- `config/SecurityConfiguration.java`
- `presentation/web/HomeController.java`
- `application.yml`, `application-dev.yml`, `application-test.yml`, `application-prod.yml`
- migración `V1__create_application_metadata.sql`
- `templates/index.html`
- `static/css/laboratorio.css`
- `static/js/laboratorio.js`
- pruebas `LaboratorioApplicationTests.java` y `HomeControllerTest.java`
- `docs/ARCHITECTURE.md`, `ROADMAP.md`, `DATA-MODEL-STATUS.md`, `DEVELOPMENT.md`, `DEMO-VISUAL.md`
- README y scripts auxiliares.

La versión 2 es principalmente una base visual/demo. No afirmar que ya existe backend académico completo, autenticación funcional, persistencia de entidades de negocio, misiones reales ni XP conectado a BD. Verificar qué hace realmente cada pantalla/control antes de describirlo como funcional.

**Compilación:** el entorno de generación no consiguió completar Maven por un problema de acceso al servidor de descargas. La compilación y pruebas deben verificarse en el computador del usuario. El ZIP fue verificado estructuralmente, no equivale a una prueba de compilación exitosa.

## 8. Problema de extracción observado
El ZIP contiene una carpeta raíz `Laboratorio/`. Si el usuario extrae el ZIP dentro de una carpeta llamada `Laboratorio-vista-ejecutable-v2`, la ruta queda:
`ACADEMY/Laboratorio-vista-ejecutable-v2/Laboratorio/`

La carpeta de proyecto real es la que contiene `pom.xml`, no la carpeta exterior. El lanzador correcto se ejecuta desde esa raíz:
`chmod +x run-laboratorio.sh && ./run-laboratorio.sh`

En el explorador del usuario, `ACADEMY` contiene además carpetas y ZIPs de versiones anteriores. No borrar ni sobrescribir nada sin inspeccionar y comparar primero. La ruta local definitiva que el usuario había establecido es:
`/home/jose/Projects/ACADEMY/Laboratorio`

## 9. Primera tarea recomendada para Codex
No empezar añadiendo más funcionalidades visuales ni rehaciendo todo. Primero:
1. Inspeccionar el repositorio local y detectar cuál carpeta contiene el proyecto activo.
2. Revisar `git status`, `pom.xml`, `README.md`, configuración Spring, seguridad, migraciones, plantillas, scripts y tests.
3. Comparar el contenido activo con el ZIP v2 si el usuario lo proporciona.
4. Identificar discrepancias, archivos faltantes, errores de compilación, dependencias y riesgos.
5. Ejecutar `./mvnw test` y `./mvnw package` (si la red y Java lo permiten).
6. Entregar un diagnóstico priorizado, sin hacer push, sin borrar ni reemplazar archivos.
7. Proponer el siguiente bloque pequeño: estabilizar base y cerrar modelo ER completo antes de crear módulos académicos.

No implementar todo el roadmap en una sola operación. Hacer entregas incrementales y verificables.

## 10. Lo que Codex debe reportar
Al finalizar cada tarea, indicar:
- objetivo y alcance;
- archivos creados/modificados;
- decisiones técnicas y razón;
- comandos ejecutados;
- resultado exacto de compilación y pruebas;
- deuda técnica/limitaciones;
- siguiente tarea recomendada.

## 11. Restricción GitHub crítica
El repositorio remoto existe y es público, pero el usuario ordenó expresamente no publicar absolutamente nada hasta que el proyecto esté completamente terminado y autorice la entrega final. No `git push`, no deploy, no publicación de paquetes, no sincronización remota.
