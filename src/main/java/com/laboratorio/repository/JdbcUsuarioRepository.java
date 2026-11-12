package com.laboratorio.repository;

import com.laboratorio.domain.Usuario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Implementación JDBC del contrato de usuarios; todas las consultas están parametrizadas. */
@Repository
public class JdbcUsuarioRepository implements UsuarioRepository {
    private final JdbcTemplate jdbc;

    public JdbcUsuarioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Usuario> findByCorreo(String correoNormalizado) {
        String correo = correoNormalizado == null ? "" : correoNormalizado.strip().toLowerCase(java.util.Locale.ROOT);
        var rows = jdbc.query("SELECT id,academia_id,correo,nombre,apellido,estado FROM usuario WHERE correo=? LIMIT 1",
                (rs, i) -> new Object[]{rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6)}, correo);
        if (rows.isEmpty()) return Optional.empty();
        Object[] row = rows.getFirst();
        List<String> roles = jdbc.query("SELECT r.codigo FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=?",
                (rs, i) -> rs.getString(1), (UUID) row[0]);
        return Optional.of(Usuario.desde((UUID) row[0], (UUID) row[1], (String) row[2], (String) row[3], (String) row[4], (String) row[5], roles));
    }

    @Override
    public boolean existsByCorreo(String correoNormalizado) {
        return idByCorreo(correoNormalizado).isPresent();
    }

    @Override
    public Optional<UUID> idByCorreo(String correoNormalizado) {
        String correo = correoNormalizado == null ? "" : correoNormalizado.strip().toLowerCase(java.util.Locale.ROOT);
        var rows = jdbc.query("SELECT id FROM usuario WHERE correo=? LIMIT 1", (rs, i) -> rs.getObject(1, UUID.class), correo);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    }
}
