# Cómo ahorrar tokens al trabajar con Codex y ChatGPT

## Reglas prácticas
1. Usa Codex para leer el repositorio y realizar cambios; evita pegar archivos completos en el chat si Codex ya tiene acceso al proyecto.
2. Mantén estas instrucciones en `AGENTS.md`. Codex debe leerlas automáticamente al trabajar en el repositorio.
3. Mantén `docs/CODEX-HANDOFF.md` como fuente de contexto del dominio y las decisiones.
4. Pide una sola tarea concreta por prompt. Evita "haz todo el proyecto".
5. Define alcance y criterios de aceptación; pide que no toque archivos fuera del alcance salvo necesidad justificada.
6. Pide un resumen final corto con archivos cambiados, pruebas y pendientes.
7. No solicites que repita contexto ya documentado. Indica: "Lee AGENTS.md y docs/CODEX-HANDOFF.md; no repitas su contenido".
8. Evita pedir explicaciones largas de código evidente. Solicita explicación de decisiones no obvias y riesgos.
9. Agrupa cambios pequeños relacionados en una sola tarea, pero separa módulos grandes (autenticación, misiones, entregas, etc.).
10. No pidas al modelo que vuelva a generar archivos que ya existen y están correctos; que inspeccione y edite lo necesario.
11. Usa resúmenes de sesión: al terminar, pide actualizar `docs/SESSION-STATE.md` con estado, decisiones, pruebas y próximo paso. En la siguiente sesión, que lea ese archivo.
12. Desactiva búsquedas web y herramientas externas cuando la tarea sea solo editar el repositorio local y no se necesite información actual.

## Plantilla breve de prompt
"Lee primero AGENTS.md, docs/CODEX-HANDOFF.md y docs/SESSION-STATE.md. Inspecciona el estado actual del repositorio. Trabaja únicamente en [alcance]. No cambies arquitectura ni reglas de negocio no aprobadas. Ejecuta [pruebas]. No hagas push, deploy ni cambios remotos. Al final resume archivos, pruebas reales, problemas y actualiza SESSION-STATE.md."

## Cuándo conviene pedir más contexto
Solo adjunta archivos o pega código cuando Codex no tenga acceso a ellos. Si Codex tiene el repositorio abierto, dale rutas y objetivos, no copias redundantes.
