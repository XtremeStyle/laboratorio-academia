package com.laboratorio.persistence;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LearningCoursesMigrationTest {
    @Test
    void createsPersistentRoboticsAndCosmosLearningPaths() {
        String url = "jdbc:h2:mem:learning_" + UUID.randomUUID().toString().replace("-", "") + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration").load().migrate();
        JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(url, "sa", ""));
        assertEquals(2, db.queryForObject("SELECT COUNT(*) FROM curso", Integer.class));
        assertEquals(16, db.queryForObject("SELECT COUNT(*) FROM leccion_curso", Integer.class));
        assertEquals(8, db.queryForObject("SELECT COUNT(*) FROM leccion_curso WHERE curso_id='c0000000-0000-4000-8000-000000000001'", Integer.class));
        assertEquals(8, db.queryForObject("SELECT COUNT(*) FROM leccion_curso WHERE curso_id='c0000000-0000-4000-8000-000000000002'", Integer.class));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () ->
            db.update("INSERT INTO progreso_leccion(id,matricula_id,curso_id,leccion_id,estado) VALUES (? ,? ,? ,? ,'COMPLETADA')",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
    }
}
