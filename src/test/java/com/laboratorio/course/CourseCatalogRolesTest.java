package com.laboratorio.course;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El catálogo de cursos resuelve el enlace para docentes y administradores: vista de solo lectura. */
@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@ActiveProfiles("test")
class CourseCatalogRolesTest {
    @Autowired JdbcTemplate db;
    @Autowired CourseService courses;
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;

    private record Person(String email, UUID id, UUID academy) {}

    private Person create(String role, String nombres) {
        UUID academy=UUID.randomUUID(), person=UUID.randomUUID();
        db.update("INSERT INTO academia(id,nombre,estado) VALUES (?,?,'ACTIVA')",academy,"Academia de roles");
        String email=role.toLowerCase()+"-"+person+"@test.invalid";
        db.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,?,?,'Roles','hash','ACTIVO')",person,academy,email,nombres);
        UUID roleId=db.queryForObject("SELECT id FROM rol WHERE codigo=?",UUID.class,role);
        db.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) VALUES (?,?,?)",UUID.randomUUID(),person,roleId);
        return new Person(email,person,academy);
    }

    private UUID trackWithCourse(UUID academy) {
        UUID track=UUID.randomUUID();
        db.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'ROBOTICA','Robótica','ACTIVO')",track,academy);
        db.update("INSERT INTO curso_semillero(curso_id,semillero_id) VALUES ('c0000000-0000-4000-8000-000000000001',?)",track);
        return track;
    }

    private void activeAssignment(UUID person, UUID academy, UUID track) {
        UUID assignment=UUID.randomUUID(), period=UUID.randomUUID();
        db.update("INSERT INTO asignacion_semillero(id,academia_id,usuario_id,semillero_id) VALUES (?,?,?,?)",assignment,academy,person,track);
        db.update("INSERT INTO periodo_asignacion(id,asignacion_id,inicio,registrado_por_id) VALUES (?,?,CURRENT_TIMESTAMP,?)",period,assignment,person);
        db.update("INSERT INTO asignacion_activa(asignacion_id,periodo_id) VALUES (?,?)",assignment,period);
    }

    @Test
    @Transactional
    void teacherWithActiveAssignmentSeesReadOnlyCatalog() {
        Person teacher=create("PROFESOR","Docente");
        UUID track=trackWithCourse(teacher.academy());
        activeAssignment(teacher.id(),teacher.academy(),track);

        CourseService.CatalogView vista=courses.catalogFor(teacher.email());
        assertFalse(vista.student());
        assertEquals(1,vista.courses().size());
        CourseService.CourseCard card=vista.courses().getFirst();
        assertNull(card.enrollment());
        assertEquals(track,card.trackId());
        assertEquals(8,card.lessons());

        // La matrícula y la escritura siguen prohibidas para docentes.
        UUID course=UUID.fromString("c0000000-0000-4000-8000-000000000001");
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(ResponseStatusException.class,
            () -> courses.enroll(teacher.email(),course,track)).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(ResponseStatusException.class,
            () -> courses.updateLesson(teacher.email(),UUID.randomUUID(),UUID.randomUUID(),"COMPLETADA","Evidencia del docente.")).getStatusCode());
    }

    @Test
    @Transactional
    void administratorSeesCoursesOfAcademyTracks() {
        Person admin=create("ADMINISTRADOR","Admin");
        UUID track=trackWithCourse(admin.academy());

        CourseService.CatalogView vistaAdmin=courses.catalogFor(admin.email());
        assertFalse(vistaAdmin.student());
        assertEquals(1,vistaAdmin.courses().size());
        assertNull(vistaAdmin.courses().getFirst().enrollment());

        // Un administrador de otra academia no ve este semillero.
        Person outsider=create("ADMINISTRADOR","Admin ajeno");
        assertEquals(0,courses.catalogFor(outsider.email()).courses().size());
    }

    @Test
    @Transactional
    void teacherWithoutActiveAssignmentSeesNoCourses() {
        Person teacher=create("PROFESOR","Docente sin asignación");
        trackWithCourse(teacher.academy());
        assertEquals(0,courses.catalogFor(teacher.email()).courses().size());
    }

    @Test
    @Transactional
    void staffProgressRequiresPermissionForTheActualTrack() throws Exception {
        Person teacher=create("PROFESOR","Docente");
        UUID track=trackWithCourse(teacher.academy());
        UUID course=UUID.fromString("c0000000-0000-4000-8000-000000000001");
        activeAssignment(teacher.id(),teacher.academy(),track);
        UUID student=UUID.randomUUID(),enrollment=UUID.randomUUID();
        db.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,?,'Estudiante','Prueba','hash','ACTIVO')",student,teacher.academy(),"student-"+student+"@test.invalid");
        db.update("INSERT INTO matricula_curso(id,curso_id,usuario_id,semillero_id,estado) VALUES (?,?,?,?,'ACTIVA')",enrollment,course,student,track);
        UUID lesson=db.queryForObject("SELECT id FROM leccion_curso WHERE curso_id=? AND numero=1",UUID.class,course);
        db.update("INSERT INTO progreso_leccion(id,matricula_id,curso_id,leccion_id,estado,evidencia,iniciado_en) VALUES (?,?,?,?,'EN_CURSO','Evidencia propia',CURRENT_TIMESTAMP)",UUID.randomUUID(),enrollment,course,lesson);
        var progress=courses.staffProgress(teacher.email(),course,track,0).rows();
        assertEquals(8,progress.size());
        assertEquals("Evidencia propia",progress.getFirst().evidence());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/academia/cursos/progreso")
                .param("curso",course.toString()).param("semillero",track.toString())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(teacher.email()).roles("PROFESOR")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Evidencia propia")));
        db.update("UPDATE progreso_leccion SET evidencia=? WHERE matricula_id=? AND leccion_id=?","<script>alert('x')</script>",enrollment,lesson);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/academia/cursos/progreso")
                .param("curso",course.toString()).param("semillero",track.toString())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(teacher.email()).roles("PROFESOR")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("&lt;script&gt;")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script>alert('x')</script>"))));
        Person outsider=create("ADMINISTRADOR","Otra academia");
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(ResponseStatusException.class,()->courses.staffProgress(outsider.email(),course,track,0)).getStatusCode());
        Person unrelated=create("PROFESOR","Otro docente");
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(ResponseStatusException.class,()->courses.staffProgress(unrelated.email(),course,track,0)).getStatusCode());
        for(int i=0;i<12;i++) {
            UUID extra=UUID.randomUUID();
            db.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,?,'Prueba','Paginación','hash','ACTIVO')",extra,teacher.academy(),"page-"+extra+"@test.invalid");
            db.update("INSERT INTO matricula_curso(id,curso_id,usuario_id,semillero_id,estado) VALUES (?,?,?,?,'ACTIVA')",UUID.randomUUID(),course,extra,track);
        }
        var firstPage=courses.staffProgress(teacher.email(),course,track,0);
        var secondPage=courses.staffProgress(teacher.email(),course,track,1);
        assertEquals(100,firstPage.rows().size());
        assertTrue(firstPage.hasNext());
        assertEquals(4,secondPage.rows().size());
        assertFalse(secondPage.hasNext());
    }

    @Test
    @Transactional
    void studentViewTakesPrecedenceWhenBothRolesPresent() {
        Person dual=create("PROFESOR","Doble rol");
        UUID course=UUID.fromString("c0000000-0000-4000-8000-000000000001");
        db.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) VALUES (?,?,?)",UUID.randomUUID(),dual.id(),
            db.queryForObject("SELECT id FROM rol WHERE codigo='ESTUDIANTE'",UUID.class));
        UUID track=trackWithCourse(dual.academy());
        activeAssignment(dual.id(),dual.academy(),track);

        CourseService.CatalogView vista=courses.catalogFor(dual.email());
        assertTrue(vista.student());
        assertEquals(1,vista.courses().size());
        assertEquals(1,vista.staffCourses().size());
        assertNull(vista.courses().getFirst().enrollment());
    }

    @Test
    @Transactional
    void administratorWithStudentRoleKeepsAcademyCatalogWithoutEnrollmentPermission() {
        Person dual=create("ADMINISTRADOR","Administradora estudiante");
        db.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) VALUES (?,?,?)",UUID.randomUUID(),dual.id(),
            db.queryForObject("SELECT id FROM rol WHERE codigo='ESTUDIANTE'",UUID.class));
        UUID track=trackWithCourse(dual.academy());
        var view=courses.catalogFor(dual.email());
        assertTrue(view.student());
        assertEquals  (0,view.courses().size());
        assertEquals(track,view.staffCourses().getFirst().trackId());
        UUID course=UUID.fromString("c0000000-0000-4000-8000-000000000001");
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(ResponseStatusException.class,
            () -> courses.enroll(dual.email(),course,track)).getStatusCode());
    }
}
