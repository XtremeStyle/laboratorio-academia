-- V12 — Escala de notas 0..5.app y ajuste búmero. Enforce en el esquema para
-- que ningún servicio escrito en otro lenguaje o ruta directa pueda grabar notas fuera de rango.
ALTER TABLE version_evaluacion
  ADD CONSTRAINT ck_version_evaluacion_calificacion CHECK (calificacion_base BETWEEN 0 AND 5);

ALTER TABLE puntaje_criterio
  ADD CONSTRAINT ck_puntaje_criterio CHECK (puntaje BETWEEN 0 AND 5);

ALTER TABLE ajuste_individual
  ADD CONSTRAINT ck_ajuste_individual CHECK (ajuste BETWEEN -5 AND 5);

-- Índice de peso para consultas de criterio ponderado
CREATE INDEX IF NOT EXISTS idx_criterio_evaluacion_peso ON criterio_evaluacion(peso) WHERE peso IS NOT NULL;
