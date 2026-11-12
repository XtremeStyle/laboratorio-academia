package com.laboratorio.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI del backend de CHANGE // SPACE-LAB OS.
 * Autorización: la UI y el documento se exponen sin credenciales en este entorno
 * (solo texto descriptivo, sin secretos). En producción, restringir /swagger-ui
 * y /v3/api-docs al ámbito administrativo o deshabilitarlos.
 */
@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI changeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CHANGE // SPACE-LAB OS — API")
                        .description("""
                                Backend de la plataforma académica CHANGE // SPACE-LAB OS.
                                Rutas públicas: portada, registro, acceso y demostración.
                                Rutas privadas: misiones, usuarios, entregas, chat, boletines y administración.
                                Autenticación: sesión por cookie (`JSESSIONID`) tras POST /login.
                                El intento de POST/GET sin rol autorizado devuelve 302 (redirect a /login)
                                o 403 según la regla de Spring Security configurada.
                                Los endpoints de laboratorio y boletines sirven recursos Thymeleaf/JSON
                                y nunca exponen datos fuera del ámbito del solicitante.""")
                        .version("0.1.0")
                        .contact(new Contact().name("CHANGE"))
                        .license(new License().name("Apache-2.0")))
                .components(new Components());
    }
}
