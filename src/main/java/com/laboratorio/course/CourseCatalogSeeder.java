package com.laboratorio.course;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vincula el catálogo estable a los semilleros de cada academia sin duplicar relaciones.
 * Se ejecuta al arranque y también al crear cada semillero nuevo, de modo que los
 * semilleros creados después del arranque reciban sus rutas formativas asociadas.
 */
@Component
@Order(20)
public class CourseCatalogSeeder implements ApplicationRunner {
    private final JdbcTemplate db;
    public CourseCatalogSeeder(JdbcTemplate db) { this.db = db; }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<UUID> tracks = db.query(
            "SELECT id FROM semillero WHERE estado='ACTIVO'", (rs, i) -> rs.getObject(1, UUID.class));
        tracks.forEach(this::link);
    }

    /** Asocia el catálogo al semillero indicado según su código. Idempotente. */
    @Transactional
    public void link(UUID semilleroId) {
        db.update("""
            INSERT INTO curso_semillero(curso_id,semillero_id)
            SELECT c.id,s.id FROM curso c JOIN semillero s ON s.id=?
            WHERE s.estado='ACTIVO' AND (
              (c.codigo='ROBOTICA-INICIAL' AND s.codigo='ROBOTICA') OR
              (c.codigo='COSMOS-INICIAL' AND s.codigo IN ('ASTRONOMIA','COSMOS')))
              AND c.estado='PUBLICADO'
              AND NOT EXISTS (SELECT 1 FROM curso_semillero cs
                              WHERE cs.curso_id=c.id AND cs.semillero_id=s.id)
            """, semilleroId);
    }
}
