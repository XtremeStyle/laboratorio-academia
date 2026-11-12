-- Se conserva la concesión y se registra cada cambio de vigencia.
ALTER TABLE insignia_usuario ADD COLUMN revocada_en TIMESTAMP WITH TIME ZONE;
CREATE TABLE evento_insignia (
 id UUID PRIMARY KEY, insignia_usuario_id UUID NOT NULL REFERENCES insignia_usuario(id),
 accion VARCHAR(20) NOT NULL CHECK (accion IN ('CONCEDIDA','REVOCADA','REACTIVADA')),
 ocurrido_en TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_evento_insignia ON evento_insignia(insignia_usuario_id,ocurrido_en);
INSERT INTO insignia(id,codigo,nombre,descripcion,estado) VALUES
 ('10000000-0000-0000-0000-000000000001','PRIMERA_MISION','Primera Misión','Primera Misión con entrega final evaluada.','ACTIVA'),
 ('10000000-0000-0000-0000-000000000002','CINCO_MISIONES','Cinco Misiones','Cinco Misiones con entrega final evaluada.','ACTIVA'),
 ('10000000-0000-0000-0000-000000000003','NOTA_PERFECTA','Nota perfecta','Una Misión con promedio personal de 5,0.','ACTIVA');
