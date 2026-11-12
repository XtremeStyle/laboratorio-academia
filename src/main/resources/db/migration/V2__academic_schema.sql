-- Esquema académico inicial. Las identidades UUID son suministradas por la aplicación.
-- Las relaciones se conservan; no se aplican borrados en cascada.
CREATE TABLE academia (
 id UUID PRIMARY KEY, nombre VARCHAR(160) NOT NULL, estado VARCHAR(20) NOT NULL,
 creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT ck_academia_estado CHECK (estado IN ('ACTIVA','INACTIVA'))
);
CREATE TABLE usuario (
 id UUID PRIMARY KEY, academia_id UUID NOT NULL REFERENCES academia(id),
 correo VARCHAR(254) NOT NULL UNIQUE, nombre VARCHAR(120) NOT NULL, apellido VARCHAR(120) NOT NULL,
 password_hash VARCHAR(255) NOT NULL, estado VARCHAR(20) NOT NULL,
 creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT ck_usuario_correo CHECK (correo = LOWER(TRIM(correo)) AND correo <> ''),
 CONSTRAINT ck_usuario_estado CHECK (estado IN ('ACTIVO','INACTIVO','BLOQUEADO')),
 UNIQUE (id, academia_id)
);
CREATE TABLE rol (id UUID PRIMARY KEY, codigo VARCHAR(40) NOT NULL UNIQUE, nombre VARCHAR(100) NOT NULL);
CREATE TABLE usuario_rol (
 id UUID PRIMARY KEY, usuario_id UUID NOT NULL REFERENCES usuario(id), rol_id UUID NOT NULL REFERENCES rol(id),
 asignado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE(usuario_id, rol_id)
);
CREATE TABLE semillero (
 id UUID PRIMARY KEY, academia_id UUID NOT NULL REFERENCES academia(id), codigo VARCHAR(50) NOT NULL,
 nombre VARCHAR(160) NOT NULL, descripcion TEXT, estado VARCHAR(20) NOT NULL,
 CONSTRAINT ck_semillero_codigo CHECK (codigo = UPPER(TRIM(codigo)) AND codigo <> ''),
 CONSTRAINT ck_semillero_estado CHECK (estado IN ('ACTIVO','INACTIVO')),
 UNIQUE(academia_id, codigo), UNIQUE(id, academia_id)
);
CREATE TABLE asignacion_semillero (
 id UUID PRIMARY KEY, academia_id UUID NOT NULL, usuario_id UUID NOT NULL, semillero_id UUID NOT NULL,
 creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(usuario_id, academia_id) REFERENCES usuario(id, academia_id),
 FOREIGN KEY(semillero_id, academia_id) REFERENCES semillero(id, academia_id),
 UNIQUE(usuario_id, semillero_id)
);
CREATE TABLE periodo_asignacion (
 id UUID PRIMARY KEY, asignacion_id UUID NOT NULL REFERENCES asignacion_semillero(id),
 inicio TIMESTAMP WITH TIME ZONE NOT NULL, fin TIMESTAMP WITH TIME ZONE,
 registrado_por_id UUID NOT NULL REFERENCES usuario(id),
 CHECK (fin IS NULL OR fin > inicio), UNIQUE(id, asignacion_id)
);
-- Una fila representa el periodo actualmente abierto; su PK impide duplicados concurrentes.
CREATE TABLE asignacion_activa (
 asignacion_id UUID PRIMARY KEY REFERENCES asignacion_semillero(id), periodo_id UUID NOT NULL UNIQUE,
 FOREIGN KEY(periodo_id, asignacion_id) REFERENCES periodo_asignacion(id, asignacion_id)
);
CREATE TABLE tripulacion (
 id UUID PRIMARY KEY, academia_id UUID NOT NULL, semillero_id UUID NOT NULL, nombre VARCHAR(160) NOT NULL,
 descripcion TEXT, estado VARCHAR(20) NOT NULL, creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(semillero_id, academia_id) REFERENCES semillero(id, academia_id),
 CHECK (estado IN ('ACTIVA','INACTIVA')), UNIQUE(semillero_id, nombre), UNIQUE(id, academia_id)
);
CREATE TABLE membresia_tripulacion (
 id UUID PRIMARY KEY, academia_id UUID NOT NULL, tripulacion_id UUID NOT NULL, usuario_id UUID NOT NULL,
 FOREIGN KEY(tripulacion_id, academia_id) REFERENCES tripulacion(id, academia_id),
 FOREIGN KEY(usuario_id, academia_id) REFERENCES usuario(id, academia_id), UNIQUE(tripulacion_id, usuario_id)
);
CREATE TABLE periodo_membresia (
 id UUID PRIMARY KEY, membresia_id UUID NOT NULL REFERENCES membresia_tripulacion(id),
 inicio TIMESTAMP WITH TIME ZONE NOT NULL, fin TIMESTAMP WITH TIME ZONE, funcion VARCHAR(20) NOT NULL,
 registrado_por_id UUID NOT NULL REFERENCES usuario(id), CHECK (fin IS NULL OR fin > inicio),
 CHECK (funcion IN ('PARTICIPANTE','RESPONSABLE')), UNIQUE(id, membresia_id)
);
CREATE TABLE membresia_activa (
 membresia_id UUID PRIMARY KEY REFERENCES membresia_tripulacion(id), periodo_id UUID NOT NULL UNIQUE,
 FOREIGN KEY(periodo_id, membresia_id) REFERENCES periodo_membresia(id, membresia_id)
);
CREATE TABLE mision (
 id UUID PRIMARY KEY, academia_id UUID NOT NULL REFERENCES academia(id), titulo VARCHAR(200) NOT NULL,
 instrucciones TEXT NOT NULL, modalidad VARCHAR(25) NOT NULL, estado VARCHAR(20) NOT NULL,
 creada_por_id UUID NOT NULL, inicio TIMESTAMP WITH TIME ZONE, vencimiento TIMESTAMP WITH TIME ZONE,
 politica_evaluacion VARCHAR(20) NOT NULL, creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(creada_por_id, academia_id) REFERENCES usuario(id, academia_id),
 CHECK (modalidad IN ('INDIVIDUAL','GRUPAL','INTERDISCIPLINARIA')),
 CHECK (estado IN ('BORRADOR','PUBLICADA','CERRADA','CANCELADA')),
 CHECK (politica_evaluacion IN ('POR_ETAPA','SOLO_FINAL')),
 CHECK (inicio IS NULL OR vencimiento IS NULL OR inicio <= vencimiento), UNIQUE(id, academia_id)
);
CREATE TABLE mision_semillero (
 id UUID PRIMARY KEY, academia_id UUID NOT NULL, mision_id UUID NOT NULL, semillero_id UUID NOT NULL,
 FOREIGN KEY(mision_id, academia_id) REFERENCES mision(id, academia_id),
 FOREIGN KEY(semillero_id, academia_id) REFERENCES semillero(id, academia_id), UNIQUE(mision_id, semillero_id)
);
CREATE TABLE etapa_mision (
 id UUID PRIMARY KEY, mision_id UUID NOT NULL REFERENCES mision(id), titulo VARCHAR(200) NOT NULL,
 instrucciones TEXT, orden INTEGER NOT NULL CHECK (orden > 0), obligatoria BOOLEAN NOT NULL,
 es_final BOOLEAN NOT NULL, vencimiento TIMESTAMP WITH TIME ZONE,
 CHECK (NOT es_final OR obligatoria), UNIQUE(mision_id, orden), UNIQUE(id, mision_id)
);
CREATE TABLE criterio_evaluacion (
 id UUID PRIMARY KEY, mision_id UUID NOT NULL REFERENCES mision(id), etapa_id UUID,
 nombre VARCHAR(160) NOT NULL, descripcion TEXT NOT NULL, peso DECIMAL(10,4), orden INTEGER NOT NULL,
 FOREIGN KEY(etapa_id, mision_id) REFERENCES etapa_mision(id, mision_id), CHECK (peso IS NULL OR peso >= 0),
 CHECK (orden > 0), UNIQUE(id, mision_id)
);
CREATE TABLE asignacion_mision (
 id UUID PRIMARY KEY, academia_id UUID NOT NULL, mision_id UUID NOT NULL, destinatario_usuario_id UUID,
 destinatario_tripulacion_id UUID, estado VARCHAR(20) NOT NULL, asignada_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(mision_id, academia_id) REFERENCES mision(id, academia_id),
 FOREIGN KEY(destinatario_usuario_id, academia_id) REFERENCES usuario(id, academia_id),
 FOREIGN KEY(destinatario_tripulacion_id, academia_id) REFERENCES tripulacion(id, academia_id),
 CHECK ((destinatario_usuario_id IS NOT NULL AND destinatario_tripulacion_id IS NULL) OR
        (destinatario_usuario_id IS NULL AND destinatario_tripulacion_id IS NOT NULL)),
 CHECK (estado IN ('ACTIVA','COMPLETADA','CANCELADA')),
 UNIQUE(mision_id, destinatario_usuario_id), UNIQUE(mision_id, destinatario_tripulacion_id), UNIQUE(id, mision_id)
);
CREATE TABLE participante_asignacion (
 id UUID PRIMARY KEY, asignacion_id UUID NOT NULL REFERENCES asignacion_mision(id), usuario_id UUID NOT NULL REFERENCES usuario(id),
 incorporado_en TIMESTAMP WITH TIME ZONE NOT NULL, retirado_en TIMESTAMP WITH TIME ZONE,
 CHECK(retirado_en IS NULL OR retirado_en > incorporado_en), UNIQUE(asignacion_id, usuario_id)
);
CREATE TABLE entrega_etapa (
 id UUID PRIMARY KEY, mision_id UUID NOT NULL, asignacion_id UUID NOT NULL, etapa_id UUID NOT NULL,
 creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(asignacion_id, mision_id) REFERENCES asignacion_mision(id, mision_id),
 FOREIGN KEY(etapa_id, mision_id) REFERENCES etapa_mision(id, mision_id), UNIQUE(asignacion_id, etapa_id)
);
CREATE TABLE autorizacion_tardia (
 id UUID PRIMARY KEY, entrega_etapa_id UUID NOT NULL REFERENCES entrega_etapa(id), concedida_por_id UUID NOT NULL REFERENCES usuario(id),
 motivo TEXT NOT NULL, concedida_en TIMESTAMP WITH TIME ZONE NOT NULL, valida_hasta TIMESTAMP WITH TIME ZONE,
 revocada_en TIMESTAMP WITH TIME ZONE, CHECK(valida_hasta IS NULL OR valida_hasta >= concedida_en),
 CHECK(revocada_en IS NULL OR revocada_en >= concedida_en), UNIQUE(id, entrega_etapa_id)
);
CREATE TABLE version_entrega (
 id UUID PRIMARY KEY, entrega_etapa_id UUID NOT NULL REFERENCES entrega_etapa(id), numero INTEGER NOT NULL CHECK(numero > 0),
 autor_id UUID NOT NULL REFERENCES usuario(id), contenido TEXT, enviada_en TIMESTAMP WITH TIME ZONE NOT NULL,
 autorizacion_id UUID, FOREIGN KEY(autorizacion_id, entrega_etapa_id) REFERENCES autorizacion_tardia(id, entrega_etapa_id),
 UNIQUE(entrega_etapa_id, numero), UNIQUE(id, entrega_etapa_id)
);
CREATE TABLE adjunto_entrega (
 id UUID PRIMARY KEY, version_id UUID NOT NULL REFERENCES version_entrega(id), nombre_original VARCHAR(255) NOT NULL,
 clave_almacenamiento VARCHAR(512) NOT NULL UNIQUE, tipo_mime VARCHAR(255) NOT NULL,
 tamano BIGINT NOT NULL CHECK(tamano >= 0), hash VARCHAR(128) NOT NULL
);
CREATE TABLE evaluacion (
 id UUID PRIMARY KEY, entrega_etapa_id UUID NOT NULL UNIQUE REFERENCES entrega_etapa(id), UNIQUE(id, entrega_etapa_id)
);
CREATE TABLE version_evaluacion (
 id UUID PRIMARY KEY, evaluacion_id UUID NOT NULL, entrega_etapa_id UUID NOT NULL, numero INTEGER NOT NULL CHECK(numero > 0),
 version_entrega_id UUID NOT NULL, evaluador_id UUID NOT NULL REFERENCES usuario(id),
 calificacion_base DECIMAL(12,4) NOT NULL, observacion TEXT, registrada_en TIMESTAMP WITH TIME ZONE NOT NULL,
 FOREIGN KEY(evaluacion_id, entrega_etapa_id) REFERENCES evaluacion(id, entrega_etapa_id),
 FOREIGN KEY(version_entrega_id, entrega_etapa_id) REFERENCES version_entrega(id, entrega_etapa_id), UNIQUE(evaluacion_id, numero)
);
CREATE TABLE puntaje_criterio (
 id UUID PRIMARY KEY, version_evaluacion_id UUID NOT NULL REFERENCES version_evaluacion(id),
 criterio_id UUID NOT NULL REFERENCES criterio_evaluacion(id), puntaje DECIMAL(12,4) NOT NULL, comentario TEXT,
 UNIQUE(version_evaluacion_id, criterio_id)
);
CREATE TABLE ajuste_individual (
 id UUID PRIMARY KEY, version_evaluacion_id UUID NOT NULL REFERENCES version_evaluacion(id), usuario_id UUID NOT NULL REFERENCES usuario(id),
 ajuste DECIMAL(12,4) NOT NULL, justificacion TEXT NOT NULL CHECK (TRIM(justificacion) <> ''), UNIQUE(version_evaluacion_id, usuario_id)
);
CREATE TABLE competencia (
 id UUID PRIMARY KEY, academia_id UUID NOT NULL REFERENCES academia(id), codigo VARCHAR(50) NOT NULL,
 nombre VARCHAR(160) NOT NULL, descripcion TEXT, estado VARCHAR(20) NOT NULL,
 CHECK(estado IN ('ACTIVA','INACTIVA')), UNIQUE(academia_id, codigo), UNIQUE(id, academia_id)
);
CREATE TABLE competencia_semillero (
 id UUID PRIMARY KEY, academia_id UUID NOT NULL, competencia_id UUID NOT NULL, semillero_id UUID NOT NULL,
 FOREIGN KEY(competencia_id, academia_id) REFERENCES competencia(id, academia_id),
 FOREIGN KEY(semillero_id, academia_id) REFERENCES semillero(id, academia_id), UNIQUE(competencia_id, semillero_id)
);
CREATE TABLE criterio_competencia (
 id UUID PRIMARY KEY, criterio_id UUID NOT NULL REFERENCES criterio_evaluacion(id), competencia_id UUID NOT NULL REFERENCES competencia(id),
 contribucion DECIMAL(10,4), CHECK(contribucion IS NULL OR contribucion >= 0), UNIQUE(criterio_id, competencia_id)
);
CREATE TABLE alerta_rendimiento (
 id UUID PRIMARY KEY, usuario_id UUID NOT NULL REFERENCES usuario(id), competencia_id UUID REFERENCES competencia(id),
 semillero_id UUID REFERENCES semillero(id), tipo VARCHAR(50) NOT NULL, estado VARCHAR(20) NOT NULL,
 generada_en TIMESTAMP WITH TIME ZONE NOT NULL, resuelta_en TIMESTAMP WITH TIME ZONE, detalle TEXT NOT NULL,
 CHECK(estado IN ('ABIERTA','RESUELTA')), CHECK(resuelta_en IS NULL OR resuelta_en >= generada_en)
);
CREATE TABLE movimiento_xp (
 id UUID PRIMARY KEY, usuario_id UUID NOT NULL REFERENCES usuario(id), semillero_id UUID REFERENCES semillero(id),
 cantidad INTEGER NOT NULL CHECK(cantidad <> 0), motivo TEXT NOT NULL, origen_tipo VARCHAR(50) NOT NULL, origen_id UUID NOT NULL,
 clave_idempotencia VARCHAR(160) NOT NULL UNIQUE, creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE rango (
 id UUID PRIMARY KEY, codigo VARCHAR(50) NOT NULL UNIQUE, nombre VARCHAR(100) NOT NULL,
 umbral_xp INTEGER NOT NULL CHECK(umbral_xp >= 0), estado VARCHAR(20) NOT NULL CHECK(estado IN ('ACTIVO','INACTIVO'))
);
CREATE TABLE insignia (
 id UUID PRIMARY KEY, codigo VARCHAR(50) NOT NULL UNIQUE, nombre VARCHAR(100) NOT NULL, descripcion TEXT,
 estado VARCHAR(20) NOT NULL CHECK(estado IN ('ACTIVA','INACTIVA'))
);
CREATE TABLE insignia_usuario (
 id UUID PRIMARY KEY, insignia_id UUID NOT NULL REFERENCES insignia(id), usuario_id UUID NOT NULL REFERENCES usuario(id),
 concedida_en TIMESTAMP WITH TIME ZONE NOT NULL, motivo TEXT NOT NULL, UNIQUE(insignia_id, usuario_id)
);
CREATE TABLE notificacion (
 id UUID PRIMARY KEY, destinatario_id UUID NOT NULL REFERENCES usuario(id), tipo VARCHAR(50) NOT NULL,
 contenido TEXT NOT NULL, creada_en TIMESTAMP WITH TIME ZONE NOT NULL, leida_en TIMESTAMP WITH TIME ZONE,
 CHECK(leida_en IS NULL OR leida_en >= creada_en)
);
CREATE TABLE auditoria (
 id UUID PRIMARY KEY, actor_id UUID REFERENCES usuario(id), accion VARCHAR(100) NOT NULL, recurso_tipo VARCHAR(100) NOT NULL,
 recurso_id UUID NOT NULL, ocurrido_en TIMESTAMP WITH TIME ZONE NOT NULL, resultado VARCHAR(50) NOT NULL,
 correlacion_id UUID, detalles TEXT
);
CREATE TABLE conversacion (
 id UUID PRIMARY KEY, academia_id UUID NOT NULL REFERENCES academia(id), tipo VARCHAR(50) NOT NULL,
 creada_en TIMESTAMP WITH TIME ZONE NOT NULL, UNIQUE(id, academia_id)
);
CREATE TABLE participante_conversacion (
 id UUID PRIMARY KEY, academia_id UUID NOT NULL, conversacion_id UUID NOT NULL, usuario_id UUID NOT NULL,
 unido_en TIMESTAMP WITH TIME ZONE NOT NULL, salido_en TIMESTAMP WITH TIME ZONE,
 FOREIGN KEY(conversacion_id, academia_id) REFERENCES conversacion(id, academia_id),
 FOREIGN KEY(usuario_id, academia_id) REFERENCES usuario(id, academia_id),
 CHECK(salido_en IS NULL OR salido_en > unido_en), UNIQUE(conversacion_id, usuario_id), UNIQUE(conversacion_id, usuario_id, academia_id)
);
CREATE TABLE mensaje (
 id UUID PRIMARY KEY, academia_id UUID NOT NULL, conversacion_id UUID NOT NULL, autor_id UUID NOT NULL,
 contenido TEXT NOT NULL, enviado_en TIMESTAMP WITH TIME ZONE NOT NULL, editado_en TIMESTAMP WITH TIME ZONE,
 eliminado_en TIMESTAMP WITH TIME ZONE,
 FOREIGN KEY(conversacion_id, autor_id, academia_id) REFERENCES participante_conversacion(conversacion_id, usuario_id, academia_id),
 CHECK(editado_en IS NULL OR editado_en >= enviado_en), CHECK(eliminado_en IS NULL OR eliminado_en >= enviado_en)
);
CREATE INDEX ix_asignacion_usuario ON asignacion_semillero(usuario_id);
CREATE INDEX ix_periodo_asignacion ON periodo_asignacion(asignacion_id, inicio);
CREATE INDEX ix_membresia_usuario ON membresia_tripulacion(usuario_id);
CREATE INDEX ix_mision_estado_fecha ON mision(academia_id, estado, vencimiento);
CREATE INDEX ix_asignacion_mision_usuario ON asignacion_mision(destinatario_usuario_id, estado);
CREATE INDEX ix_asignacion_mision_tripulacion ON asignacion_mision(destinatario_tripulacion_id, estado);
CREATE INDEX ix_version_entrega_fecha ON version_entrega(entrega_etapa_id, enviada_en);
CREATE INDEX ix_alerta_usuario ON alerta_rendimiento(usuario_id, estado);
CREATE INDEX ix_xp_usuario_semillero ON movimiento_xp(usuario_id, semillero_id);
CREATE INDEX ix_notificacion_destinatario ON notificacion(destinatario_id, leida_en);
CREATE INDEX ix_mensaje_conversacion ON mensaje(conversacion_id, enviado_en);

INSERT INTO rol(id, codigo, nombre) VALUES
 ('00000000-0000-0000-0000-000000000001', 'ADMINISTRADOR', 'Administrador'),
 ('00000000-0000-0000-0000-000000000002', 'PROFESOR', 'Profesor'),
 ('00000000-0000-0000-0000-000000000003', 'ESTUDIANTE', 'Estudiante');
