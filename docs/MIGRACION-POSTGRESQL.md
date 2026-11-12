# Migración de H2 (desarrollo, legado) → PostgreSQL 18 local

**Estado: COMPLETADA (11 nov 2026).** Ambas instancias (8080 y la entrega en 8090) ya operan contra PostgreSQL real. La base H2 queda únicamente como respaldo privado en `…/PROYECTO/data/`.

## Entorno real

- Puerto `5433` en `127.0.0.1`, autenticación `trust` (solo desarrollo; para producción: TLS, rol de menor privilegio y contraseña).
- Clúster en `~/.local/share/space-lab/postgres/data`, propiedad de `jose`, con `systemd --user` habilitado (`change-space-lab-postgres.service`).
- Base `change_space_lab`, propietario `spaceLab`.
- Servicio PostgreSQL administrado por `systemctl --user` (arranque automático al iniciar sesión de usuario).

## Datos reales importados (verificados)

| Tabla | Filas |
|---|---|
| `usuario` | 14 |
| `progreso_leccion` | 18 |
| `matricula_curso` | 3 |
| `auditoria` | 25 |
| `flyway_schema_history` | 10 migraciones (V1–V10) |

## Comandos

```bash
# estado del servidor
systemctl --user status change-space-lab-postgres.service
/usr/bin/pg_isready -h 127.0.0.1 -p 5433 -U spaceLab
/usr/bin/psql -h 127.0.0.1 -p 5433 -U spaceLab -d change_space_lab

# backup lógico completo
pg_dump -h 127.0.0.1 -p 5433 -U spaceLab -Fc change_space_lab > respaldos/change_space_lab-$(date +%F).dump

# restaurar
pg_restore -h 127.0.0.1 -p 5433 -U spaceLab -d change_space_lab respaldos/archivo.dump
```

## Inicio de la app contra PostgreSQL

```bash
cd ~/Projects/CHANGE-SPACE-LAB
./INICIAR-CON-POSTGRES.sh    # puerto 8091 por defecto; PORT=8090 para otro
```

La instancia principal de desarrollo escucha en `8080` contra PostgreSQL; la entrega empaquetada escucha en `8090` también contra PostgreSQL (misma base).
