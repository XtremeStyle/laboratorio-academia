package com.laboratorio.identity;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityHardeningTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;

    @Test void loginThrottleReturns429After20AttemptsFromSameIp() throws Exception {
        String ip = "203.0.113.99";
        for (int i = 0; i < 20; i++) {
            mvc.perform(post("/login").with(csrf())
                            .param("username", "throttler@example.test")
                            .param("password", "incorrecta-larga-123")
                            .with(request -> { request.setRemoteAddr(ip); return request; }))
                    .andExpect(status().is3xxRedirection());
        }
        mvc.perform(post("/login").with(csrf())
                        .param("username", "throttler@example.test")
                        .param("password", "incorrecta-larga-123")
                        .with(request -> { request.setRemoteAddr(ip); return request; }))
                .andExpect(status().is(429))
                .andExpect(header().exists("Retry-After"));
    }

    @Test void securityHeadersPresentOnPublicLoginPage() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("Referrer-Policy"))
                .andExpect(header().exists("Permissions-Policy"));
    }

    @Test void actuatorEndpointsBehindHealthInfoAreDeniedForEveryAuthenticatedRole() throws Exception {
        mvc.perform(get("/actuator/env").with(user("estudiante1@example.test").roles("ESTUDIANTE")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/actuator/heapdump").with(user("admin@example.test").roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden());
    }

    @Test void studentCannotPeekIntoAnotherStudentsCourseEnrollment() throws Exception {
        UUID academy = UUID.randomUUID(), track = UUID.randomUUID(), course = UUID.randomUUID();
        UUID studentA = UUID.randomUUID(), studentB = UUID.randomUUID();
        jdbc.update("INSERT INTO academia(id,nombre,estado) VALUES (?,'Seg','ACTIVA')", academy);
        jdbc.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'SEG1','Seg','ACTIVO')", track, academy);
        jdbc.update("INSERT INTO curso(id,codigo,titulo,descripcion,nivel,duracion_minutos,estado) VALUES (?,'SEG101','Curso de seguridad','Descripción','INICIAL',60,'PUBLICADO')", course);
        jdbc.update("INSERT INTO curso_semillero(curso_id,semillero_id) VALUES (?,?)", course, track);
        for (Object[] row : new Object[][]{{studentA, "alumno-a@example.test"}, {studentB, "alumno-b@example.test"}}) {
            UUID id = (UUID) row[0]; String correo = (String) row[1];
            jdbc.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,?,'Ana','Ejemplo',?,'ACTIVO')", id, academy, correo, encoder.encode("clave-de-prueba-123"));
            jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='ESTUDIANTE'", UUID.randomUUID(), id);
            UUID assignment = UUID.randomUUID(), asignPeriod = UUID.randomUUID();
            jdbc.update("INSERT INTO asignacion_semillero(id,academia_id,usuario_id,semillero_id) VALUES (?,?,?,?)", assignment, academy, id, track);
            jdbc.update("INSERT INTO periodo_asignacion(id,asignacion_id,inicio,registrado_por_id) VALUES (?,?,CURRENT_TIMESTAMP,?)", asignPeriod, assignment, id);
            jdbc.update("INSERT INTO asignacion_activa(asignacion_id,periodo_id) VALUES (?,?)", assignment, asignPeriod);
        }
        jdbc.update("INSERT INTO matricula_curso(id,curso_id,usuario_id,semillero_id,estado) VALUES (?,?,?,?,'ACTIVA')", UUID.randomUUID(), course, studentA, track);
        UUID enrollment = jdbc.queryForObject("SELECT id FROM matricula_curso WHERE curso_id=? AND usuario_id=?", UUID.class, course, studentA);
        mvc.perform(get("/cursos/estudiar").with(user("alumno-b@example.test").roles("ESTUDIANTE")).param("matricula", enrollment.toString()))
                .andExpect(status().isNotFound());
    }
}
