package com.laboratorio.repository;

import com.laboratorio.domain.Usuario;
import java.util.Optional;
import java.util.UUID;

/** Contrato de lectura/escritura de identidades. Implementaciones con SQL parametrizado (ver JdbcUsuarioRepository). */
public interface UsuarioRepository {
    Optional<Usuario> findByCorreo(String correoNormalizado);

    boolean existsByCorreo(String correoNormalizado);

    /** Devuelve la id de la identidad con el correo dado o vacío. */
    Optional<UUID> idByCorreo(String correoNormalizado);
}
