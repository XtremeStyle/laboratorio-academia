package com.laboratorio.research;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Datos exclusivamente demostrativos; nunca se ejecuta en el perfil de producción. */
@Component
@Profile("dev")
@Order(10)
public class DemoSemilleros implements ApplicationRunner {
    private static final UUID ACADEMIA = UUID.fromString("1de00000-0000-4000-8000-000000000001");
    private static final UUID ROBOTICA = UUID.fromString("1de00000-0000-4000-8000-000000000002");
    private static final UUID ASTRONOMIA = UUID.fromString("1de00000-0000-4000-8000-000000000003");
    private final JdbcTemplate jdbc;

    public DemoSemilleros(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        jdbc.update("INSERT INTO academia(id,nombre,estado) SELECT ?,?,? WHERE NOT EXISTS (SELECT 1 FROM academia WHERE id=?)",
                ACADEMIA, "Academia de demostración", "ACTIVA", ACADEMIA);
        insertarSiFalta(ROBOTICA, "ROBOTICA", "Robótica", "Diseña y construye soluciones que conectan el mundo físico y digital.");
        insertarSiFalta(ASTRONOMIA, "ASTRONOMIA", "Astronomía y Física Espacial", "Investiga el universo y desarrolla pensamiento científico.");
    }

    private void insertarSiFalta(UUID id, String codigo, String nombre, String descripcion) {
        jdbc.update("""
                INSERT INTO semillero(id,academia_id,codigo,nombre,descripcion,estado)
                SELECT ?,?,?,?,?,'ACTIVO' WHERE NOT EXISTS (SELECT 1 FROM semillero WHERE academia_id=? AND codigo=?)
                """, id, ACADEMIA, codigo, nombre, descripcion, ACADEMIA, codigo);
    }

    @Transactional(readOnly = true)
    public List<SemilleroVista> buscar(String consulta) {
        String texto = consulta == null ? "" : consulta.strip();
        if (texto.length() > 100) throw new IllegalArgumentException("La búsqueda admite hasta 100 caracteres.");
        // Escapar comodines para que la búsqueda trate el texto ingresado literalmente.
        String literal = texto.toLowerCase(java.util.Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return jdbc.query("""
                SELECT codigo, nombre, descripcion FROM semillero
                WHERE academia_id = ? AND estado = 'ACTIVO'
                  AND (LOWER(nombre) LIKE ? ESCAPE '!' OR LOWER(codigo) LIKE ? ESCAPE '!')
                ORDER BY nombre LIMIT 100
                """, (rs, row) -> new SemilleroVista(rs.getString("codigo"), rs.getString("nombre"), rs.getString("descripcion")),
                ACADEMIA, "%" + literal + "%", "%" + literal + "%");
    }

    public record SemilleroVista(String codigo, String nombre, String descripcion) { }
}
