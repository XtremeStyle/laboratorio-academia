# Contribuir a CHANGE // SPACE-LAB OS

Gracias por tu interés. Este documento resume cómo preparar el entorno y proponer cambios.

## Entorno

- Java 21 o superior; `curl` y `tar` para el lanzador Linux.
- Internet en la primera compilación (descarga Apache Maven y dependencias).
- PostgreSQL 18 opcional; H2 se usa por defecto.

```bash
./mvnw clean package   # compila y ejecuta la batería de pruebas
./run-laboratorio.sh   # arranca en http://localhost:8080 (H2)
```

## Flujo de trabajo

1. Crea una rama desde `main` con la convención `<tipo>/<tema>` (`feature/`, `fix/`, `docs/`).
2. Mantén los cambios pequeños y revisables. Sin debilitar ni saltar pruebas.
3. Ejecuta `./mvnw test` antes de abrir el pull request; la batería debe quedar en verde (69 pruebas).
4. Mensajes de commit con Conventional Commits (`feat:`, `fix:`, `docs:`…).
5. Nunca subas secretos, `.env*` ni datos de `data/`; `.gitignore` ya los excluye.

## Estilo

- Java 21, Spring Boot 3.x, Maven, Thymeleaf. Sin dependencias nuevas sin justificar.
- Idioma del código y la documentación: español.
- Ver [`SECURITY.md`](SECURITY.md) para el modelo de seguridad y [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md) para las decisiones de diseño.
