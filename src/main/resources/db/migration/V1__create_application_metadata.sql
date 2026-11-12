-- Tabla técnica mínima para verificar que Flyway puede aplicar migraciones.
-- No representa todavía el modelo académico del dominio.
CREATE TABLE app_metadata (
    metadata_key VARCHAR(80) PRIMARY KEY,
    metadata_value VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO app_metadata (metadata_key, metadata_value)
VALUES ('schema_version', '1');
