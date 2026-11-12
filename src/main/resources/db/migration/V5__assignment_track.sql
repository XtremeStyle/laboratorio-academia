-- Las asignaciones históricas solo se migran si su Misión tiene un semillero inequívoco.
ALTER TABLE asignacion_mision ADD COLUMN semillero_destino_id UUID;
UPDATE asignacion_mision SET semillero_destino_id=(SELECT ms.semillero_id FROM mision_semillero ms WHERE ms.mision_id=asignacion_mision.mision_id)
 WHERE (SELECT COUNT(*) FROM mision_semillero ms WHERE ms.mision_id=asignacion_mision.mision_id)=1;
ALTER TABLE asignacion_mision ALTER COLUMN semillero_destino_id SET NOT NULL;
ALTER TABLE asignacion_mision ADD CONSTRAINT fk_asignacion_mision_semillero FOREIGN KEY(mision_id,semillero_destino_id) REFERENCES mision_semillero(mision_id,semillero_id);
