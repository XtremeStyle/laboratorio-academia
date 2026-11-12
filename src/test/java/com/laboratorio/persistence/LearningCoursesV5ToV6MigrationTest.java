package com.laboratorio.persistence;

import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** La migración V6 se aplica sobre una base V5 con datos existentes sin perderlos ni alterarlos. */
class LearningCoursesV5ToV6MigrationTest {
    private static final String URL_BASE = "jdbc:h2:mem:v5v6_%s;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

    @Test
    void migratesExistingV5DataToV6WithoutDataLoss() {
        String url = URL_BASE.formatted(UUID.randomUUID().toString().replace("-", ""));
        // 1. Base al estado V5, exactamente el esquema anterior a los cursos.
        Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("5")).load().migrate();
        JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(url, "sa", ""));
        List<String> versions = db.queryForList("SELECT \"version\" FROM \"flyway_schema_history\"", String.class);
        assertEquals(6, versions.size());
        assertEquals("5", versions.getLast());
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE upper(table_name)='CURSO'", Integer.class));

        // 2. Datos previos existentes antes de la migración.
        UUID academy=UUID.randomUUID(), student=UUID.randomUUID(), track=UUID.randomUUID();
        UUID assignment=UUID.randomUUID(), period=UUID.randomUUID();
        db.update("INSERT INTO academia(id,nombre,estado) VALUES (?,?,'ACTIVA')",academy,"Academia previa");
        db.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,?,'Estudiante','Previo','hash','ACTIVO')",student,academy,"previo-v5@test.invalid");
        UUID role=db.queryForObject("SELECT id FROM rol WHERE codigo='ESTUDIANTE'",UUID.class);
        db.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) VALUES (?,?,?)",UUID.randomUUID(),student,role);
        db.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'ROBOTICA','Robótica previa','ACTIVO')",track,academy);
        db.update("INSERT INTO asignacion_semillero(id,academia_id,usuario_id,semillero_id) VALUES (?,?,?,?)",assignment,academy,student,track);
        db.update("INSERT INTO periodo_asignacion(id,asignacion_id,inicio,registrado_por_id) VALUES (?,?,CURRENT_TIMESTAMP,?)",period,assignment,student);
        db.update("INSERT INTO asignacion_activa(asignacion_id,periodo_id) VALUES (?,?)",assignment,period);

        // 3. Migración completa hasta V6 sobre la base con datos.
        Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration").target(MigrationVersion.fromVersion("6")).load().migrate();
        List<String> versionsAfter = db.queryForList("SELECT \"version\" FROM \"flyway_schema_history\"", String.class);
        assertEquals(7, versionsAfter.size());
        assertEquals("6", versionsAfter.getLast());
        assertEquals(2, db.queryForObject("SELECT COUNT(*) FROM curso", Integer.class));
        assertEquals(12, db.queryForObject("SELECT COUNT(*) FROM leccion_curso", Integer.class));
        // Ningún dato previo se perdió ni se alteró.
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM usuario", Integer.class));
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM semillero", Integer.class));
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM asignacion_activa", Integer.class));
        assertEquals("previo-v5@test.invalid", db.queryForObject("SELECT correo FROM usuario WHERE id=?", String.class, student));
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM matricula_curso", Integer.class));

        // 4. El esquema V6 permite el flujo completo sobre los datos migrados.
        db.update("INSERT INTO curso_semillero(curso_id,semillero_id) VALUES ('c0000000-0000-4000-8000-000000000001',?)",track);
        UUID enrollment=UUID.randomUUID();
        db.update("INSERT INTO matricula_curso(id,curso_id,usuario_id,semillero_id,estado) VALUES (?,'c0000000-0000-4000-8000-000000000001',?,?,'ACTIVA')",enrollment,student,track);
        UUID lesson=db.queryForObject("SELECT id FROM leccion_curso WHERE curso_id='c0000000-0000-4000-8000-000000000001' AND numero=1",UUID.class);
        db.update("INSERT INTO progreso_leccion(id,matricula_id,curso_id,leccion_id,estado,iniciado_en) VALUES (?,?,?,?,?,CURRENT_TIMESTAMP)",UUID.randomUUID(),enrollment,"c0000000-0000-4000-8000-000000000001",lesson,"EN_CURSO");
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM progreso_leccion WHERE matricula_id=?", Integer.class, enrollment));
        Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration").load().migrate();
        assertEquals(16, db.queryForObject("SELECT COUNT(*) FROM leccion_curso", Integer.class));
        assertEquals("EN_CURSO", db.queryForObject("SELECT estado FROM progreso_leccion WHERE matricula_id=?", String.class, enrollment));
        assertEquals("ACTIVA", db.queryForObject("SELECT estado FROM matricula_curso WHERE id=?", String.class, enrollment));
    }
}
