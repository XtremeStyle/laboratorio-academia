package com.laboratorio.domain;

import java.util.Set;
import java.util.UUID;

/** Cuenta docente: crea Misiones, evalúa y consulta progreso dentro de su ámbito. */
public final class Profesor extends Usuario {
    public Profesor(UUID id, UUID academiaId, String correo, String nombre, String apellido, EstadoUsuario estado, Set<Rol> roles) {
        super(id, academiaId, correo, nombre, apellido, estado, roles);
    }
}
