# CHANGE // SPACE-LAB OS — Estado de la entrega

**Estado**: Fase de desarrollo (no es producción). Funciona, se compila, se instala, persiste datos en PostgreSQL 18 y pasa las 66 pruebas automatizadas.

## ¿Qué tienes en este momento?

- **Backend funcional** con Spring Boot 3.5, Java 21, Flyway V1→V11.
- **Dos instancias** en local: `:8080` (principal) y `:8090` (entrega empaquetada).
- **PostgreSQL 18** local viva (`change-space-lab-postgres.service` en su sesión de usuario).
- **Autenticación acotada** (`/registro` + `/administracion/solicitudes`) y CSRF/CSP activos.
- **REST API + Swagger** en `/api/**` y `/v3/api-docs` (8 rutas detectadas).
- **66/66 pruebas verdes** (integración, concurrencia, migración, seguridad, accesibilidad).
- **Dos laboratorios interactivos** (Robótica: edición de circuitos, PWM, estaciones; Astronomía: órbita, cohetes, fases de la Luna).
- **Cuentas dev** documentadas para verificar: `dev-admin/dev-prof/dev-estu1/dev-estu2 @espacio.local`.

## Qué NO está listo para producción

- TLS/HTTPS, auth de PostgreSQL por contraseña, monitorización centralizada.
- Pentest real y escaneo de dependencias automatizado.
- WCAG 2.2 AA completo.
- Repositorio público: `github.com/XtremeStyle/laboratorio-academia.git` lleva solo el commit de inicialización; el commit `6c21bf7` lo actualiza pero **aún no se subió**.
- Indice/benchmarking de consultas (Flyway OK pero sin `EXPLAIN` analizado).

## Cómo arrancar una nueva instancia

```bash
# desde /home/jose/Projects/CHANGE-SPACE-LAB
./install-space-lab.sh              # instala Java 21, compila, crea change-space-lab
open /usr/share/applications/change-space-lab.desktop
```

## Verificación

```bash
cd /home/jose/Projects/CHANGE-SPACE-LAB
./docs/confirmar.sh
```
