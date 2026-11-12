package com.laboratorio.research;

import org.junit.jupiter.api.Test;
import com.laboratorio.identity.DevelopmentAdministrator;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:demo_query_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@Transactional
class DemoSemillerosTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired DemoSemilleros demo;
    @Autowired PasswordEncoder encoder;

    @Test void listsAndFiltersRealDatabaseRows() throws Exception {
        mvc.perform(get("/demostracion/semilleros"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Robótica")))
                .andExpect(content().string(containsString("Astronomía y Física Espacial")));
        mvc.perform(get("/demostracion/semilleros").param("q", "ROBOTICA"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Robótica")))
                .andExpect(content().string(not(containsString("Astronomía y Física Espacial"))));
        jdbc.update("UPDATE semillero SET nombre = ? WHERE codigo = ?", "Robótica actualizada", "ROBOTICA");
        mvc.perform(get("/demostracion/semilleros").param("q", "actualizada"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Robótica actualizada")));
    }

    @Test void seedIsIdempotentAndSearchTreatsWildcardsLiterally() throws Exception {
        demo.run(null);
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM semillero", Integer.class));
        mvc.perform(get("/demostracion/semilleros").param("q", "%"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("No hay semilleros")));
        mvc.perform(get("/demostracion/semilleros").param("q", "' OR 1=1 --"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("No hay semilleros")));
    }

    @Test void escapesSearchAndRejectsOversizedInput() throws Exception {
        mvc.perform(get("/demostracion/semilleros").param("q", "<script>"))
                .andExpect(status().isOk()).andExpect(content().string(not(containsString("<script>"))));
        mvc.perform(get("/demostracion/semilleros").param("q", "x".repeat(101)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("hasta 100 caracteres")));
    }
    @Test void explicitLocalAdministratorIsCreatedOnceWithEncodedPassword() {
        var env = new MockEnvironment().withProperty("LAB_ADMIN_EMAIL", "ADMIN@example.test")
                .withProperty("LAB_ADMIN_PASSWORD", "clave-larga-de-prueba");
        var bootstrap = new DevelopmentAdministrator(jdbc, encoder, env);
        bootstrap.run(null);
        bootstrap.run(null);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE correo='admin@example.test'", Integer.class));
        String hash = jdbc.queryForObject("SELECT password_hash FROM usuario WHERE correo='admin@example.test'", String.class);
        assertTrue(encoder.matches("clave-larga-de-prueba", hash));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE r.codigo='ADMINISTRADOR'", Integer.class));
    }

}
