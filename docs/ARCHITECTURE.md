# Arquitectura inicial

## Estilo

Aplicación monolítica modular con Spring MVC y renderizado del lado del servidor.
Se evita distribuir servicios antes de que exista una necesidad real.

## Capas previstas

1. `presentation`: controladores MVC, formularios y DTOs de entrada/salida.
2. `application`: servicios y coordinación de casos de uso.
3. `domain`: entidades y reglas de negocio.
4. `infrastructure`: persistencia, adaptadores e integraciones.

El paquete inicial solo contiene la capa de presentación mínima y el arranque.
Los paquetes de dominio se crearán cuando el modelo ER esté aprobado.

## Persistencia

- PostgreSQL en producción.
- H2 persistente en archivo para desarrollo local y H2 en memoria para pruebas automatizadas.
- Flyway es la única fuente de verdad para cambios de esquema.
- Hibernate `ddl-auto=validate`; Hibernate no crea ni modifica tablas.
- `spring.jpa.open-in-view=false` para no mantener sesiones JPA abiertas durante la vista.

## Seguridad

- Spring Security está instalado desde el inicio.
- CSRF permanece habilitado por defecto.
- Solo `/`, `/error`, recursos estáticos y `/actuator/health` son públicos en el esqueleto.
- Las rutas restantes requieren autenticación.
- La autenticación consulta usuarios y roles persistidos con BCrypt. Administración de semilleros comprueba rol vigente y academia. Faltan gestión de cuentas, autorización por recurso para otros módulos, revocación general de sesiones y auditoría.

## Configuración

- `dev`: H2 en archivo local; consulta pública limitada a la academia de demostración.
- `test`: H2 aislada para pruebas.
- `prod`: PostgreSQL mediante `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`.
- No se almacenan credenciales reales en el repositorio.

## Decisiones aplazadas

No se implementa todavía el modelo de entidades académicas, porque faltan por aprobar
el ER completo, restricciones, ciclo de vida de asignaciones y reglas de evaluación.
