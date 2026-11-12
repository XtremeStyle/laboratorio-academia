-- V11 elimina tablas y el índice huérfano de tablas sin usuarios en el código actual.
-- En el código la tabla activa es xp_movimiento (V10).
DROP INDEX IF EXISTS ix_xp_usuario_semillero;
DROP TABLE IF EXISTS movimiento_xp;
DROP TABLE IF EXISTS rango;
