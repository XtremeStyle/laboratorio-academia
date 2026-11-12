-- V12 — Escala de notas 0..5.ante/ajusta y peso de criterio en el esquema. Cubre PG y H2.
ALTER TABLE version_evaluacion
  ADD CONSTRAINT ck_version_evaluacion_calificacion CHECK (calificacion_base BETWEEN 0 AND 5);

ALTER TABLE puntaje_criterio
  ADD CONSTRAINT ck_puntaje_criterio CHECK (puntaje BETWEEN 0 AND 5);

ALTER TABLE ajuste_individual
  ADD CONSTRAINT ck_ajuste_individual CHECK (ajuste BETWEEN -5 AND 5);

CREATE INDEX IF NOT EXISTS idx_criterio_evaluacion_peso ON criterio_evaluacion(peso);
