package com.laboratorio.domain;

import java.util.Set;
import java.util.UUID;

/** Cuenta administrativa de la academia: gestiona usuarios, semilleros, Tripulaciones y auditoría. */
public final class Administrador extends Usuario {
    public Administrador(UUID id, UUID academiaId, String correo, String nombre, String apellido, EstadoUsuario estado, Set<Rol> roles) {
        super(id, academiaId, correo, nombre, apellido, estado, roles);
    }
}
