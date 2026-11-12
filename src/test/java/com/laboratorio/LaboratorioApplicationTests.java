package com.laboratorio;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class LaboratorioApplicationTests {

    @Test
    void contextLoads() {
        // La prueba verifica el arranque completo con H2 y Flyway.
    }
}
