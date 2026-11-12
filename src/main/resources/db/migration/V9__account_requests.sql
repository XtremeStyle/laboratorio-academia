-- Solicitudes de acceso al entorno privado (autoservicio acotado con aprobación administrativa).
CREATE TABLE solicitud_cuenta (
 id UUID PRIMARY KEY,
 academia_id UUID NOT NULL REFERENCES academia(id),
 correo VARCHAR(254) NOT NULL,
 nombre VARCHAR(120) NOT NULL,
 apellido VARCHAR(120) NOT NULL,
 rol_solicitado VARCHAR(20) NOT NULL,
 mensaje VARCHAR(500),
 estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
 creada_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 resuelta_en TIMESTAMP WITH TIME ZONE,
 resuelta_por UUID REFERENCES usuario(id),
 CONSTRAINT ck_solicitud_correo CHECK (correo = LOWER(TRIM(correo)) AND correo <> ''),
 CONSTRAINT ck_solicitud_rol CHECK (rol_solicitado IN ('ESTUDIANTE','PROFESOR')),
 CONSTRAINT ck_solicitud_estado CHECK (estado IN ('PENDIENTE','APROBADA','RECHAZADA')),
 UNIQUE (academia_id, correo)
);
CREATE INDEX idx_solicitud_estado ON solicitud_cuenta(estado);
