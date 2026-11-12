# Continuación de Laboratorio con OpenCode

## Objetivo
Llegar a una versión lista para pruebas funcionales con datos ficticios: integrar cuidadosamente el trabajo CHANGE, completar los recorridos académicos prioritarios, mejorar los laboratorios de Robótica y Astronomía y pulir la experiencia visual. No declarar terminado sin comprobar los criterios de aceptación.

## Fuentes
- Proyecto principal: `/home/jose/Projects/ACADEMY/CONTEXTO-LABORATORIO/PROYECTO`.
- Trabajo del otro chat, para comparar e integrar por partes: `/home/jose/Projects/Pruebas/CHANGE-backend-progreso`.
- Leer `AGENTS.md`, `../AGENTS.md`, `../ESTADO-Y-TRASPASO.md`, `docs/SESSION-STATE.md`, `docs/ROADMAP.md` y en la otra carpeta `docs/CHANGE-LABS.md`, `docs/RUTAS-FORMATIVAS-LABORATORIOS.md` y el final de `docs/SESSION-STATE.md`. Los documentos describen estados de momentos distintos; comprobar el código y dar prioridad a las instrucciones actuales del propietario.
- La otra carpeta incluye cambios de diseño, Astronomía, cursos y migración V6. No copiarla íntegramente sobre el proyecto principal ni usar su JAR como prueba de compilación.

## Protección y límites
- No leer ni enviar datos de `data/`, adjuntos, `.env`, credenciales ni copias privadas al modelo; trabajar con datos sintéticos. No imprimir secretos.
- No publicar, desplegar, hacer push ni commits. Conservar ambos árboles y la base de datos.
- Antes de aplicar cualquier migración a la base local: detener la aplicación, hacer y comprobar una copia privada consistente. Primero probar migraciones sobre bases de prueba aisladas. Las copias de código no incluyen datos.
- Usar exclusivamente modelos que OpenCode identifique y cobre como gratuitos. No cambiar a uno de pago ni usar agentes o modelos auxiliares de pago; si no puede garantizarse, detenerse e informar. La disponibilidad y los precios pueden cambiar.

## Orden y criterios de aceptación
1. Comparar versiones y ejecutar las pruebas existentes en ambos árboles sin afectar datos reales. Integrar cambios coherentes por grupos, conservando permisos, comportamiento y datos existentes. Comprobar la migración V6 y sus pruebas, incluida actualización desde V5 y permisos entre semilleros.
2. Completar y probar recorridos de administración, docencia y estudiantes: acceso, semilleros, tripulaciones, cursos, misiones, entregas, evaluación, progreso, insignias, boletines y chat. Documentar claramente cualquier límite o funcionalidad faltante.
3. Mejorar los dos laboratorios con explicaciones y actividades pedagógicas, controles accesibles, resultados correctos y estados claros. Verificar fórmulas y restricciones; no presentar las simulaciones como instrumentos reales.
4. Unificar la estética CHANGE con navegación clara, legibilidad, adaptación a móvil, accesibilidad de teclado y contraste. Revisar flujos y errores en Chromium con datos ficticios.
5. Ejecutar compilación y pruebas pertinentes, incluidas pruebas de permisos y migración, y realizar una prueba manual integral. Documentar arranque, cuentas de prueba sin credenciales reales, límites y riesgos. No afirmar que es apto para producción sin revisión de seguridad y las verificaciones correspondientes.
6. Actualizar estado, hoja de ruta, modelo de datos, bitácora y copia de código según los AGENTS. Pedir al propietario solo decisiones que realmente bloqueen el trabajo; seguir de forma autónoma en las demás.

Trabajar en bloques pequeños: completar cada uno, comprobarlo y dejar registrado el siguiente. No abrir servicios al exterior ni depender de servicios adicionales sin autorización.
