package com.laboratorio.domain;

import java.util.Set;
import java.util.UUID;

/** Cuenta de estudiante: puede matricularse en Cursos y entregar Misiones. */
public final class Estudiante extends Usuario {
    public Estudiante(UUID id, UUID academiaId, String correo, String nombre, String apellido, EstadoUsuario estado, Set<Rol> roles) {
        super(id, academiaId, correo, nombre, apellido, estado, roles);
    }
}
