# Guía de desarrollo

## Ramas

- `main`: versión estable.
- `develop`: integración de trabajo.
- `feature/<tema>`: funcionalidades.
- `fix/<tema>`: correcciones.
- `docs/<tema>`: documentación.

## Comandos

```bash
# Ejecutar aplicación (perfil dev + H2)
./mvnw spring-boot:run

# Ejecutar pruebas
./mvnw clean test

# Empaquetar
./mvnw clean package

# Ejecutar JAR
java -jar target/laboratorio-0.1.0-SNAPSHOT.jar
```

En Windows reemplazar `./mvnw` por `.\mvnw.cmd`.

## Convención de commits

Conventional Commits: `feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`.

## Verificaciones antes de integrar

1. Ejecutar `./mvnw clean test`.
2. Revisar `git diff`.
3. Confirmar que no se añadieron secretos ni archivos `target/`.
4. Usar commits pequeños y descriptivos.
