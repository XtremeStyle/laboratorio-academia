package com.laboratorio.api;

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
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiRestControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;

    UUID user;
    @BeforeEach void setup() {
        UUID academy = UUID.randomUUID();
        jdbc.update("INSERT INTO academia(id,nombre,estado) VALUES (?,'API','ACTIVA')", academy);
        jdbc.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'API','API','ACTIVO')", UUID.randomUUID(), academy);
        user = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,'api@example.test','Ana','Solo',?,'ACTIVO')", user, academy, encoder.encode("api-clave-12345"));
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT gen_random_uuid(),?,id FROM rol WHERE codigo='ESTUDIANTE'", user);
    }

    @Test void anonymousUserIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/api/perfil"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test void authenticatedUserGetsProfileAsJson() throws Exception {
        mvc.perform(get("/api/perfil").with(user("api@example.test").roles("ESTUDIANTE")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPathBodyContains("api@example.test", "Ana Solo"));
    }

    private static org.springframework.test.web.servlet.ResultMatcher jsonPathBodyContains(String correo, String nombre) {
        return jsonPathBodyContains(correo, nombre, null);
    }

    private static org.springframework.test.web.servlet.ResultMatcher jsonPathBodyContains(String correo, String nombre, ResultMatcher extra) {
        return result -> {
            String body = result.getResponse().getContentAsString();
            org.junit.jupiter.api.Assertions.assertTrue(body.contains("\"nombre\":\"" + nombre + "\""), "el perfil debe incluir nombre: " + body);
            org.junit.jupiter.api.Assertions.assertTrue(body.contains("\"correo\":\"" + correo + "\""), "el perfil debe incluir correo: " + body);
        };
    }
}
