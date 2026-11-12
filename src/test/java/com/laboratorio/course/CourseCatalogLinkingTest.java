package com.laboratorio.course;

import com.laboratorio.research.SemilleroAdministration;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Los semilleros creados después del arranque también reciben sus cursos asociados. */
@SpringBootTest
@ActiveProfiles("test")
class CourseCatalogLinkingTest {
    @Autowired JdbcTemplate db;
    @Autowired SemilleroAdministration tracks;
    @Autowired CourseCatalogSeeder catalog;
    @Autowired CourseService courses;

    private UUID academyWithAdmin(String email) {
        UUID academy=UUID.randomUUID(), admin=UUID.randomUUID();
        db.update("INSERT INTO academia(id,nombre,estado) VALUES (?,?,'ACTIVA')",academy,"Academia de enlaces");
        db.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,?,'Admin','Enlaces','hash','ACTIVO')",admin,academy,email);
        UUID role=db.queryForObject("SELECT id FROM rol WHERE codigo='ADMINISTRADOR'",UUID.class);
        db.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) VALUES (?,?,?)",UUID.randomUUID(),admin,role);
        return academy;
    }

    @Test
    @Transactional
    void newlyCreatedTrackReceivesLinkedCoursesWithoutRestart() {
        String email="admin-enlaces-"+UUID.randomUUID()+"@test.invalid";
        UUID academy=academyWithAdmin(email);
        // Creado después del arranque de la aplicación: la asociación debe ocurrir en el alta.
        tracks.create(email,"ROBOTICA","Robótica creada en caliente","Semillero de prueba de asociación dinámica.");
        UUID track=db.queryForObject("SELECT id FROM semillero WHERE academia_id=? AND codigo='ROBOTICA'",UUID.class,academy);
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM curso_semillero WHERE semillero_id=? AND curso_id='c0000000-0000-4000-8000-000000000001'",Integer.class,track));

        tracks.create(email,"ASTRONOMIA","Astronomía creada en caliente","Semillero Cosmos de prueba.");
        UUID cosmos=db.queryForObject("SELECT id FROM semillero WHERE academia_id=? AND codigo='ASTRONOMIA'",UUID.class,academy);
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM curso_semillero WHERE semillero_id=? AND curso_id='c0000000-0000-4000-8000-000000000002'",Integer.class,cosmos));
    }

    @Test
    @Transactional
    void unlinkedTrackCodeGetsNoCoursesAndLinkIsIdempotent() {
        String email="admin-sin-ruta-"+UUID.randomUUID()+"@test.invalid";
        UUID academy=academyWithAdmin(email);
        tracks.create(email,"FISICA","Física sin ruta asociada","Código sin curso en el catálogo estable.");
        UUID track=db.queryForObject("SELECT id FROM semillero WHERE academia_id=? AND codigo='FISICA'",UUID.class,academy);
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM curso_semillero WHERE semillero_id=?",Integer.class,track));
        // Relanzar el vinculado no duplica relaciones.
        catalog.link(track);
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM curso_semillero WHERE semillero_id=?",Integer.class,track));
        UUID other=UUID.randomUUID();
        db.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'ROBOTICA','Robótica directa','ACTIVO')",other,academy);
        catalog.link(other);
        catalog.link(other);
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM curso_semillero WHERE semillero_id=?",Integer.class,other));
        assertTrue(db.queryForObject("SELECT estado FROM semillero WHERE id=?",String.class,other).equals("ACTIVO"));
    }
}
