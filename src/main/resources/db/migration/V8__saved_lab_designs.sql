CREATE TABLE diseno_laboratorio (
 id UUID PRIMARY KEY,
 usuario_id UUID NOT NULL REFERENCES usuario(id),
 tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('CIRCUITO','COHETE')),
 contenido TEXT NOT NULL,
 actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE (usuario_id,tipo)
);
