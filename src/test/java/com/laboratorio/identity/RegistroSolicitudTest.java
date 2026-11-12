package com.laboratorio.identity;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegistroSolicitudTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    UUID academy = UUID.randomUUID();
    UUID admin = UUID.randomUUID();

    @BeforeEach void setup() {
        jdbc.update("INSERT INTO academia(id,nombre,estado) VALUES (?, 'Academia de prueba', 'ACTIVA')", academy);
        jdbc.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,'admin@example.test','Admin','Prueba',?,'ACTIVO')", admin, academy, encoder.encode("admin-clave-123456"));
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='ADMINISTRADOR'", UUID.randomUUID(), admin);
    }

    @Test void registroPublicoCreaSolicitudPendienteYEsNeutroAlDuplicar() throws Exception {
        mvc.perform(post("/registro").with(csrf())
                        .param("nombre", "Ana").param("apellido", "Cadete")
                        .param("correo", "NUEVA@example.test").param("rol", "ESTUDIANTE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Transmisión recibida")));
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM solicitud_cuenta WHERE correo='nueva@example.test' AND estado='PENDIENTE'", Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, count);
        // La repetición no duplica ni filtra la existencia
        mvc.perform(post("/registro").with(csrf())
                        .param("nombre", "Ana").param("apellido", "Cadete")
                        .param("correo", "nueva@example.test").param("rol", "ESTUDIANTE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Transmisión recibida")));
        count = jdbc.queryForObject("SELECT COUNT(*) FROM solicitud_cuenta WHERE correo='nueva@example.test'", Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, count);
        // La solicitud para un rol no permitido es rechazada en validación
        mvc.perform(post("/registro").with(csrf())
                        .param("nombre", "Eve").param("apellido", "Intruso")
                        .param("correo", "eve@example.test").param("rol", "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ESTUDIANTE o PROFESOR")));
        Integer admins = jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE correo='eve@example.test'", Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(0, admins);
    }

    @Test void registroRequiereCsrf() throws Exception {
        mvc.perform(post("/registro")
                        .param("nombre", "Ana").param("apellido", "Cadete")
                        .param("correo", "sin-csrf@example.test").param("rol", "ESTUDIANTE"))
                .andExpect(status().isForbidden());
    }

    @Test void aprobarCreaCuentaConRolSolicitadoYLoginFunciona() throws Exception {
        mvc.perform(post("/registro").with(csrf())
                        .param("nombre", "Tech").param("apellido", "Líder")
                        .param("correo", "techlead@example.test").param("rol", "PROFESOR"))
                .andExpect(status().isOk());
        UUID solicitud = jdbc.queryForObject("SELECT id FROM solicitud_cuenta WHERE correo='techlead@example.test'", UUID.class);
        mvc.perform(post("/administracion/solicitudes/aprobar").with(csrf()).with(user("admin@example.test").roles("ADMINISTRADOR"))
                        .param("solicitud", solicitud.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Credenciales temporales")));
        String estado = jdbc.queryForObject("SELECT estado FROM solicitud_cuenta WHERE correo='techlead@example.test'", String.class);
        org.junit.jupiter.api.Assertions.assertEquals("APROBADA", estado);
        String rol = jdbc.queryForObject("SELECT r.codigo FROM usuario u JOIN usuario_rol ur ON ur.usuario_id=u.id JOIN rol r ON r.id=ur.rol_id WHERE u.correo='techlead@example.test'", String.class);
        org.junit.jupiter.api.Assertions.assertEquals("PROFESOR", rol);
        // La contraseña temporal permite iniciar sesión
        String temporalHash = jdbc.queryForObject("SELECT password_hash FROM usuario WHERE correo='techlead@example.test'", String.class);
        org.junit.jupiter.api.Assertions.assertNotNull(temporalHash);
        // Rechazar una solicitud ya aprobada está prohibido en la bandeja
        mvc.perform(post("/administracion/solicitudes/rechazar").with(csrf()).with(user("admin@example.test").roles("ADMINISTRADOR"))
                        .param("solicitud", solicitud.toString()))
                .andExpect(status().is3xxRedirection());
    }

    @Test void estudianteNoPuedeAprobarRechazar() throws Exception {
        UUID estudiante = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,'estudiante@example.test','Est','Udiante',?,'ACTIVO')", estudiante, academy, encoder.encode("estudiante-clave-123"));
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='ESTUDIANTE'", UUID.randomUUID(), estudiante);
        mvc.perform(get("/administracion/solicitudes").with(user("estudiante@example.test").roles("ESTUDIANTE")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/administracion/solicitudes/aprobar").with(csrf()).with(user("estudiante@example.test").roles("ESTUDIANTE"))
                        .param("solicitud", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    @Test void rechazarNoCreaCuentaYMarcaLaSolicitud() throws Exception {
        mvc.perform(post("/registro").with(csrf())
                        .param("nombre", "No").param("apellido", "Admitido")
                        .param("correo", "rechazado@example.test").param("rol", "PROFESOR"))
                .andExpect(status().isOk());
        UUID solicitud = jdbc.queryForObject("SELECT id FROM solicitud_cuenta WHERE correo='rechazado@example.test'", UUID.class);
        mvc.perform(post("/administracion/solicitudes/rechazar").with(csrf()).with(user("admin@example.test").roles("ADMINISTRADOR"))
                        .param("solicitud", solicitud.toString()))
                .andExpect(status().is3xxRedirection());
        String estado = jdbc.queryForObject("SELECT estado FROM solicitud_cuenta WHERE correo='rechazado@example.test'", String.class);
        org.junit.jupiter.api.Assertions.assertEquals("RECHAZADA", estado);
        Integer existe = jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE correo='rechazado@example.test'", Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(0, existe);
    }
}
