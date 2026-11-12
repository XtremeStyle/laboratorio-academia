-- Libro de movimientos XP: historial auditável de cada ajuste de experiencia.
CREATE TABLE xp_movimiento (
 id UUID PRIMARY KEY,
 academia_id UUID NOT NULL REFERENCES academia(id),
 usuario_id UUID NOT NULL REFERENCES usuario(id),
 asignacion_id UUID NOT NULL REFERENCES asignacion_mision(id),
 mision_id UUID NOT NULL REFERENCES mision(id),
 evaluacion_id UUID REFERENCES evaluacion(id),
 cantidad INT NOT NULL,
 saldo INT NOT NULL CHECK (saldo >= 0),
 motivo VARCHAR(20) NOT NULL,
 creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT ck_xp_motivo CHECK (motivo IN ('EVALUACION','CORRECCION'))
);
CREATE INDEX idx_xp_mov_usuario ON xp_movimiento(usuario_id, creado_en DESC);
CREATE INDEX idx_xp_mov_asignacion ON xp_movimiento(asignacion_id);
