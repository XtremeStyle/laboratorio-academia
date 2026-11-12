# Versión visual ejecutable

## Alcance
Esta entrega reemplaza la landing page inicial por un panel de trabajo que permite visualizar la dirección de producto: navegación, resumen académico, semilleros, misiones, progreso y Tripulación.

## Interacciones disponibles
- Cambiar entre tema oscuro y claro. La selección se conserva en `localStorage` del navegador.
- Navegar a secciones mediante el menú lateral.
- Abrir y cerrar el menú lateral en pantallas pequeñas.
- Fecha localizada según la configuración `es-CO` del navegador.

## Datos demostrativos
Las cifras del panel están escritas en la plantilla. La consulta de semilleros de desarrollo lee dos registros ficticios de la base local. Ninguno representa datos reales. No se debe presentar esta versión como un sistema académico terminado.

## Arquitectura de esta entrega
- Spring MVC entrega la plantilla Thymeleaf.
- CSS y JavaScript se sirven desde recursos estáticos locales.
- H2 permite arrancar el contexto `dev` sin infraestructura externa.
- PostgreSQL se mantiene como objetivo de producción, no como requisito para esta demostración.

## Validación
Ejecutar `./mvnw test` en un entorno con Java 21 e internet disponible. La ejecución de Maven requiere descargar dependencias la primera vez.
