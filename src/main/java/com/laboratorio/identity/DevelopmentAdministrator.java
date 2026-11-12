package com.laboratorio.identity;

import java.util.Locale;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Inicialización explícita y únicamente local; nunca sustituye una cuenta existente. */
@Component
@Profile("dev")
@Order(100)
public class DevelopmentAdministrator implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    private final Environment env;
    public DevelopmentAdministrator(JdbcTemplate jdbc, PasswordEncoder passwords, Environment env) {
        this.jdbc = jdbc; this.passwords = passwords; this.env = env;
    }
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = env.getProperty("LAB_ADMIN_EMAIL", "").strip().toLowerCase(Locale.ROOT);
        String password = env.getProperty("LAB_ADMIN_PASSWORD", "");
        if (email.isEmpty() && password.isEmpty()) return;
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || email.length() > 254 || password.length() < 12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("LAB_ADMIN_EMAIL y LAB_ADMIN_PASSWORD (12 a 72 caracteres) son obligatorios para inicializar el administrador local.");
        }
        // Solo inicializar un administrador si aún no existe ninguno, sin modificar cuentas existentes.
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE r.codigo='ADMINISTRADOR'", Integer.class);
        if (count != null && count > 0) return;
        if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT COUNT(*) > 0 FROM usuario WHERE correo=?", Boolean.class, email))) {
            throw new IllegalStateException("El correo de inicialización ya pertenece a otra cuenta.");
        }
        UUID academy = UUID.fromString("1de00000-0000-4000-8000-000000000001");
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,?,'Administrador','Local',?,'ACTIVO')", id, academy, email, passwords.encode(password));
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='ADMINISTRADOR'", UUID.randomUUID(), id);
    }
}
