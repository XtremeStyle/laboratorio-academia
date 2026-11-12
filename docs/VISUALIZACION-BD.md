# CHANGE // SPACE-LAB OS — Visor de base de datos

Generado a partir de PostgreSQL `change_space_lab` (V11). Tablas: **50**. Datos reales verificados.

## Conteo por tabla (en este momento)

```
academia │ 1 filas
adjunto_entrega │ 0 filas
ajuste_individual │ 0 filas
alerta_rendimiento │ 0 filas
app_metadata │ 1 filas
asignacion_activa │ 9 filas
asignacion_mision │ 0 filas
asignacion_semillero │ 11 filas
auditoria │ 25 filas
autorizacion_tardia │ 0 filas
chat_privado │ 0 filas
chat_tripulacion │ 0 filas
competencia │ 0 filas
competencia_semillero │ 0 filas
conversacion │ 0 filas
criterio_competencia │ 0 filas
criterio_evaluacion │ 0 filas
curso │ 2 filas
curso_semillero │ 2 filas
diseno_laboratorio │ 0 filas
entrega_etapa │ 0 filas
etapa_mision │ 0 filas
evaluacion │ 0 filas
evento_insignia │ 0 filas
flyway_schema_history │ 11 filas
insignia │ 3 filas
insignia_usuario │ 0 filas
leccion_curso │ 16 filas
matricula_curso │ 3 filas
membresia_activa │ 0 filas
membresia_tripulacion │ 0 filas
mensaje │ 0 filas
mision │ 0 filas
mision_semillero │ 0 filas
notificacion │ 0 filas
participante_asignacion │ 0 filas
participante_conversacion │ 0 filas
periodo_asignacion │ 11 filas
periodo_membresia │ 0 filas
progreso_leccion │ 18 filas
puntaje_criterio │ 0 filas
rol │ 3 filas
semillero │ 2 filas
solicitud_cuenta │ 1 filas
tripulacion │ 0 filas
usuario │ 18 filas
usuario_rol │ 21 filas
version_entrega │ 0 filas
version_evaluacion │ 0 filas
xp_movimiento │ 0 filas

## Árbol lógico (jerarquía FK)

```text
academia
├── usuario
│     ├── usuario_rol ←── rol
│     └── asignacion_semillero
│           ├── periodo_asignacion
│           ├── asignacion_activa
│           └── tripulacion ←── asignacion_tripulacion
│                 ├── periodo_membresia
│                 └── membresia_activa
├── semillero
│     ├── curso_semillero ←── curso
│     │     └── leccion_curso
│     │           └── progreso_leccion
│     └── mision_semillero ←── mision
│           ├── etapa_mision
│           ├── asignacion_mision
│           ├── participante_asignacion
│           ├── entrega_etapa
│           │     └── version_entrega
│           │           └── evaluacion
│           │                 └── version_evaluacion
│           │                       └── puntaje_criterio
│           │                             └── criterio_evaluacion
│           │                                   └── criterio_competencia
│           │                                         └── competencia
│           └── autorizacion_tardia
├── matricula_curso ←── curso_semillero
├── notificacion
├── auditoria
├── cambio_de_rol?  (no aplica)
├── adjunto_entrega ←── version_entrega
├── chat_*
└── flyway_schema_history
```

## Respaldos verificados

| Fecha | Backup | SHA-256 |
|---|---|---|
| 2026-11-11 20:30 | `respaldos/datos-antes-v11-20261111-203038.tar.gz` | `datos-antes-v11-20261111-203038.tar.gz.sha256` |
| 2026-11-11 20:30 | `respaldos/pg-antes-v11-20261111-203043.dump` | (pg_dump nativo) |

## Notas

- `xp_movimiento` (V10) es la tabla activa de movimientos de XP; la tabla legado `movimiento_xp` (V2) se eliminó en V11.
- Este documento se regenera desde `psql` consultando el esquema; no commitea datos.
