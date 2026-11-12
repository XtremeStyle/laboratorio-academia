package com.laboratorio.course;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@ActiveProfiles("test")
class CourseLearningFlowTest {
    @Autowired JdbcTemplate db;
    @Autowired CourseService courses;
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;

    @Test
    @Transactional
    void studentCanEnrollAndRecordEvidenceForALesson() throws Exception {
        UUID academy=UUID.randomUUID(), student=UUID.randomUUID(), track=UUID.randomUUID();
        UUID assignment=UUID.randomUUID(), period=UUID.randomUUID();
        UUID course=UUID.fromString("c0000000-0000-4000-8000-000000000001");
        db.update("INSERT INTO academia(id,nombre,estado) VALUES (?,?,'ACTIVA')",academy,"Academia de prueba");
        String email="student-"+student+"@test.invalid";
        db.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,?,'Estudiante','Prueba','hash','ACTIVO')",student,academy,email);
        UUID role=db.queryForObject("SELECT id FROM rol WHERE codigo='ESTUDIANTE'",UUID.class);
        db.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) VALUES (?,?,?)",UUID.randomUUID(),student,role);
        db.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'ROBOTICA','Robótica','ACTIVO')",track,academy);
        db.update("INSERT INTO curso_semillero(curso_id,semillero_id) VALUES (?,?)",course,track);
        db.update("INSERT INTO asignacion_semillero(id,academia_id,usuario_id,semillero_id) VALUES (?,?,?,?)",assignment,academy,student,track);
        db.update("INSERT INTO periodo_asignacion(id,asignacion_id,inicio,registrado_por_id) VALUES (?,?,CURRENT_TIMESTAMP,?)",period,assignment,student);
        db.update("INSERT INTO asignacion_activa(asignacion_id,periodo_id) VALUES (?,?)",assignment,period);

        assertEquals(1,courses.catalog(email).size());
        courses.enroll(email,course,track);
        UUID enrollment=db.queryForObject("SELECT id FROM matricula_curso WHERE curso_id=? AND usuario_id=?",UUID.class,course,student);
        courses.enroll(email,course,track);
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM matricula_curso WHERE curso_id=? AND usuario_id=?",Integer.class,course,student));
        // Simula una matrícula creada antes de que el curso tuviera ocho lecciones.
        db.update("DELETE FROM progreso_leccion WHERE matricula_id=? AND leccion_id IN (SELECT id FROM leccion_curso WHERE curso_id=? AND numero>6)",enrollment,course);
        assertEquals(8,courses.detail(email,enrollment).lessons().size());
        assertEquals(8,db.queryForObject("SELECT COUNT(*) FROM progreso_leccion WHERE matricula_id=?",Integer.class,enrollment));
        UUID lesson=db.queryForObject("SELECT id FROM leccion_curso WHERE curso_id=? AND numero=1",UUID.class,course);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/laboratorio/disenos/CIRCUITO")
                .contentType("application/json").content("{\"voltage\":5,\"parts\":[{\"a\":1,\"b\":0,\"r\":220}]}")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(email).roles("ESTUDIANTE"))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/cursos/estudiar")
                .param("matricula",enrollment.toString())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(email).roles("ESTUDIANTE")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Añadir diseño guardado a la evidencia")));
        var bad=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/cursos/leccion")
                .param("matricula",enrollment.toString()).param("leccion",lesson.toString())
                .param("estado","COMPLETADA").param("evidencia","corta")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(email).roles("ESTUDIANTE"))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is3xxRedirection()).andReturn();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/cursos/estudiar")
                .param("matricula",enrollment.toString()).flashAttrs(bad.getFlashMap())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(email).roles("ESTUDIANTE")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("entre 12 y 5000 caracteres")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString(">corta</textarea>")));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> courses.updateLesson(email,enrollment,lesson,"EN_CURSO","x".repeat(5001)));
        courses.updateLesson(email,enrollment,lesson,"COMPLETADA","Medición registrada con procedimiento, valores y conclusión.");
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM progreso_leccion WHERE matricula_id=? AND estado='COMPLETADA'",Integer.class,enrollment));
        courses.updateLesson(email,enrollment,lesson,"EN_CURSO","");
        assertEquals("COMPLETADA",db.queryForObject("SELECT estado FROM progreso_leccion WHERE matricula_id=? AND leccion_id=?",String.class,enrollment,lesson));
        assertEquals("ACTIVA",db.queryForObject("SELECT estado FROM matricula_curso WHERE id=?",String.class,enrollment));
        for (UUID remaining : db.queryForList("SELECT leccion_id FROM progreso_leccion WHERE matricula_id=? AND estado<>'COMPLETADA'",UUID.class,enrollment)) {
            courses.updateLesson(email,enrollment,remaining,"COMPLETADA","Evidencia de aprendizaje y resultados registrados.");
        }
        assertEquals("COMPLETADA",db.queryForObject("SELECT estado FROM matricula_curso WHERE id=?",String.class,enrollment));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/cursos/estudiar")
                .param("matricula",enrollment.toString())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(email).roles("ESTUDIANTE")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("modo de consulta")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Guardar avance"))));
    }

    @Test
    @Transactional
    void losingActiveAssignmentRevokesCourseAccessAndWriting() {
        UUID academy=UUID.randomUUID(), student=UUID.randomUUID(), track=UUID.randomUUID();
        UUID assignment=UUID.randomUUID(), period=UUID.randomUUID();
        UUID course=UUID.fromString("c0000000-0000-4000-8000-000000000001");
        db.update("INSERT INTO academia(id,nombre,estado) VALUES (?,?,'ACTIVA')",academy,"Academia de prueba");
        String email="revocado-"+student+"@test.invalid";
        db.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,?,'Estudiante','Revocado','hash','ACTIVO')",student,academy,email);
        UUID role=db.queryForObject("SELECT id FROM rol WHERE codigo='ESTUDIANTE'",UUID.class);
        db.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) VALUES (?,?,?)",UUID.randomUUID(),student,role);
        db.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'ROBOTICA','Robótica','ACTIVO')",track,academy);
        db.update("INSERT INTO curso_semillero(curso_id,semillero_id) VALUES (?,?)",course,track);
        db.update("INSERT INTO asignacion_semillero(id,academia_id,usuario_id,semillero_id) VALUES (?,?,?,?)",assignment,academy,student,track);
        db.update("INSERT INTO periodo_asignacion(id,asignacion_id,inicio,registrado_por_id) VALUES (?,?,CURRENT_TIMESTAMP,?)",period,assignment,student);
        db.update("INSERT INTO asignacion_activa(asignacion_id,periodo_id) VALUES (?,?)",assignment,period);
        courses.enroll(email,course,track);
        UUID enrollment=db.queryForObject("SELECT id FROM matricula_curso WHERE curso_id=? AND usuario_id=?",UUID.class,course,student);
        UUID lesson=db.queryForObject("SELECT id FROM leccion_curso WHERE curso_id=? AND numero=1",UUID.class,course);
        courses.updateLesson(email,enrollment,lesson,"COMPLETADA","Evidencia inicial con mediciones, cálculos y conclusión completa.");
        assertEquals(8,courses.detail(email,enrollment).lessons().size());

        // El estudiante pierde su asignación activa al semillero: se revoca acceso y escritura.
        db.update("DELETE FROM asignacion_activa WHERE asignacion_id=? AND periodo_id=?",assignment,period);
        assertEquals(0,courses.catalog(email).size());
        var detail = org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.web.server.ResponseStatusException.class,
            () -> courses.detail(email,enrollment));
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND,detail.getStatusCode());
        var write = org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.web.server.ResponseStatusException.class,
            () -> courses.updateLesson(email,enrollment,lesson,"EN_CURSO",""));
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND,write.getStatusCode());
        var reenroll = org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.web.server.ResponseStatusException.class,
            () -> courses.enroll(email,course,track));
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN,reenroll.getStatusCode());
        // La matrícula y el progreso se conservan sin datos perdidos.
        assertEquals("ACTIVA",db.queryForObject("SELECT estado FROM matricula_curso WHERE id=?",String.class,enrollment));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM progreso_leccion WHERE matricula_id=? AND estado='COMPLETADA'",Integer.class,enrollment));

        // Al reactivar la asignación, el acceso vuelve y el historial se mantiene.
        db.update("INSERT INTO asignacion_activa(asignacion_id,periodo_id) VALUES (?,?)",assignment,period);
        assertEquals(1,courses.catalog(email).size());
        assertEquals(8,courses.detail(email,enrollment).lessons().size());
        courses.updateLesson(email,enrollment,lesson,"EN_CURSO","");
        assertEquals("COMPLETADA",db.queryForObject("SELECT estado FROM progreso_leccion WHERE matricula_id=? AND leccion_id=?",String.class,enrollment,lesson));
    }

    @Test
    @Transactional
    void studentWithoutActiveAssignmentCannotSeeOrEnroll() {
        UUID academy=UUID.randomUUID(), student=UUID.randomUUID(), track=UUID.randomUUID(), other=UUID.randomUUID();
        UUID course=UUID.fromString("c0000000-0000-4000-8000-000000000001");
        db.update("INSERT INTO academia(id,nombre,estado) VALUES (?,?,'ACTIVA')",academy,"Academia de prueba");
        String email="outsider-"+student+"@test.invalid";
        db.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,?,'Estudiante','Ajenos','hash','ACTIVO')",student,academy,email);
        UUID role=db.queryForObject("SELECT id FROM rol WHERE codigo='ESTUDIANTE'",UUID.class);
        db.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) VALUES (?,?,?)",UUID.randomUUID(),student,role);
        db.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'ROBOTICA','Robótica','ACTIVO')",track,academy);
        db.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'FISICA','Física','ACTIVO')",other,academy);
        db.update("INSERT INTO curso_semillero(curso_id,semillero_id) VALUES (?,?)",course,track);

        assertEquals(0,courses.catalog(email).size());
        var http = org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.web.server.ResponseStatusException.class,
            () -> courses.enroll(email,course,track));
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN,http.getStatusCode());
        // Sin semillero con código permitido no hay asociación: el curso no es visible.
        db.update("INSERT INTO curso_semillero(curso_id,semillero_id) VALUES (?,?)",course,other);
        assertEquals(0,courses.catalog(email).size());
        var httpOther = org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.web.server.ResponseStatusException.class,
            () -> courses.enroll(email,course,other));
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN,httpOther.getStatusCode());
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM matricula_curso",Integer.class));
    }
}
