CREATE TABLE chat_tripulacion (
 tripulacion_id UUID PRIMARY KEY, academia_id UUID NOT NULL, conversacion_id UUID NOT NULL UNIQUE,
 FOREIGN KEY(tripulacion_id,academia_id) REFERENCES tripulacion(id,academia_id),
 FOREIGN KEY(conversacion_id,academia_id) REFERENCES conversacion(id,academia_id)
);
CREATE TABLE chat_privado (
 id UUID PRIMARY KEY, academia_id UUID NOT NULL, conversacion_id UUID NOT NULL UNIQUE,
 usuario_1_id UUID NOT NULL, usuario_2_id UUID NOT NULL,
 FOREIGN KEY(usuario_1_id,academia_id) REFERENCES usuario(id,academia_id),
 FOREIGN KEY(usuario_2_id,academia_id) REFERENCES usuario(id,academia_id),
 FOREIGN KEY(conversacion_id,academia_id) REFERENCES conversacion(id,academia_id),
 CHECK(usuario_1_id <> usuario_2_id), UNIQUE(academia_id,usuario_1_id,usuario_2_id)
);
